# Docker Commands - Local Development

> Quick reference for managing the local PostgreSQL database with Docker.

## Prerequisites

- Docker Desktop running
- Terminal in project root (`the-replicant/`)

---

## Common Commands

### Start Database

```powershell
docker-compose up -d
```


### Stop Database

```powershell
docker-compose down
```

### Check Status

```powershell
docker ps
```

### View Logs

```powershell
# Follow logs in real-time
docker logs -f thereplicant-db

# Last 50 lines only
docker logs --tail 50 thereplicant-db
```

---

## Database Access

### Connect with psql

```powershell
docker exec -it thereplicant-db psql -U replicant -d replicant_db
```

### Useful psql Commands

| Command | Description |
|---------|-------------|
| `\dt` | List all tables |
| `\d table_name` | Describe table structure |
| `\l` | List all databases |
| `\q` | Exit psql |

---

### Check connection backend

http://localhost:8080/actuator/health

http://localhost:8080/swagger-ui.html

---

## Troubleshooting

### Reset Database (Delete All Data)

```powershell
docker-compose down -v
docker-compose up -d
```

> ⚠️ The `-v` flag removes the volume, **deleting all data**.

### Rebuild Container

```powershell
docker-compose down
docker-compose up -d --build
```

### Check Container Health

```powershell
docker inspect --format='{{.State.Health.Status}}' thereplicant-db
```

---

## Environment Variables

These are the default development values (from `.env.example`):

| Variable | Default Value |
|----------|---------------|
| `POSTGRES_USER` | replicant |
| `POSTGRES_PASSWORD` | replicant123 |
| `POSTGRES_DB` | replicant_db |
| `POSTGRES_PORT` | 5432 |

> **Note**: These are **local development credentials only**. Production credentials should never be committed to version control.

---

## Quick Verification

After starting Docker, verify with:

```powershell
# 1. Check container is running
docker ps | findstr thereplicant

# 2. Test database connection
docker exec thereplicant-db pg_isready -U replicant -d replicant_db
```

Expected output: `replicant_db - accepting connections`

---

**Last Updated**: February 2026
