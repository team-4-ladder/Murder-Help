# API 명세서 — Murder-Help

`develop` 브랜치(`9cddb9b`) 기준으로 작성되었습니다.

## 목차

- [공통 사항](#공통-사항)
- [인증 (Auth)](#1-인증-auth)
- [회원 (Member)](#2-회원-member)
- [상품 (Product)](#3-상품-product)
- [검색 (Search)](#4-검색-search)
- [장바구니 (Cart)](#5-장바구니-cart)
- [주문 (Order)](#6-주문-order)
- [결제 (Payment)](#7-결제-payment)
- [환불 (Refund)](#8-환불-refund)
- [리뷰 (Review)](#9-리뷰-review)
- [채팅 (Chat)](#10-채팅-chat)
- [PortOne 연동 (Config · Webhook)](#11-portone-연동-config--webhook)
- [관리자 (Admin)](#12-관리자-admin)
- [에러 코드](#에러-코드)

---

## 공통 사항

### Base URL

```
/api
```

### 공통 응답 포맷

모든 응답은 아래 형태로 감싸져 내려갑니다. (`ApiResponse<T>`)

**성공**

```json
{
  "code": "SUCCESS",
  "data": { }
}
```

**실패**

```json
{
  "code": "PRODUCT_002",
  "message": "재고가 부족합니다."
}
```

> `data`, `message`는 값이 없으면 응답 JSON에서 생략됩니다. (`@JsonInclude(NON_NULL)`)

모든 API의 성공 응답은 `200 OK`입니다. 생성 API도 `201 Created`가 아닌 `200 OK`로 내려갑니다.

### 페이지 응답 포맷

페이지 응답은 두 가지 형태가 섞여 있습니다. 호출 전에 어느 쪽인지 확인해 주세요.

**(A) `PageResponse<T>` — 상품 목록 · 상품 검색**

```json
{
  "code": "SUCCESS",
  "data": {
    "items": [ ],
    "page": 1,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "hasNext": true
  }
}
```

**(B) Spring Data `Page<T>` 직렬화 — 주문 목록 · 채팅방 목록 · 채팅 메시지 내역**

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [ ],
    "number": 0,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

**공통 페이지 파라미터**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | int | X | `1` | 페이지 번호 (**1부터 시작**, `WebConfig.oneIndexedParameters`) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |
| `sort` | String | X | API별 기본값 | `필드명,asc` / `필드명,desc` 형식 (상품 API의 `sort`는 별도 규칙) |

> 요청 `page`는 1부터 시작하지만, (B) 형태의 응답 `number`는 **0부터** 시작합니다.
> (A) 형태의 응답 `page`는 요청과 같은 1부터 시작하는 번호입니다.

### 인증 방식

로그인 이후 발급받은 JWT를 `Authorization` 헤더에 `Bearer` 스킴으로 담아 요청합니다.

```
Authorization: Bearer {accessToken}
```

- 아래 경로만 인증 없이 호출할 수 있습니다.
  - `POST /api/auth/signup`, `POST /api/auth/login`, `POST /api/auth/reissue`
  - `POST /api/webhooks/portone`
  - `GET /api/searches/popular`
  - `/ws/**` (WebSocket 연결. 연결 후 `StompAuthInterceptor`가 JWT를 검증합니다)
- **그 외 `/api/**` 는 모두 인증이 필요합니다.** 상품 조회·검색도 로그인해야 호출할 수 있습니다.
- `memberId`는 **서버가 JWT에서 직접 추출**합니다. 클라이언트가 body/param으로 넘긴 값은 신뢰하지 않습니다.
- 인증 실패 시 `401 Unauthorized` (`AUTH_001`)가 반환됩니다.
- 리프레시 토큰은 응답 헤더의 `refreshToken` 쿠키로 내려가며, 재발급(`/api/auth/reissue`) 시 쿠키로 전송합니다.

### 등급 권한

회원 등급(`Grade`)과 상품 등급(`ProductTier`)은 같은 체계를 공유하며, 회원은 **자기 등급 이하의 상품만** 조회·구매할 수 있습니다.

| 등급 | 설명 |
|---|---|
| `YELLOW` | 기본 등급 |
| `PURPLE` | 누적 결제 100,000원 이상 |
| `RED` | 누적 결제 500,000원 이상 |
| `GREEN` | 관리자 등급 (전체 상품 접근 + 관리자 API 호출 가능) |

관리자 전용 API(`@PreAuthorize("hasRole('GREEN')")`)를 다른 등급이 호출하면 `403 Forbidden` (`AUTH_002`)가 반환됩니다.

---

## 1. 인증 (Auth)

### 1-1. 회원가입

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/signup` |
| 인증 | 불필요 |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `name` | String | O | 공백 불가 |
| `email` | String | O | 이메일 형식 |
| `password` | String | O | 8자 이상 |
| `phone` | String | O | `010-0000-0000` 형식 |

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "password": "abcd1234!",
  "phone": "010-1234-5678"
}
```

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

### 1-2. 로그인

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/login` |
| 인증 | 불필요 |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `email` | String | O | 공백 불가 |
| `password` | String | O | 공백 불가 |

```json
{
  "email": "user@example.com",
  "password": "abcd1234!"
}
```

**동작 개요**: 액세스 토큰은 응답 body로, 리프레시 토큰은 `refreshToken` 쿠키로 함께 내려갑니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

---

### 1-3. 토큰 재발급

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/reissue` |
| 인증 | 불필요 (`refreshToken` 쿠키 필요) |
| 성공 응답 | `200 OK` |

**Cookie**

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `refreshToken` | String | O | 로그인 시 발급된 리프레시 토큰 |

**동작 개요**: 리프레시 토큰을 검증한 뒤 새 액세스 토큰을 발급하고, 리프레시 토큰 쿠키도 새로 내려줍니다.
쿠키가 없거나 유효하지 않으면 `401 Unauthorized` (`MEMBER_005` / `MEMBER_004`)가 반환됩니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

---

### 1-4. 로그아웃

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/auth/logout` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**동작 개요**: 저장된 리프레시 토큰을 제거하고 `refreshToken` 쿠키를 만료시킵니다.

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

## 2. 회원 (Member)

### 2-1. 내 정보 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/members/me` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "phone": "010-1234-5678",
    "grade": "PURPLE",
    "profileImageUrl": "https://cdn.example.com/profiles/1/uuid.png"
  }
}
```

> `profileImageUrl`은 프로필 이미지를 등록하지 않았으면 `null`입니다.

---

### 2-2. 내 정보 수정

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/members/me` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `name` | String | O | 공백 불가, 최대 50자 |
| `phone` | String | O | 공백 불가, 최대 20자 |

```json
{
  "name": "홍길동",
  "phone": "010-1234-5678"
}
```

**Response Body**: 2-1과 동일한 `MemberResponse`입니다.

---

### 2-3. 프로필 이미지 업로드

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/members/me/profile-image` |
| 인증 | 필요 |
| Content-Type | `multipart/form-data` |
| 성공 응답 | `200 OK` |

**Request Part**

| 파트 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `image` | File | O | 5MB 이하, `image/jpeg` · `image/png` · `image/webp` |

**동작 개요**: S3에 `profiles/{memberId}/{uuid}.{확장자}` 키로 업로드하고, 공개 URL을 회원 정보에 저장합니다.
제약을 어기면 `400 Bad Request` (`COMMON_001`)가 반환됩니다.

**Response Body**: 2-1과 동일한 `MemberResponse`입니다.

---

### 2-4. 프로필 이미지 삭제

| 항목 | 내용 |
|---|---|
| Method | `DELETE` |
| URL | `/api/members/me/profile-image` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**동작 개요**: S3 객체를 삭제하고 `profileImageUrl`을 `null`로 되돌립니다. 등록된 이미지가 없으면 아무 것도 하지 않고 현재 정보를 그대로 반환합니다.

**Response Body**: 2-1과 동일한 `MemberResponse`입니다.

---

## 3. 상품 (Product)

### 3-1. 상품 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/products` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `tier` | String | O | - | 상품 등급. `yellow`, `purple`, `red`, `green` (대소문자 무관) |
| `category` | String | X | - | 카테고리 필터 |
| `subCategory` | String | X | - | 서브 카테고리 필터 |
| `sort` | String | X | `POPULAR` | `POPULAR`(인기순), `PRICE_ASC`(낮은 가격순), `PRICE_DESC`(높은 가격순), `NEWEST`(최신 등록순) |
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

> `tier`는 스펙상 선택 파라미터이지만, 값이 없으면 `ProductTier.fromValue()`에서 예외가 발생해 `400 Bad Request` (`COMMON_001`)가 반환됩니다. 실제로는 필수로 보내야 합니다.
> 본인 등급보다 높은 `tier`를 요청하면 접근이 거부됩니다.

예: `GET /api/products?tier=purple&category=poison&sort=PRICE_ASC&page=1&size=20`

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      {
        "id": 1,
        "productCode": "P-0001",
        "name": "상품명",
        "description": "상품 설명",
        "category": "poison",
        "subCategory": "liquid",
        "price": 10000,
        "tier": "yellow",
        "imageUrl": "https://cdn.example.com/products/1.png",
        "status": "ON_SALE"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 42,
    "totalPages": 3,
    "hasNext": true
  }
}
```

---

### 3-2. 상품 상세 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/products/{productId}` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `productId` | Long | 상품 ID |

**동작 개요**: 회원 등급으로 접근 가능한 상품인지 검증한 뒤 상세 정보와 스펙 목록을 반환합니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 1,
    "productCode": "P-0001",
    "name": "상품명",
    "description": "상품 설명",
    "category": "poison",
    "subCategory": "liquid",
    "price": 10000,
    "stockQuantity": 25,
    "tier": "yellow",
    "imageUrl": "https://cdn.example.com/products/1.png",
    "status": "ON_SALE",
    "specs": [
      {
        "name": "용량",
        "value": "100ml",
        "sortOrder": 1
      }
    ]
  }
}
```

---

### 3-3. 상품 검색 (v1 · 캐시 미적용)

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/v1/products/search` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `tier` | String | O | - | 상품 등급 (3-1과 동일) |
| `keyword` | String | X | - | 상품명 검색어 |
| `sort` | String | X | `POPULAR` | 3-1과 동일 |
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

**동작 개요**: 매 요청마다 DB에서 직접 검색합니다.

**Response Body**: 3-1과 동일한 `PageResponse<ProductResponse>`입니다.

---

### 3-4. 상품 검색 (v2 · 캐시 적용)

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/v2/products/search` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Query Parameters**: 3-3과 동일합니다.

**동작 개요**: v1과 요청·응답 계약이 같고, 캐시(`productSearch`)를 적용한 버전입니다.
등급 접근 검증은 캐시 히트 여부와 무관하게 매 요청마다 먼저 수행합니다.

**Response Body**: 3-1과 동일한 `PageResponse<ProductResponse>`입니다.

---

## 4. 검색 (Search)

### 4-1. 인기 검색어 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/searches/popular` |
| 인증 | 불필요 |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `limit` | int | X | `10` | 조회 개수 (1 ~ 10) |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "rank": 1,
      "keyword": "청산가리",
      "score": 128
    }
  ]
}
```

---

### 4-2. 인기 검색어 집계

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/searches/popular` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `keyword` | String | O | 공백 불가. 집계할 검색어 |

**동작 개요**: 로그인 회원 기준으로 검색어 점수를 올립니다. (동일 회원의 중복 집계 방지 포함)

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

## 5. 장바구니 (Cart)

### 5-1. 장바구니 담기

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/cart/items` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `productId` | Long | O | 1 이상 |
| `quantity` | Integer | O | 1 이상 |

```json
{
  "productId": 1,
  "quantity": 2
}
```

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "id": 10,
    "productId": 1,
    "quantity": 2
  }
}
```

---

### 5-2. 장바구니 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/cart/items` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "id": 10,
      "productId": 1,
      "productCode": "P-0001",
      "name": "상품명",
      "category": "poison",
      "subCategory": "liquid",
      "price": 10000,
      "tier": "yellow",
      "imageUrl": "https://cdn.example.com/products/1.png",
      "status": "ON_SALE",
      "stockQuantity": 25,
      "quantity": 2
    }
  ]
}
```

> 페이징 없이 전체 목록을 배열로 반환합니다.

---

### 5-3. 장바구니 수량 변경

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/cart/items/{cartItemId}` |
| 인증 | 필요 (본인 장바구니 검증) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `cartItemId` | Long | 장바구니 상품 ID |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `quantity` | Integer | O | 1 이상 |

```json
{
  "quantity": 3
}
```

**Response Body**: 5-1과 동일한 `CartItemResponse`입니다.

---

### 5-4. 장바구니 상품 단건 삭제

| 항목 | 내용 |
|---|---|
| Method | `DELETE` |
| URL | `/api/cart/items/{cartItemId}` |
| 인증 | 필요 (본인 장바구니 검증) |
| 성공 응답 | `200 OK` |

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

### 5-5. 장바구니 상품 선택 삭제

| 항목 | 내용 |
|---|---|
| Method | `DELETE` |
| URL | `/api/cart/items` |
| 인증 | 필요 (본인 장바구니 검증) |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `cartItemIds` | Long[] | O | 1개 이상, 중복 불가, 각 값은 1 이상 |

```json
{
  "cartItemIds": [10, 11, 12]
}
```

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

## 6. 주문 (Order)

### 6-1. 주문 생성

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/orders` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `cartItemIds` | Long[] | O | 1개 이상, 중복 불가, 각 값은 1 이상 |
| `receiverName` | String | O | 공백 불가 |
| `receiverPhone` | String | O | 공백 불가 |
| `deliveryAddress` | String | O | 공백 불가 |
| `deliveryRequest` | String | X | 배송 요청사항 |

```json
{
  "cartItemIds": [10, 11],
  "receiverName": "홍길동",
  "receiverPhone": "010-1234-5678",
  "deliveryAddress": "서울시 강남구 테헤란로 1",
  "deliveryRequest": "부재 시 경비실에 맡겨주세요"
}
```

**동작 개요**: 장바구니 항목으로 주문(`PENDING_PAYMENT`)과 결제(`PENDING`)를 함께 생성하고 재고를 차감합니다.
응답의 `portonePaymentId`로 PortOne 결제창을 띄운 뒤 [7-1 결제 승인](#7-1-결제-승인)을 호출하는 흐름입니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 100,
    "paymentId": 300,
    "portonePaymentId": "order-100-1a2b3c",
    "orderNumber": "20260916-000100",
    "status": "PENDING_PAYMENT",
    "totalAmount": 30000
  }
}
```

---

### 6-2. 주문 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/orders` |
| 인증 | 필요 (본인 주문만 조회) |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `period` | String | X | `MONTH_3` | 조회 기간. `MONTH_1`, `MONTH_3`, `MONTH_6`, `MONTH_12`, `ALL` |
| `status` | String | X | - | 주문 상태 필터. `PENDING_PAYMENT`, `PAID`, `PREPARING_DELIVERY`, `SHIPPING`, `DELIVERED`, `CANCELED` |
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

예: `GET /api/orders?period=MONTH_6&status=PAID&page=1&size=20`

**동작 개요**: 최신 주문순(`createdAt DESC`)으로 정렬됩니다. 응답은 Spring Data `Page`를 그대로 직렬화한 (B) 형태입니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "orderId": 100,
        "paymentId": 300,
        "orderNumber": "20260916-000100",
        "status": "PAID",
        "totalAmount": 30000,
        "receiverName": "홍길동",
        "receiverPhone": "010-1234-5678",
        "deliveryAddress": "서울시 강남구 테헤란로 1",
        "deliveryRequest": "부재 시 경비실에 맡겨주세요",
        "items": [
          {
            "productName": "상품명",
            "unitPrice": 10000,
            "quantity": 2,
            "review": null
          }
        ]
      }
    ],
    "number": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

> `items[].review`는 해당 주문 상품에 작성한 리뷰이며, 아직 작성하지 않았으면 `null`입니다.

---

### 6-3. 주문 상세 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/orders/{orderId}` |
| 인증 | 필요 (본인 주문만 조회) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `orderId` | Long | 주문 ID |

**동작 개요**: 다른 회원의 주문을 조회하면 `404 Not Found` (`ORDER_001`)로 응답합니다.

**Response Body**: 6-2 `content[]` 원소와 동일한 `OrderResponse`입니다.

---

## 7. 결제 (Payment)

### 7-1. 결제 승인

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/payments/confirm` |
| 인증 | 필요 (본인 주문 검증) |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `orderId` | Long | O | 필수 |
| `portonePaymentId` | String | O | 공백 불가. 주문 생성 응답의 값 |

```json
{
  "orderId": 100,
  "portonePaymentId": "order-100-1a2b3c"
}
```

**동작 개요**: PortOne 결제창 완료 후 호출합니다. 서버가 **주문 소유자 · 중복 처리 · PG 결제 상태 · 결제 금액**을 재검증한 뒤 결제와 주문 상태를 확정합니다.

```text
Payment  PENDING -> COMPLETED
Order    PENDING_PAYMENT -> PAID
```

- 이미 처리된 결제는 `409 Conflict` (`PAYMENT_005`)로 거부됩니다.
- PG 결제가 완료되지 않았으면 `400 Bad Request` (`PAYMENT_004`), 금액이 다르면 `400 Bad Request` (`PAYMENT_002`)로 거부되고 결제는 `FAILED`로 기록됩니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "paymentId": 300,
    "orderId": 100,
    "amount": 30000,
    "paymentStatus": "COMPLETED",
    "orderStatus": "PAID",
    "message": "결제가 완료되었습니다."
  }
}
```

---

### 7-2. 결제 취소

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/payments/{id}/cancel` |
| 인증 | 필요 (본인 결제 검증) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 결제 ID |

**Request Body** (생략 가능)

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `reason` | String | X | 최대 500자 |

```json
{
  "reason": "결제 화면에서 이탈"
}
```

**동작 개요**: **PG 승인 전** 결제 대기 상태의 결제를 취소합니다. 주문은 `CANCELED`로 전이되고 재고가 복구됩니다.
결제가 이미 완료된 건은 취소가 아니라 [8-1 환불 요청](#8-1-환불-요청)을 사용해야 합니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "paymentId": 300,
    "orderId": 100,
    "portonePaymentId": "order-100-1a2b3c",
    "paymentStatus": "CANCELLED",
    "orderStatus": "CANCELED",
    "message": "결제가 취소되었습니다."
  }
}
```

---

### 7-3. 결제 실패 처리

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/payments/{id}/fail` |
| 인증 | 필요 (로그인만 확인, 본인 결제 검증 없음) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `id` | Long | 결제 ID |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `failReason` | String | O | `PG_DECLINED`, `AMOUNT_MISMATCH`, `USER_CANCELLED` |

```json
{
  "failReason": "PG_DECLINED"
}
```

**동작 개요**: 프론트에서 PG 결제가 실패했을 때 호출합니다. 결제를 `FAILED`로 기록하고 주문을 취소 처리합니다.

**Response Body**: 7-2와 동일한 `PaymentCancelResponse`입니다.

---

## 8. 환불 (Refund)

### 8-1. 환불 요청

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/refunds` |
| 인증 | 필요 (본인 결제 검증) |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `paymentId` | Long | O | 필수 |
| `cancelReason` | String | O | 공백 불가 |
| `items` | Object[] | O | 1개 이상 |
| `items[].orderItemId` | Long | O | 필수 |
| `items[].requestQuantity` | int | O | 1 이상 |

```json
{
  "paymentId": 300,
  "cancelReason": "단순 변심",
  "items": [
    {
      "orderItemId": 500,
      "requestQuantity": 1
    }
  ]
}
```

**동작 개요**: 주문 상품 단위 **부분 환불**을 지원합니다. 환불 가능 수량을 검증한 뒤 PG 취소를 요청하고, 환불 금액에 따라 결제 상태를 전이시킵니다.

```text
Payment  COMPLETED -> PARTIAL_REFUND 또는 FULL_REFUND
Order    전액 환불이면 CANCELED
재고      환불 수량만큼 복구
```

- 본인 결제가 아니면 `403 Forbidden` (`REFUND_002`)로 거부됩니다.
- 잔여 환불 가능 수량을 넘기면 `400 Bad Request` (`REFUND_005`)로 거부됩니다.
- 동일 결제에 대한 환불이 처리 중이면 `429 Too Many Requests` (`REFUND_007`)가 반환됩니다.
- PG 취소가 실패하면 환불은 `PG_FAILED`로 기록됩니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "refundId": 50,
    "status": "COMPLETED",
    "pgRefundAmount": 10000,
    "refundedAt": "2026-09-16T10:00:00"
  }
}
```

> `status`는 `COMPLETED`(환불 완료) / `PG_FAILED`(PG 취소 실패) 중 하나입니다.

---

### 8-2. 환불 가능 상품 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/refunds/refundable/{paymentId}` |
| 인증 | 필요 (본인 결제 검증) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `paymentId` | Long | 결제 ID |

**동작 개요**: 주문 수량에서 이미 환불된 수량을 뺀 잔여 환불 가능 수량을 계산해 반환합니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "orderItemId": 500,
      "productName": "상품명",
      "orderPrice": 10000,
      "originalQuantity": 2,
      "remainQuantity": 1
    }
  ]
}
```

---

### 8-3. 환불 이력 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/refunds/history/{paymentId}` |
| 인증 | 필요 (본인 결제 검증) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `paymentId` | Long | 결제 ID |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "refundId": 50,
      "refundDate": "2026-09-16T10:00:00",
      "status": "COMPLETED",
      "pgRefundAmount": 10000,
      "items": [
        {
          "refundItemId": 70,
          "productName": "상품명",
          "refundQuantity": 1,
          "itemRefundAmount": 10000
        }
      ]
    }
  ]
}
```

---

## 9. 리뷰 (Review)

### 9-1. 리뷰 작성

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/reviews` |
| 인증 | 필요 (본인 주문 상품 검증) |
| 성공 응답 | `200 OK` |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `orderItemId` | Long | O | 필수 |
| `rating` | Integer | O | 1 ~ 5 |
| `content` | String | O | 공백 불가 |

```json
{
  "orderItemId": 500,
  "rating": 5,
  "content": "배송이 빠릅니다."
}
```

**동작 개요**: **배송 완료(`DELIVERED`)된 주문 상품**만 리뷰를 작성할 수 있고, 주문 상품 하나당 리뷰는 한 건입니다.
조건을 어기면 `400 Bad Request` (`COMMON_001`), 본인 주문이 아니면 `403 Forbidden` (`AUTH_002`)가 반환됩니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "reviewId": 900,
    "orderItemId": 500,
    "productId": 1,
    "rating": 5,
    "content": "배송이 빠릅니다.",
    "createdAt": "2026-09-16T10:00:00",
    "updatedAt": "2026-09-16T10:00:00"
  }
}
```

---

### 9-2. 리뷰 수정

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/reviews/{reviewId}` |
| 인증 | 필요 (작성자 본인만) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `reviewId` | Long | 리뷰 ID |

**Request Body**

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `rating` | Integer | O | 1 ~ 5 |
| `content` | String | O | 공백 불가 |

```json
{
  "rating": 4,
  "content": "생각보다 무난합니다."
}
```

**Response Body**: 9-1과 동일한 `ReviewResponse`입니다.

---

### 9-3. 리뷰 삭제

| 항목 | 내용 |
|---|---|
| Method | `DELETE` |
| URL | `/api/reviews/{reviewId}` |
| 인증 | 필요 (작성자 본인만) |
| 성공 응답 | `200 OK` |

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

### 9-4. 작성 가능한 리뷰 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/reviews/pending` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**동작 개요**: 배송이 완료됐고 아직 리뷰를 쓰지 않은 주문 상품 목록을 반환합니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "orderItemId": 500,
      "productCode": "P-0001",
      "productName": "상품명",
      "purchasedAt": "2026-09-10T10:00:00",
      "imageUrl": "https://cdn.example.com/products/1.png"
    }
  ]
}
```

---

### 9-5. 내가 쓴 리뷰 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/reviews/me` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "reviewId": 900,
      "orderItemId": 500,
      "productId": 1,
      "productCode": "P-0001",
      "productName": "상품명",
      "rating": 5,
      "content": "배송이 빠릅니다.",
      "createdAt": "2026-09-16T10:00:00",
      "updatedAt": "2026-09-16T10:00:00"
    }
  ]
}
```

---

### 9-6. 상품별 리뷰 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/products/{productId}/reviews` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `productId` | Long | 상품 ID |

**동작 개요**: 해당 상품의 리뷰를 최신순으로 반환합니다. 페이징은 없습니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "reviewId": 900,
      "orderItemId": 500,
      "productId": 1,
      "rating": 5,
      "content": "배송이 빠릅니다.",
      "createdAt": "2026-09-16T10:00:00",
      "updatedAt": "2026-09-16T10:00:00"
    }
  ]
}
```

---

## 10. 채팅 (Chat)

### 10-1. 채팅방 생성

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/chat/rooms` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**동작 개요**: 문의 채팅방을 만들고 봇 인사 메시지를 함께 발송합니다. 진행 중인 상담이 이미 있으면 `400 Bad Request` (`CHAT_003`)로 거부됩니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "roomId": 7,
    "title": "홍길동님의 문의",
    "status": "BOT_MODE",
    "createdAt": "2026-09-16T10:00:00",
    "updatedAt": "2026-09-16T10:00:00",
    "lastMessage": "무엇을 도와드릴까요?",
    "customerProfileImageUrl": "https://cdn.example.com/profiles/1/uuid.png"
  }
}
```

---

### 10-2. 내 채팅방 목록 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/chat/rooms/my` |
| 인증 | 필요 (본인 채팅방만) |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `status` | String | X | - | `BOT_MODE`, `WAITING`, `IN_PROGRESS`, `COMPLETED` |
| `keyword` | String | X | - | 검색어 |
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

**동작 개요**: 마지막 메시지 최신순으로 정렬됩니다. 응답은 Spring Data `Page`를 그대로 직렬화한 (B) 형태이며, `content[]` 원소는 10-1의 `ChatRoomResponse`입니다.

---

### 10-3. 전체 채팅방 목록 조회 (관리자)

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/chat/rooms` |
| 인증 | 필요 (`GREEN` 등급만) |
| 성공 응답 | `200 OK` |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `customerId` | Long | X | - | 특정 고객의 채팅방만 조회 |
| `status` | String | X | - | `BOT_MODE`, `WAITING`, `IN_PROGRESS`, `COMPLETED` |
| `keyword` | String | X | - | 검색어 |
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

**Response Body**: 10-2와 동일한 형태입니다.

---

### 10-4. 채팅방 단건 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/chat/rooms/{roomId}` |
| 인증 | 필요 (참여자 또는 관리자) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `roomId` | Long | 채팅방 ID |

**동작 개요**: 참여자가 아니면 `403 Forbidden` (`CHAT_004`)가 반환됩니다.

**Response Body**: 10-1과 동일한 `ChatRoomResponse`입니다.

---

### 10-5. 채팅 메시지 내역 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/chat/rooms/{roomId}/messages` |
| 인증 | 필요 (참여자 또는 관리자) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `roomId` | Long | 채팅방 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---|---|---|---|---|
| `page` | int | X | `1` | 페이지 번호 (1부터 시작) |
| `size` | int | X | `20` | 페이지 크기 (최대 100) |

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 3001,
        "roomId": 7,
        "memberId": 1,
        "senderEmail": "user@example.com",
        "senderName": "홍길동",
        "senderGrade": "PURPLE",
        "senderProfileImageUrl": "https://cdn.example.com/profiles/1/uuid.png",
        "content": "주문 취소하고 싶습니다.",
        "messageType": "TEXT",
        "createdAt": "2026-09-16T10:00:00"
      }
    ],
    "number": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

> `messageType`은 `TEXT`(일반 메시지), `SYSTEM`(시스템 안내), `BUTTON`(봇 선택지) 중 하나입니다.

---

### 10-6. 상담 종료

| 항목 | 내용 |
|---|---|
| Method | `PATCH` |
| URL | `/api/chat/rooms/{roomId}/close` |
| 인증 | 필요 (참여자 또는 관리자) |
| 성공 응답 | `200 OK` |

**Path Variables**

| 파라미터 | 타입 | 설명 |
|---|---|---|
| `roomId` | Long | 채팅방 ID |

**동작 개요**: 채팅방을 `COMPLETED`로 전이시킵니다. 종료된 방에서는 더 이상 메시지를 보낼 수 없습니다.

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

### 10-7. 채팅방 캐시 복구 (관리자)

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/chat/rooms/cache/recover` |
| 인증 | 필요 (`GREEN` 등급만) |
| 성공 응답 | `200 OK` |

**동작 개요**: Redis의 `chat_last_messages` 캐시를 DB 기준으로 다시 채웁니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": "12개 채팅방 캐시 복구 완료"
}
```

---

## 11. PortOne 연동 (Config · Webhook)

### 11-1. PortOne 설정 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/config/portone` |
| 인증 | 필요 |
| 성공 응답 | `200 OK` |

**동작 개요**: 프론트엔드가 PortOne 결제창을 띄울 때 사용하는 공개 설정값을 내려줍니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": {
    "storeId": "store-xxxxxxxx",
    "channelKey": "channel-key-xxxxxxxx"
  }
}
```

---

### 11-2. PortOne 웹훅 수신

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/webhooks/portone` |
| 인증 | 불필요 (웹훅 서명으로 검증) |
| 성공 응답 | `200 OK` |

**Headers**

| 헤더 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `webhook-id` | String | O | 웹훅 식별자 |
| `webhook-timestamp` | String | O | 웹훅 발송 시각 |
| `webhook-signature` | String | O | 웹훅 서명 |

**Request Body**: PortOne이 전송하는 원본 JSON 문자열입니다.

**동작 개요**: 서명을 검증한 뒤 이벤트를 처리하고 `WebhookEvent`로 기록합니다.
서명 검증에 실패해도 재전송을 막기 위해 `200 OK`로 응답합니다.

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

### 11-3. 웹훅 이벤트 조회

| 항목 | 내용 |
|---|---|
| Method | `GET` |
| URL | `/api/webhooks` |
| 인증 | 필요 (본인 결제 건만) |
| 성공 응답 | `200 OK` |

**동작 개요**: 로그인한 회원의 결제·주문에 연결된 웹훅 이벤트만 반환합니다.

**Response Body**

```json
{
  "code": "SUCCESS",
  "data": [
    {
      "id": 1,
      "paymentId": 300,
      "webhookId": "whk_xxxxxxxx",
      "eventType": "Transaction.Paid",
      "status": "PROCESSED",
      "payload": "{\"type\":\"Transaction.Paid\", ...}",
      "processedAt": "2026-09-16T10:00:01",
      "failReason": null,
      "createdAt": "2026-09-16T10:00:00",
      "updatedAt": "2026-09-16T10:00:01"
    }
  ]
}
```

> `status`는 `RECEIVED`(수신), `PROCESSED`(처리 완료), `IGNORED`(처리 대상 아님), `FAILED`(처리 실패) 중 하나입니다.

---

## 12. 관리자 (Admin)

### 12-1. 상품 랭킹 수동 갱신

| 항목 | 내용 |
|---|---|
| Method | `POST` |
| URL | `/api/admin/product-rankings/refresh` |
| 인증 | 필요 (`GREEN` 등급만) |
| 성공 응답 | `200 OK` |

**동작 개요**: 매일 새벽 3시에 도는 상품 랭킹 배치를 수동으로 실행합니다. 다중 인스턴스 중복 실행은 ShedLock(Redis)으로 막습니다.

**Response Body**

```json
{
  "code": "SUCCESS"
}
```

---

## 에러 코드

### 공통

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `COMMON_001` | 400 | 잘못된 입력값입니다. / 입력값이 올바르지 않습니다. |
| `COMMON_002` | 500 | 서버 에러가 발생했습니다. |
| `COMMON_003` | 404 | 요청한 리소스를 찾을 수 없습니다. |

### 인증

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `AUTH_001` | 401 | 인증이 필요합니다. |
| `AUTH_002` | 403 | 권한이 없습니다. |
| `AUTH_003` | 409 | 이미 가입된 이메일 입니다. |
| `AUTH_004` | 404 | 이메일 또는 비밀번호가 올바르지 않습니다. |

### 회원

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `MEMBER_001` | 404 | 회원을 찾을 수 없습니다. |
| `MEMBER_002` | 409 | 이미 존재하는 이메일입니다. |
| `MEMBER_003` | 401 | 이메일 또는 비밀번호가 올바르지 않습니다. |
| `MEMBER_004` | 401 | 유효하지 않은 리프레시 토큰입니다. |
| `MEMBER_005` | 401 | 리프레시 토큰을 찾을 수 없습니다. |

### 상품

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `PRODUCT_001` | 404 | 상품을 찾을 수 없습니다. |
| `PRODUCT_002` | 409 | 재고가 부족합니다. |
| `PRODUCT_003` | 400 | 가격은 0 이상이어야 합니다. |
| `PRODUCT_004` | 400 | 재고는 0 이상이어야 합니다. |
| `PRODUCT_005` | 409 | 현재 판매하지 않는 상품입니다. |

### 장바구니

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `CART_001` | 404 | 장바구니 상품을 찾을 수 없습니다. |

### 주문

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `ORDER_001` | 404 | 주문을 찾을 수 없습니다. |
| `ORDER_002` | 400 | 유효하지 않은 주문 상태 변경입니다. |
| `ORDER_003` | 403 | 해당 주문에 접근할 권한이 없습니다. |
| `ORDER_004` | 409 | 결제대기 상태의 주문만 취소할 수 있습니다. |

### 결제

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `PAYMENT_001` | 404 | 결제 정보를 찾을 수 없습니다. |
| `PAYMENT_002` | 400 | 결제 금액이 일치하지 않습니다. |
| `PAYMENT_003` | 400 | 유효하지 않은 결제 상태 변경입니다. |
| `PAYMENT_004` | 400 | PG사 결제가 완료되지 않았습니다. |
| `PAYMENT_005` | 409 | 이미 처리된 결제입니다. |

### 환불

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `REFUND_001` | 404 | 존재하지 않는 환불 건입니다. |
| `REFUND_002` | 403 | 본인의 결제 건만 환불할 수 있습니다. |
| `REFUND_003` | 400 | 환불 가능한 결제 상태가 아닙니다. |
| `REFUND_004` | 404 | 환불 대상 상품이 존재하지 않습니다. |
| `REFUND_005` | 400 | 잔여 환불 가능 수량을 초과했습니다. |
| `REFUND_006` | 409 | DB와 PG사의 결제 잔액이 일치하지 않습니다. |
| `REFUND_007` | 429 | 환불 처리가 진행 중입니다. 잠시 후 다시 시도해주세요. |
| `REFUND_008` | 400 | 수량은 1 이상이어야 합니다. |

### 리뷰

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `REVIEW_001` | 404 | 리뷰를 찾을 수 없습니다. |

### 채팅

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `CHAT_001` | 404 | 채팅방을 찾을 수 없습니다. |
| `CHAT_002` | 400 | 유효하지 않은 채팅방 상태 변경입니다. |
| `CHAT_003` | 400 | 이미 진행 중인 상담이 존재합니다. |
| `CHAT_004` | 403 | 해당 채팅방에 접근할 권한이 없습니다. |

### 웹훅

| 코드 | HTTP Status | 메시지 |
|---|---|---|
| `WEBHOOK_001` | 401 | 웹훅 서명이 유효하지 않습니다. |
| `WEBHOOK_002` | 404 | 웹훅 이벤트를 찾을 수 없습니다. |
