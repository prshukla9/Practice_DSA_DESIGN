# Multi-stage Dockerfile template for all Spring Boot services
# Build: docker build -f Dockerfile --build-arg SERVICE=patient-service -t patient-service:1.0.0 .

ARG SERVICE
FROM maven:3.9-eclipse-temurin-8 AS build
ARG SERVICE
WORKDIR /app
COPY pom.xml .
COPY common-lib ./common-lib
COPY patient-service ./patient-service
COPY consent-service ./consent-service
COPY integration-service ./integration-service
COPY ai-service ./ai-service
COPY notification-service ./notification-service
RUN mvn -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:8-jre-alpine
ARG SERVICE
WORKDIR /app
COPY --from=build /app/${SERVICE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
