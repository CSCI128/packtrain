FROM eclipse-temurin:23-jdk AS builder

WORKDIR /build

COPY grading-admin-service/.mvn/ .mvn
COPY grading-admin-service/mvnw grading-admin-service/pom.xml ./

RUN ./mvnw dependency:go-offline

COPY grading-admin-service/src ./src

# # basically, copy any needed config files from the config docker container
# # right now this is just copying the openapi config
# RUN --mount=type=bind,target=/config,source=/,from=agent-rodriguez-config:latest \
#     cp --remove-destination /config/build-config/openapi.yaml /app/src/main/resources/openapi.yaml

RUN ./mvnw clean package -Dmaven.test.skip=true

# Use a smaller base image for running the application
FROM eclipse-temurin:23-jre

WORKDIR /app

COPY --from=builder /build/target/*.jar app.jar

CMD ["java", "-jar", "/app/app.jar"]
