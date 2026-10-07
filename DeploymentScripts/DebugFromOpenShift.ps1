$token  = 'sha256~j9Fb7romHI4YbZukHMv7tgWpDBLfOxKjCjItdLxuPf4'
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



#Optinonal open Java debug port from Open Shift (For receiver in this case)
# 1. Kill any existing process using port 5005
Get-NetTCPConnection -LocalPort 5005 -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Cleared old process on port 5005."
}


# 2. Find the active RUNNING receiver pod dynamically
$podName = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -like "receiver-*" -and $_ -like "*Running*" } | Select-Object -First 1).Split()[0]

Write-Host "Pod found " $podName
if ($podName) {
    Write-Host "Found active receiver pod: $podName" -ForegroundColor Green
    # 3. Open port-forward in a new window
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$podName 5005:5005 -n contain-dvl"
} else {
    Write-Host "Error: No running receiver pod found!" -ForegroundColor Red
}
