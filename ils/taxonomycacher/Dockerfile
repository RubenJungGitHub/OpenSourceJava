# ===== STAGE 1: BUILD =====
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# ... (Keep stage 1 setup)

# 1. Build shared module
COPY shared/pom.xml shared/
COPY shared/src shared/src
RUN mvn -B -U -f shared/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2


# 3. Build taxonomycacher module
COPY taxonomycacher/pom.xml taxonomycacher/
COPY taxonomycacher/src taxonomycacher/src
RUN mvn -B -U -f taxonomycacher/pom.xml clean package -DskipTests -Dmaven.repo.local=/build/.m2

# ===== STAGE 2: RUNTIME =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# Prepare directories
RUN mkdir -p /app/config /app/data


# Copy fat JAR
COPY --from=build /build/taxonomycacher/target/*.jar app.jar

# Copy application config
COPY --from=build /build/shared/target/classes/application.yaml /app/data/application.yaml


# Expose port and set environment
EXPOSE 60000
ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml


ENTRYPOINT ["java","-jar","/app/app.jar"]