# StreamCart Order Service

> Event-driven microservice for processing orders in an e-commerce platform

## Overview

The Order Service is part of the StreamCart e-commerce ecosystem, a distributed system of microservices communicating via Apache Kafka. This service handles order creation, order retrieval, and publishes order events for downstream processing by payment and inventory services.

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    StreamCart Ecosystem                      │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────┐  │
│  │   Order      │      │   Payment    │      │ Inventory│  │
│  │   Service    │──────│   Service    │──────│ Service  │  │
│  │  (port 8081) │      │  (port 8082) │      │(port 8083│  │
│  └──────┬───────┘      └──────┬───────┘      └────┬─────┘  │
│         │                     │                    │         │
│         │    ┌──────────────────────────┐         │         │
│         └────│   Apache Kafka Broker    │─────────┘         │
│              │   (Event Bus)            │                   │
│              └──────────────────────────┘                   │
│                                                              │
│  Topics: order.created, payment.processed, inventory.updated│
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

- **Java 21** - Latest LTS version with modern language features
- **Spring Boot 3.5.6** - Framework for microservices
- **Spring Security + JWT** - Stateless authentication
- **Spring Data JPA + Hibernate** - Database ORM
- **PostgreSQL** - Relational database (database-per-service pattern)
- **Apache Kafka** - Event streaming platform
- **SpringDoc OpenAPI** - API documentation (Swagger)
- **Lombok** - Reducing boilerplate code
- **BCrypt** - Password encryption
- **Docker Compose** - Local development infrastructure

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### 1. Start Infrastructure

```bash
cd local-dev
docker-compose up -d
```

This starts:
- PostgreSQL (port 5433)
- Apache Kafka (port 9092)
- Zookeeper (port 2181)

### 2. Start Application

```bash
./mvnw spring-boot:run
```

The application will:
- Auto-create database schema
- Seed 23 products (The Office themed)
- Seed 4 demo users (dev mode only)
- Start on port 8081

### 3. Access API Documentation

Open Swagger UI: **http://localhost:8081/swagger-ui.html**

Interactive documentation with "Try it out" functionality for all endpoints.

### 4. Test with Demo User

```bash
# Login with Michael Scott
curl -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"mscott","password":"worldsbestboss"}'
```

**Demo Credentials:**

| Username   | Password        | Name           |
|-----------|----------------|----------------|
| `mscott`  | `worldsbestboss` | Michael Scott  |
| `dschrute`| `beetsfacts`     | Dwight Schrute |
| `jhalpert`| `tuna4life`      | Jim Halpert    |
| `pbeesly` | `fineart2023`    | Pam Beesly     |

## API Endpoints

### Public Endpoints (No Authentication)

| Method | Endpoint            | Description                    |
|--------|---------------------|--------------------------------|
| POST   | `/api/auth/register`| Register new user account      |
| POST   | `/api/auth/login`   | Login and receive JWT token    |

### Protected Endpoints (JWT Required)

| Method | Endpoint              | Description                       |
|--------|-----------------------|-----------------------------------|
| POST   | `/api/orders`         | Create a new order                |
| GET    | `/api/orders/my-orders`| Get all orders for current user  |
| GET    | `/api/orders/{orderId}`| Get specific order by ID         |

**Authentication:** Include JWT token in header:
```
Authorization: Bearer <your_jwt_token>
```

**Try it in Swagger:** Click the "Authorize" button in Swagger UI and paste your JWT token.

## Database Schema

### Tables

**users**
- Primary user accounts with encrypted passwords
- One-to-many relationship with orders

**orders**
- Order records with status tracking
- Belongs to a user

**order_items**
- Individual line items in an order
- Belongs to an order

**products**
- Product catalog (23 seeded products)
- Referenced by order items

### Entity Relationships

```
users (1) ──< (many) orders
orders (1) ──< (many) order_items
```

## Event-Driven Architecture

### Published Events

**Topic:** `order.created`

Published when a new order is created. Consumed by payment-service and inventory-service.

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "mscott",
  "totalAmount": 699.00,
  "items": [
    {
      "productId": "DM-PAPER-001",
      "productName": "Dunder Mifflin Paper - Premium White",
      "quantity": 100,
      "price": 6.99
    }
  ],
  "timestamp": "2025-10-20T14:30:00"
}
```

### Event Flow

```
1. User creates order via POST /api/orders
2. Order Service saves order to database
3. Order Service publishes order.created event to Kafka
4. Payment Service consumes event → processes payment
5. Inventory Service consumes event → updates stock
```

## Authentication & Security

### JWT Authentication

- Stateless authentication (no server-side sessions)
- Token expiration: 15 minutes
- Token contains: username, issued time, expiration time
- Signed with HS256 algorithm

### Security Features

- **BCrypt Password Hashing** - One-way encryption with automatic salting
- **JWT Signature Verification** - Prevents token tampering
- **CSRF Disabled** - Not needed for stateless REST APIs
- **HTTPS Ready** - Always use HTTPS in production

### How It Works

```
1. User registers/login → Receives JWT token
2. Client includes token in Authorization header
3. JwtAuthenticationFilter validates token on each request
4. SecurityContextHolder stores authenticated user
5. Controller can access current user via @AuthenticationPrincipal
```

## Development

### Project Structure

```
src/main/java/com/streamcart/order/
├── config/              # Spring configuration (Security, OpenAPI)
├── controller/          # REST endpoints
├── service/             # Business logic
├── repository/          # JPA repositories
├── entity/              # Database entities
├── dto/                 # Request/Response objects
├── publisher/           # Kafka producers
├── consumer/            # Kafka consumers (future)
├── security/            # JWT utilities & filters
├── seeder/              # Data seeders (dev mode)
└── exception/           # Custom exceptions & handlers
```

### Code Conventions

- **Entities:** Singular nouns (Order, Payment, Product)
- **DTOs:** Suffix with Request/Response/Event
- **Services:** Suffix with Service, use @Transactional
- **Controllers:** Suffix with Controller, use @RestController
- **Repositories:** Suffix with Repository, extend JpaRepository
- **Kafka Topics:** Lowercase dot notation (order.created)

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=OrderServiceTest

# Run with coverage
./mvnw test jacoco:report
```

## Configuration

### Application Profiles

**Development (default):**
```yaml
spring.profiles.active: dev
```
- Seeds demo users and products
- SQL logging enabled
- Debug logging

**Production:**
```yaml
spring.profiles.active: prod
```
- No demo user seeding
- Minimal logging
- Optimized for performance

### Environment Variables

| Variable              | Description                | Default               |
|----------------------|----------------------------|-----------------------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection    | localhost:5433        |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker  | localhost:9092        |
| `JWT_SECRET`          | JWT signing key            | (set in application.yml) |
| `JWT_EXPIRATION`      | Token validity in ms       | 900000 (15 minutes)   |

## Local Development

### Reset Database

```bash
cd local-dev
docker-compose down -v  # Remove volumes
docker-compose up -d    # Restart with fresh database
```

### View Kafka Events

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic order.created \
  --from-beginning
```

### Connect to Database

```bash
psql -h localhost -p 5433 -U order_user -d order_db
# Password: order_pass
```

### Useful Queries

```sql
-- View all orders with users
SELECT o.order_id, u.username, o.total_amount, o.status, o.created_at
FROM orders o
JOIN users u ON o.user_id = u.id
ORDER BY o.created_at DESC;

-- View order items
SELECT oi.product_name, oi.quantity, oi.price, o.order_id
FROM order_items oi
JOIN orders o ON oi.order_id = o.id;

-- View products
SELECT product_id, name, price, stock_quantity FROM products;
```

## Deployment

### Building for Production

```bash
# Create executable JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/order-service-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod
```

### Docker Image (Future)

```bash
# Build image
docker build -t streamcart/order-service:latest .

# Run container
docker run -p 8081:8081 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/order_db \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  streamcart/order-service:latest
```

### Kubernetes Deployment (Planned)

- Helm charts for deployment
- ConfigMaps for configuration
- Secrets for sensitive data
- Horizontal Pod Autoscaling (HPA)
- Service mesh integration

### CI/CD Pipeline (Planned)

- GitHub Actions / Jenkins
- Automated testing on PR
- Docker image build & push
- Deployment to dev/staging/prod
- Integration tests in pipeline

## Monitoring & Observability (Future)

- **Metrics:** Prometheus + Grafana
- **Logging:** ELK Stack (Elasticsearch, Logstash, Kibana)
- **Tracing:** Zipkin / Jaeger for distributed tracing
- **Health Checks:** Spring Actuator endpoints

## Documentation

- **API Docs:** Swagger UI at `/swagger-ui.html`
- **OpenAPI Spec:** JSON/YAML at `/api-docs`
- **Data Seeding:** See `local-dev/DATA-SEEDING.md`

## Contributing

### Git Workflow

```bash
# Create feature branch
git checkout -b feature/add-order-cancellation

# Make changes with tests
git add .
git commit -m "feat: Add order cancellation endpoint"

# Push and create PR
git push origin feature/add-order-cancellation
```

### Commit Message Convention

- `feat:` - New feature
- `fix:` - Bug fix
- `test:` - Add or update tests
- `refactor:` - Code refactoring
- `docs:` - Documentation changes
- `chore:` - Maintenance tasks

## Troubleshooting

### Application won't start

**Check Docker containers are running:**
```bash
docker ps
```

**Check logs:**
```bash
docker-compose logs postgres
docker-compose logs kafka
```

### JWT token expired

Tokens expire after 15 minutes. Login again to get a fresh token.

### Can't connect to database

Ensure PostgreSQL is running and credentials match `application.yml`:
```bash
docker-compose ps postgres
```

### Kafka events not publishing

Check Kafka broker is healthy:
```bash
docker-compose logs kafka | grep -i error
```

## License

Proprietary - StreamCart Team

## Contact

- **Team:** StreamCart Development Team
- **Email:** support@streamcart.com
- **Repository:** [Link to repository]

---

**Built with ❤️ using Spring Boot and best practices in microservices architecture.**

