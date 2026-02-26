# ===== BUILD STAGE =====
FROM azul/zulu-openjdk:21 AS build

WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Copy everything (root + submodules)
COPY . .

# Build all modules in one go
RUN mvn clean install -DskipTests

# ===== RUNTIME STAGE =====
FROM azul/zulu-openjdk:21-jre

WORKDIR /app
RUN mkdir -p /app/config /app/data

# Copy the main JAR
COPY --from=build /app/target/*.jar app.jar

# Copy config + cert
COPY --from=build /app/src/main/resources/application-container.yaml /app/data/application.yaml
COPY --from=build /app/src/main/resources/Containselfsigned_cert.p12 /app/config/Containselfsigned_cert.p12

EXPOSE 8080

ENV SPRING_CONFIG_LOCATION=/app/data/application.yaml
ENV APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12

ENTRYPOINT ["java", "-jar", "app.jar"]