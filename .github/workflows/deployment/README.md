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

bootstrap은 EC2 운영 디렉터리, Git 저장소와 공유 네트워크만 초기화한다. 운영 Compose와
스크립트는 설치하지 않으며, 설정 파일도 생성하거나 덮어쓰지 않는다.

`deployment/compose/` 또는 `deployment/scripts/`가 변경된 `dev`·`cd-test` push는
별도 GitHub Actions를 실행한다. 이 작업은 deployment 폴더의 `compose`와 `scripts`만으로
만든 `ec2-deployment` 브랜치를 갱신한다. EC2의
`puppyrun` 디렉터리는 이 브랜치를 직접 pull한 작업 트리이므로 파일별 설치나 복사가
필요 없다. `config/`, `locks/`, `logs/`, `state/`와 환경변수·Firebase 서비스 계정 파일은
Git 추적 대상이 아니므로 EC2에서만 관리한다.

최초 설치 후에는 GitHub Actions의 **Sync deployment configuration to EC2**를 한 번
수동 실행해 `ec2-deployment` 브랜치를 만들고 EC2에 checkout한다.

배포 디렉터리는 `puppyrun`으로 고정하며, 최초 동기화 때 자동 생성된다.

```text
.github/workflows/deployment/
├── bootstrap-ec2.sh       # EC2 최초 설치 진입점
├── scripts/               # deploy, rollback, infra, health check, cleanup
├── compose/               # backend/infra Compose, Grafana Alloy 설정
└── README.md               # EC2 운영 안내
```

```bash
sudoedit puppyrun/config/deploy.env  # AWS/ECR와 Compose 프로젝트 이름
sudoedit puppyrun/config/app.env     # Spring Boot와 RabbitMQ 설정
sudoedit puppyrun/config/infra.env   # Grafana Cloud remote_write 인증값
```

Firebase 서비스 계정 JSON도 EC2에만 저장한다.

```bash
sudo install -o root -g ubuntu -m 640 \
  ./firebase-service-account.json \
  puppyrun/config/firebase-service-account.json
```

`app.env`에는 다음을 유지한다.

```dotenv
FCM_ACCOUNT_PATH=file:/run/secrets/firebase-service-account.json
```

## 실행 순서

처음에는 infra를 한 번 올린 후 GitHub Actions 배포를 실행한다.

```bash
sudo puppyrun/scripts/infra.sh up
sudo puppyrun/scripts/infra.sh status
```

이후 GitHub Actions는 ECR push 후 backend 배포만 실행한다. 수동 배포와 rollback도 backend에만 영향을 준다.
후보 이미지는 로컬 `puppyrun-runtime:candidate`로 실행해 검증하고, 성공한 경우에만 `current`로 승격한다.
실패하면 ECR에서 다시 받지 않고 로컬 `current` 이미지로 즉시 복구한다. `previous`는 수동 rollback용으로 보관한다.

```bash
sudo puppyrun/scripts/deploy.sh <github-sha>
sudo puppyrun/scripts/rollback.sh
```

## 설정 변경

- `app.env` 또는 `infra.env`를 수정한 뒤에는 아래 한 명령으로 두 Compose에 반영한다. backend 이미지는 바뀌지 않는다.

  ```bash
  sudo puppyrun/scripts/apply-config.sh
  ```

  backend health check가 실패하면 마지막으로 성공한 `app.env`로 자동 복원한다.
- backend 배포·rollback: RabbitMQ 데이터 volume과 Alloy 컨테이너는 유지된다.

배포 로그는 `puppyrun/logs/latest-deploy.log`에서 확인한다. infra 로그는 `sudo puppyrun/scripts/infra.sh logs`로 확인한다.
