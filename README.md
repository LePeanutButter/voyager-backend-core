# SmarTrip - Backend Core

[![Standard Readme](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg?style=flat-square)](https://maven.apache.org/)
![License](https://img.shields.io/badge/license-GPL%203.0-blue.svg)

> The core backend API for the SmarTrip tourism platform: REST services for users, travel plans, social features, traveler matching and compatibility, and Google OAuth—persisted on PostgreSQL with Flyway migrations.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Postman (local API tests)](#postman-local-api-tests)
- [Security](#security)
- [Deployment](#deployment)
- [Contributors](#contributors)
- [License](#license)

## Background

This repository is the **Spring Boot** service for SmarTrip. It exposes REST APIs under `/api/v1` and persists data with **JPA** and **PostgreSQL**. In scope for this codebase:

- **User management**: Registration, login (JWT), profiles, and role-based admin operations where implemented
- **Travel planning**: CRUD for travel plans, plan activities, reservations, sharing tokens, and status updates
- **Social**: Connections between travelers, feed/posts, reviews, comments, likes, and messaging-related endpoints
- **Matching & compatibility**: Traveler matching (`GET /matches`) and compatibility scoring (`POST /compatibility/matches`) implemented in this service
- **Activity sharing**: Canonical and legacy HTTP endpoints to share activities between users
- **Google OAuth**: Browser-based sign-in flow endpoints under `/auth/google`
- **Integrations**: Configurable outbound URLs (for example the notification service base URL in `application.yml`)

The API follows REST best practices and implements the Richardson Maturity Model Level 3, ensuring consistent, scalable, and maintainable endpoints.

## Install

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- PostgreSQL 14 or higher (for local or external RDS)
- Docker (optional: local image build, Docker Compose stack, or EC2 deployment)

### Local Development Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/LePeanutButter/voyager-backend-core.git
   cd voyager-backend-core
   ```

2. **Set up the database**

   ```bash
   # Example: create PostgreSQL database and user (adjust names to match your env)
   createdb -U postgres tourism_platform
   ```

   Configure the datasource via environment variables or a local `.env` file (see [Environment Variables](#environment-variables)). Defaults in `src/main/resources/application.yml` point at `localhost` for development.

3. **Build the application**

   ```bash
   mvn clean install
   ```

4. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080/api/v1`

### Docker (local image)

1. **Build the Docker image**

   ```bash
   docker build -t voyager-backend:latest .
   ```

2. **Run the container** (database must be reachable from the container; use host networking or point `DB_URL` to your PostgreSQL host)

   ```bash
   docker run --rm -p 8080:8080 -p 8081:8081 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e DB_URL=jdbc:postgresql://host.docker.internal:5432/tourism_platform \
     -e DB_USERNAME=... \
     -e DB_PASSWORD=... \
     -e JWT_SECRET=... \
     voyager-backend:latest
   ```

   With `SPRING_PROFILES_ACTIVE=prod`, Actuator management listens on **8081** inside the container (API remains on **8080**), which aligns with ALB health checks in the companion infrastructure scripts.

### Docker Compose (optional)

`docker-compose.yml` is **optional**: it is useful for a quick local stack (PostgreSQL + backend) when you do not use an external database. Production-style deployments target **EC2 + RDS** and the manual deploy script below, not Compose.

## Usage

### Authentication

Most API endpoints require a valid JWT token. Include the token in the Authorization header:

```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/v1/users/1
```

### Key Endpoints

#### User Management

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john_doe",
    "password": "SecurePass123!"
  }'
```

#### Travel Planning

```bash
# Create a travel plan
curl -X POST http://localhost:8080/api/v1/travel-plans \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Paris Adventure",
    "description": "A week-long trip to Paris",
    "startDate": "2024-06-15T10:00:00",
    "endDate": "2024-06-22T18:00:00",
    "estimatedBudget": 3000.00,
    "numberOfTravelers": 2,
    "originLocation": "New York",
    "destinationLocation": "Paris, France"
  }'
```

#### Social Features

```bash
# Send connection request
curl -X POST http://localhost:8080/api/v1/social/connections \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": 123
  }'
```

### Environment Variables

Configure the application using environment variables (also supported via Spring Boot relaxed binding in `application.yml`):

```bash
# Database
export DB_URL=jdbc:postgresql://localhost:5432/tourism_platform
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password

# Application
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=your-long-random-secret

# CORS (see application.yml — patterns for AWS Academy / ALB, or explicit allow-all)
export CORS_USE_AWS_HOSTNAME_PATTERNS=true
export CORS_ALLOW_ALL_ORIGINS=false

# Optional
export SERVER_PORT=8080

# Travel catalog (GET /catalog/* — JWT required)
# Default: mock Amadeus-shaped JSON (~560 records, dates 2026-05-25 … 2027-01-31), no API key.
export AMADEUS_CATALOG_ENABLED=true
export AMADEUS_MOCK_MODE=true
# Live Amadeus (set mock false + OAuth2 app credentials):
# export AMADEUS_MOCK_MODE=false
# export AMADEUS_API_HOST=https://test.api.amadeus.com
# export AMADEUS_CLIENT_ID=...
# export AMADEUS_CLIENT_SECRET=...
# Optional: AMADEUS_CONNECT_TIMEOUT_MS, AMADEUS_READ_TIMEOUT_MS
```

For **RDS**, append SSL parameters as needed, for example: `DB_URL=jdbc:postgresql://your-host:5432/tourism_platform?sslmode=require`.

### Travel catalog (Amadeus-compatible)

Authenticated endpoints under **`/catalog`** (full path: `/api/v1/catalog/...`):

| Method | Path | Amadeus API (approx.) |
|--------|------|------------------------|
| GET | `/catalog/flights` | Flight Offers Search v2 |
| GET | `/catalog/hotels/by-city` | Hotel list by city |
| GET | `/catalog/hotels/offers` | Hotel offers shopping v3 |
| GET | `/catalog/activities` | Tours & activities by lat/long |

**Default (`AMADEUS_MOCK_MODE=true`)**: responses are generated locally with the **same JSON shapes** as Amadeus (flight-offer, hotel refs, hotel-offers, activities) so the web/mobile client can switch to production later without changing parsers. The dataset includes **560+** mocked flights, hotels, and activities across many countries/cities and departure/stay dates from **2026-05-25** through **2027-01-31** (deterministic seed).

**Live Amadeus**: set `AMADEUS_MOCK_MODE=false` and provide `AMADEUS_CLIENT_ID` / `AMADEUS_CLIENT_SECRET`; the server still uses **client credentials** only on the backend. Pricing and availability then come from [Amadeus Self-Service APIs](https://developers.amadeus.com) (`AMADEUS_API_HOST`).

## API Documentation

The API includes comprehensive Swagger/OpenAPI documentation:

- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v1/api-docs` (see `springdoc.api-docs.path` in `application.yml`)

### Response Format

All API responses follow a consistent format:

```json
{
  "timestamp": "2024-04-21T12:00:00Z",
  "status": 200,
  "message": "Operation successful",
  "data": { },
  "path": "/api/v1/users/123"
}
```

### Error Handling

Error responses include detailed validation information:

```json
{
  "timestamp": "2024-04-21T12:00:00Z",
  "status": 400,
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format"
    }
  ],
  "path": "/api/v1/users"
}
```

## Development

### Project Structure

```
src/main/java/com/tourism/platform/
  controller/          # REST API endpoints
  service/             # Business logic layer
  repository/          # Data access layer
  model/               # JPA entities
  dto/                 # Data transfer objects
  config/              # Configuration classes
  exception/           # Exception handling
```

### Code Quality

The project uses several tools to maintain code quality:

- **Lombok**: Reduces boilerplate code
- **Spring Validation**: Input validation
- **Swagger**: API documentation
- **JUnit 5**: Unit testing
- **Maven**: Build management

### Adding New Features

1. **Create Entity**: Add JPA entity in `model/` package
2. **Create DTO**: Add data transfer object in `dto/` package
3. **Create Repository**: Add repository interface in `repository/` package
4. **Create Service**: Add business logic in `service/` package
5. **Create Controller**: Add REST endpoints in `controller/` package

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run specific test class
mvn test -Dtest=UserServiceTest
```

### Test Structure

- **Unit Tests**: Located in `src/test/java/`
- **Integration Tests**: Test database interactions
- **API Tests**: Test REST endpoints
- **Coverage**: JaCoCo reports generated in `target/site/jacoco/`

## Postman (local API tests)

A ready-to-import Postman collection and local environment live at the **repository root**:

- `Voyager-Backend.postman_collection.json` - grouped requests (health, auth, users, travel plans, social, matching, activity sharing)
- `Voyager-Backend.local.postman_environment.json` - `baseUrl`, test username/password

Import both files in Postman, select the **Voyager Backend - Local** environment, run **Health** then **Login** (tests persist the JWT into collection variables). See **[POSTMAN.md](POSTMAN.md)** for the recommended flow and variable notes.

## Security

### Authentication

- **JWT Tokens**: Stateless authentication
- **Token Expiration**: Configurable token lifetime
- **Refresh Tokens**: Optional token refresh mechanism

### Authorization

- **Role-Based Access Control**: User roles and permissions
- **Method Security**: Fine-grained access control
- **Endpoint Security**: URL-based security rules

### Data Protection

- **Password Encryption**: BCrypt hashing
- **HTTPS**: TLS encryption in transit (recommended in production)
- **Environment Variables**: Sensitive configuration externalized
- **SQL Injection**: JPA parameterized queries

### CORS

CORS is driven by `app.cors` properties (exact origins, origin patterns, optional AWS hostname patterns for Academy/ALB frontends, and an explicit allow-all escape hatch). See `src/main/resources/application.yml` and environment variables `CORS_*`.

## Deployment

### CI (GitHub Actions)

Workflows under `.github/workflows/` build and test the project, run SonarCloud/Trivy where configured, and produce a **Docker image tarball** for manual handoff (AWS Academy Learner Lab: no automated push to a registry required):

- `docker build -t voyager-backend:latest .`
- `docker save voyager-backend:latest` → artifact consumed on the EC2 instance

Download the deployment artifact from the workflow run (image `.tar` + `scripts/ec2-deploy-backend.sh`).

### EC2 + RDS (manual script)

The database runs on **RDS** (or any external PostgreSQL). The container only runs the Spring Boot app.

1. **Prepare environment file** on the instance (default path used by the script: `/opt/voyager-backend/environment`). Use `KEY=value` lines, for example:

   ```bash
   SPRING_PROFILES_ACTIVE=prod
   DB_HOST=your-rds-endpoint.region.rds.amazonaws.com
   DB_PORT=5432
   DB_NAME=tourism_platform
   DB_USERNAME=your_user
   DB_PASSWORD=your_password
   DB_URL=jdbc:postgresql://your-rds-endpoint.region.rds.amazonaws.com:5432/tourism_platform?sslmode=require
   JWT_SECRET=your-long-random-secret
   ```

2. **Copy** the image tarball from CI and `scripts/ec2-deploy-backend.sh` onto the server.

3. **Run the deploy script** (as root):

   ```bash
   sudo chmod +x ec2-deploy-backend.sh
   sudo ./ec2-deploy-backend.sh /path/to/voyager-backend-image.tar
   ```

The script installs Docker if needed, ensures the PostgreSQL **database** exists on RDS (empty schema is fine), loads the image, and registers a **systemd** unit that runs `docker run` with `--env-file`, publishing **8080** and **8081**. Schema migrations are applied by **Flyway** when the application starts.

Optional overrides: `VOYAGER_INSTALL_ROOT`, `VOYAGER_ENV_FILE`, `VOYAGER_IMAGE`, `VOYAGER_SERVICE_NAME` (see script header comments).

Infrastructure (VPC, RDS instances, ALB, etc.) is maintained separately in the **voyager-infrastructure** repository; align health checks with Actuator on port **8081** when using the `prod` profile.

### Health Checks

- **Health (API context)**: `GET /api/v1/actuator/health`
- **Metrics**: `GET /api/v1/actuator/metrics` (restricted for non-admin callers as configured)
- **Info**: `GET /api/v1/actuator/info`

### Monitoring

- **Application Logs**: Structured JSON logging
- **Performance Metrics**: Micrometer integration
- **Database Monitoring**: Hikari connection pool metrics

## Contributors

- Andrés Felipe Calderón Ramírez - [AndresFelipeCalderonRamirez](https://github.com/AndresFelipeCalderonRamirez)
- Laura Natalia Perilla Quintero - [Lanapequin](https://github.com/Lanapequin)
- Ricardo Andres Ayala Garzon - [lRicardol](https://github.com/lRicardol)
- Santiago Amaya Zapata - [SantiagoAmaya21](https://github.com/SantiagoAmaya21)
- Santiago Botero Garcia - [LePeanutButter](https://github.com/LePeanutButter)

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

### License Summary

- **Commercial Use**: Yes
- **Modification**: Yes
- **Distribution**: Yes
- **Private Use**: Yes
- **Liability**: No
- **Warranty**: No

### Copyright

© 2026 Voyager Team. All rights reserved.
