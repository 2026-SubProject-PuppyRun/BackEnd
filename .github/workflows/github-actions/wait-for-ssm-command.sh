#!/usr/bin/env bash

set -euo pipefail

: "${COMMAND_ID:?COMMAND_ID is required}"
: "${INSTANCE_ID:?INSTANCE_ID is required}"

MAX_WAITER_CYCLES="${MAX_WAITER_CYCLES:-6}"

for attempt in $(seq 1 "$MAX_WAITER_CYCLES"); do
  if aws ssm wait command-executed \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID"; then
    echo "SSM deployment completed successfully."
    exit 0
  fi

  STATUS=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --query 'Status' \
    --output text)

  case "$STATUS" in
    Success)
      echo "SSM deployment completed successfully."
      exit 0
      ;;
    Pending|InProgress|Delayed)
      echo "SSM command is still running ($STATUS): $attempt/$MAX_WAITER_CYCLES"
      ;;
    *)
      echo "SSM deployment failed with status: $STATUS"
      exit 1
      ;;
  esac
done

echo "SSM deployment did not finish within ${MAX_WAITER_CYCLES} waiter cycles."
exit 1
