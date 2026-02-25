# ---------- BUILD STAGE ----------
FROM azul/zulu-openjdk:21 AS build
WORKDIR /app

# Install Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# ---------- Build shared ----------
COPY ../shared ./shared
RUN mvn -f shared/pom.xml clean install -DskipTests

# ---------- Build uuidutil ----------
COPY ../uuidutil ./uuidutil
RUN mvn -f uuidutil/pom.xml clean package -DskipTests

# ---------- RUNTIME STAGE ----------
FROM azul/zulu-openjdk:21-jre
WORKDIR /app

# Create config/data folders
RUN mkdir -p /app/config /app/data

# Copy the built uuidutil JAR
COPY --from=build /app/uuidutil/target/*.jar app.jar

# Expose service port
EXPOSE 5002

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]