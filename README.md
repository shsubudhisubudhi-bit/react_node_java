# Java Developer Test – submission

**New here?** Open **`START_HERE.txt`** — one-page Docker instructions (no Java/Node install needed if you use Docker).

---

Hi — this is the full stack for the test: **React (5173) → Node gateway (3000) → Spring Boot (8080)**.  
The UI talks to Node only; Node forwards everything to Java. Java holds the data, validation, logging, and the extra bits (persistence, metrics, rate limit, optional API keys).

You need **Java 11+**, **Maven**, **Node 16+**, and ports **8080**, **3000**, **5173** free — **unless** you use Docker below.

---

## Run with Docker (one command — no local JDK/Node needed)

Install **[Docker Desktop](https://www.docker.com/products/docker-desktop/)** (Windows / Mac / Linux). Then from the **project root** (folder that contains `docker-compose.yml`):

```bash
docker compose up --build -d
```

Wait until all three containers are up (first build can take a few minutes). Then open:

- **UI:** http://localhost:5173  
- **Gateway:** http://localhost:3000/health  
- **Java:** http://localhost:8080/health  

Stop everything:

```bash
docker compose down
```

Data written by Java is stored in the Docker volume `java-data` (survives restarts). To wipe data: `docker compose down -v`.

**Windows:** you can double-click **`docker-up.bat`** in the project root (same as `docker compose up --build -d`). **`docker-down.bat`** stops the stack.

More detail: **`DOCKER.md`**.

---

## Run it (three terminals — without Docker)

Unzip, `cd` into the project root, then:

**1 – Java**
```
cd java-backend
mvn spring-boot:run
```
Wait until you see something like `Started Application` — server is on **8080**.

**2 – Node**
```
cd node-backend
npm install
npm start
```
You should see it listening on **3000**.

**3 – React**
```
cd react-frontend
npm install
npm run dev
```
Open **http://localhost:5173** in the browser.

First time on each of Node/React folders, `npm install` is required. Java only needs Maven to download deps on first `mvn spring-boot:run`.

---

## Quick sanity check

Hit the gateway health (this checks Node **and** that Node can reach Java):

```
curl http://localhost:3000/health
```

You want `goBackend.status` to be `ok`. If that’s there, the chain is fine.

---

## Postman

Import **postman-collection.json** (File → Import in Postman). Requests are grouped by folder — start with “PHASE 1” and work down.  
Base URLs use variables `{{node}}` and `{{java}}` — they’re set on the collection to `http://localhost:3000` and `http://localhost:8080`.

**Note:** For **validation errors** (400s), the gateway sometimes returns a short `{"error":"..."}` while Java returns the full body with `details` and `timestamp`. If you need to show field-level errors, call **Java directly** on 8080 for that request, or it’s still a valid 400 either way.

---

## PowerShell / automated check

From the project root (with all three services up):

```
powershell -ExecutionPolicy Bypass -File .\test-suite.ps1
```

You should see `PASS: 40` and `FAIL: 0` at the end. That script hits both Node and Java.

**COMMANDS.txt** has the same flows as copy-paste commands if you prefer the terminal over Postman.

---

## What’s implemented (mapping to the spec)

Core (required): create user (`POST /api/users`), create task (`POST /api/tasks`), update task (`PUT /api/tasks/{id}`), request logging in Java.  
Extras: JSON persistence under `java-backend/data/data.json` (created after first writes), bean validation, detailed health `GET /health/detailed` (Java), metrics `GET /api/metrics` (Java), rate limiting, optional API key auth.

Auth is **off** by default (`auth.enabled=false` in `application.properties`). Turn it on only if you want to demo keys — then send header `X-API-Key` with one of the keys listed in that file.

---

## Config you might touch

**Java** – `java-backend/src/main/resources/application.properties`  
Port, data file path, auth toggle, rate limit.

**Node** – `node-backend/.env`  
`GO_BACKEND_URL` must match wherever Java runs (default `http://localhost:8080`).

If **8080 is busy**, pick another port in both places (e.g. 8081 in `server.port` and the same in `GO_BACKEND_URL`), restart Java then Node.

---

## Files worth opening

| File | What it’s for |
|------|----------------|
| `docker-compose.yml` | Run full stack with Docker |
| `DOCKER.md` | Docker details / troubleshooting |
| `docker-up.bat` / `docker-down.bat` | Windows one-click start/stop |
| `TEST_REQUIREMENTS.md` | Original brief |
| `TESTING_GUIDE.md` | Longer manual test walkthrough |
| `COMMANDS.txt` | Terminal copy-paste |
| `postman-collection.json` | Postman import |
| `test-suite.ps1` | One-shot automated checks |

---

## Project layout (short)

- `java-backend/` – Spring Boot app (`controller`, `service`, `config`, etc.)
- `node-backend/` – Express proxy
- `react-frontend/` – Vite + React UI

That’s it. If something doesn’t start, check ports and that Java is up before Node. Good luck with the review.
# react_node_java
