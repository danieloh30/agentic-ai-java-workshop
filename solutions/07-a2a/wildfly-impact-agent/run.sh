#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
engine="${CONTAINER_ENGINE:-}"
if [[ -z "$engine" ]]; then
  if command -v podman >/dev/null 2>&1 && podman info >/dev/null 2>&1; then engine=podman
  elif command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then engine=docker
  else echo "Start Podman or Docker, then retry." >&2; exit 1; fi
fi
name="${WF_CONTAINER_NAME:-apex-wildfly-impact}"
port="${WF_PORT:-8888}"
case "${1:-up}" in
  down) "$engine" stop "$name"; exit ;;
  logs) exec "$engine" logs -f "$name" ;;
  up) ;;
  *) echo "Usage: $0 [up|down|logs]" >&2; exit 2 ;;
esac
: "${OPENAI_API_KEY:?Export OPENAI_API_KEY before starting WildFly}"
if "$engine" container inspect "$name" >/dev/null 2>&1; then
  echo "Container $name already exists. Use ./run.sh down before starting again." >&2; exit 1
fi
# Reuse the workshop Maven wrapper; this project remains a standalone WAR.
../multi-agent-system/mvnw -B -ntp -f "$PWD/pom.xml" verify
"$engine" build -f Containerfile -t localhost/apex-wildfly-impact:workshop .
run_args=(-d --rm --name "$name" -p "127.0.0.1:${port}:8080")
if [[ "$engine" == docker ]]; then run_args+=(--add-host=host.docker.internal:host-gateway); fi
"$engine" run "${run_args[@]}" \
  -e OPENAI_API_KEY -e OPENAI_MODEL="${OPENAI_MODEL:-gpt-4o}" \
  -e OPENAI_BASE_URL="${OPENAI_BASE_URL:-https://api.openai.com/v1}" \
  -e PUBLIC_AGENT_URL="http://localhost:${port}/" \
  localhost/apex-wildfly-impact:workshop >/dev/null
for attempt in {1..60}; do
  if curl -fsS "http://localhost:${port}/.well-known/agent-card.json" >/dev/null 2>&1; then
    echo "WildFly impact agent ready: http://localhost:${port}/.well-known/agent-card.json"
    exit 0
  fi
  sleep 1
done
echo "WildFly did not become ready. Inspect: $engine logs $name" >&2
exit 1
