# Step 1: build the app
FROM maven:3.9.3-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the JAR
RUN mvn clean package -DskipTests

# Step 2: create minimal runtime image
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy JAR from the build stage
COPY --from=build /app/target/banking-app-0.0.1-SNAPSHOT.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","/app/app.jar"]
