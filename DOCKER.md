# Docker

Everything runs in containers: **Java → Node → React (nginx)**. Your machine only needs **Docker Desktop** (or Docker Engine + Compose plugin).

## One command

From the project root (where `docker-compose.yml` is):

```bash
docker compose up --build -d
```

- **`-d`** = background. Drop `-d` if you want logs in the terminal.

First run builds images (Maven + npm); can take 5–10 minutes. Later starts are faster.

## URLs (on your PC)

| Service | URL |
|---------|-----|
| React UI | http://localhost:5173 |
| Node gateway | http://localhost:3000 |
| Java API | http://localhost:8080 |

The React app is built with `VITE_API_URL=http://localhost:3000` so the **browser** talks to the gateway on your host (same as non-Docker setup).

Inside Docker, Node uses `GO_BACKEND_URL=http://java:8080` to reach Spring Boot on the internal network (you don’t change this for normal use).

## Stop

```bash
docker compose down
```

Remove containers **and** the Java data volume:

```bash
docker compose down -v
```

## Logs

```bash
docker compose logs -f
```

One service:

```bash
docker compose logs -f java
docker compose logs -f node
docker compose logs -f web
```

## Requirements

- Docker **Compose V2** (`docker compose`, not only `docker-compose`).
- Ports **5173**, **3000**, **8080** free on the host.

## Troubleshooting

**Port already in use** — change ports in `docker-compose.yml` under `ports:` (e.g. `"8081:8080"` for Java) and adjust Postman / `VITE_API_URL` if you change the gateway port.

**Build fails** — run `docker compose build --no-cache` once and read the error at the end.

**Java unhealthy** — `docker compose logs java` — often first start needs more time; `start_period` in compose is already set.

**Windows path with spaces** — unzip the project to a path **without spaces** if Docker build scripts misbehave.
