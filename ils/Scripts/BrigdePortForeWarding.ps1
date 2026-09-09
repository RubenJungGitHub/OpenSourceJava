Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/mongodb 27017:27017 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/taxonomyservice 53000:53000 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/notificationservice 5001:5001 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/kie-server 8080:8080 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward service/postgres 5432:5432 -n contain-dvl"


#Optinonal open Java debug port from Open Shift (For receiver in this case)
# 1. Kill any existing process using port 5005
Get-NetTCPConnection -LocalPort 5005 -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
    Write-Host "Cleared old process on port 5005."
}

# 2. Find the active receiver pod dynamically
$podName = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -like "receiver-*" } | Select-Object -First 1).Split()[0]

if ($podName) {
    Write-Host "Found active receiver pod: $podName" -ForegroundColor Green
    # 3. Open port-forward in a new window
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward pod/$podName 5005:5005 -n contain-dvl"
} else {
    Write-Host "Error: No receiver pod found!" -ForegroundColor Red
}