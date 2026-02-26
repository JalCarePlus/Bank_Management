# Use official Java 17 image
FROM eclipse-temurin:17-jdk-jammy

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/banking-app-0.0.1-SNAPSHOT.jar app.jar

# Expose port (Render uses PORT env)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]