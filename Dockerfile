# Use lightweight Java image
FROM eclipse-temurin:21-jdk-alpine

# Set working directory
WORKDIR /app

# Copy generated jar file
COPY target/banking-app-0.0.1-SNAPSHOT.jar app.jar

# Expose application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]