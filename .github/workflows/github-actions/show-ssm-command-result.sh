#!/usr/bin/env bash

set -euo pipefail

: "${COMMAND_ID:?COMMAND_ID is required}"
: "${INSTANCE_ID:?INSTANCE_ID is required}"

aws ssm get-command-invocation \
  --command-id "$COMMAND_ID" \
  --instance-id "$INSTANCE_ID" \
  --query '{Status:Status,ResponseCode:ResponseCode,StandardOutput:StandardOutputContent,StandardError:StandardErrorContent}' \
  --output json || true
