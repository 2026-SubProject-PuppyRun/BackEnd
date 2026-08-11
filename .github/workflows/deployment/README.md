# EC2 배포 구성

Compose의 관심사를 두 개로 분리한다.

| 구성 | 파일 | 관리 대상 | 실행 명령 |
| --- | --- | --- | --- |
| backend | `compose/docker-compose.backend.yml` | Spring Boot 이미지, Firebase mount, health check, rollback | `scripts/deploy.sh <github-sha>` |
| infra | `compose/docker-compose.infra.yml` | RabbitMQ, Grafana Alloy, EC2 metric 수집 | `scripts/infra.sh up` |

두 Compose는 `puppyrun-app`(backend ↔ RabbitMQ)과 `puppyrun-observability`(Alloy → backend metrics) 네트워크만 공유한다. 따라서 backend 배포나 rollback은 RabbitMQ와 Alloy를 재생성하지 않는다.

기존 단일 Compose에서 이미 RabbitMQ를 실행 중이라면, 최초 `infra.sh up` 전에 기존 RabbitMQ가 점유한 5672/15672 포트를 비워야 한다. 자동 삭제하지 않으며, 아래로 먼저 확인한다.

```bash
docker ps --filter name=puppyrun-rabbitmq
```

## 최초 1회 설정

```bash
git clone --branch dev https://github.com/2026-SubProject-PuppyRun/BackEnd.git
cd BackEnd
sudo bash .github/workflows/deployment/bootstrap-ec2.sh
```

bootstrap은 스크립트·Compose·공유 네트워크와 아래 템플릿을 설치한다. 기존 운영 설정 파일은 덮어쓰지 않는다.

```text
.github/workflows/deployment/
├── bootstrap-ec2.sh       # EC2 최초 설치 진입점
├── scripts/               # deploy, rollback, infra, health check, cleanup
├── compose/               # backend/infra Compose, Grafana Alloy 설정
└── env/                   # EC2에 최초 생성할 환경변수 템플릿
```

```bash
sudoedit /home/ubuntu/puppyrun/config/deploy.env  # AWS/ECR와 Compose 프로젝트 이름
sudoedit /home/ubuntu/puppyrun/config/app.env     # Spring Boot와 RabbitMQ 설정
sudoedit /home/ubuntu/puppyrun/config/infra.env   # Grafana Cloud remote_write 인증값
```

Firebase 서비스 계정 JSON도 EC2에만 저장한다.

```bash
sudo install -o root -g ubuntu -m 640 \
  /home/ubuntu/firebase-service-account.json \
  /home/ubuntu/puppyrun/config/firebase-service-account.json
```

`app.env`에는 다음을 유지한다.

```dotenv
FCM_ACCOUNT_PATH=file:/run/secrets/firebase-service-account.json
```

## 실행 순서

처음에는 infra를 한 번 올린 후 GitHub Actions 배포를 실행한다.

```bash
sudo /home/ubuntu/puppyrun/scripts/infra.sh up
sudo /home/ubuntu/puppyrun/scripts/infra.sh status
```

이후 GitHub Actions는 ECR push 후 backend 배포만 실행한다. 수동 배포와 rollback도 backend에만 영향을 준다.
후보 이미지는 로컬 `puppyrun-runtime:candidate`로 실행해 검증하고, 성공한 경우에만 `current`로 승격한다.
실패하면 ECR에서 다시 받지 않고 로컬 `current` 이미지로 즉시 복구한다. `previous`는 수동 rollback용으로 보관한다.

```bash
sudo /home/ubuntu/puppyrun/scripts/deploy.sh <github-sha>
sudo /home/ubuntu/puppyrun/scripts/rollback.sh
```

## 설정 변경

- `app.env` 또는 `infra.env`를 수정한 뒤에는 아래 한 명령으로 두 Compose에 반영한다. backend 이미지는 바뀌지 않는다.

  ```bash
  sudo /home/ubuntu/puppyrun/scripts/apply-config.sh
  ```

  backend health check가 실패하면 마지막으로 성공한 `app.env`로 자동 복원한다.
- backend 배포·rollback: RabbitMQ 데이터 volume과 Alloy 컨테이너는 유지된다.

배포 로그는 `/home/ubuntu/puppyrun/logs/latest-deploy.log`에서 확인한다. infra 로그는 `sudo /home/ubuntu/puppyrun/scripts/infra.sh logs`로 확인한다.
