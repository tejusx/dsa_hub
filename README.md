# DSA Hub

> A centralized platform to practice, track, and manage progress across popular DSA problem sheets.

DSA Hub brings multiple well-known DSA sheets into one place, while maintaining a **canonical problem system** so that the same problem appearing across different sheets can share progress.

The project is currently under active development.

---

## 🚀 What is DSA Hub?

When preparing for DSA, it is common to follow multiple problem sheets such as Striver A2Z, NeetCode 150, Blind 75, Love Babbar 450, and LeetCode lists.

The problem is that the same problem can appear in multiple sheets, making progress tracking messy.

DSA Hub aims to solve this by:

- Bringing multiple DSA sheets into one platform
- Maintaining a canonical identity for each problem
- Tracking problem-solving progress
- Synchronizing progress across sheets
- Providing direct links to external problem platforms
- Preserving the original organization of each sheet
- Supporting guest progress using browser storage
- Providing user accounts for persistent progress
- Providing a unified search experience

---

## 📚 Supported Sheets

Currently integrated sheets include:

| Sheet | Problems |
|---|---:|
| Striver A2Z | 402 |
| NeetCode 150 | 150 |
| Blind 75 | 75 |
| Love Babbar 450 | 435 |
| LeetCode 75 | 75 |
| LeetCode Top 150 | 150 |

> Problem counts represent the currently imported source data and may change as the data pipeline is refined.

---

## ✨ Key Features

### Canonical Problem System

A problem is represented by a canonical problem ID rather than treating every sheet occurrence as a completely separate problem.

For example, if the same LeetCode problem appears in multiple sheets:

```text
Sheet A
    └── Two Sum ─────────┐
                         │
Sheet B                  ├── Canonical Problem: Two Sum
    └── Two Sum ─────────┘
```

This allows progress to be shared across sheets.

---

### 🔄 Cross-Sheet Progress

By default, solving a canonical problem can reflect its solved state across the sheets where that problem appears.

The application also supports sheet-specific progress through the existing progress architecture.

---

### 📊 Progress Tracking

Track:

- Overall solved problems
- Sheet progress
- Section progress
- Subsection progress
- Remaining problems

Progress is based on canonical problems rather than simply counting every occurrence as a separate problem.

---

### 🔗 External Platform Links

Problems can contain links to external platforms such as:

- LeetCode
- GeeksforGeeks
- Take U Forward
- NeetCode
- YouTube

The problem title opens the internal problem page, while platform links open the corresponding external resource.

---

### 👤 Authentication

Registered users can maintain persistent progress.

The project includes:

- Registration
- Login
- JWT-based authentication
- User settings
- Persistent progress

---

### 👻 Guest Mode

Users can start using the platform without creating an account.

Guest progress is stored locally and can later be migrated to a registered account.

---

## 🏗️ Architecture

The project is divided into three major parts:

```text
                    DSA Hub
                       │
        ┌──────────────┴──────────────┐
        │                             │
     Frontend                      Backend
        │                             │
     React + Vite              Spring Boot
        │                             │
        └──────────────┬──────────────┘
                       │
                   PostgreSQL
```

There is also a separate data-processing pipeline responsible for converting raw sheet data into normalized canonical problems and sheet memberships.

---

## 🛠️ Tech Stack

### Frontend

- React
- Vite
- JavaScript
- CSS

### Backend

- Java
- Spring Boot
- Spring Security
- JWT
- Maven

### Database

- PostgreSQL

### Data Processing

- Python
- JSON-based source datasets
- Normalization and matching scripts

---

## 📁 Project Structure

```text
DSA Hub/
│
├── data/
│   ├── raw/
│   ├── processed/
│   └── config/
│
├── database/
│   ├── schema.sql
│   └── README.md
│
├── docs/
│   ├── architecture.md
│   ├── database_schema.md
│   └── guest-progress-migration.md
│
├── scripts/
│   ├── normalize.py
│   ├── match_problems.py
│   ├── validate.py
│   └── import_database.py
│
├── backend/
│   └── Spring Boot application
│
└── frontend/
    └── React + Vite application
```

---

## 🔄 Data Pipeline

The DSA sheets are processed through a normalization pipeline before being imported into PostgreSQL.

```text
Raw Sheet Data
      ↓
Normalization
      ↓
Problem Matching
      ↓
Canonical Problem IDs
      ↓
Validation
      ↓
Database Import
      ↓
Spring Boot API
      ↓
React Frontend
```

The pipeline is designed to prevent duplicate canonical problems while preserving sheet-specific occurrences and ordering.

---

## 🗄️ Database Design

The database separates canonical problems from their appearance inside individual sheets.

Important concepts include:

```text
problems
    ↓
Canonical problem

sheets
    ↓
DSA sheet

sheet_problems
    ↓
Sheet ↔ Problem relationship

sheet_problem_occurrences
    ↓
Exact position/order within a sheet

user_problem_progress
    ↓
Global canonical progress

user_sheet_problem_progress
    ↓
Sheet-specific progress

user_problem_attempts
    ↓
Attempt history
```

This allows the application to distinguish between:

- A unique problem
- A problem appearing in multiple sheets
- Global progress
- Sheet-specific progress
- Individual attempts

---

## 🔐 Security

Sensitive configuration is kept outside the repository.

Environment variables are used for values such as:

```text
DB_PASSWORD
JWT_SECRET
```

Secrets should never be committed to GitHub.

---

## 💻 Running Locally

### Prerequisites

Install:

- Java
- Maven
- Node.js
- npm
- PostgreSQL
- Python

---

### 1. Clone the repository

```bash
git clone https://github.com/t3jax/dsa_hub.git
cd dsa_hub
```

---

### 2. Setup PostgreSQL

Create the required database:

```text
dsa_hub
```

Then configure the required database credentials through environment variables.

---

### 3. Start the Backend

```bash
cd backend
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

---

### 4. Start the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run dev
```

The frontend will be available through the Vite development server.

---

## 🧪 Validation

Frontend:

```bash
cd frontend
npm run lint
npm run build
```

Backend:

```bash
cd backend
mvn test
```

Data validation:

```bash
python scripts/validate.py
```

Database import:

```bash
python scripts/import_database.py --apply
```

---

## 🗺️ Roadmap

The project is actively being developed.

Planned/improving areas include:

- [ ] Complete source-based hierarchy for every sheet
- [ ] Improved sheet navigation
- [ ] Advanced problem search and filtering
- [ ] Improved dashboard analytics
- [ ] Better progress visualization
- [ ] More DSA sheets
- [ ] Improved mobile experience
- [ ] Better problem discovery
- [ ] More detailed user statistics
- [ ] Improved study workflows

---

## 🎯 Project Goals

The long-term goal of DSA Hub is to make DSA preparation easier by combining multiple resources into a single structured workspace.

Instead of maintaining separate progress across different sheets, users should be able to:

```text
Choose a sheet
      ↓
Find a problem
      ↓
Solve it
      ↓
Track progress
      ↓
See that progress reflected wherever the same problem appears
```

---

## 📌 Current Status

**Active Development**

The core data pipeline, database architecture, backend APIs, authentication foundation, and frontend foundation are implemented.

The UI, sheet hierarchy, navigation, and overall user experience are still being actively refined.

---

## 🤝 Contributing

This project is currently being developed as a personal project.

Suggestions, issues, and contributions are welcome as the project evolves.

---

## 📄 License

License information will be added as the project approaches a stable release.
