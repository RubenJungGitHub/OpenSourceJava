# ===============================
# PowerShell: Setup MongoDB BI Connector DSN
# ===============================

# === 1️⃣ Variables ===
$mongosqldHost = "127.0.0.1"
$mongosqldPort = 3307
$mongosqldUser = "admin"
$mongosqldPassword = "changeitsosecure"
$dsnName = "MongoBI_Connector"
$dsnDescription = "DSN for MongoDB BI Connector"
$odbcDriverName = "MySQL ODBC 9.6 Unicode Driver"

# === 2️⃣ Create a mongosqld user with mysql_native_password ===
# Connect using the mysql client bundled with mongosqld
#$mysqlExe = "C:\Program Files\MongoDB\Connector for BI\2.14\bin\mysql.exe"
$mysqlExe = "C:\ProgramData\chocolatey\bin\mysql.exe"

$createUserSQL = @"
CREATE USER IF NOT EXISTS '$mongosqldUser'@'%' IDENTIFIED WITH mysql_native_password BY '$mongosqldPassword';
GRANT ALL PRIVILEGES ON *.* TO '$mongosqldUser'@'%';
FLUSH PRIVILEGES;
"@

Write-Host "Creating mongosqld user with mysql_native_password..."
& $mysqlExe --host=$mongosqldHost --port=$mongosqldPort --user=$mongosqldUser --password=$mongosqldPassword --execute=$createUserSQL

# === 3️⃣ Create 64-bit ODBC DSN ===
# Uses registry keys for System DSN (64-bit)
$regPath = "HKLM:\SOFTWARE\ODBC\ODBC.INI\$dsnName"
$regPathDrivers = "HKLM:\SOFTWARE\ODBC\ODBCINST.INI\$odbcDriverName"

# Check if driver exists
if (-not (Test-Path $regPathDrivers)) {
    Write-Error "ODBC driver '$odbcDriverName' not found. Make sure 64-bit driver is installed."
    exit
}

# Create DSN registry keys
Write-Host "Creating ODBC System DSN: $dsnName..."
New-Item -Path $regPath -Force | Out-Null
Set-ItemProperty -Path $regPath -Name "Driver" -Value $regPathDrivers
Set-ItemProperty -Path $regPath -Name "Server" -Value $mongosqldHost
Set-ItemProperty -Path $regPath -Name "Port" -Value $mongosqldPort
Set-ItemProperty -Path $regPath -Name "User" -Value $mongosqldUser
Set-ItemProperty -Path $regPath -Name "Password" -Value $mongosqldPassword
Set-ItemProperty -Path $regPath -Name "Database" -Value ""
Set-ItemProperty -Path $regPath -Name "OPTION" -Value 67108864 # 0x04000000 = SSL required
Set-ItemProperty -Path $regPath -Name "SSLMode" -Value "REQUIRED"
Set-ItemProperty -Path $regPath -Name "TrustServerCertificate" -Value "Yes"
Set-ItemProperty -Path $regPath -Name "Description" -Value $dsnDescription

# Add DSN name to ODBC.INI list
$dsnListPath = "HKLM:\SOFTWARE\ODBC\ODBC.INI\ODBC Data Sources"
Set-ItemProperty -Path $dsnListPath -Name $dsnName -Value $odbcDriverName

Write-Host "✅ DSN '$dsnName' created successfully!"
Write-Host "You can now select this DSN in Power BI (ODBC) to connect to mongosqld."