# --- 1. CONFIGURATION ---
$projectRoot = "C:\ContainOpenSource\Java\taxonomyservice"
$repoContainer = "developer-alfresco-1"
$shareContainer = "developer-share-1"
$composePath = "C:\Users\NL07428\alfresco-university\envs\sandbox\acs\developer\compose-withvolume.yml"
$constraintJarPath = "$projectRoot\alfrescointegration\target"
$m2Repo = "C:\Users\NL07428\.m2\repository"

# Internal Paths
$repoExtPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/extension/"
$shareExtPath = "/usr/local/tomcat/shared/classes/alfresco/web-extension/"

Set-Location $projectRoot

# --- 1. CONFIGURATION --- FOR JARS


# Target Paths
$targetExtPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/classes/alfresco/extension/"
$targetLibPath = "/usr/local/tomcat/webapps/alfresco/WEB-INF/lib/"
$shareExtPath  = "/usr/local/tomcat/shared/classes/alfresco/web-extension/"

Function Wait-For-Alfresco {
    param($maxRetries = 50)
    Write-Host "Checking Alfresco Health..." -ForegroundColor Cyan
    $count = 0
    while ($count -lt $maxRetries) {
        try {
            # Target the Repository directly to bypass Proxy interference
            $response = Invoke-WebRequest -Uri "http://localhost:8080/alfresco/api/-default-/public/alfresco/versions/1/probes/-ready-" -UseBasicParsing -TimeoutSec 2
            
            # If we get a 200, the Repo is alive. 
            if ($response.StatusCode -eq 200) { 
                Write-Host "ALFRESCO IS ONLINE!" -ForegroundColor Green
                return $true 
            }
        } catch { 
            # Still warming up...
        }
        $count++
        Write-Progress -Activity "Waiting for Alfresco" -Status "Attempt $count of $maxRetries"
        Start-Sleep -s 5
    }
    return $false
}

# --- 2. INJECTION LOGIC ---
Write-Host "--- STAGE 1: Injecting Dependencies & Unified Model ---" -ForegroundColor Yellow

# A. CLEANUP: Remove the old standalone taxonomy model to prevent namespace collisions
docker exec -u 0 $repoContainer rm -f "${targetExtPath}containdocumentmodel.xml"

# B. Copying JARs (Mongo + Custom)
#Get-ChildItem -Path "$m2Repo", "$projectRoot" -Filter "*.jar" -Recurse | Where-Object { $_.FullName -match "mongodb|bson|taxonomy" -and $_.FullName -notmatch "sources|javadoc" } | ForEach-Object {
#    Write-Host "Deploying JAR: $($_.Name)" -ForegroundColor Green
#    docker cp $_.FullName "${repoContainer}:$targetLibPath"
#}


# B. Copying ONLY the Constraint JAR
# Target the specific output folder of your Alfresco-side project

#remove first
docker exec -u 0 developer-alfresco-1 rm /usr/local/tomcat/webapps/alfresco/WEB-INF/lib/alfrescointegration-1.0-SNAPSHOT.jar
$jarFile = Get-ChildItem -Path $constraintJarPath -Filter "*.jar" | 
           Where-Object { $_.Name -notmatch "sources|javadoc" } | 
           Select-Object -First 1

if ($jarFile) {
    Write-Host "Deploying Constraint JAR: $($jarFile.Name)" -ForegroundColor Green
    docker cp $jarFile.FullName "${repoContainer}:$targetLibPath"
} else {
    Write-Host "No JAR found in $constraintJarPath. Did you run 'mvn clean install'?" -ForegroundColor Red
}

# ==========================================
# STAGE 1: REPOSITORY (THE BRAIN)
# ==========================================
Write-Host "--- STAGE 1: Injecting Model to Repository ---" -ForegroundColor Cyan

#No... this is no longer required... in the JAR
#xmlMainContext = "$projectRoot\contain-model-context.xml"
$xmlModel = "$projectRoot\containdocumentmodel.xml"

# Force start if stopped
docker start $repoContainer

#No longer required.. they are in the constraint.jar with references. 
#$repoFiles = @($xmlMainContext, $xmlModel)
$repoFiles = @()
foreach ($f in $repoFiles) {
    $name = Split-Path $f -Leaf
    Write-Host "Injecting to Repo: $name" -ForegroundColor Green
    docker cp "$f" "${repoContainer}:$repoExtPath"
    
    # Scrub Windows Junk (BOM and CRLF) & Set Permissions
    docker exec -u 0 $repoContainer sed -i '1s/^\xEF\xBB\xBF//' "${repoExtPath}$name"
    docker exec -u 0 $repoContainer sed -i 's/\r$//' "${repoExtPath}$name"
    docker exec -u 0 $repoContainer chown alfresco:alfresco "${repoExtPath}$name"
}

# Clean caches and Restart Repo
Write-Host "Restarting Repository to register model..." -ForegroundColor Magenta
docker restart $repoContainer

# Wait for Repo to be "Ready" (Crucial before touching Share)
$bootWait = 60
for ($i = 0; $i -le $bootWait; $i++) {
    $percent = ($i / $bootWait) * 100
    Write-Progress -Activity "Waiting for Repository Brain..." -Status "$($bootWait - $i)s remaining" -PercentComplete $percent
    Start-Sleep -s 1
}

# ==========================================
# STAGE 2: SHARE (THE FACE) - REPAIRED
# ==========================================
Write-Host "--- STAGE 2: Injecting Config to Share ---" -ForegroundColor Cyan

$shareXml = "$projectRoot\share-config-custom.xml"
$targetShareName = "share-config-custom.xml"

Write-Host "Injecting to Share: $targetShareName" -ForegroundColor Green
docker cp "$shareXml" "${shareContainer}:${shareExtPath}${targetShareName}"

# 1. Scrub Windows Junk (BOM and CRLF)
# Using 'exec -u 0' to run as root so we don't need to worry about user 'alfresco' permissions yet
docker exec -u 0 $shareContainer sed -i '1s/^\xEF\xBB\xBF//' "${shareExtPath}${targetShareName}"
docker exec -u 0 $shareContainer sed -i 's/\r$//' "${shareExtPath}${targetShareName}"

# 2. Set Permissions (Broadly compatible version)
# If 'alfresco' user fails, we use 'root' or skip it, as 'exec -u 0' already handles the file creation
Write-Host "Setting file permissions..." -ForegroundColor Gray
docker exec -u 0 $shareContainer chmod 666 "${shareExtPath}${targetShareName}"

Write-Host "Restarting Share to refresh UI..." -ForegroundColor Magenta
docker restart developer-share-1