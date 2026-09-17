# 🔪 Murder-Help

> Spring Boot와 React로 만든 등급제 커머스 시스템

회원이 상품을 조회하고 장바구니에 담아 주문·결제·환불까지 진행할 수 있으며,
**회원 등급에 따라 접근 가능한 상품 등급이 달라지는** 커머스 플랫폼입니다.
Redis 캐시로 검색 성능을 확보하고, WebSocket 기반 실시간 문의 채팅을 함께 제공합니다.

프론트엔드 빌드 결과물이 Spring Boot의 정적 리소스로 포함되므로, 배포 산출물은 Docker 이미지 하나입니다.

![Static Badge](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)
![Static Badge](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Static Badge](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Static Badge](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)

![Static Badge](https://img.shields.io/badge/MySQL%208.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Static Badge](https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white)

![Static Badge](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Static Badge](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Static Badge](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonwebservices&logoColor=white)

![Static Badge](https://img.shields.io/badge/React%2019-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Static Badge](https://img.shields.io/badge/TypeScript-3178C6?style=for-the-badge&logo=typescript&logoColor=white)
![Static Badge](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Static Badge](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

## 🔗 Links

- [Wiki](https://github.com/team-4-ladder/Murder-Help/wiki)
- [API 명세서](./docs/api/API_SPEC.md)
- [상품 검색 성능 테스트 보고서](./docs/product-search-performance-test.md)
- [k6 부하 테스트 실행 가이드](./scripts/k6/README.md)

---

## 🚀 시작하기 (Getting Started)

### 요구 사항

| 항목 | 버전 |
|---|---|
| JDK | 17 |
| Gradle | Wrapper 포함 |
| Node | 22 이상 |
| pnpm | 10 |
| MySQL | 8.0 |
| Redis | 7 |

---

## 📌 주요 도메인 구조

### 패키지 구조

```
org.example.murderhelp
├── domain
│   ├── auth      # 회원가입 / 로그인 / 토큰 재발급 / 로그아웃
│   ├── member    # 내 정보 조회·수정, 프로필 이미지, 등급 정책
│   ├── product   # 상품 조회, 등급·카테고리 필터, 검색(v1/v2), 랭킹 배치
│   ├── search    # 인기 검색어 집계
│   ├── cart      # 장바구니 CRUD (담기/조회/수량변경/선택삭제)
│   ├── order     # 주문 생성, 기간별 주문 조회, 주문 상세
│   ├── payment   # PortOne 결제 승인/취소/실패 처리
│   ├── refund    # 부분·전액 환불, 환불 가능 수량 계산, 환불 이력
│   ├── review    # 리뷰 작성/수정/삭제, 상품별·내 리뷰 조회
│   └── chat      # STOMP 실시간 문의 채팅, 자동 응답 봇
├── infra
│   └── portone   # PortOne 클라이언트 / 설정 / 웹훅 수신·검증
└── global
    ├── config       # Security, Redis, Cache, WebSocket, QueryDSL, ShedLock
    ├── jwt          # JwtProvider
    ├── filter       # JwtAuthFilter
    ├── interceptor  # StompAuthInterceptor (WebSocket 연결 인증)
    ├── error        # ErrorCode, BusinessException, GlobalExceptionHandler
    ├── response     # 공통 응답 포맷(ApiResponse, PageResponse)
    ├── annotation   # 커스텀 검증 어노테이션(UniqueIdList 등)
    ├── util         # CookieUtil
    └── entity       # 공통 엔티티(BaseTimeEntity)
```

### 프론트엔드 구조

```
frontend/src
├── api          # 백엔드 호출 모듈 (auth, cart, member, orders, payment, ...)
├── components
│   ├── admin    # 상품 랭킹 관리
│   ├── auth     # 로그인 모달, 접근 제어 Gate
│   ├── cart     # 장바구니
│   ├── chat     # 플로팅 채팅 위젯, 관리자 채팅 대시보드
│   ├── common   # 공통 UI 조각
│   ├── member   # 마이페이지, 등급 배지/진행도
│   ├── order    # 결제, 주문 상세, 환불 모달
│   └── product  # 상품 카드/상세/리뷰, 사이드바
└── lib          # 테마, 등급 계산, 세션 스토리지
```

---

## 📌 주요 기능

### 등급 정책

회원 등급(`Grade`)과 상품 등급(`ProductTier`)이 같은 색 체계를 공유하며,
회원은 **자기 등급 이하의 상품만** 조회·구매할 수 있습니다.

| 등급 | 승급 기준 (누적 결제 금액) | 접근 가능 상품 |
|---|---|---|
| ![YELLOW](https://img.shields.io/badge/YELLOW-FFC107?style=flat-square&labelColor=FFC107&color=FFC107) | 기본 등급 | ![](https://img.shields.io/badge/yellow-FFC107?style=flat-square) |
| ![PURPLE](https://img.shields.io/badge/PURPLE-8B5CF6?style=flat-square&labelColor=8B5CF6&color=8B5CF6) | 100,000원 이상 | ![](https://img.shields.io/badge/yellow-FFC107?style=flat-square) ![](https://img.shields.io/badge/purple-8B5CF6?style=flat-square) |
| ![RED](https://img.shields.io/badge/RED-EF4444?style=flat-square&labelColor=EF4444&color=EF4444) | 500,000원 이상 | ![](https://img.shields.io/badge/yellow-FFC107?style=flat-square) ![](https://img.shields.io/badge/purple-8B5CF6?style=flat-square) ![](https://img.shields.io/badge/red-EF4444?style=flat-square) |
| ![GREEN](https://img.shields.io/badge/GREEN-22C55E?style=flat-square&labelColor=22C55E&color=22C55E) | 운영자가 직접 부여 (관리자) | 전체 |

### 상품 검색 (v1 / v2)

동일한 검색 기능을 두 벌로 제공하며, Redis 캐시 적용 효과를 비교할 수 있습니다.

| API | 방식 |
|---|---|
| `GET /api/v1/products/search` | 매 요청마다 MySQL에서 직접 검색 |
| `GET /api/v2/products/search` | Redis Cache-aside, 캐시 미스일 때만 MySQL 조회 |

로컬 측정 기준 v2가 평균 응답 시간 약 90% 감소, 처리량 약 1.5배를 기록했습니다.
자세한 내용은 [성능 테스트 보고서](./docs/product-search-performance-test.md)에 정리되어 있습니다.

### 상품 랭킹 배치

매일 새벽 3시에 상품 랭킹을 갱신합니다. 다중 인스턴스 환경에서 중복 실행되지 않도록 **ShedLock(Redis)** 으로 잠금을 겁니다.
관리자는 `POST /api/admin/product-rankings/refresh`로 수동 갱신도 할 수 있습니다.

### 결제 · 환불

- PortOne 결제 승인 시 서버에서 **PG 결제 상태와 결제 금액을 재검증**한 뒤 주문을 확정합니다.
- PortOne 웹훅을 수신해 서명을 검증하고, 처리 상태를 `WebhookEvent`로 기록합니다.
- 주문 항목 단위 **부분 환불**을 지원하며, 이미 환불된 수량을 제외한 환불 가능 수량을 계산합니다.

### 실시간 문의 채팅

- STOMP over WebSocket(SockJS 폴백)으로 1:1 문의 채팅을 제공합니다.
- 연결 시 `StompAuthInterceptor`가 JWT를 검증합니다.
- **Redis Pub/Sub**으로 여러 애플리케이션 인스턴스 간 메시지를 전파합니다.
- 상담원 연결 전까지는 `BotScenario` 기반 자동 응답 봇이 대응합니다.

---

## Team Code Convention

### 📌 계층 구조

```
|- global
   |- config
   |- filter
   |- error
   |- response
|- infra
   |- ...
|- domain
   |- ...
      |- controller
      |- facade      # 여러 도메인을 묶는 트랜잭션이 필요한 경우에만
      |- service
      |- repository
      |- entity
      |- dto
```

`controller → (facade) → service → repository` 순으로 호출합니다.
여러 도메인에 걸친 트랜잭션은 `facade`에 둡니다 (`OrderFacade`, `PaymentFacade`, `RefundFacade`, `ChatFacade`).

모든 응답은 `ApiResponse`로 감싸고, 페이지 응답은 `PageResponse`를 사용합니다.
예외는 `BusinessException`과 `ErrorCode`로 정의하고 `GlobalExceptionHandler`에서 변환합니다.

### 코드

#### Enum

- Grade (회원 등급)

| Status | Description |
|---|---|
| `YELLOW` | 기본 등급 |
| `PURPLE` | 누적 결제 100,000원 이상 |
| `RED` | 누적 결제 500,000원 이상 |
| `GREEN` | 관리자 등급 (전체 상품 접근) |

- ProductTier (상품 등급)

| Status | Description |
|---|---|
| `YELLOW` | 모든 회원이 구매 가능 |
| `PURPLE` | PURPLE 이상 구매 가능 |
| `RED` | RED 이상 구매 가능 |
| `GREEN` | GREEN(관리자)만 구매 가능 |

- Product

| Status | Description |
|---|---|
| `ON_SALE` | 판매 중 |
| `SOLD_OUT` | 품절 |
| `DISCONTINUED` | 판매 중지 |

- Order

| Status | Description |
|---|---|
| `PENDING_PAYMENT` | 주문 생성 후 결제 대기 |
| `PAID` | 결제 완료 |
| `PREPARING_DELIVERY` | 배송 준비 중 |
| `SHIPPING` | 배송 중 |
| `DELIVERED` | 배송 완료 |
| `CANCELED` | 주문 취소 또는 전액 환불 완료 |

> 상태 전이 규칙은 `OrderStatus.canTransitTo()`에 정의되어 있습니다.

- Payment

| Status | Description |
|---|---|
| `PENDING` | 결제 대기 |
| `COMPLETED` | 결제 완료 (PG 승인 + 서버 검증 통과) |
| `FAILED` | 결제 실패 (PG 거절, 서버 검증 실패 등) |
| `CANCELLED` | 결제 완료 전 사용자가 직접 취소 (PG 미승인) |
| `PARTIAL_REFUND` | 일부 금액 환불 |
| `FULL_REFUND` | 전액 환불 완료 |

- Payment Fail Reason

| Type | Description |
|---|---|
| `PG_DECLINED` | PG사에서 결제를 거절 |
| `AMOUNT_MISMATCH` | 결제 금액 검증 실패 |
| `USER_CANCELLED` | 사용자가 결제를 취소 |

- Refund

| Status | Description |
|---|---|
| `COMPLETED` | 환불 완료 |
| `PG_FAILED` | PG 환불 요청 실패 |

- Webhook Event

| Status | Description |
|---|---|
| `RECEIVED` | 수신 완료, 처리 전 |
| `PROCESSED` | 정상 처리 완료 |
| `IGNORED` | 처리 대상이 아닌 이벤트 (예: `Transaction.Ready`) |
| `FAILED` | 처리 중 에러 발생, 재처리 대상 |

- Chat Room

| Status | Description |
|---|---|
| `BOT_MODE` | 자동 응답 봇 대응 중 (고객만 발화 가능) |
| `WAITING` | 상담원 배정 대기 |
| `IN_PROGRESS` | 상담 진행 중 |
| `COMPLETED` | 상담 종료 (발화 불가) |

- Chat Message

| Type | Description |
|---|---|
| `TEXT` | 일반 텍스트 메시지 |
| `SYSTEM` | 시스템 안내 메시지 |
| `BUTTON` | 봇 선택지 버튼 메시지 |

- Product Sort

| Type | Description |
|---|---|
| `POPULAR` | 인기순 (기본값) |
| `PRICE_ASC` | 낮은 가격순 |
| `PRICE_DESC` | 높은 가격순 |
| `NEWEST` | 최신 등록순 |

---

## 📌 Flowchart

### 1. 상품

회원 등급을 확인해 **조회 가능한 등급의 상품만** 목록으로 내려줍니다.

<details>
<summary><b>상품 목록 조회 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 고객
    participant Server as 상품 서버
    participant DB as DB

    rect rgb(245, 238, 255)
        Note over Customer,DB: [1단계] 상품 전체 조회 요청 및 회원 등급 확인

        Customer->>Server: GET /api/products
        Server->>Server: 인증 정보에서 회원 ID 확인
        Server->>DB: 회원 ID로 현재 회원 등급 조회
        DB-->>Server: 회원 등급 반환
    end

    rect rgb(255, 240, 240)
        Note over Server,DB: [2단계] 회원 등급에 따라 조회 범위 결정

        alt 옐로 회원
            Server->>DB: 최소 조회 등급이 YELLOW인 상품 조회
        else 퍼플 회원
            Server->>DB: 최소 조회 등급이 YELLOW, PURPLE인 상품 조회
        else 레드 회원
            Server->>DB: 최소 조회 등급이 YELLOW, PURPLE, RED인 상품 조회
        else 그린 회원
            Server->>DB: 모든 등급의 상품 조회
        end

        DB-->>Server: 조회 가능한 상품 목록 반환
    end

    rect rgb(238, 252, 240)
        Note over Customer,Server: [3단계] 상품 목록 응답

        Server-->>Customer: 200 OK (회원 등급에 맞는 상품 목록)
        Note over Customer: 조회 가능한 상품만 화면에 표시
    end
```

</details>

---

### 2. 상품 상세

상품 존재 여부를 먼저 확인하고, 회원 등급이 상품의 최소 조회 등급 이상일 때만 상세 정보를 반환합니다.

<details>
<summary><b>상품 상세 조회 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 고객
    participant Server as 상품 서버
    participant DB as DB

    rect rgb(255, 252, 230)
        Note over Customer,DB: [1단계] 상품 상세 조회 요청

        Customer->>Server: GET /api/products/{id}
        Server->>Server: 인증 정보에서 회원 ID 확인
        Server->>DB: 회원 ID로 현재 회원 등급 조회
        DB-->>Server: 회원 등급 반환
    end

    rect rgb(245, 238, 255)
        Note over Server,DB: [2단계] 상품 정보 확인

        Server->>DB: 상품 ID로 상품 조회
        DB-->>Server: 조회 결과 반환

        alt 상품이 존재하지 않음
            Server-->>Customer: 404 Not Found (상품을 찾을 수 없음)
        else 상품이 존재함
            rect rgb(255, 240, 240)
                Note over Server: [3단계] 상품 조회 권한 확인
                Note over Server: 등급 순서: YELLOW → PURPLE → RED → GREEN
                Server->>Server: 회원 등급과 상품의 최소 조회 등급 비교
            end

            alt 회원 등급이 상품의 최소 조회 등급보다 낮음
                Server-->>Customer: 403 Forbidden (조회 가능한 등급이 아님)
            else 회원 등급이 상품의 최소 조회 등급 이상
                rect rgb(238, 252, 240)
                    Note over Customer,Server: [4단계] 상품 상세 응답

                    Server-->>Customer: 200 OK (상품 상세 정보)
                    Note over Customer: 상품명, 가격, 설명, 재고 등 표시
                end
            end
        end
    end
```

</details>

---

### 3. 장바구니

등급·판매 상태를 확인한 뒤 장바구니에 담습니다. 이미 담은 상품이면 수량을 합산하며, 이 단계에서 재고는 차감하지 않습니다.

<details>
<summary><b>장바구니 담기 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 고객
    participant Server as 장바구니 서버
    participant DB as DB

    rect rgb(255, 252, 230)
        Note over Customer,DB: [1단계] 장바구니 추가 요청

        Customer->>Server: POST /api/cart-items (상품 ID, 수량)
        Server->>Server: 인증 정보에서 회원 ID 확인
        Server->>Server: 요청 수량이 1 이상인지 검증
    end

    rect rgb(245, 238, 255)
        Note over Server,DB: [2단계] 상품 및 회원 등급 확인

        Server->>DB: 회원 등급 및 상품 정보 조회
        DB-->>Server: 조회 결과 반환

        Note over Server: 등급 순서: YELLOW → PURPLE → RED → GREEN

        alt 상품이 존재하지 않음
            Server-->>Customer: 404 Not Found (상품 없음)
        else 회원 등급이 상품의 최소 조회 등급보다 낮음
            Server-->>Customer: 403 Forbidden (등급 부족)
        else 판매 불가 상품
            Server-->>Customer: 추가 실패 (판매 불가 상품)
        else 추가 가능한 상품
            rect rgb(255, 240, 240)
                Note over Server,DB: [3단계] 장바구니 저장

                Server->>DB: 본인의 장바구니에 동일 상품이 있는지 조회
                DB-->>Server: 조회 결과 반환

                alt 동일 상품이 이미 존재
                    Server->>DB: 기존 수량에 요청 수량 합산
                else 처음 담는 상품
                    Server->>DB: 장바구니 항목 생성 (회원 ID, 상품 ID, 수량)
                end

                DB-->>Server: 저장 완료
            end

            rect rgb(238, 252, 240)
                Note over Customer,Server: [4단계] 추가 완료 응답

                Server-->>Customer: 추가 완료 (장바구니 항목 ID, 상품 ID, 최종 수량)
            end
        end
    end

    Note over Server,DB: 장바구니 추가 시 재고는 차감하지 않음
```

</details>

---

### 4. 주문

재고를 잠그고(`FOR UPDATE`) 확인한 뒤 차감하고, 결제 대기 상태의 주문과 주문 상품 스냅샷을 저장합니다.

<details>
<summary><b>주문 생성 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor Customer as 고객
    participant Server as 주문 서버
    participant DB as DB

    rect rgb(240, 245, 255)
        Note over Customer,DB: [1단계] 주문 요청 및 검증

        Customer->>Server: POST /api/orders (장바구니 항목 ID 목록, 배송 정보)
        Server->>Server: 요청 값 검증 (@Valid)
        Server->>DB: 트랜잭션 시작
        Server->>DB: 본인 장바구니 항목 조회
        DB-->>Server: 장바구니 항목 (상품 ID·수량)

        break 요청한 항목 중 조회되지 않는 항목이 있음
            Server->>DB: 롤백
            Server-->>Customer: 400 잘못된 입력
        end
    end

    rect rgb(255, 252, 235)
        Note over Server,DB: [2단계] 재고 확인 및 차감

        Server->>DB: 주문 상품 조회 (FOR UPDATE, id 순 정렬)
        DB-->>Server: 상품 정보 및 현재 재고 반환
        Server->>Server: 상품별로 판매 상태·재고 확인 후 차감

        break 판매 중이 아니거나 재고 부족 (처음 걸린 상품에서 중단)
            Server->>DB: 전체 롤백
            Server-->>Customer: 400 주문 실패 (예외 메시지)
        end
    end

    rect rgb(240, 255, 242)
        Note over Server,DB: [3단계] 주문 생성

        Server->>Server: 주문 금액 계산 (단가 × 수량 합계)
        Server->>DB: 주문 저장 (PENDING_PAYMENT)
        Server->>DB: 주문 상품 저장 (상품명·단가·수량 스냅샷)
        Server->>DB: 결제 정보 저장 (PENDING)
        Server->>Server: 상품 캐시 무효화
        Server->>DB: 트랜잭션 커밋 (재고 차감 반영)
        DB-->>Server: 저장 완료
    end

    rect rgb(245, 240, 255)
        Note over Customer,Server: [4단계] 주문 완료 및 결제 안내

        Server-->>Customer: 200 OK (orderId, paymentId, portonePaymentId, status: PENDING_PAYMENT)
        Note over Customer: 결제 단계로 진행
    end
```

</details>

---

### 5. 결제

PG 결제 결과를 서버가 **재조회해 금액과 상태를 검증**한 뒤 주문 상태를 확정합니다.

<details>
<summary><b>결제 흐름</b></summary>

```mermaid
sequenceDiagram
    actor 고객
    participant 주문서버
    participant PG
    participant DB

    rect rgba(126,109,255,0.08)
    Note over 고객,DB: [1단계] 주문 생성
    고객->>주문서버: POST /api/orders
    주문서버->>주문서버: 요청 데이터 검증
    주문서버->>DB: 재고 확인 후 주문 저장 (PENDING)
    DB-->>주문서버: 주문 생성 완료 (orderId 반환)
    end

    rect rgba(126,109,255,0.08)
    Note over 고객,DB: [2단계] PG 결제 요청
    주문서버-->>고객: 결제 페이지 정보 응답 (orderId, 금액)
    고객->>PG: 결제 수단 선택 및 결제 진행
    end

    rect rgba(240,140,60,0.08)
    Note over 고객,DB: [3단계] 결제 결과 검증
    PG->>주문서버: Webhook 콜백 (결제 결과 통지)
    주문서버->>PG: 결제 상태 재조회 API 호출
    PG-->>주문서버: 검증된 결제 정보 반환 (금액/상태)
    alt 금액·상태 일치 (결제 성공)
        주문서버->>DB: 주문 상태 PAID로 갱신
        DB-->>주문서버: 갱신 완료
    else 금액 불일치 또는 결제 실패
        주문서버->>DB: 주문 상태 FAILED로 갱신
        주문서버->>PG: 결제 취소 요청 (금액 불일치 시)
    end
    end

    rect rgba(90,200,140,0.08)
    Note over 고객,DB: [4단계] 결과 응답
    주문서버->>DB: 결제 이력 저장
    주문서버-->>고객: 200 OK (결제 완료/실패 결과)
    end
```

</details>

---

### 6. 환불

환불 가능 상태인지 검증하고 PG 취소를 요청한 뒤, 성공하면 재고를 복원하고 주문 상태를 갱신합니다.

<details>
<summary><b>환불 흐름</b></summary>

```mermaid
sequenceDiagram
    actor 고객
    participant 주문서버
    participant PG
    participant DB

    rect rgba(126,109,255,0.08)
        Note over 고객,DB: [1단계] 환불 요청 접수 및 잔액 대조
        고객->>주문서버: POST /api/refunds (paymentId, 환불 사유, 상품별 환불 수량)
        주문서버->>DB: 결제 및 주문 정보 조회
        DB-->>주문서버: 결제 정보 반환
        opt PG 결제 금액이 0원보다 큼
            주문서버->>PG: 결제 조회
            PG-->>주문서버: 결제 금액·취소된 금액 반환
            주문서버->>주문서버: PG 잔액과 DB 잔액 비교
            break 잔액 불일치
                주문서버-->>고객: 409 DB·PG 잔액 불일치
            end
        end
    end

    rect rgba(240,140,60,0.08)
        Note over 고객,DB: [2단계] 환불 검증 및 DB 반영 (하나의 트랜잭션)
        주문서버->>DB: 트랜잭션 시작
        주문서버->>DB: 결제 건 잠금 (FOR UPDATE)
        주문서버->>DB: 결제·주문 상품·기존 환불 내역 조회
        DB-->>주문서버: 조회 결과 반환
        주문서버->>주문서버: 환불 가능 여부 검증
        break 검증 실패
            주문서버->>DB: 롤백
            주문서버-->>고객: 4xx 환불 불가 응답
            Note over 주문서버: 429 5초 내 중복 요청<br/>403 본인 결제 아님<br/>400 환불 불가 결제 상태<br/>404 주문 상품 없음<br/>400 환불 가능 수량 초과
        end
        주문서버->>주문서버: 전액/부분 환불 판단 및 환불 금액 계산
        alt 전액 환불
            주문서버->>DB: 결제 FULL_REFUND, 주문 CANCELED
        else 부분 환불
            주문서버->>DB: 결제 PARTIAL_REFUND (주문 상태 유지)
        end
        주문서버->>DB: 환불 상품 잠금 (FOR UPDATE) 후 재고 복원
        주문서버->>DB: 환불 내역(COMPLETED)·환불 상품 저장
        주문서버->>DB: 회원 누적 구매금액·등급 차감
        주문서버->>DB: 트랜잭션 커밋
    end

    rect rgba(217,90,90,0.08)
        Note over 고객,DB: [3단계] PG 결제 취소
        alt PG 환불 금액 0원
            주문서버->>주문서버: PG 호출 생략 (성공 처리)
        else PG 환불 금액 있음
            주문서버->>PG: 결제 취소/부분취소 요청 (타임아웃 시 최대 3회 시도)
            PG-->>주문서버: 취소 처리 결과 반환
        end
        opt 취소 실패
            주문서버->>DB: 환불 상태 PG_FAILED로 갱신
            Note over 주문서버: 재고·결제 상태는 되돌리지 않음<br/>CRITICAL 로그로 수동 보정 요청
        end
    end

    rect rgba(90,200,140,0.08)
        Note over 고객,DB: [4단계] 환불 결과 응답
        주문서버-->>고객: 200 OK (refundId, status, pgRefundAmount, refundedAt)
    end
```

</details>

---

### 7. 채팅

문의 시작부터 상담원 응대, 상담 종료까지의 WebSocket 흐름입니다.

<details>
<summary><b>실시간 문의 채팅 흐름</b></summary>

```mermaid
    sequenceDiagram
        autonumber
        actor C as 고객 (App)
        participant S as 서버 / DB
        actor A as 관리자 (Admin Web)

        %% 1단계: 문의 시작 및 대기
        rect rgb(240, 245, 255)
        Note over C, S: [1단계] 문의 시작 (상태: WAITING)
        C->>S: POST /api/chat/rooms (방 생성 요청)
        S-->>C: roomId 반환 (status: WAITING)
        %% 불필요한 GET 조회 생략됨!
        Note over C: 빈 화면으로 채팅방 UI 렌더링
        C->>S: WebSocket CONNECT & SUBSCRIBE (/sub/chat/{roomId})
        C->>S: WebSocket PUBLISH ("결제가 안 됩니다")
        S->>S: 메시지 DB 저장
        end

        %% 2단계: 관리자 확인 및 실시간 상담
        rect rgb(245, 255, 245)
        Note over S, A: [2단계] 실시간 상담 (상태: IN_PROGRESS)
        A->>S: GET /api/chat/rooms (전체 방 목록 조회)
        S-->>A: 방 목록 반환 (WAITING 방 식별)
        A->>S: GET /api/chat/rooms/{roomId}/messages (과거 내역 조회)
        S-->>A: 이전 대화 내역 반환 ("결제가 안 됩니다")
        A->>S: WebSocket CONNECT & SUBSCRIBE (/sub/chat/{roomId})
        A->>S: WebSocket PUBLISH ("네, 고객님 확인해 드리겠습니다")
        S-->>C: WebSocket 전송 (관리자 메시지 브로드캐스트)

        Note over C, A: 실시간 양방향 채팅 진행 (PUB / SUB)
        C<<->>A: WebSocket 실시간 대화
        end

        %% 3단계: 상담 종료
        rect rgb(255, 245, 245)
        Note over C, A: [3단계] 상담 종료 (상태: COMPLETED)
        A->>S: PATCH /api/chat/rooms/{roomId}/close (상담 종료 요청)
        S->>S: 방 상태 변경 (status: COMPLETED)
        S-->>C: WebSocket 알림 (상담 종료 시스템 메시지)
        S-->>A: 200 OK (종료 처리 완료)

        Note over C: 입력창 비활성화 (Lock)
        Note over A: 입력창 비활성화 (Lock)
        end
```

</details>

---

### 8. 로그인/회원가입

회원가입은 이메일 중복을 검증한 뒤 기본 등급으로 계정을 만들고, 로그인·로그아웃은 토큰을 발급·무효화합니다.

<details>
<summary><b>로그인 · 회원가입 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor C as 고객 (App)
    participant S as 서버 / DB

    %% 1단계: 회원가입
    rect rgb(240, 245, 255)
    Note over C, S: [1단계] 회원가입 (CODE YELLOW 등급으로 시작)
    C->>S: POST /api/auth/signup (email, password, name, phone)
    S->>S: 이메일 중복 검증
    alt 이메일 중복
    S-->>C: 409 Conflict ("이미 사용 중인 이메일입니다")
    else 검증 통과
    S->>S: 비밀번호 암호화 저장, CODE YELLOW 등급 부여
    S-->>C: 201 Created (자동 로그인 처리, 세션 발급)
    end
    end

    %% 2단계: 로그인
    rect rgb(245, 255, 245)
    Note over C, S: [2단계] 로그인
    C->>S: POST /api/auth/login (email, password)
    S->>S: 저장된 비밀번호와 비교 검증
    alt 인증 성공
    S-->>C: 200 OK (세션/토큰 발급, 로그인 상태 유지)
    else 인증 실패
    S-->>C: 401 Unauthorized ("아이디 또는 비밀번호가 일치하지 않습니다")
    end
    end

    %% 3단계: 로그아웃
    rect rgb(255, 245, 245)
    Note over C, S: [3단계] 로그아웃
    C->>S: POST /api/auth/logout
    S->>S: 세션/토큰 무효화
    S-->>C: 200 OK (로그인 화면으로 이동)
    end
```

</details>

---

### 9. 리뷰

구매가 확정된 주문 상품에만 리뷰를 작성할 수 있고, 주문 상품 하나당 리뷰는 한 건입니다.

<details>
<summary><b>리뷰 작성 · 조회 흐름</b></summary>

```mermaid
sequenceDiagram
    autonumber
    actor C as 고객 (App)
    participant S as 서버 / DB

    %% 1단계: 리뷰 작성
    rect rgb(240, 245, 255)
    Note over C, S: [1단계] 리뷰 작성 (구매 확정 상품 대상)
    C->>S: GET /api/order-items/{orderItemId}/review (기존 리뷰 여부 확인)
    S-->>C: 리뷰 없음 (작성 가능)
    Note over C: 별점 선택 + 내용 입력 후 등록 버튼 클릭
    C->>S: POST /api/reviews (order_item_id, product_id, rating, content)
    S->>S: UNIQUE(order_item_id) 검증 후 리뷰 저장
    S-->>C: 201 Created (등록된 리뷰 반환)
    end

    %% 2단계: 리뷰 목록 조회
    rect rgb(245, 255, 245)
    Note over C, S: [2단계] 리뷰 목록 조회 (상품 상세 페이지)
    C->>S: GET /api/products/{productId}/reviews
    S-->>C: 리뷰 목록 + 평균 평점 + 별점 분포 반환
    Note over C: 리스트 렌더링 (최신순), 새로고침 없이 등록 반영
    end

    %% 3단계: 예외 - 중복 작성
    rect rgb(255, 245, 245)
    Note over C, S: [예외] 이미 리뷰가 존재하는 주문상품
    C->>S: POST /api/reviews (동일 order_item_id)
    S-->>C: 409 Conflict ("이미 작성한 리뷰가 있습니다")
    end
```

</details>

---

## 🛠️ CI / CD

### CI (`.github/workflows/ci.yml`)

`develop`, `main` 대상 PR과 push에서 실행됩니다.

- **frontend** — pnpm 의존성 설치 후 프론트엔드 빌드. 프론트 빌드 실패를 CD의 Docker 빌드가 아니라 PR 단계에서 잡습니다.
- **build** — 백엔드 컴파일, 테스트, JaCoCo 리포트 생성. 커버리지 요약을 PR 코멘트로 남깁니다.

### CD (`.github/workflows/cd.yml`)

`develop`, `main`에 머지되면 실행됩니다. 두 브랜치 모두 직접 push가 막혀 있어 트리거는 사실상 "PR 머지"를 뜻합니다.

1. arm64 러너에서 Docker 이미지 빌드 (배포 대상 EC2가 t4g arm64)
2. GHCR과 ECR에 이미지 push
3. `Project` · `Environment` 태그로 EC2 인스턴스를 조회
4. SSM으로 배포 명령 실행

| 브랜치 | 배포 환경 | `APP_ENV` |
|---|---|---|
| `main` | prod | `prod` |
| `develop` | dev | `dev` |

운영 설정값(DB · Redis · JWT · PortOne)은 AWS Parameter Store(`/murder-help/{APP_ENV}/`)에서 주입합니다.
문서(`**.md`), `docs/`, `.github/` 변경만 있는 경우 CD는 건너뜁니다.

```mermaid
flowchart TB
    subgraph PR["🐙 GitHub · PR 검증"]
        direction LR
        A["📝 PR 생성<br/>develop / main 대상"]
        B["🧪 CI<br/>프론트 빌드 · Java 테스트"]
        C["✅ 리뷰 승인 · 머지"]
        A --> B -->|"통과"| C
    end

    subgraph CD["⚡ GitHub Actions · CD"]
        direction LR
        D["⚡ CD 시작<br/>레지스트리 로그인 · AWS OIDC 인증"]
        E["🐳 ARM64 이미지 빌드<br/>프론트 + Spring Boot"]
        F["📤 이미지 업로드<br/>태그: 브랜치-커밋SHA"]
        D --> E --> F
    end

    PR -->|"머지 → push"| CD

    GHCR[("📦 GHCR<br/>이미지 보관")]
    ECR[("📦 AWS ECR<br/>배포 이미지 저장")]
    ENV{"🎯 배포 환경 선택"}
    DEV["🧪 develop → dev"]
    PROD["🌐 main → prod"]

    CD -->|"이미지 push"| GHCR
    CD -->|"이미지 push"| ECR
    CD -->|"업로드 완료 후"| ENV
    ENV --> DEV
    ENV --> PROD

    subgraph AWS["☁️ AWS · 배포 명령"]
        direction LR
        LOOKUP["🔎 태그로 EC2 조회<br/>Project=murder-help<br/>Environment=dev / prod"]
        SSM["🛰️ SSM Run Command<br/>대상 EC2에 원격 명령 전달"]
        LOOKUP --> SSM
    end

    DEV --> AWS
    PROD --> AWS

    subgraph EC2["🖥️ EC2 · 컨테이너 교체"]
        direction LR
        PULL["📥 ECR 이미지 pull"]
        REPLACE["🔄 기존 컨테이너 삭제<br/>→ 새 컨테이너 실행<br/>8080 · APP_ENV 설정"]
        CHECK["🧹 이미지 정리<br/>docker ps 확인"]
        PULL --> REPLACE --> CHECK
    end

    AWS -.->|"배포 명령 실행"| EC2
    ECR ==>|"이미지 다운로드"| EC2
    EC2 --> DONE["🏁 CD 완료<br/>Actions가 SSM 명령 성공 확인"]

    classDef github fill:#F3E8FF,stroke:#8B5CF6,color:#3B0764
    classDef image fill:#DBEAFE,stroke:#3B82F6,color:#1E3A8A
    classDef aws fill:#FFF3D6,stroke:#F59E0B,color:#78350F
    classDef done fill:#DCFCE7,stroke:#22C55E,color:#14532D

    class A,B,C,D,ENV,DEV,PROD github
    class E,F,GHCR,ECR,PULL image
    class LOOKUP,SSM,REPLACE,CHECK aws
    class DONE done

    style PR fill:transparent,stroke:#C4B5FD
    style CD fill:transparent,stroke:#93C5FD
    style AWS fill:transparent,stroke:#FCD34D
    style EC2 fill:transparent,stroke:#FCD34D
```

---

# 🧑‍💻 Contributors

<a href="https://github.com/prjkmo112"><img src="https://github.com/prjkmo112.png?s=50" width="50px" alt="prjkmo112"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
<a href="https://github.com/OdinAhn"><img src="https://github.com/OdinAhn.png?s=50" width="50px" alt="OdinAhn"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
<a href="https://github.com/wjdals0508"><img src="https://github.com/wjdals0508.png?s=50" width="50px" alt="wjdals0508"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
<a href="https://github.com/yulimlvphs"><img src="https://github.com/yulimlvphs.png?s=50" width="50px" alt="yulimlvphs"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
<a href="https://github.com/Yong-B"><img src="https://github.com/Yong-B.png?s=50" width="50px" alt="Yong-B"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
<a href="https://github.com/zcookiez"><img src="https://github.com/zcookiez.png?s=50" width="50px" alt="zcookiez"/></a>&nbsp;&nbsp;&nbsp;&nbsp;
