# API Specification Convention

이 컨벤션은 Codex가 PuppyRun API 명세서를 일관된 형식으로 작성하기 위한 지침입니다. 출력 구조와 성공·실패 예시는 `.github/pull_request_template.md`의 PR 작성 양식을 따릅니다.

---

## 1. 적용 조건

다음 요청을 받으면 이 문서를 먼저 읽고 적용합니다.

- API 명세서 신규 작성
- 기존 API 명세서 수정 또는 검토
- 화면이나 사용자 흐름에 필요한 API 목록 정리
- PR 본문에 추가할 API 요청·응답 및 테스트 시나리오 작성

---

## 2. 작성 전 분석 순서

구현된 API를 문서화할 때는 별도 질문에 앞서 다음 파일을 확인합니다.

1. **Controller**
   - 클래스와 메서드의 Mapping을 조합하여 실제 Method와 URL을 확인합니다.
   - `@RequestBody`, `@RequestPart`, `@RequestParam`, `@PathVariable` 및 성공 상태 코드를 확인합니다.
2. **Request DTO**
   - JSON 필드명, 타입, 필수 여부와 Jakarta Validation 제약을 확인합니다.
3. **Response DTO**
   - 실제 직렬화되는 필드명과 중첩 구조, `null` 가능 여부를 확인합니다.
4. **Service와 Repository**
   - 날짜 관계, 소유권, 중복, 상태 전이 등 DTO만으로 알 수 없는 비즈니스 규칙을 확인합니다.
5. **인증과 오류 처리**
   - Security 설정과 인증 헤더를 확인합니다.
   - 발생 가능한 `BusinessException`을 `ErrorCode.java`의 HTTP 상태, 코드, 설명과 매핑합니다.
6. **연관 흐름**
   - 선행 API에서 반환된 ID가 필요한지, 반복 또는 병렬 호출이 가능한지, 여러 호출이 하나의 트랜잭션인지 확인합니다.

코드와 기존 문서가 다르면 코드를 기준으로 작성하고 차이를 `확인 필요 사항`에 기록합니다.

---

## 3. 핵심 작성 원칙

### 3.1 사실과 제안 구분

- 현재 코드에서 확인한 내용만 `구현 명세`로 작성합니다.
- 아직 없는 엔드포인트나 변경이 필요한 계약은 `제안 명세`로 표시합니다.
- REST 관례상 더 적합하더라도 코드가 `200 OK`를 반환하면 임의로 `201 Created`라고 작성하지 않습니다.
- 합의되지 않은 필수 여부, 기본값, 최대 길이, 정렬, 페이지 크기를 추측하지 않습니다.

### 3.2 이름과 타입

- URL과 JSON 필드명은 대소문자를 포함하여 코드와 동일하게 작성합니다.
- UUID는 `UUID`, 날짜는 `LocalDate (YYYY-MM-DD)`, 날짜·시간은 `LocalDateTime (ISO-8601)`으로 표기합니다.
- Enum은 허용값을 모두 기재합니다. 값이 많으면 코드 범위와 참조 위치를 함께 제공합니다.
- 금액, 거리, 무게 등 단위가 있는 값은 설명에 단위를 반드시 표시합니다.

### 3.3 필수 여부와 검증

- 필수 여부는 `O`와 `X`로 통일합니다.
- `@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Pattern` 등 실제 검증 조건을 제약조건 열에 작성합니다.
- DTO 검증과 Entity 또는 Service의 실제 처리 방식이 충돌하면 숨기지 않고 `확인 필요 사항`에 기록합니다.

### 3.4 요청과 응답

- JSON API는 `Content-Type: application/json`을 작성합니다.
- 파일 API는 실제 파트 이름과 함께 `multipart/form-data`를 작성합니다.
- 인증이 필요한 API는 `Authorization: Bearer {accessToken}`을 작성합니다.
- Body가 없는 API는 빈 JSON을 만들지 말고 `Request Body 없음`이라고 작성합니다.
- 성공 예시는 실제 DTO 필드를 모두 포함하며 UUID와 날짜는 유효한 형식의 예시를 사용합니다.
- 목록 응답은 데이터가 있는 예시와 빈 목록의 의미를 설명합니다.

### 3.5 오류

- 각 API에는 대표 성공 케이스 1개 이상과 의미 있는 실패 케이스 1개 이상을 작성합니다.
- 가능한 오류는 HTTP 상태, `ErrorCode`, 발생 조건으로 정리합니다.
- 오류 JSON은 프로젝트의 `ErrorResponse` 구조를 사용합니다.
- 근거 없이 `500 Internal Server Error`를 정상적인 비즈니스 실패처럼 추가하지 않습니다.

```json
{
  "code": "CLIENT_001",
  "description": "잘못된 요청입니다.",
  "message": "실제 오류 메시지",
  "timestamp": "2026-08-01T10:00:00",
  "path": "/api/resource"
}
```

---

## 4. 표준 출력 양식

API 명세서는 아래 순서와 제목을 유지합니다. 해당하지 않는 파라미터 섹션은 생략할 수 있지만, 섹션의 이름과 순서는 바꾸지 않습니다.

````markdown
# 📌 API 명세서: [JIRA 티켓 번호 또는 기능명]

# 📝 기능 요약 (Summary)

- 무엇을 위한 API인지 두괄식으로 작성합니다.
- 선행 호출, 후속 호출 및 트랜잭션 경계를 작성합니다.
- 구현 전 문서라면 **제안 명세**라고 표시합니다.

# 🛠️ API 목록 (Endpoints)

| 기능 | Method | URL | 인증 | 설명 |
|---|---|---|---|---|
| 리소스 등록 | `POST` | `/api/resources` | 필요 | 새로운 리소스를 등록합니다. |

# 📸 API 명세 및 시나리오 (API Specifications)

## 1. [API 이름]

* **Method**: `POST`
* **URL**: `/api/resources/{resourceId}`
* **Content-Type**: `application/json`
* **Header**: `Authorization: Bearer {accessToken}`

### Path Parameters

| 이름 | 타입 | 필수 | 설명 |
|---|---|---:|---|
| `resourceId` | UUID | O | 대상 리소스 ID |

### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 제약조건 | 설명 |
|---|---|---:|---|---|---|
| `page` | Integer | X | `0` | 0 이상 | 페이지 번호 |

### Request Body Fields

| 필드 | 타입 | 필수 | 제약조건 | 설명 |
|---|---|---:|---|---|
| `name` | String | O | 1~50자 | 이름 |

### Response Fields

| 필드 | 타입 | null 가능 | 설명 |
|---|---|---:|---|
| `resourceId` | UUID | X | 생성된 리소스 ID |

### ✅ 1-1: [성공 케이스]

**Request Body**

```json
{
  "name": "예시"
}
```

**Response (200 OK)**

```json
{
  "resourceId": "0fb6ae63-309d-412a-a5c2-e991e9527dae",
  "name": "예시"
}
```

### ❌ 1-2: [실패 케이스]

**Request Body**

```json
{
  "name": ""
}
```

**Response (400 Bad Request)**

```json
{
  "code": "CLIENT_001",
  "description": "잘못된 요청입니다.",
  "message": "이름은 필수입니다.",
  "timestamp": "2026-08-01T10:00:00",
  "path": "/api/resources/0fb6ae63-309d-412a-a5c2-e991e9527dae"
}
```

### 발생 가능한 오류

| HTTP 상태 | ErrorCode | 발생 조건 |
|---|---|---|
| `400 Bad Request` | `CLIENT_001` | 필수값 누락 또는 형식 오류 |
| `401 Unauthorized` | `AUTH_003` | Authorization 헤더 누락 |
| `403 Forbidden` | `AUTH_006` | 대상 리소스에 대한 권한 없음 |
| `404 Not Found` | `CLIENT_002` | 대상 리소스가 존재하지 않음 |

# ⚠️ 확인 필요 사항 (Open Questions)

- 코드와 문서가 불일치하거나 정책 결정이 필요한 내용만 작성합니다.
- 확인할 내용이 없으면 이 섹션을 생략합니다.
````

---

## 5. 여러 API로 구성된 화면 작성 규칙

- 화면의 사용자 동작 순서대로 API를 배치합니다.
- 선행 응답의 ID를 후속 URL이나 Body에서 사용하면 흐름을 명시합니다.
- 선택 입력은 `0..N회 호출`, 필수 입력은 `1회 호출`처럼 호출 횟수를 작성합니다.
- 병렬 호출 가능 여부와 일부 호출 실패 시 저장 상태를 작성합니다.
- 여러 HTTP 호출을 하나의 DB 트랜잭션처럼 표현하지 않습니다.
- 화면 제출을 단일 트랜잭션으로 보장해야 하지만 통합 API가 없다면 `확인 필요 사항`으로 기록합니다.

---

## 6. 최종 검증 체크리스트

- [ ] PR 템플릿과 동일한 제목, 요약, API 목록, 성공·실패 시나리오 흐름을 사용했는가?
- [ ] Method와 URL이 Controller Mapping 조합과 정확히 일치하는가?
- [ ] 요청 필드와 Validation이 Request DTO와 일치하는가?
- [ ] 응답 필드명과 중첩 구조가 Response DTO와 일치하는가?
- [ ] 실제 Controller가 반환하는 HTTP 상태를 작성했는가?
- [ ] 인증 헤더와 Content-Type을 작성했는가?
- [ ] Service의 비즈니스 검증과 소유권 검증을 반영했는가?
- [ ] 오류 상태와 코드가 `ErrorCode.java`에 실제로 존재하는가?
- [ ] 구현 명세와 제안 명세를 명확히 구분했는가?
- [ ] 코드와 계약의 불일치를 `확인 필요 사항`에 기록했는가?
