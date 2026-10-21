<#
.SYNOPSIS
    Complete Staging and Deployment Script for Alfresco & Taxonomy Integration on OpenShift.
#>

# ==========================================
# 1. CONFIGURATION
# ==========================================
$projectRoot = "D:\contAIn\alfrescotaxintegration"
$namespace = "contain-dvl" 

$token  = 'sha256~sHwvQt4MbxRklcYBJHvyoRIfqRAshpEA7rye_JbCSpM'
$server      = 'https://api.ocp0.anders-nogic.atos.net:6443'

# --- GitHub Registry Configuration ---
# --- GitHub Registry Configuration ---
$ghcrUser    = "RubenJungGitHub"
$ghcrToken   = "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"   
$ghcrImage = "ghcr.io/$($ghcrUser.ToLower())/alfresco-custom"
$buildTag    = "build-" + (Get-Date -Format "yyyyMMdd-HHmmss")
$targetImage = "${ghcrImage}:${buildTag}"
$DeployONly = $false

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
    Write-Host "[DeployONly is set to True] Skipping Maven build and using existing artifacts..." -ForegroundColor Yellow
}
Set-Location $projectRoot

# ==========================================
# STAGE 2: Build & Push Repository Docker Image
# ==========================================
Write-Host "--- STAGE 2: Preparing Repository Extension Files and Building Docker Image ---" -ForegroundColor Yellow

$stagingDir = "$projectRoot\openshift-build-staging"
if (Test-Path $stagingDir) { Remove-Item $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path "$stagingDir\alfresco\extension\model" -Force | Out-Null
New-Item -ItemType Directory -Path "$stagingDir\alfresco\extension\context" -Force | Out-Null

$sourceRepoJar = "$projectRoot\contain-alfresco\contain-alfresco-platform\target\contain-alfresco-platform-1.0-SNAPSHOT.jar"
if (-not (Test-Path $sourceRepoJar)) {
    Write-Host "[ERROR] Could not find compiled platform JAR at: $sourceRepoJar" -ForegroundColor Red
    exit 1
}
Copy-Item -Path $sourceRepoJar -Destination $stagingDir

$sourceModel = "$projectRoot\contain-alfresco\contain-alfresco-platform\src\main\resources\alfresco\module\contain-alfresco-platform\model\containdocumentmodel.xml"
$sourceContextDir = "$projectRoot\contain-alfresco\contain-alfresco-platform\src\main\resources\alfresco\module\contain-alfresco-platform\context"

if (Test-Path $sourceModel) {
    Copy-Item -Path $sourceModel -Destination "$stagingDir\alfresco\extension\model\"
}
if (Test-Path $sourceContextDir) {
    Copy-Item -Path "$sourceContextDir\*" -Destination "$stagingDir\alfresco\extension\context\" -Recurse
}

$repoDockerfileContent = @"
FROM quay.io/alfresco/alfresco-content-repository:23.2.1
USER root
COPY contain-alfresco-platform-1.0-SNAPSHOT.jar /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/
COPY alfresco/extension /usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/extension
USER alfresco
"@
Set-Content -Path "$stagingDir\Dockerfile.repo" -Value $repoDockerfileContent

# ==========================================
# STAGE 2.5: Build & Push Share Docker Image
# ==========================================
Write-Host "--- STAGE 2.5: Preparing Share Extension Files and Building Docker Image ---" -ForegroundColor Yellow

$sourceShareJar = "$projectRoot\contain-alfresco\contain-alfresco-share\target\contain-alfresco-share-1.0-SNAPSHOT.jar"
if (-not (Test-Path $sourceShareJar)) {
    Write-Host "[ERROR] Could not find compiled Share JAR at: $sourceShareJar" -ForegroundColor Red
    exit 1
}
Copy-Item -Path $sourceShareJar -Destination $stagingDir

New-Item -ItemType Directory -Path "$stagingDir\alfresco\web-extension" -Force | Out-Null
$sourceShareXml = "$projectRoot\contain-alfresco\contain-alfresco-share\src\main\resources\alfresco\web-extension\share-config-custom.xml"
if (Test-Path $sourceShareXml) {
    Copy-Item -Path $sourceShareXml -Destination "$stagingDir\alfresco\web-extension\"
}

$shareDockerfileContent = @"
FROM quay.io/alfresco/alfresco-share:23.2.1
USER root
COPY contain-alfresco-share-1.0-SNAPSHOT.jar /usr/local/tomcat/webapps/share/WEB-INF/lib/
COPY alfresco/web-extension/share-config-custom.xml /usr/local/tomcat/shared/classes/alfresco/web-extension/share-config-custom.xml
USER alfresco
"@
Set-Content -Path "$stagingDir\Dockerfile.share" -Value $shareDockerfileContent

# Log into GHCR via Docker CLI
Write-Host "Logging into GitHub Container Registry..." -ForegroundColor Cyan
$ghcrToken | docker login ghcr.io -u $ghcrUser --password-stdin

if ($LASTEXITCODE -ne 0) {
    Write-Host "[ERROR] Failed to log into GHCR via Docker CLI." -ForegroundColor Red
    exit 1
}

# Build and Push Repository Image
Write-Host "Building Repository Docker image locally..." -ForegroundColor Cyan
docker build -f "$stagingDir\Dockerfile.repo" -t $targetImage $stagingDir
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "Pushing Repository image to GHCR..." -ForegroundColor Cyan
docker push $targetImage
if ($LASTEXITCODE -ne 0) { exit 1 }

# Define Target Share Image Tag
$targetShareImage = $targetImage -replace "repo", "share"
if ($targetShareImage -eq $targetImage) { $targetShareImage = "$targetImage-share" }

Write-Host "Building Share Docker image locally ($targetShareImage)..." -ForegroundColor Cyan
docker build -f "$stagingDir\Dockerfile.share" -t $targetShareImage $stagingDir
if ($LASTEXITCODE -ne 0) { exit 1 }

Write-Host "Pushing Share image to GHCR..." -ForegroundColor Cyan
docker push $targetShareImage
if ($LASTEXITCODE -ne 0) { exit 1 }

Remove-Item $stagingDir -Recurse -Force -ErrorAction SilentlyContinue


# ==========================================
# STAGE 2.6: Configure Repository Environment Properties
# ==========================================
Write-Host "--- STAGE 2.6: Setting Repository configuration properties ---" -ForegroundColor Cyan
#oc set env dc/alfresco TAXONOMY_SERVICE_URL=https://taxonomyservice:53000 -n $namespace

# ==========================================
# STAGE 2.7: Configure Share Environment Properties
# ==========================================
Write-Host "--- STAGE 2.7: Setting Share repository port and options ---" -ForegroundColor Cyan
#oc set image dc/share share=$targetShareImage -n $namespace
#oc set env dc/share REPO_PORT=8082 -n $namespace
#oc set env dc/share "JAVA_OPTS=-XX:MinRAMPercentage=50 -XX:MaxRAMPercentage=80 -Dorg.apache.catalina.webresources.Cache.maxSize=200000 -Dalfresco.host=alfresco -Dalfresco.port=8082 -Dalfresco.context= -Dalfresco.protocol=http" -n $namespace

# ==========================================
# STAGE 3: Rollout Repository Deployment
# ==========================================
Write-Host "--- STAGE 3: Rolling out new Repository image via DeploymentConfig ---" -ForegroundColor Magenta
oc set image dc/alfresco alfresco=$targetImage -n $namespace
oc rollout latest dc/alfresco -n $namespace

$rolloutTimeout = 90
$elapsed = 0
$repoHealthy = $false

while ($elapsed -lt $rolloutTimeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    
    $pullErrorPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^alfresco-" -and $_ -match "ErrImagePull|ImagePullBackOff|CrashLoopBackOff" } | Select-Object -First 1)
    if ($pullErrorPod) {
        Write-Host "[ERROR] Detected repo pod error. Aborting." -ForegroundColor Red
        oc describe pod (($pullErrorPod -split '\s+')[0]) -n $namespace
        exit 1
    }

    $statusCheck = oc rollout status dc/alfresco -n $namespace --watch=false 2>$null
    if ($statusCheck -match "rolled out" -or $statusCheck -match "successfully rolled out") {
        $repoHealthy = $true
        break
    }
    Write-Host "Waiting for repository rollout... ($elapsed/${rolloutTimeout}s)" -ForegroundColor DarkGray
}

if (-not $repoHealthy) {
    Write-Host "[ERROR] Repository rollout timed out or failed." -ForegroundColor Red
    exit 1
} else {
    Write-Host "Repository rollout success." -ForegroundColor Green
}

# ==========================================
# STAGE 4: Rollout Share Deployment
# ==========================================
Write-Host "--- STAGE 4: Rolling out new Share image via DeploymentConfig ---" -ForegroundColor Magenta

# Zorg dat de juiste nieuwe unieke image-tag wordt gekoppeld aan de DeploymentConfig
$targetShareImage = $targetImage -replace "repo", "share"
if ($targetShareImage -eq $targetImage) { $targetShareImage = "$targetImage-share" }

oc set image dc/share share=$targetShareImage -n $namespace
oc rollout latest dc/share -n $namespace

$elapsed = 0
$shareHealthy = $false

while ($elapsed -lt $rolloutTimeout) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    
    $pullErrorPod = (oc get pods -n $namespace --no-headers | Where-Object { $_ -match "^share-" -and $_ -match "ErrImagePull|ImagePullBackOff|CrashLoopBackOff" } | Select-Object -First 1)
    if ($pullErrorPod) {
        Write-Host "[ERROR] Detected share pod error. Aborting." -ForegroundColor Red
        oc describe pod (($pullErrorPod -split '\s+')[0]) -n $namespace
        exit 1
    }

    $statusCheck = oc rollout status dc/share -n $namespace --watch=false 2>$null
    if ($statusCheck -match "rolled out" -or $statusCheck -match "successfully rolled out") {
        $shareHealthy = $true
        break
    }
    Write-Host "Waiting for Share rollout... ($elapsed/${rolloutTimeout}s)" -ForegroundColor DarkGray
}

if (-not $shareHealthy) {
    Write-Host "[ERROR] Share rollout timed out or failed." -ForegroundColor Red
    exit 1
} else {
    Write-Host "Share rollout success." -ForegroundColor Green
}

# ==========================================
# STAGE 5: Cleanup Obsolete Pods
# ==========================================
Write-Host "--- STAGE 5: Cleaning up obsolete pods ---" -ForegroundColor Cyan
oc delete pods --field-selector=status.phase=Failed --ignore-not-found=true -n $namespace
oc delete pods --field-selector=status.phase=Succeeded --ignore-not-found=true -n $namespace

# --- STAGE 6: Defensively Ensure Alfresco is Scaled Up and Healthy ---
Write-Host "Verifying final replica state for Alfresco..." -ForegroundColor Cyan
oc scale dc/alfresco --replicas=1 -n $namespace

Write-Host "Waiting for Alfresco rollout to complete..." -ForegroundColor Cyan
oc rollout status dc/alfresco -n $namespace --timeout=120s

Write-Host "Deployment pipeline finished successfully!" -ForegroundColor Green


Write-Host "--- ALL PIPELINE STAGES COMPLETED SUCCESSFULLY ---" -ForegroundColor Green