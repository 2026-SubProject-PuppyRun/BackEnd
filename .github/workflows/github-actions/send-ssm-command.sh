#!/usr/bin/env bash

set -euo pipefail

: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

if ! echo "$RELEASE_TAG" | grep -Eq '^[A-Za-z0-9-]+$'; then
  echo "Release tag has invalid format."
  exit 1
fi

if ! echo "$DEPLOY_IMAGE_TAG" | grep -Eq '^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$'; then
  echo "Image tag has invalid format."
  exit 1
fi

if ! echo "$AWS_ACCOUNT_ID" | grep -Eq '^[0-9]{12}$'; then
  echo "AWS_ACCOUNT_ID must be a 12-digit AWS account ID."
  exit 1
fi

if ! echo "$AWS_REGION" | grep -Eq '^[a-z]{2}(-[a-z]+)+-[0-9]+$'; then
  echo "AWS_REGION has an invalid format."
  exit 1
fi


ROOT_DIRECTORY="/home/ubuntu/puppyrun"
NEW_DIRECTORY="$ROOT_DIRECTORY/new"

# S3를 거치지 않고, 비밀 파일을 제외한 배포 구성만 SSM 명령에 포함해 EC2로 전달한다.
# 명시 목록만 묶으므로 .env나 로컬 메타데이터가 전송될 수 없다.
RELEASE_ARCHIVE_BASE64=$(tar -czf - \
  -C "$RELEASE_BUNDLE_DIRECTORY" \
  deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml \
  | base64 | tr -d '\n')
if [ -z "$RELEASE_ARCHIVE_BASE64" ]; then
  echo "Release archive is empty."
  exit 1
fi


SSM_COMMANDS=$(cat <<EOF
bash -s <<'REMOTE_DEPLOY_SCRIPT'
set -eu

test ! -e "$NEW_DIRECTORY"

mkdir -p "$NEW_DIRECTORY"

# 다운로드·pull·기동 중 어느 단계에서 실패해도 다음 배포를 막지 않도록 후보 폴더만 정리한다.
# 성공하면 deploy.sh가 new를 current로 이동하므로 이 정리 함수는 아무것도 삭제하지 않는다.
cleanup_new() {
  if [ -e "$NEW_DIRECTORY" ]; then
    rm -rf -- "$NEW_DIRECTORY"
  fi
}
trap cleanup_new 0 1 2 15

echo "===== Receive release files from SSM ====="

base64 --decode <<'RELEASE_ARCHIVE' | tar -xzf - -C "$NEW_DIRECTORY"
$RELEASE_ARCHIVE_BASE64
RELEASE_ARCHIVE


echo "===== Copy EC2 environment ====="

# 민감한 .env는 S3에 저장하지 않고 EC2 로컬 원본에서 각 릴리스로 독립 복사한다.
install -m 600 \
"/home/ubuntu/.env" \
"$NEW_DIRECTORY/.env"


echo "===== Verify AWS identity ====="

aws sts get-caller-identity


echo "===== Login ECR ====="

aws ecr get-login-password \
--region "$AWS_REGION" \
| docker login \
--username AWS \
--password-stdin \
"$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"


echo "===== Pull image ====="

docker pull \
"$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/puppyrun-backend:$DEPLOY_IMAGE_TAG"


echo "===== Deploy ====="

AWS_REGION="$AWS_REGION" \
AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
RELEASE_TAG="$RELEASE_TAG" \
SKIP_IMAGE_PULL=true \
bash "$NEW_DIRECTORY/deploy.sh" "$DEPLOY_IMAGE_TAG" || {
  deploy_status=\$?
  # deploy.sh가 current 복구를 마친 뒤 실패한 후보 파일만 정리한다.
  rm -rf -- "$NEW_DIRECTORY"
  exit "\$deploy_status"
}

REMOTE_DEPLOY_SCRIPT
EOF
)


COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg cmd "$SSM_COMMANDS" '[$cmd]')" \
  --query 'Command.CommandId' \
  --output text)


echo "SSM command ID: $COMMAND_ID"

echo "command_id=$COMMAND_ID" >> "$GITHUB_OUTPUT"
