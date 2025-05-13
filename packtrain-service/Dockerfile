FROM maven:3-eclipse-temurin-23-alpine AS deps

WORKDIR /app

COPY packtrain-service/pom.xml ./

RUN mvn dependency:go-offline

FROM maven:3-eclipse-temurin-23-alpine AS build
COPY --from=deps /root/.m2/repository /root/.m2/repository

WORKDIR /app
COPY packtrain-service /app

COPY openapi/openapi.yml /app/src/main/resources/openapi.yml

RUN mvn clean package -Dmaven.test.skip=true

# Use a smaller base image for running the application
FROM eclipse-temurin:23-jre-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["java", "-jar", "/app/app.jar"]
