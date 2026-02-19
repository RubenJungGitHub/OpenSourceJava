cls

#Build ECM
#cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
#docker build -t ils-app:latest .
#docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml down 
#docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml up --build



#Build  notificationservice
#cd C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service
#docker build -t notification-service-notificationservice:latest .
#docker compose  -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service\docker-compose.yaml down 
#docker compose -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\notification-service\docker-compose.yaml up -d --build
#docker images --digests | findstr notification-service


#Build  subsrciption service
cd C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service
#docker build -t notification-service-notificationservice:latest .
docker compose  -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service\docker-compose.yaml down 
docker compose -f C:\ContainOpenSource\Java\SharePointHandler\Sharepoint\subscriber-service\docker-compose.yaml up -d --build
docker images --digests | findstr notification-service



#docker run -it --network ecm_ils-network --entrypoint sh notification-service-notificationservice:latest


#netstat -ano | findstr :5433
#Get-Process -Id 32148
#taskkill /PID 7404 /F


#docker rm -f postgres
#docker volume ls        # find volume used for Postgres
#docker volume rm  main_postgres-data