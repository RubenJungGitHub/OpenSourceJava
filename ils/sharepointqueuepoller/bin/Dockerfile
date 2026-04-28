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

# 4. Bouw sharepointqueuepoller module
# Nu zijn alle lokale dependencies (uuidutil, shared, receiver) aanwezig in de lokale .m2
COPY sharepointqueuepoller/pom.xml sharepointqueuepoller/
COPY sharepointqueuepoller/src sharepointqueuepoller/src
RUN mvn -B -U -f sharepointqueuepoller/pom.xml clean package -DskipTests

# Stage 2: Package the final runtime image
FROM eclipse-temurin:21-jre

# Werkdirectory in de container
WORKDIR /app

# Kopieer de uiteindelijke JAR van de build stage
COPY --from=build /build/sharepointqueuepoller/target/sharepointqueuepoller-3.3.0.jar /app/sharepointqueuepoller.jar

# Kopieer de configuratie
# Let op: zorg dat het pad naar application-container.yaml klopt in je projectstructuur
COPY --from=build /build/sharepointqueuepoller/target/classes/application-container.yaml /app/data/application.yaml

# Omgevingsvariabelen voor Spring Boot
ENV SPRING_CONFIG_LOCATION=file:/app/data/application.yaml

# Start de applicatie
ENTRYPOINT ["java", "-jar", "sharepointqueuepoller.jar"]