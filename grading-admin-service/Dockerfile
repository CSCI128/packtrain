FROM maven:3-eclipse-temurin-21-alpine AS deps

WORKDIR /app

ADD pom.xml /app

RUN mvn dependency:go-offline

FROM maven:3-eclipse-temurin-21-alpine AS build
COPY --from=deps /root/.m2/repository /root/.m2/repository

WORKDIR /app
COPY . /app
# basically, copy any needed config files from the config docker container
# right now this is just copying the openapi config
RUN --mount=type=bind,target=/config,source=/,from=agent-rodriguez-config:latest \
    cp --remove-destination /config/build-config/openapi.yaml /app/src/main/resources/openapi.yaml
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine AS run

COPY --from=build /app/target/AgentRodriguezService*.jar /app/app.jar

CMD ["java", "-jar", "/app/app.jar"]
