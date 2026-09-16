# ==========================================
# 1. CONFIGURATION
# ==========================================
$projectRoot = "D:\contAIn\alfrescotaxintegration"
$namespace = "contain-dvl" 

$token       = 'sha256~8UJKuUrXdrjrp-H83pYMD08SlUhkBf5QX_o9hJXm-ug'
$server      = 'https://api.ocp0.anders-nogic.atos.net:6443'

# --- GitHub Registry Configuration ---
# --- GitHub Registry Configuration ---
$ghcrUser    = "RubenJungGitHub"
$ghcrToken   = "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"   
$ghcrImage = "ghcr.io/$($ghcrUser.ToLower())/alfresco-custom"
$buildTag    = "build-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$targetImage = "${ghcrImage}:${buildTag}"
$DeployONly = $true

cls

# ==========================================
# STAGE 0: Login to OpenShift & Setup Secret
# ==========================================
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server --insecure-skip-tls-verify=true

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to login to OpenShift!" -ForegroundColor Red
    exit 1
}

oc project $namespace


# Link secret to the default service account so runtime pods can pull from GHCR
oc secret link default github-secret --for=pull -n $namespace 2>$null

# ==========================================
# STAGE 0.5: Cleanup Previous Build Clutter
# ==========================================
Write-Host "--- Cleaning up previous failed and completed pods ---" -ForegroundColor Cyan
oc delete pods --field-selector=status.phase=Failed --ignore-not-found=true -n $namespace
oc delete pods --field-selector=status.phase=Succeeded --ignore-not-found=true -n $namespace

# ==========================================
# STAGE 1: Build Local Maven Package
# ==========================================
Write-Host "--- STAGE 1: Building local Maven package ---" -ForegroundColor Cyan
Set-Location "$projectRoot\contain-alfresco"

if (-not $DeployONly) {
    Write-Host "Running Maven build..." -ForegroundColor Cyan
    mvn clean package -DskipTests

    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] Maven build failed!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "[$DeployONly is set to True] Skipping Maven build and using existing artifacts..." -ForegroundColor Yellow
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Maven build failed!" -ForegroundColor Red
    exit 1
}
Set-Location $projectRoot

# ==========================================
# STAGE 2: Build Docker Image Locally & Push to GHCR
# ==========================================
Write-Host "--- STAGE 2: Building and Pushing Image to GitHub Container Registry ---" -ForegroundColor Yellow

$stagingDir = "$projectRoot\openshift-build-staging"
if (Test-Path $stagingDir) { Remove-Item $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir | Out-Null

$sourceJar = "$projectRoot\contain-alfresco\contain-alfresco-platform\target\contain-alfresco-platform-1.0-SNAPSHOT.jar"
if (-not (Test-Path $sourceJar)) {
    Write-Host "[ERROR] Could not find compiled platform JAR at: $sourceJar" -ForegroundColor Red
    exit 1
}
Copy-Item -Path $sourceJar -Destination $stagingDir

# Create Dockerfile pointing to public base image (e.g., Quay or DockerHub)
$dockerfileContent = @"
FROM quay.io/alfresco/alfresco-content-repository:23.2.1
USER root
COPY contain-alfresco-platform-1.0-SNAPSHOT.jar /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/
USER alfresco
"@
Set-Content -Path "$stagingDir\Dockerfile" -Value $dockerfileContent

# Log into GHCR via Docker CLI locally
Write-Host "Logging into GitHub Container Registry..." -ForegroundColor Cyan
$ghcrToken | docker login ghcr.io -u $ghcrUser --password-stdin

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to log into GHCR via Docker CLI." -ForegroundColor Red
    exit 1
}

Write-Host "Building Docker image locally..." -ForegroundColor Cyan
docker build -t $targetImage $stagingDir

Write-Host "Pushing image to GitHub Container Registry..." -ForegroundColor Cyan
docker push $targetImage

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to push image to GHCR." -ForegroundColor Red
    exit 1
}

Remove-Item $stagingDir -Recurse -Force -ErrorAction SilentlyContinue

# ==========================================
# STAGE 3: Rollout Using GHCR Image Reference
# ==========================================
Write-Host "--- STAGE 3: Rolling out new image via DeploymentConfig ---" -ForegroundColor Magenta

Write-Host "Updating dc/alfresco to use image: $targetImage" -ForegroundColor Cyan
oc set image dc/alfresco alfresco=$targetImage -n $namespace

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to update image on dc/alfresco." -ForegroundColor Red
    exit 1
}

Write-Host "Triggering latest rollout for dc/alfresco..." -ForegroundColor Cyan
oc rollout latest dc/alfresco -n $namespace

# Monitor rollout with a built-in safety check for image pull errors
$rolloutTimeout = 90
$elapsed = 0
$isHealthy = $false

while ($elapsed -lt $rolloutTimeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    
    $pullErrorPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco-" -and $_ -match "ErrImagePull|ImagePullBackOff|CrashLoopBackOff" } | Select-Object -First 1)
    if ($pullErrorPod) {
        Write-Host "[ERROR] Detected pod image pull or crash loop error. Aborting rollout wait." -ForegroundColor Red
        oc describe pod (($pullErrorPod -split '\s+')[0]) -n $namespace
        exit 1
    }

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
        Get-Content -Path $shareXml -Raw -Encoding Byte | oc exec -i -n $namespace $sharePod -- sh -c "cat > $targetSharePath"
        oc exec -n $namespace $sharePod -- sed -i '1s/^\xEF\xBB\xBF//' "$targetSharePath"
        oc exec -n $namespace $sharePod -- sed -i 's/\r$//' "$targetSharePath"
        oc exec -n $namespace $sharePod -- chmod 666 "$targetSharePath"
        oc rollout latest dc/share -n $namespace 2>$null
        Write-Host "--- SHARE CONFIGURATION INJECTED AND RESTARTED SUCCESSFULLY ---" -ForegroundColor Green
    }
}

# ==========================================
# STAGE 5: Cleaning up obsolete build pods
# ==========================================
Write-Host "--- STAGE 5: Cleaning up obsolete pods ---" -ForegroundColor Cyan
oc delete pods --field-selector=status.phase=Failed --ignore-not-found=true -n $namespace
oc delete pods --field-selector=status.phase=Succeeded --ignore-not-found=true -n $namespace

Write-Host "--- ALL PIPELINE STAGES COMPLETED SUCCESSFULLY ---" -ForegroundColor Green