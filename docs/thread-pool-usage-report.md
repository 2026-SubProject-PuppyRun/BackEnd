# PuppyRun 스레드풀 사용 분석

## 1. 요약

PuppyRun은 하나의 스레드풀만 사용하는 구조가 아니다. 현재 코드에는 다음과 같이 서로 다른 비동기 실행 기반이 함께 존재한다.

| 구분 | 실행 기반 | 주요 용도 | 호출 측 대기 여부 |
|---|---|---|---|
| 알림 작업 | Spring `ThreadPoolTaskExecutor` (`notificationTaskExecutor`) | 알림 발송 진입, FCM 결과 콜백 | 대기하지 않음 |
| S3 비동기 삭제 | Spring `SimpleAsyncTaskExecutor` 폴백 | 파일별 새 스레드에서 삭제 | 대기하지 않음 |
| 통계 병렬 조회 | JVM `ForkJoinPool.commonPool()` | 주간 통계 3개, 월간 통계 2개 병렬 조회 | `join()`으로 대기 |
| S3 다중 업로드 | JVM `ForkJoinPool.commonPool()` | 파일별 병렬 업로드 | `join()`으로 대기 |
| 정기 작업 | Spring Scheduler의 기본 스케줄러 | 날씨 갱신, 산책 리마인드 시작 | 작업 형태에 따라 즉시 반환 |
| 날씨 비동기 I/O | Reactor/Netty 이벤트 루프 | WebClient HTTP 요청과 응답 처리 | 스케줄러에서는 비동기, 일반 조회에서는 `block()` |
| 운영 오류 전송 | Logback `AsyncAppender`의 워커 | ERROR 로그를 Discord로 전송 | 애플리케이션 스레드는 대기하지 않음 |

애플리케이션이 직접 크기를 설정한 풀은 `notificationTaskExecutor` 하나다. 반면 `CompletableFuture.supplyAsync()`에는 실행자를 전달하지 않아 JVM 공용 풀을 암묵적으로 사용하며, 실행자 이름이 없는 S3 `@Async`는 실행자 선택이 모호해져 `SimpleAsyncTaskExecutor`로 폴백한다. 따라서 알림, S3 삭제, 통계/S3 업로드는 서로 다른 실행 기반을 사용한다.

## 2. 알림 전용 ThreadPoolTaskExecutor

`AsyncConfig`는 `@EnableAsync`로 Spring 비동기 실행을 활성화하고 `notificationTaskExecutor`를 빈으로 등록한다.

```java
executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
executor.setQueueCapacity(50);
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
executor.setThreadNamePrefix("FCM-Noti-");
```

설정의 의미는 다음과 같다.

- 기본 스레드 수는 서버가 인식한 CPU 코어 수(`N`)다.
- 코어 스레드가 모두 사용 중이면 작업을 최대 50개까지 큐에 쌓는다.
- 큐까지 가득 찬 뒤에만 스레드를 최대 `2N`개까지 늘린다.
- 최대 스레드와 큐가 모두 포화되면 `CallerRunsPolicy`가 작업 제출 스레드에서 직접 실행한다. 작업 유실을 막고 자연스러운 역압을 주지만, 스케줄러나 Firebase 완료 스레드가 직접 작업을 수행해 일시적으로 느려질 수 있다.
- 작업 스레드 이름은 `FCM-Noti-`로 시작하므로 로그에서 실행 주체를 식별할 수 있다.

### 2.1 알림 발송 흐름

매일 20시에 `WalkingRemindScheduler`가 `NotificationProcessor.broadcast()`를 호출한다. 이 메서드에는 `@Async("notificationTaskExecutor")`가 지정되어 있어 스케줄러 스레드는 실제 발송을 기다리지 않고 반환한다.

```text
Spring Scheduler
  → NotificationProcessor 프록시
  → notificationTaskExecutor
  → 수신 가능 회원 1,000명 단위 조회
  → 사용자별 PushTask 생성
  → FCM 요청 500건 단위 제출
```

`NotificationProcessor`는 회원을 1,000명씩 커서 기반으로 조회하고, `NotificationEventClient`는 이를 다시 FCM 제한에 맞춰 500건씩 나눈다. FCM 전송 자체는 Firebase SDK의 `sendEachAsync()`가 수행한다. `notificationTaskExecutor`는 Firebase 네트워크 요청 그 자체보다 `ApiFuture`가 완료된 뒤 성공/실패 콜백을 실행하는 데 사용된다.

콜백에서는 전송 결과를 기록하고, 실패한 FCM 토큰을 모아 DB에서 비활성화한다. 토픽 메시지 발송과 토픽 구독·해제 결과 콜백도 동일한 실행자를 사용한다.

즉, 이 풀에는 다음 두 종류의 작업이 함께 들어온다.

1. `NotificationProcessor`의 발송 대상 조회 및 메시지 생성 작업
2. Firebase 비동기 요청이 끝난 뒤 실행되는 결과 처리 작업

한 번의 회원 조회 결과가 1,000명이면 FCM 요청은 최대 두 묶음으로 제출된다. 프로세서는 각 FCM 결과를 기다리지 않고 다음 회원 페이지를 계속 처리하므로 대상자가 많을 때 결과 콜백이 큐에 누적될 수 있다.

### 2.2 일반 API에서의 FCM 작업

알림 설정 변경, 전체 알림 동의, 토큰 갱신 시 발생하는 토픽 구독·해제도 Firebase SDK의 비동기 API를 사용한다. 요청 스레드는 비동기 요청을 등록한 뒤 반환하고, 완료 콜백은 `notificationTaskExecutor`에서 처리된다.

단, `validateFcmToken()`은 Firebase의 동기 `send(message, true)`를 사용하므로 이 검증은 API 요청 스레드를 점유한다.

## 3. S3 작업에서의 사용

### 3.1 다중 업로드: ForkJoinPool.commonPool

`S3Service.uploadAll()`은 파일마다 `CompletableFuture.supplyAsync()`를 생성한다. 두 번째 인자로 `Executor`를 전달하지 않았기 때문에 `notificationTaskExecutor`가 아니라 JVM 전역의 `ForkJoinPool.commonPool()`에서 실행된다.

```text
HTTP 요청 스레드
  → 파일 수만큼 commonPool에 업로드 제출
  → 각 파일을 S3에 병렬 업로드
  → 요청 스레드가 모든 Future를 join()
  → 롤백 보상 이벤트 등록
  → 응답 진행
```

업로드는 병렬이지만 메서드 자체는 비동기 API가 아니다. 호출한 HTTP 요청 스레드는 모든 업로드가 끝날 때까지 `join()`에서 대기한다. 이 방식은 소수 파일의 전체 업로드 시간을 줄일 수 있지만, 블로킹 네트워크 I/O를 CPU 작업 중심의 공용 ForkJoinPool에 넣고 있어 통계 조회 등 다른 공용 풀 작업과 서로 영향을 줄 수 있다.

일부 파일 업로드가 실패해도 `CompletableFuture.allOf()`로 나머지 작업의 완료를 기다린다. 이후 성공한 파일 Key만 롤백 이벤트에 등록하고 원래 예외를 다시 던지므로, 외부 DB 트랜잭션이 롤백될 때 부분 성공한 S3 객체도 보상 삭제 대상이 된다.

### 3.2 삭제: Spring @Async

`delete()`와 `deleteAll()`에는 이름을 지정하지 않은 `@Async`가 붙어 있다. 겉으로는 프로젝트가 직접 만든 `notificationTaskExecutor`를 사용할 것처럼 보이지만, 실제 자동 구성까지 포함하면 그렇지 않다.

Spring Boot는 `@Scheduled`를 위해 `taskScheduler`라는 `ThreadPoolTaskScheduler` 빈도 만든다. 이 클래스 역시 `TaskExecutor` 타입이므로 Spring Async가 기본 실행자를 찾을 때 `notificationTaskExecutor`와 `taskScheduler` 두 후보가 발견된다. 유일한 후보가 없고 `taskExecutor`라는 이름의 빈도 없기 때문에 Spring 6.1의 `AsyncExecutionInterceptor`는 `SimpleAsyncTaskExecutor`를 생성해 폴백한다.

따라서 현재 S3 삭제는 `notificationTaskExecutor`의 코어 수, 최대 수, 큐 50, `CallerRunsPolicy` 설정을 적용받지 않는다. `SimpleAsyncTaskExecutor`는 스레드풀이 아니라 기본적으로 작업마다 새 플랫폼 스레드를 생성하고 동시성 제한도 두지 않는다. 삭제 요청이 한꺼번에 증가하면 생성되는 스레드 수도 함께 증가할 수 있다.

주요 호출 지점은 다음과 같다.

- 펫 기본 프로필 변경 후 기존 이미지 삭제
- 일기 삭제 후 첨부 이미지 삭제
- 산책 기록 수정·삭제 후 이미지 삭제
- DB 트랜잭션 롤백 이벤트 처리 후 보상 삭제

호출자는 S3 삭제 완료를 기다리지 않는다. 다만 정상적인 DB 삭제 트랜잭션 안에서 곧바로 비동기 S3 삭제를 제출하는 경로는 이후 DB 트랜잭션이 롤백되어도 이미 파일이 삭제될 수 있다. 정상 삭제도 커밋 이후 이벤트로 실행하도록 맞추면 정합성이 더 명확해진다.

`@Async`는 Spring 프록시를 거쳐 호출될 때만 동작한다. 현재 호출 지점은 다른 서비스나 이벤트 핸들러에서 주입받은 `S3Service`를 호출하므로 프록시가 적용된다. 같은 클래스 내부에서 `this.delete()`처럼 호출하면 비동기가 적용되지 않는다.

## 4. 통계 조회에서의 병렬 처리

`TrackingActivityService`는 서로 독립적인 DB 조회를 `CompletableFuture`로 동시에 실행한다.

### 4.1 주간 통계

다음 세 작업을 `ForkJoinPool.commonPool()`에 제출한다.

1. 최근 7일 산책 차트 조회
2. 이번 주 반려견별 산책 통계 조회
3. 지난주 반려견별 산책 통계 조회

`CompletableFuture.allOf(...).join()`으로 세 작업이 모두 끝날 때까지 기다린 후 응답을 조립한다. 직렬로 세 쿼리를 실행하는 것보다 지연 시간을 줄일 수 있지만, HTTP 요청 스레드는 결과가 준비될 때까지 계속 대기한다.

### 4.2 월간 통계

다음 두 작업을 같은 방식으로 병렬 실행한다.

1. 연초부터 대상 월 말일까지의 월별 기록 조회 및 그룹화
2. 최근 15주 기여도 조회

두 Future가 끝나면 `thenApply()`에서 결과를 합치고 마지막 `join()`으로 동기 응답을 반환한다.

외부 `TrackingActivityService`의 읽기 전용 트랜잭션은 요청 스레드에 귀속되며 commonPool 스레드로 전파되지 않는다. 다만 병렬 작업에서 호출하는 `TrackingStatistics`, `PetStatistics`, Spring Data Repository는 각각 Spring 프록시를 통과하므로 각 작업 스레드에서 별도의 읽기 트랜잭션/영속성 컨텍스트를 사용한다. 결과적으로 한 API 요청이 동시에 여러 DB 커넥션을 사용할 수 있다.

## 5. 스케줄러와 Reactor 실행 모델

`@Scheduled` 작업은 두 개지만 별도 스케줄러 풀 크기는 설정하지 않았다. 따라서 Spring Boot의 기본 스케줄링 실행자를 사용하며 기본 풀 크기는 1이다.

- `WalkingRemindScheduler`는 알림 작업을 `notificationTaskExecutor`에 넘기고 빠르게 반환한다.
- `WeatherScheduler`는 Reactor 파이프라인을 만든 뒤 `subscribe()`하고 반환한다. 지역별 지연과 WebClient 통신은 이후 Reactor 스케줄러 및 Netty 이벤트 루프에서 진행된다.

따라서 현재 두 스케줄 작업의 본체는 기본 스케줄러를 오래 점유하지 않는다. 다만 앞으로 `@Scheduled` 메서드에 오래 걸리는 동기 작업이 추가되면 단일 스케줄러 스레드 때문에 다른 정기 작업 시작이 밀릴 수 있다.

일반 날씨 조회의 `WeatherService.getRegionalWeather()`는 같은 WebClient를 사용하지만 마지막에 `block()`을 호출한다. 이 경로에서는 네트워크 I/O는 Netty 이벤트 루프가 처리하더라도 HTTP 요청 스레드는 응답이 올 때까지 대기한다.

## 6. Logback 비동기 처리

운영 프로필에서는 ERROR 로그를 `ASYNC_DISCORD` appender로 전달한다. Logback `AsyncAppender`가 내부 큐와 별도 워커 스레드로 Discord 전송을 처리하므로 예외를 기록한 요청 스레드가 웹훅 통신 완료를 기다리지 않는다.

이 실행 기반은 Spring의 `notificationTaskExecutor`나 JVM commonPool과는 별개이며 애플리케이션 코드에서 크기를 관리하는 스레드풀도 아니다. 현재 큐 크기나 폐기 정책을 별도로 설정하지 않아 Logback 기본값을 사용한다.

## 7. 전체 실행 흐름

```text
HTTP 요청(Tomcat 요청 스레드)
  ├─ 통계 API ─→ ForkJoinPool.commonPool ─→ 병렬 DB 조회 ─→ join ─→ 응답
  ├─ 다중 업로드 ─→ ForkJoinPool.commonPool ─→ 병렬 S3 업로드 ─→ join ─→ 응답
  ├─ S3 삭제 ─→ SimpleAsyncTaskExecutor ─→ 작업별 스레드에서 삭제
  └─ FCM 설정 ─→ Firebase 비동기 요청 ─→ notificationTaskExecutor에서 결과 콜백

Spring Scheduler
  ├─ 산책 리마인드 ─→ notificationTaskExecutor ─→ 대상 조회/FCM 요청
  └─ 날씨 갱신 ─→ Reactor/Netty 이벤트 루프 ─→ 캐시 갱신

ERROR 로그
  └─ Logback AsyncAppender 워커 ─→ Discord Webhook
```

## 8. 현재 구조의 장점과 주의점

### 장점

- 대량 알림과 S3 삭제를 호출 스레드에서 분리해 응답 및 스케줄러 점유 시간을 줄였다.
- 알림 풀에 제한된 큐와 `CallerRunsPolicy`를 적용해 무제한 작업 적재와 조용한 작업 유실을 방지했다.
- 독립적인 통계 쿼리와 파일 업로드를 병렬화해 단일 요청의 총 처리 시간을 줄일 수 있다.
- 알림, S3 삭제, Reactor I/O, Logback이 서로 다른 실행 기반을 사용한다.

### 주의점 및 개선 우선순위

1. **블로킹 I/O용 풀 분리**: 통계 DB 조회와 S3 업로드가 공용 ForkJoinPool을 공유한다. `statisticsTaskExecutor`, `s3TaskExecutor`처럼 용도별 bounded executor를 만들고 `supplyAsync(..., executor)`로 명시하는 것이 안전하다.
2. **S3 삭제 풀 명시**: bounded `s3TaskExecutor`를 등록하고 `@Async("s3TaskExecutor")`처럼 실행자를 명시해야 현재의 무제한 스레드 생성 가능성과 향후 빈 구성에 따른 동작 변화를 제거할 수 있다.
3. **DB 풀과 동시성 조율**: 통계 요청 하나가 2~3개의 DB 작업을 동시에 만들므로 스레드풀 크기를 HikariCP 커넥션 수와 함께 결정해야 한다. 요청이 많으면 단일 요청 지연은 줄어도 전체 DB 부하는 커질 수 있다.
4. **트랜잭션 이후 삭제**: 정상 S3 삭제도 `AFTER_COMMIT` 이벤트로 옮기면 DB 롤백 시 파일만 사라지는 상황을 줄일 수 있다.
5. **업로드 보상 내구성**: 다중 업로드 부분 실패도 보상 대상으로 등록되지만 이벤트가 메모리 기반이므로, 프로세스 종료까지 견뎌야 한다면 별도의 정리 배치나 영속적인 보상 작업 저장소가 필요하다.
6. **컨텍스트 전파**: 현재 TaskDecorator가 없어 MDC, SecurityContext 같은 ThreadLocal 정보는 비동기 스레드로 자동 전파되지 않는다. 비동기 로그 추적 ID가 필요하면 전파 구성이 필요하다.
7. **관측 지표 추가**: 활성 스레드 수, 큐 크기, 거절 횟수, 작업 대기 시간과 처리 시간을 메트릭으로 노출해야 실제 부하에 맞춰 풀 크기를 조정할 수 있다.

## 9. 결론

PuppyRun의 스레드 사용 전략은 알림의 완전 비동기 처리와 통계·업로드의 요청 내부 병렬 처리로 나뉜다. 알림은 크기가 제한된 Spring 전용 풀을 명시적으로 사용해 비교적 통제되어 있다. 반면 통계 조회와 S3 다중 업로드는 JVM 공용 ForkJoinPool에 의존하며, S3 삭제는 이름 없는 `@Async` 때문에 `SimpleAsyncTaskExecutor`로 폴백한다.

따라서 현재 구조를 한 문장으로 요약하면 **“알림은 전용 풀로 분리했지만, DB/S3 병렬 작업은 공용 풀에 남아 있고 S3 삭제는 관리되지 않는 작업별 스레드 실행으로 폴백하는 상태”**다. 다음 개선에서는 작업 성격별 bounded executor 분리, DB 커넥션 풀과의 동시성 정합, 트랜잭션 커밋 이후 파일 삭제를 우선 적용하는 것이 적절하다.
