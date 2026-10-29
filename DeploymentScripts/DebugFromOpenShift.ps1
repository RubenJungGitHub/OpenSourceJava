$token  = 'sha256~bz1St_C8qKLTT5kT9zanScjL_bsEgre9vL-8PhnWSg4'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
cls

# 1. Kill oude processen op zowel poort 5005 (receiver) als poort 5006 (poller)
foreach ($port in @(5005, 5006)) {
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        $pidVal = $connection.OwningProcess
        $ProcessName = Get-Process -Id $pidVal -ErrorAction SilentlyContinue | Select-Object -ExpandProperty ProcessName
        Write-Host "Process $ProcessName with PID $pidVal is using port $port. Stopping it..." -ForegroundColor Yellow
        Stop-Process -Id $pidVal -Force -ErrorAction SilentlyContinue
    } else {
        Write-Host "No active process found on port $port." -ForegroundColor Cyan
    }
}

# 2. Zoek de actieve RUNNING receiver pod en zet port-forward op (poort 5005 -> 5005)
$receiverPod = (oc get pods -n $project --no-headers | Where-Object { $_ -like "receiver-*" -and $_ -like "*Running*" } | Select-Object -First 1).Split()[0]

if ($receiverPod) {
    Write-Host "Found active receiver pod: $receiverPod" -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$receiverPod 5005:5005 -n $project"
} else {
    Write-Host "Error: No running receiver pod found!" -ForegroundColor Red
}

# 3. Zoek de actieve RUNNING alfrescoqueuepoller pod en zet port-forward op (lokale poort 5006 -> pod poort 5005)
$pollerPod = (oc get pods -n $project --no-headers | Where-Object { $_ -like "alfrescoqueuepoller-*" -and $_ -like "*Running*" } | Select-Object -First 1).Split()[0]

if ($pollerPod) {
    Write-Host "Found active poller pod: $pollerPod" -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$pollerPod 5006:5005 -n $project"
} else {
    Write-Host "Error: No running alfrescoqueuepoller pod found!" -ForegroundColor Red
}