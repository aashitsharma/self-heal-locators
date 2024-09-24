FROM openjdk:17-oracle
MAINTAINER AASHIT
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]