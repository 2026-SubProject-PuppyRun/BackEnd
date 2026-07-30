# Commit Message Convention

이 프로젝트의 커밋 메시지는 다음 규칙을 엄격히 따릅니다.

## 1. 구조

```
<type>: <subject> (#<issue-number>)

- class name:
  - Changes
  - Changes
  ...
- class name:
  - Changes
  - Changes
```

## 2. Type (타입)

- `feat`: 새로운 기능 추가
- `fix`: 버그 수정
- `refactor`: 코드 리팩토링 (기능 변경 없음)
- `style`: 코드 포맷팅, 세미콜론 누락 등 (코드 변경 없음)
- `docs`: 문서 수정
- `test`: 테스트 코드 추가/수정
- `chore`: 빌드 업무, 패키지 매니저 설정 등
- `rename`: 파일/폴더명 수정 또는 이동

## 3. 규칙

- **Subject**: 100자 이내로 작성, 마침표 금지. 한글 사용 권장.
- **Body**: 무엇을, 왜 변경했는지 자세히 설명. (여러 줄인 경우 `-` 사용)
- **Issue Number**: 모든 커밋은 관련된 이슈 번호를 포함해야 함 (예: #39).

## 4. Codex 작성 지침

Codex는 커밋 메시지 작성을 요청받으면 이 파일과 `git status`, `git diff`, 관련 커밋 히스토리를 확인하고 위 형식의 초안을 작성합니다.
이슈 번호를 저장소에서 확인할 수 없으면 임의로 만들지 말고 사용자에게 확인합니다.
사용자가 커밋 실행을 명시적으로 요청하지 않았다면 메시지 초안만 제공합니다.
