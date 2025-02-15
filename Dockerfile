# Stage 1: Build the application using Gradle with Java 17
FROM gradle:8.2.1-jdk17 AS builder
WORKDIR /home/gradle/project
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./
COPY src src
RUN ./gradlew build -Dquarkus.package.type=uber-jar

# Stage 2: Create the runtime image using OpenJDK 17
FROM openjdk:17-jdk
WORKDIR /app
COPY --from=builder /home/gradle/project/build/*-runner.jar app.jar
EXPOSE 8182
ENTRYPOINT ["java", "-jar", "app.jar"]
