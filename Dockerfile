# syntax=docker/dockerfile:1
# BuildKit cachea ~/.m2 entre builds (DOCKER_BUILDKIT=1, por defecto en Docker Desktop).
# Multi-stage Dockerfile for Tourism Platform Backend
# Stage 1: Build stage using Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven pom.xml and leverage Docker layer caching
# This layer is only rebuilt when pom.xml changes
COPY pom.xml .

# Download dependencies (capa + caché Maven en el host de Docker)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
# Use -DskipTests to skip tests in production build for faster build time
# Use -Dspring-boot.repackage.skip=false to ensure the JAR is created
ARG MAVEN_PROFILE=prod
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -Dspring-boot.repackage.skip=false -P${MAVEN_PROFILE}

# Stage 2: Runtime stage using lightweight JRE
FROM eclipse-temurin:17-jre-alpine AS runtime

# Install necessary packages for health checks and monitoring
RUN apk add --no-cache curl tzdata && \
    rm -rf /var/cache/apk/*

# Set timezone to UTC for consistent logging
ENV TZ=UTC

# Create non-root user for security
RUN addgroup -g 1000 -S appgroup && \
    adduser -u 1000 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# API (8080) and Actuator management (8081) when SPRING_PROFILES_ACTIVE=prod — map both on the host for ALB health checks.
EXPOSE 8080 8081

# Set JVM arguments for production
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Set Spring Boot profile
ENV SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata and container orchestration
LABEL org.opencontainers.image.title="Tourism Platform Backend" \
      org.opencontainers.image.description="Backend API for Tourism Intelligent Platform" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.vendor="Tourism Platform Team" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.source="https://github.com/tourism-platform/backend" \
      maintainer="tourism-platform-team@example.com"
