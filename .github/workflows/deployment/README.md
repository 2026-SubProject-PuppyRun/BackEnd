# EC2 운영 배포 스크립트

GitHub Actions는 이미지를 ECR에 불변 태그로 push한 뒤, 비밀값 없는 릴리스 번들을 S3에 업로드한다. EC2는 번들을 후보 릴리스 디렉터리에 풀고 `.env`를 EC2 로컬에서 복사한 뒤 배포한다.

```bash
sudo install -d -m 700 /home/ubuntu/puppyrun/config /home/ubuntu/puppyrun/releases
sudo install -m 600 .env /home/ubuntu/puppyrun/config/.env
```

각 후보 릴리스는 `/home/ubuntu/puppyrun/releases/<release-tag>/`에 생성되며, `.env`, `docker-compose.deploy.yml`, `deploy.sh`, `rollback.sh`, `health-check.sh`, 이미지 digest·배포 시각·파일 SHA-256이 든 `metadata.env`를 가진다. `new`, `current`, `previous` 링크가 각각 후보·현재 성공·직전 성공 릴리스를 가리킨다. 후보 health check가 실패하면 `current`의 이미지·환경·Compose로 즉시 재기동하며 링크는 바꾸지 않는다. 성공 시에만 `previous ← current`, `current ← new`로 승격하고 더 오래된 previous 릴리스는 정리한다.

```bash
AWS_REGION=ap-northeast-2 AWS_ACCOUNT_ID=<account-id> bash /home/ubuntu/puppyrun/current/rollback.sh
```

자동 롤백 알림은 배포 후 제한된 시간 창에서, 5xx 비율 또는 컨테이너 재시작과 readiness 실패가 2~3분 연속으로 발생할 때만 연결한다. ECR digest와 EC2의 `current`/`previous` 릴리스가 롤백의 정본이며, `.env`는 S3에 업로드하지 않는다.

## Prometheus / Grafana Cloud

애플리케이션 메트릭은 `http://127.0.0.1:8081/actuator/prometheus`에서 제공된다. 배포 Compose는 8081을 EC2 loopback에만 바인딩하므로, Grafana Cloud에 연결한 Alloy 또는 Grafana Agent를 EC2에서 실행하고 `127.0.0.1:8081`을 scrape 대상으로 설정한다. 이 포트를 보안 그룹이나 공인 인터넷에 노출하지 않는다.
