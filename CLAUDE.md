# CUTY 서버 (mr-world-cuty-server)

## 먼저 읽을 문서
- ./README.md : 서비스 소개, 패키지 구조, 로컬 실행 방법
- 기능/API 명세의 기준은 노션 "소공: 미스터월드"의 API 명세서 (README와 다르면 노션이 우선)
  https://app.notion.com/p/3dc5eb167524806ba82de41f0e8b9dfc

## 프로젝트 설명
- 여행 큐레이션 서비스 CUTY(미스터 월드)의 Spring Boot 백엔드
- Java 21 / Spring Boot 4.0 / JPA / Spring Security + JWT
- 런타임 DB는 MySQL, 테스트는 H2

## 명령어
- 실행: `./gradlew bootRun`
- 테스트: `./gradlew test`
- 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`

## 작업 원칙
- 구현 전에 노션 API 명세와 해당 도메인 패키지의 기존 코드를 먼저 확인
- API 경로와 도메인 구분은 명세와 대응되므로 임의 변경 금지
- 패키지는 도메인 기준으로 나누고, 각 패키지에 Controller / Service / Repository / 엔티티를 함께 둔다
- 엔티티를 응답으로 직접 반환하지 않고 DTO를 쓴다
- 설정 값(DB 정보, JWT secret 등)은 환경변수로 주입한다
- 기존 코드 스타일(Lombok, 한국어 주석)을 따른다

## 디렉토리 해석
- src/main/java/com/mrworld/yaho/ : 도메인별 패키지 (auth, member, tour, booking, payment, inventory, customer)
- security/ : JWT 필터와 토큰 제공자
- config/ : 전역 설정
- common/ : 공통 응답, 예외 처리, 공용 Enum

## 피해야 할 것
- 계층별 전역 패키지(controller/, service/ 등) 신설
- 명세에 없는 API나 도메인 추가
- 비밀번호, JWT secret 등 민감 정보를 커밋하거나 코드에 하드코딩
- 테스트가 MySQL에 의존하게 만드는 변경
- 요청받지 않은 DB 스키마 및 `ddl-auto` 설정 변경
