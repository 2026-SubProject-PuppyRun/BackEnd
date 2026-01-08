# BackEnd

퍼피런 BE

|  커밋 타입   | 설명                               | 예시 커밋 메시지                                    |
|:--------:|:---------------------------------|:---------------------------------------------|
|   Feat   | 새로운 기능 추가 (API, 비즈니스 로직, DB 스키마) | Feat: 토큰 재발급(RTR) API 구현 (#15)               |
|   Fix    | 버그 수정                            | Fix: JWT 만료 예외가 500으로 반환되는 오류 수정             |
| Refactor | 코드 리팩토링 (기능 변경 없음, 구조 개선)        | Refactor: AuthService 로그인 로직 메서드 분리          |
|   Test   | 테스트 코드 추가 및 리팩토링                 | Test: MemberRepository 회원 조회 단위 테스트 추가       |
|   Docs   | 문서 수정 (Swagger, README, JavaDoc) | Docs: 회원가입 API 명세서(Swagger) 응답값 수정           |
|  Chore   | 빌드 설정, 패키지 매니저, 설정 파일 수정         | Chore: build.gradle 의존성 라이브러리 추가             |
|  Infra   | 서버, Docker, CI/CD, DB 설정         | Infra: GitHub Actions CI 배포 스크립트 작성          |
|  Style   | 코드 포맷팅, 세미콜론, 오타 수정 (로직 영향 X)    | Style: Google Java Style Guide 적용 및 포맷팅      |
|  Rename  | 파일/폴더명 수정, 위치 이동                 | Rename: MemberDto -> MemberResponseDto 이름 변경 |
|  Remove  | 사용하지 않는 코드, 파일 삭제                | Remove: 사용하지 않는 System.out.println 로그 삭제     |
