
cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
##recreate image!
#mvn clean package
#docker build -t ils-app:latest .


$User = "contAIn"
$UserPassword = "contAIn123"

$saUser = "SA"
$saPassword = "contAIn123!"
$dbName = "contAInBallenbak"
$networkName = "ils-network"
$containers = @("Redis-service", "Contain-ILS", "sql-express")
$containers = @("Contain-ILS", "sql-express")
foreach ($c in $containers) {
    if (docker ps -a --format "{{.Names}}" | Select-String "^$c$") {
        docker stop $c
        docker rm $c
    }
}

# Remove old network if it exists
$existingNetwork = docker network ls --format "{{.Name}}" | Where-Object { $_ -eq $networkName }


if (-not $existingNetwork) {
    Write-Host "Creating network $networkName..."
    docker network create $networkName
} else {
    Write-Host "Network $networkName already exists."
}

#recreate sql-express-data
#docker volume rm sql-express-data
docker volume create sql-express-data

#Disable for now. Redis is running and does not need rerun each time
#docker run -d --name Redis-service --network ils-network -p 6379:6379 -p 8001:8001 redis/redis-stack:latest


#Run Redis Stack container
#Wait for redis 
do {
    $redisReady = Test-NetConnection -ComputerName localhost -Port 6379
    Write-Host "Waiting for Redis service......."
    Start-Sleep -Seconds 2

} until ($redisReady.TcpTestSucceeded)





##Run SQL Express in a container 
    docker run -d --name sql-express `
    --name sql-express `
    --network ils-network `
    -e ACCEPT_EULA=Y `
    -e MSSQL_PID=Express `
    -e "SA_PASSWORD=$saPassword" `
    -e "MSSQL_PID=Express" `
    -p 14330:1433 `
    -v sql-express-data:/var/opt/mssql `
    --health-cmd=" /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P contAIn123! -Q 'SELECT 1' || exit 1 " `
    --health-interval=10s `
    --health-timeout=3s `
    --health-retries=10 `
    mcr.microsoft.com/mssql/server:2019-latest


#docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=contAIn123!" -p 14331:1433 --name sql-test -d mcr.microsoft.com/mssql/server:2019-latest

Write-Host "Waiting for SQL Server to accept connections..."

$maxAttempts = 30
$attempt = 0



do {
    try {
        docker exec sql-express /opt/mssql-tools/bin/sqlcmd `
            -S localhost `
            -U sa `
            -P $saPassword `
            -d master `
            -Q "SELECT 1" | Out-Null

        Write-Host "SQL Server is ready."
        break
    }
    catch {
        $attempt++
        Write-Host "SQL not ready yet... ($attempt/$maxAttempts)"
        Start-Sleep -Seconds 3
    }
} while ($attempt -lt $maxAttempts)

if ($attempt -eq $maxAttempts) {throw "SQL Server did not become ready in time."}




# Connect SQL Express to ils-network for container access
#docker network connect ils-network sql-express
Write-Host "SQL Express connected to ils-network."

Write-Host "Initializing database and SQL user..."

# Create database
docker run -i --rm --network ils-network mcr.microsoft.com/mssql-tools `
    /opt/mssql-tools/bin/sqlcmd `
    -S sql-express `
    -U "$saUser" `
    -P "$saPassword" `
    -d master `
    -Q "IF DB_ID('$dbName') IS NULL CREATE DATABASE [$dbName];"

# Create Contain user
docker run -i --rm --network ils-network mcr.microsoft.com/mssql-tools `
    /opt/mssql-tools/bin/sqlcmd `
    -S sql-express `
    -U "$saUser" `
    -P "$saPassword" `
    -d master `
    -Q @"
IF NOT EXISTS (SELECT * FROM sys.sql_logins WHERE name = '$User')
BEGIN
    CREATE LOGIN [$User] WITH PASSWORD = '$UserPassword', CHECK_POLICY = OFF, CHECK_EXPIRATION = OFF;
END;
USE [$dbName];
IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = '$User')
BEGIN
    CREATE USER [$User] FOR LOGIN [$User];
    EXEC sp_addrolemember 'db_owner', '$User';
END;
"@


docker run --rm `
  --network ils-network `
  -v "C:\ContainOpenSource\Java\OpenSourceJava\ECM\src\main\resources\db\migration:/scripts" `
  mcr.microsoft.com/mssql-tools `
  /opt/mssql-tools/bin/sqlcmd `
    -S sql-express `
    -U sa `
    -P $saPassword `
    -d contAInBallenbak `
    -i /scripts/V1__create_tblIOLog.sql `
    -b


docker run -it --rm --network ils-network redis/redis-stack redis-cli -h Redis-service -p 6379 ping

docker run -d `
  --name Contain-ILS `
  --network ils-network `
  -p 5000:5000 `
  -v "C:/ContainOpenSource/Java/OpenSourceJava/ECM/src/main/resources/application.yaml:/app/config/application.yaml" `
  -v "C:/ContainOpenSource/Java/OpenSourceJava/ECM/src/main/resources/Containselfsigned_cert.p12:/app/config/Containselfsigned_cert.p12" `
  -e SPRING_REDIS_HOST=Redis-service `
  -e APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12 `
  -e SPRING_DATASOURCE_URL="jdbc:sqlserver://sql-express:1433;databaseName=contAInBallenbak;encrypt=false" `
  -e SPRING_DATASOURCE_USERNAME=$saUser `
  -e SPRING_DATASOURCE_PASSWORD=$saPassword `
  ils-app

  #Run this when end-user container is recreated to add ActiveMQ to the internal container network.
  docker network connect ils-network end-user-activemq-1

#Start-Sleep -Seconds 30

#Write-Host "Triggering Flyway migrations manually..."
#docker exec -i Contain-ILS java -jar /app/app.jar flyway:clean
#docker exec -i Contain-ILS java -jar /app/app.jar flyway:migrate




# Check if Contain-ILS is already on the network
$connected = docker network inspect $networkName --format '{{range .Containers}}{{.Name}} {{end}}' | Select-String "Contain-ILS"

if ($connected -eq $null) {
    Write-Host "Connecting Contain-ILS to network..."
    docker network connect $networkName Contain-ILS
} else {
    Write-Host "Contain-ILS is already connected to $networkName"
}

# Same for Redis
$connected = docker network inspect $networkName --format '{{range .Containers}}{{.Name}} {{end}}' | Select-String "Redis-service"

if ($connected -eq $null) {
    Write-Host "Connecting Redis-service to network..."
    docker network connect $networkName Redis-service
} else {
    Write-Host "Redis-service is already connected to $networkName"
}
#docker ps
#docker logs -f Redis-service

 #>


#Attach to docker terminal real time
#docker logs -f Contain-ILS