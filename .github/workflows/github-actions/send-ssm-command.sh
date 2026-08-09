#!/usr/bin/env bash

set -euo pipefail

: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${DEPLOY_DIRECTORY:?DEPLOY_DIRECTORY is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

SSM_COMMANDS=$(cat <<EOF
mkdir -p $DEPLOY_DIRECTORY && \
aws s3 cp s3://$AWS_S3_BUCKET/puppyrun/releases/$RELEASE_TAG.tar.gz $DEPLOY_DIRECTORY/release.tar.gz && \
tar -xzf $DEPLOY_DIRECTORY/release.tar.gz -C $DEPLOY_DIRECTORY && \
rm -f $DEPLOY_DIRECTORY/release.tar.gz && \
cd $DEPLOY_DIRECTORY && \
RELEASE_TAG=$RELEASE_TAG DEPLOY_IMAGE_TAG=$DEPLOY_IMAGE_TAG AWS_S3_BUCKET=$AWS_S3_BUCKET bash deploy.sh
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

