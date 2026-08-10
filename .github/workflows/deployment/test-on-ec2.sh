#!/usr/bin/env sh

set -eu

# 역할: EC2에서 GitHub Actions 없이 현재 deployment 폴더를 new 후보로 적재하고 실제 배포 흐름을 테스트한다.
# 사용법: AWS_REGION=... AWS_ACCOUNT_ID=... sh test-on-ec2.sh <immutable-image-tag>
# 빠른 파일 적재 검증: AWS_REGION=... AWS_ACCOUNT_ID=... sh test-on-ec2.sh --sync-only
SOURCE_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIRECTORY="/home/ubuntu/puppyrun"
NEW_DIRECTORY="$ROOT_DIRECTORY/new"
LOG_DIRECTORY="$ROOT_DIRECTORY/logs"

SYNC_ONLY=false
if [ "${1:-}" = "--sync-only" ]; then
  SYNC_ONLY=true
  IMAGE_TAG=""
else
  IMAGE_TAG="${1:-}"
fi
if [ "$SYNC_ONLY" = false ] && [ -z "$IMAGE_TAG" ]; then
  echo "Usage: AWS_REGION=... AWS_ACCOUNT_ID=... $0 <immutable-image-tag>"
  echo "   or: AWS_REGION=... AWS_ACCOUNT_ID=... $0 --sync-only"
  exit 2
fi

# sync-only는 파일 적재만 확인하므로 AWS/ECR 연결 정보가 필요 없다.
if [ "$SYNC_ONLY" = false ]; then
  : "${AWS_REGION:?AWS_REGION is required}"
  : "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
fi

RELEASE_TAG="${RELEASE_TAG:-manual-$(date -u +%Y%m%d%H%M%S)}"
LOG_FILE="$LOG_DIRECTORY/deploy-$RELEASE_TAG.log"
ARCHIVE_PATH="/tmp/puppyrun-$RELEASE_TAG.tar.gz"

if [ -e "$NEW_DIRECTORY" ]; then
  echo "new already exists: $NEW_DIRECTORY"
  echo "Inspect it or remove it after confirming no deployment is running."
  exit 1
fi

mkdir -p "$LOG_DIRECTORY" "$NEW_DIRECTORY"
printf '===== Manual EC2 deployment test: %s =====\n' "$RELEASE_TAG" > "$LOG_FILE"
printf 'Source: %s\nImage: %s\n' "$SOURCE_DIRECTORY" "${IMAGE_TAG:-sync-only}" >> "$LOG_FILE"

# .env와 metadata는 EC2에서 관리한다. 그 외 deployment 폴더 전체를 후보에 복사한다.
if ! tar -czf "$ARCHIVE_PATH" \
  --exclude='./.env' \
  --exclude='./.env.*' \
  --exclude='./metadata.env' \
  -C "$SOURCE_DIRECTORY" \
  . >> "$LOG_FILE" 2>&1; then
  echo "Failed to create release archive. See: $LOG_FILE"
  exit 1
fi

if ! tar -xzf "$ARCHIVE_PATH" -C "$NEW_DIRECTORY" >> "$LOG_FILE" 2>&1; then
  rm -f "$ARCHIVE_PATH"
  echo "Failed to extract release archive. See: $LOG_FILE"
  exit 1
fi
rm -f "$ARCHIVE_PATH"

for required_file in deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml; do
  if [ ! -f "$NEW_DIRECTORY/$required_file" ]; then
    echo "Missing synchronized file: $required_file" | tee -a "$LOG_FILE"
    exit 1
  fi
done

ln -sfn "deploy-$RELEASE_TAG.log" "$LOG_DIRECTORY/latest.log"

if [ "$SYNC_ONLY" = true ]; then
  echo "Deployment files were synchronized to: $NEW_DIRECTORY"
  echo "Sync log: $LOG_FILE"
  exit 0
fi

echo "Starting deploy.sh directly on EC2. Log: $LOG_FILE"

# deploy.sh는 EC2 /home/ubuntu/.env를 복사하고 ECR pull·health check·current/previous 승격까지 수행한다.
if AWS_REGION="$AWS_REGION" \
  AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
  RELEASE_TAG="$RELEASE_TAG" \
  sh "$NEW_DIRECTORY/deploy.sh" "$IMAGE_TAG" >> "$LOG_FILE" 2>&1; then
  echo "Manual deployment test succeeded."
  tail -n 80 "$LOG_FILE"
else
  echo "Manual deployment test failed. See: $LOG_FILE"
  tail -n 200 "$LOG_FILE" || true
  exit 1
fi
