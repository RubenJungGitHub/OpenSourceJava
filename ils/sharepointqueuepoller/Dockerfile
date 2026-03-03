# ===== STAGE 1: BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1️⃣ Build shared module (dependency)
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2

# 2️⃣ Build receiver module (dependency)
COPY receiver/pom.xml receiver/
COPY receiver/src receiver/src
RUN mvn -B -U -f receiver/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2

# 3️⃣ Build SharePointQueuePoller (main app)
COPY sharepointqueuepoller/pom.xml sharepointqueuepoller/
COPY sharepointqueuepoller/src sharepointqueuepoller/src
RUN mvn -B -U -f sharepointqueuepoller/pom.xml clean package -DskipTests -Dmaven.repo.local=/build/.m2

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/config /app/data

# Copy built JAR
COPY --from=build /build/sharepointqueuepoller/target/*.jar app.jar

# Copy config
COPY --from=build /build/sharepointqueuepoller/src/main/resources/application-container.yaml /app/data/application.yaml

# Force profile
ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml
ENV SPRING_PROFILES_ACTIVE=sp
ENV APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12

ENTRYPOINT ["java","-jar","/app/app.jar"]