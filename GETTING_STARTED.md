# Getting started

This file is a short onboarding path. The full spec is in `TEST_REQUIREMENTS.md`; this one is just “clone/unzip → run → verify”.

## Prerequisites

Check versions (anything recent is usually fine):

```
java -version
mvn -version
node -version
npm -version
```

## Install deps (once per machine)

From the project root:

```
cd java-backend
mvn -q -DskipTests compile
```

```
cd ../node-backend
npm install
```

```
cd ../react-frontend
npm install
```

You don’t have to run `mvn compile` every time; it just warms Maven. `npm install` is what matters for Node/React.

## Start order

Always **Java first**, then Node, then React.

1. `cd java-backend` → `mvn spring-boot:run` → wait for “Started Application”
2. `cd node-backend` → `npm start`
3. `cd react-frontend` → `npm run dev` → open http://localhost:5173

## Smoke test

- Browser: http://localhost:5173 — you should see users, tasks, stats, green health text.
- Terminal: `curl http://localhost:3000/health` — nested `goBackend` should be ok.

## Where the code lives

- **Java:** `java-backend/src/main/java/com/developer/test/` — controllers, DTOs, `DataStore`, filters.
- **Node:** `node-backend/` — routes call Java via `utils/httpClient.js`.
- **React:** `react-frontend/src/` — `App.jsx`, `services/api.js`.

## More detail

See **README.md** for endpoints, Postman, and the test script.  
See **TESTING_GUIDE.md** if you want to walk through requests manually.
