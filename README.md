# PuppyRun Backend

반려견과의 산책을 기록하고, 건강 변화와 활동 통계를 확인하며, 날씨와 생활 패턴에 맞는 산책 알림을 받을 수 있는 **반려견 산책 관리 서비스**의 백엔드입니다.

PuppyRun은 GPS 산책 경로, 거리, 시간, 페이스, 휴식 구간과 사진을 기록합니다. 기록된 데이터는 반려견별 활동 통계와 산책 일기로 연결되며, 최근 산책 지역과 시간대를 분석해 날씨 기반 맞춤형 푸시 알림을 제공합니다.

## 주요 기능

### 반려견 관리

- 반려견 기본 정보 등록, 조회, 수정, 삭제
- 견종별 기본 색상과 기준 몸무게 제공
- 프로필 이미지 등록 및 기본 이미지 전환
- 몸무게 변경 이력과 누적 산책 거리 조회

### 산책 기록

- 산책 시작·종료 시각, 거리, 평균 페이스, 휴식 시간 저장
- GPS 원본 경로와 공간 분석용 `Point`, `LineString` 데이터 저장
- 여러 반려견과 하나의 산책 기록 연결
- 산책 사진 업로드와 공개 범위 관리
- 산책 목록, 상세 조회, 수정, 삭제

### 산책 일기

- 산책 기록과 연결된 일기 작성
- 제목, 내용, 작성 시각, 날씨, 사진 저장
- 산책 경로를 포함한 일기 상세 조회
- 작성자 소유권 검증과 일기 수정·삭제

### 활동 통계

- 일별 산책 내역과 하루 누적 거리·시간·횟수 제공
- 최근 7일 활동 차트와 반려견별 주간 비교 제공
- 반려견별 거리, 시간, 속도, 빈도, 휴식 시간 분석
- 연간 월별 요약과 월간 기여도 차트 제공
- 반려견 몸무게 변화 이력 제공

### 날씨 및 맞춤 알림

- 기상청 API 기반 현재 날씨와 예보 조회
- 지역별 날씨 데이터를 Caffeine Cache에 주기적으로 갱신
- Firebase Cloud Messaging 기반 푸시 알림 발송
- 전체·유형별 알림 동의와 FCM 토픽 구독 관리
- 최근 30일 산책 기록을 분석해 평일·주말 선호 시간대 계산
- 최근 산책 지역, 선호 시간대, 예보를 조합한 산책 가이드 발송

## 서비스 구조

![데이터플로우](./assets/data_folow.png)

## 패키지 구조

```text
org.zerock.puppyrun
├── PuppyRunApplication.java
├── auth
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── DTO
│   └── service
│       └── 회원가입, 로그인, JWT 발급·재발급
├── member
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── DTO
│   ├── entity
│   ├── exception
│   ├── repository
│   └── service
│       └── 계정 조회, 비밀번호·닉네임 변경
├── pet
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── DTO
│   ├── entity
│   ├── repository
│   └── service
│       └── 반려견 정보, 프로필, 몸무게 이력 관리
├── tracking
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── DTO
│   ├── entity
│   ├── repository
│   ├── service
│   └── util
│       └── 산책 기록, GPS 경로, 반려견 연결, 페이스 변환
├── diary
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── DTO
│   ├── entity
│   ├── repository
│   └── service
│       └── 산책 일기와 사진·날씨 정보 관리
├── statistics
│   ├── controller
│   │   └── Response
│   ├── DTO
│   └── service
│       └── 일간·주간·월간 및 반려견별 통계 집계
├── weather
│   ├── controller
│   │   └── response
│   ├── DTO
│   ├── exception
│   └── service
│       └── 기상청 API 호출, 응답 변환, 지역별 예보 조회
├── notification
│   ├── client
│   ├── controller
│   │   ├── request
│   │   └── response
│   ├── entity
│   ├── repository
│   ├── sender
│   └── service
│       └── 알림 설정, 산책 패턴 분석, FCM 발송
└── common
    ├── auth
    │   ├── jwt
    │   └── security
    ├── config
    ├── entity
    ├── exception
    ├── init
    ├── s3
    │   ├── rollback
    │   └── support
    └── scheduler
        └── 공통 설정, 인증, 예외 처리, 파일 저장, 배치 작업
```

### 패키지 설계 특징

| 구분 | 설명 |
|---|---|
| 도메인 중심 구성 | 인증, 회원, 반려견, 산책, 일기, 통계, 날씨, 알림을 기능 단위로 분리합니다. |
| 계층 분리 | 각 도메인에서 API, 비즈니스 로직, 영속성 책임을 `controller`, `service`, `repository`로 나눕니다. |
| Command/Query 분리 | 변경과 조회 흐름이 복잡한 `pet`, `tracking` 도메인은 Command와 Query 서비스를 분리합니다. |
| 공통 관심사 분리 | JWT, Security, S3, 캐시, 비동기 실행, 스케줄링, 전역 예외 처리를 `common`에서 관리합니다. |
| 통계 전용 조회 | 대량 집계와 기간별 통계는 QueryDSL 프로젝션으로 필요한 데이터만 조회합니다. |
| 공간 데이터 활용 | 산책 경로를 원본 JSON과 MySQL Spatial 데이터로 함께 저장합니다. |

##  ERD

![ERD 사진](./assets/erd.png)

- `Member`는 여러 반려견과 산책 기록, 일기를 소유합니다.
- `PetTracking`은 하나의 산책에 여러 반려견이 참여할 수 있도록 연결합니다.
- `TrackingRoute`는 산책 원본 좌표와 공간 검색용 경로를 함께 보관합니다.
- `Diary`는 산책과 연결되지만, 산책이 삭제된 뒤에도 독립적으로 유지될 수 있습니다.
- `WalkingPreference`는 최근 산책 위치와 평일·주말 선호 시간대를 저장합니다.
- `WalkingPreference`는 회원의 최근 산책 지역과 평일·주말 선호 산책 시간대를 저장하여, 개인화된 산책 추천과 알림 기능에 활용할 수 있도록 구성했습니다.
- `NotificationSettings`는 회원별 알림 설정 정보를 관리하며, FCM 토큰, 활성화 여부, 푸시 알림 동의 여부를 저장합니다.

- `NotificationOptOuts`는 알림 타입별 수신 거부 정보를 관리하여, 사용자가 특정 유형의 알림만 선택적으로 비활성화할 수 있도록 설계했습니다.
## 기술 스택

| 영역 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Web | Spring MVC, WebClient, Bean Validation |
| Security | Spring Security, JWT, BCrypt |
| Persistence | Spring Data JPA, QueryDSL 5, Hibernate Spatial |
| Database | MySQL, H2 for test |
| Cache | Spring Cache, Caffeine |
| Storage | AWS S3 |
| Push | Firebase Admin SDK, FCM |
| Async/Batch | Spring Async, Spring Scheduler, CompletableFuture |
| Build/Deploy | Gradle, Docker, Docker Compose |
| Logging | Logback, Discord Appender |


## 커밋 컨벤션

| 커밋 타입 | 설명 | 예시 커밋 메시지 |
|:---:|---|---|
| `Feat` | 새로운 기능 추가 | `Feat: 토큰 재발급 API 구현 (#15)` |
| `Fix` | 버그 수정 | `Fix: JWT 만료 예외 응답 수정` |
| `Refactor` | 기능 변경 없는 구조 개선 | `Refactor: 로그인 로직 메서드 분리` |
| `Test` | 테스트 코드 추가 및 개선 | `Test: 회원 조회 단위 테스트 추가` |
| `Docs` | 문서 수정 | `Docs: README 서비스 구조 추가` |
| `Chore` | 빌드 및 설정 변경 | `Chore: QueryDSL 의존성 추가` |
| `Infra` | 서버, Docker, CI/CD 변경 | `Infra: 배포용 Docker 설정 추가` |
| `Style` | 코드 포맷 및 오타 수정 | `Style: 코드 포맷 정리` |
| `Rename` | 파일 또는 디렉터리 이름 변경 | `Rename: MemberDto 이름 변경` |
| `Remove` | 사용하지 않는 코드 삭제 | `Remove: 미사용 로그 제거` |
