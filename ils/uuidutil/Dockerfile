# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy shared module first
COPY shared/pom.xml shared/
COPY shared/src shared/src

# Build shared into container-local Maven repo
RUN mvn -f shared/pom.xml clean install -DskipTests -Dmaven.repo.local=/build/.m2

# Copy uuidutil module
COPY uuidutil/pom.xml uuidutil/
COPY uuidutil/src uuidutil/src

# Build uuidutil using local repo
RUN mvn -f uuidutil/pom.xml clean package -DskipTests -Dmaven.repo.local=/build/.m2

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /build/uuidutil/target/*.jar app.jar

EXPOSE 5002
ENTRYPOINT ["java","-jar","/app/app.jar"]