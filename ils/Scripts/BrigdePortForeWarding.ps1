# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
$token  = 'sha256~QvOTf_iwnkaWH50ZBLXvlYRRo0QXj9RwQCvba35eaCc'

$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server



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


# Define a collection of port-forwarding rules: Service/Resource, LocalPort, RemotePort, Namespace
$portForwards = @(
    @{ Resource = "svc/mongodb";             LocalPort = 27017; RemotePort = 27017; Namespace = "contain-dvl" },
    @{ Resource = "svc/taxonomyservice";     LocalPort = 53000; RemotePort = 53000; Namespace = "contain-dvl" },
    @{ Resource = "svc/notificationservice"; LocalPort = 9200;  RemotePort = 9200;  Namespace = "contain-dvl" }, # Elastic
    @{ Resource = "svc/notificationservice"; LocalPort = 5001;  RemotePort = 5001;  Namespace = "contain-dvl" },
    @{ Resource = "svc/kie-server";          LocalPort = 8080;  RemotePort = 8080;  Namespace = "contain-dvl" },
    @{ Resource = "service/postgres";        LocalPort = 5432;  RemotePort = 5432;  Namespace = "contain-dvl" }
)

# Loop through each item, enable the port, and start the port-forward process
foreach ($pf in $portForwards) {
    EnablePortFirst $pf.LocalPort
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward $($pf.Resource) $($pf.LocalPort):$($pf.RemotePort) -n $($pf.Namespace)"
}

# Use 'start' to open these in separate windows
Start-Process "C:\ContainOpenSource\NGrok\GrokonLocalHost-StartAll.cmd"


# Optional: Open Java debug port from OpenShift (For receiver in this case)
# 1. Check/Clear process using port 5005
Get-NetTCPConnection -LocalPort 5005 -ErrorAction SilentlyContinue | ForEach-Object {
    # Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Cleared old process on port 5005."
}

# 2. Find the active RUNNING receiver pod dynamically
$podName = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -like "receiver-*" -and $_ -like "*Running*" } | Select-Object -First 1).Split()[0]

Write-Host "Pod found: $podName"
if ($podName) {
    Write-Host "Found active receiver pod: $podName" -ForegroundColor Green
    # 3. Open port-forward in a new window (uncomment below line if you want to activate it)
    # Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$podName 5005:5005 -n contain-dvl"
} else {
    Write-Host "Error: No running receiver pod found!" -ForegroundColor Red
}