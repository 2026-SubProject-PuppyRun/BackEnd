#!/usr/bin/env bash

set -euo pipefail
# ENV_FILE의 내용을 GitHub Actions 로그에 노출하지 않는다.
set +x

: "${ENV_FILE:?ENV_FILE is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"

umask 077
printf '%s\n' "$ENV_FILE" > "$RELEASE_BUNDLE_DIRECTORY/.env"
chmod 600 "$RELEASE_BUNDLE_DIRECTORY/.env"
