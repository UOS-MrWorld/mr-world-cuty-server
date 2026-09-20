# MrWorldYaho

**특별한 날, 가장 멋있는 장소에서 잊지 못할 여행을**

Mr. World는 여행 목적과 테마에 맞춰 원하는 여행 상품을 선택하고 
호텔·교통·식사 등의 옵션을 자유롭게 구성할 수 있는 **맞춤형 테마 여행 서비스**이다.

## 서비스 소개

- **Honeymoon Romance** — 특별한 날을 위한 로맨틱 허니문 여행
- **Parents Healing** — 부모님을 위한 효도·힐링 여행
- **Golf Challenge** — 골프와 함께하는 테마 여행
- **Outdoor Trekking** — 자연과 함께하는 아웃도어 여행

여행 테마를 선택한 뒤 투어 등급, 호텔, 교통, 식사 등의 옵션을 원하는 대로 구성할 수 있다.

> Software Engineering Project · University of Seoul

## 기술 스택

- Java 17 / Spring Boot 4
- Spring Data JPA, Spring Security (JWT)
- MySQL (로컬 개발/실행), H2 (테스트)
- Maven

## 패키지 구조

도메인(화면·기능 단위)별로 패키지를 나누고 각 패키지 안에 `Controller / Service`를 둔다. 계층(전역 controller 패키지, 전역 service 패키지 …)으로 나누지 않고 **도메인 기준으로만** 나눈 이유는 API 명세의 도메인 구분과 그대로 대응시켜서 어떤 기능을 고치려 할 때 패키지 하나만 보면 되게 하기 위해서이다.

```
com.mrworld.yaho/
├── config/     # 아직 비어있음. SecurityConfig(JWT), WebConfig 등 전역 설정이 들어갈 자리
├── common/     # 아직 비어있음. 공통 응답 포맷, 예외 처리, 공용 Enum이 들어갈 자리
│
├── auth/       # 회원가입 / 로그인 / 토큰 재발급 / 로그아웃
├── member/     # 내 회원정보 조회·수정, 이전 여행 이력
├── tour/       # 여행상품 — 고객용 조회/검색 + 직원용 등록/수정/삭제/현황
├── wish/       # 찜 등록/해제/목록
├── booking/    # 여행신청, 취소, 결제, 여행확정(최소 인원 도달 시 자동 확정) 로직까지 포함
├── inventory/  # 직원용 재고 관리, 여행 확정 시 재고 차감
└── customer/   # 직원용 고객 관리, 단골 등급 정책
```

각 도메인 패키지는 `Controller`(엔드포인트)와 `Service`(비즈니스 로직) 두 클래스만 있는 상태다. `Repository`/엔티티는 데이터 모델이 정해지는 대로 같은 패키지 안에 추가하면 된다.

| 패키지 | 클래스 | 매핑되는 API |
| --- | --- | --- |
| `auth` | `AuthController` | `/api/v1/auth/**` |
| `member` | `MemberController` | `/api/v1/members/**` |
| `tour` | `TourController` | `/api/v1/tours/**` (고객용) |
| `tour` | `StaffTourController` | `/api/v1/staff/tours/**` (직원용) |
| `wish` | `WishController` | `/api/v1/wishes/**` |
| `booking` | `BookingController` | `/api/v1/bookings/**` |
| `booking` | `PaymentController` | `/api/v1/payments` |
| `inventory` | `InventoryController` | `/api/v1/staff/inventory/**` |
| `customer` | `CustomerController` | `/api/v1/staff/customers/**` |
| `customer` | `LoyaltyPolicyController` | `/api/v1/staff/loyalty-policy` |

같은 패키지 안에 고객용/직원용 컨트롤러가 같이 있는 경우(`tour`, `customer`)는 데이터(엔티티)를 공유하되 권한 체계가 달라서 클래스만 분리해뒀다.

## 로컬 실행

```bash
./mvnw spring-boot:run
```

MySQL 접속 정보는 환경변수로 넣는다. (`src/main/resources/application.yml` 참고)

```bash
export DB_USERNAME=root
export DB_PASSWORD=your-local-password
export JWT_SECRET=아무-임의-문자열
```

테스트(`./mvnw test`)는 MySQL 없이 H2 인메모리 DB로 둔다.
