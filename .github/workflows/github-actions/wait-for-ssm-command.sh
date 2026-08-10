#!/usr/bin/env bash

set -euo pipefail

# 역할: EC2의 "번들 준비 → pull → deploy → health check" SSM 명령이 끝날 때까지 기다린다.
# 이 스크립트는 health를 직접 검사하지 않고 EC2 deploy.sh의 종료 상태만 전달받는다.
: "${COMMAND_ID:?COMMAND_ID is required}"
: "${INSTANCE_ID:?INSTANCE_ID is required}"

MAX_WAITER_CYCLES="${MAX_WAITER_CYCLES:-6}"

# AWS waiter가 일시적으로 Pending/InProgress를 반환할 수 있으므로 제한 횟수만큼 재확인한다.
for attempt in $(seq 1 "$MAX_WAITER_CYCLES"); do
  if aws ssm wait command-executed \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID"; then
    echo "EC2 pull and deploy command completed successfully."
    exit 0
  fi

  STATUS=$(aws ssm get-command-invocation \
    --command-id "$COMMAND_ID" \
    --instance-id "$INSTANCE_ID" \
    --query 'Status' \
    --output text)

  case "$STATUS" in
    Success)
      echo "EC2 pull and deploy command completed successfully."
      exit 0
      ;;
    Pending|InProgress|Delayed)
      echo "SSM command is still running ($STATUS): $attempt/$MAX_WAITER_CYCLES"
      ;;
    *)
      echo "EC2 pull or deploy command failed with status: $STATUS"
      exit 1
      ;;
  esac
done

echo "EC2 pull or deploy command did not finish within ${MAX_WAITER_CYCLES} waiter cycles."
exit 1
