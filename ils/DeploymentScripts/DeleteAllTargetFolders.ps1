
cd D:\contAIn\OpenSourceJava\ils\
# Zoek en verwijder alle 'target' mappen recursief
Get-ChildItem -Path . -Recurse -Directory -Filter "target" | ForEach-Object {
    Write-Host "Verwijderen: $($_.FullName)" -ForegroundColor Yellow
    Remove-Item -Path $_.FullName -Recurse -Force
}
Write-Host "Klaar! Alle target mappen zijn opgeschoond." -ForegroundColor Green