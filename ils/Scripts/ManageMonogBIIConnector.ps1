#$sigHash = (Get-Content $Env:HomePath\Downloads\mongodb-bi-win32-x86_64-v2.14.27.msi.sha256 | Out-String).SubString(0,64).ToUpper(); `
#$fileHash = (Get-FileHash $Env:HomePath\Downloads\mongodb-bi-win32-x86_64-v2.14.27.msi).Hash.Trim(); `
#echo $sigHash; echo $fileHash; `
#$sigHash -eq $fileHash


cd "C:\Program Files\MongoDB\Connector for BI\2.14\bin"
#.\mongosqld.exe --mongo-uri "mongodb://localhost:27017/?authSource=admin" --addr 0.0.0.0:3307
#.\mongosqld.exe --mongo-uri "mongodb://localhost:27017/?authSource=admin" --mongo-username "admin" --mongo-password "admin" --addr 0.0.0.0:3307 --auth
# ──────────────── CONFIGURATION ────────────────
# -------------------------------
# ManageMonogBIIConnector.ps1
# -------------------------------
$a = 1;
# 1️⃣ Configurable variables
$mongoExe       = "C:\Program Files\MongoDB\Server\6.0\bin\mongo.exe"  # path to mongo.exe
$mongosqldExe   = "C:\Program Files\MongoDB\Connector for BI\2.14\bin\mongosqld.exe"



$mongoHost      = "localhost"
$mongoPort      = 27017
$authDB         = "admin"
$mongoUser      = "admin"
$mongoPwd       = "admin"
$bindAddr       = "0.0.0.0:3307"
$bindHost = "0.0.0.0"
$bindPort = 3307

$mongosqldArgs = @(
    "--mongo-uri", "mongodb://localhost:$mongoPort/?authSource=$authDB",
    "--mongo-username", $mongoUser,
    "--mongo-password", $mongoPwd,
    "--addr", $bindHost,
    "--port", $bindPort,
    "--sslMode", "requireSSL",
    "--sslPEMKeyFile", $combinedPem,
    "--auth"
)

Start-Process -FilePath $mongosqldExe -ArgumentList $mongosqldArgs -NoNewWindow
Write-Host "mongosqld started with SSL (self-signed) on $bindAddr"

# SSL / PEM paths
$openssl        = "C:\Program Files\OpenSSL-Win64\bin\openssl.exe"
$p12File        = "C:\ContainOpenSource\OpenSourceJava\ils\receiver\src\main\resources\Containselfsigned_cert.p12"
$p12Password    = "changeitsosecure"
$certPem        = "C:\ContainOpenSource\OpenSourceJava\ils\receiver\src\main\resources\cert.pem"
$keyPem         = "C:\ContainOpenSource\OpenSourceJava\ils\receiver\src\main\resources\key.pem"
$combinedPem    = "C:\ContainOpenSource\OpenSourceJava\ils\receiver\src\main\resources\mongosqld.pem"
$mongoExe = "C:\Program Files\MongoDB\Server\8.2\bin\mongod.exe"
$mongoShell = "C:\Shell\Mongo\bin\mongosh.exe"
$mongoDataPath = "C:\MongoData"
# -------------------------------
# 2️⃣ Extract certificate and key
Write-Host "Extracting certificate and key..."
& $openssl pkcs12 -in $p12File -clcerts -nokeys -out $certPem -passin pass:$p12Password
& $openssl pkcs12 -in $p12File -nocerts -nodes -out $keyPem -passin pass:$p12Password

# Combine cert + key
Get-Content $certPem, $keyPem | Set-Content $combinedPem
Write-Host "Combined PEM created at: $combinedPem"

# -------------------------------
# 3️⃣ Install certificate to Trusted Root
Write-Host "Installing certificate in Trusted Root..."
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($combinedPem)
$store = New-Object System.Security.Cryptography.X509Certificates.X509Store("Root","LocalMachine")
$store.Open("ReadWrite")
$store.Add($cert)
$store.Close()
Write-Host "Certificate installed successfully."


# -------------------------------
# 3️⃣ Start MongoDB temporarily without auth to create admin user
# -------------------------------
Write-Host "`nStarting MongoDB without authentication..."
$mongoProcess = Start-Process -FilePath $mongoExe -ArgumentList "--dbpath `"$mongoDataPath`" --port $mongoPort --bind_ip 127.0.0.1 --quiet" -PassThru
Start-Sleep -Seconds 5  # wait for MongoDB to start

Write-Host "Creating admin user..."

& $mongoShell "mongodb://127.0.0.1:$mongoPort/$authDB" --eval @"
use $authDB;
if (db.getUser('$mongoUser') == null) {
    db.createUser({
        user: '$mongoUser',
        pwd: '$mongoPwd',
        roles: [{role: 'root', db: '$authDB'}],
        mechanisms: ['SCRAM-SHA-1']
    });
    print('✅ Admin user created');
} else {
    print('Admin user already exists');
}
"@

# Stop temporary MongoDB
Write-Host "Stopping temporary MongoDB..."
Stop-Process -Id $mongoProcess.Id -Force
Start-Sleep -Seconds 5


# Set the auth source as an environment variable
$env:MONGO_AUTH_SOURCE = "admin"

$mongosqldExe = "C:\Program Files\MongoDB\Connector for BI\2.14\bin\mongosqld.exe"
$configFile   = "C:\ContainOpenSource\Java\OpenSourceJava\ils\receiver\src\main\resources\mongosqld.conf"

Write-Host "Starting mongosqld with Professional Reporting User..."

& "C:\Program Files\MongoDB\Connector for BI\2.14\bin\mongosqld.exe" --config "C:\ContainOpenSource\Java\OpenSourceJava\ils\receiver\src\main\resources\mongosqld.conf" --sampleNamespaces "ilstools.tbl_iolog" --sampleSize 1000

& "$mongosqldExe" --config "$configFile" `
  --mongo-username "reportUser" `
  --mongo-password "password123" `
  --mongo-authenticationSource "admin" `
  --mongo-authenticationMechanism "SCRAM-SHA-256"

# Check log for success
Start-Sleep -Seconds 2
Get-Content "C:\Users\NL07428\mongosqld.log" -Tail 5

Start-Sleep -Seconds 3
netstat -ano | findstr 3307

Write-Host "`n✅ mongosqld started successfully on port ${bindAddr}"
Write-Host "You can now connect Power BI using MySQL ODBC 9.6 driver."

Test-NetConnection -ComputerName 127.0.0.1 -Port 27017

C:\Shell\Mongo\bin\mongosh.exe "mongodb://admin:admin@127.0.0.1:27017/ilstools?authSource=admin&authMechanism=SCRAM-SHA-256&directConnection=true"

C:\Shell\Mongo\bin\mongosh.exe "mongodb://admin:admin@127.0.0.1:27017/ilstools?authSource=admin&authMechanism=SCRAM-SHA-1&directConnection=true"

C:\Shell\Mongo\bin\mongosh.exe "mongodb://127.0.0.1:27017/?directConnection=true"

C:\Shell\Mongo\bin\mongosh.exe "mongodb://admin:admin@127.0.0.1:27017/ilstools?authSource=admin&authMechanism=SCRAM-SHA-256&directConnection=true"

C:\Shell\Mongo\bin\mongosh.exe "mongodb://admin:admin@host.docker.internal:27017/ilstools?authSource=admin&authMechanism=SCRAM-SHA-256"

netstat -ano | findstr 3003
netstat -ano | findstr 27017

taskkill /F /PID 26676