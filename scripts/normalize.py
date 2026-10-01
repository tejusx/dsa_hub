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


# =========================================================
# 2. NEETCODE 150
# =========================================================

def process_neetcode():
    data = load_json(
        "neetcode-150-list.json"
    )

    records = []

    def walk_category(
        category_name,
        problems
    ):

        if not isinstance(
            problems,
            dict
        ):
            return

        position = 1

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
        "in_strivers": "striver-a2z"
    }

    if not isinstance(data, list):
        return records

    for index, item in enumerate(data):

        if not isinstance(item, dict):
            continue

        title = item.get("title")

        if not title:
            continue

        sheets = []

        for flag, sheet_name in sheet_flags.items():

            value = item.get(flag)

            if str(value).strip().lower() == "yes":
                sheets.append(sheet_name)

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
                    )
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
            "neetcode150"
        ) is True:

            sheets.append(
                "neetcode-150"
            )

        if item.get(
            "blind75"
        ) is True:

            sheets.append(
                "blind-75"
            )

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

                source_id=code,

                leetcode_id=leetcode_id
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
                        )
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
        "a2z.json": process_a2z,
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
