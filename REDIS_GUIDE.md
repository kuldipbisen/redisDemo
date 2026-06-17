# Redis - Complete Guide

---

## 1. What is Redis?

**Redis** (Remote Dictionary Server) is an open-source, **in-memory data structure store** used as:
- A **cache**
- A **database**
- A **message broker**

It stores data in **RAM** (not disk), making it extremely fast — capable of handling **millions of operations per second**.

> Created by Salvatore Sanfilippo in 2009. Now maintained by Redis Ltd.

---

## 2. Why Use Redis?

| Problem | Without Redis | With Redis |
|---|---|---|
| Repeated DB queries | DB hit every request (slow) | Served from memory (fast) |
| Session storage | Stored in DB or server memory | Centralized, scalable |
| Rate limiting | Complex DB queries | Atomic counters in Redis |
| Real-time leaderboard | Expensive SQL sorting | Sorted sets (instant) |
| Pub/Sub messaging | Need a message queue setup | Built into Redis |

### Key Advantages
- ⚡ **Ultra-fast** — sub-millisecond latency (data lives in RAM)
- 🔄 **Versatile data structures** — strings, lists, sets, hashes, sorted sets, streams
- ⏰ **Built-in TTL** — auto-expire keys without manual cleanup
- 📦 **Atomic operations** — thread-safe without locks
- 🔁 **Persistence options** — can save to disk (RDB / AOF)
- 🌐 **Cluster support** — horizontal scaling

---

## 3. Redis Data Structures

| Structure | Description | Example Use Case | API Endpoint |
|---|---|---|---|
| **String** | Simple key-value | Session token, page view counter | `GET /api/redis/string` |
| **Hash** | Map of field-value pairs | User profile with partial updates | `GET /api/redis/hash` |
| **List** | Ordered list (push/pop) | Recent activity feed | `GET /api/redis/list` |
| **Set** | Unique unordered values | Unique visitors, tag management | `GET /api/redis/set` |
| **Sorted Set** | Set with scores | Real-time leaderboard | `GET /api/redis/sorted-set` |
| **Stream** | Append-only log | Order lifecycle event log | `GET /api/redis/stream` |
| **Bitmap** | Bit-level operations | Daily login tracking | `GET /api/redis/bitmap` |
| **HyperLogLog** | Approximate unique count | Unique page visitor count | `GET /api/redis/hyperloglog` |

---

### 3.1 String
> Simplest type. Stores text, numbers, or serialized objects. Max size 512MB.

**Use Case in App:** Store user session token with 1-hour TTL + atomic page view counter

**CLI:**
```bash
SET session:user:101 "token-abc-xyz-789" EX 3600   # session with TTL
GET session:user:101

SET pageviews:home 0
INCR pageviews:home          # atomic increment → 1
INCRBY pageviews:home 5      # add 5 → 6
TTL session:user:101         # seconds remaining
```

**Java (Service — `RedisDataStructureService.java`):**
```java
ValueOperations<String, String> ops = redisTemplate.opsForValue();

ops.set("session:user:101", "token-abc-xyz-789", Duration.ofHours(1));  // with TTL
ops.set("pageviews:home", "0");
ops.increment("pageviews:home", 5);                  // atomic increment

String session   = ops.get("session:user:101");
String pageViews = ops.get("pageviews:home");
Long   ttl       = redisTemplate.getExpire("session:user:101");
```

**Sample Response (`GET /api/redis/string`):**
```json
{
  "structure": "String",
  "useCase": "Session token storage with auto-expiry",
  "session": "token-abc-xyz-789",
  "pageViews": "8",
  "ttlSeconds": 3598
}
```

---

### 3.2 Hash
> A map of field-value pairs stored under one key. Like a row in a DB table.

**Use Case in App:** Store user profile — update only one field without reloading the full object

**CLI:**
```bash
HSET user:profile:202 name "John Doe" email "john@example.com" role "ADMIN" country "India"
HGET user:profile:202 name           # "John Doe"
HGETALL user:profile:202             # all fields
HPUT user:profile:202 role "SUPER_ADMIN"  # update single field
HLEN user:profile:202                # field count
```

**Java (Service — `RedisDataStructureService.java`):**
```java
HashOperations<String, String, String> ops = redisTemplate.opsForHash();
String key = "user:profile:202";

ops.put(key, "name",    "John Doe");
ops.put(key, "email",   "john@example.com");
ops.put(key, "role",    "ADMIN");

// Update only role — no need to reload full object
ops.put(key, "role", "SUPER_ADMIN");

Map<String, String> profile = ops.entries(key);   // full profile
long fieldCount = ops.size(key);
```

**Sample Response (`GET /api/redis/hash`):**
```json
{
  "structure": "Hash",
  "useCase": "User profile with partial field updates",
  "profile": { "name": "John Doe", "email": "john@example.com", "role": "SUPER_ADMIN", "country": "India" },
  "fieldCount": 4
}
```

---

### 3.3 List
> Ordered collection. Supports push/pop from both ends. Ideal for queues and activity feeds.

**Use Case in App:** Store last 5 user activities — auto-trim older entries

**CLI:**
```bash
LPUSH activity:user:303 "Logged in"
LPUSH activity:user:303 "Viewed Product #55"
LPUSH activity:user:303 "Added to Cart"
LPUSH activity:user:303 "Applied Coupon"
LPUSH activity:user:303 "Placed Order #789"
LTRIM activity:user:303 0 4              # keep only last 5
LRANGE activity:user:303 0 -1            # get all
LLEN activity:user:303                   # count
```

**Java (Service — `RedisDataStructureService.java`):**
```java
ListOperations<String, String> ops = redisTemplate.opsForList();
String key = "activity:user:303";

ops.leftPush(key, "Logged in");
ops.leftPush(key, "Placed Order #789");
ops.trim(key, 0, 4);                          // keep only last 5

List<String> activities = ops.range(key, 0, -1);
Long size = ops.size(key);
```

**Sample Response (`GET /api/redis/list`):**
```json
{
  "structure": "List",
  "useCase": "Recent activity feed (latest 5 actions)",
  "activities": ["Placed Order #789", "Applied Coupon", "Added to Cart", "Viewed Product #55", "Logged in"],
  "count": 5
}
```

---

### 3.4 Set
> Unordered collection of **unique** strings. Supports union, intersection, difference.

**Use Case in App:** Track unique page visitors, find new vs returning users

**CLI:**
```bash
SADD visitors:2024-06-17 "user1" "user2" "user3" "user4" "user1"  # duplicate ignored
SADD visitors:2024-06-16 "user2" "user3" "user5" "user6"
SCARD visitors:2024-06-17                         # unique count today
SINTER visitors:2024-06-17 visitors:2024-06-16    # returning users
SDIFF  visitors:2024-06-17 visitors:2024-06-16    # new users today
SUNION visitors:2024-06-17 visitors:2024-06-16    # all users both days
```

**Java (Service — `RedisDataStructureService.java`):**
```java
SetOperations<String, String> ops = redisTemplate.opsForSet();

ops.add("visitors:2024-06-17", "user1", "user2", "user3", "user4", "user1"); // dupe ignored
ops.add("visitors:2024-06-16", "user2", "user3", "user5", "user6");

long uniqueToday     = ops.size("visitors:2024-06-17");
Set<String> returning = ops.intersect("visitors:2024-06-17", "visitors:2024-06-16");
Set<String> newUsers  = ops.difference("visitors:2024-06-17", "visitors:2024-06-16");
Set<String> allUsers  = ops.union("visitors:2024-06-17", "visitors:2024-06-16");
```

**Sample Response (`GET /api/redis/set`):**
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

---

### 3.5 Sorted Set
> Like Set but each member has a **score**. Always sorted by score. Perfect for rankings.

**Use Case in App:** Real-time game leaderboard with live score updates

**CLI:**
```bash
ZADD leaderboard:game 5000 "PlayerAlpha"
ZADD leaderboard:game 8500 "PlayerBeta"
ZADD leaderboard:game 9100 "PlayerDelta"
ZINCRBY leaderboard:game 2000 "PlayerAlpha"     # add points
ZREVRANGE leaderboard:game 0 2 WITHSCORES       # top 3
ZREVRANK leaderboard:game "PlayerAlpha"         # rank (0-based)
ZSCORE leaderboard:game "PlayerAlpha"           # current score
```

**Java (Service — `RedisDataStructureService.java`):**
```java
ZSetOperations<String, String> ops = redisTemplate.opsForZSet();

ops.add("leaderboard:game", "PlayerAlpha",   5000);
ops.add("leaderboard:game", "PlayerBeta",    8500);
ops.add("leaderboard:game", "PlayerDelta",   9100);
ops.incrementScore("leaderboard:game", "PlayerAlpha", 2000);  // live score update

Set<ZSetOperations.TypedTuple<String>> top3 =
        ops.reverseRangeWithScores("leaderboard:game", 0, 2);

Long rank   = ops.reverseRank("leaderboard:game", "PlayerAlpha");
Double score = ops.score("leaderboard:game", "PlayerAlpha");
```

**Sample Response (`GET /api/redis/sorted-set`):**
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

---

### 3.6 Stream
> Append-only log. Consumers read messages in order. Supports consumer groups for distributed processing.

**Use Case in App:** Order lifecycle event log — track each state change as an event

**CLI:**
```bash
XADD orders * event ORDER_PLACED   orderId ORD-001 userId 101 amount 1500
XADD orders * event PAYMENT_SUCCESS orderId ORD-001 method UPI
XADD orders * event ORDER_SHIPPED  orderId ORD-001 courier FedEx
XADD orders * event ORDER_DELIVERED orderId ORD-001 rating 5
XRANGE orders - +                  # read all events
XLEN orders                        # total event count
```

**Java (Service — `RedisDataStructureService.java`):**
```java
StreamOperations<String, String, String> ops = redisTemplate.opsForStream();

ops.add("stream:orders", Map.of("event", "ORDER_PLACED",    "orderId", "ORD-001", "amount", "1500"));
ops.add("stream:orders", Map.of("event", "PAYMENT_SUCCESS", "orderId", "ORD-001", "method", "UPI"));
ops.add("stream:orders", Map.of("event", "ORDER_SHIPPED",   "orderId", "ORD-001", "courier", "FedEx"));
ops.add("stream:orders", Map.of("event", "ORDER_DELIVERED", "orderId", "ORD-001", "rating", "5"));

List<MapRecord<String, String, String>> records =
        ops.range("stream:orders", Range.unbounded());
```

**Sample Response (`GET /api/redis/stream`):**
```json
{
  "structure": "Stream",
  "useCase": "Order lifecycle event log / audit trail",
  "totalEvents": 4,
  "events": [
    { "id": "1718634001-0", "data": { "event": "ORDER_PLACED",    "orderId": "ORD-001" } },
    { "id": "1718634002-0", "data": { "event": "PAYMENT_SUCCESS", "orderId": "ORD-001" } },
    { "id": "1718634003-0", "data": { "event": "ORDER_SHIPPED",   "orderId": "ORD-001" } },
    { "id": "1718634004-0", "data": { "event": "ORDER_DELIVERED", "orderId": "ORD-001" } }
  ]
}
```

---

### 3.7 Bitmap
> Treats a string as an array of bits. Extremely memory-efficient for boolean flags per user ID.
> **1 million users = only ~125 KB in Redis**

**Use Case in App:** Track which users logged in today using bit offset = userId

**CLI:**
```bash
SETBIT logins:2024-06-17 1  1    # user 1  logged in
SETBIT logins:2024-06-17 5  1    # user 5  logged in
SETBIT logins:2024-06-17 42 1    # user 42 logged in
SETBIT logins:2024-06-17 99 1    # user 99 logged in

GETBIT logins:2024-06-17 1       # 1 (logged in)
GETBIT logins:2024-06-17 10      # 0 (not logged in)
BITCOUNT logins:2024-06-17       # 4 (total logins today)

# Users active on BOTH days
BITOP AND result logins:2024-06-17 logins:2024-06-16
BITCOUNT result
```

**Java (Service — `RedisDataStructureService.java`):**
```java
redisTemplate.opsForValue().setBit("logins:2024-06-17", 1,  true);
redisTemplate.opsForValue().setBit("logins:2024-06-17", 5,  true);
redisTemplate.opsForValue().setBit("logins:2024-06-17", 42, true);
redisTemplate.opsForValue().setBit("logins:2024-06-17", 99, true);

boolean user1LoggedIn  = Boolean.TRUE.equals(redisTemplate.opsForValue().getBit("logins:2024-06-17", 1));
boolean user10LoggedIn = Boolean.TRUE.equals(redisTemplate.opsForValue().getBit("logins:2024-06-17", 10));

Long totalLogins = redisTemplate.execute((RedisConnection conn) ->
        conn.stringCommands().bitCount("logins:2024-06-17".getBytes())
);
```

**Sample Response (`GET /api/redis/bitmap`):**
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

---

### 3.8 HyperLogLog
> Probabilistic data structure to count **unique items** with ~12 KB memory regardless of dataset size. ~0.81% error rate.

**Use Case in App:** Count unique visitors across pages without storing all user IDs

**CLI:**
```bash
PFADD hll:visitors:home    "user1" "user2" "user3" "user4" "user2"  # dupe ignored
PFADD hll:visitors:products "user2" "user3" "user5" "user6" "user7"
PFCOUNT hll:visitors:home           # ~4
PFCOUNT hll:visitors:products       # ~5
PFMERGE hll:visitors:all hll:visitors:home hll:visitors:products
PFCOUNT hll:visitors:all            # ~7 (unique across all pages)
```

**Java (Service — `RedisDataStructureService.java`):**
```java
HyperLogLogOperations<String, String> ops = redisTemplate.opsForHyperLogLog();

ops.add("hll:visitors:home",     "user1", "user2", "user3", "user4", "user2"); // dupe ignored
ops.add("hll:visitors:products", "user2", "user3", "user5", "user6", "user7");

long homeUnique    = ops.size("hll:visitors:home");
long productUnique = ops.size("hll:visitors:products");

ops.union("hll:visitors:all", "hll:visitors:home", "hll:visitors:products");
long totalUnique = ops.size("hll:visitors:all");
```

**Sample Response (`GET /api/redis/hyperloglog`):**
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

---

## 4. Other Real-World Use Cases

### 4.1 Session Management
```
User logs in → session stored in Redis with TTL
User makes request → session fetched from Redis (not DB)
TTL expires → session auto-deleted (user logged out)
```

### 4.2 Rate Limiting
```
Each API call → INCR counter in Redis
If counter > 100 in 1 min → block request
Counter auto-resets via TTL
```

### 4.3 Leaderboard (Gaming/Apps)
```java
// Add score
redisTemplate.opsForZSet().add("leaderboard", "playerA", 5000);

// Get top 10
redisTemplate.opsForZSet().reverseRange("leaderboard", 0, 9);
```

### 4.4 Pub/Sub Messaging
```
Publisher → pushes message to Redis channel
Subscriber → receives message in real time
Use case: notifications, live chat, event broadcasting
```

### 4.5 Distributed Locking
```
Microservice A → acquires lock in Redis
Microservice B → waits (lock exists)
Microservice A done → releases lock
Use case: prevent duplicate payment processing
```

### 4.6 Job Queues
```
Producer → pushes job to Redis List (LPUSH)
Consumer → pops job and processes (BRPOP)
Use case: email sending, report generation
```

### 4.7 Geospatial Queries
```java
// Store location
redisTemplate.opsForGeo().add("locations", point, "store1");

// Find stores within 5km
redisTemplate.opsForGeo().radius("locations", "store1", 5, KILOMETERS);
```

---

## 5. Current Application — Redis + MySQL Demo

### Project Structure
```
src/main/java/com/example/redismysqldemo/
│
├── config/
│   └── RedisConfig.java               ← RedisTemplate + CacheManager + StringRedisTemplate beans
│
├── controller/
│   ├── ProductController.java         ← CRUD endpoints (/api/products)
│   └── RedisDataStructureController.java  ← Data structure demo endpoints (/api/redis/*)
│
├── service/
│   ├── ProductService.java            ← Both annotation + manual Redis implementations
│   └── RedisDataStructureService.java ← All 8 data structure demos with use cases
│
├── model/
│   └── Product.java
│
├── repository/
│   └── ProductRepository.java
│
└── RedisMysqlDemoApplication.java     ← @EnableCaching enabled
```

---

### Architecture — Product Cache (Cache-Aside Pattern)
```
Client Request
      │
      ▼
ProductController  (/api/products)
      │
      ▼
ProductService
      │
      ├──► Redis (check cache first)
      │         │
      │    HIT ◄┘  MISS
      │              │
      └──────────────►  MySQL (fetch from DB)
                             │
                         Store in Redis
                             │
                         Return to Client
```

---

### Product Cache Scenarios

| Operation | Cache Behavior | Log Output |
|---|---|---|
| `POST /api/products` | Saves to DB + writes to Redis (`@CachePut`) | `CACHE WRITE - saving new product` |
| `GET /api/products/{id}` (1st call) | Cache MISS → hits DB → stores in Redis | `CACHE MISS - DB called for id: X` |
| `GET /api/products/{id}` (2nd call) | Cache HIT → **DB never called** | *(no log — method body skipped)* |
| `GET /api/products` | Always hits DB — no cache | `NO CACHE - getAll always fetches from DB` |
| `PUT /api/products/{id}` | Updates DB + refreshes Redis (`@CachePut`) | `CACHE UPDATE - updating DB and refreshing Redis` |
| `DELETE /api/products/{id}` | Deletes from DB + evicts from Redis (`@CacheEvict`) | `CACHE EVICT - removing from Redis and DB` |
| TTL Expiry (60 min) | Redis auto-deletes — next GET is a MISS again | `CACHE MISS - DB called for id: X` |

---

### Redis Key Format
```
products::1      ← annotation-based  (@Cacheable / @CachePut / @CacheEvict)
product::1       ← manual RedisTemplate
```

---

### Configuration (`application.properties`)
```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379

# TTL: data auto-deleted from Redis after 60 minutes
app.cache.ttl-minutes=60
```

---

### Two Cache Implementations in `ProductService`

#### A) Annotation-Based
```java
@Cacheable(value = "products", key = "#id")   // check cache first → skip method on HIT
@CachePut(value = "products", key = "#id")    // always write result to cache
@CacheEvict(value = "products", key = "#id")  // remove key from cache
```

#### B) Manual RedisTemplate
```java
ValueOperations<String, String> ops = redisTemplate.opsForValue();
Product cached = ops.get(key);                     // read from cache
if (cached != null) return cached;                 // HIT — skip DB
ops.set(key, dbProduct, cacheTtl);                 // write with TTL
redisTemplate.delete(key);                         // evict on delete
```

---

### RedisConfig — Three Beans Registered

```java
// 1. For Product caching (manual approach)
@Bean RedisTemplate<String, Product> redisTemplate(...)

// 2. For annotation-based caching (@Cacheable etc.)
@Bean CacheManager cacheManager(...)

// 3. For data structure demos (String, Hash, List, Set etc.)
@Bean StringRedisTemplate stringRedisTemplate(...)
```

---

### Data Structure Demo Endpoints

| Endpoint | Structure | Use Case Demonstrated |
|---|---|---|
| `GET /api/redis/string` | **String** | Session token with TTL + atomic page view counter |
| `GET /api/redis/hash` | **Hash** | User profile — update single field without reloading |
| `GET /api/redis/list` | **List** | Last 5 user activities with auto-trim |
| `GET /api/redis/set` | **Set** | Unique visitors + new vs returning user analysis |
| `GET /api/redis/sorted-set` | **Sorted Set** | Game leaderboard with live score updates |
| `GET /api/redis/stream` | **Stream** | Order lifecycle events as append-only log |
| `GET /api/redis/bitmap` | **Bitmap** | Daily login tracking — 1M users in 125 KB |
| `GET /api/redis/hyperloglog` | **HyperLogLog** | Unique page visitors — fixed 12 KB memory |

---

## 6. Redis Persistence Options

| Mode | Description | Use When |
|---|---|---|
| **No persistence** | Pure in-memory, lost on restart | Pure cache only |
| **RDB** (Snapshot) | Periodic snapshot to disk | Tolerate some data loss |
| **AOF** (Append Only File) | Logs every write operation | Need full durability |
| **RDB + AOF** | Both combined | Maximum safety |

---

## 7. Redis vs Other Technologies

| Feature | Redis | Memcached | Database (MySQL) |
|---|---|---|---|
| Speed | ⚡ Sub-ms | ⚡ Sub-ms | 🐢 Slower |
| Data structures | Rich (10+) | String only | Tables/rows |
| Persistence | Yes (optional) | No | Yes |
| TTL support | Yes | Yes | Manual |
| Pub/Sub | Yes | No | No |
| Clustering | Yes | Yes | Yes |

---

## 8. Interview Questions & Answers

### Basic Level

**Q1. What is Redis and what makes it fast?**
> Redis is an in-memory key-value store. It's fast because it stores data in **RAM** instead of disk, uses **single-threaded event loop** (no lock overhead), and has **efficient data structures** optimized for speed.

---

**Q2. What data types does Redis support?**
> String, Hash, List, Set, Sorted Set, Stream, Bitmap, HyperLogLog, and Geospatial indexes.

---

**Q3. What is TTL in Redis?**
> TTL (Time To Live) is the expiry duration for a key. After TTL expires, Redis **automatically deletes** the key. Set using:
> ```
> SET key value EX 3600   ← expires in 3600 seconds (1 hour)
> ```

---

**Q4. What is the difference between `@Cacheable` and `@CachePut`?**
> - `@Cacheable` — checks cache first; **skips method** if cache hit
> - `@CachePut` — **always executes method** and updates the cache with the result

---

**Q5. What is `@CacheEvict`?**
> Removes one or all entries from the cache. Used on delete operations to keep Redis and DB in sync.
> ```java
> @CacheEvict(value = "products", allEntries = true)  // clear entire cache
> @CacheEvict(value = "products", key = "#id")         // remove specific key
> ```

---

### Intermediate Level

**Q6. What is cache stampede and how to prevent it?**
> When TTL expires and many requests hit the DB simultaneously. Prevention strategies:
> - **Mutex lock** — only one request fetches, others wait
> - **Probabilistic early expiry** — randomly refresh before TTL ends
> - **Longer TTL with background refresh**

---

**Q7. What is the difference between Redis RDB and AOF persistence?**
> - **RDB** — periodic snapshot, compact file, slight data loss risk on crash
> - **AOF** — logs every write, more durable, larger file, slower
> - **Best practice** — use both together

---

**Q8. How does Redis handle concurrency if it's single-threaded?**
> Redis uses a **single-threaded event loop** for commands, so no race conditions on data. I/O is handled via **multiplexing**. Since Redis 6.0, I/O threads are multi-threaded but command execution remains single-threaded.

---

**Q9. What is Redis Pub/Sub?**
> A messaging pattern where:
> - **Publisher** sends message to a **channel**
> - **Subscribers** receive it in real time
> - Messages are **not persisted** — if no subscriber is listening, message is lost
> - Use **Redis Streams** if persistence is needed

---

**Q10. What is the difference between Redis Cluster and Redis Sentinel?**
> - **Sentinel** — monitors master/slave, handles **automatic failover** (HA)
> - **Cluster** — distributes data across multiple nodes for **horizontal scaling + HA**

---

### Advanced Level

**Q11. What is a Redis distributed lock (Redlock)?**
> An algorithm to implement distributed locks across multiple Redis nodes:
> 1. Try to acquire lock on N Redis instances
> 2. If majority (N/2 + 1) acquired within timeout → lock held
> 3. Release all locks after operation
> Used to prevent duplicate processing in distributed systems.

---

**Q12. How would you cache a list of products (getAll)?**
> Caching lists is tricky — any item change invalidates the whole list.
> Options:
> - Cache with a short TTL and accept slight staleness
> - Use `@CacheEvict(allEntries = true)` on any create/update/delete
> - Store as a Redis Hash (field per product ID)

---

**Q13. What happens if Redis runs out of memory?**
> Redis uses **eviction policies** (set via `maxmemory-policy`):
> | Policy | Behaviour |
> |---|---|
> | `noeviction` | Return error on write (default) |
> | `allkeys-lru` | Evict least recently used keys |
> | `volatile-lru` | Evict LRU keys with TTL set |
> | `allkeys-random` | Evict random keys |

---

**Q14. What is cache invalidation and why is it hard?**
> Cache invalidation = keeping cache in sync with DB when data changes.
> It's hard because:
> - **When to invalidate?** — on every write, or TTL-based?
> - **What to invalidate?** — single key or related keys?
> - **Distributed systems** — multiple app instances may have stale cache
>
> Common strategies: **Write-through**, **Write-behind**, **Cache-aside** (used in this app)

---

**Q15. What is the Cache-Aside pattern?**
> The pattern used in this application:
> ```
> 1. Check Redis → HIT? Return data
> 2. MISS? → Fetch from DB
> 3. Store result in Redis
> 4. Return data
> ```
> App controls what goes into cache. Most common pattern for read-heavy workloads.

---

## 9. Quick Redis CLI Commands

```bash
# Connect
redis-cli

# Set / Get
SET product:1 "iPhone"
GET product:1

# Set with TTL (seconds)
SET product:1 "iPhone" EX 3600

# Check TTL remaining
TTL product:1

# Delete a key
DEL product:1

# Check if key exists
EXISTS product:1

# Get all keys (careful in production!)
KEYS *

# Flush all data
FLUSHALL

# Monitor real-time commands
MONITOR
```

---

## 10. Best Practices

- ✅ Always set a **TTL** — never cache without expiry
- ✅ Use **meaningful key names** — `products::1` not just `1`
- ✅ Cache only **read-heavy, rarely changing** data
- ✅ Evict cache on **every write** (CachePut / CacheEvict)
- ✅ Use **connection pooling** (Lettuce — default in Spring Boot)
- ❌ Don't cache **user-specific sensitive data** without encryption
- ❌ Don't use `KEYS *` in production — use `SCAN` instead
- ❌ Don't store **very large objects** — keep values small

---

*Guide covers: Redis fundamentals, all 8 data structures with CLI + Java examples, this Spring Boot application (both cache implementations + data structure demos), and interview preparation.*
