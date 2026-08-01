# OTP Sentinel — frontend

React + TypeScript + Vite + Tailwind CSS single-page UI for the OTP incident investigation console. Consumes the Spring Boot backend's REST API (`docs/06-api-contracts.md` at the repo root).

## Development

```bash
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and expects the backend on `http://localhost:8080` (CORS for this is already configured via the backend's `dev` Spring profile — see the repo root README).

## Build

```bash
npm run build
```

Production builds are not deployed from `frontend/` directly — the repo root `Dockerfile` builds this project and copies its output into the Spring Boot application's `static/` resources, so `docker compose up --build` from the repo root serves everything as one artifact.

## Tests

```bash
npm run test
```
