# CUTY 서버 (mr-world-cuty-server)

## 먼저 읽을 문서
- ./README.md : 서비스 소개, 패키지 구조, API 매핑표, 비즈니스 규칙, 로컬 실행 방법

## 기능 명세 기준
- 기능 명세의 기준(source of truth)은 노션 "소공: 미스터월드" 프로젝트 페이지의 **API 명세서** 페이지와 그 하위 "엔드포인트" DB이다
- https://app.notion.com/p/3dc5eb167524806ba82de41f0e8b9dfc
- 기능을 구현하거나 API를 바꾸기 전에 노션 명세를 확인한다. README, 이 문서, 엑셀 명세서와 다르면 노션이 우선이며 어긋난 문서는 노션에 맞춰 고친다

## 프로젝트 설명
- 취향 기반 여행 큐레이션 서비스 CUTY(미스터 월드)의 백엔드 (서울시립대 소프트웨어공학 프로젝트)
- Java 21 / Spring Boot 4.0 / Gradle Wrapper
- Spring Data JPA, Spring Security + JWT(jjwt), springdoc(Swagger)
- 런타임 DB는 MySQL, 테스트는 H2 인메모리
- REST API 베이스 경로는 `/api/v1`이며 권한별로 경로가 나뉜다
  - `/api/v1/auth/**` : 인증 불필요 (회원가입, 로그인, 토큰 재발급)
  - `/api/v1/members/me` : 로그인한 본인 정보
  - `/api/v1/customer/**` : 고객용
  - `/api/v1/staff/**` : 직원용

## 명령어
- 실행: `./gradlew bootRun` (Windows PowerShell은 `.\gradlew.bat bootRun`)
- 테스트: `./gradlew test`
- 빌드: `./gradlew build`
- 실행에 필요한 환경변수: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`

## 작업 원칙
- 코드 수정 전 해당 도메인 패키지의 기존 코드와 README의 API 매핑표를 먼저 확인
- API 경로, 도메인 구분은 노션 API 명세서와 대응되므로 임의 변경 금지
- 권한은 `CUSTOMER`(고객 전용 API)와 `STAFF`(직원 전용 API) 두 가지다. 공통 API는 역할 prefix 없이 `/api/v1/**`를 쓴다
- 에러 응답은 `status`, `code`, `message`, `path`, `timestamp` 형식으로 통일한다. 코드는 `INVALID_INPUT`(400), `UNAUTHORIZED`(401), `FORBIDDEN`/`CUSTOMER_ONLY`/`STAFF_ONLY`(403), `RESOURCE_NOT_FOUND`(404), `CONFLICT`(409), `INTERNAL_SERVER_ERROR`(500)
- 날짜는 `LocalDate = YYYY-MM-DD`, `LocalDateTime = ISO-8601`을 쓴다
- 새 기능은 해당 도메인 패키지 안에 Controller / Service / Repository / 엔티티를 함께 둔다
- 고객용/직원용 컨트롤러는 같은 패키지에 두되 클래스를 분리한다 (예: `TourController`, `StaffTourController`)
- 직원용 경로 보호는 `SecurityConfig`의 권한 설정으로 한다. 컨트롤러 분리만으로 보호되지 않는다
- 고객용과 직원용 응답은 DTO를 분리하고 엔티티를 그대로 반환하지 않는다
- 예약·취소 마감, 여행 확정, 인원 미달 환불, 재고 차감, 여행 완료 처리와 단골 등급 산정은 별도 API가 아니라 서버 내부 로직으로 구현한다
- 결제는 Mock 방식이며 금액은 항상 서버에서 계산한 값을 기준으로 한다
- 설정 값(DB 접속 정보, JWT secret 등)은 환경변수로 주입하고 코드나 properties에 직접 쓰지 않는다
- 기존 코드 스타일(Lombok 사용, 한국어 주석)을 따른다

## 도메인 규칙 
- 도메인: Auth, Member, Tour, Booking, Payment, Customer/Loyalty, Inventory
- 예약 상태: `PENDING` → `PAID` → `CONFIRMED` / `CANCELLED`. `PENDING` 예약만 수정 가능
- 투어 등급은 Classic / Grand / Premium이며 허니문·효도 투어는 Grand 이상만 선택 가능
- 예약·취소는 투어 시작 3일 전까지만 가능
- 자동 확정: 마감 시점에 `PAID` 예약의 `guestCount` 합이 3명 이상이면 투어를 `CONFIRMED`, 미달이면 `CANCELLED`
- 인원 미달로 취소되면 해당 투어의 성공 결제는 `REFUNDED`, 예약은 `CANCELLED`로 바꾼다 (Mock 환불)
- 재고 차감: 투어 확정 시 `TOUR_GIFT.quantityPerGuest × 전체 확정 인원`만큼 `INVENTORY_ITEM.quantity` 차감
- 여행 종료 후 투어를 `COMPLETED`로 바꾸고, 완료 여행 횟수에 따라 고객 단골 등급을 자동 산정한다
- 이전 여행 목록: 본인의 결제 완료 예약 중 투어가 `COMPLETED`인 것만 최근 순으로 조회
- 단골 등급 기준과 할인율은 코드에 고정하며, 직원이 고객별 또는 등급별(`by-grade`)로 할인을 적용한다
- 현재 노션 명세에는 확정 SMS 알림이 없다. 필요해지면 명세에 먼저 반영한다

## 디렉토리 해석
- src/main/java/com/mrworld/yaho/ : 도메인별 패키지
  - auth / member / tour / booking / payment / inventory / customer : 도메인 기능
  - 이전 여행 이력(`/customer/travel-history`)은 `customer`가 아니라 `booking` 패키지에 둔다 (`customer`는 직원용 고객 관리·단골 정책)
  - `payment`는 `BookingRepository`/`Booking` 엔티티만 참조하고 `BookingService`는 주입하지 않는다 (빈 순환 참조 방지)
  - security/ : JWT 필터와 토큰 제공자
  - config/ : SecurityConfig, SwaggerConfig 등 전역 설정
  - common/ : 공통 응답 포맷, 예외 처리, 공용 Enum이 들어갈 자리
- src/main/resources/application.properties : 앱 설정 (값은 환경변수 참조)
- src/test/ : 테스트 (H2 사용, 별도 application.properties)

## 피해야 할 것
- 계층별 전역 패키지(controller/, service/ 등) 신설 (도메인 기준으로만 나눈다)
- 명세서에 없는 API나 도메인 추가 (예: 찜 기능은 명세서에 없다)
- `.env`, 비밀번호, JWT secret 등 민감 정보를 커밋하거나 코드에 하드코딩
- 테스트가 MySQL에 의존하게 만드는 변경 (테스트는 H2로 돌아야 한다)
- 요청받지 않은 `ddl-auto` 설정이나 DB 스키마 관련 설정 변경
