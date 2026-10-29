PS C:\Users\nl07428>
PS C:\Users\nl07428> # Kill alle actieve Docker-processen
PS C:\Users\nl07428> Get-Process "*docker*" -ErrorAction SilentlyContinue | Stop-Process -Force
PS C:\Users\nl07428> Get-Process "*com.docker*" -ErrorAction SilentlyContinue | Stop-Process -Force
PS C:\Users\nl07428>