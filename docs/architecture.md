# DSA Hub architecture

## Responsibility flow

```text
Raw source datasets + source config
          ↓
Normalization scripts
          ↓
Canonical matching and deduplication
          ↓
Validation and quality report
          ↓
Website-ready processed JSON
          ↓
Idempotent PostgreSQL import
          ↓
Future backend API
          ↓
Future frontend
```

### Data pipeline

- `data/raw/` contains source-owned datasets. Never edit these as a way to correct generated output.
- `data/config/sources.json` controls which raw sources normalization reads. A source can be disabled without deleting its file.
- `scripts/normalize.py` maps source-specific shapes into `data/processed/normalized_problems.json`.
- `scripts/match_problems.py` matches canonical problems, creates stable IDs, resolves sheet memberships, applies enabled sheet configuration, and produces master, sheet, and final datasets.
- `data/config/sheets.json` controls names, publication state, and display order. Newly discovered sheets default to disabled.
- `scripts/validate.py` checks the generated contract and writes `data/processed/validation_report.json`.
- `data/processed/dsa_hub_final.json` is the website-ready import boundary. The frontend and backend must not parse raw datasets or depend on processed JSON at runtime.

### Import and application

- `database/schema.sql` defines the PostgreSQL application schema.
- `scripts/import_database.py` validates and imports the final JSON in a transaction. The default invocation is offline/check-only; `--apply` is explicit and requires `DATABASE_URL` and psycopg.
- PostgreSQL owns runtime problem, sheet, membership, account, settings, and user progress data after import. The backend will expose that data through an API.
- The future frontend owns presentation and user interaction. It should query the backend, never load the raw/processed source JSON as its production data store.
- Pipeline configs control source inclusion and published sheet state. Database rows preserve disabled sheets for future activation; runtime sheet listings filter for enabled sheets.

## Data identity and progress

Every canonical problem has one stable ID: `lc-{leetcode_id}` where available, otherwise a deterministic normalized-title slug and hash. Sheet membership is a separate many-to-many relation. A membership occurrence may repeat for different positions or sections, but does not duplicate the canonical problem.

User progress is independent of problem membership. By default, `user_settings.sync_progress_across_sheets` is true, and the backend stores/reads progress by `(user_id, problem_id)`. When the setting is false, it uses `(user_id, sheet_id, problem_id)`. This allows independent sheet tracking without creating duplicate problems.

## Repository structure now and later

```text
DSA Hub/
├── data/
│   ├── raw/                 # source datasets; read-only to the pipeline
│   ├── processed/           # generated files; do not hand-edit
│   └── config/              # source and published-sheet controls
├── scripts/
│   ├── normalize.py
│   ├── match_problems.py
│   ├── validate.py
│   └── import_database.py   # offline checks by default; explicit DB apply
├── database/
│   └── schema.sql
├── docs/
│   ├── architecture.md
│   └── database_schema.md
├── backend/                 # create in the backend implementation phase
└── frontend/                # create in the frontend implementation phase
```

The backend and frontend directories are intentionally not created yet. The current repository had no application code in either location, and the requested phase stops before backend/frontend implementation.

## Operational rules

1. Edit source/configuration or pipeline code, then regenerate; never hand-edit processed JSON.
2. Run normalization, matching, and validation before importing.
3. Review validation and the generated data-quality summary before applying a database import.
4. Apply imports as a transaction. Keep the processed final file available as an auditable snapshot, but do not serve it at runtime.
5. Treat stable problem IDs as database keys and never derive progress IDs from sheet position/title at request time.
