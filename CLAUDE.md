# CLAUDE.md

Guidance for AI assistants (and contributors) working in this repository.

## Commit messages & PRs

- **NEVER** mention Claude, Anthropic, or add `Co-Authored-By` / co-authoring trailers in commit messages or PR bodies. No AI attribution in git history.
- Follow conventional commits: `feat(scope): ...`, `fix(scope): ...`, `chore: ...`, `docs: ...`, etc.
- Keep the subject line short and imperative. Add a body only when it conveys real information.
- Commit or push only when explicitly asked.

## Project overview

Compliance engine — a multi-stack application:

- **Frontend:** Angular (`src/`, `angular.json`, `package.json`).
- **Backend (JVM):** Spring/Java via Maven (`pom.xml`, `mvnw.cmd`, `src/`).
- **AI / OCR pipeline:** Python — FastAPI chat backend, OCR text extraction, and a Qdrant-backed knowledge base (`api/`, `knowledge_base/`, `requirements.txt`, `step1.py`–`step4.py`).

## Common commands

```sh
# Angular frontend
npm install
npm start            # dev server

# Python / OCR / API (use the project venv)
./venv/Scripts/python.exe -m pip install -r requirements.txt

# Java backend
./mvnw.cmd <goal>
```

## Conventions

- Environment is Windows + PowerShell; prefer PowerShell syntax for shell tasks.
- Do not commit virtualenvs (`venv/`, `venv38/`, `venv_rapid/`) — they are gitignored.
- Secrets live in `.env`; never commit real credentials.
- **Save all source files as UTF-8 without a BOM, LF line endings.** The repo
  `.editorconfig` sets this automatically (install your editor's EditorConfig
  support). Saving as Windows-1252 / ANSI produces "mojibake" (garbled `──`,
  `—`, `✓`) and has corrupted the workspace component repeatedly.

## Pre-commit hooks

This repo uses the [pre-commit](https://pre-commit.com) framework to block
mojibake/BOMs and other common issues before they land. Set it up once per clone:

```sh
./venv/Scripts/python.exe -m pip install -r requirements-dev.txt
./venv/Scripts/python.exe -m pre_commit install   # installs the commit + push hooks
```

- Config: `.pre-commit-config.yaml`. The custom mojibake/BOM guard lives in
  `scripts/check_encoding.py`; the rest are standard hooks from
  `pre-commit/pre-commit-hooks` (large files, merge markers, BOM, YAML/JSON, …).
- Run on demand: `pre_commit run --all-files` (or `run no-mojibake --all-files`).
- CI (`.github/workflows/pre-commit.yml`) re-runs the hooks on every PR/push, so
  the checks can't be skipped by not installing the local hook.
