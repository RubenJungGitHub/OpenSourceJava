# 1. Herstel de JAVA_HOME naar de juiste versie (v21)
$javaPath = "C:\Program Files\Java\jdk-21.0.10" 
$env:JAVA_HOME = $javaPath
$env:Path = "$javaPath\bin;" + $env:Path
Set-Location -Path "C:\ContainOpenSource\Java\OpenSourceJava\ils"
Write-Host "--- JAVA_HOME ingesteld op: $env:JAVA_HOME ---" -ForegroundColor Cyan

# Check of java echt gevonden wordt
if (!(Test-Path "$javaPath\bin\java.exe")) {
    Write-Host "FOUT: Pad $javaPath\bin\java.exe niet gevonden!" -ForegroundColor Red
    Write-Host "Kijk even in C:\Program Files\Java\ hoe de map exact heet." -ForegroundColor Yellow
    Read-Host "Druk op Enter..."
    exit
}

# 2. Kill Java processen (tegen file locks)
Stop-Process -Name "java" -Force -ErrorAction SilentlyContinue

# 3. De 'Nuke' van alle oude troep
Write-Host "--- Caches en oude 'Properies' verwijderen... ---" -ForegroundColor Yellow
Get-ChildItem -Path . -Filter "target" -Recurse | Remove-Item -Force -Recurse -ErrorAction SilentlyContinue
Remove-Item -Path "$HOME\.m2\repository\contain\opensource\shared" -Recurse -Force -ErrorAction SilentlyContinue

# 4. De schone Maven build
cd C:
Write-Host "--- Starten van de schone build met JDK 21... ---" -ForegroundColor Green
& mvn clean install -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "--- BUILD SUCCESS! De 't' is nu officieel overal verwerkt. ---" -ForegroundColor Green
} else {
    Write-Host "--- BUILD FAILED. Check de foutmelding hierboven. ---" -ForegroundColor Red
}

Read-Host "Druk op Enter om af te sluiten..."