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
- Gradle 9 (Gradle Wrapper 사용, 별도 설치 불필요)

## 패키지 구조

도메인(화면·기능 단위)별로 패키지를 나누고 각 패키지 안에 `Controller / Service`를 둔다. 계층(전역 controller 패키지, 전역 service 패키지 …)으로 나누지 않고 **도메인 기준으로만** 나눈 이유는 API 명세의 도메인 구분과 그대로 대응시켜서 어떤 기능을 고치려 할 때 패키지 하나만 보면 되게 하기 위해서이다.

```
com.mrworld.yaho/
├── config/     # 아직 비어있음. SecurityConfig(JWT), WebConfig 등 전역 설정이 들어갈 자리
├── common/     # 아직 비어있음. 공통 응답 포맷, 예외 처리, 공용 Enum이 들어갈 자리
│
├── auth/       # 회원가입 / 로그인 / 토큰 재발급 / 로그아웃
├── member/     # 내 회원정보 조회·수정
├── tour/       # 여행상품 — 고객용 목록·상세·음성 검색 + 직원용 등록/수정/현황 조회
├── booking/    # 가격 계산, 여행신청, PENDING 예약 수정, 취소, 결제(Mock), 여행확정(결제 완료 3명 이상 시 자동 확정), 확정 SMS 알림, 이전 여행 이력
├── inventory/  # 직원용 재고 관리, 여행 확정 시 재고 차감
└── customer/   # 직원용 고객 관리, 단골 등급 정책·할인 적용
```

각 도메인 패키지는 `Controller`(엔드포인트)와 `Service`(비즈니스 로직) 두 클래스만 있는 상태다. `Repository`/엔티티는 데이터 모델이 정해지는 대로 같은 패키지 안에 추가하면 된다.

API 경로는 권한별로 나뉜다. 인증 없이 쓰는 `/api/v1/auth/**`, 로그인한 본인 정보용 `/api/v1/members/me`, 고객용 `/api/v1/customer/**`, 직원용 `/api/v1/staff/**`이다. 직원용 경로 보호는 컨트롤러 분리가 아니라 `SecurityConfig`에서 권한을 걸어서 한다.

| 패키지 | 클래스 | 매핑되는 API |
| --- | --- | --- |
| `auth` | `AuthController` | `/api/v1/auth/**` (signup, login, refresh, logout) |
| `member` | `MemberController` | `/api/v1/members/me` (조회, 수정) |
| `tour` | `TourController` | `/api/v1/customer/tours/**` (목록, 상세, `voice-search`) |
| `tour` | `StaffTourController` | `/api/v1/staff/tours/**` (등록, 수정, 현황 목록·상세) |
| `booking` | `BookingController` | `/api/v1/customer/tours/{tourId}/quote`, `/api/v1/customer/bookings/**` (신청, 수정, 목록, 상세, `cancel`), `/api/v1/customer/travel-history` |
| `booking` | `PaymentController` | `/api/v1/customer/payments` |
| `inventory` | `InventoryController` | `/api/v1/staff/inventory/**` (등록, 조회, 수정) |
| `customer` | `CustomerController` | `/api/v1/staff/customers/**` (목록, 상세, `loyalty-discount`, `loyalty-discount/by-grade`) |
| `customer` | `LoyaltyPolicyController` | `/api/v1/staff/loyalty-policy` |

같은 패키지 안에 고객용/직원용 컨트롤러가 같이 있는 경우(`tour`, `customer`)는 데이터(엔티티)를 공유하되 권한 체계가 달라서 클래스만 분리해뒀다.

### 서버 내부 비즈니스 로직 (별도 API 없음)

- **여행 자동 확정**: 투어 시작 3일 전 마감 시점에 `PAID` 예약의 `guestCount` 합이 3명 이상이면 `CONFIRMED`, 미달이면 `CANCELLED`
- **확정 SMS 발송**: 확정된 상품의 신청 고객 연락처로만 발송
- **재고 차감**: 확정 시 `TOUR_GIFT.quantityPerGuest × 전체 확정 인원`만큼 재고 차감

### 주요 규칙

- 예약 상태: `PENDING`(결제 전, 장바구니 역할) → `PAID` → `CONFIRMED` / `CANCELLED`. `PENDING` 예약만 수정 가능
- 투어 등급: Classic / Grand / Premium. 허니문·효도 투어는 Grand 이상만 선택 가능
- 취소는 출발 3일 전까지만 가능
- 결제는 서버에서 계산한 최종 금액 기준의 Mock 결제이며 중복 결제를 막는다
- 이전 여행 목록은 본인의 결제 완료 예약 중 투어가 `COMPLETED`인 것만 최근 순으로 조회
- 단골 등급 기준과 할인율은 코드에 고정된 값을 사용하며, 직원이 고객별 또는 등급별로 할인을 적용한다

## 로컬 실행

사전 준비: **JDK 21**, 로컬 **MySQL 실행 + `cuty` 데이터베이스 생성** (`CREATE DATABASE cuty;`)

```bash
# macOS / Linux / Git Bash
./gradlew bootRun

# Windows (PowerShell / cmd)
.\gradlew.bat bootRun
```

Gradle은 프로젝트에 포함된 Wrapper(`gradlew`)가 지정 버전(9.7.1)을 자동으로 내려받아 쓴다. 처음 실행할 때만 시간이 걸린다.

MySQL 접속 정보와 JWT secret은 환경변수로 넣는다. (`src/main/resources/application.properties` 참고)

### .env.example

```bash
DB_URL=
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=
```

테스트는 MySQL 없이 H2 인메모리 DB로 돈다.

```bash
./gradlew test
```

## 빌드

```bash
./gradlew build      # 컴파일 + 테스트 + jar 생성 (build/libs/)
./gradlew clean      # build/ 폴더 삭제
```
