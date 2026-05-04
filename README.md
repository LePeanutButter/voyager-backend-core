# SmarTrip - Backend Core

[![Standard Readme](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)
[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg?style=flat-square)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-blue.svg?style=flat-square)](https://maven.apache.org/)
![License](https://img.shields.io/badge/license-GPL%203.0-blue.svg)

> The core backend API for the SmarTrip tourism intelligent platform, providing user management, travel planning, and AI-powered recommendations.

## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Security](#security)
- [Deployment](#deployment)
- [Contributors](#contributors)
- [License](#license)

## Background

SmarTrip is a comprehensive tourism platform that connects travelers with personalized AI-powered recommendations, social features, and seamless trip planning capabilities. This backend API serves as the central nervous system of the platform, handling:

- **User Management**: Authentication, authorization, and profile management
- **Travel Planning**: Itinerary creation, activity scheduling, and reservation management
- **Social Features**: User connections, reviews, and community interactions
- **AI Integration**: Personalized recommendations and intelligent matching
- **External Services**: Payment processing and third-party tourism provider integrations

The API follows REST best practices and implements the Richardson Maturity Model Level 3, ensuring consistent, scalable, and maintainable endpoints.

## Install

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- PostgreSQL 14 or higher
- Docker (optional, for containerized deployment)

### Local Development Setup

1. **Clone the repository**

   ```bash
   git clone https://github.com/LePeanutButter/voyager-backend-core.git
   cd voyager-backend-core
   ```

2. **Set up the database**

   ```bash
   # Create PostgreSQL database
   createdb -U postgres tourism_platform

   # Update application.yml with your database credentials
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   ```

3. **Build the application**

   ```bash
   mvn clean install
   ```

4. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080/api/v1`

### Docker Setup

1. **Build the Docker image**

   ```bash
   docker build -t smartrip-backend:latest .
   ```

2. **Run with Docker Compose**
   ```bash
   docker-compose up -d
   ```

## Usage

### Authentication

All API endpoints (except authentication endpoints) require a valid JWT token. Include the token in the Authorization header:

```bash
curl -H "Authorization: Bearer <your-jwt-token>" \
     http://localhost:8080/api/v1/users/profile
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

Configure the application using environment variables:

```bash
# Database configuration
export DB_USERNAME=your_db_user
export DB_PASSWORD=your_db_password
export DB_URL=jdbc:postgresql://localhost:5432/tourism_platform

# Application configuration
export SPRING_PROFILES_ACTIVE=prod
export SERVER_PORT=8080
```

## API Documentation

The API includes comprehensive Swagger/OpenAPI documentation:

- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/api/v1/api-docs`

### Response Format

All API responses follow a consistent format:

```json
{
  "timestamp": "2024-04-21T12:00:00Z",
  "status": 200,
  "message": "Operation successful",
  "data": { ... },
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
- **HTTPS**: TLS encryption in transit
- **Environment Variables**: Sensitive configuration externalized
- **SQL Injection**: JPA parameterized queries

## Deployment

### Production Deployment

1. **Environment Setup**

   ```bash
   # Set production environment variables
   export SPRING_PROFILES_ACTIVE=prod
   export DB_URL=jdbc:postgresql://prod-db:5432/tourism_platform
   export DB_USERNAME=${DB_USERNAME}
   export DB_PASSWORD=${DB_PASSWORD}
   ```

2. **Docker Deployment**

   ```bash
   # Build production image
   docker build -t smartrip-backend:1.0.0 .

   # Run with production configuration
   docker run -d \
     --name smartrip-backend \
     -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e DB_URL=${DB_URL} \
     -e DB_USERNAME=${DB_USERNAME} \
     -e DB_PASSWORD=${DB_PASSWORD} \
     smartrip-backend:1.0.0
   ```

3. **Kubernetes Deployment**
   ```bash
   # Apply Kubernetes manifests
   kubectl apply -f k8s/
   ```

### Health Checks

The application includes built-in health endpoints:

- **Health Check**: `GET /api/v1/actuator/health`
- **Metrics**: `GET /api/v1/actuator/metrics`
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
