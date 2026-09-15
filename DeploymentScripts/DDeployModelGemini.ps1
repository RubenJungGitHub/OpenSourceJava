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

$token       = 'sha256~eSx4mibiPtW_V-Eh88mjrGcem9srlemkQySqiTpAzsM'
$server      = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project     = "contain-dvl"
$namespace   = $project
$projectRoot = "D:\contAIn\alfrescotaxintegration"

cls

# ==========================================
# STAGE 0: Login to OpenShift
# ==========================================
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server --insecure-skip-tls-verify=true

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to login to OpenShift!" -ForegroundColor Red
    exit 1
}

oc project $namespace
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
# STAGE 2: Trigger Native Build with ImageStream Tag Output
# ==========================================
Write-Host "--- STAGE 2: Triggering Build with ImageStream Tag Output ---" -ForegroundColor Yellow

# Grant builder service account permissions
oc policy add-role-to-user system:image-puller system:serviceaccount:$namespace:builder -n $namespace 2>$null

$buildTag = "build-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$stagingDir = "$projectRoot\openshift-build-staging"

if (Test-Path $stagingDir) { Remove-Item $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir | Out-Null

$sourceJar = "$projectRoot\contain-alfresco\contain-alfresco-platform\target\contain-alfresco-platform-1.0-SNAPSHOT.jar"
if (-not (Test-Path $sourceJar)) {
    Write-Host "[ERROR] Could not find compiled platform JAR at: $sourceJar" -ForegroundColor Red
    exit 1
}
Copy-Item -Path $sourceJar -Destination $stagingDir

# Define base image tag strategy
$targetIs = "share"
$detectedTag = "23.2.1"

Write-Host "Using base ImageStreamTag: ${targetIs}:${detectedTag}" -ForegroundColor Green

# Create Dockerfile for custom build
$dockerfileContent = @"
FROM ${targetIs}:${detectedTag}
USER root
COPY contain-alfresco-platform-1.0-SNAPSHOT.jar /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/
USER alfresco
"@
Set-Content -Path "$stagingDir\Dockerfile" -Value $dockerfileContent

oc create imagestream alfresco-custom -n $namespace --dry-run=client -o yaml | oc apply -f -

# Create BuildConfig
$buildConfigYaml = @"
apiVersion: build.openshift.io/v1
kind: BuildConfig
metadata:
  name: alfresco-custom-build
  namespace: $namespace
spec:
  serviceAccount: builder
  output:
    to:
      kind: ImageStreamTag
      name: "alfresco-custom:$buildTag"
  source:
    type: Binary
  strategy:
    type: Docker
    dockerStrategy:
      dockerfilePath: Dockerfile
      from:
        kind: ImageStreamTag
        name: "${targetIs}:${detectedTag}"
        namespace: "$namespace"
"@
Set-Content -Path "$projectRoot\alfresco-build-pipeline.yaml" -Value $buildConfigYaml

oc apply -f "$projectRoot\alfresco-build-pipeline.yaml" -n $namespace
Start-Sleep -Seconds 2

oc start-build alfresco-custom-build --from-dir="$stagingDir" --follow -n $namespace

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] OpenShift internal build failed!" -ForegroundColor Red
    exit 1
}

Remove-Item $stagingDir -Recurse -Force -ErrorAction SilentlyContinue

# ==========================================
# STAGE 3: Rollout Using ImageStreamTag Reference
# ==========================================
Write-Host "--- STAGE 3: Rolling out new image via DeploymentConfig ---" -ForegroundColor Magenta

Write-Host "Updating dc/alfresco to use image stream tag: alfresco-custom:$buildTag" -ForegroundColor Cyan
oc set image dc/alfresco alfresco=image-registry.openshift-image-registry.svc:5000/$namespace/alfresco-custom:$buildTag -n $namespace

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to update image on dc/alfresco." -ForegroundColor Red
    exit 1
}

Write-Host "Triggering latest rollout for dc/alfresco..." -ForegroundColor Cyan
oc rollout latest dc/alfresco -n $namespace

# Monitor rollout with a built-in safety check for image pull errors
$rolloutTimeout = 60
$elapsed = 0
$isHealthy = $false

while ($elapsed -lt $rolloutTimeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    
    # Check if a pod is throwing an image pull error
    $pullErrorPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco-" -and $_ -match "ErrImagePull|ImagePullBackOff|CrashLoopBackOff" } | Select-Object -First 1)
    if ($pullErrorPod) {
        Write-Host "[ERROR] Detected pod image pull or crash loop error. Aborting rollout wait." -ForegroundColor Red
        oc describe pod (($pullErrorPod -split '\s+')[0]) -n $namespace
        exit 1
    }

    # Check if rollout succeeded
    $statusCheck = oc rollout status dc/alfresco -n $namespace --watch=false 2>$null
    if ($statusCheck -match "successfully rolled out") {
        $isHealthy = $true
        break
    }
    Write-Host "Waiting for rollout... ($elapsed/${rolloutTimeout}s)" -ForegroundColor DarkGray
}

if (-not $isHealthy) {
    Write-Host "[ERROR] Rollout timed out or failed to stabilize." -ForegroundColor Red
    exit 1
}

# ==========================================
# STAGE 4: Injecting Config to OpenShift Share
# ==========================================
Write-Host "--- STAGE 4: Injecting Config to OpenShift Share ---" -ForegroundColor Yellow

$shareXml = "$projectRoot\contain-alfresco\contain-alfresco-share\src\main\resources\alfresco\web-extension\share-config-custom.xml"
$targetSharePath = "/usr/local/tomcat/shared/classes/alfresco/web-extension/share-config-custom.xml"

$sharePod = (oc get pods -n $namespace --no-headers -o custom-columns=":metadata.name" | Where-Object { $_ -match "share" } | Select-Object -First 1).Trim()

if ($sharePod) {
    Write-Host "Found Share Pod: $sharePod" -ForegroundColor Green

    if (Test-Path $shareXml) {
        Write-Host "Copying share-config-custom.xml to Share pod..." -ForegroundColor Cyan
        Get-Content -Path $shareXml -Raw -Encoding Byte | oc exec -i -n $namespace $sharePod -- sh -c "cat > $targetSharePath"

        if ($LASTEXITCODE -ne 0) {
            Write-Host "[ERROR] Failed to copy config file to Share pod." -ForegroundColor Red
        } else {
            Write-Host "Scrubbing line endings and setting permissions..." -ForegroundColor Gray
            oc exec -n $namespace $sharePod -- sed -i '1s/^\xEF\xBB\xBF//' "$targetSharePath"
            oc exec -n $namespace $sharePod -- sed -i 's/\r$//' "$targetSharePath"
            oc exec -n $namespace $sharePod -- chmod 666 "$targetSharePath"

            Write-Host "Restarting Share deployment to refresh UI..." -ForegroundColor Magenta
            oc rollout latest dc/share -n $namespace 2>$null
            if ($LASTEXITCODE -ne 0) {
                oc rollout restart deployment/share -n $namespace 2>$null
            }

            Write-Host "--- SHARE CONFIGURATION INJECTED AND RESTARTED SUCCESSFULLY ---" -ForegroundColor Green
        }
    } else {
        Write-Host "[ERROR] Could not find share-config-custom.xml at: $shareXml" -ForegroundColor Red
    }
} else {
    Write-Host "[WARNING] Could not find the OpenShift Share pod to inject configuration." -ForegroundColor Yellow
}

# ==========================================
# STAGE 5: Cleaning up obsolete build pods
# ==========================================
Write-Host "--- STAGE 5: Cleaning up obsolete build pods ---" -ForegroundColor Cyan

oc delete pods --field-selector=status.phase=Failed --ignore-not-found=true -n $namespace
oc delete pods --field-selector=status.phase=Succeeded --ignore-not-found=true -n $namespace

Write-Host "--- ALL PIPELINE STAGES COMPLETED SUCCESSFULLY ---" -ForegroundColor Green