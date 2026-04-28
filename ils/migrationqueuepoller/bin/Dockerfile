# Stage 1: Build all modules
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /build


# 1. Bouw shared module
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests

# 2. Bouw uuidutil (Cruciaal: Maven zoekt deze in Central en vindt hem niet)
COPY uuidutil/pom.xml uuidutil/
COPY uuidutil/src uuidutil/src
RUN mvn -B -U -f uuidutil/pom.xml clean install -DskipTests

# 3. Bouw receiver module
COPY receiver/pom.xml receiver/
COPY receiver/src receiver/src
RUN mvn -B -U -f receiver/pom.xml clean install -DskipTests

# Migration Poller module
COPY migrationqueuepoller/pom.xml migrationqueuepoller/
COPY migrationqueuepoller/src migrationqueuepoller/src
RUN mvn -B -U -f migrationqueuepoller/pom.xml clean package -DskipTests

# Stage 2: Runtime image
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy fat jar
COPY --from=build /build/migrationqueuepoller/target/migrationqueuepoller-3.3.0.jar /app/migrationqueuepoller.jar

# Copy application.yaml
COPY --from=build /build/migrationqueuepoller/target/classes/application-container.yaml /app/data/application.yaml
ENV SPRING_CONFIG_LOCATION=file:/app/data/application.yaml

# Expose debug port (optional)
EXPOSE 5005
# Expose HTTP port (if needed)
EXPOSE 55000

# Run in debug mode (suspend=n means container starts immediately)
ENTRYPOINT ["java","-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005","-jar","migrationqueuepoller.jar"]