import json
import re
from pathlib import Path
from urllib.parse import urlparse


# =========================================================
# PATHS
# =========================================================

BASE_DIR = Path(__file__).resolve().parent.parent

RAW_DIR = BASE_DIR / "data" / "raw"
PROCESSED_DIR = BASE_DIR / "data" / "processed"
CONFIG_DIR = BASE_DIR / "data" / "config"

PROCESSED_DIR.mkdir(parents=True, exist_ok=True)

# Scoped Striver A2Z YouTube corrections from the completed read-only audit.
# Missing-link positions receive their source video; seven explicit positions
# replace an inappropriate normalized video with the source video.
STRIVER_SOURCE_VIDEO_POSITIONS = frozenset({
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
    28, 29, 31, 33, 35, 58, 63, 64, 65, 67, 68, 70, 71, 75, 78, 79, 90, 92, 99, 100,
    102, 104, 105, 107, 112, 114, 115, 129, 131, 132, 141, 150, 152, 156, 164, 169,
    170, 173, 175, 189, 190, 193, 194, 195, 196, 197, 199, 201, 218, 219, 221, 239,
    241, 244, 248, 255, 257, 258, 259, 260, 262, 263, 265, 266, 267, 268, 269, 273,
    274, 275, 276, 283, 285, 286, 297, 299, 304, 307, 309, 310, 316, 318, 319, 324,
    327, 328, 329, 330, 333, 335, 338, 340, 341, 344, 354, 355, 357, 358, 359, 360,
    364, 365, 367, 368, 369, 370, 373, 375, 376, 377, 380, 381, 383, 384, 385, 388,
    391, 401, 402
})
STRIVER_SOURCE_VIDEO_OVERRIDES = {
    55: "https://www.youtube.com/watch?v=twuC1F6gLI8&list=PLgUwDviBIf0rGlzIn_7rsaR2FQ5e6ZOL9&index=4",
    81: "https://youtu.be/bR7mQgwQ_o8",
    97: "https://youtu.be/n7uwj04E0I4",
    261: "https://youtu.be/KV4mRzTjlAk",
    279: "https://youtu.be/9TJYWh0adfk",
    321: "https://www.youtube.com/watch?v=U5Mw4eyUmw4&list=PLgUwDviBIf0oE3gA41TKO2H5bHpPd7fzn&index=36",
    347: "https://youtu.be/_rgTlyky1uQ",
}


# =========================================================
# BASIC HELPERS
# =========================================================

def load_json(filename):
    path = RAW_DIR / filename

    if not path.exists():
        raise FileNotFoundError(
            f"Missing raw file: {path}"
        )

    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_json(filename, data):
    path = PROCESSED_DIR / filename

    with open(path, "w", encoding="utf-8") as f:
        json.dump(
            data,
            f,
            indent=2,
            ensure_ascii=False
        )

    print(f"[OK] {filename}")


def clean_text(value):
    if value is None:
        return ""

    if not isinstance(value, str):
        value = str(value)

    return re.sub(r"\s+", " ", value).strip()


def clean_url(value):
    if not isinstance(value, str):
        return None

    value = value.strip()

    if not value:
        return None

    if value.startswith("http://") or value.startswith("https://"):
        return value

    return None


def slugify(text):
    text = clean_text(text).lower()

    text = re.sub(
        r"[^a-z0-9]+",
        "-",
        text
    )

    text = re.sub(
        r"-+",
        "-",
        text
    )

    return text.strip("-")


def leetcode_slug_from_url(url):
    if not url:
        return None

    match = re.search(
        r"leetcode\.com/problems/([^/?#]+)",
        url
    )

    if match:
        return match.group(1)

    return None


def leetcode_id_from_record(record):
    possible_keys = [
        "leetcode_id",
        "leetcodeId",
        "frontendQuestionId",
        "questionFrontendId",
        "question_id",
        "questionId"
    ]

    for key in possible_keys:

        value = record.get(key)

        if value is not None and str(value).strip():
            return str(value).strip()

    # Sometimes the ID is embedded in a code like:
    # 0001-two-sum
    code = record.get("code")

    if isinstance(code, str):

        match = re.match(
            r"^0*(\d+)-",
            code.strip()
        )

        if match:
            return match.group(1)

    return None


def make_record(
    *,
    source,
    source_index,
    title,
    difficulty=None,
    topics=None,
    links=None,
    sheets=None,
    section=None,
    subsection=None,
    position=None,
    source_id=None,
    leetcode_id=None,
    extra=None
):

    title = clean_text(title)

    if not title and source != "book-sets.json":
        return None

    return {
        "source": source,
        "source_index": source_index,

        "source_id": (
            str(source_id)
            if source_id is not None
            else None
        ),

        "title": title,

        "difficulty": (
            clean_text(difficulty)
            if difficulty is not None
            else None
        ),

        "topics": sorted(
            set(
                clean_text(x)
                for x in (topics or [])
                if clean_text(x)
            )
        ),

        "links": {
            k: v
            for k, v in (links or {}).items()
            if clean_url(v)
        },

        "sheets": list(
            dict.fromkeys(
                s for s in (sheets or [])
                if s
            )
        ),

        "section": section,
        "subsection": subsection,
        "position": position,

        "leetcode_id": (
            str(leetcode_id)
            if leetcode_id is not None
            else None
        ),

        "extra": extra or {}
    }


# =========================================================
# 1. STRIVER A2Z
# =========================================================

def process_a2z():
    data = load_json("a2z.json")

    records = []

    def walk(obj):

        if isinstance(obj, list):

            for item in obj:
                walk(item)

            return

        if not isinstance(obj, dict):
            return

        # Actual A2Z question object
        if "question_title" in obj:

            title = obj.get(
                "question_title"
            )

            links = {
                "takeuforward": obj.get(
                    "post_link"
                ),
                "youtube": obj.get(
                    "yt_link"
                ),
                "gfg": obj.get(
                    "gfg_link"
                ),
                "coding_ninjas": obj.get(
                    "cs_link"
                ),
                "leetcode": obj.get(
                    "lc_link"
                ),
                "editorial": obj.get(
                    "editorial_link"
                ),
                "takeuforward_plus": obj.get(
                    "plus_link"
                )
            }

            topics = []

            raw_topics = obj.get(
                "ques_topic"
            )

            if isinstance(raw_topics, str):

                try:
                    parsed = json.loads(
                        raw_topics
                    )

                    if isinstance(parsed, list):

                        for topic in parsed:

                            if isinstance(topic, dict):

                                label = topic.get(
                                    "label"
                                )

                                if label:
                                    topics.append(
                                        label
                                    )

                except json.JSONDecodeError:
                    pass

            elif isinstance(raw_topics, list):

                for topic in raw_topics:

                    if isinstance(topic, dict):

                        label = topic.get(
                            "label"
                        )

                        if label:
                            topics.append(
                                label
                            )

            difficulty_map = {
                0: "Easy",
                1: "Easy",
                2: "Medium",
                3: "Hard"
            }

            raw_difficulty = obj.get(
                "difficulty"
            )

            difficulty = difficulty_map.get(
                raw_difficulty,
                str(raw_difficulty)
                if raw_difficulty is not None
                else None
            )

            records.append(
                make_record(
                    source="a2z.json",
                    source_index=len(records),

                    title=title,

                    difficulty=difficulty,

                    topics=topics,

                    links=links,

                    sheets=[
                        "striver-a2z"
                    ],

                    section=obj.get(
                        "step_title"
                    ),

                    subsection=obj.get(
                        "sub_step_title"
                    ),

                    position=obj.get(
                        "sl_no"
                    ),

                    source_id=obj.get(
                        "id"
                    ),

                    leetcode_id=None
                    ,extra={
                        "company_tags": obj.get("company_tags"),
                        "ques_topic": raw_topics,
                        "legacy_post_link": obj.get("post_link"),
                        "legacy_gfg_link": obj.get("gfg_link"),
                        "legacy_cs_link": obj.get("cs_link"),
                        "legacy_plus_link": obj.get("plus_link"),
                        "legacy_editorial_link": obj.get("editorial_link"),
                    }
                )
            )

            return

        # Continue walking nested A2Z structure
        for value in obj.values():
            walk(value)

    walk(data)

    return [
        r for r in records
        if r is not None
    ]


def process_striver_a2z():
    """Adapt the current Striver outline and its companion enrichment dataset."""
    base_items = load_json("striver/base_items.json")
    enriched = load_json("striver/data.json").get("problems", [])

    def title_key(value):
        return re.sub(r"[^a-z0-9]+", " ", clean_text(value).casefold()).strip()

    def context_key(*values):
        return " ".join(title_key(value) for value in values if value)

    # Old source fields remain supplementary. Ambiguous title groups are not
    # attached automatically, avoiding accidental cross-topic metadata merges.
    legacy = {}
    old_path = RAW_DIR / "a2z.json"
    if old_path.exists():
        old_records = process_a2z()
        for record in old_records:
            legacy.setdefault(title_key(record["title"]), []).append(record)

    enriched_by_title = {}
    for item in enriched:
        enriched_by_title.setdefault(title_key(item.get("title")), []).append(item)

    match_report = {
        "strategy": ["normalized title + topic/section context", "normalized title + slug", "unique normalized title"],
        "base_practice_count": sum(1 for item in base_items if item.get("layout") == "practice"),
        "learning_records_excluded": sum(1 for item in base_items if item.get("layout") == "learning"),
        "nonpractice_records_excluded": sum(1 for item in base_items if item.get("layout") is None),
        "data_problem_count": len(enriched),
        "matches": [],
        "unmatched_base": [],
        "unmatched_data": [],
        "ambiguous": [],
    }
    legacy_metadata_attached = 0
    matched_data_ids = set()
    records = []

    def platform_name(value, url=None):
        key = re.sub(r"[^a-z]", "", clean_text(value).casefold())
        aliases = {"takeuforward": "takeuforward", "tuf": "takeuforward", "leetcode": "leetcode",
                  "geeksforgeeks": "gfg", "gfg": "gfg", "youtube": "youtube",
                  "spoj": "spoj", "code360": "code360", "codingninjas": "code360",
                  "discussion": "discussion"}
        return aliases.get(key)

    def platform_from_url(url):
        host = (urlparse(url).netloc or "").casefold()
        path = (urlparse(url).path or "").casefold()
        if host.endswith("spoj.com"):
            return "spoj"
        if "codingninjas.com" in host or "code360" in host or "naukri.com" in host:
            return "code360"
        if "geeksforgeeks.org" in host:
            return "gfg"
        if host.endswith("leetcode.com") and "/discuss/" in path:
            return "discussion"
        return None

    for base_index, item in enumerate(base_items):
        if item.get("layout") != "practice":
            continue
        title = clean_text(item.get("title"))
        key = title_key(title)
        candidates = enriched_by_title.get(key, [])
        selected = None
        method = None
        if len(candidates) == 1:
            selected, method = candidates[0], "normalized_title"
        elif len(candidates) > 1:
            context = context_key(item.get("topic"), item.get("section"))
            contextual = [candidate for candidate in candidates
                          if context_key(candidate.get("topic"), candidate.get("stepTitle")) in context
                          or context_key(candidate.get("topic")) == context_key(item.get("topic"))]
            if len(contextual) == 1:
                selected, method = contextual[0], "normalized_title_and_context"
            else:
                slugged = [candidate for candidate in candidates
                           if item.get("slug") and candidate.get("slug") == item.get("slug")]
                if len(slugged) == 1:
                    selected, method = slugged[0], "normalized_title_and_slug"
                else:
                    match_report["ambiguous"].append({"title": title, "section": item.get("section"),
                                                       "topic": item.get("topic"), "candidate_count": len(candidates)})
        if selected is None:
            match_report["unmatched_base"].append({"title": title, "section": item.get("section"), "topic": item.get("topic")})
            # Keep the practice occurrence with its structural source data even
            # when enrichment could not be assigned confidently.
            selected = {"title": title, "order": None, "platform": "takeuforward",
                        "url": item.get("tufUrl"), "altUrls": [], "difficulty": "",
                        "tier": item.get("tier"), "slug": item.get("slug"),
                        "article": item.get("articleUrl"), "video": item.get("videoUrl")}
        else:
            matched_data_ids.add(id(selected))
            match_report["matches"].append({"title": title, "order": selected.get("order"), "method": method,
                                             "section": item.get("section"), "topic": item.get("topic")})
        old_candidates = legacy.get(key, [])
        old = old_candidates[0] if len(old_candidates) == 1 else None

        links = {}
        def add_link(platform, url):
            normalized = platform_name(platform)
            if selected.get("order") in {101, 122, 123, 148, 149, 202, 203, 206, 207}:
                normalized = platform_from_url(url) or normalized
            url = clean_url(url)
            if normalized and url and normalized not in links:
                links[normalized] = url

        add_link(selected.get("platform"), selected.get("url"))
        for alt in selected.get("altUrls") or []:
            if isinstance(alt, dict):
                add_link(alt.get("platform"), alt.get("url"))
        add_link("takeuforward", item.get("tufUrl"))
        add_link("leetcode", item.get("leetcodeUrl"))
        if old:
            old_links = old.get("links") or {}
            for platform in ("takeuforward", "gfg", "leetcode"):
                if platform in (item.get("suppressLegacyPlatforms") or []):
                    continue
                add_link(platform, old_links.get(platform))
            if not links.get("youtube"):
                add_link("youtube", old_links.get("youtube"))

        position = selected.get("order")
        if position in STRIVER_SOURCE_VIDEO_POSITIONS:
            source_video = clean_url(selected.get("video") or item.get("videoUrl"))
            if source_video:
                links["youtube"] = source_video
        if position in STRIVER_SOURCE_VIDEO_OVERRIDES:
            links["youtube"] = STRIVER_SOURCE_VIDEO_OVERRIDES[position]

        extras = {
            "section_path": [clean_text(part) for part in (item.get("sectionPath") or [])
                            if clean_text(part)],
            "outline_topic": item.get("outlineTopic"),
            "layout": item.get("layout"),
            "tier": selected.get("tier") or item.get("tier"),
            "slug": selected.get("slug") or item.get("slug"),
            "lc_slug": selected.get("lcSlug"),
            "article": selected.get("article") or item.get("articleUrl"),
            "video": selected.get("video") or item.get("videoUrl"),
            "premium": selected.get("premium"),
            "duration": selected.get("duration") or item.get("duration"),
            "is_trial": item.get("isTrial"),
            "base_tuf_id": item.get("tufId"),
            "source_data_id": selected.get("id"),
            "source_platform": platform_name(selected.get("platform")),
            "alternate_links": selected.get("altUrls") or [],
            "canonical_problem_id": selected.get("canonicalProblemId"),
            "canonical_problem_title": selected.get("canonicalProblemTitle"),
        }
        if old_candidates:
            if old:
                legacy_metadata_attached += 1
                old_source = old.get("extra") or {}
                extras["legacy_a2z"] = {
                    "company_tags": old_source.get("company_tags"),
                    "ques_topic": old_source.get("ques_topic"),
                    "cs_link": old_source.get("legacy_cs_link"),
                    "plus_link": old_source.get("legacy_plus_link"),
                    "editorial_link": old_source.get("legacy_editorial_link"),
                    "post_link": old_source.get("legacy_post_link"),
                    "gfg_link": old_source.get("legacy_gfg_link"),
                }
            else:
                extras["legacy_a2z_match_status"] = "ambiguous; not attached"

        records.append(make_record(
            source="striver-a2z", source_index=(selected.get("order") or base_index + 1) - 1,
            title=title, difficulty=selected.get("difficulty") or None,
            topics=[item.get("topic"), item.get("outlineTopic")], links=links,
            sheets=["striver-a2z"], section=clean_text(item.get("section")),
            subsection=clean_text(item.get("topic")), position=selected.get("order"),
            source_id=item.get("tufId"), extra=extras))

    for candidates in enriched_by_title.values():
        for candidate in candidates:
            if id(candidate) not in matched_data_ids:
                match_report["unmatched_data"].append({"title": candidate.get("title"), "order": candidate.get("order")})

    records = [record for record in records if record is not None]
    records.sort(key=lambda record: (record.get("position") is None, record.get("position") or 0))
    match_report["matched_count"] = len(match_report["matches"])
    match_report["unmatched_count"] = len(match_report["unmatched_base"]) + len(match_report["unmatched_data"])
    match_report["legacy_metadata_records_attached"] = legacy_metadata_attached
    match_report["legacy_metadata_ambiguous"] = sum(1 for item in records
        if (item.get("extra") or {}).get("legacy_a2z_match_status") == "ambiguous; not attached")
    save_json("striver_migration_report.json", match_report)
    return records


# =========================================================
# 2. NEETCODE 150
# =========================================================

def process_neetcode():
    data = load_json(
        "neetcode-150-list.json"
    )

    records = []

    position = 1

    def walk_category(
        category_name,
        problems
    ):

        nonlocal position

        if not isinstance(
            problems,
            dict
        ):
            return

        for title, info in problems.items():

            if not isinstance(
                info,
                dict
            ):
                continue

            if not (
                info.get("url")
                or info.get("nurl")
            ):
                continue

            lc_url = clean_url(
                info.get("url")
            )

            neetcode_url = clean_url(
                info.get("nurl")
            )

            records.append(
                make_record(
                    source=(
                        "neetcode-150-list.json"
                    ),

                    source_index=len(
                        records
                    ),

                    title=title,

                    difficulty=info.get(
                        "difficulty"
                    ),

                    topics=[
                        category_name
                    ],

                    links={
                        "leetcode": lc_url,
                        "neetcode": neetcode_url
                    },

                    sheets=[
                        "neetcode-150"
                    ],

                    section=category_name,

                    position=position,

                    leetcode_id=None
                )
            )

            position += 1

    # Actual structure:
    #
    # {
    #   "Arrays & Hashing": {
    #       "Two Sum": {...},
    #       ...
    #   }
    # }

    if isinstance(data, dict):

        for category, problems in data.items():

            walk_category(
                category,
                problems
            )

    return [
        r for r in records
        if r is not None
    ]


# =========================================================
# 3. BOOLEANSTACK DATASET
# =========================================================

def process_booleanstack():
    data = load_json(
        "booleanstack_dsa_problems.json"
    )

    records = []

    sheet_flags = {
        "in_algoprep": "algoprep-151",
        "in_shradha": "shradha-maam",
        "in_siddharth": "siddharth-singh",
        "in_final450": "love-babbar-450",
        "in_fraz": "fraz-leetcode",
        # Striver A2Z occurrences now come exclusively from its structural adapter.
    }

    if not isinstance(data, list):
        return records

    love_babbar_position = 0
    for index, item in enumerate(data):

        if not isinstance(item, dict):
            continue

        title = item.get("title")

        if not title:
            continue

        sheets = []
        sheet_occurrences = []

        for flag, sheet_name in sheet_flags.items():

            value = item.get(flag)

            if str(value).strip().lower() == "yes":
                sheets.append(sheet_name)
                if sheet_name == "love-babbar-450":
                    love_babbar_position += 1
                    topic = clean_text(item.get("topic"))
                    sheet_occurrences.append({
                        "sheet_id": sheet_name,
                        "position": love_babbar_position,
                        "section": topic,
                        "subsection": None,
                        "section_path": [topic] if topic else [],
                    })

        links = {
            "leetcode": item.get(
                "problem_url"
            ),
            "youtube": item.get(
                "fraz_editorial"
            )
        }

        records.append(
            make_record(
                source=(
                    "booleanstack_dsa_problems.json"
                ),

                source_index=index,

                title=title,

                difficulty=item.get(
                    "difficulty"
                ),

                topics=[
                    item.get("topic")
                ],

                links=links,

                sheets=sheets,

                source_id=item.get(
                    "problem_id"
                ),

                extra={
                    "companies": item.get(
                        "companies"
                    ),
                    "hints_remarks": item.get(
                        "hints_remarks"
                    ),
                    "strivers_path": item.get(
                        "strivers_path"
                    ),
                    "raw_variations": item.get(
                        "raw_variations"
                    ),
                    "sheet_occurrences": sheet_occurrences,
                }
            )
        )

    return [
        r for r in records
        if r is not None
    ]


# =========================================================
# 4. PROBLEM SITE DATA
# =========================================================

def process_problem_site_data():
    data = load_json(
        "problemSiteData.json"
    )

    records = []

    if not isinstance(data, list):
        return records

    blind_position = 0
    for index, item in enumerate(data):

        if not isinstance(item, dict):
            continue

        title = item.get(
            "problem"
        )

        if not title:
            continue

        sheets = []

        if item.get(
            "blind75"
        ) is True:
            sheets.append(
                "blind-75"
            )
            blind_position += 1

        slug = item.get(
            "link"
        )

        if isinstance(slug, str):

            slug = slug.strip(
                "/"
            )

            lc_url = (
                "https://leetcode.com/problems/"
                + slug
                + "/"
            )

        else:
            lc_url = None

        video = item.get(
            "video"
        )

        youtube_url = None

        if isinstance(video, str) and video:

            youtube_url = (
                "https://www.youtube.com/watch?v="
                + video
            )

        leetcode_id = None

        code = item.get(
            "code"
        )

        if isinstance(code, str):

            match = re.match(
                r"^0*(\d+)-",
                code
            )

            if match:
                leetcode_id = (
                    match.group(1)
                )

        records.append(
            make_record(
                source=(
                    "problemSiteData.json"
                ),

                source_index=index,

                title=title,

                difficulty=item.get(
                    "difficulty"
                ),

                topics=[
                    item.get("pattern")
                ],

                links={
                    "leetcode": lc_url,
                    "youtube": youtube_url
                },

                sheets=sheets,

                section=item.get("pattern") if "blind-75" in sheets else None,
                position=blind_position if "blind-75" in sheets else None,

                source_id=code,

                leetcode_id=leetcode_id,
                extra={"sheet_occurrences": ([{
                    "sheet_id": "blind-75",
                    "position": blind_position,
                    "section": item.get("pattern"),
                    "subsection": None,
                    "section_path": [item.get("pattern")] if item.get("pattern") else [],
                }] if "blind-75" in sheets else [])}
            )
        )

    return [
        r for r in records
        if r is not None
    ]


# =========================================================
# 5. LEETCODE REFERENCE DATA
# =========================================================

def process_leetcode():
    data = load_json(
        "leetcode.json"
    )

    records = []

    def process_item(
        item,
        index
    ):

        if not isinstance(item, dict):
            return

        title = (
            item.get("title")
            or item.get("name")
        )

        if not title:
            return

        slug = (
            item.get("titleSlug")
            or item.get("slug")
            or item.get("title_slug")
        )

        lc_url = None

        if slug:

            lc_url = (
                "https://leetcode.com/problems/"
                + str(slug).strip("/")
                + "/"
            )

        else:

            possible_url = (
                item.get("url")
                or item.get("leetcode")
            )

            lc_url = clean_url(
                possible_url
            )

            slug = leetcode_slug_from_url(
                lc_url
            )

        records.append(
            make_record(
                source="leetcode.json",

                source_index=index,

                title=title,

                difficulty=item.get(
                    "difficulty"
                ),

                topics=[t.get("name") for t in item.get("topicTags", []) if isinstance(t, dict) and t.get("name")]
                if isinstance(item.get("topicTags"), list) else item.get("topics", []) if isinstance(item.get("topics"), list) else [],

                links={
                    "leetcode": lc_url
                },

                source_id=(
                    item.get(
                        "questionFrontendId"
                    )
                    or item.get(
                        "frontendQuestionId"
                    )
                    or item.get("id")
                ),

                leetcode_id=(
                    item.get(
                        "questionFrontendId"
                    )
                    or item.get(
                        "frontendQuestionId"
                    )
                ),

                extra={
                    "leetcode_slug": slug,
                    "paid_only": item.get("paidOnly"),
                    "has_solution": item.get("hasSolution"),
                    "has_video_solution": item.get("hasVideoSolution")
                }
            )
        )

    def find_questions(obj):
        if isinstance(obj, dict):
            for key, value in obj.items():
                if key in ("questions", "problemsetQuestionList") and isinstance(value, list):
                    return value
            for value in obj.values():
                found = find_questions(value)
                if found is not None:
                    return found
        elif isinstance(obj, list):
            if obj and isinstance(obj[0], dict) and ("frontendQuestionId" in obj[0] or "titleSlug" in obj[0]):
                return obj
            for value in obj:
                found = find_questions(value)
                if found is not None:
                    return found
        return None

    questions = find_questions(data)
    if questions is not None:
        for index, item in enumerate(questions):
            process_item(item, index)

    return [
        r for r in records
        if r is not None
    ]


# =========================================================
# 6. LEETCODE BOOK SETS
# =========================================================

def process_book_sets():

    data = load_json(
        "book-sets.json"
    )

    records = []

    leetcode_topics_by_id = {}
    leetcode_data = load_json("leetcode.json")

    def find_question_list(obj):
        if isinstance(obj, dict):
            for key, value in obj.items():
                if key == "questions" and isinstance(value, list):
                    return value
                found = find_question_list(value)
                if found is not None:
                    return found
        elif isinstance(obj, list):
            if obj and isinstance(obj[0], dict) and "frontendQuestionId" in obj[0]:
                return obj
            for value in obj:
                found = find_question_list(value)
                if found is not None:
                    return found
        return None

    for question in find_question_list(leetcode_data) or []:
        question_id = str(question.get("frontendQuestionId") or "").lstrip("0")
        tags = question.get("topicTags") or []
        if question_id:
            leetcode_topics_by_id[question_id] = [tag.get("name") for tag in tags
                                                  if isinstance(tag, dict) and tag.get("name")]

    if not isinstance(
        data,
        list
    ):
        return records

    for sheet_index, sheet in enumerate(
        data
    ):

        if not isinstance(
            sheet,
            dict
        ):
            continue

        sheet_title = clean_text(
            sheet.get("title")
        )

        if not sheet_title:
            continue

        # Create a clean sheet ID
        sheet_id = slugify(
            sheet_title
        )

        problems = sheet.get(
            "problems",
            []
        )

        if not isinstance(
            problems,
            list
        ):
            continue

        for position, leetcode_id in enumerate(
            problems,
            start=1
        ):

            topics = leetcode_topics_by_id.get(str(leetcode_id).lstrip("0"), [])
            section = topics[0] if sheet_id in {"leetcode-75", "leetcode-top-150"} and topics else None

            records.append(
                make_record(
                    source="book-sets.json",

                    source_index=len(
                        records
                    ),

                    # No title here.
                    # The matching stage will resolve
                    # this using leetcode.json.
                    title="",

                    sheets=[
                        sheet_id
                    ],

                    position=position,

                    section=section,
                    topics=topics,

                    leetcode_id=str(
                        leetcode_id
                    ),

                    extra={
                        "sheet_title": sheet_title,
                        "description": sheet.get(
                            "description"
                        ),
                        "tags": sheet.get(
                            "tags",
                            []
                        ),
                        "section_path": [section] if section else [],
                    }
                )
            )

    return [
        r for r in records
        if r is not None
    ]


# =========================================================
# RUN ALL SOURCES
# =========================================================

def main():

    all_records = []

    processors = {
        "a2z.json": process_striver_a2z,
        "neetcode-150-list.json": process_neetcode,
        "booleanstack_dsa_problems.json": process_booleanstack,
        "problemSiteData.json": process_problem_site_data,
        "leetcode.json": process_leetcode,
        "book-sets.json": process_book_sets,
    }

    config_path = CONFIG_DIR / "sources.json"
    if config_path.exists():
        source_config = json.loads(config_path.read_text(encoding="utf-8"))
    else:
        source_config = {"sources": [{"id": Path(p).stem, "file": p, "enabled": True} for p in sorted(p.name for p in RAW_DIR.glob("*.json"))]}
        CONFIG_DIR.mkdir(parents=True, exist_ok=True)
        config_path.write_text(json.dumps(source_config, indent=2), encoding="utf-8")

    print()
    print("=" * 60)
    print("DSA HUB NORMALIZATION")
    print("=" * 60)

    for source in source_config.get("sources", []):
        filename = source.get("file")
        processor = processors.get(filename)
        if not source.get("enabled", False):
            print(f"Skipping disabled source {filename}")
            continue
        if not filename or not (RAW_DIR / filename).exists():
            print(f"Skipping missing source {filename}")
            continue
        if processor is None:
            print(f"Skipping unsupported source {filename}")
            continue

        print()
        print(
            f"Processing {filename}..."
        )

        records = processor()

        print(
            f"  Records: {len(records)}"
        )

        all_records.extend(
            records
        )

    # -----------------------------------------------------
    # Remove accidental exact duplicates
    # -----------------------------------------------------

    seen = set()
    cleaned = []

    for record in all_records:

        key = (
            record["source"],
            record["source_index"]
        )

        if key in seen:
            continue

        seen.add(key)

        cleaned.append(
            record
        )

    # -----------------------------------------------------
    # Save
    # -----------------------------------------------------

    save_json(
        "normalized_problems.json",
        {
            "version": "2.0",

            "description":
                "Source-specific normalized "
                "DSA dataset.",

            "records": cleaned
        }
    )

    print()
    print("=" * 60)
    print("NORMALIZATION COMPLETE")
    print("=" * 60)

    print(
        "Total records:",
        len(cleaned)
    )

    print("=" * 60)


if __name__ == "__main__":
    main()
