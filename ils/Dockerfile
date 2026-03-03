# ===== STAGE 1: BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy everything
COPY pom.xml ./
COPY shared ./shared
COPY receiver ./receiver
COPY sharepointqueuepoller ./sharepointqueuepoller
COPY uuidutil ./uuidutil

# Build all modules in one go (reactor builds dependencies correctly)
RUN mvn -B -U clean install -DskipTests -Dmaven.repo.local=/build/.m2

# ===== STAGE 2: UUIDUTIL IMAGE =====
FROM eclipse-temurin:21-jre-alpine AS uuidutil
WORKDIR /app
COPY --from=build /build/uuidutil/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ===== STAGE 3: RECEIVER IMAGE =====
FROM eclipse-temurin:21-jre-alpine AS receiver
WORKDIR /app
COPY --from=build /build/receiver/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# ===== STAGE 4: SHAREPOINT POLLER IMAGE =====
FROM eclipse-temurin:21-jre-alpine AS sharepointpoller
WORKDIR /app
COPY --from=build /build/sharepointqueuepoller/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]