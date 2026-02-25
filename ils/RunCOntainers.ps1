
cd C:\ContainOpenSource\Java\OpenSourceJava\ECM
##recreate image!
cls


#Command to check inside conainer
#docker exec -it Contain-ILS sh
#ls 
#ls /app
#ls config/
#cat /app/config/application.yaml

$User = "contAIn"
$UserPassword = "contAIn123"
$dbName = "contAInBallenbak"
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
#Rebuild image 
#docker build -t ils-app:latest .

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

#Run SQL Express in a container 
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

Write-Host "SQL Express connected to ils-network."


Write-Host "Initializing database and SQL user..."

# Create contain login and DB
Write-Host "Create login and DB if non existant"

docker run -i --rm --network ils-network mcr.microsoft.com/mssql-tools `
    /opt/mssql-tools/bin/sqlcmd `
    -S sql-express `
    -U "$saUser" `
    -P "$saPassword" `
    -d master `
    -Q "IF DB_ID('$dbName') IS NULL CREATE DATABASE [$dbName];"


Write-Host "Create contAIn user"
# Create Contain user
docker run -i --rm --network ils-network mcr.microsoft.com/mssql-tools `
/opt/mssql-tools/bin/sqlcmd `
-S sql-express `
-U "$saUser" `
-P "$saPassword" `
-d master `
-Q @"
IF NOT EXISTS (SELECT * FROM sys.sql_logins WHERE name = '$saUser')
BEGIN
    CREATE LOGIN [$saUser]
    WITH PASSWORD = '$saPassword',
         CHECK_POLICY = OFF,
         CHECK_EXPIRATION = OFF;
END;

USE [$dbName];

IF NOT EXISTS (SELECT * FROM sys.database_principals WHERE name = '$saUser')
BEGIN
    CREATE saUser [$saUser] FOR LOGIN [$saUser];
    EXEC sp_addrolemember 'db_owner', '$saUser';
END;

IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tblIOLog')
BEGIN
    CREATE TABLE [dbo].[tblIOLog] (
        [UUID] varchar(36) NOT NULL,
        [containIOUUID] varchar(36) NOT NULL,
        [PlatformID] varchar(36) NULL,
        [Path] varchar(max) NULL,
        [IOAction] varchar(max) NOT NULL,
        [IOSource] varchar(50) NOT NULL,
        [IODestination] varchar(50) NOT NULL,
        [PKIHash] varchar(max) NULL,
        [IOreference] varchar(50) NOT NULL,
        [AdditionalInfo] varchar(max) NULL,
        [LogDateTime] datetime NOT NULL,
        [ActionPerformed] varchar(20) NOT NULL,
        [ActionPerformedBy] varchar(50) NOT NULL,
        CONSTRAINT chk_AP CHECK (
            [ActionPerformed] IN (
                'IOMOVED','ASSIGNUUID','IORENAMED','IOCLASSIFIED',
                'COPIEDUUID','IODELETED','IOCOPIED','IOBOUND'
            )
        ),
        CONSTRAINT PK_tblIOLog PRIMARY KEY ([UUID])
    );
END;


IF NOT EXISTS (SELECT * FROM sys.tables WHERE name = 'tblSPDeltalinkRepository')
BEGIN
    CREATE TABLE [dbo].[tblSPDeltalinkRepository] (
        [LogDateTime] DATETIME NOT NULL,
        [LastDeltaLink] VARCHAR(MAX) NOT NULL,
        [SourceID] VARCHAR(256) NOT NULL,
        [TokenID] VARCHAR(MAX) NOT NULL
    );
END;
"@





docker run --rm `
  --network ils-network `
  -v "C:\ContainOpenSource\Java\OpenSourceJava\ils\src\main\resources\db\migration:/scripts" `
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
  -p 5000:8080 `
  -v C:/ContainOpenSource/Java/OpenSourceJava/ils/src/main/resources/application-container.yaml:/app/config/application.yaml `
  -v C:/ContainOpenSource/Java/OpenSourceJava/ils/src/main/resources/Containselfsigned_cert.p12:/app/config/Containselfsigned_cert.p12 `
  -v C:/ContainOpenSource/Java/OpenSourceJava/ils/data:/app/data `
  -e SPRING_CONFIG_LOCATION=/app/config/application.yaml `
  -e APP_KEYSTORE_PATH=/app/config/Containselfsigned_cert.p12 `
  -e SPRING_REDIS_HOST=Redis-service `
    ils-app 
  


  #Run this when end-user container is recreated to add ActiveMQ to the internal container network.
  docker network connect ils-network end-user-activemq-1

#Start-Sleep -Seconds 30

#Write-Host "Triggering Flyway migrations manually..."
#docker exec -i Contain-ILS java -jar /app/app.jar flyway:clean
#docker exec -i Contain-ILS java -jar /app/app.jar flyway:migrate

#NOw also connect Contain-ILS to the alfresco network otherwise communication wll not be possible 
docker network connect end-user_default Contain-ILS


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