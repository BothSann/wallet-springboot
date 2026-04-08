# Digital Wallet API

> A RESTful Digital Wallet backend built with Spring Boot 4 and Java 25 — supporting multi-currency wallets, secure transfers, and real-time exchange rates.

![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.5-6DB33F?logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-JJWT_0.13.0-000000?logo=jsonwebtokens&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?logo=flyway&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-enabled-red)

---

## About the Project

This is a personal learning project built to explore production-grade patterns in Spring Boot — including stateless JWT authentication, idempotent financial operations, multi-currency support, and async event-driven notifications.

The API covers the full lifecycle of a digital wallet: user registration and authentication, wallet creation and balance management, peer-to-peer transfers, currency exchange, transaction history, and an admin panel with audit logging.

> **Note:** This project was built for learning purposes and as a portfolio piece. It is not deployed in production.

---

## Features

### Authentication

- User registration with automatic default wallet creation
- Login with JWT access token (15 min) + refresh token (7 days)
- Token refresh and logout with server-side token revocation

### Wallet Management

- Create multiple wallets per user, each with its own currency (USD or KHR)
- Deposit, withdraw, and transfer funds between wallets
- Currency exchange between your own wallets using live exchange rates
- Per-wallet daily spending limits (configurable by user, capped by system)

### Security Extras

- Optional 4-digit PIN required for withdrawals, transfers, and exchanges
- All mutating operations protected by idempotency keys to prevent duplicate processing

### Transactions

- Paginated transaction history across all user wallets
- Filter by transaction type (DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT)
- Per-transaction detail with currency, exchange rate, and balance snapshots

### Multi-Currency

- Supports USD and KHR (Cambodian Riel)
- Exchange rates fetched from [exchangerate-api.com](https://www.exchangerate-api.com) and cached hourly
- Daily limit enforcement normalized to USD equivalent across all currencies

### Admin Panel

- List, view, and deactivate user accounts
- Browse all wallets in the system
- Query a full audit log with filters by entity, actor, and action

### Email Notifications

- Async email sent on successful deposit and on transfer received
- Notifications fire only after the database transaction commits — financial integrity is never affected by email failures

### Audit Trail

- All significant entity changes recorded in `audit_log` with actor ID, action, and before/after JSON snapshots

---

## Tech Stack

| Layer             | Technology                                     |
| ----------------- | ---------------------------------------------- |
| Framework         | Spring Boot 4.0.5                              |
| Language          | Java 25                                        |
| Database          | PostgreSQL 16 (Docker)                         |
| ORM               | Spring Data JPA / Hibernate                    |
| Authentication    | Spring Security + JJWT 0.13.0                  |
| Schema Migrations | Flyway                                         |
| Email             | Spring Mail (Mailtrap for dev, Gmail for prod) |
| Build             | Maven                                          |
| Containerization  | Docker Compose                                 |
| Utilities         | Lombok                                         |

---

## Architecture Overview

The project follows a **package-by-feature** structure. Each domain is self-contained with its own controller, service, repository, entity, and DTOs.

```
com.bothsann.wallet/
├── auth/           → Registration, login, JWT, refresh tokens, Spring Security config
├── wallet/         → Wallet entity, deposit, withdraw, transfer, exchange, PIN, daily limits
├── transaction/    → Transaction entity, paginated history, per-transaction detail
├── admin/          → Admin-only user/wallet management and audit log queries
└── shared/         → Cross-cutting: exceptions, config, enums, events, email, audit, currency
```

The `shared` package handles everything that cuts across domains:

- **`shared/exception`** — 18 custom exceptions with a single `GlobalExceptionHandler`
- **`shared/config`** — JWT, wallet, and currency configuration properties
- **`shared/currency`** — Exchange rate fetching, caching, and conversion
- **`shared/email`** — Async email service driven by Spring application events
- **`shared/audit`** — Audit log entity, service, and admin query support
- **`shared/event`** — Domain events (`DepositSuccessEvent`, `TransferReceivedEvent`)

---

## Key Design Decisions

### Idempotency Keys

Every wallet mutation endpoint (`/deposit`, `/withdraw`, `/transfer`, `/exchange`) requires an `Idempotency-Key` header. The key is stored in the `transactions` table with a unique constraint. If a client retries a request with the same key, the server returns the original response instead of processing it twice. This makes all financial operations safe to retry.

### Optimistic Locking

The `Wallet` entity has a `@Version` field that Hibernate uses for optimistic locking. If two concurrent requests try to update the same wallet balance simultaneously, one will succeed and the other will receive a `409 Conflict` — no distributed lock needed.

### Stateless JWT with Stored Refresh Tokens

Access tokens are short-lived (15 min) and fully stateless — the server validates them by signature alone. Refresh tokens are stored in the database so they can be individually revoked on logout. This balances statelessness with the ability to invalidate sessions.

### Async Email via Transactional Events

Email notifications use Spring's `ApplicationEventPublisher` with `@TransactionalEventListener(phase = AFTER_COMMIT)` and `@Async`. This guarantees two things: (1) the email only fires after the database transaction has fully committed, and (2) email delivery happens on a separate thread pool so it never delays the HTTP response. An email failure is logged but never rolls back the financial operation.

### PIN at User Level, Not Wallet Level

The transaction PIN is stored on the `User` entity (as a BCrypt hash), not on individual wallets. This means one PIN protects all of a user's wallets — consistent UX regardless of how many wallets you have. (The PIN was originally on `Wallet` and migrated to `User` in V8.)

### Daily Limits Normalized to USD

Each wallet has its own daily spending limit, but comparison always happens in USD. If you spend KHR from a KHR wallet, the amount is converted to USD using the cached exchange rate before checking against the limit. This keeps the limit meaningful across currencies.

### Immutable Transaction Records

Transactions are write-once. Once created, they are never updated. Each record stores `balance_before` and `balance_after`, making the transaction history a self-contained audit trail. If something goes wrong, you can reconstruct the wallet's full balance history from transactions alone.

### Flyway Owns the Schema

`spring.jpa.hibernate.ddl-auto` is set to `validate`. Hibernate checks that the schema matches the entities on startup but never creates or alters tables. All DDL is handled exclusively by Flyway migration files.

---

## Getting Started

### Prerequisites

- **Java 25** (JDK)
- **Docker** and **Docker Compose**
- **IntelliJ IDEA** (recommended) or any IDE

### 1. Clone the Repository

```bash
git clone <repo-url>
cd wallet
```

### 2. Start the Database

```bash
docker compose up -d
```

This starts a PostgreSQL 16 container at `localhost:5432` with database `wallet_db`. Flyway will run all migrations automatically on first startup.

### 3. Create Your Local Config

Create `src/main/resources/application-local.yml` (this file is gitignored):

```yaml
spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  mail:
    host: sandbox.smtp.mailtrap.io # Mailtrap SMTP for dev
    port: 2525
    username: <your-mailtrap-username>
    password: <your-mailtrap-password>

application:
  jwt:
    secret: <base64-encoded-secret> # Min 64 bytes, base64-encoded
  currency:
    api-key: <your-exchange-rate-api-key>
```

See `docs/credentials-guide.md` for detailed instructions on generating a JWT secret, setting up Mailtrap, and getting an exchange rate API key.

### 4. Run the Application

In IntelliJ, set the active Spring profile to `local` in your Run/Debug Configuration, then press the play button. Alternatively:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 5. Access the API

```
http://localhost:8080/api/v1
```

---

## Environment Variables

These values belong in `application-local.yml` for local development, or as environment variables in a deployment environment.

| Key                      | Description                                                           |
| ------------------------ | --------------------------------------------------------------------- |
| `JWT_SECRET`             | Base64-encoded HMAC-SHA256 secret (minimum 64 bytes)                  |
| `MAIL_PASSWORD`          | SMTP password for your mail provider                                  |
| `EXCHANGE_RATE_API_KEY`  | API key from [exchangerate-api.com](https://www.exchangerate-api.com) |
| `WALLET_MAX_DAILY_LIMIT` | System-wide maximum daily limit users can set (default: `50000`)      |

<!-- See `docs/credentials-guide.md` for the full setup guide. -->

---

## API Overview

All endpoints are prefixed with `/api/v1`. Authenticated endpoints require an `Authorization: Bearer <token>` header.

| Domain       | Base Path       | Description                                                              |
| ------------ | --------------- | ------------------------------------------------------------------------ |
| Auth         | `/auth`         | Register, login, token refresh, logout                                   |
| Wallet       | `/wallet`       | Create wallets, deposit, withdraw, transfer, exchange, PIN, daily limits |
| Transactions | `/transactions` | Paginated history and per-transaction detail                             |
| Admin        | `/admin`        | User management, wallet overview, audit logs (ADMIN role required)       |

**Important:** All mutating wallet endpoints (`/deposit`, `/withdraw`, `/transfer`, `/exchange`) require an `Idempotency-Key` request header with a unique value (e.g., a UUID) per operation.

For the complete endpoint reference including request/response schemas, see the **[Postman Collection](#)** _(link coming soon)_ or refer to the controller source files under `src/main/java/com/bothsann/wallet/`.

---

## Database Migrations

All schema changes are managed by Flyway. Migration files live in `src/main/resources/db/migration/`. **Never modify an existing migration file** — always add a new `V{n+1}__description.sql`.

| Migration                             | Description                                                                             |
| ------------------------------------- | --------------------------------------------------------------------------------------- |
| `V1__create_users_table.sql`          | Users table with email index                                                            |
| `V2__create_wallets_table.sql`        | Wallets table with FK to users                                                          |
| `V3__create_transactions_table.sql`   | Transactions table with idempotency key uniqueness                                      |
| `V4__create_refresh_tokens_table.sql` | Refresh token storage for JWT revocation                                                |
| `V5__add_pin_hash_to_wallets.sql`     | Adds PIN hash column to wallets (initial PIN support)                                   |
| `V6__add_daily_limit_to_wallets.sql`  | Adds per-wallet daily spending limit                                                    |
| `V7__multi_currency_support.sql`      | Exchange rates table and exchange_rate column on transactions                           |
| `V8__dual_currency_wallets.sql`       | Moves PIN from wallet to user; adds `is_default` flag; allows multiple wallets per user |
| `V9__create_audit_log_table.sql`      | Audit log table with JSONB old/new value storage                                        |

---

## Project Structure

```
wallet/
├── src/
│   ├── main/
│   │   ├── java/com/bothsann/wallet/
│   │   │   ├── WalletApplication.java
│   │   │   ├── auth/              → JWT auth, Spring Security, refresh tokens
│   │   │   ├── wallet/            → Wallet operations, PIN, daily limits
│   │   │   ├── transaction/       → Transaction history and detail
│   │   │   ├── admin/             → Admin endpoints and audit log queries
│   │   │   └── shared/            → Config, exceptions, events, email, currency
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml   ← gitignored, created by you
│   │       └── db/migration/           ← Flyway SQL files
├── docs/                               ← Setup and development guides
├── docker-compose.yml
└── pom.xml
```

---

## Running Tests

```bash
./mvnw test
```

> Tests are a planned addition to this project.

---

## Contributing

This is a personal project, but feedback and suggestions are welcome. Feel free to open an issue or reach out.

---

## License

[MIT](LICENSE)
