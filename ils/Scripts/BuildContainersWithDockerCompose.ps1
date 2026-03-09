cls

$existingNetwork = docker network ls --format "{{.Name}}" | Where-Object { $_ -eq $networkName }

#networks per container
docker ps -q | ForEach-Object {
    $c = docker inspect $_ | ConvertFrom-Json
    $name = $c.Name.TrimStart('/')
    $networks = $c.NetworkSettings.Networks.PSObject.Properties.Name -join ', '
    Write-Output "$name : $networks"
}

#recreate  network to make it attachebale 
docker network rm ils_ils_default
docker network create --driver=bridge --attachable ils_ils_default


#Check network connected containers
docker network ls
docker network inspect ils_default
docker network inspect ils_ils_default
docker network inspect ils-network
docker network inspect ils_ils-network
docker network inspect ecm_ils-network
docker network inspect end-user_default


docker network connect --ip 172.23.0.10 ils_ils_default subscriber-service

#build redis 
#docker run -d --name Redis-service --network ils_default -p 6379:6379 -p 8001:8001 redis/redis-stack:latest


#Build  shared 
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
cd C:\ContainOpenSource\Java\OpenSourceJava\ils\shared
mvn clean install -DskipTests=true


#Set springboot to version 21
#mvn clean spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"

#build uuidutil
cls
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
cd C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil
mvn clean install -DskipTests=true
#docker build -t sharedresources:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil\docker-compose.yml up -d --build
#docker images --digests | findstr notification-service

#The above two probably dont require build because it is define din the root pom.xml
#	<packaging>pom</packaging>
#	<modules>
#		<module>shared</module>
#		<module>uuidutil</module>
#	</modules>


#Build receiver
cls
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
cd C:\ContainOpenSource\Java\OpenSourceJava\ils
docker build -t ils-app:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ils\receiver\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ils\receiver\docker-compose.yml up --build



#Build  notificationservice
#cd C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service
#docker build -t notification-service-notificationservice:latest .
#docker compose  -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service\docker-compose.yaml down 
#docker compose -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service\docker-compose.yaml up -d --build
#docker images --digests | findstr notification-service


#Build  subsrciption service
#cd C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service
#docker build -t notification-service-notificationservice:latest .
#docker compose  -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service\docker-compose.yaml down 
#docker compose -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service\docker-compose.yaml up -d --build
#docker images --digests | findstr notification-service





#Build  UUID service
#cdC:\ContainOpenSource\Java\OpenSourceJava\ECM\uuidutil>
#docker build -t uuidutil:latest .
#docker compose  -fContainOpenSource\Java\OpenSourceJava\ECM\uuidutil\docker-compose.yaml down 
#docker compose -f ContainOpenSource\Java\OpenSourceJava\ECM\uuidutil\docker-compose.yaml up -d --build
#docker images --digests | findstr notification-service



#docker run -it --network ecm_ils-network --entrypoint sh notification-service-notificationservice:latest


#netstat -ano | findstr :5433
#Get-Process -Id 32148
#taskkill /PID 7404 /F


#docker rm -f postgres
#docker volume ls        # find volume used for Postgres
#docker volume rm  main_postgres-data

docker network connect ils_ils_default subscriber-service
docker inspect subscriber-service --format '{{json .NetworkSettings.Networks}}' | ConvertFrom-Json
docker network inspect ils_ils_default

docker inspect receiver --format '{{json .NetworkSettings.Networks}}' | ConvertFrom-Json


docker rm -f subscriber-service
docker network inspect ils_ils_default
cd C:\ContainOpenSource\Java\SharePointHandler\Sharepoint
docker compose -f docker-compose.yaml up -d --build subscriber-service
docker inspect subscriber-service --format '{{json .NetworkSettings.Networks}}' | ConvertFrom-Json
docker exec -it subscriber-service ping postgres-db

$env:JAVA_HOME="C:\Users\NL07428\AppData\Roaming\Code\User\globalStorage\pleiades.java-extension-pack-jdk\java\21"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn clean spring-boot:run