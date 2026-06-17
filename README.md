# Spring Boot Redis + MySQL CRUD Example

This project demonstrates:
- Spring Boot REST API (Maven)
- MySQL persistence with Spring Data JPA
- Redis cache for fast read and cache invalidation on update/delete

## 1) Prerequisites
- Java 17+
- Maven 3.9+
- MySQL Server (or MySQL via Docker)
- Redis Server (or Redis via Docker)

## 2) Quick start with Docker (optional)
From project root:

```yaml
# docker-compose.yml is already included in this project.
```

Run:

```bat
docker compose up -d
```

This starts:
- MySQL on `localhost:3306`
- Redis on `localhost:6379`

## 3) Configure application
Edit `src/main/resources/application.properties` if your credentials differ.

Default values used:
- MySQL DB: `redis_demo`
- MySQL user: `root`
- MySQL password: `root123`

## 4) Run the app
```bat
mvn spring-boot:run
```

App runs on `http://localhost:8080`.

## 5) API endpoints
Base URL: `http://localhost:8080/api/products`

### Create
```http
POST /api/products
Content-Type: application/json

{
  "name": "Keyboard",
  "description": "Mechanical keyboard",
  "price": 2999.00,
  "quantity": 10
}
```

### Get by ID (reads Redis first, then MySQL)
```http
GET /api/products/{id}
```

### Get all
```http
GET /api/products
```

### Update
```http
PUT /api/products/{id}
Content-Type: application/json

{
  "name": "Keyboard Pro",
  "description": "RGB mechanical keyboard",
  "price": 3499.00,
  "quantity": 8
}
```

### Delete
```http
DELETE /api/products/{id}
```

## 6) MySQL Workbench setup
In MySQL Workbench create a connection with:
- Hostname: `127.0.0.1`
- Port: `3306`
- Username: `root`
- Password: `root123`

After app starts, open SQL editor and run:

```sql
USE redis_demo;
SELECT * FROM products;
```

## 7) Run tests
```bat
mvn test
```

