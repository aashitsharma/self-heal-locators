
## To Start Analytics Service using mvn
- mvn clean install
- mvn spring-boot:run

#Below Commands are for Docker

## To Start the Analytics Dump Service
 - docker compose up --build

## To Stop the Analytics Dump Service
- docker compose down -v

## To Scale Up Analytics Dump Service
- docker compose up --scale springboot-app=5
