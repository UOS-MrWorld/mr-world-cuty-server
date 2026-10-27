# CUTY: Curate Your Travel

**여행을 고르는 것부터 나만의 여행을 만드는 것까지**

CUTY는 사용자의 취향과 관심사를 바탕으로 
**나에게 맞는 여행을 큐레이션하는 여행 서비스**이다.

수많은 여행 정보 속에서 원하는 여행지를 직접 찾고 비교하는 번거로움을 줄이고
사용자의 취향을 바탕으로 여행 테마와 콘텐츠를 탐색할 수 있도록 돕는다.

> **CUTY = Curate Your Travel**  
> "당신의 취향으로 당신만의 여행을 큐레이션하세요"

## 서비스 소개

- **Honeymoon Romance** — 특별한 날을 위한 로맨틱 허니문 여행
- **Parents Healing** — 부모님을 위한 효도·힐링 여행
- **Golf Challenge** — 골프와 함께하는 테마 여행
- **Outdoor Trekking** — 자연과 함께하는 아웃도어 여행

여행 테마를 선택한 뒤 투어 등급, 호텔, 교통, 식사 등의 옵션을 원하는 대로 구성할 수 있다.

> Software Engineering Project · University of Seoul

## 기술 스택

- Java 21 / Spring Boot 4.0
- Spring Data JPA, Spring Security (JWT)
- MySQL (로컬 개발/실행), H2 (테스트)
- springdoc-openapi (Swagger UI)
- Solapi SDK (투어 확정 문자 발송)
- Gradle 9 (Gradle Wrapper 사용, 별도 설치 불필요)
- GitHub Actions (PR·main push 시 테스트 실행)

## 패키지 구조

도메인(화면·기능 단위)별로 패키지를 나누고, 각 패키지 안에 `Controller` / `Service` / `Repository` / 엔티티 / DTO를 함께 둔다. 계층(전역 controller 패키지, 전역 service 패키지 …)으로 나누지 않고 **도메인 기준으로만** 나눈 이유는 API 명세의 도메인 구분과 그대로 대응시켜서 어떤 기능을 고치려 할 때 패키지 하나만 보면 되게 하기 위해서이다.

```
com.mrworld.yaho/
├── config/        # 전역 설정: Security(JWT 권한), Swagger, 스케줄러, SMS 발송 방식 선택
├── common/        # 공통 에러 응답(ResultDto), 전역 예외 처리, 공용 Enum
├── security/      # JWT 토큰 발급·검증, 인증 필터
│
├── auth/          # 회원가입 / 로그인 / 토큰 재발급 / 로그아웃
├── member/        # 내 회원정보 조회·수정
├── tour/          # 고객용 상품 목록·검색·음성 검색·상세, 직원용 상품 등록·수정·현황 조회,
│                  # 마감 스케줄러(TourScheduler: D-7 확정·취소, 여행 종료 후 완료 처리)
├── booking/       # 예약 생성·PENDING 옵션 수정·목록·상세·취소, 옵션 가격 계산(quote), 여행 내역
├── payment/       # Mock 결제, 환불(예약 취소·인원 미달 투어 취소에서 사용)
├── inventory/     # 직원용 재고 등록·조회·수정, 투어 확정 시 재고 차감
├── customer/      # 직원용 고객 조회, 단골 등급 기준 조회, 단골 할인 등급별 일괄 적용
└── notification/  # 투어 확정 문자 발송 (Solapi 실제 발송 / 로그만 남기는 Mock)
```

공용 enum(`Role`, `TourStatus`, `BookingStatus`, `PaymentStatus`, `LoyaltyGrade` 등)은 `common/enums`에 있다.

API 경로는 권한별로 나뉜다. 인증 없이 쓰는 `/api/v1/auth/**`, 로그인한 본인 정보용 `/api/v1/members/me`, 고객용 `/api/v1/customer/**`, 직원용 `/api/v1/staff/**`이다. 고객/직원 구분은 컨트롤러 분리가 아니라 `SecurityConfig`에서 경로별 권한(`CUSTOMER`, `STAFF`)을 걸어서 한다.

| 패키지 | 클래스 | 매핑되는 API |
| --- | --- | --- |
| `auth` | `AuthController` | `/api/v1/auth/**` (signup, login, refresh, logout) |
| `member` | `MemberController` | `/api/v1/members/me` (조회, 수정) |
| `tour` | `TourController` | `/api/v1/customer/tours/**` (목록, `search`, `voice-search`, 상세, `quote`), `/api/v1/staff/tours/**` (등록, 현황 목록, 상세 현황, 수정) |
| `booking` | `BookingController` | `/api/v1/customer/bookings/**` (생성, 목록, 상세, PENDING 수정, `cancel`), `/api/v1/customer/travel-history` |
| `payment` | `PaymentController` | `/api/v1/customer/payments` |
| `inventory` | `InventoryController` | `/api/v1/staff/inventory/**` (목록, 등록, 수정) |
| `customer` | `CustomerController` | `/api/v1/staff/customers/**` (목록, 상세, `loyalty-discount/by-grade`), `/api/v1/staff/loyalty-policy` |

`tour`, `customer` 패키지는 고객용/직원용 API를 컨트롤러 하나가 함께 제공한다(설계 산출물의 `TourController`, `CustomerController`와 동일). 전체 요청·응답 형식은 노션 API 명세서와 Swagger UI에서 확인한다.

### 서버 내부 비즈니스 로직 (별도 API 없음)

`TourScheduler`가 매일 자정(Asia/Seoul)에 실행한다. 투어마다 별도 트랜잭션으로 처리하므로 한 투어가 실패해도 나머지는 처리되고, 실패한 투어는 다음 실행에 다시 시도한다.

- **예약 마감과 투어 확정·취소**: 투어 시작 7일 전이 지나면 예약·결제·취소가 마감된다. 마감된 모집 중 투어는 `PAID` 예약의 `guestCount` 합이 3명 이상이면 `CONFIRMED`, 미달이면 `CANCELLED`가 된다. 허니문은 2명 단위로만 예약하므로 실제로는 4명(2쌍)부터 확정된다.
- **확정 시**: 기념품 재고를 `quantityPerGuest × 확정 인원`만큼 차감하고, 결제 완료 예약마다 확정 문자(예약번호·인원·옵션 포함)를 보낸다. 문자는 확정 처리가 커밋된 뒤에 보내며, 발송이 실패해도 확정은 유지된다.
- **인원 미달 취소 시**: 해당 투어의 성공 결제를 `REFUNDED`, 결제 완료 예약을 `CANCELLED`로 바꾼다.
- **마감 후 남은 `PENDING` 예약**은 결제할 수 없으므로 `CANCELLED` 처리한다.
- **여행 완료 처리**: 종료일이 지난 `CONFIRMED` 투어를 `COMPLETED`로 바꾸고, 참가 고객의 단골 등급을 완료 여행 횟수로 다시 정한다.

### 주요 규칙

- **상태**
  - 투어: `RECRUITING`(모집 중) → `CONFIRMED`(확정) → `COMPLETED`(완료), 또는 `CANCELLED`(인원 미달 취소)
  - 예약: `PENDING`(결제 전, 장바구니 역할) → `PAID`(결제 완료), 또는 `CANCELLED`. `PENDING` 예약만 옵션을 수정할 수 있다.
  - 결제: `SUCCESS` / `FAILED` / `REFUNDED`
- **옵션 선택**: 투어 등급은 Classic / Grand / Premium이며 허니문·효도 투어는 Grand 이상만 선택할 수 있다. 허니문은 2명만 예약할 수 있고, 2인 승용차는 3명 이상이면 선택할 수 없다.
- **금액**: 할인 전 총액 = (1인 기본가 + 호텔·식사·교통 추가금) × 인원 + 음료 추가금
- **단골 할인**
  - 등급 기준과 할인율은 코드에 고정된 값이다(`NORMAL` 0회 0%, `VIP` 완료 여행 5회 이상 10%). 등급은 여행 완료 처리 때 자동으로 정해진다.
  - 할인은 직원이 등급별 일괄 적용으로 켠 고객에게만 적용된다.
  - 예약 금액에는 예약 시점 할인율을 보여 주고, 실제 결제 금액은 결제 시점 할인율로 다시 계산한다.
- **결제(Mock)**: 카드번호가 `"1"`이면 승인, 그 외는 거절(`FAILED`)이다. 거절되면 예약은 `PENDING`으로 남아 다시 결제할 수 있고, 성공 결제는 예약당 하나만 허용한다.
- **취소**: 투어 시작 7일 전까지 가능하며, 결제 완료 예약은 결제가 환불된다.
- **기념품 재고**: 재고가 부족해도 예약·결제·확정을 막지 않는다. 확정 시 재고가 마이너스까지 차감될 수 있으며, 직원은 재고 목록의 `quantity`(현재 수량, 마이너스면 부족)와 `requiredQuantity`(모집 중 투어 결제 인원에게 줄 사용 예정 수량)로 부족 여부를 확인해 채운다.
- **여행 내역**: 본인의 결제 완료 예약 중 투어가 취소되지 않은 것(예약한 여행 + 완료된 여행)을 여행 시작일 최근 순으로 보여 준다.

## 로컬 실행

사전 준비: **JDK 21**, 로컬 **MySQL 실행 + `cuty` 데이터베이스 생성** (`CREATE DATABASE cuty;`)

```bash
# macOS / Linux / Git Bash
./gradlew bootRun

# Windows (PowerShell / cmd)
.\gradlew.bat bootRun
```

Gradle은 프로젝트에 포함된 Wrapper(`gradlew`)가 지정 버전(9.7.1)을 자동으로 내려받아 쓴다. 처음 실행할 때만 시간이 걸린다.

실행 후 Swagger UI(`http://localhost:8080/swagger-ui/index.html`)에서 API를 확인하고 호출해 볼 수 있다.

MySQL 접속 정보, JWT secret, SMS 설정은 환경변수로 넣는다. (`src/main/resources/application.properties` 참고)

### .env.example

```bash
DB_URL=
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=

# 투어 확정 문자(Solapi). 셋 중 하나라도 비어 있으면 실제로 보내지 않고 로그만 남긴다.
SMS_API_KEY=
SMS_API_SECRET=
SMS_SENDER=    # Solapi에 등록한 발신번호 (숫자만)
```

## 테스트

테스트는 MySQL 없이 H2 인메모리 DB로 돌고, 환경변수가 있어도 실제 문자를 보내지 않는다.

```bash
./gradlew test
```

PR을 열거나 `main`에 push하면 GitHub Actions가 같은 테스트를 실행한다. `main`은 이 테스트(`test`)가 통과해야 머지할 수 있다.

## 빌드

```bash
./gradlew build      # 컴파일 + 테스트 + jar 생성 (build/libs/)
./gradlew clean      # build/ 폴더 삭제
```
