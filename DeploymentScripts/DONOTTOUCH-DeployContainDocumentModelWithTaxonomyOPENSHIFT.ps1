cls


function Test-AlfrescoReady {
    param($podName, $ns, $maxRetries = 60)
    Write-Host "Analyzing " $podName " conditions for health..." -ForegroundColor Cyan
    # Dynamically find the running Alfresco pod name as a strict single string
    $count = 0
    while ($count -lt $maxRetries) {
        # Fetch full JSON and let PowerShell handle the property lookup cleanly
        $podsJson = oc get pod alfresco-1-l95kk -n contain-dvl -o json 2>$null | ConvertFrom-Json
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



# --- 1. CONFIGURATION ---
$projectRoot = "D:\contAIn\taxonomyservice"
$namespace = "contain-dvl" # Update with your OpenShift namespace if needed
$constraintJarPath = "$projectRoot\alfrescointegration\target"

# Internal Tomcat Paths (both live inside the same container now)
$targetLibPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/lib/"
$shareExtPath  = "/usr/local/tomcat/shared/classes/alfresco/web-extension/"

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
# STAGE 2: INJECTION LOGIC (JAR & SHARE CONFIG)
# ==========================================
Write-Host "--- STAGE 2: Injecting Constraint JAR & Share Config ---" -ForegroundColor Yellow

# A. Remove old JAR version first
oc exec $alfrescoPod -n $namespace -- rm -f "${targetLibPath}alfrescointegration-1.0-SNAPSHOT.jar"

# B. Find and copy the new Constraint JAR
$jarFile = Get-ChildItem -Path $constraintJarPath -Filter "*.jar" | Where-Object { $_.Name -notmatch "sources|javadoc" } | Select-Object -First 1

if ($jarFile) {
    Write-Host "Deploying Constraint JAR: $($jarFile.Name)" -ForegroundColor Green
    oc cp $jarFile.FullName "$namespace/${alfrescoPod}:${targetLibPath}$($jarFile.Name)"
} else {
    Write-Host "No JAR found in $constraintJarPath. Did you run 'mvn clean install'?" -ForegroundColor Red
}

# C. Inject Share Config XML
$shareXml = "$projectRoot\share-config-custom.xml"
$targetShareName = "share-config-custom.xml"

Write-Host "Injecting Share Config: $targetShareName" -ForegroundColor Green
oc cp "$shareXml" "$namespace/${alfrescoPod}:${shareExtPath}${targetShareName}"

# Scrub Windows Junk (BOM and CRLF) & Set Permissions for Share config
oc exec $alfrescoPod -n $namespace -- sed -i '1s/^\xEF\xBB\xBF//' "${shareExtPath}${targetShareName}"
oc exec $alfrescoPod -n $namespace -- sed -i 's/\r$//' "${shareExtPath}${targetShareName}"
oc exec $alfrescoPod -n $namespace -- chmod 666 "${shareExtPath}${targetShareName}"


# ==========================================
# STAGE 3: ALFRESCO HEALTH CHECK
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