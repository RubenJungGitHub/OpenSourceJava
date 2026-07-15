$PORT = 9090
Get-NetTCPConnection -LocalPort $PORT | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }