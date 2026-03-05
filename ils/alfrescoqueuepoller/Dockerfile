# Stage 1: Build all modules
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /build

# Copy and build shared module (if exists)
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests

# Copy and build receiver module
COPY receiver/pom.xml receiver/
COPY receiver/src receiver/src
RUN mvn -B -U -f receiver/pom.xml clean install -DskipTests

# Copy and build alfrescoqueuepoller module
COPY alfrescoqueuepoller/pom.xml alfrescoqueuepoller/
COPY alfrescoqueuepoller/src alfrescoqueuepoller/src
RUN mvn -B -U -f alfrescoqueuepoller/pom.xml clean package -DskipTests

# Stage 2: Package the final runtime image
FROM eclipse-temurin:21-jre

# Copy the final JAR from build stage
COPY --from=build /build/alfrescoqueuepoller/target/alfrescoqueuepoller-3.3.0.jar /app/alfrescoqueuepoller.jar
COPY --from=build /build/alfrescoqueuepoller/target/classes/application-container.yaml /app/data/application.yaml
ENV SPRING_CONFIG_LOCATION=file:/app/data/application.yaml
WORKDIR /app

# Command to run your application
ENTRYPOINT ["java","-jar","alfrescoqueuepoller.jar"]