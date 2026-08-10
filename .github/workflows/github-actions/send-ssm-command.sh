#!/usr/bin/env bash

set -euo pipefail

# 역할: GitHub Actions에서 EC2로 단 하나의 SSM 명령을 전송한다.
# EC2 흐름: S3 번들 다운로드 → new 후보 구성 → ECR pull → 후보 deploy.sh 실행.
# .env는 S3에 포함하지 않고 EC2 config/.env를 후보 디렉터리에 복사한다.
: "${INSTANCE_ID:?INSTANCE_ID is required}"
: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"
: "${RELEASE_TAG:?RELEASE_TAG is required}"
: "${DEPLOY_IMAGE_TAG:?DEPLOY_IMAGE_TAG is required}"
: "${AWS_REGION:?AWS_REGION is required}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID is required}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT is required}"

# SSM heredoc에 삽입되는 값은 제한된 태그 형식만 허용한다.
if ! [[ "$RELEASE_TAG" =~ ^[A-Za-z0-9-]+$ ]] || ! [[ "$DEPLOY_IMAGE_TAG" =~ ^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$ ]]; then
  echo "Release tag or image tag has an invalid format."
  exit 1
fi

ROOT_DIRECTORY="/home/ubuntu/puppyrun"
CANDIDATE_DIRECTORY="$ROOT_DIRECTORY/releases/$RELEASE_TAG"

# 아래 명령들은 EC2에서 순서대로 실행된다. 앞 단계 실패 시 set -e로 즉시 중단된다.
SSM_COMMANDS=$(cat <<EOF
set -euo pipefail
test ! -e $CANDIDATE_DIRECTORY
mkdir -p $CANDIDATE_DIRECTORY
# Git에서 온 비밀값 없는 번들을 후보 릴리스 디렉터리에 해제한다.
aws s3 cp s3://$AWS_S3_BUCKET/puppyrun/releases/$RELEASE_TAG.tar.gz $CANDIDATE_DIRECTORY/release.tar.gz
tar -xzf $CANDIDATE_DIRECTORY/release.tar.gz -C $CANDIDATE_DIRECTORY
rm -f $CANDIDATE_DIRECTORY/release.tar.gz
# EC2에만 있는 환경 파일을 이번 릴리스의 독립 스냅샷으로 만든다.
install -m 600 $ROOT_DIRECTORY/config/.env $CANDIDATE_DIRECTORY/.env
ln -sfn $CANDIDATE_DIRECTORY $ROOT_DIRECTORY/new.next
mv -Tf $ROOT_DIRECTORY/new.next $ROOT_DIRECTORY/new
# 이미지 pull이 성공한 경우에만 후보 deploy.sh가 서버 교체와 health check를 수행한다.
echo "===== Verify EC2 IAM identity ====="
aws sts get-caller-identity --query 'Arn' --output text
echo "===== Login to ECR ====="
if ! aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com; then
  echo "ECR login failed. Check the EC2 instance profile permissions and AWS region/account settings."
  exit 1
fi
docker pull $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/puppyrun-backend:$DEPLOY_IMAGE_TAG
AWS_REGION=$AWS_REGION AWS_ACCOUNT_ID=$AWS_ACCOUNT_ID RELEASE_TAG=$RELEASE_TAG SKIP_IMAGE_PULL=true bash $CANDIDATE_DIRECTORY/deploy.sh $DEPLOY_IMAGE_TAG
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
