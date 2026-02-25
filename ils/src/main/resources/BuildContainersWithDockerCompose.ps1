cls

$existingNetwork = docker network ls --format "{{.Name}}" | Where-Object { $_ -eq $networkName }

#Build  shared 
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
cd C:\ContainOpenSource\Java\OpenSourceJava\ils\shared
mvn clean install -DskipTests=true


#build uuidutil
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.10"
$env:Path = $env:JAVA_HOME + "\bin;" + $env:Path
cd C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil
mvn clean install -DskipTests=true
#docker build -t sharedresources:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ils\uuidutil\docker-compose.yml up -d --build
#docker images --digests | findstr notification-service



#Build ECM
cd C:\ContainOpenSource\Java\OpenSourceJava\ils
docker build -t ils-app:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ils\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ils\docker-compose.yml up --build



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