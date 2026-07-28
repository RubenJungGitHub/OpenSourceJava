<#
.SYNOPSIS
    Tags and pushes a list of local Docker images to GitHub Container Registry (GHCR).
#>

Clear-Host
$ErrorActionPreference = "Continue"

# ==========================================
# 1. CONFIGURATION
# ==========================================
$ghUser = "RubenJungGitHub"      # Your GitHub username
$githubToken = "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" 

# Define the collection of container images to process
$containers = @(
    @{ Repo = "ils-uuidutil";       LocalTag = "latest"; TargetTag = "v1.0" },
    @{ Repo = "ils-receiver";    LocalTag = "latest"; TargetTag = "v1.0" },
    @{ Repo = "ils-taxonomycacher";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "ils-taxonomy-service-ssl";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "ils-taxonomycacher";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "sharepoint-subscriptionservice";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "sharepoint-notificationservice";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "ilsqueuepollers-migrationqueuepoller";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "ilsqueuepollers-sharepointqueuepoller";    LocalTag = "latest"; TargetTag = "v1.0" }
    @{ Repo = "ilsqueuepollers-alfrescoqueuepoller";    LocalTag = "latest"; TargetTag = "v1.0" }
)
# ==========================================
# 2. GHCR AUTHENTICATION (ONCE FOR ALL)
# ==========================================
Write-Host "Authenticating Docker with GHCR..." -ForegroundColor Cyan

$loginOutput = $githubToken | docker login ghcr.io -u $ghUser --password-stdin 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Authentication to ghcr.io failed!" -ForegroundColor Red
    Write-Host $loginOutput -ForegroundColor Red
    exit 1
} else {
    Write-Host "Login Succeeded!`n" -ForegroundColor Green
}

# Trackers for summary at the end
$successCount = 0
$failedCount  = 0

# ==========================================
# 3. PROCESS COLLECTION
# ==========================================
foreach ($item in $containers) {
    $localRepo = $item.Repo
    $localTag  = $item.LocalTag
    $targetTag = $item.TargetTag

    Write-Host "--------------------------------------------------" -ForegroundColor DarkGray
    Write-Host "Processing Container: $localRepo`:$localTag" -ForegroundColor Magenta

    # 3a. Search for local image
    $searchPattern = "$localRepo`:$localTag"
    $localImage = (docker images --format "{{.Repository}}:{{.Tag}}" | Select-String -Pattern "^$searchPattern$" | Select-Object -First 1)

    # Fallback to repo name if exact tag isn't matched
    if (-not $localImage) {
        $localImage = (docker images --format "{{.Repository}}:{{.Tag}}" | Select-String -Pattern $localRepo | Select-Object -First 1)
    }

    if (-not $localImage) {
        Write-Host " [X] ERROR: Local image '$searchPattern' not found! Skipping..." -ForegroundColor Red
        $failedCount++
        continue
    }

    $localImage = $localImage.ToString().Trim()
    Write-Host " [+] Found local image: $localImage" -ForegroundColor Cyan

    # 3b. Tag Image
    $ghcrImage = "ghcr.io/$($ghUser.ToLower())/$($localRepo.ToLower()):$targetTag"
    Write-Host " [+] Tagging as:       $ghcrImage" -ForegroundColor Yellow

    docker tag $localImage $ghcrImage
    if ($LASTEXITCODE -ne 0) {
        Write-Host " [X] ERROR: Failed to tag $localImage" -ForegroundColor Red
        $failedCount++
        continue
    }

    # 3c. Push Image
    Write-Host " [+] Pushing $ghcrImage to GHCR..." -ForegroundColor Green
    docker push $ghcrImage

    if ($LASTEXITCODE -eq 0) {
        Write-Host " [V] SUCCESS: Pushed $ghcrImage" -ForegroundColor Green
        $successCount++
    } else {
        Write-Host " [X] ERROR: Push failed for $ghcrImage" -ForegroundColor Red
        $failedCount++
    }
}

# ==========================================
# 4. SUMMARY REPORT
# ==========================================
Write-Host "`n==========================================" -ForegroundColor DarkGray
Write-Host " BATCH PUSH COMPLETE" -ForegroundColor Cyan
Write-Host " Successful: $successCount" -ForegroundColor Green
Write-Host " Failed:     $failedCount" -ForegroundColor Red
Write-Host "==========================================" -ForegroundColor DarkGray