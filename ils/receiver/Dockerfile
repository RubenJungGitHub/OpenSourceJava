# ===== STAGE 1: BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 1️⃣ Build shared module
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2

# 2️⃣ Build receiver module
COPY receiver/pom.xml receiver/
COPY receiver/src receiver/src
RUN mvn -B -U -f receiver/pom.xml clean package -DskipTests -Dmaven.repo.local=/build/.m2

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/config /app/data

COPY --from=build /build/receiver/target/*.jar app.jar
#COPY --from=build /build/receiver/src/main/resources/application-container.yaml /app/data/application.yaml
#COPY --from=build /build/receiver/src/main/resources/Containselfsigned_cert.p12 /app/config/Containselfsigned_cert.p12
COPY --from=build /build/receiver/target/classes/application-container.yaml /app/data/application.yaml
COPY --from=build /build/receiver/target/classes/Containselfsigned_cert.p12 /app/config/Containselfsigned_cert.p12

EXPOSE 5002
ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml
ENV APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12
ENTRYPOINT ["java","-jar","/app/app.jar"]