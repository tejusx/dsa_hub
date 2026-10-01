import json
import hashlib
import re
from pathlib import Path
from collections import Counter


BASE_DIR = Path(__file__).resolve().parent.parent
PROCESSED_DIR = BASE_DIR / "data" / "processed"


def load(filename):

    with open(
        PROCESSED_DIR / filename,
        "r",
        encoding="utf-8"
    ) as f:

        return json.load(f)


problems = load(
    "master_problems.json"
)["problems"]

sheets = load(
    "sheets.json"
)["sheets"]

memberships = load(
    "sheet_problems.json"
)["sheet_problems"]


errors = []
warnings = []


def expected_problem_id(problem):
    leetcode_id = str(problem.get("leetcode_id") or "").strip()
    if leetcode_id:
        return f"lc-{leetcode_id}"
    identity = re.sub(r"[^a-z0-9]+", " ", (problem.get("title") or "").casefold()).strip()
    if not identity:
        identity = str((problem.get("source_ids") or [""])[0]).strip().casefold()
    slug = re.sub(r"[^a-z0-9]+", "-", identity).strip("-")[:48] or "problem"
    digest = hashlib.sha256(identity.encode("utf-8")).hexdigest()[:12]
    return f"p-{slug}-{digest}"


# ---------------------------------------------------------
# Problem IDs
# ---------------------------------------------------------

problem_ids = [
    p["problem_id"]
    for p in problems
]

duplicate_problem_ids = [
    pid
    for pid, count in Counter(problem_ids).items()
    if count > 1
]

if duplicate_problem_ids:

    errors.append({
        "type": "duplicate_problem_ids",
        "count": len(duplicate_problem_ids),
        "ids": duplicate_problem_ids[:50]
    })

duplicate_leetcode_ids = [
    key for key, count in Counter(str(p.get("leetcode_id")) for p in problems if p.get("leetcode_id")).items()
    if count > 1
]
if duplicate_leetcode_ids:
    errors.append({"type": "duplicate_leetcode_ids", "count": len(duplicate_leetcode_ids), "ids": duplicate_leetcode_ids[:50]})

unstable_problem_ids = [
    {"problem_id": p.get("problem_id"), "expected": expected_problem_id(p)}
    for p in problems if p.get("problem_id") != expected_problem_id(p)
]
if unstable_problem_ids:
    errors.append({"type": "unstable_problem_ids", "count": len(unstable_problem_ids), "items": unstable_problem_ids[:50]})


# ---------------------------------------------------------
# Empty titles
# ---------------------------------------------------------

missing_titles = [
    p["problem_id"]
    for p in problems
    if not p.get("title", "").strip()
]

if missing_titles:

    errors.append({
        "type": "missing_titles",
        "count": len(missing_titles),
        "ids": missing_titles[:50]
    })


# ---------------------------------------------------------
# Sheet IDs
# ---------------------------------------------------------

sheet_ids = [
    s["sheet_id"]
    for s in sheets
]

duplicate_sheet_ids = [
    sid
    for sid, count in Counter(sheet_ids).items()
    if count > 1
]

if duplicate_sheet_ids:

    errors.append({
        "type": "duplicate_sheet_ids",
        "count": len(duplicate_sheet_ids),
        "ids": duplicate_sheet_ids
    })


# ---------------------------------------------------------
# Membership validation
# ---------------------------------------------------------

problem_id_set = set(problem_ids)
sheet_id_set = set(sheet_ids)

invalid_problem_refs = []
invalid_sheet_refs = []

membership_keys = []

for m in memberships:

    pid = m["problem_id"]
    sid = m["sheet_id"]

    membership_keys.append(
        (sid, pid, str(m.get("position")), m.get("section"), m.get("subsection"))
    )

    if pid not in problem_id_set:

        invalid_problem_refs.append(
            {
                "sheet_id": sid,
                "problem_id": pid
            }
        )

    if sid not in sheet_id_set:

        invalid_sheet_refs.append(
            {
                "sheet_id": sid,
                "problem_id": pid
            }
        )


if invalid_problem_refs:

    errors.append({
        "type": "invalid_problem_references",
        "count": len(invalid_problem_refs),
        "items": invalid_problem_refs[:50]
    })


if invalid_sheet_refs:

    errors.append({
        "type": "invalid_sheet_references",
        "count": len(invalid_sheet_refs),
        "items": invalid_sheet_refs[:50]
    })


# ---------------------------------------------------------
# Duplicate memberships
# ---------------------------------------------------------

duplicate_memberships = [
    key
    for key, count in Counter(
        membership_keys
    ).items()
    if count > 1
]

if duplicate_memberships:

    errors.append({
        "type": "duplicate_memberships",
        "count": len(duplicate_memberships),
        "items": duplicate_memberships[:50]
    })


# ---------------------------------------------------------
# URL checks
# ---------------------------------------------------------

for problem in problems:

    links = problem.get(
        "links",
        {}
    )

    for name, url in links.items():

        if not isinstance(url, str):
            warnings.append({
                "type": "invalid_url",
                "problem_id": problem["problem_id"],
                "link_type": name
            })

        elif not (
            url.startswith("http://")
            or url.startswith("https://")
        ):

            warnings.append({
                "type": "invalid_url",
                "problem_id": problem["problem_id"],
                "link_type": name,
                "url": url
            })


# ---------------------------------------------------------
# Missing useful links
# ---------------------------------------------------------

missing_leetcode = sum(
    1
    for p in problems
    if p.get("leetcode_slug")
    and not p.get("links", {}).get("leetcode")
)

missing_title = sum(
    1
    for p in problems
    if not p.get("title")
)


# ---------------------------------------------------------
# Sheet statistics
# ---------------------------------------------------------

sheet_counts = Counter(
    m["sheet_id"]
    for m in memberships
)


# ---------------------------------------------------------
# Report
# ---------------------------------------------------------

report = {

    "status": (
        "PASS"
        if not errors
        else "FAIL"
    ),

    "statistics": {

        "problems": len(problems),

        "leetcode_id_problems": sum(bool(p.get("leetcode_id")) for p in problems),

        "deterministic_non_leetcode_id_problems": sum(not p.get("leetcode_id") for p in problems),

        "sheets": len(sheets),

        "memberships": len(memberships),

        "problems_with_leetcode": sum(
            1
            for p in problems
            if p.get("links", {}).get(
                "leetcode"
            )
        ),

        "problems_with_gfg": sum(
            1
            for p in problems
            if p.get("links", {}).get(
                "gfg"
            )
        ),

        "problems_with_coding_ninjas": sum(
            1
            for p in problems
            if p.get("links", {}).get(
                "coding_ninjas"
            )
        ),

        "problems_with_youtube": sum(
            1
            for p in problems
            if p.get("links", {}).get(
                "youtube"
            )
        ),

        "missing_leetcode_links": missing_leetcode,

        "missing_titles": missing_title
    },

    "sheet_counts": dict(
        sorted(sheet_counts.items())
    ),

    "errors": errors,

    "warnings": warnings[:500]
}


with open(
    PROCESSED_DIR / "validation_report.json",
    "w",
    encoding="utf-8"
) as f:

    json.dump(
        report,
        f,
        indent=2,
        ensure_ascii=False
    )


print("\n================================")
print("VALIDATION RESULT")
print("================================")

print(
    "Status:",
    report["status"]
)

print(
    "Problems:",
    len(problems)
)

print(
    "Sheets:",
    len(sheets)
)

print(
    "Memberships:",
    len(memberships)
)

print(
    "Errors:",
    len(errors)
)

print(
    "Warnings:",
    len(warnings)
)

print("================================")


if errors:

    print("\nERRORS:")

    for error in errors:

        print(
            f"- {error['type']}: "
            f"{error.get('count', 0)}"
        )

    raise SystemExit(1)

else:

    print("\nEverything passed validation.")
