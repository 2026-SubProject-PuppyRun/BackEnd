#!/usr/bin/env bash
set -euo pipefail

# RabbitMQ와 Alloy만 독립적으로 관리한다. backend 이미지는 이 스크립트가 교체하지 않는다.
ROOT=/home/ubuntu/puppyrun
CONFIG="$ROOT/config"
COMPOSE="$ROOT/compose/docker-compose.infra.yml"
ACTION="${1:-up}"

[[ -r "$CONFIG/app.env" && -r "$CONFIG/infra.env" && -r "$CONFIG/deploy.env" ]] || {
  echo "Missing app.env, infra.env, or deploy.env."; exit 1;
}
source "$CONFIG/deploy.env"
: "${INFRA_COMPOSE_PROJECT_NAME:=puppyrun-infra}"

compose() {
  docker compose --project-name "$INFRA_COMPOSE_PROJECT_NAME" \
    --env-file "$CONFIG/app.env" --env-file "$CONFIG/infra.env" -f "$COMPOSE" "$@"
}

case "$ACTION" in
  up) compose up -d --remove-orphans ;;
  restart) compose restart ;;
  down) compose down ;;
  logs) compose logs --tail 200 -f ;;
  status) compose ps ;;
  *) echo "Usage: infra.sh [up|restart|down|logs|status]"; exit 2 ;;
esac
