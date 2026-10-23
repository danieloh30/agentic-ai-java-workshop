#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
variant="$(basename "$PWD")"
if [[ "$variant" == "lab" ]]; then default_port=55435; else default_port=55436; fi
export PART3_DB_PORT="${PART3_DB_PORT:-$default_port}"
engine="${CONTAINER_ENGINE:-}"
if [[ -z "$engine" ]]; then
  if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then engine=docker
  elif command -v podman >/dev/null 2>&1 && podman info >/dev/null 2>&1; then engine=podman
  else echo "Start Docker or Podman, then run ./dev.sh again." >&2; exit 1; fi
fi
container="apex-part3-${variant}-db"
volume="apex-part3-${variant}-data"
if "$engine" container inspect "$container" >/dev/null 2>&1; then
  "$engine" start "$container" >/dev/null
else
  "$engine" run -d --name "$container" -p "127.0.0.1:${PART3_DB_PORT}:5432" \
    -e POSTGRES_DB=apex -e POSTGRES_USER=workshop -e POSTGRES_PASSWORD=workshop \
    -v "${volume}:/var/lib/postgresql/data" docker.io/library/postgres:17.11 >/dev/null
fi
ready=false
for attempt in {1..30}; do
  if "$engine" exec "$container" pg_isready -U workshop -d apex >/dev/null 2>&1; then ready=true; break; fi
  sleep 1
done
if [[ "$ready" != true ]]; then echo "Database not ready: inspect $container logs." >&2; exit 1; fi
echo "Persistent PostgreSQL: $container, volume $volume, localhost:$PART3_DB_PORT"
if [[ "${1:-}" == "--prepare" ]]; then exec ./mvnw test -DskipTests; fi
: "${OPENAI_API_KEY:?Export OPENAI_API_KEY before starting the application}"
exec ./mvnw quarkus:dev
