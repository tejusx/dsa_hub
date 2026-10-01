"""Idempotently import the website-ready dataset into PostgreSQL.

Run without arguments for offline input checks. Use --apply with DATABASE_URL set
to initialize/update the schema and import the current final JSON dataset.
Requires psycopg v3 only for --apply: pip install "psycopg[binary]".
"""
import argparse
import json
import os
import re
import sys
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent.parent
FINAL_FILE = BASE_DIR / "data" / "processed" / "dsa_hub_final.json"
SCHEMA_FILE = BASE_DIR / "database" / "schema.sql"


def load_and_validate():
    with FINAL_FILE.open("r", encoding="utf-8") as file:
        dataset = json.load(file)
    for key in ("problems", "sheets", "sheet_problems"):
        if not isinstance(dataset.get(key), list):
            raise ValueError(f"Final dataset field {key!r} must be an array")

    problems = dataset["problems"]
    sheets = dataset["sheets"]
    memberships = dataset["sheet_problems"]
    problem_ids = [p.get("problem_id") for p in problems]
    sheet_ids = [s.get("sheet_id") for s in sheets]
    if None in problem_ids or len(problem_ids) != len(set(problem_ids)):
        raise ValueError("Problem IDs must be present and unique")
    if None in sheet_ids or len(sheet_ids) != len(set(sheet_ids)):
        raise ValueError("Sheet IDs must be present and unique")
    problem_id_set, sheet_id_set = set(problem_ids), set(sheet_ids)
    for problem in problems:
        if not isinstance(problem.get("title"), str) or not problem["title"].strip():
            raise ValueError(f"Problem {problem.get('problem_id')} has no title")
        if not isinstance(problem.get("links", {}), dict) or not isinstance(problem.get("topics", []), list):
            raise ValueError(f"Problem {problem['problem_id']} has malformed links/topics")
        leetcode_id = problem.get("leetcode_id")
        if leetcode_id is not None and problem["problem_id"] != f"lc-{leetcode_id}":
            raise ValueError(f"Problem ID does not match its LeetCode ID: {problem['problem_id']}")
        for link_type, url in problem.get("links", {}).items():
            if not isinstance(url, str) or not re.match(r"^https?://[^\s]+$", url):
                raise ValueError(f"Malformed link {link_type!r} on {problem['problem_id']}")
    for sheet in sheets:
        if not isinstance(sheet.get("name"), str) or not isinstance(sheet.get("enabled"), bool):
            raise ValueError(f"Malformed sheet entry: {sheet!r}")
    for membership in memberships:
        if membership.get("problem_id") not in problem_id_set:
            raise ValueError(f"Dangling problem membership: {membership!r}")
        if membership.get("sheet_id") not in sheet_id_set:
            raise ValueError(f"Dangling sheet membership: {membership!r}")
        if membership.get("position") is not None and not isinstance(membership["position"], int):
            raise ValueError(f"Non-integer membership position: {membership!r}")
    return dataset


def apply_dataset(dataset, database_url):
    try:
        import psycopg
    except ImportError as exc:
        raise RuntimeError('Install psycopg v3 with: pip install "psycopg[binary]"') from exc

    schema_sql = SCHEMA_FILE.read_text(encoding="utf-8")
    with psycopg.connect(database_url) as connection:
        with connection.cursor() as cursor:
            cursor.execute(schema_sql)

            for problem in dataset["problems"]:
                metadata = problem.get("metadata") or {}
                cursor.execute(
                    """INSERT INTO problems
                       (problem_id, title, leetcode_id, leetcode_slug, difficulty,
                        leetcode_paid_only, leetcode_has_solution, leetcode_has_video_solution, updated_at)
                       VALUES (%s, %s, %s, %s, %s, %s, %s, %s, now())
                       ON CONFLICT (problem_id) DO UPDATE SET
                         title = EXCLUDED.title, leetcode_id = EXCLUDED.leetcode_id,
                         leetcode_slug = EXCLUDED.leetcode_slug, difficulty = EXCLUDED.difficulty,
                         leetcode_paid_only = EXCLUDED.leetcode_paid_only,
                         leetcode_has_solution = EXCLUDED.leetcode_has_solution,
                         leetcode_has_video_solution = EXCLUDED.leetcode_has_video_solution,
                         updated_at = now()""",
                    (problem["problem_id"], problem["title"], problem.get("leetcode_id"),
                     problem.get("leetcode_slug"), problem.get("difficulty"), metadata.get("paid_only"),
                     metadata.get("has_solution"), metadata.get("has_video_solution")),
                )
                cursor.execute("DELETE FROM problem_links WHERE problem_id = %s", (problem["problem_id"],))
                cursor.executemany(
                    "INSERT INTO problem_links (problem_id, link_type, url) VALUES (%s, %s, %s)",
                    [(problem["problem_id"], kind, url) for kind, url in (problem.get("links") or {}).items()],
                )
                cursor.execute("DELETE FROM problem_topics WHERE problem_id = %s", (problem["problem_id"],))
                cursor.executemany(
                    "INSERT INTO problem_topics (problem_id, topic) VALUES (%s, %s)",
                    [(problem["problem_id"], topic) for topic in dict.fromkeys(problem.get("topics") or [])],
                )

            sheet_ids = [sheet["sheet_id"] for sheet in dataset["sheets"]]
            if sheet_ids:
                cursor.execute("UPDATE sheets SET is_enabled = FALSE, updated_at = now() WHERE NOT (sheet_id = ANY(%s))", (sheet_ids,))
            cursor.execute("UPDATE sheet_problems SET is_active = FALSE")
            cursor.execute("UPDATE sheet_problem_occurrences SET is_active = FALSE")

            for sheet in dataset["sheets"]:
                cursor.execute(
                    """INSERT INTO sheets (sheet_id, name, is_enabled, display_order, updated_at)
                       VALUES (%s, %s, %s, %s, now())
                       ON CONFLICT (sheet_id) DO UPDATE SET name = EXCLUDED.name,
                         is_enabled = EXCLUDED.is_enabled, display_order = EXCLUDED.display_order,
                         updated_at = now()""",
                    (sheet["sheet_id"], sheet["name"], sheet["enabled"], sheet.get("order", 9999)),
                )

            for membership in dataset["sheet_problems"]:
                pair = (membership["sheet_id"], membership["problem_id"])
                cursor.execute(
                    """INSERT INTO sheet_problems (sheet_id, problem_id, is_active) VALUES (%s, %s, TRUE)
                       ON CONFLICT (sheet_id, problem_id) DO UPDATE SET is_active = TRUE""",
                    pair,
                )
                cursor.execute(
                    """INSERT INTO sheet_problem_occurrences
                         (sheet_id, problem_id, position, section, subsection, is_active)
                       VALUES (%s, %s, %s, %s, %s, TRUE)
                       ON CONFLICT (sheet_id, problem_id, position, section, subsection)
                       DO UPDATE SET is_active = TRUE""",
                    (membership["sheet_id"], membership["problem_id"], membership.get("position"),
                     membership.get("section"), membership.get("subsection")),
                )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true", help="Apply the import to DATABASE_URL")
    args = parser.parse_args()
    dataset = load_and_validate()
    print(f"Input is valid: {len(dataset['problems'])} problems, {len(dataset['sheets'])} sheets, "
          f"{len(dataset['sheet_problems'])} membership occurrences")
    print(f"Published sheets: {sum(bool(s['enabled']) for s in dataset['sheets'])}; "
          f"disabled sheets retained: {sum(not s['enabled'] for s in dataset['sheets'])}")
    if not args.apply:
        print("No database connection made. Pass --apply with DATABASE_URL to import.")
        return
    database_url = os.environ.get("DATABASE_URL")
    if not database_url:
        raise SystemExit("DATABASE_URL must be set when using --apply")
    apply_dataset(dataset, database_url)
    print("Import committed successfully.")


if __name__ == "__main__":
    try:
        main()
    except (OSError, ValueError, RuntimeError, json.JSONDecodeError) as error:
        print(f"Import failed: {error}", file=sys.stderr)
        raise SystemExit(1)
