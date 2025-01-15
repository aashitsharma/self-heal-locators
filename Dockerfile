# Base Image (for installing Java and Maven)
FROM ubuntu:22.04 AS base

# Set environment variables for non-interactive installation
ENV DEBIAN_FRONTEND=noninteractive

# Update dpkg repositories and install essential tools
RUN apt-get update && \
    apt-get install -y curl rsync wget ssh git xvfb unzip gnupg ca-certificates-java

# Install OpenJDK-17
RUN apt-get update && \
    apt-get install -y openjdk-17-jdk ant && \
    apt-get clean;

# Fix certificate issues
RUN apt-get update && \
    apt-get install -y ca-certificates-java && \
    apt-get clean && \
    update-ca-certificates -f;

# Setup JAVA_HOME -- useful for docker commandline
ENV JAVA_HOME /usr/lib/jvm/java-1.17.0-openjdk-amd64/
RUN export JAVA_HOME

# Set environment variables for Maven
ENV MAVEN_VERSION=3.9.4
ENV MAVEN_HOME=/opt/maven
ENV PATH=${MAVEN_HOME}/bin:${PATH}

# Install Maven
RUN mkdir -p /opt && \
    wget -q -O - http://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar -xzf - -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} ${MAVEN_HOME}

# Verify Maven installation
RUN mvn --version

# Setup MAVEN Properties
ENV MAVEN_CONFIG /root/.m2
RUN export M2_HOME=/opt/maven

# Build Stage: Use Maven for building your app
FROM base AS build
WORKDIR /app

# Copy only necessary files for Maven build
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package spring-boot:repackage -DskipTests

# Runtime Stage: Use Java and Maven installed in the base image
FROM base

# Copy the JDK and Maven installation from the base stage
COPY --from=build /usr/lib/jvm/java-1.17.0-openjdk-amd64 /usr/lib/jvm/java-1.17.0-openjdk-amd64
COPY --from=build /opt/maven /opt/maven

# Set environment variables for Java and Maven
ENV JAVA_HOME=/usr/lib/jvm/java-1.17.0-openjdk-amd64
ENV MAVEN_HOME=/opt/maven
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

COPY . .
