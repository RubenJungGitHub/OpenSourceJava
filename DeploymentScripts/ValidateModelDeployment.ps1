

# ==========================================
# 1. CONFIGURATION
# ==========================================

$token       = 'sha256~8UJKuUrXdrjrp-H83pYMD08SlUhkBf5QX_o9hJXm-ug'
$server      = 'https://api.ocp0.anders-nogic.atos.net:6443'
$Namespace = "contain-dvl"
$ProjectRoot = "D:\contAIn\alfrescotaxintegration"
$token       = 'sha256~8UJKuUrXdrjrp-H83pYMD08SlUhkBf5QX_o9hJXm-ug'
$server      = 'https://api.ocp0.anders-nogic.atos.net:6443'
# ==========================================
# STAGE 0: Login to OpenShift & Setup Secret
# ==========================================
cls
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server --insecure-skip-tls-verify=true

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " ALFRESCO MODEL DEPLOYMENT VALIDATOR" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Local Artifact Validation
$jarPath = "$ProjectRoot\contain-alfresco\contain-alfresco-platform\target\contain-alfresco-platform-1.0-SNAPSHOT.jar"
Write-Host "`n[1/4] Checking local compiled JAR..." -ForegroundColor Yellow
if (Test-Path $jarPath) {
    Write-Host "[OK] Local JAR found at: $jarPath" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Local JAR missing! Run Maven build first." -ForegroundColor Red
    exit 1
}

# 2. Inspect JAR contents using .NET Zip Archive (No external tools needed)
Write-Host "`n[2/4] Inspecting internal JAR file structure..." -ForegroundColor Yellow
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jarPath)
$entries = $zip.Entries | Select-Object -ExpandProperty FullName

$hasModel = $entries | Where-Object { $_ -match "model/.*\.xml" }
$hasContext = $entries | Where-Object { $_ -match "context/.*\.xml" }
$hasModuleProp = $entries | Where-Object { $_ -match "module\.properties" }

if ($hasModel) { Write-Host "  [OK] Model XML found: $($hasModel -join ', ')" -ForegroundColor Green }
else { Write-Host "  [WARNING] No model XML files found matching expected path." -ForegroundColor Red }

if ($hasContext) { Write-Host "  [OK] Context XML found: $($hasContext -join ', ')" -ForegroundColor Green }
else { Write-Host "  [WARNING] No context XML files found." -ForegroundColor Red }

if ($hasModuleProp) { Write-Host "  [OK] module.properties found." -ForegroundColor Green }
else { Write-Host "  [WARNING] module.properties missing!" -ForegroundColor Red }

$zip.Dispose()

# 3. OpenShift Pod & Container Verification
Write-Host "`n[3/4] Checking OpenShift container environment..." -ForegroundColor Yellow
$podName = (oc get pods -n $Namespace --no-headers -o custom-columns=":metadata.name" | Where-Object { $_ -match "^alfresco-" } | Select-Object -First 1).Trim()

if (-not $podName) {
    Write-Host "[ERROR] No active Alfresco pod found in namespace $Namespace." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Found active Alfresco Pod: $podName" -ForegroundColor Green

# Check if JAR exists in pod
$jarInPod = oc exec -n $Namespace $podName -- sh -c "ls -l /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/contain-alfresco-platform-1.0-SNAPSHOT.jar" 2>$null
if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK] Custom JAR is present inside the container webapp WEB-INF/lib/" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Custom JAR NOT found inside the running container!" -ForegroundColor Red
}

# 4. Log Inspection for Model Loading
Write-Host "`n[4/4] Scanning container logs for model bootstrap errors or success..." -ForegroundColor Yellow
$logs = oc logs $podName -n $Namespace --tail=400
$matchingLogs = $logs | Where-Object { $_ -match "dictionary|model|bootstrap|containdocumentmodel" }

if ($matchingLogs) {
    Write-Host "Found relevant log entries:" -ForegroundColor Cyan
    $matchingLogs | ForEach-Object { Write-Host "  $_" -ForegroundColor DarkGray }
} else {
    Write-Host "[WARNING] No dictionary or model bootstrap lines found in the last 400 log lines." -ForegroundColor Yellow
}


oc exec -n $namespace dc/alfresco -- find /usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/extension -type f
oc exec -n $namespace dc/alfresco -- cat /usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/extension/context/bootstrap-context.xml

Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host " VALIDATION COMPLETE" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan