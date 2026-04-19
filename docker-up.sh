#!/bin/sh
set -e
cd "$(dirname "$0")"
if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running. Start Docker Desktop (or the Docker daemon), then try again."
  exit 1
fi
echo "Starting stack (first build may take several minutes)..."
docker compose up --build -d
echo ""
echo "Open: http://localhost:5173"
echo "Health: http://localhost:3000/health"
