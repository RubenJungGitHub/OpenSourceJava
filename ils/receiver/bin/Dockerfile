# ===== STAGE 1: BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Build shared module first
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2

# Build receiver module (plain JAR)
COPY receiver/pom.xml receiver/
COPY receiver/src receiver/src
RUN mvn -B -U -f receiver/pom.xml clean package -DskipTests -Dmaven.repo.local=/build/.m2

# Build fat JAR for Docker runtime (docker profile)
RUN mvn -B -Pdocker -f receiver/pom.xml clean package spring-boot:repackage -DskipTests -Dmaven.repo.local=/build/.m2

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Prepare directories
RUN mkdir -p /app/config /app/data

# Copy fat JAR
COPY --from=build /build/receiver/target/*.jar app.jar

# Copy application config
COPY --from=build /build/receiver/target/classes/application-container.yaml /app/data/application.yaml

# ✅ Copy the keystore from build stage classes folder
COPY --from=build /build/receiver/target/classes/Containselfsigned_cert.p12 /app/config/Containselfsigned_cert.p12

# Expose port and set environment
EXPOSE 5002
ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml
ENV APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12

ENTRYPOINT ["java","-jar","/app/app.jar"]