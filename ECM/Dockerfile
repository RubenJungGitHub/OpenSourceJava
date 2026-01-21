# ===== BUILD STAGE =====
FROM azul/zulu-openjdk:21 AS build

WORKDIR /app
COPY pom.xml .

# Install Maven if needed (some images include it)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

COPY src ./src
RUN mvn clean package -DskipTests

# ===== RUNTIME STAGE =====
FROM azul/zulu-openjdk:21-jre

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


#Run from terminal or powershell :

#The underpinned line is to build the docker IMAGE. 
#RUN FROM  terminal
#cd C:\ContainOpenSource\Java\OpenSourceJava\ECM> 
#docker build -t ils-app:latest .

#The next lines are for creating the container and run the config.
#docker network create ils-network

#docker run -d -p
#  --name Contain-ILS 
#  --network ils-network 
#    -v C:\path\to\application.yml:/app/config/application.yml 
#  -p 5000:5000
#  -e SPRING_REDIS_HOST=Redis-service 
#  -e SPRING_REDIS_PORT=6379 
#  ils-app

#docker logs -f Contain-ILS
#docker run -d --name Redis-service --network ils-network -p 6379:6379 -p 8001:8001 redis/redis-stack-server:latest
#docker network create ils-network   # if not already created
#docker network connect ils-network Contain-ILS
#docker network connect ils-network Redis-service

#docker exec -it Contain-ILS java -version
