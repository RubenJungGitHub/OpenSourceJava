cls
$uri = "https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco/s/atos/apply-rule"

# Replace with your actual admin username and password
$username = "admin"
$password = "admin"

# Convert credentials to a Basic Auth header
$pair = "${username}:${password}"
$encodedCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes($pair))
$headers = @{
    "Authorization" = "Basic $encodedCreds"
    "Content-Type"  = "application/json; charset=utf-8"
}

# 1. Define your payload as a hashtable
$payloadHash = @{
    nodeId   = "24f15e43-8d74-4963-8acb-3d794a14d82a"
    typeName = "contain:containdocument"
}

# 2. Convert to JSON and force UTF-8 byte encoding (bypasses Windows PowerShell UTF-16/BOM issues)
$jsonString = $payloadHash | ConvertTo-Json
$body = [System.Text.Encoding]::UTF8.GetBytes($jsonString)

# Send the request
try {
    $response = Invoke-RestMethod -Uri $uri -Method Post -Headers $headers -Body $body -Proxy $null
    $response | ConvertTo-Json
} catch {
    Write-Error "Request failed: $_"
}