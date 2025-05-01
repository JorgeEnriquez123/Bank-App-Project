# Bank App Project
This app is a simple banking application that allows users to create accounts, have debit and credit cards, have a virtual wallet, deposit and withdraw money, and transfer funds between accounts. 
It also includes services like a virtual coin wallet, credits, and transaction management.

It is built using Spring Boot and uses Reactive programming with Project Reactor and RxJava. Microservice architecture is used to separate the different functionalities of the application into different services.

# Technologies
- Java 17
- Spring Boot
- Spring WebFlux
- Reactive MongoDB
- Spring Data
- Spring Cloud Gateway
- Spring Cloud Config
- Spring Cloud Circuit Breaker
- Spring Cloud Discovery
- Spring Security (Gateway Level)
- JWT
- Redis
- WebClient
- Kafka
- Docker
- OpenAPI (Contract first)
- JUnit
- Mockito
- Lombok

# How to run

## Running the whole app with Docker
```shell
docker-compose up --build
```

## Running each service separately and running the dependencies with Docker
1. Start the dependencies:
```shell
docker-compose -f docker-compose-dependencies.yml up
```
2. Start each service separately (run this in each service folder or start them with your IDE):
```shell
mvn clean install
mvn spring-boot:run
```

# Reaching the services through the API Gateway
Though you can reach each service directly, another option is to reach them through the API Gateway.

Making a request through the API Gateway (http://localhost:6666) requires a JWT token to be passed in the Authorization header (except for the Customer Service).

The security requirement is simulated by requiring a JWT token that contains the DNI of the Customer, so the request can pass through.
## Steps
1. Register a new customer using the Customer Service
2. Use the '/customers/dni/:dni/login' path to get a JWT token
3. Use the JWT token to make requests to the other services through the API Gateway by using the gateway base URL (http://localhost:6666) and the service path.