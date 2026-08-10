#!/usr/bin/env bash

set -euo pipefail

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


echo "===== Create release archive ====="

RELEASE_ARCHIVE_BASE64=$(tar -czf - \
  -C "$RELEASE_BUNDLE_DIRECTORY" \
  deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml \
  | base64 | tr -d '\n')


if [ -z "$RELEASE_ARCHIVE_BASE64" ]; then
  echo "Release archive is empty."
  exit 1
fi


SSM_COMMANDS=$(cat <<EOF
set -eu


ROOT_DIRECTORY="$ROOT_DIRECTORY"
NEW_DIRECTORY="$NEW_DIRECTORY"
LOG_DIRECTORY="$LOG_DIRECTORY"
LOG_FILE="$LOG_FILE"


mkdir -p "\$ROOT_DIRECTORY"
mkdir -p "\$LOG_DIRECTORY"


exec >> "\$LOG_FILE" 2>&1


echo "================================="
echo "Deployment started"
echo "Release : $RELEASE_TAG"
echo "Image   : $DEPLOY_IMAGE_TAG"
echo "================================="


if [ -e "\$NEW_DIRECTORY" ]; then
  echo "Deployment already exists: \$NEW_DIRECTORY"
  exit 1
fi


mkdir -p "\$NEW_DIRECTORY"


echo "===== Extract release files ====="


base64 -d <<'RELEASE_ARCHIVE' | tar -xzf - -C "\$NEW_DIRECTORY"
$RELEASE_ARCHIVE_BASE64
RELEASE_ARCHIVE


echo "===== Create latest log link ====="

ln -sfn \
"deploy-$RELEASE_TAG.log" \
"\$LOG_DIRECTORY/latest.log"


echo "===== Start deploy script ====="


nohup env \
  AWS_REGION="$AWS_REGION" \
  AWS_ACCOUNT_ID="$AWS_ACCOUNT_ID" \
  RELEASE_TAG="$RELEASE_TAG" \
  bash "\$NEW_DIRECTORY/deploy.sh" "$DEPLOY_IMAGE_TAG" \
  >> "\$LOG_FILE" 2>&1 < /dev/null &


DEPLOY_PID=\$!


echo "deploy.sh started"
echo "PID: \$DEPLOY_PID"

echo "===== SSM command finished ====="

EOF
)


COMMAND_ID=$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --parameters commands="$(jq -n --arg command "$SSM_COMMANDS" '[ $command ]')" \
  --query 'Command.CommandId' \
  --output text)


echo "EC2 deployment started asynchronously"
echo "SSM Command ID: $COMMAND_ID"
echo "Deployment log: $LOG_FILE"
