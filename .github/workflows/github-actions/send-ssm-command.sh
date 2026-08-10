#!/usr/bin/env bash

set -euo pipefail

# 역할: 안전한 배포 파일만 SSM으로 EC2 new 폴더에 복사하고, EC2의 deploy.sh를 비동기로 시작한다.
# GitHub Actions는 SSM 명령이 접수된 것까지만 확인하며 배포 결과를 기다리지 않는다.
: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"

if ! [[ "$DEPLOY_IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Image tag has invalid format."
  exit 1
fi
if ! [[ "$RELEASE_TAG" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "Release tag has invalid format."
  exit 1
fi
if ! [[ "$AWS_ACCOUNT_ID" =~ ^[0-9]{12}$ ]]; then
  echo "AWS_ACCOUNT_ID must be a 12-digit AWS account ID."
  exit 1
fi
if ! [[ "$AWS_REGION" =~ ^[a-z]{2}(-[a-z]+)+-[0-9]+$ ]]; then
  echo "AWS_REGION has an invalid format."
  exit 1
fi

ROOT_DIRECTORY="/home/ubuntu/puppyrun"
NEW_DIRECTORY="$ROOT_DIRECTORY/new"
LOG_DIRECTORY="$ROOT_DIRECTORY/logs"
LOG_FILE="$LOG_DIRECTORY/deploy-$RELEASE_TAG.log"

# .env와 metadata를 제외한 파일 네 개만 압축한다. 비밀값은 SSM 메시지에도 포함하지 않는다.
RELEASE_ARCHIVE_BASE64=$(tar -czf - \
  -C "$RELEASE_BUNDLE_DIRECTORY" \
  deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml \
  | base64 | tr -d '\n')

SSM_COMMANDS=$(cat <<EOF
set -eu

mkdir -p "$ROOT_DIRECTORY"
mkdir -p "$LOG_DIRECTORY"
if [ -e "$NEW_DIRECTORY" ]; then
  echo "A deployment is already in progress: $NEW_DIRECTORY"
  exit 1
fi

mkdir -p "$NEW_DIRECTORY"
base64 -d <<'RELEASE_ARCHIVE' | tar -xzf - -C "$NEW_DIRECTORY"
$RELEASE_ARCHIVE_BASE64
RELEASE_ARCHIVE

# deploy.sh가 EC2 로컬 /home/ubuntu/.env를 복사하고 ECR pull·health check·승격을 모두 처리한다.
# 표준 출력과 오류를 릴리스별 파일에 남긴다. 실패해도 GitHub Actions는 완료를 기다리지 않는다.
ln -sfn "deploy-$RELEASE_TAG.log" "$LOG_DIRECTORY/latest.log"
nohup env \
  AWS_REGION="$AWS_REGION" \
  AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
  RELEASE_TAG="$RELEASE_TAG" \
  sh "$NEW_DIRECTORY/deploy.sh" "$DEPLOY_IMAGE_TAG" \
  > "$LOG_FILE" 2>&1 < /dev/null &
EOF
)

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg command "$SSM_COMMANDS" '[ $command ]')" \
  --query 'Command.CommandId' \
  --output text)

echo "EC2 deployment started asynchronously. SSM command ID: $COMMAND_ID"
echo "Deployment log: $LOG_FILE"
