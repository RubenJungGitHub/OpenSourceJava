


# --- 1. CONFIGURATION ---
$projectRoot = "D:\contAIn\alfrescotaxintegration"
$namespace = "contain-dvl" # Update with your OpenShift namespace if needed
$constraintJarPath = "$projectRoot\contain-alfresco\contain-alfresco-platform\target\"
$ModelPath = "$projectRoot\contain-alfresco\contain-alfresco-share\src\main\resources\alfresco\web-extension"

# Internal Tomcat Paths (both live inside the same container now)
$targetLibPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/lib/"
$shareExtPath  = "/usr/local/tomcat/shared/classes/alfresco/web-extension/"

$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"
$token  = 'sha256~UaQBCubCPbHDkc3D6AO6C1H3awjEpOt-hLeqRcwlVJw'

#------------------------------------------------------------------
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server


function Test-AlfrescoReady {
    param($podName, $ns, $maxRetries = 60)
    Write-Host "Analyzing " $podName " conditions for health..." -ForegroundColor Cyan
    # Dynamically find the running Alfresco pod name as a strict single string
    $count = 0
    while ($count -lt $maxRetries) {
        # Fetch full JSON and let PowerShell handle the property lookup cleanly
        $podsJson = oc get pod $podName -n contain-dvl -o json 2>$null | ConvertFrom-Json
        $conditions = @($podsJson.status.conditions)

        # Filter for the Ready condition status safely
        $readyStatus = ($conditions | Where-Object { $_.type -eq "Ready" }).status

        Write-Host "Ready Status is: '$readyStatus'" -ForegroundColor Yellow
        
        if ($readyStatus -eq "True") {
            Write-Host "Pod $podName is fully READY and healthy according to OpenShift!" -ForegroundColor Green
            return $true
        }
        
        $count++
        Write-Host "Pod still initializing... Attempt $count of $maxRetries" -ForegroundColor DarkGray
        Start-Sleep -Seconds 5
    }
    return $false
}



Set-Location $projectRoot

Write-Host "--- STAGE 1: Resolving Alfresco Pod ---" -ForegroundColor Cyan

# Dynamically find the active pod whose name starts with "alfresco"


if (-not $alfrescoPod) {
    # Fallback using standard line splitting if regex jsonpath acts up in your shell version
    $alfrescoPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco" } | Select-Object -First 1) -replace '\s+.*$', ''
}

#Write-Host "Found Alfresco Pod: $alfrescoPod" -ForegroundColor Green

if (-not $alfrescoPod) {
    Write-Host "[ERROR] Could not find an active pod starting with 'alfresco' in namespace '$namespace'." -ForegroundColor Red
    exit
}



if (!(Test-AlfrescoReady $alfrescoPod $namespace))
{
  Write-Host "[ERROR] 'alfresco' pod is not healthy in namespace '$namespace'." -ForegroundColor Red
    exit

}

# ==========================================
# STAGE 2: INJECTION LOGIC (JAR)
# ==========================================
Write-Host "--- STAGE 2: Injecting Constraint JAR & Share Config ---" -ForegroundColor Yellow

# A. Remove old JAR version first
Write-Host "Removing old JAR version..." -ForegroundColor DarkGray
oc exec $alfrescoPod -n $namespace -- rm -f "${targetLibPath}contain-alfresco-platform-1.0-SNAPSHOT.jar"

# Validate that it is actually gone
Write-Host "Validating old JAR removal..." -ForegroundColor Cyan
$checkOld = oc exec $alfrescoPod -n $namespace -- sh -c "if [ -f '${targetLibPath}contain-alfresco-platform-1.0-SNAPSHOT.jar' ]; then echo 'EXISTS'; else echo 'REMOVED'; fi"

if ($checkOld.Trim() -eq "EXISTS") {
    Write-Host "[WARNING] Old JAR file still exists in the container!" -ForegroundColor Yellow
} else {
    Write-Host "Old JAR successfully removed." -ForegroundColor Green
}


$jarFile = Get-ChildItem -Path $constraintJarPath -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1

if ($jarFile) {
    Write-Host "Deploying Platform JAR: $($jarFile.Name)" -ForegroundColor Green
    
    # Stream local file via native Windows tar directly into the container's target directory
    tar.exe -cf - -C "$($jarFile.DirectoryName)" "$($jarFile.Name)" | oc exec -i -n $namespace $alfrescoPod -- tar xf - -C "$targetLibPath"
    
    $repoDest = "$targetLibPath$($jarFile.Name)"
    
    # Verify it landed safely
    Write-Host "Verifying JAR in repository lib..." -ForegroundColor Cyan
    $verifyCheck = oc exec $alfrescoPod -n $namespace -- sh -c "if [ -f '$repoDest' ]; then echo 'EXISTS'; else echo 'MISSING'; fi"

    if ($verifyCheck.Trim() -eq "EXISTS") {
        Write-Host "Platform JAR successfully verified at: $repoDest" -ForegroundColor Green
    } else {
        Write-Host "[ERROR] Platform JAR verification failed! The file is missing from the container." -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[ERROR] Platform JAR not found. Did you run 'mvn clean install'?" -ForegroundColor Red
    exit
}

# ==========================================
# STAGE 3: Injecting and Validating Share Config
# ==========================================
Write-Host "--- STAGE 3: Injecting and Validating Share Config ---" -ForegroundColor Yellow

$localSharePath = "$ModelPath\share-config-custom.xml"
$shareFileObj = Get-Item $localSharePath
$remoteShareFile = "$shareExtPath$($shareFileObj.Name)"

# A. Ensure the target directory exists inside the container first
Write-Host "Ensuring remote web-extension directory exists..." -ForegroundColor DarkGray
oc exec $alfrescoPod -n $namespace -- mkdir -p "$shareExtPath"

# B. Remove old Share config version if present
Write-Host "Removing old Share config..." -ForegroundColor DarkGray
oc exec $alfrescoPod -n $namespace -- rm -f "$remoteShareFile"

# Validate that the old config is gone
Write-Host "Validating old Share config removal..." -ForegroundColor Cyan
$checkOldShare = oc exec $alfrescoPod -n $namespace -- sh -c "if [ -f '$remoteShareFile' ]; then echo 'EXISTS'; else echo 'REMOVED'; fi"

if ($checkOldShare.Trim() -eq "EXISTS") {
    Write-Host "[WARNING] Old Share config file still exists in the container!" -ForegroundColor Yellow
} else {
    Write-Host "Old Share config successfully removed." -ForegroundColor Green
}

# C. Copy the new Share config using tar
Write-Host "Copying new Share config via tar stream..." -ForegroundColor DarkGray
tar.exe -cf - -C "$($shareFileObj.DirectoryName)" "$($shareFileObj.Name)" | oc exec -i -n $namespace $alfrescoPod -- tar xf - -C "$shareExtPath"

# D. Validate that the new Share config landed safely
Write-Host "Verifying Share config in container..." -ForegroundColor Cyan
$verifyShare = oc exec $alfrescoPod -n $namespace -- sh -c "if [ -f '$remoteShareFile' ]; then echo 'EXISTS'; else echo 'MISSING'; fi"

if ($verifyShare.Trim() -eq "EXISTS") {
    Write-Host "Share config successfully verified at: $remoteShareFile" -ForegroundColor Green
} else {
    Write-Host "[ERROR] Share config verification failed! The file is missing from the container." -ForegroundColor Red
    exit 1
}

# E. Scrub Windows Junk (BOM and CRLF) & Set Permissions
Write-Host "Cleaning encoding and setting permissions..." -ForegroundColor DarkGray
oc exec $alfrescoPod -n $namespace -- sed -i '1s/^\xEF\xBB\xBF//' "$remoteShareFile"
oc exec $alfrescoPod -n $namespace -- sed -i 's/\r$//' "$remoteShareFile"
oc exec $alfrescoPod -n $namespace -- chmod 666 "$remoteShareFile"

Write-Host "Share configuration deployment complete!" -ForegroundColor Green


# ==========================================
# STAGE 4: ALFRESCO HEALTH CHECK
# ==========================================
function Wait-For-Alfresco-OpenShift {
    param($maxRetries = 60)
    Write-Host "Checking Alfresco Health via port-forward..." -ForegroundColor Cyan

    # Re-fetch the new pod name after the rollout restart
    $currentPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco" } | Select-Object -First 1) -replace '\s+.*$', ''

    # Open a temporary background port-forward for the readiness probe
    $pfJob = Start-Job -ScriptBlock {
        param($ns, $pod)
        oc port-forward pod/$pod 8080:8080 -n $ns
    } -ArgumentList $namespace, $currentPod

    Start-Sleep -Seconds 3 # Give port-forward a moment to bind

    $count = 0
    $isOnline = $false
    while ($count -lt $maxRetries) {
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-" -UseBasicParsing -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                Write-Host "ALFRESCO IS ONLINE!" -ForegroundColor Green
                $isOnline = $true
                break
            }
        } catch {
            # Still warming up...
        }
        $count++
        Write-Host "Waiting for Alfresco... Attempt $count of $maxRetries" -ForegroundColor DarkGray
        Start-Sleep -Seconds 5
    }

    # Clean up the background port-forward job
    Stop-Job $pfJob -ErrorAction SilentlyContinue
    Remove-Job $pfJob -ErrorAction SilentlyContinue

    return $isOnline
}

$healthCheckPassed = Wait-For-Alfresco-OpenShift
if (-not $healthCheckPassed) {
    Write-Host "[WARNING] Alfresco did not report healthy within the timeout window." -ForegroundColor Yellow
} else {
    Write-Host "--- DEPLOYMENT AND HEALTH CHECK COMPLETED SUCCESSFULLY ---" -ForegroundColor Green
}