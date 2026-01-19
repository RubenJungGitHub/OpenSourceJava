
cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
#docker build -t ils-app:java21 
#docker network create ils-network

#Furthermore is to run the container from scratch 
<#
docker run -d `
  --name Contain-ILS `
  --network ils-network `
  -p 5000:5000 `
  -e SPRING_REDIS_HOST=Redis-service `
  -e SPRING_REDIS_PORT=6379 `
  ils-app:java21
 #>


  # Only run with new config file 
  docker run -d --name Contain-ILS --network ils-network -v C:\ContainOpenSource\Java\OpenSourceJava\ECM\src\main\resources\application.yml:/app/config/application.yml -p 5000:5000  ils-app

# Stop and remove old container if exists
docker stop Redis-service -ErrorAction SilentlyContinue
docker rm Redis-service -ErrorAction SilentlyContinue

# Run new Redis Stack container with UI enabled
docker run -d `
  --name Redis-service `
  -p 6379:6379 `
  -p 8001:8001 `
  -e REDISSTACK_UI_ENABLE=true `
  -e REDISSTACK_PASSWORD= `
  redis/redis-stack-server:latest

  docker logs -f Redis-service

 #>

#docker run -d --name Redis-service --network ils-network -p 6379:6379 -p 8001:8001 redis/redis-stack-server:latest
docker network connect ils-network Contain-ILS
docker network connect ils-network Redis-service
docker logs -f Contain-ILS

