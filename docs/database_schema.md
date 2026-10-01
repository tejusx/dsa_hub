# DSA Hub database schema

## Recommendation

Use PostgreSQL 15 or newer for production. The data has canonical problems, normalized links and topics, ordered many-to-many sheet membership, users, and user progress. PostgreSQL constraints and transactions fit these relationships and make imports reliable. The application should query PostgreSQL through its future backend; processed JSON remains an import artifact and must not be a runtime data store.

The application schema is defined in [`../database/schema.sql`](../database/schema.sql). It contains no seeded problem rows and no fake users or progress. `scripts/import_database.py` loads the generated dataset.

## Actual import dataset

`data/processed/dsa_hub_final.json` is an object with `version`, `problems`, `sheets`, and `sheet_problems` arrays. A problem has `problem_id`, `title`, nullable `leetcode_id`/`leetcode_slug`, `difficulty`, `topics` (strings), `links` (link-type to URL), `sources`, `source_ids`, `sheets`, `sections`, and `metadata`. IDs for LeetCode problems are `lc-{leetcode_id}`; non-LeetCode IDs are deterministic `p-{title-slug}-{sha256-prefix}` values.

Sheets contain `sheet_id`, `name`, `enabled`, `order`, and a generated `problem_count`. Membership occurrences contain `sheet_id`, `problem_id`, nullable integer `position`, nullable `section`, and nullable `subsection`. The final file currently has eight link types: `leetcode`, `gfg`, `coding_ninjas`, `youtube`, `takeuforward`, `takeuforward_plus`, `editorial`, and `neetcode`. Difficulty values are `Easy`, `Medium`, and `Hard`.

## Tables and relationships

- **users** stores account identity and the authentication hash that a later authentication feature will manage. Email has a case-insensitive unique index.
- **user_settings** is one-to-one with users. The setting `sync_progress_across_sheets` is `TRUE` by default. A database trigger creates the settings row for every new user.
- **problems** stores one canonical problem per stable `problem_id`. LeetCode IDs and slugs are unique when present. The database checks that a row with a LeetCode ID uses `lc-{leetcode_id}`. Difficulty is nullable and constrained to the three imported values. The three LeetCode capability flags are retained as typed columns.
- **problem_links** stores zero or more typed URLs for a problem, with one URL per `(problem_id, link_type)`.
- **problem_topics** stores zero or more canonical topic strings per problem.
- **sheets** stores every imported sheet, including disabled sheets. `is_enabled` controls publication; `display_order` comes from the processed sheet order.
- **sheet_problems** stores each distinct `(sheet_id, problem_id)` pair once. It is also the foreign-key target for sheet-specific progress.
- **sheet_problem_occurrences** stores every membership occurrence and its position/section/subsection. Multiple rows can exist for one pair when the source places the problem in multiple sections. PostgreSQL 15's `UNIQUE NULLS NOT DISTINCT` constraint prevents only exact duplicate occurrences while allowing legitimate repeated placements.
- **user_problem_progress** stores shared progress with primary key `(user_id, problem_id)`.
- **user_sheet_problem_progress** stores independent progress with primary key `(user_id, sheet_id, problem_id)` and a foreign key to the sheet/problem pair.
- **user_problem_attempts** stores an append-only attempt history separately from the latest progress summary. Its optional sheet must be a sheet containing the problem.

The main relationships are:

```text
users 1 ── 1 user_settings
users 1 ── many user_problem_progress many ── 1 problems
users 1 ── many user_sheet_problem_progress many ── 1 sheet_problems
problems 1 ── many problem_links
problems 1 ── many problem_topics
problems many ── many sheets (through sheet_problems)
sheet_problems 1 ── many sheet_problem_occurrences
users 1 ── many user_problem_attempts many ── 1 problems
```

## What belongs in the application database

Import canonical title/ID, LeetCode ID/slug, difficulty, curated links, topics, sheet publication/order, and membership occurrences. LeetCode's `paid_only`, `has_solution`, and `has_video_solution` flags are mapped to typed problem columns.

Do not copy pipeline bookkeeping (`sources`, `source_ids`, source record indices), duplicate master `sheets`/`sections` summaries, generated counts, JSON wrapper versions, or raw source configuration into operational tables. These can be recomputed from source/import data or retained in the repository for provenance. The importer does not copy arbitrary `metadata` blobs or unverified URLs into canonical links.

Some raw information is not present in the final dataset: `problemSiteData.json` has language availability/code fields that normalization currently omits; book-set descriptions/tags are not retained as per-sheet descriptions in the final sheet records; BooleanStack has raw sheet tags/flags and textual hints beyond the subset placed in master metadata. These could support later solution-language, sheet-description, or company/hint features, but should first be deliberately added to the processed contract rather than read from raw files at runtime. A2Z's `company_tags` field is empty for the inspected question records.

## Progress synchronization behavior

The default setting is `sync_progress_across_sheets = TRUE`.

1. With synchronization on, read and upsert `user_problem_progress` by `(user_id, problem_id)`. To display a problem in any sheet, join through `sheet_problems` and use that one shared progress row. For example, solving `lc-1` from Striver makes `lc-1` solved in NeetCode, Blind 75, and LeetCode 75 automatically; no per-sheet copies are needed.
2. With synchronization off, read and upsert `user_sheet_problem_progress` by `(user_id, sheet_id, problem_id)`. The same canonical `problems` row is reused; only the user's progress differs by sheet.
3. The backend checks the setting for every progress read/write and uses the corresponding table. Sheet-specific writes must first verify the `(sheet_id, problem_id)` pair is active.
4. Keep both progress scopes when the setting changes so turning synchronization off does not destroy a user's prior per-sheet history. When turning it on, do not silently discard scoped rows; shared reads use the canonical row. A later product decision can define an explicit reconciliation action (for example, merge solved state and preserve notes) if desired.

`solved_at` is required when `solved = TRUE`. The summary tables already provide notes, bookmarks, revision status, user difficulty, and last attempted time. Individual submissions belong in `user_problem_attempts`, not duplicate problem rows.

## Idempotent import flow

Run `python scripts/import_database.py` for offline structural checks and counts. This makes no database connection. To apply, install psycopg v3, set `DATABASE_URL`, then run `python scripts/import_database.py --apply`.

The importer validates references and ID formats, applies the schema in a transaction, and upserts problems, sheets, distinct memberships, and occurrence rows. It replaces imported link/topic rows for each imported problem, updates current membership occurrences, and marks stale occurrences/pairs inactive rather than deleting them. It imports disabled sheets but leaves them disabled; application sheet queries must filter `sheets.is_enabled = TRUE`. Data absent from the current import is not deleted, preserving historical user progress. Re-running the same import is idempotent.

Important indexes cover stable/LeetCode identifiers, title and difficulty lookup, topic/link lookup, enabled sheet ordering, membership joins/ordering, user progress lookup, bookmarks, and attempt history. No current problem data is hardcoded in SQL.
