# `getTrackingSummaryByPetId` JPA-only 성능 비교

## 결론

QueryDSL을 제거하더라도 JPA Criteria API로 `SUM`, `COUNT`, `AVG`를 DB에서 계산하면 성능 저하는 없었다. 이번 데이터와 쿼리 구조에서는 오히려 현재 QueryDSL 쿼리보다 약 40~50% 빨랐다.

반면 JPA로 `PetTracking`과 `Tracking` 엔티티를 모두 조회한 뒤 Java에서 합산하면 조회 건수가 늘어날수록 급격히 느려졌다. 10만 건에서 577.334ms로, DB 집계를 사용하는 JPA Criteria 방식(80.461ms)보다 약 7.2배 느렸다.

이 결과는 QueryDSL과 JPA 라이브러리 자체의 우열이 아니다. 생성되는 SQL과 집계를 수행하는 위치의 차이다.

## 측정 조건

- DB: MySQL 8.4 Testcontainers
- 데이터: `tracking` 100,000건, `pet_tracking` 100,000건, 펫 5마리
- 기간 조건에 일치하는 데이터: 10,000 / 50,000 / 100,000건
- 각 방식마다 2회 워밍업 후 5회 실행한 중앙값
- SQL 출력 비활성화
- 결과 DTO의 거리, 시간, 횟수, 평균 pace, 휴식 시간이 모두 같은지 검증
- 로컬 환경: Apple M5, RAM 24GB, Docker Desktop 할당 메모리 약 7.8GB, 테스트 JVM Java 21 / 최대 힙 512MB

## 결과

단위는 ms이다.

| 기간 내 일치 건수 | 현재 QueryDSL DB 집계 (1 query) | JPA Criteria DB 집계 (2 queries) | JPA 엔티티 조회 + Java 집계 (2 queries) |
|---:|---:|---:|---:|
| 10,000 | 129.680 | 65.222 | 125.717 |
| 50,000 | 125.650 | 67.718 | 317.487 |
| 100,000 | 134.585 | 80.461 | 577.334 |

독립적으로 한 번 더 실행한 10만 건 결과는 각각 135.928ms, 81.789ms, 580.360ms로 비슷했다.

## 구현별 차이

### 현재 QueryDSL

펫을 기준으로 `pet_tracking`을 left join하고, 기간 조건은 `tracking` left join의 `ON` 절에 둔다. 산책 기록이 없는 펫도 0으로 반환하는 장점이 있지만, 선택한 펫의 `pet_tracking` 전체를 조인한 뒤 기간을 판별한다. 그래서 기간에 일치하는 행을 1만 건에서 10만 건으로 늘려도 실행 시간이 약 125~135ms로 거의 일정했다. 테스트 테이블 전체 10만 건을 계속 처리하기 때문이다.

### JPA Criteria DB 집계

현재 엔티티에는 `Pet -> PetTracking` 역방향 컬렉션이 없다. 같은 반환 동작을 만들기 위해 다음 두 쿼리를 사용했다.

1. 선택한 펫 메타데이터 조회
2. `PetTracking -> Tracking` inner join 후 DB에서 합계/횟수/평균 집계

쿼리는 한 번 더 실행하지만 큰 결과 집합을 애플리케이션으로 전송하지 않아 가장 빨랐다. 기록이 없는 펫은 첫 번째 조회 결과를 이용해 0으로 채울 수 있다.

### JPA 엔티티 조회 + Java 집계

N+1을 피하기 위해 fetch join을 사용했지만, 조건에 맞는 `PetTracking` 및 `Tracking` 엔티티를 모두 생성하고 영속성 컨텍스트에서 관리해야 한다. 10만 건에서는 약 20만 개의 주요 엔티티 객체를 생성하므로 시간과 힙 사용량이 함께 증가한다.

## 권장안

- QueryDSL을 제거해야 한다면 Spring Data JPA의 JPQL/Criteria projection으로 DB 집계를 유지한다.
- repository에서 엔티티 목록을 받은 뒤 stream으로 합산하는 구현은 피한다.
- 현재 QueryDSL도 기간 조건을 더 일찍 줄일 수 있는 쿼리 구조와 인덱스를 검토할 가치가 있다.
- JPQL이 현재 QueryDSL과 완전히 동일한 SQL을 생성한다면 실행 시간 역시 거의 같다고 보는 것이 맞다.

## 재실행

Docker Desktop이 실행 중이어야 한다.

```shell
PET_TRACKING_BENCHMARK_ENABLED=true \
  ./gradlew test --tests '*PetTrackingSummaryPerformanceTest'
```

성능 테스트는 일반 테스트 실행 시간을 늘리지 않도록 환경 변수가 없으면 자동으로 skip된다.
