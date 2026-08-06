<#
.SYNOPSIS
    Tags a local Docker image and pushes it to GitHub Container Registry (GHCR).
#>

Clear-Host
$ErrorActionPreference = "Stop"

# ==========================================
# 1. CONFIGURATION
# ==========================================
$localRepo = "vocbench-vocbench"          # Local Docker repository name
$localTag  = "latest"                # Local Docker tag
$ghUser    = "RubenJungGitHub"       # Your GitHub username
$targetTag = "v1.0"                  # Target tag for GHCR

# IMPORTANT: Replace with your actual Classic Personal Access Token (ghp_...)
$githubToken = "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" 

# ==========================================
# 2. GHCR AUTHENTICATION
# ==========================================
Write-Host "Authenticating Docker with GHCR..." -ForegroundColor Cyan

# Feed the token into docker login via STDIN
$loginOutput = $githubToken | docker login ghcr.io -u $ghUser --password-stdin 2>&1

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Authentication to ghcr.io failed!" -ForegroundColor Red
    Write-Host $loginOutput -ForegroundColor Red
    exit 1
} else {
    Write-Host "Login Succeeded!" -ForegroundColor Green
}

# ==========================================
# 3. LOCATE LOCAL DOCKER IMAGE
# ==========================================
$searchPattern = "$localRepo`:$localTag"
Write-Host "`nSearching for local image '$searchPattern'..." -ForegroundColor Cyan

# Query Docker for the repository:tag string
$localImage = (docker images --format "{{.Repository}}:{{.Tag}}" | Select-String -Pattern "^$searchPattern$" | Select-Object -First 1)

# Fallback: search by repository name only if exact tag wasn't found
if (-not $localImage) {
    $localImage = (docker images --format "{{.Repository}}:{{.Tag}}" | Select-String -Pattern $localRepo | Select-Object -First 1)
}

if (-not $localImage) {
    Write-Host "ERROR: Could not find local image matching '$localRepo'!" -ForegroundColor Red
    Write-Host "Available local images:" -ForegroundColor DarkGray
    docker images --format "{{.Repository}}:{{.Tag}}"
    exit 1
}

$localImage = $localImage.ToString().Trim()
Write-Host "Found local image: $localImage" -ForegroundColor Cyan

# ==========================================
# 4. CONSTRUCT TARGET & TAG IMAGE
# ==========================================
# Formats ghcr.io path ensuring username and repository are strictly lowercased
$ghcrImage = "ghcr.io/$($ghUser.ToLower())/$($localRepo.ToLower()):$targetTag"

Write-Host "Tagging as:       $ghcrImage" -ForegroundColor Yellow

docker tag $localImage $ghcrImage

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to tag image!" -ForegroundColor Red
    exit 1
}

# ==========================================
# 5. PUSH TO GITHUB CONTAINER REGISTRY
# ==========================================
Write-Host "`nPushing image to GitHub..." -ForegroundColor Green

docker push $ghcrImage

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nSUCCESS: Image pushed to GHCR!" -ForegroundColor Green
    Write-Host "Target: $ghcrImage" -ForegroundColor Cyan
} else {
    Write-Host "`nERROR: Push failed. Verify your Classic Token has 'write:packages' scope." -ForegroundColor Red
}