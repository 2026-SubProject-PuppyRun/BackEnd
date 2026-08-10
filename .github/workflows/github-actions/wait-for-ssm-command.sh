#!/usr/bin/env bash

set -euo pipefail

# 역할: deploy.sh 실행 결과가 아니라, EC2 new 폴더로의 파일 적재 결과만 확인한다.
: "${COMMAND_ID:?COMMAND_ID is required}"
: "${INSTANCE_ID:?INSTANCE_ID is required}"

if ! aws ssm wait command-executed \
  --command-id "$COMMAND_ID" \
  --instance-id "$INSTANCE_ID"; then
  STATUS=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --query 'Status' \
    --output text || true)
  echo "EC2 deployment file synchronization failed: $STATUS"
  exit 1
fi

echo "EC2 deployment files were synchronized successfully."
