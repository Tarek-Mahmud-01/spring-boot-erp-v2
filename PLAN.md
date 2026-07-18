# Guru ERP v2 — Build Plan (latest)

**Last updated:** 2026-07-18
**Companion doc:** see [ARCHITECTURE.md](ARCHITECTURE.md) for the full technical architecture (source of truth).

---

## Goal

Full ground-up rewrite of `guru-erp-revamp` (a mature ~90%-complete single-tenant ERP: 855 commits,
~100 tables, POS + double-entry GL + procurement + CRM + inventory all built) from **React + FastAPI + PG**
→ **React + Java Spring Boot (Gradle) + PG**.

- Rewrite **both** frontend and backend.
- **Concept/feature-driven, NOT a structure clone** — carry the data model + business rules, build the
  structure fresh and clean.
- The old app stays as **read-only reference + spec** (`CLAUDE.md`, `PRD.md`, `TECH_STACK.md`, `DESIGNS.md`).

---

## Confirmed decisions

| # | Decision |
|---|---|
| 1 | Full rewrite of BOTH frontend + backend, concept-driven (not a structure clone) |
| 2 | **New separate git repo** at `H:\wamp64\www\guru-erp-v2`; old repo = reference; same DB for parity |
| 3 | **Everything project-local on H:, nothing global** (own JDK 21, Gradle wrapper, project Postgres) |
| 4 | **Custom non-default ports:** PG **55432** · Redis **56379** · backend **58080** · Vite **53000** |
| 5 | Backend: **Spring Boot 3, Java 21, Gradle Kotlin DSL, QueryDSL, MapStruct, Flyway, Testcontainers** |
| 6 | JDK 21 via **Gradle toolchains auto-provision** (foojay resolver) — nothing global |
| 7 | Postgres: **project-local Docker** (postgres:16) on 55432; `pg_dump` from the container |
| 8 | **Data layer: `createAsyncThunk` + `createSlice` + Axios (PRIMARY); RTK Query = master data ONLY** |
| 9 | Frontend: React 18 + Vite + TS, RHF + zod, Tailwind + Radix, react-i18next, WebSocket/SSE, Vitest/Playwright/MSW |
| 10 | **Pilot module: `settings + access`** (masters + RBAC) end-to-end first |
| 11 | **Small feature-wise files, no long pages/classes** (caps below) |
| 12 | **Persistent layout** (sidebar loads once) + **lazy per-page/tab/modal loading** (load only what's needed) |
| 13 | **All business logic on the backend**; frontend renders UI only |

---

## File-size & readability caps (hard rule)

- **Frontend:** page = composition only ≤150 lines (cap 200); component ≤120; hook/util ≤150.
  No logic in components — data via thunk+selector (master via RTK Query hook), forms via RHF+zod.
- **Backend:** controller ≤150; service class ≤~250 (split by use-case when it grows); DTOs as records; MapStruct mappers.
- Small files, clear names, minimum code / maximum reuse (compose from `shared/` / `platform/`).

---

## Execution phases

**Phase 0 — Foundations (in progress)**
- Backend: Spring Boot skeleton (Kotlin DSL) + `platform/` (BaseEntity, Money, ULID, audit hash-chain,
  RFC-7807, Spring Security JWT + `@PreAuthorize`, outbox, status machine, request-id/logging) + Flyway.
- Frontend: `app/` + `shared/` foundation (axios http + interceptors, thunk/slice conventions,
  masterDataApi RTK-Query-master-only, AppShell persistent layout, lazy router, DataTable, RHF+zod form, i18n, auth).
- Infra: project-local `docker-compose.yml` (Postgres 55432 + Redis 56379).

**Phase 1 — Pilot: `settings + access` end-to-end**
Company, location, fiscal period, numbering, tax/currency masters + users, roles, permissions (RBAC).
Chosen first because everything depends on it and it proves security + audit + CRUD-list patterns.
Delivers the canonical backend module slice + frontend feature pattern. Parity-diff vs old app.

**Phase 2 — Rollout** (dependency order):
`settings + access` → `product` → `inventory` → `procurement` → `sales` → `pos` → `crm` →
`finance/GL` (last, most careful) → `hr` → `notification` (new) → `reporting`.

**Phase 3 — Cross-cutting & cutover:** notifications, approvals, reporting on read-replica; full parity pass; retire FastAPI.

---

## Current build status (2026-07-18)

| Item | Status |
|---|---|
| `ARCHITECTURE.md` | ✅ written |
| `PLAN.md` (this file) | ✅ written |
| `docker-compose.yml` (PG 55432 + Redis 56379) | ⬜ TODO |
| Frontend foundation | ⚠️ Partial — correct deps landed (axios, RTK, RHF+zod, radix, i18n) but wrong structure (flat `theme/`, not `app/ modules/ shared/`) and npm instead of pnpm → **rebuild to spec** |
| Backend foundation | ⚠️ Gradle skeleton only (`build.gradle.kts` + wrapper); `src/main/java` empty → **build `platform/`** |
| Flyway `V1__baseline.sql` (full ~100-table schema) | ⬜ TODO — `pg_dump --schema-only` from the reference DB |
| Pilot: settings + access | ⬜ TODO |
| Parity harness vs old FastAPI | ⬜ TODO |

**Note:** the three Phase-0 scaffold agents were interrupted when the previous process exited; partial work
is on disk (see status above). Next step is to rebuild the foundations cleanly against `ARCHITECTURE.md`.

---

## Toolchain / host notes

- Host has only **JDK 14** (global) + Android Studio **JBR** as JAVA_HOME — neither is used directly.
  JDK 21 is provisioned project-locally by Gradle toolchains. The JBR can *bootstrap* the Gradle wrapper
  (inline JAVA_HOME for that one command only), which then downloads JDK 21 for compilation.
- **Node 22 + pnpm 11** available (frontend uses pnpm).
- **Docker 29** available (project-local Postgres + Redis).
- No PostgreSQL client on host (WAMP has MySQL only) → `pg_dump` comes from the Docker postgres container.

---

## Verification

- **Backend:** per-module parity harness (new Spring vs old FastAPI on same DB, diff JSON); JUnit +
  Testcontainers; GL reproduces `.tmp_je.json`/`.tmp_tb.json` (balanced TB, matching voucher numbers);
  one audit row per mutation; positive+negative authorization test per endpoint.
- **Frontend:** Vitest + Playwright (+ MSW); axe-core a11y per screen; regression-test `BUG-21`
  (server pagination beyond the old 500 cap); visual diff vs old app.
