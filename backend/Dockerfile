# Build + run ledger-core (service chính). Multi-stage: build jar bằng JDK 21 rồi chạy trên JRE.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
