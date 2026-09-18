# Database Seed Data and API Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Seed every database table and business status with at least one consistent record, align the order schema with the Java/API contract, and verify the portal, admin, and monitor clients against the backend.

**Architecture:** Keep the existing MySQL bootstrap path and add an idempotent seed script loaded after the schema. Use fixed IDs and deterministic timestamps so relational references and repeated local runs are stable. Validate contracts with focused backend tests and a real HTTP smoke flow; repair only confirmed frontend/backend mismatches.

**Tech Stack:** MySQL 8, Spring Boot 3.2, MyBatis-Plus, JUnit/Spring test tooling already present, Vue 3/Vite, Axios.

**Spec:** Confirmed in chat on 2026-09-18.

## Global Constraints

- Preserve existing endpoint paths and response envelope (`code`, `message`, `data`).
- Seed data must be safe to run repeatedly and must satisfy all foreign-key-like references even though the current schema does not declare FKs.
- Do not add new runtime dependencies when existing Maven/npm dependencies are sufficient.
- Every new non-trivial behavior gets a focused runnable test or smoke check.

---

### Task 1: Align schema and add deterministic seed data

**Files:**
- Modify: `docs/sql/schema.sql`
- Create: `docs/sql/seed.sql`
- Modify: `docker-compose.yml`

- [ ] Add `seckill_price` and `quantity` columns to `t_order` so the table matches `Order` and `OrderVO`.
- [ ] Create idempotent seed data for users, products (on-sale and off-shelf), activities (not-started, running, ended, cancelled), seckill logs (pending/success/failure/cancelled), orders (pending/paid/cancelled/refunded), and payments (pending/success/failure/refunded).
- [ ] Use fixed IDs and `INSERT ... ON DUPLICATE KEY UPDATE` (or delete/reinsert only the fixed fixture IDs) so re-running the script is deterministic and preserves unrelated local data.
- [ ] Mount `seed.sql` as a second MySQL init script after `schema.sql`.
- [ ] Run the SQL against a local MySQL container and query each table/status to verify at least one row per required case.

### Task 2: Add backend contract and service tests

**Files:**
- Create: `backend/seckill-user/src/test/java/com/reditickets/user/UserApiContractTest.java`
- Create: `backend/seckill-product/src/test/java/com/reditickets/product/ProductApiContractTest.java`
- Create: `backend/seckill-activity/src/test/java/com/reditickets/activity/ActivityApiContractTest.java`
- Create: `backend/seckill-order/src/test/java/com/reditickets/order/OrderPaymentContractTest.java`
- Create: `backend/seckill-seckill/src/test/java/com/reditickets/seckill/SeckillStatusContractTest.java`

- [ ] Write tests first for response-envelope shape, seeded list/detail lookups, order field mapping, and the not-started/ended/repeated seckill branches.
- [ ] Run the focused tests and confirm failures identify missing test infrastructure or contract defects.
- [ ] Use existing Spring test dependencies/configuration; avoid replacing real service logic with mocks where a seeded database path is available.
- [ ] Fix only contract defects exposed by the tests, then run all backend tests and package compilation.

### Task 3: Verify and repair frontend API integration

**Files:**
- Modify only the affected files under `frontend/seckill-portal/src/api` and `frontend/seckill-portal/src/views`.
- Modify only the affected files under `frontend/seckill-admin/src` and `frontend/seckill-monitor/src`.

- [ ] Install existing lockfile dependencies for all three frontend apps.
- [ ] Build each app and record compiler/runtime errors.
- [ ] Verify Axios unwrapping against the backend `Result` envelope, auth token persistence, pagination fields, and seeded status labels.
- [ ] Repair confirmed mismatches with the smallest local change and add a lightweight client-side check where practical.

### Task 4: Run real end-to-end smoke checks

**Files:**
- Create: `docs/testing/api-integration-smoke.md`

- [ ] Start MySQL, Redis, RocketMQ, backend, and frontend services using the repository's documented path or the smallest available local subset.
- [ ] Exercise register/login, product list/detail, activity list/detail, BFF detail, seckill pending/result, order list/detail, and payment status endpoints with seeded IDs.
- [ ] Exercise portal, admin, and monitor entry points through their built assets or dev servers.
- [ ] Record command, URL, expected response, actual response, and any environment limitations in the smoke report.
- [ ] Re-run backend tests and all frontend builds after final fixes; report evidence and remaining external-environment gaps.
