# Postman API Documentation — Redis + MySQL Demo

**Base URL:** `http://localhost:8080`

---

## Table of Contents

1. [Product API (CRUD with Redis Cache)](#1-product-api)
   - [Create Product](#11-create-product)
   - [Get Product by ID (Cache HIT / MISS)](#12-get-product-by-id)
   - [Get All Products](#13-get-all-products)
   - [Update Product](#14-update-product)
   - [Delete Product](#15-delete-product)

2. [Redis Data Structure Demos](#2-redis-data-structure-demos)
   - [String](#21-string)
   - [Hash](#22-hash)
   - [List](#23-list)
   - [Set](#24-set)
   - [Sorted Set](#25-sorted-set)
   - [Stream](#26-stream)
   - [Bitmap](#27-bitmap)
   - [HyperLogLog](#28-hyperloglog)

---

---

# 1. Product API

## 1.1 Create Product

| Field | Value |
|---|---|
| **Method** | `POST` |
| **URL** | `http://localhost:8080/api/products` |
| **Response Status** | `201 Created` |

### Steps in Postman
```
1. Open Postman → Click "New Request"
2. Set method to POST
3. Enter URL: http://localhost:8080/api/products
4. Click "Body" tab
5. Select "raw"
6. Select "JSON" from the dropdown (not Text)
7. Paste the JSON below
8. Click "Send"
```

### Request Body (raw JSON)
```json
{
  "name": "iPhone 15 Pro",
  "description": "Apple smartphone with A17 Pro chip",
  "price": 129999.00,
  "quantity": 50
}
```

### Headers (auto-set when you select JSON)
```
Content-Type: application/json
```

### Sample Response
```json
{
  "id": 1,
  "name": "iPhone 15 Pro",
  "description": "Apple smartphone with A17 Pro chip",
  "price": 129999.00,
  "quantity": 50
}
```

### Console Log (proves cache written)
```
[ANNOTATION] CACHE WRITE - saving new product to DB and Redis
```

### Validation Rules
| Field | Rule |
|---|---|
| `name` | Required, cannot be blank |
| `price` | Required, must be greater than 0 |
| `quantity` | Required, cannot be negative |
| `description` | Optional |

---

## 1.2 Get Product by ID

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/products/{id}` |
| **Response Status** | `200 OK` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/products/1
3. No Body needed
4. Click "Send"
```

### URL Examples
```
http://localhost:8080/api/products/1
http://localhost:8080/api/products/2
http://localhost:8080/api/products/5
```

### Sample Response
```json
{
  "id": 1,
  "name": "iPhone 15 Pro",
  "description": "Apple smartphone with A17 Pro chip",
  "price": 129999.00,
  "quantity": 50
}
```

### Cache Behaviour — Watch Console Logs

**1st call** → Cache MISS (hits DB):
```
[ANNOTATION] CACHE MISS - DB called for id: 1. Will cache result.
Hibernate: select * from products where id = 1    ← SQL appears
```

**2nd call (same id)** → Cache HIT (no DB):
```
(no log, no SQL printed)  ← served from Redis ✅
```

---

## 1.3 Get All Products

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/products` |
| **Response Status** | `200 OK` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/products
3. No Body needed
4. Click "Send"
```

### Sample Response
```json
[
  {
    "id": 1,
    "name": "iPhone 15 Pro",
    "description": "Apple smartphone with A17 Pro chip",
    "price": 129999.00,
    "quantity": 50
  },
  {
    "id": 2,
    "name": "Samsung Galaxy S24",
    "description": "Android flagship phone",
    "price": 89999.00,
    "quantity": 30
  }
]
```

### Console Log
```
[ANNOTATION] NO CACHE - getAll always fetches from DB
Hibernate: select * from products     ← always hits DB
```

> ⚠️ `getAll` always hits the DB — no cache used for list operations.

---

## 1.4 Update Product

| Field | Value |
|---|---|
| **Method** | `PUT` |
| **URL** | `http://localhost:8080/api/products/{id}` |
| **Response Status** | `200 OK` |

### Steps in Postman
```
1. Set method to PUT
2. Enter URL: http://localhost:8080/api/products/1
3. Click "Body" tab
4. Select "raw" → "JSON"
5. Paste the JSON below
6. Click "Send"
```

### Request Body (raw JSON)
```json
{
  "name": "iPhone 15 Pro Max",
  "description": "Updated — Apple smartphone with titanium body",
  "price": 159999.00,
  "quantity": 25
}
```

### Sample Response
```json
{
  "id": 1,
  "name": "iPhone 15 Pro Max",
  "description": "Updated — Apple smartphone with titanium body",
  "price": 159999.00,
  "quantity": 25
}
```

### Console Log (proves cache refreshed)
```
[ANNOTATION] CACHE UPDATE - updating DB and refreshing Redis for id: 1
```

---

## 1.5 Delete Product

| Field | Value |
|---|---|
| **Method** | `DELETE` |
| **URL** | `http://localhost:8080/api/products/{id}` |
| **Response Status** | `204 No Content` |

### Steps in Postman
```
1. Set method to DELETE
2. Enter URL: http://localhost:8080/api/products/1
3. No Body needed
4. Click "Send"
5. Response body will be empty (204 No Content)
```

### URL Example
```
http://localhost:8080/api/products/1
```

### Console Log (proves cache evicted)
```
[ANNOTATION] CACHE EVICT - removing from Redis and DB for id: 1
```

> After delete, calling `GET /api/products/1` will return `404 Not Found`

---

---

# 2. Redis Data Structure Demos

> All data structure endpoints are **GET** requests.
> **No Body, No Headers, No Params required.**
> Data is pre-loaded in the service for demonstration.

---

## 2.1 String

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/string` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/string
3. Click "Send"
```

### What it does
- Stores a **session token** with 1-hour TTL
- Stores a **page view counter** and increments it atomically

### Sample Response
```json
{
  "structure": "String",
  "useCase": "Session token storage with auto-expiry",
  "session": "token-abc-xyz-789",
  "pageViews": "8",
  "ttlSeconds": 3598
}
```

### Console Log
```
[STRING] Session stored for user:101
[STRING] Session: token-abc-xyz-789, Page views: 8, TTL: 3598s
```

---

## 2.2 Hash

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/hash` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/hash
3. Click "Send"
```

### What it does
- Stores a **user profile** as a Hash (name, email, role, country)
- Updates only the **role field** without reloading full object

### Sample Response
```json
{
  "structure": "Hash",
  "useCase": "User profile with partial field updates",
  "profile": {
    "name": "John Doe",
    "email": "john@example.com",
    "role": "SUPER_ADMIN",
    "country": "India"
  },
  "fieldCount": 4
}
```

### Console Log
```
[HASH] User profile stored for user:202
[HASH] Role updated to SUPER_ADMIN
[HASH] Profile: {name=John Doe, email=john@example.com, role=SUPER_ADMIN, country=India}
```

---

## 2.3 List

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/list` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/list
3. Click "Send"
```

### What it does
- Pushes **5 user activities** into a Redis List
- Auto-trims to keep only the **latest 5 entries**

### Sample Response
```json
{
  "structure": "List",
  "useCase": "Recent activity feed (latest 5 actions)",
  "activities": [
    "Placed Order #789",
    "Applied Coupon",
    "Added to Cart",
    "Viewed Product #55",
    "Logged in"
  ],
  "count": 5
}
```

### Console Log
```
[LIST] Activity log updated for user:303
[LIST] Recent activities: [Placed Order #789, Applied Coupon, Added to Cart, ...]
```

---

## 2.4 Set

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/set` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/set
3. Click "Send"
```

### What it does
- Tracks **unique visitors** for today and yesterday
- Shows **returning users** (intersection), **new users** (difference), **all users** (union)
- Duplicate entries are automatically ignored

### Sample Response
```json
{
  "structure": "Set",
  "useCase": "Unique visitor tracking and set operations",
  "uniqueToday": 4,
  "returningUsers": ["user2", "user3"],
  "newUsers": ["user1", "user4"],
  "allUsers": ["user1", "user2", "user3", "user4", "user5", "user6"]
}
```

### Console Log
```
[SET] Unique today: 4, Returning: [user2, user3], New: [user1, user4]
```

---

## 2.5 Sorted Set

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/sorted-set` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/sorted-set
3. Click "Send"
```

### What it does
- Adds **5 players** with scores to a leaderboard
- Increments **PlayerAlpha's** score by 2000 (live update)
- Returns **top 3 players** ranked by score

### Sample Response
```json
{
  "structure": "Sorted Set",
  "useCase": "Real-time game leaderboard",
  "top3": [
    { "position": 1, "player": "PlayerDelta",   "score": 9100.0 },
    { "position": 2, "player": "PlayerBeta",    "score": 8500.0 },
    { "position": 3, "player": "PlayerAlpha",   "score": 7000.0 }
  ],
  "playerAlphaRank": 3,
  "playerAlphaScore": 7000.0
}
```

### Console Log
```
[SORTED SET] Top 3: [{position=1, player=PlayerDelta, score=9100.0}, ...]
```

---

## 2.6 Stream

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/stream` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/stream
3. Click "Send"
```

### What it does
- Appends **4 order lifecycle events** to a Redis Stream
- Reads all events back in order (like an audit log)

### Sample Response
```json
{
  "structure": "Stream",
  "useCase": "Order lifecycle event log / audit trail",
  "totalEvents": 4,
  "events": [
    { "id": "1718634001234-0", "data": { "event": "ORDER_PLACED",    "orderId": "ORD-001", "amount": "1500" } },
    { "id": "1718634002345-0", "data": { "event": "PAYMENT_SUCCESS", "orderId": "ORD-001", "method": "UPI" } },
    { "id": "1718634003456-0", "data": { "event": "ORDER_SHIPPED",   "orderId": "ORD-001", "courier": "FedEx" } },
    { "id": "1718634004567-0", "data": { "event": "ORDER_DELIVERED", "orderId": "ORD-001", "rating": "5" } }
  ]
}
```

### Console Log
```
[STREAM] Order events appended
[STREAM] Events read: 4
```

---

## 2.7 Bitmap

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/bitmap` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/bitmap
3. Click "Send"
```

### What it does
- Sets login flags for **users 1, 5, 42, 99** using bit offset = userId
- Checks if **user 1** and **user 10** logged in
- Counts **total logins today**

### Sample Response
```json
{
  "structure": "Bitmap",
  "useCase": "Memory-efficient daily login tracking",
  "user1LoggedIn": true,
  "user10LoggedIn": false,
  "totalLoginsToday": 4,
  "memoryNote": "1 million users tracked in only ~125 KB"
}
```

### Console Log
```
[BITMAP] Login flags set for users 1, 5, 42, 99
[BITMAP] User1 logged in: true, User10 logged in: false, Total: 4
```

---

## 2.8 HyperLogLog

| Field | Value |
|---|---|
| **Method** | `GET` |
| **URL** | `http://localhost:8080/api/redis/hyperloglog` |

### Steps in Postman
```
1. Set method to GET
2. Enter URL: http://localhost:8080/api/redis/hyperloglog
3. Click "Send"
```

### What it does
- Adds visitors to **home page** and **product page** HyperLogLogs
- Duplicates are automatically ignored (counted only once)
- Merges both to count **total unique visitors** across all pages

### Sample Response
```json
{
  "structure": "HyperLogLog",
  "useCase": "Approximate unique visitor count across pages",
  "homePageUnique": 4,
  "productPageUnique": 5,
  "totalUniqueVisitors": 7,
  "memoryNote": "Fixed ~12 KB memory regardless of dataset size",
  "accuracy": "~99.19% accurate (0.81% error rate)"
}
```

### Console Log
```
[HLL] Home unique: 4, Product unique: 5, Total unique: 7
```

---

## Quick Reference — All Endpoints

### Product API

| Method | URL | Body Required | Status |
|---|---|---|---|
| `POST` | `/api/products` | ✅ JSON | 201 Created |
| `GET` | `/api/products` | ❌ None | 200 OK |
| `GET` | `/api/products/{id}` | ❌ None | 200 OK |
| `PUT` | `/api/products/{id}` | ✅ JSON | 200 OK |
| `DELETE` | `/api/products/{id}` | ❌ None | 204 No Content |

### Redis Data Structure Demos

| Method | URL | Body Required | Status |
|---|---|---|---|
| `GET` | `/api/redis/string` | ❌ None | 200 OK |
| `GET` | `/api/redis/hash` | ❌ None | 200 OK |
| `GET` | `/api/redis/list` | ❌ None | 200 OK |
| `GET` | `/api/redis/set` | ❌ None | 200 OK |
| `GET` | `/api/redis/sorted-set` | ❌ None | 200 OK |
| `GET` | `/api/redis/stream` | ❌ None | 200 OK |
| `GET` | `/api/redis/bitmap` | ❌ None | 200 OK |
| `GET` | `/api/redis/hyperloglog` | ❌ None | 200 OK |

---

## Common Errors

| Error | Cause | Fix |
|---|---|---|
| `404 Not Found` | Wrong URL or product ID doesn't exist | Check URL and ID |
| `400 Bad Request` | Missing required field in body | Add `name`, `price`, `quantity` |
| `405 Method Not Allowed` | Wrong HTTP method used | Check method (GET/POST/PUT/DELETE) |
| `500 Internal Server Error` | Redis or MySQL not running | Start Redis (`redis-server`) and MySQL |
| `Connection refused` | App not started | Run the Spring Boot application |

---

*Base URL: `http://localhost:8080` | Server Port: `8080` (configured in application.properties)*
