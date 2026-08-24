# ==========================================
# 1. CONFIGURATION
# ==========================================
$projectRoot = "D:\contAIn\alfrescotaxintegration"
$namespace = "contain-dvl" 
$constraintJarPath = "$projectRoot\contain-alfresco\contain-alfresco-platform\target\"
$ModelPath = "$projectRoot\contain-alfresco\contain-alfresco-share\src\main\resources\alfresco\web-extension"

# Internal Tomcat Paths
$targetLibPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/lib/"
$shareExtPath  = "/usr/local/tomcat/shared/classes/alfresco/web-extension/"

$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"
$token  = 'sha256~6j8DfxNYZ9H4mqseMgHZVkYhUP9kHjspdfvyy0hqd4Y'

$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

cls
function Test-AlfrescoReady {
    param($podName, $ns, $maxRetries = 60)
    Write-Host "Analyzing $podName conditions for health..." -ForegroundColor Cyan
    $count = 0
    while ($count -lt $maxRetries) {
        $podsJson = oc get pod $podName -n $ns -o json 2>$null | ConvertFrom-Json
        $conditions = @($podsJson.status.conditions)
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

function Wait-For-Alfresco-OpenShift {
    param($maxRetries = 60)
    Write-Host "Checking Alfresco Health via port-forward..." -ForegroundColor Cyan

    $currentPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco" } | Select-Object -First 1) -replace '\s+.*$', ''

    $pfJob = Start-Job -ScriptBlock {
        param($ns, $pod)
        oc port-forward pod/$pod 8080:8080 -n $ns
    } -ArgumentList $namespace, $currentPod

    Start-Sleep -Seconds 3 

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
            # Warming up...
        }
        $count++
        Write-Host "Waiting for Alfresco... Attempt $count of $maxRetries" -ForegroundColor DarkGray
        Start-Sleep -Seconds 5
    }

    Stop-Job $pfJob -ErrorAction SilentlyContinue
    Remove-Job $pfJob -ErrorAction SilentlyContinue

    return $isOnline
}


# ==========================================
# 3. EXECUTION FLOW
# ==========================================

Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server

Set-Location $projectRoot

# ==========================================
# STAGE 1: Build Local Maven Package
# ==========================================
Write-Host "--- STAGE 1: Building local Maven package ---" -ForegroundColor Cyan
Set-Location "$projectRoot\contain-alfresco"
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Maven build failed!" -ForegroundColor Red
    exit 1
}
Set-Location $projectRoot

# ==========================================
# STAGE 2: Trigger Native Build with Direct Registry Push
# ==========================================
Write-Host "--- STAGE 2: Triggering Build with Direct Registry Push ---" -ForegroundColor Yellow

# 1. Dynamically create the Dockerfile needed for the build strategy
$dockerfileContent = @"
FROM quay.io/alfresco/alfresco-content-repository:23.2.1
USER root
COPY contain-alfresco/contain-alfresco-platform/target/*.jar /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/
USER alfresco
"@
Set-Content -Path "$projectRoot\Dockerfile.openshift" -Value $dockerfileContent

# 2. Ensure the ImageStream exists to hold the final artifact
oc create imagestream alfresco-custom -n $namespace --dry-run=client -o yaml | oc apply -f -

# 3. Create a BuildConfig using a DockerImage output spec pointing to the internal service registry
$buildConfigYaml = @"
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: alfresco-custom-build
  namespace: $namespace
spec:
  output:
    to:
      kind: DockerImage
      name: "image-registry.openshift-image-registry.svc:5000/$namespace/alfresco-custom:latest"
  source:
    type: Binary
  strategy:
    type: Docker
    dockerStrategy:
      dockerfilePath: Dockerfile.openshift
"@
Set-Content -Path "$projectRoot\alfresco-build-pipeline.yaml" -Value $buildConfigYaml

# 4. Apply and execute the build cleanly
oc apply -f "$projectRoot\alfresco-build-pipeline.yaml" -n $namespace
Start-Sleep -Seconds 2

oc start-build alfresco-custom-build --from-dir="$projectRoot" --follow -n $namespace

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] OpenShift internal build failed!" -ForegroundColor Red
    exit 1
}

# ==========================================
# STAGE 3: Rollout New Image in OpenShift
# ==========================================
Write-Host "--- STAGE 3: Rolling out new image ---" -ForegroundColor Magenta
oc rollout restart deployment/alfresco -n $namespace 2>$null
if ($LASTEXITCODE -ne 0) {
    oc rollout restart dc/alfresco -n $namespace 2>$null
}

Write-Host "Waiting for old pod to terminate and new pod to spin up..." -ForegroundColor DarkGray
Start-Sleep -Seconds 15

# Re-resolve the new pod name dynamically
$alfrescoPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco" } | Select-Object -First 1) -replace '\s+.*$', ''

if (-not $alfrescoPod) {
    Write-Host "[ERROR] Could not resolve the newly deployed Alfresco pod." -ForegroundColor Red
    exit 1
}

Write-Host "Active new pod detected: $alfrescoPod" -ForegroundColor Green

if (!(Test-AlfrescoReady $alfrescoPod $namespace)) {
    Write-Host "[ERROR] New Alfresco pod failed to become ready." -ForegroundColor Red
    exit 1
}

$healthCheckPassed = Wait-For-Alfresco-OpenShift

if (-not $healthCheckPassed) {
    Write-Host "[WARNING] Alfresco did not report healthy within the timeout window." -ForegroundColor Yellow
    exit 1
} else {
    Write-Host "--- DEPLOYMENT AND HEALTH CHECK COMPLETED SUCCESSFULLY ---" -ForegroundColor Green
    
    # ==========================================
    # DIAGNOSTIC: VERIFY FILES IN NEW OPENSHIFT POD
    # ==========================================
    Write-Host "--- Checking Files Inside New OpenShift Pod ---" -ForegroundColor Cyan
    oc exec -n $namespace $alfrescoPod -c alfresco -- ls -la /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/ | Select-String "contain|tax|integration"
}

# ==========================================
# STAGE 4: Injecting Config to OpenShift Share (The Face)
# ==========================================
Write-Host "--- STAGE 4: Injecting Config to OpenShift Share ---" -ForegroundColor Yellow

# Updated path matching your Maven project directory structure
$shareXml = "$projectRoot\contain-alfresco\contain-alfresco-share\src\main\resources\alfresco\web-extension\share-config-custom.xml"
$targetSharePath = "/usr/local/tomcat/shared/classes/alfresco/web-extension/share-config-custom.xml"

# 1. Safely find the running Share pod without throwing null errors
$sharePod = ""
$podsJson = oc get pods -n $namespace -o json 2>$null | ConvertFrom-Json
if ($podsJson -and $podsJson.items) {
    $sharePod = ($podsJson.items | Where-Object { $_.metadata.name -match "share" } | Select-Object -First 1).metadata.name
}

if (-not $sharePod) {
    $sharePod = (oc get pods -n $namespace --no-headers -o custom-columns=":metadata.name" | Where-Object { $_ -match "share" } | Select-Object -First 1).Trim()
}

if ($sharePod) {
    Write-Host "Found Share Pod: $sharePod" -ForegroundColor Green

    if (Test-Path $shareXml) {
        # 2. Copy using tar via stdin/stdout redirection to avoid Windows drive-letter parsing issues
        Write-Host "Copying share-config-custom.xml to Share pod..." -ForegroundColor Cyan
        Get-Content -Path $shareXml -Raw -Encoding Byte | oc exec -i -n $namespace $sharePod -- sh -c "cat > $targetSharePath"

        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] Failed to copy config file to Share pod." -ForegroundColor Red
        } else {
            # 3. Scrub Windows Junk (BOM and CRLF) and fix permissions
            Write-Host "Scrubbing line endings and setting permissions..." -ForegroundColor Gray
            oc exec -n $namespace $sharePod -- sed -i '1s/^\xEF\xBB\xBF//' "$targetSharePath"
            oc exec -n $namespace $sharePod -- sed -i 's/\r$//' "$targetSharePath"
            oc exec -n $namespace $sharePod -- chmod 666 "$targetSharePath"

            # 4. Restart Share deployment to load the UI config
            Write-Host "Restarting Share deployment to refresh UI..." -ForegroundColor Magenta
            oc rollout restart deployment/share -n $namespace 2>$null
            if ($LASTEXITCODE -ne 0) {
                oc rollout restart dc/share -n $namespace 2>$null
            }

            Write-Host "--- SHARE CONFIGURATION INJECTED AND RESTARTED SUCCESSFULLY ---" -ForegroundColor Green
        }
    } else {
        Write-Host "[ERROR] Could not find share-config-custom.xml at: $shareXml" -ForegroundColor Red
    }
} else {
    Write-Host "[WARNING] Could not find the OpenShift Share pod to inject configuration." -ForegroundColor Yellow
}
Write-Host "--- STAGE 5: Cleaning up obsolete build pods ---" -ForegroundColor Cyan
# Clean up failed/completed build pods
oc delete pods --field-selector=status.phase=Failed --ignore-not-found=true
oc delete pods --field-selector=status.phase=Succeeded --ignore-not-found=true