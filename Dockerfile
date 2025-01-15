# Build Stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only necessary files to take advantage of Docker caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# Add metadata
LABEL maintainer="AASHIT"

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application's default port (Spring Boot defaults to 8080)
EXPOSE 8080

# Set the entry point to start the Spring Boot app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
