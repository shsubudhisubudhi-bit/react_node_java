# Testing guide

Use this if you want to hit endpoints by hand (curl / Postman / PowerShell).  
Start the three services first — same order as **README.md** (Java → Node → React).

**Ports:** Java **8080**, Node **3000**, UI **5173**.  
If 8080 is taken on your PC, change `server.port` in `java-backend/.../application.properties` and `GO_BACKEND_URL` in `node-backend/.env` to the **same** port and restart.

| What | URL | Folder |
|------|-----|--------|
| Spring Boot | http://localhost:8080 | `java-backend/` |
| Express gateway | http://localhost:3000 | `node-backend/` |
| React (Vite) | http://localhost:5173 | `react-frontend/` |

---

## Start Services

Open **three separate terminals** and run these commands.

### Terminal 1 – Java backend

```
cd java-backend
mvn spring-boot:run
```

Wait for `Started Application in X seconds`.

### Terminal 2 – Node backend

```
cd node-backend
npm start
```

Wait for `Node.js backend server running on http://localhost:3000`.

### Terminal 3 – React frontend

```
cd react-frontend
npm run dev
```

Wait for `Local: http://localhost:5173/`.

---

## Quick Health Check (run this first)

**PowerShell**
```powershell
Invoke-RestMethod http://localhost:3000/health
```

**CMD**
```
curl http://localhost:3000/health
```

**Expected:**
```json
{"status":"ok","message":"Node.js backend is running","goBackend":{"status":"ok","message":"Java backend is running"}}
```

If this works, the React -> Node -> Java chain is fully up.

---

# PHASE 1 – Read Endpoints (GET)

### 1.1 List all users

**PowerShell**
```powershell
Invoke-RestMethod http://localhost:3000/api/users
```

**CMD**
```
curl http://localhost:3000/api/users
```

**Expected:** JSON with `users` array and `count`.

### 1.2 Get one user by id

```powershell
Invoke-RestMethod http://localhost:3000/api/users/1
```
```
curl http://localhost:3000/api/users/1
```

### 1.3 Get one user that does not exist (should return 404)

**PowerShell**
```powershell
try { Invoke-RestMethod http://localhost:3000/api/users/999 }
catch { Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" }
```

**CMD**
```
curl -i http://localhost:3000/api/users/999
```

**Expected:** `HTTP/1.1 404 Not Found`

### 1.4 List tasks with filters

```powershell
Invoke-RestMethod "http://localhost:3000/api/tasks"
Invoke-RestMethod "http://localhost:3000/api/tasks?status=pending"
Invoke-RestMethod "http://localhost:3000/api/tasks?userId=2"
```
```
curl "http://localhost:3000/api/tasks"
curl "http://localhost:3000/api/tasks?status=pending"
curl "http://localhost:3000/api/tasks?userId=2"
```

### 1.5 Stats

```powershell
Invoke-RestMethod http://localhost:3000/api/stats
```
```
curl http://localhost:3000/api/stats
```

**Expected:** `{"users":{"total":N},"tasks":{"total":N,"pending":N,"inProgress":N,"completed":N}}`

---

# PHASE 2.1 – Create User (POST)

### 2.1.1 Create a valid user

**PowerShell**
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/users `
  -ContentType 'application/json' `
  -Body '{"name":"Demo User","email":"demo@client.com","role":"developer"}'
```

**CMD**
```
curl -X POST http://localhost:3000/api/users -H "Content-Type: application/json" -d "{\"name\":\"Demo User\",\"email\":\"demo@client.com\",\"role\":\"developer\"}"
```

**Expected:** `HTTP 201` with `{"id":N,"name":"Demo User","email":"demo@client.com","role":"developer"}`

### 2.1.2 Validation: missing fields (should return 400)

```powershell
try {
  Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/users `
    -ContentType 'application/json' -Body '{"name":"X"}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```
```
curl -i -X POST http://localhost:3000/api/users -H "Content-Type: application/json" -d "{\"name\":\"X\"}"
```

**Expected:** `HTTP 400`, body includes `"Validation failed"` and lists missing fields.

### 2.1.3 Validation: invalid email (should return 400)

```powershell
try {
  Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/users `
    -ContentType 'application/json' `
    -Body '{"name":"X","email":"not-an-email","role":"dev"}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```
```
curl -i -X POST http://localhost:3000/api/users -H "Content-Type: application/json" -d "{\"name\":\"X\",\"email\":\"not-an-email\",\"role\":\"dev\"}"
```

**Expected:** `HTTP 400`, `"email must be a valid email address"`.

### 2.1.4 Verify new user is in the list

```powershell
Invoke-RestMethod http://localhost:3000/api/users
```

The user you created in 2.1.1 must appear in the `users` array.

---

# PHASE 2.2 – Create Task (POST)

### 2.2.1 Create a task assigned to user 1

**PowerShell**
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/tasks `
  -ContentType 'application/json' `
  -Body '{"title":"Write report","status":"pending","userId":1}'
```

**CMD**
```
curl -X POST http://localhost:3000/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"Write report\",\"status\":\"pending\",\"userId\":1}"
```

**Expected:** `HTTP 201` with new task object.

### 2.2.2 Invalid status (should return 400)

```powershell
try {
  Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/tasks `
    -ContentType 'application/json' `
    -Body '{"title":"X","status":"wrong","userId":1}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```
```
curl -i -X POST http://localhost:3000/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"X\",\"status\":\"wrong\",\"userId\":1}"
```

**Expected:** `HTTP 400`, `"status must be one of [pending, in-progress, completed]"`.

### 2.2.3 Unknown userId (should return 400)

```powershell
try {
  Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/tasks `
    -ContentType 'application/json' `
    -Body '{"title":"X","status":"pending","userId":999}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```
```
curl -i -X POST http://localhost:3000/api/tasks -H "Content-Type: application/json" -d "{\"title\":\"X\",\"status\":\"pending\",\"userId\":999}"
```

**Expected:** `HTTP 400`, `"userId 999 does not exist"`.

### 2.2.4 Assign a new task to a specific user (e.g. user id 7)

First check that user 7 exists:
```powershell
Invoke-RestMethod http://localhost:3000/api/users/7
```

If it exists, assign a task to them:
```powershell
Invoke-RestMethod -Method POST -Uri http://localhost:3000/api/tasks `
  -ContentType 'application/json' `
  -Body '{"title":"Client review","status":"pending","userId":7}'
```

Verify:
```powershell
Invoke-RestMethod "http://localhost:3000/api/tasks?userId=7"
```

---

# PHASE 2.3 – Update Task (PUT)

### 2.3.1 Partial update – change only status

**PowerShell**
```powershell
Invoke-RestMethod -Method PUT -Uri http://localhost:3000/api/tasks/1 `
  -ContentType 'application/json' `
  -Body '{"status":"completed"}'
```

**CMD**
```
curl -X PUT http://localhost:3000/api/tasks/1 -H "Content-Type: application/json" -d "{\"status\":\"completed\"}"
```

**Expected:** `HTTP 200` with the updated task. `title` and `userId` unchanged.

### 2.3.2 Reassign a task to a different user

```powershell
Invoke-RestMethod -Method PUT -Uri http://localhost:3000/api/tasks/1 `
  -ContentType 'application/json' `
  -Body '{"userId":2}'
```

### 2.3.3 Non-existent task (should return 404)

```powershell
try {
  Invoke-RestMethod -Method PUT -Uri http://localhost:3000/api/tasks/9999 `
    -ContentType 'application/json' `
    -Body '{"status":"completed"}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```
```
curl -i -X PUT http://localhost:3000/api/tasks/9999 -H "Content-Type: application/json" -d "{\"status\":\"completed\"}"
```

**Expected:** `HTTP 404`, `"Task 9999 not found"`.

### 2.3.4 Invalid status on update (should return 400)

```powershell
try {
  Invoke-RestMethod -Method PUT -Uri http://localhost:3000/api/tasks/1 `
    -ContentType 'application/json' -Body '{"status":"wrong"}'
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)"
  $_.ErrorDetails.Message
}
```

---

# PHASE 2.4 – Request Logging

This cannot be checked via HTTP response – instead, look at the Java backend
terminal (Terminal 1 where Spring Boot is running). Every HTTP request should
produce a single log line with this shape:

```
HH:mm:ss.SSS INFO  access - GET /api/users -> 200 (8 ms) ip=...
HH:mm:ss.SSS INFO  DataStore - Created user id=4 email=demo@client.com
HH:mm:ss.SSS INFO  access - POST /api/users -> 201 (102 ms) ip=...
HH:mm:ss.SSS WARN  access - GET /api/users/999 -> 404 (14 ms) ip=...
HH:mm:ss.SSS WARN  access - POST /api/users -> 400 (18 ms) ip=...
```

What to verify:
- Every request produces a line.
- Each line contains: method, path, status code, duration (ms), client IP.
- 2xx / 3xx responses are logged at INFO, 4xx at WARN, 5xx at ERROR.
- Business events (user/task created/updated) have their own `DataStore` log line.

---

# PHASE 3 – Persistence + Detailed Health (bonus)

### 3.1 Detailed health

**PowerShell**
```powershell
Invoke-RestMethod http://localhost:8080/health/detailed
```

**CMD**
```
curl http://localhost:8080/health/detailed
```

**Expected:** JSON containing `uptimeMs`, `memory.usedMb`, `memory.maxMb`,
`jvm.version`, `data.users`, `data.tasks`.

### 3.2 Persistence – data survives restart

Look at the file on disk:
```
type java-backend\data\data.json
```

This JSON file is rewritten (atomically) on every create/update. If you
restart the Java backend, the created users and tasks are still there.

---

# PHASE 5 – Metrics + Rate Limiting (bonus)

### 5.1 Live metrics

**PowerShell**
```powershell
Invoke-RestMethod http://localhost:8080/api/metrics
```

**CMD**
```
curl http://localhost:8080/api/metrics
```

**Expected:** JSON containing:
- `totalRequests` – number of HTTP requests served
- `uptimeSeconds`
- `averageDurationMs`
- `requestsByRoute` – count per `METHOD /path`
- `requestsByStatus` – count per HTTP status code

### 5.2 Rate limiting (120 requests per minute per IP)

**PowerShell**
```powershell
$r = @{}; 1..135 | ForEach-Object {
  try { $c = [int](Invoke-WebRequest -Uri "http://localhost:8080/api/users" -UseBasicParsing).StatusCode }
  catch { $c = [int]$_.Exception.Response.StatusCode }
  $r[$c] = 1 + ($r[$c] -as [int])
}
$r
```

**Expected:**
```
Name Value
---- -----
 200   120
 429    15
```

First 120 requests succeed, the rest are rejected with `HTTP 429 Too Many
Requests` and a `Retry-After` header.

---

# Automated Test Suite

A single script that runs 40 checks across all phases at once.

```powershell
powershell -ExecutionPolicy Bypass -File .\test-suite.ps1
```

**Expected last lines:**
```
PASS: 40
FAIL: 0
```

This one command is the strongest single proof that every requirement works.

---

# UI Walkthrough (http://localhost:5173)

Open the React UI in a browser and verify:

1. **Green health banner** at the top says `Java backend is running`.
   Proves React -> Node -> Java chain.
2. **Stats tiles** show users total and tasks broken down by status.
3. **Users panel** lists all users.
4. **Tasks panel** lists all tasks.
5. **Status filter buttons** (`All / Pending / In Progress / Completed`)
   filter the tasks list.
6. **Click on a user** in the Users panel – the right panel filters to only
   that user's assigned tasks.
7. **Refresh button** reloads everything.

### Live POST demo through the UI

The UI itself does not have a "create user/task" form, but you can prove
POST works visually:

1. Note the current user count in the Stats panel.
2. In a terminal, run the POST from section 2.1.1.
3. Click **Refresh All Data** in the UI.
4. The new user appears in the Users panel and the stats count increases.

Do the same for a task (section 2.2.1) and watch the Tasks panel update.

### Browser DevTools proof

- **Network tab** (F12 -> Network -> Fetch/XHR) shows real HTTP calls to
  `http://localhost:3000/api/*` with status `200`, proving the UI is
  talking to the real backend.
- **Console tab** can be used to run quick `fetch()` tests against the
  Node gateway.

---

# Troubleshooting

### "Port 8080 already in use"
Something else is bound to 8080. Pick another port (e.g. 8081), set it in both `application.properties` (`server.port`) and `node-backend/.env` (`GO_BACKEND_URL`), restart Java then Node.

### PowerShell JSON parsing errors with curl
PowerShell does not forward `\"` inside `curl.exe` arguments cleanly when
the JSON contains spaces. Use `Invoke-RestMethod` (shown throughout this
doc) or run the curl command from **CMD** instead.

### `ECONNREFUSED` from Node backend
The Java backend is not running. Start it first (Terminal 1).

### Rate limit hit during testing
The limiter is configured at 120 requests/minute/IP. Wait 60 seconds, or
temporarily disable it by editing `java-backend/src/main/resources/application.properties`:
```
ratelimit.enabled=false
```
and restart the Java backend.

---

# Summary Checklist

| # | Requirement | Endpoint | Status |
|---|---|---|---|
| Phase 1 | React + Node + Java chain | all GETs | Pass |
| Phase 2.1 | Create user | `POST /api/users` | Pass |
| Phase 2.2 | Create task | `POST /api/tasks` | Pass |
| Phase 2.3 | Update task (partial) | `PUT /api/tasks/{id}` | Pass |
| Phase 2.4 | Structured request logging | Java terminal | Pass |
| Phase 3 | JSON file persistence | `data/data.json` | Pass |
| Phase 3 | Detailed health | `GET /health/detailed` | Pass |
| Phase 5 | Metrics | `GET /api/metrics` | Pass |
| Phase 5 | Rate limiting | 429 after 120/min | Pass |

Total automated tests: **40 passing / 0 failing**.
