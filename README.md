## AI 리뷰 가이드라인

### 코틀린 컨벤션
- 함수명은 camelCase 사용
- 클래스 내 프로퍼티 순서: const -> val -> var
- companion object는 클래스 하단에 위치
- 데이터 클래스는 불변 객체로 설계

### 주의 깊게 봐야 할 파일
- Repository 관련 파일: 데이터 계층 핵심 로직
- Activity/Fragment: UI 생명주기 관련
- Network/API 관련: 에러 처리 및 보안