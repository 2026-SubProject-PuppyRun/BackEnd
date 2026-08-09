#!/usr/bin/env bash

set -euo pipefail

: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${RELEASE_BUNDLE_DIRECTORY:?RELEASE_BUNDLE_DIRECTORY is required}"

ARCHIVE_PATH="/tmp/release-${RELEASE_TAG}.tar.gz"

echo "===== Archiving deployment bundle ====="
tar -czf "$ARCHIVE_PATH" -C "$RELEASE_BUNDLE_DIRECTORY" .

echo "===== Uploading release bundle to S3 ====="
aws s3 cp "$ARCHIVE_PATH" "s3://${AWS_S3_BUCKET}/puppyrun/releases/${RELEASE_TAG}.tar.gz"

rm -f "$ARCHIVE_PATH"

echo "[v] Successfully uploaded release bundle to S3: s3://${AWS_S3_BUCKET}/puppyrun/releases/${RELEASE_TAG}.tar.gz"
