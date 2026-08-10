#!/usr/bin/env bash
set -euo pipefail

HEALTH_URL="${1:?Usage: health-check.sh <readiness-url>}"
MAX_RETRIES="${MAX_RETRIES:-12}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

for attempt in $(seq 1 "$MAX_RETRIES"); do
  if curl --fail --silent --show-error "$HEALTH_URL" >/dev/null; then
    echo "Health check passed: $attempt/$MAX_RETRIES"
    exit 0
  fi
  sleep "$SLEEP_SECONDS"
done

docker logs --tail 200 puppyrun-backend || true
echo "Health check failed: $HEALTH_URL"
exit 1
