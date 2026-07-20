$PORT = 56000
Get-NetTCPConnection -LocalPort $PORT | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }


netstat -ano | findstr :56000