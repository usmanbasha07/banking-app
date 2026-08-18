# =========================================================
#  Multi-stage Dockerfile
#  Stage 1: Build the JAR using Maven
#  Stage 2: Run the JAR using a lightweight JRE image
#
#  Pass the active Spring profile at runtime:
#    docker run -e SPRING_PROFILES_ACTIVE=prod ...
# =========================================================

# ---------- Stage 1: Build ----------
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom first (layer caching — dependencies
# are only re-downloaded when pom.xml changes)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies without building source (cache layer)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build the JAR (skip tests in image build)
COPY src src
RUN ./mvnw package -DskipTests -B

# ---------- Stage 2: Run ----------
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create a non-root user for security
RUN addgroup -S banking && adduser -S banking -G banking

WORKDIR /app

# Copy only the built JAR from builder stage
COPY --from=builder /build/target/banking-app-0.0.1-SNAPSHOT.jar app.jar

# Change ownership to non-root user
RUN chown banking:banking app.jar

USER banking

# Expose port (overridable via SERVER_PORT env var)
EXPOSE 8080

# Active profile and other secrets are injected via environment variables
# at container runtime — never baked into the image
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]