#!/usr/bin/env bash

set -euo pipefail

# 역할: Git에서 변경된 배포 파일을 비밀값 없는 S3 릴리스 번들로 만든다.
# 번들은 EC2 new 후보 디렉터리에만 풀리며 .env와 current/previous 상태는 절대 포함하지 않는다.
: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"

ARCHIVE_PATH="/tmp/puppyrun-release-${RELEASE_TAG}.tar.gz"

# 후보 배포와 롤백에 필요한 코드/Compose만 묶는다.
tar -czf "$ARCHIVE_PATH" \
  -C "$RELEASE_BUNDLE_DIRECTORY" \
  deploy.sh rollback.sh health-check.sh docker-compose.deploy.yml
# release tag를 키로 사용해 동일 GitHub Actions 실행의 EC2 후보가 정확한 번들을 받게 한다.
aws s3 cp "$ARCHIVE_PATH" "s3://${AWS_S3_BUCKET}/puppyrun/releases/${RELEASE_TAG}.tar.gz"
rm -f "$ARCHIVE_PATH"
