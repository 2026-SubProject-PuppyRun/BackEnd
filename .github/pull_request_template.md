# 📌 Pull Request: [JIRA 티켓 번호 또는 기능명]

# 📝 작업 요약 (Summary)

<!-- 
이 PR에서 수행한 작업의 핵심 내용을 간략히 요약해주세요. 
무엇을, 왜 변경했는지 설명하면 좋습니다.
예: 
- JWT 기반 인증 시스템 구축 (로그인, 회원가입)
- Refresh Token Rotation (RTR) 전략 적용하여 보안 강화
- AuthController 및 AuthService 리팩토링
-->

# 🛠️ 변경 사항 (Changes)

<!-- 
주요 변경 사항을 카테고리별로 나누어 작성해주세요. 
코드의 구조적 변경이나 로직의 핵심 변화를 적어주세요.

예: 
## 1. 보안 및 인증 (Security & JWT)
- **JwtTokenProvider 리팩토링**
  - 토큰 타입 구분: TokenType Enum(ACCESS, REFRESH)을 도입하여 토큰 생성 시 type claim을 포함하도록 수정.
  - 검증 로직 강화: `getUserIdFromRefreshToken` 메서드를 추가하여, 재발급 요청 시 토큰 타입이 반드시 REFRESH인지 확인하도록 제한.
## 2. 비즈니스 로직 (AuthService)•
- **서비스 통합**: 기존 `SignUpService`, `SignInService`를 `AuthService`로 통합하여 응집도 향상.
- **트랜잭션 전략 최적화**: 클래스 레벨에 @Transactional(readOnly = true)를 적용하고, 쓰기 작업(registerMember 등)에만 별도 트랜잭션 적용.
-->

## 1.

## 2.

# 📸 테스트 시나리오 (Test Scenarios)

<!-- 
변경 사항을 검증하기 위해 수행한 테스트 시나리오를 작성해주세요. 
Postman이나 Swagger 테스트 결과를 복사해 넣으면 리뷰에 도움이 됩니다.
-->

## 1. <!-- [테스트할 API 이름 (예: 로그인)] -->

* **Method**: <!-- GET, POST, PUT, DELETE 중 선택 (예: POST) -->
* **URL**: <!-- 엔드포인트 경로 (예: /api/auth/sign-in) -->
* **Content-Type**: `application/json`
* **Header**: <!-- 필요한 헤더가 있다면 기재 (예: Authorization, Refresh-Token) -->

### ✅ 1-1 : <!-- [성공 케이스 설명 (예: 정상 로그인)] -->

<!-- 정상적으로 통신이 완료 되었을 경우의 요청과 응답을 작성해주세요. -->

**Request Body**

```
{
}
```

**Response (200 OK)**

```
{
}

```

### ❌ 1-2 : <!-- [실패 케이스 설명 (예: 잘못된 요청)] -->

<!-- 예외 상황이 발생했을 경우의 요청과 응답을 작성해주세요. -->

**Request Body**

```
{
}
```

**Response (400 Bad Request)**

```
{
}
```
