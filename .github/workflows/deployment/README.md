# EC2 배포 초기 설치

일반 배포는 GitHub Actions가 ECR에 SHA 태그 이미지를 push한 뒤 SSM으로 아래 파일만 실행한다.

```bash
sudo /home/ubuntu/puppyrun/scripts/deploy.sh <github-sha>
```

EC2에서 저장소를 준비한 뒤 한 번만 bootstrap을 실행한다.

```bash
sudo bash .github/workflows/deployment/bootstrap-ec2.sh
sudoedit /home/ubuntu/puppyrun/config/deploy.env
sudoedit /home/ubuntu/puppyrun/config/app.env
```

배포 로그는 `/home/ubuntu/puppyrun/logs/latest-deploy.log`에서 확인한다. EC2 인스턴스 프로파일에는 ECR pull 권한이 필요하다.

Firebase 서비스 계정 JSON은 Git과 Docker 이미지에 포함하지 않는다. EC2에서 아래 경로로 저장하고 `app.env`에 컨테이너 내부 경로를 지정한다.

```bash
sudo install -o root -g ubuntu -m 640 \
  /home/ubuntu/firebase-service-account.json \
  /home/ubuntu/puppyrun/config/firebase-service-account.json
```

```dotenv
FCM_ACCOUNT_PATH=file:/run/secrets/firebase-service-account.json
```

배포 성공 후에는 `current-image`, `previous-image`를 제외한 Puppyrun backend 이미지를 자동 정리한다. 수동 정리가 필요하면 아래를 실행한다.

```bash
sudo /home/ubuntu/puppyrun/scripts/cleanup-images.sh
```
