<#
.SYNOPSIS
    Resets/Clears corrupted Alfresco user preferences for the admin account.
#>

# Bypass self-signed/enterprise SSL certificate checks
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

# Force modern TLS security protocols required by the OpenShift route
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12, [Net.SecurityProtocolType]::Tls13

$uri = "https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco/s/api/people/admin/preferences"

Write-Host "Connecting to Alfresco to reset admin preferences..." -ForegroundColor Cyan

# Prompt for credentials securely once
$credential = Get-Credential

try {
    # Send DELETE request with -Proxy $null to bypass local network proxy blocks
    Invoke-RestMethod -Uri $uri -Method Delete -Credential $credential -Proxy $null -ErrorAction Stop
    Write-Host "Success! Admin preferences have been reset. Refresh your Share UI." -ForegroundColor Green
}
catch {
    Write-Warning "An error occurred while resetting preferences:"
    Write-Error $_.Exception.Message
}



#Alternitavely enter pod : THIS WORKS!
# Find your running alfresco pod name if it differs from 'alfresco'
cls
#oc get pods -n contain-dvl
$PodName = "Alfresco"
oc get pods -l io.kompose.service=$PodName
$pod = (oc get pods --no-headers | Where-Object { $_ -match $podname -and $_ -notmatch "deploy|build" } | Select-Object -First 1).Split()[0]

cls
Write-Host "Detected $pod..."

# Execute the curl delete directly inside the container
oc exec -it $pod -n contain-dvl -- curl -X DELETE -u admin:admin "http://localhost:8080/alfresco/s/api/people/admin/preferences"