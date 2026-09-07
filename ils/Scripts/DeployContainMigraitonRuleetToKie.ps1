#------------------------------------------------------------------
$token  = 'sha256~SKBL2bQtpJijeFPX1hOr-1Ndwaw5PXOZYoXmtiJ7V64'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server

$pair = "admin:admin"
$bytes = [System.Text.Encoding]::ASCII.GetBytes($pair)
$base64 = [System.Convert]::ToBase64String($bytes)
$headers = @{
    "Authorization" = "Basic $base64"
    "Content-Type" = "application/json"
    "Accept" = "application/json"
}

# Explicitly structure the release-id fields to prevent null mapping
$body = @{
    "release-id" = @{
        "group-id" = "com.myspace"
        "artifact-id" = "contAlnMigrationRuleset"
        "version" = "1.0.0-SNAPSHOT"
    }
} | ConvertTo-Json -Depth 3

Invoke-RestMethod -Uri "http://localhost:8080/kie-server/services/rest/server/containers/contAlnMigrationRuleset" -Headers $headers -Method PUT -Body $body