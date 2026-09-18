# API Integration Smoke Test

Run after MySQL, Redis, RocketMQ, and the unified backend are available:

```powershell
pwsh -File docs/testing/api-integration-smoke.ps1
```

Fixture credentials are `demo_user` / `password`. Fixed IDs are defined in `docs/sql/seed.sql`.

The script verifies login, product list/detail, activity list/detail, BFF seckill-page, authenticated order list/detail, and a seeded seckill-result lookup. It fails on a non-200 response or a missing login token.

Frontend builds:

```powershell
Push-Location frontend/seckill-portal; npm ci; npm run build; Pop-Location
Push-Location frontend/seckill-admin; npm ci; npm run build; Pop-Location
Push-Location frontend/seckill-monitor; npm ci; npm run build; Pop-Location
```
