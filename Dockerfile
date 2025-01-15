# Build Stage
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only necessary files for Maven build
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /home/ubuntu/1mg/analytics_event_dump

# Create logs directory (if required)
RUN mkdir -p logs

# Add metadata
LABEL maintainer="AASHIT"
LABEL app="analytics_event_dump"

# Copy everything from the build stage to the runtime stage
COPY --from=build /app .

# Expose the application's default port (Spring Boot defaults to 8080)
EXPOSE 8080

# Set the entry point to start the Spring Boot app
ENTRYPOINT ["sh", "-c", "java -jar target/com.onemg.analytics.data.dump-*.jar"]

# Copy code folder
COPY . .
