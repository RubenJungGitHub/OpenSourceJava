$port = 53000
$connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
if ($connection) {
    $pidVal = $connection.OwningProcess
    $ProcessName = Get-Process -Id $pidVal | Select-Object Id, ProcessName, Path
    Write-Host "Process $processName with PID $pidVal is using port $port. Stopping it..." -ForegroundColor Yellow
    Stop-Process -Id $pidVal -Force
} else {
    Write-Host "No active process found directly on port $port via Get-NetTCPConnection." -ForegroundColor Cyan
}