# Guru ERP v2 — Full Architecture

**Stack:** React 18 + Vite + TypeScript (frontend) · Java 21 + Spring Boot 3 + Gradle Kotlin DSL (backend) · PostgreSQL 16 · Redis 7
**Model:** single-tenant modular monolith · concept/feature-driven · full rewrite of the FastAPI+React `guru-erp-revamp` app (kept as reference spec).

This document is the **single source of truth** for the v2 build. Every file we write must obey it.

---

## 0. Non-negotiable project rules

1. **Project-local on H:, nothing global.** Own JDK 21 (Gradle toolchain auto-provision), Gradle wrapper,
   project-local Postgres/Redis via this repo's own Docker Compose. No machine-wide installs, no global
   PATH/JAVA_HOME changes.
2. **Custom non-default ports** (other projects use the defaults — zero conflicts):

   | Service | Port | Notes |
   |---|---|---|
   | PostgreSQL | **55432** | docker maps 55432→5432 |
   | Redis | **56379** | docker maps 56379→6379 |
   | Spring Boot backend | **58080** | `server.port` |
   | Vite frontend dev | **53000** | `server.port`, `strictPort` |

3. **Small files, split by small feature.** No long files, no God-components/classes. Caps in §6.
4. **Backend owns all business logic.** Frontend renders UI only — no aggregation, no authoritative money math.
5. **Load code & data only when needed.** Lazy modules, persistent layout, per-page/tab/modal loading.
6. **Every mutation:** audit row + `@PreAuthorize` permission + soft delete + optimistic lock.

---

## 1. Repository layout

```
H:\wamp64\www\guru-erp-v2\
├── ARCHITECTURE.md            ← this file (source of truth)
├── docker-compose.yml         ← project-local Postgres(55432) + Redis(56379)
├── .env / .env.example
├── backend/                   ← Spring Boot (Gradle Kotlin DSL)
└── frontend/                  ← React + Vite
```
New separate git repo. The old app at `H:\wamp64\www\guru-erp-revamp` is **read-only reference** — never imported or modified.

---

## 2. Backend architecture (Spring Boot 3, Java 21, Kotlin DSL, QueryDSL)

Concept-driven modular monolith. One package per bounded context. **Vertical slices**, not layer-first.

```
com.guru.erp
├── GuruErpApplication.java
├── platform/                          # cross-cutting foundation — built ONCE, reused everywhere
│   ├── entity/BaseEntity              # @MappedSuperclass: id, publicId(ULID), status,
│   │                                  #   createdAt/By, updatedAt/By, deletedAt, @Version
│   │                                  #   @SQLRestriction("deleted_at is null") soft delete
│   ├── entity/JpaAuditingConfig       # @EnableJpaAuditing + AuditorAware (from security ctx)
│   ├── money/Money                    # @Embeddable long minor-units + char(3) + HALF_EVEN math
│   ├── id/Ulid                        # ULID generator (app-side, char(26))
│   ├── security/                      # Spring Security: stateless JWT (+TOTP later),
│   │                                  #   @EnableMethodSecurity → @PreAuthorize from permissions
│   ├── error/                         # @RestControllerAdvice → RFC-7807 ProblemDetail,
│   │                                  #   DomainException + stable error-code catalogue
│   ├── web/                           # RequestIdFilter (X-Request-ID→MDC), access log,
│   │                                  #   PageResponse<T> for server-driven pagination
│   ├── outbox/                        # transactional outbox event bus (POS→GL→Inv→CRM)
│   └── status/                        # status_master lookup + status_history + state machine
│
└── modules/                           # one package per DOMAIN CONCEPT (Epic)
    ├── settings/  access/  product/  inventory/  procurement/  sales/
    ├── pos/  crm/  finance/  hr/  notification/  reporting/
```

**Per-module vertical slice:**
```
modules/<concept>/
├── controller/   @RestController — thin: parse → @PreAuthorize → call service → return DTO   (≤150 lines)
├── service/      @Service @Transactional — ALL business rules, audit, outbox emit            (≤250 lines/class)
├── repository/   Spring Data JPA + QueryDSL for join-fetch/projection (NO N+1)
├── domain/       JPA entities extending BaseEntity
├── dto/          Java records (request/response)
└── mapper/       MapStruct entity↔DTO
```

### Backend invariants (project-wide)
- **Money** = `long` minor units + `char(3)` currency (never `double`). Tax rounding `HALF_EVEN` at line level.
- **IDs**: ULID `char(26)` public (app-generated) + `bigint` internal FK.
- **Timestamps**: UTC `timestamptz`.
- **Audit**: append-only, hash-chained `audit_logs` (sha256 prev/row hash, DB-level REVOKE UPDATE/DELETE);
  one audit row per CREATE/UPDATE/DELETE/RESTORE with before/after JSONB + actor + request-id.
- **Authorization**: `@PreAuthorize("hasAuthority('<perm.code>')")` on every endpoint; permission catalogue + roles.
- **Soft delete**: `deleted_at IS NULL` = live; hard delete only when zero references.
- **Optimistic lock**: `@Version` on every entity.
- **Errors**: RFC-7807 problem+json + stable error codes catalogued in `/docs/errors.md`.

### Database & migrations
- Keep the exact `guru_erp` schema (~100 tables). Baseline = `pg_dump --schema-only` of the reference DB
  → **Flyway `V1__baseline.sql`**; forward changes as `V2+`. Do NOT hand-translate the 150 Alembic files.
- `spring.jpa.hibernate.ddl-auto=validate` (Flyway owns the schema).
- Highest-risk ports (extra parity tests): **Finance/GL** (nested-set COA, debit-XOR-credit CHECK,
  balanced-voucher poster, `gl_posting_log` idempotency), **audit hash-chain**, **POS fulfillment+outbox**, **money/tax**.

---

## 3. Frontend architecture (React 18 + Vite + TS)

Feature-first (mirrors backend concepts), lazy-loaded, low-level reusable components.

```
frontend/src/
├── app/
│   ├── layouts/     AppShell (PERSISTENT Header/Sidebar/Breadcrumb/Footer + <Outlet/>)
│   ├── router/      lazy route registry + permission-driven route/menu generation
│   ├── store/       configureStore + global slices (auth, user, permission, theme,
│   │                language, company, notification, menu)
│   └── providers/   Redux, Router, Theme, i18n, Auth, Notification
│
├── modules/         (one self-contained, lazy-loaded module per concept)
│   └── <concept>/   pages/ · components/ · api/ (axios calls) · thunk/ (createAsyncThunk) ·
│                    slice/ (createSlice) · hooks/ · utils/ · routes/
│
└── shared/          (GLOBAL, reused everywhere)
    ├── components/  Button, Input, Select, Field, Modal, Drawer, Tabs, Card, StatusPill,
    │                Pagination, DataTable (server-driven, presentational), Can (perm gate)
    ├── hooks/       useAppDispatch, useAppSelector (typed), usePermission
    ├── services/    http.ts (axios instance + interceptors), masterDataApi.ts (RTK Query — master data only), authService
    ├── utils/       formatMoney (display only), formatDate, cn (clsx+tailwind-merge)
    ├── constants/   routePaths, permissionCodes
    └── types/       Paginated<T>, Money, ApiError, …
```
**Two-folder rule:** a module imports only from `shared/` and its own folder.

### 3.1 Data layer — THUNK-FIRST (decision)
**PRIMARY = `createAsyncThunk` + `createSlice` + a shared axios instance.**
- `createAsyncThunk` for **every business/API op**: login, all CRUD, approvals, transfers, payroll,
  workflow, notifications, dashboard refresh, upload, export/import, audit.
- `createSlice` for state: global slices + per-module feature slices (each module owns its slice).
- A thunk's `.fulfilled` may update **multiple slices** explicitly (e.g. Create Invoice → invoice +
  dashboard + customerBalance + stock + audit + notification), UI re-renders, no page refresh.
  This explicit control is *why* we avoid RTK Query for transactions.
- `shared/services/http.ts` = axios instance + interceptors (JWT, `/api` prefix, 401-refresh,
  RFC-7807 → toast). All thunks call through it.
- `condition` guard on thunks → no duplicate fetch; master data loads once.

**RTK Query = master/reference data ONLY** (country, currency, unit, tax, department, config…): load once,
cache. **Never** for employee/invoice/sales/purchase/stock/payroll/approval/dashboard-transaction data.

### 3.2 Persistent layout + lazy loading (load only what's needed)
- **AppShell mounts once**; Sidebar/Header never remount on navigation — only `<Outlet/>` content swaps.
- Every page = `React.lazy()` + dynamic `import()` in `<Suspense>` (content-area spinner). No eager pages.
- **Startup loads ONLY:** login, current user, permissions, company settings, theme, language, menu, layout.
  No module code/data at startup.
- **Permission-driven** menu + routes: unauthorized modules are never downloaded.
- **Per-page/tab/modal:** one main API on mount; tabs fetch on activation; modals lazy-import on open;
  dashboard widgets load independently (a slow one doesn't block others).
- **Master vs transaction data:** master cached once; transaction data loaded on demand, never preloaded.
- **Server-driven lists:** `?page&size&sort&filter` → joined DTO. No client-side whole-list filtering,
  no `.map().find()` joins, no `Promise.all(keys.map(fetch))` fan-outs.

### 3.3 Design system
Tailwind CSS-variable tokens (no raw hex in components). Brand primary `#714B67` (mauve), accent
`#00A09D` (teal), Inter font, 14px body, semantic status colors, dark mode via `data-theme`.
Radix primitives under `shared/components`. Lucide icons. WCAG 2.1 AA.

---

## 4. Ports, tooling, run

```
# DB + cache (project-local)
docker compose up -d              # Postgres :55432, Redis :56379

# backend  (Gradle provisions JDK 21 via toolchain; wrapper bootstraps with any host JDK)
cd backend && ./gradlew bootRun   # http://localhost:58080  (health: /actuator/health)

# frontend
cd frontend && pnpm install && pnpm dev   # http://localhost:53000
```
JDK 21 comes from **Gradle toolchains + foojay-resolver** (downloaded into Gradle's own cache) — nothing global.

---

## 5. Execution plan

- **Phase 0 — Foundations:** backend `platform/` + Flyway baseline + docker-compose; frontend `app/` + `shared/`
  (axios http, thunk/slice conventions, masterDataApi, AppShell, lazy router, DataTable, form, i18n, auth).
- **Phase 1 — Pilot:** `settings + access` end-to-end (company/location/fiscal/numbering/tax/currency masters +
  users/roles/permissions RBAC). Proves security + audit + CRUD-list patterns. Parity-diff vs old app.
- **Phase 2 — Rollout** (dependency order): settings+access → product → inventory → procurement → sales →
  pos → crm → finance/GL (last & most careful) → hr → notification (new) → reporting.
- **Phase 3 — Cross-cutting & cutover:** notifications, approvals, reporting on read-replica; full parity pass; retire FastAPI.

---

## 6. File-size & readability caps (enforced in review)

**Frontend:** page = composition only ≤150 lines (cap 200); component ≤120; hook/util ≤150. No logic in
components — data via thunk+selector (master via RTK Query hook), forms via RHF+zod, logic in hooks/utils.

**Backend:** controller ≤150; service class ≤~250 (split by use-case when it grows, e.g.
`PurchaseOrderCreate` / `PurchaseOrderTransition`); small methods; DTOs as records; MapStruct mappers.

**General:** small files, clear names, minimum code / maximum reuse (compose from `shared/`/`platform/`).

---

## 7. Verification

- **Backend:** per-module parity harness (new Spring vs old FastAPI on same DB, diff JSON); JUnit +
  Testcontainers Postgres; GL reproduces `.tmp_je.json`/`.tmp_tb.json` (balanced trial balance, matching
  voucher numbers); assert one audit row per mutation; positive+negative authorization test per endpoint.
- **Frontend:** Vitest + Playwright (+ MSW); axe-core a11y per screen; regression-test `BUG-21`
  (server pagination returns rows beyond the old 500 cap); visual diff vs old app.

---

## 8. Reference (read-only, in the old repo)

`CLAUDE.md`, `TECH_STACK.md`, `PRD.md` (§11 data model), `DESIGNS.md`, `/docs/errors.md`;
`backend/app/core/{audit,soft_delete,transactional,permissions,access}.py`, `app/outbox/`,
`app/finance/models.py` + `finance/views/` (GL), `app/products/` (cleanest slice), `app/pos/fulfillment.py`,
`app/global_utils/tax.py` (tax test vectors); `frontend/src/shared/ui/*`, `tailwind.config.ts`, `app/router.tsx`.
