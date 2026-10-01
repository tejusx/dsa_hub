"""Merge normalized DSA records using identifiers before conservative title matching."""
import json
import re
import hashlib
from pathlib import Path
from collections import Counter

BASE = Path(__file__).resolve().parent.parent
PROC = BASE / "data" / "processed"
CONFIG = BASE / "data" / "config"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False), encoding="utf-8")


def title_key(value):
    value = (value or "").lower().strip()
    # Only strip conventional numbered prefixes; preserve titles such as "3-d DP".
    value = re.sub(r"^(?:leetcode\s*)?\d+\s*(?:[.)]|[-:]\s+)\s*", "", value)
    return re.sub(r"[^a-z0-9]+", " ", value).strip()


def normalize_topic(value):
    """Apply only explicit, safe aliases; keep distinct concepts untouched."""
    value = re.sub(r"\s+", " ", str(value)).strip()
    if value.casefold() == "arrays":
        return "Array"
    return value


def stable_identity(value):
    return re.sub(r"[^a-z0-9]+", " ", (value or "").casefold()).strip()


def deterministic_problem_id(record):
    leetcode_id = str(record.get("leetcode_id") or "").strip()
    if leetcode_id:
        return f"lc-{leetcode_id}"
    identity = stable_identity(record.get("title"))
    if not identity:
        identity = str(record.get("source_id") or "").strip().casefold()
    if not identity:
        raise ValueError(f"Cannot form a stable identity for record: {record!r}")
    slug = re.sub(r"[^a-z0-9]+", "-", identity).strip("-")[:48] or "problem"
    digest = hashlib.sha256(identity.encode("utf-8")).hexdigest()[:12]
    return f"p-{slug}-{digest}"


def clean_list(values):
    out = []
    for value in values or []:
        if isinstance(value, (str, int, float)):
            value = str(value).strip()
            if value and value not in out:
                out.append(value)
    return out


def main():
    records = load(PROC / "normalized_problems.json").get("records", [])
    config_path = CONFIG / "sheets.json"
    config = load(config_path) if config_path.exists() else {"sheets": []}
    sheet_config = {s["id"]: dict(s) for s in config.get("sheets", []) if s.get("id")}
    discovered = {sid for r in records for sid in r.get("sheets", []) if sid}
    for sid in sorted(discovered - sheet_config.keys()):
        sheet_config[sid] = {"id": sid, "name": sid.replace("-", " ").title(), "enabled": False, "order": 9999}
        print(f"[NEW SHEET] {sid} (disabled by default)")
    if len(sheet_config) != len(config.get("sheets", [])):
        config["sheets"] = sorted(sheet_config.values(), key=lambda s: (s.get("order", 9999), s["id"]))
        save(config_path, config)

    # Build canonical LeetCode records first, so all stronger identifiers resolve to them.
    records.sort(key=lambda r: (r.get("source") != "leetcode.json", r.get("source_index", 0)))
    leetcode_slugs = {
        str((r.get("extra") or {}).get("leetcode_slug") or "").strip().lower()
        for r in records if r.get("source") == "leetcode.json"
    }
    leetcode_slugs.discard("")
    masters, by_id, by_slug, by_title = {}, {}, {}, {}
    lc_id_map = {}

    def add_unique(target, values):
        for value in values:
            if value not in target:
                target.append(value)

    for record in records:
        source = record.get("source") or "unknown"
        lc_id = str(record.get("leetcode_id")).strip() if record.get("leetcode_id") else None
        extra = record.get("extra") or {}
        slug = extra.get("leetcode_slug")
        if not slug:
            match = re.search(r"leetcode\.com/problems/([^/?#]+)", (record.get("links") or {}).get("leetcode", ""), re.I)
            slug = match.group(1).lower() if match else None
        # A URL slug is an identity only when it exists in the LeetCode reference set.
        # Several source records contain LeetCode-shaped links for non-LeetCode problems.
        if slug and slug.lower() not in leetcode_slugs:
            slug = None
        key_title = title_key(record.get("title"))
        is_reference = source == "leetcode.json"

        # Book set entries carry IDs and memberships only; attach them to the reference record.
        if source == "book-sets.json":
            master_id = lc_id_map.get(lc_id)
            if not master_id:
                continue
        else:
            master_id = (by_id.get(lc_id) if lc_id else None) or (by_slug.get(slug) if slug else None)
            if not master_id and key_title:
                candidate = by_title.get(key_title)
                if candidate:
                    candidate_id = masters[candidate].get("leetcode_id")
                    if not lc_id or not candidate_id or candidate_id == lc_id:
                        master_id = candidate
            if master_id is None:
                master_id = deterministic_problem_id(record)
                if master_id in masters:
                    raise ValueError(f"Deterministic problem ID collision: {master_id}")
                masters[master_id] = {"problem_id": master_id, "title": record.get("title") or None,
                    "leetcode_id": None, "leetcode_slug": None, "difficulty": None, "topics": [], "links": {},
                    "sources": [], "source_ids": [], "sheets": [], "sections": [], "metadata": {}}

        master = masters[master_id]
        if record.get("title") and (not master["title"] or is_reference):
            master["title"] = record["title"]
        if lc_id and not master["leetcode_id"]:
            master["leetcode_id"] = lc_id
        if slug and not master["leetcode_slug"]:
            master["leetcode_slug"] = slug
        if record.get("difficulty") and not master["difficulty"]:
            master["difficulty"] = record["difficulty"]
        topics = [normalize_topic(topic) for topic in clean_list(record.get("topics"))]
        add_unique(master["topics"], topics)
        for name, url in (record.get("links") or {}).items():
            if isinstance(url, str) and url.startswith(("http://", "https://")):
                if name == "leetcode":
                    match = re.search(r"leetcode\.com/problems/([^/?#]+)", url, re.I)
                    if match and match.group(1).lower() not in leetcode_slugs:
                        master["metadata"].setdefault("unverified_links", {}).setdefault(source, {})[name] = url
                        continue
                master["links"].setdefault(name, url)
        add_unique(master["sources"], [source])
        add_unique(master["source_ids"], clean_list([record.get("source_id")]))
        add_unique(master["sheets"], clean_list(record.get("sheets")))
        if record.get("section") or record.get("subsection") or record.get("position") is not None:
            detail = {"sheet_ids": clean_list(record.get("sheets")), "section": record.get("section"),
                      "subsection": record.get("subsection"), "position": record.get("position")}
            if detail not in master["sections"]:
                master["sections"].append(detail)
        for key, value in (record.get("extra") or {}).items():
            if value not in (None, "", [], {}):
                master["metadata"].setdefault(key, value)
        if key_title:
            by_title.setdefault(key_title, master_id)
        if lc_id:
            by_id.setdefault(lc_id, master_id)
            if is_reference:
                lc_id_map[lc_id] = master_id
        if slug:
            by_slug.setdefault(slug, master_id)

    memberships, seen = [], set()
    for pid, p in masters.items():
        for sid in p["sheets"]:
            details = [d for d in p["sections"] if sid in d["sheet_ids"]]
            if not details:
                details = [{"position": None, "section": None, "subsection": None}]
            for d in details:
                item = {"sheet_id": sid, "problem_id": pid, "position": d.get("position"),
                        "section": d.get("section"), "subsection": d.get("subsection")}
                key = (sid, pid, str(item["position"]), item["section"], item["subsection"])
                if key not in seen:
                    seen.add(key)
                    memberships.append(item)

    sheet_counts = Counter(m["sheet_id"] for m in memberships)
    sheets = sorted(({"sheet_id": sid, "name": s.get("name", sid), "enabled": bool(s.get("enabled", False)),
                      "order": s.get("order", 9999), "problem_count": sheet_counts[sid]}
                     for sid, s in sheet_config.items()), key=lambda s: (s["order"], s["sheet_id"]))
    save(PROC / "master_problems.json", {"version": "2.0", "problem_count": len(masters), "problems": list(masters.values())})
    save(PROC / "sheet_problems.json", {"version": "2.0", "membership_count": len(memberships), "sheet_problems": memberships, "memberships": memberships})
    save(PROC / "sheets.json", {"version": "2.0", "sheet_count": len(sheets), "sheets": sheets})
    save(PROC / "dsa_hub_final.json", {"version": "2.0", "problems": list(masters.values()),
         "sheets": sheets, "sheet_problems": memberships})
    print(f"Normalized records: {len(records)}")
    print(f"LeetCode reference IDs: {len(lc_id_map)}")
    print(f"Master problems: {len(masters)}")
    print(f"Sheets discovered: {len(sheets)}; enabled: {sum(s['enabled'] for s in sheets)}")
    print(f"Memberships: {len(memberships)}")
    for s in sheets:
        print(f"{s['sheet_id']}: {s['problem_count']} ({'enabled' if s['enabled'] else 'disabled'})")


if __name__ == "__main__":
    main()
