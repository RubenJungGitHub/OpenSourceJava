    $token  = 'sha256~UaQBCubCPbHDkc3D6AO6C1H3awjEpOt-hLeqRcwlVJw'

#------------------------------------------------------------------
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$namespace = "contain-dvl"

cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server
    

if (-not $alfrescoPod) {
# Fallback using standard line splitting if regex jsonpath acts up in your shell version
$alfrescoPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco" } | Select-Object -First 1) -replace '\s+.*$', ''
}

# ==========================================
# ROBUST FILE VERIFICATION & ASSERTION
# ==========================================
Write-Host "--- Verifying Files Exist Inside OpenShift Pod ---" -ForegroundColor Cyan

# 1. Check for the Constraint JAR in /WEB-INF/lib/
$jarCheck = oc exec -n $namespace $alfrescoPod -- sh -c "ls /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/ 2>/dev/null | grep -E 'alfrescointegration|taxonomy|contain'"
if (-not $jarCheck) {
    Write-Host "[ERROR] Constraint JAR was NOT found in the pod's lib directory!" -ForegroundColor Red
    throw "Deployment verification failed: Missing required JAR file."
} else {
    Write-Host "[SUCCESS] Found target JAR: $jarCheck" -ForegroundColor Green
}

# 2. Check for the Share Config XML in web-extension/
$shareXmlCheck = oc exec -n $namespace $alfrescoPod -- sh -c "[ -f /usr/local/tomcat/shared/classes/alfresco/web-extension/share-config-custom.xml ] && echo 'EXISTS' || echo 'MISSING'"
if ($shareXmlCheck -notmatch "EXISTS") {
    Write-Host "[ERROR] share-config-custom.xml was NOT found in the Share web-extension directory!" -ForegroundColor Red
    throw "Deployment verification failed: Missing Share configuration XML."
} else {
    Write-Host "[SUCCESS] Share configuration XML verified in place." -ForegroundColor Green
}