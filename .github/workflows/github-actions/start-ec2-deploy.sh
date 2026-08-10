#!/usr/bin/env bash

set -euo pipefail

# 역할: 동기화가 완료된 EC2 new/deploy.sh를 백그라운드로 시작한다.
# 배포·health check·롤백 결과는 EC2 로그와 Grafana Cloud에서 관제한다.
: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"

if ! [[ "$DEPLOY_IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Image tag has invalid format."
  exit 1
fi
if ! [[ "$RELEASE_TAG" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "Release tag has invalid format."
  exit 1
fi

ROOT_DIRECTORY="/home/ubuntu/puppyrun"
NEW_DIRECTORY="$ROOT_DIRECTORY/new"
LOG_DIRECTORY="$ROOT_DIRECTORY/logs"
LOG_FILE="$LOG_DIRECTORY/deploy-$RELEASE_TAG.log"

SSM_COMMANDS=$(cat <<EOF
set -eu
test -f "$NEW_DIRECTORY/deploy.sh"
mkdir -p "$LOG_DIRECTORY"
ln -sfn "deploy-$RELEASE_TAG.log" "$LOG_DIRECTORY/latest.log"

nohup env \
  AWS_REGION="$AWS_REGION" \
  AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
  RELEASE_TAG="$RELEASE_TAG" \
  sh "$NEW_DIRECTORY/deploy.sh" "$DEPLOY_IMAGE_TAG" \
  >> "$LOG_FILE" 2>&1 < /dev/null &
EOF
)

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg command "$SSM_COMMANDS" '[ $command ]')" \
  --query 'Command.CommandId' \
  --output text)

echo "EC2 deploy.sh started asynchronously. SSM command ID: $COMMAND_ID"
echo "Deployment log: $LOG_FILE"
