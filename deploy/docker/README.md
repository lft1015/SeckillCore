# Docker deployment

This deployment runs the unified Spring Boot launcher, MySQL 8, Redis 7, RocketMQ 5, and the three Vue frontends. The launcher has Nacos disabled by configuration, so Nacos is not required for this single-container backend deployment.

## Start

From the repository root, create the local environment file and replace the password value:

```powershell
Copy-Item .env.example .env
docker compose up --build -d
```

The database schema at `docs/sql/schema.sql` is imported automatically when the `mysql-data` volume is first created.

## Access

| Service | Address |
| --- | --- |
| Portal | `http://localhost:3000` |
| Admin | `http://localhost:3001` |
| Monitor | `http://localhost:3002` |
| Backend API | `http://localhost:8080` |

The frontend containers proxy `/api` requests to the backend, so browser requests do not require a separate API URL configuration.

## Operations

```powershell
docker compose ps
docker compose logs -f backend
docker compose down
```

`docker compose down` preserves the MySQL and Redis volumes. To initialize a completely fresh local database, remove the named volumes intentionally and start the stack again.
