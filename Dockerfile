# Base Image (for installing Java and Maven)
FROM buntu:22.04 AS base

# Set environment variables for non-interactive installation
ENV DEBIAN_FRONTEND=noninteractive

# Install Java and Maven
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    tar \
    curl \
    git \
    && apt-get clean

# Install Maven
RUN wget https://dlcdn.apache.org/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz -P /tmp \
    && tar -xvzf /tmp/apache-maven-3.8.6-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.8.6/bin/mvn /usr/bin/mvn \
    && rm /tmp/apache-maven-3.8.6-bin.tar.gz

# Set environment variables for Java and Maven
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV MAVEN_HOME=/opt/apache-maven-3.8.6
ENV PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"

# Verify Java and Maven installation
RUN java -version && mvn -version

# Build Stage: Use Maven for building your app
FROM maven:3.8.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only necessary files for Maven build
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage: Use Java and Maven installed in the base image
FROM buntu:22.04

# Copy the JDK and Maven installation from the base stage
COPY --from=base /usr/lib/jvm/java-17-openjdk-amd64 /usr/lib/jvm/java-17-openjdk-amd64
COPY --from=base /opt/apache-maven-3.8.6 /opt/apache-maven-3.8.6

# Set environment variables for Java and Maven
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV MAVEN_HOME=/opt/apache-maven-3.8.6
ENV PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"

WORKDIR /home/ubuntu/1mg/analytics_event_dump

# Create logs directory (if required)
RUN mkdir -p logs

# Add metadata
LABEL maintainer="AASHIT"
LABEL app="analytics_event_dump"

# Copy everything from the build stage to the runtime stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application's default port (Spring Boot defaults to 8080)
EXPOSE 8080

# Set the entry point to start the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
