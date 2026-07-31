# Test Code Generation Convention

Codex 에이전트가 PuppyRun 테스트를 작성·수정·검토할 때 적용하는 핵심 규칙입니다.

## 1. 작업 순서

1. `git status --short`로 기존 변경을 확인합니다.
2. 대상 Service와 연결된 Repository, DTO, 예외를 읽습니다.
3. 기존 테스트와 `src/testFixtures`에서 재사용할 코드를 찾습니다.
4. 아래 기준으로 테스트 유형을 선택합니다.
5. 기능 단위의 `given / when / then` 시나리오를 작성합니다.
6. 대상 테스트를 실행하고 영향 범위가 넓으면 전체 테스트를 실행합니다.

Production 로직을 읽지 않고 메서드 이름만 보고 테스트를 만들지 않습니다.

## 2. 테스트 유형

| 조건 | 테스트 |
|---|---|
| DB, JPA 연관관계, 트랜잭션이 핵심 | 서비스 통합 테스트 |
| 계산, 분기, 호출 순서가 핵심 | Mock 기반 단위 테스트 |
| 외부 API, S3, JWT 연동 | 외부 경계만 Mock 처리 |
| URL, Parameter, Body, Validation 변경 | `@WebMvcTest` |
| 대량 데이터 성능 비교 | 환경 변수 기반 수동 테스트 |

단순 Getter, 한 줄 위임, DTO 변환만을 위한 작은 테스트는 만들지 않습니다.

## 3. 통합 테스트

```java
class NewServiceTest extends TestContainerConfig {
}
```

- 공통 설정: `src/test/java/org/zerock/puppyrun/a_config/TestContainerConfig.java`
- MySQL 8.4.5 컨테이너를 테스트 JVM에서 공유합니다.
- 각 테스트는 `@Transactional`로 종료 후 롤백합니다.
- 테스트마다 `@SpringBootTest`, Datasource, Container 설정을 복사하지 않습니다.
- 운영 Profile이나 운영 DB에 연결하지 않습니다.

## 4. Fixture와 TestData

- Fixture 경로: `src/testFixtures/java/org/zerock/puppyrun/fixture`
- Fixture는 반복되는 기본값과 새 Request DTO만 제공합니다.
- Fixture는 Repository 저장이나 Service 호출을 하지 않습니다.
- 상태가 적으면 Enum Object Mother, 변화가 많으면 Builder/Factory를 사용합니다.
- 이름은 `PET_OWNER`, `PET_STRANGER`, `NEW_GOOGLE`처럼 역할을 표현합니다.
- TestData 경로: `src/testFixtures/java/org/zerock/puppyrun/support`
- TestData는 여러 Fixture를 실제 Production Service로 저장·조합합니다.
- 필요한 Service만 받고 모든 Service/Repository를 주입하는 클래스를 만들지 않습니다.
- 필요한 데이터만 생성하고 결과는 최소 정보의 `record`를 우선합니다.

준비 데이터는 Production Service로 생성합니다. Repository 직접 저장은 Repository 테스트, 성능 테스트, Service로 만들 수 없는 상태 재현에만 허용하며 이유를 주석으로 남깁니다.

## 5. 테스트 형식

```java
@Test
@DisplayName("사용자 관점의 기대 동작")
void methodName() {
    // given
    // 필요한 데이터만 준비

    // when
    // 검증할 기능 실행

    // then
    // 반환값과 의미 있는 상태 검증
}
```

- 모든 테스트에 `given / when / then`을 작성합니다.
- 비즈니스 데이터를 `@BeforeEach`에 숨기지 않습니다.
- `@BeforeEach`는 Mock 설정, 설정값 주입, Cache 초기화에만 사용합니다.
- 반환값과 저장 상태, 연관관계, 정렬, 개수, 날짜, 단위 변환을 검증합니다.
- 예외는 타입과 중요한 Message 또는 Error Code를 확인합니다.
- 테스트 순서와 다른 테스트 데이터에 의존하지 않습니다.
- 고정 가능한 날짜는 `LocalDate.now()` 대신 고정값을 사용합니다.

## 6. API 계약 테스트

Controller 계약 변경 시 아래 규격을 정확히 검증합니다.

- Method와 URL
- Path Variable, Query Parameter의 이름·타입·필수 여부·기본값
- Request Body의 모든 필드와 Validation
- 성공·실패 Response의 필드와 타입
- Response JSON의 `snake_case`
- HTTP Status와 Error Code

## 7. 현재 코드 주의사항

- 펫 등록 시 최초 체중 이력도 생성되므로 등록 직후 이력 수는 1입니다.
- `WeatherServiceTest`는 테스트마다 Cache를 초기화합니다.
- 기존 이메일에 새로운 Social Provider ID가 들어오면 자동 연결하지 않습니다.
- S3 일부 업로드 실패 시 성공한 파일은 롤백 이벤트에 포함합니다.
- 성능 테스트는 환경 변수 없이 Skip되는 것이 정상입니다.

## 8. 실행과 완료 보고

```bash
./gradlew test --tests '*대상테스트명'
./gradlew compileTestJava
./gradlew clean test
```

공통 설정, Fixture, Production 로직 또는 여러 도메인을 바꾸면 전체 테스트를 실행합니다.

완료 시 변경한 테스트, 주요 시나리오, 테스트 유형, Fixture 변경, 실행 명령, 통과·실패·Skip 수와 남은 위험을 보고합니다. 실패를 숨기거나 단순히 완료했다고 보고하지 않습니다.
