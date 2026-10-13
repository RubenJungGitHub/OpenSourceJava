$token  = 'sha256~YwUAqyYdFpSChkL2KD65RkNZsUJICPauuLUKZOTYnFM'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
cls


function EnablePortFirst{
    param($port)
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        $pidVal = $connection.OwningProcess
        $ProcessName = Get-Process -Id $pidVal | Select-Object Id, ProcessName, Path
        Write-Host "Process $processName with PID $pidVal is using port $port. Stopping it..." -ForegroundColor Yellow
        Stop-Process -Id $pidVal -Force
    } else {
        Write-Host "No active process found directly on port $port via Get-NetTCPConnection." -ForegroundColor Cyan
    }
}


# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server
$port = 27017
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/mongodb 27017:27017 -n contain-dvl"

$port = 53000
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/taxonomyservice 53000:53000 -n contain-dvl"

$port = 55500
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/taxonomyservice 55000:55000 -n contain-dvl"



$port = 5001
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/notificationservice 5001:5001 -n contain-dvl"

$port = 8080
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/kie-server 8080:8080 -n contain-dvl"

$port = 5432
EnablePortFirst $port
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward service/postgres 5432:5432 -n contain-dvl"


# Use 'start' to open these in separate windows
Start-Process "C:\ContainOpenSource\NGrok\GrokonLocalHost-StartAll.cmd"





#Optinonal open Java debug port from Open Shift (For receiver in this case)
# 1. Kill any existing process using port 5005
Get-NetTCPConnection -LocalPort 5005 -ErrorAction SilentlyContinue | ForEach-Object {
 #   Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Cleared old process on port 5005."
}


# 2. Find the active RUNNING receiver pod dynamically
$podName = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -like "receiver-*" -and $_ -like "*Running*" } | Select-Object -First 1).Split()[0]

Write-Host "Pod found " $podName
if ($podName) {
    Write-Host "Found active receiver pod: $podName" -ForegroundColor Green
    # 3. Open port-forward in a new window
#    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$podName 5005:5005 -n contain-dvl"
} else {
    Write-Host "Error: No running receiver pod found!" -ForegroundColor Red
}
