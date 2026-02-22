# ========================
# Stage 1: Build
# ========================
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Gradle wrapper and build files first (layer caching)
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Grant execute permission
RUN chmod +x gradlew

# Download dependencies (cached unless build.gradle changes)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN ./gradlew bootJar --no-daemon -x test

# ========================
# Stage 2: Runtime
# ========================
FROM eclipse-temurin:25-jre

WORKDIR /app

# Add non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/novel-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R appuser:appuser /app

USER appuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM tuning for Standard_B1ms (2GB RAM) - limit heap to ~512MB
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=50.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]

