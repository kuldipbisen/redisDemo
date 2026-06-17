# Redis Setup and Run Guide (Windows)

This guide explains everything needed to run Redis for this Spring Boot project.

## 1) Applications Required

Install these applications first:

1. **Java 17+** (required by Spring Boot app)
2. **Maven 3.9+** (to run the project)
3. **MySQL Server + MySQL Workbench** (database)
4. **Redis** (cache)

You can run Redis in **two ways**:
- Option A (recommended): Docker Desktop
- Option B: Native Redis-compatible server on Windows (Memurai)

---

## 2) Option A - Run Redis Using Docker Desktop (Recommended)

### Step A1 - Install Docker Desktop

```bat
winget install -e --id Docker.DockerDesktop
```

After installation:
- Open Docker Desktop
- Wait until it shows engine is running
- Restart terminal

### Step A2 - Verify Docker

```bat
docker --version
docker compose version
```

### Step A3 - Start Redis from project folder

```bat
cd "C:\Users\KuldipkumarRadhelalB\Documents\Redis\Redis examples"
docker compose up -d redis
```

### Step A4 - Verify Redis is running

```bat
docker ps
docker exec -it redis_demo_redis redis-cli ping
```

Expected output:

```text
PONG
```

### Step A5 - Stop Redis (when needed)

```bat
cd "C:\Users\KuldipkumarRadhelalB\Documents\Redis\Redis examples"
docker compose stop redis
```

---

## 3) Option B - Run Redis Without Docker (Memurai)

Memurai is a Redis-compatible server for Windows.

### Step B1 - Install Memurai
- Download Community Edition from the Memurai website.
- Install with default settings.

### Step B2 - Start service

```bat
net start Memurai
```

### Step B3 - Verify Redis endpoint
If `redis-cli` is installed:

```bat
redis-cli -h 127.0.0.1 -p 6379 ping
```

Expected output:

```text
PONG
```

---

## 4) MySQL Setup (Workbench)

Create connection in MySQL Workbench:
- Host: `127.0.0.1`
- Port: `3306`
- Username: `root`
- Password: `root123`

The Spring Boot app uses DB name `redis_demo` and auto-creates it if missing.

---

## 5) Spring Boot Configuration Check

File: `src/main/resources/application.properties`

Required Redis properties:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Required MySQL properties (already in project):

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/redis_demo?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=root123
```

---

## 6) Full Run Process

### Step 1 - Start MySQL
Start your local MySQL service (or MySQL via Docker if you use Docker for DB too).

### Step 2 - Start Redis
Use Option A or Option B above.

### Step 3 - Run Spring Boot app

```bat
cd "C:\Users\KuldipkumarRadhelalB\Documents\Redis\Redis examples"
mvn spring-boot:run
```

### Step 4 - Verify API is reachable

```bat
curl http://localhost:8080/api/products
```

---

## 7) Troubleshooting

### Error: `'docker' is not recognized`
- Docker Desktop is not installed, not started, or terminal is not restarted.
- Fix: install Docker Desktop, start it, reopen terminal.

### Error: `Unable to connect to Redis` / `Connection refused`
- Redis server is not running on `localhost:6379`.
- Fix: start Redis and verify with `PING`.

### Error: MySQL connection failure
- Verify MySQL service is running.
- Verify username/password and port in `application.properties`.

---

## 8) Quick Command Checklist

```bat
cd "C:\Users\KuldipkumarRadhelalB\Documents\Redis\Redis examples"
docker compose up -d redis
mvn spring-boot:run
```

If Redis is running correctly, your create/update/delete APIs should work with cache support.


## 8) How Data is Created in Redis

### Flow: `POST /api/products` → saves to MySQL + caches in Redis

**Step 1 – API Call (Controller)**
```json
POST /api/products
Content-Type: application/json

{
  "name": "Laptop",
  "description": "Gaming Laptop",
  "price": 999.99,
  "quantity": 10
}
```

**Step 2 – Service saves to MySQL, then puts in Redis cache**
```java
public Product create(Product product) {
    Product saved = productRepository.save(product);  // Save to MySQL
    putInCache(saved);                                 // Store in Redis ✅
    return saved;
}

private void putInCache(Product product) {
    redisTemplate.opsForValue().set("product::" + id, product, cacheTtl);
    // Key format: "product::1", "product::2", etc.
    // TTL: 10 minutes (configurable via app.cache.ttl-minutes)
}
```

### Test it with curl
```bash
# Create a product (stores in MySQL + Redis)
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","description":"Gaming Laptop","price":999.99,"quantity":10}'

# Fetch it — first hit loads from MySQL and caches in Redis
curl http://localhost:8080/api/products/1

# Second fetch — served directly from Redis ⚡
curl http://localhost:8080/api/products/1
```

### Redis Key Pattern
| Operation | Redis Key       | TTL        |
|-----------|-----------------|------------|
| Create    | `product::{id}` | 10 minutes |
| Update    | `product::{id}` | 10 minutes |
| Delete    | `product::{id}` | removed    |
