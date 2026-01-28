cls
cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
docker build -t ils-app:latest .
docker compose  -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml down 
docker compose -f C:\ContainOpenSource\Java\OpenSourceJava\ECM\docker-compose.yml up --build


netstat -ano | findstr :5432
Get-Process -Id 32148

docker rm -f postgres
docker volume ls        # find volume used for Postgres
docker volume rm  main_postgres-data