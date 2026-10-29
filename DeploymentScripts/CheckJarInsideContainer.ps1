cls
$token = 'sha256~gelGcfoDLvgr7uPUB_rZRAxeCcJZEMvVxfX81qAPIco'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

oc login --token=$token --server=$server
oc project $project

$pod = "alfrescoqueuepoller-1-qh9rb"
$localPath = "D:\temp\alfrescoqueuepoller.jar"

# Zorg dat de doelmap bestaat
if (!(Test-Path "D:\temp")) {
    New-Item -ItemType Directory -Path "D:\temp" | Out-Null
}

Write-Host "Bezig met ophalen en converteren van de JAR vanuit de pod..." -ForegroundColor Yellow

# 1. Haal de jar op uit de pod als een base64 string via oc exec
$base64String = oc exec $pod -- sh -c "base64 /app/alfrescoqueuepoller.jar"

# 2. Converteer de base64 string lokaal direct terug naar een echt .jar bestand
if ($base64String) {
    # Opschonen van eventuele extra witregels of carriage returns
    $cleanBase64 = [System.String]::Join("", $base64String)
    $bytes = [System.Convert]::FromBase64String($cleanBase64)
    [System.IO.File]::WriteAllBytes($localPath, $bytes)
    
    if (Test-Path $localPath) {
        Write-Host "Klaar! De JAR is succesvol binnengehaald op: $localPath" -ForegroundColor Green
    } else {
        Write-Host "Fout bij het wegschrijven van het lokale bestand." -ForegroundColor Red
    }
} else {
    Write-Host "Kon geen data ophalen uit de pod." -ForegroundColor Red
}