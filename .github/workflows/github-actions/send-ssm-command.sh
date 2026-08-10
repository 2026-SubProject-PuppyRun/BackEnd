#!/usr/bin/env bash

set -euo pipefail

# 역할: deployment 폴더 전체에서 민감 파일을 제외한 구성만 EC2 new 폴더에 적재한다.
# 이 스크립트는 파일 적재만 담당하며, deploy.sh 실행은 별도 SSM 명령이 담당한다.
: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

if ! [[ "$RELEASE_TAG" =~ ^[A-Za-z0-9-]+$ ]]; then
  echo "Release tag has invalid format."
  exit 1
fi

ROOT_DIRECTORY="/home/ubuntu/puppyrun"
NEW_DIRECTORY="$ROOT_DIRECTORY/new"
LOG_DIRECTORY="$ROOT_DIRECTORY/logs"
LOG_FILE="$LOG_DIRECTORY/deploy-$RELEASE_TAG.log"

# .env와 배포 중 생성되는 metadata는 EC2에만 둔다. 나머지 deployment 파일은 모두 최신화한다.
RELEASE_ARCHIVE_BASE64=$(tar -czf - \
  --exclude='./.env' \
  --exclude='./.env.*' \
  --exclude='./metadata.env' \
  -C "$RELEASE_BUNDLE_DIRECTORY" \
  . | base64 | tr -d '\n')

if [ -z "$RELEASE_ARCHIVE_BASE64" ]; then
  echo "Release archive is empty."
  exit 1
fi

SSM_COMMANDS=$(cat <<EOF
mkdir -p "$LOG_DIRECTORY"
exec >> "$LOG_FILE" 2>&1
set -eu

echo "===== Sync release files: $RELEASE_TAG ====="
if [ -e "$NEW_DIRECTORY" ]; then
  echo "new already exists: $NEW_DIRECTORY"
  exit 1
fi

mkdir -p "$NEW_DIRECTORY"
base64 -d <<'RELEASE_ARCHIVE' | tar -xzf - -C "$NEW_DIRECTORY"
$RELEASE_ARCHIVE_BASE64
RELEASE_ARCHIVE

# 배포에 필요한 파일이 실제 new 폴더에 적재됐는지 확인한다.
for required_file in deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml; do
  test -f "$NEW_DIRECTORY/\$required_file"
done

echo "Release files synchronized successfully."
EOF
)

COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg command "$SSM_COMMANDS" '[ $command ]')" \
  --query 'Command.CommandId' \
  --output text)

echo "SSM file-sync command ID: $COMMAND_ID"
echo "Sync log: $LOG_FILE"
echo "command_id=$COMMAND_ID" >> "$GITHUB_OUTPUT"
