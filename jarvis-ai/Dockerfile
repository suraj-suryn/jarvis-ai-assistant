# ---- Build Stage ----
FROM gradle:8-jdk17-alpine AS builder
WORKDIR /app
COPY . .
RUN gradle build -x test --no-daemon

# ---- Runtime Stage ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/jarus-ai.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
