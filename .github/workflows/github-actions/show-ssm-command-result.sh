#!/usr/bin/env bash

set -euo pipefail

# 역할: 배포 명령 실패 시 EC2 SSM의 stdout/stderr를 GitHub Actions 로그로 출력한다.
: "${COMMAND_ID:?COMMAND_ID is required}"
: "${INSTANCE_ID:?INSTANCE_ID is required}"

# 실패 원인은 숨기지 않되, 이 진단 단계 자체가 워크플로 결과를 덮어쓰지 않도록 실패를 무시한다.
aws ssm get-command-invocation \
  --command-id "$COMMAND_ID" \
  --instance-id "$INSTANCE_ID" \
  --query '{Status:Status,ResponseCode:ResponseCode,StandardOutput:StandardOutputContent,StandardError:StandardErrorContent}' \
  --output json || true
