#!/bin/bash

set -e

echo "===== Deployment Start ====="


COMPOSE_FILE="docker-compose.deploy.yml"
SERVICE_NAME="backend"


echo "===== Docker Compose Pull ====="

docker compose \
  -f $COMPOSE_FILE \
  pull $SERVICE_NAME


echo "===== Docker Compose Restart ====="

docker compose \
  -f $COMPOSE_FILE \
  up -d $SERVICE_NAME


echo "===== Health Check ====="

chmod +x health-check.sh

./health-check.sh


echo "===== Remove Old Images ====="

docker image prune -f


echo "===== Deployment Success ====="
