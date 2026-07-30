# PR Generation Convention

이 컨벤션은 Codex가 프로젝트의 변경 사항을 분석하여 `.github/pull_request_template.md` 형식에 맞는 PR 초안을 생성하기 위한 지침입니다.

---

## 1. 분석 단계 (Analysis & Extraction)

PR 작성 명령을 받으면 CLI는 별도의 추가 질문 없이 다음 데이터를 수집하고 분석합니다.

1. **브랜치 및 커밋 히스토리**: 비교 기준 브랜치, 현재 브랜치, 주요 커밋 메시지를 확인합니다.
2. **Commit History**: `git log --pretty=format:"%s"`를 통해 PR 범위의 작업 맥락을 파악합니다.
3. **Diff Analysis**: `git diff` 내용을 분석하여 수정된 기능, 엔티티 변경, DTO의 Validation 어노테이션 등을 추출합니다.
4. **Exception Mapping**:
    * `Service` 레이어의 `throw new ...` 로직을 추적합니다.
    * **중요**: `ErrorCode.java`를 참조하여 발생 가능성이 있는 예외의 **Error Code**와 **Description**을 매핑합니다.

5. **코드 구조 탐색**:
    - `request/` 폴더: DTO 구조와 Validation 규칙 확인
    - `response/` 폴더: 응답 필드와 데이터 타입 확인
    - `service/` 폴더: 핵심 비즈니스 로직 및 트랜잭션 전략 확인

---

## 2. 작성 지침 (Writing Guidelines)

* **JIRA/기능명**: 브랜치명 또는 커밋 메시지의 이슈 번호를 참조하여 작성합니다.
* **구조 일치**: 출력물은 반드시 프로젝트의 `pull_request_template.md` 섹션 구조(`Summary`, `Changes`, `Test Scenarios`)를 100% 준수해야 합니다.
* **두괄식 요약**: 작업의 핵심 이유(Why)를 가장 먼저 기술합니다.
* **DB 영향도**: JPA 엔티티나 DDL 수정이 포함된 경우 별도의 강조 표시를 합니다.

---

## 3. 테스트 시나리오 규격 (Technical Test Cases)

### ✅ 성공 케이스 (200 OK)

* 정상적인 입/출력 JSON 예시를 포함합니다.
* 실제 `request` DTO 구조를 기반으로 한 JSON 예시 작성

### ❌ 실패 케이스 (Edge Cases)

* 비즈니스 로직 예외 및 Validation 실패 케이스를 포함합니다.
* 에러 응답은 반드시 아래 JSON 규격을 엄격히 준수합니다.

```json
{
  "code": "ERROR_CODE",
  "description": "ErrorCode.java에 정의된 설명",
  "message": "사용자에게 보여줄 실제 메시지",
  "path": "/api/v1/resource",
  "timestamp": "2026-04-23T10:00:00"
}
```

## 4. 자동화 및 실행 (Automation)

- 명령어 실행: 사용자가 PR 작성을 요청하면 즉시 분석을 시작합니다.
- 형식 검증: 생성된 마크다운이 템플릿 양식에 맞는지 최종 확인합니다.
- 외부 상태 변경: PR 생성, 푸시, 클립보드 복사는 사용자가 명시적으로 요청한 경우에만 실행합니다.
