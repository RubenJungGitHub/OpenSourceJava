# ===== BUILD STAGE =====
FROM azul/zulu-openjdk:21 AS build

WORKDIR /app
COPY pom.xml .

# Install Maven (needed only in build stage)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY src ./src
RUN mvn clean package -DskipTests

# ===== RUNTIME STAGE =====
FROM azul/zulu-openjdk:21-jre

WORKDIR /app
RUN mkdir -p /app/config /app/data

# Copy JAR from build stage
COPY --from=build /app/target/*.jar app.jar

# Copy config + cert from build stage
COPY --from=build /app/src/main/resources/application-container.yaml /app/data/application.yaml
COPY --from=build /app/src/main/resources/Containselfsigned_cert.p12 /app/config/Containselfsigned_cert.p12

EXPOSE 8080

# Environment variables for Spring Boot
ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml
ENV APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12

ENTRYPOINT ["java", "-jar", "app.jar"]
