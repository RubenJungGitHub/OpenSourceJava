$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"



#------------------------------------------------------------------
$token  = 'sha256~j9Fb7romHI4YbZukHMv7tgWpDBLfOxKjCjItdLxuPf4'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

$localTag   = "latest"               # Local Docker tag
$ghUser     = "RubenJungGitHub"      # Your GitHub username

# IMPORTANT: Replace with your actual Classic Personal Access Token (ghp_...)
$githubToken = "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" 


cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server

Write-Host "[INFO] Logging into Quay.io..." -ForegroundColor Cyan
$quayPass | docker login quay.io -u $quayUser --password-stdin

# 2. Create/update OpenShift image pull secret
Write-Host "[INFO] Creating/updating OpenShift image pull secret..." -ForegroundColor Cyan
oc create secret docker-registry alfrescoquayio `
  --docker-server=quay.io `
  --docker-username=$quayUser `
  --docker-password=$quayPass `
  -n $project --dry-run=client -o yaml | oc apply -f -

# 3. Link the secret to the default service account for pulling images
oc secrets link default alfrescoquayio --for=pull -n $project
oc patch sa default --type=json -p='[{"op": "add", "path": "/imagePullSecrets/-", "value": {"name": "alfrescoquayio"}}]' -n $project 2>$null


#ALLWAYS REDEPLOY POSTGRESS IF ALFRESOnew IS REDEPLOYED!
# Define which components to process in this run
$PODS = @(
    #-------------------------------------------
    #"Postgres",
    #"Alfresconew"
    #-------------------------------------------
    #"ALfrescoupdate"
    #"ActiveMQ"
    #"Routes"
    #"Ollama"
    #"AlfrescoQueuePoller"
    #"SharepointQueuePoller"
    #"MigrationQueuePoller"
    #"MongoDB"
    #"REDIS"
    #"RHBC",
    #"RHKIE"
    "Receiver"
    #"SharePointNotification"
    #"SharePointSubscription"
    #'GraphDB'
    #'VocBench3'
    #'TaxonomyService'
    #'TaxonomyCacher'
)

#First start Desktop if not running
if (Get-Process "Docker Desktop" -ErrorAction SilentlyContinue) {
    Write-Host "Docker Desktop process is running." -ForegroundColor Green
} 
else 
{
    Write-Host "Docker Desktop is NOT running." -ForegroundColor Red
    Write-Host "[INFO] Starting Docker Desktop..." -ForegroundColor Cyan
    # Start Docker Desktop from its standard installation path
    Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    
    Write-Host "[INFO] Waiting for Docker engine to initialize (approx. 30 seconds)..." -ForegroundColor Yellow
    Start-Sleep -Seconds 30
}
# Verify connection
docker ps


# --- STAGE 1: PostgreSQL Infrastructure ---
if ($PODS -contains "Postgres" -or $PODS -contains "postgres") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\AFFRESCOPOSTGRESSFIRST\'
    Write-Host "[INFO] Starting recreation of PostgreSQL..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for PostgreSQL pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/postgres -n $project --timeout=120s
    #Foreward port
    Write-Host "[INFO] Starting background port-forward for PostgreSQL..." -ForegroundColor Cyan
    Start-Job -Name "PostgresPortForward" -ScriptBlock {
        oc port-forward svc/postgres 5432:5432 -n contain-dvl
    }
}

# --- STAGE 2: ActiveMQ Infrastructure ---
if ($PODS -contains "ActiveMQ") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOACTIVEMQFIRST\'
    Write-Host "[INFO] Starting recreation of ActiveMQ..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for ActiveMQ pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/activemq -n $project --timeout=120s

    # Give ActiveMQ a brief buffer to fully open its NIO socket listener
    Write-Host "[INFO] Sleeping 15 seconds to allow ActiveMQ network listener to bind..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
}

# --- STAGE 2.5: Update ALfresco 
if ($PODS -contains "ALfrescoupdate" -or $PODS -contains "aLfrescoupdate") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Applying core Alfresco manifests non-destructively..." -ForegroundColor Cyan
    
    # 1. Apply manifests safely without deleting existing resources or data
    oc apply -R -f $folder -n $project

    # Force deployment configs to recognize the pull secret
    $patchSecret = '{"spec":{"template":{"spec":{"imagePullSecrets":[{"name":"alfrescoquayio"}]}}}}'
    oc patch dc/alfresco -n $project --type=merge -p $patchSecret 2>$null
    oc patch dc/search -n $project --type=merge -p $patchSecret 2>$null
    oc patch dc/share -n $project --type=merge -p $patchSecret 2>$null

    # Fetch Search DeploymentConfig, modify security context in memory, and apply
    $dcSearch = oc get dc search -n $project -o json | ConvertFrom-Json
    if (-not $dcSearch.spec.template.spec.securityContext) {
        $dcSearch.spec.template.spec | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
    }
    $dcSearch.spec.template.spec.securityContext.runAsUser = 0
    $dcSearch.spec.template.spec.securityContext.fsGroup = 0

    foreach ($container in $dcSearch.spec.template.spec.containers) {
        if ($container.name -eq "search") {
            if (-not $container.securityContext) {
                $container | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
            }
            $container.securityContext.allowPrivilegeEscalation = $true
            $container.securityContext.runAsUser = 0
        }
    }
    $dcSearch | ConvertTo-Json -Depth 20 | oc apply -f -

    # 2. Trigger rolling update for core Alfresco to pull the new image with updated models
    Write-Host "[INFO] Rolling out core Alfresco with the latest image..." -ForegroundColor Cyan
    oc rollout latest dc/alfresco -n $project

    # Wait for Alfresco to be fully ready
    Write-Host "[INFO] Waiting for Alfresco repository pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/alfresco -n $project --timeout=300s

    # --- STAGE 4: Share Component (Last) ---
    $shareFolder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOSHARELAST\'
    Write-Host "[INFO] Applying Share manifests safely..." -ForegroundColor Cyan
    
    oc apply -R -f $shareFolder -n $project

    # Apply root security context patch to Share using a clean hashtable
    $dcShare = oc get dc share -n $project -o json | ConvertFrom-Json
    if (-not $dcShare.spec.template.spec.securityContext) {
        $dcShare.spec.template.spec | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
    }
    $dcShare.spec.template.spec.securityContext.runAsUser = 0
    $dcShare.spec.template.spec.securityContext.fsGroup = 0

    foreach ($container in $dcShare.spec.template.spec.containers) {
        if ($container.name -eq "share") {
            if (-not $container.securityContext) {
                $container | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
            }
            $container.securityContext.allowPrivilegeEscalation = $true
            $container.securityContext.runAsUser = 0
        }
    }
    $dcShare | ConvertTo-Json -Depth 20 | oc apply -f -

    # Trigger rolling update for Share
    oc rollout latest dc/share -n $project

    # Wait for Share to finish rolling out and starting
    Write-Host "[INFO] Waiting for Share pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/share -n $project --timeout=300s

    # Ensure license is present without breaking pods unnecessarily
    Write-Host "[INFO] Checking active Alfresco pod for license..." -ForegroundColor Cyan
    $alfrescoPod = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -match "alfresco" -and $_ -match "Running" } | Select-Object -First 1).Split()[0]

    if (-not $alfrescoPod) {
        Write-Host "[ERROR] Could not find a running Alfresco pod!" -ForegroundColor Red
    } else {
        $localLicense = "D:\contAIn\alfresco-university\license\acs\Alfresco-ent23-EvidenBelgiumNVExpiration2026-09-19.lic"
        if (Test-Path $localLicense) {
            $fileBytes = [System.IO.File]::ReadAllBytes($localLicense)
            $encodedContent = [Convert]::ToBase64String($fileBytes)
            $remoteCommand = "mkdir -p /usr/local/tomcat/alf_data/license && echo '$encodedContent' | base64 -d > /usr/local/tomcat/alf_data/license/Alfresco-ent23.lic"
            oc exec -n contain-dvl $alfrescoPod -- sh -c "$remoteCommand"
        }
    }
}


# --- STAGE 3: Core Alfresco & Search Services ---
if ($PODS -contains "Alfresconew" -or $PODS -contains "alfresconew") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Starting recreation of Core Alfresco stack..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    # Force deployment configs to recognize the pull secret
    $patchSecret = '{"spec":{"template":{"spec":{"imagePullSecrets":[{"name":"alfrescoquayio"}]}}}}'
    oc patch dc/alfresco -n $project --type=merge -p $patchSecret 2>$null
    oc patch dc/search -n $project --type=merge -p $patchSecret 2>$null
    oc patch dc/share -n $project --type=merge -p $patchSecret 2>$null

    # Fetch Search DeploymentConfig, modify security context in memory, and apply
    $dcSearch = oc get dc search -n $project -o json | ConvertFrom-Json
    if (-not $dcSearch.spec.template.spec.securityContext) {
        $dcSearch.spec.template.spec | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
    }
    $dcSearch.spec.template.spec.securityContext.runAsUser = 0
    $dcSearch.spec.template.spec.securityContext.fsGroup = 0

    foreach ($container in $dcSearch.spec.template.spec.containers) {
        if ($container.name -eq "search") {
            if (-not $container.securityContext) {
                $container | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
            }
            $container.securityContext.allowPrivilegeEscalation = $true
            $container.securityContext.runAsUser = 0
        }
    }
    $dcSearch | ConvertTo-Json -Depth 20 | oc apply -f -


    # Trigger rollout for core Alfresco
    oc rollout latest dc/alfresco -n $project

    # Wait for Alfresco to be fully ready
    Write-Host "[INFO] Waiting for Alfresco repository pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/alfresco -n $project --timeout=300s

    # --- STAGE 4: Share Component (Last) ---
    $shareFolder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOSHARELAST\'
    Write-Host "[INFO] Deploying Share component from dedicated folder..." -ForegroundColor Cyan
    
    oc delete -R -f $shareFolder -n $project --ignore-not-found --wait=false
    oc apply -R -f $shareFolder -n $project

    # Apply root security context patch to Share using a clean hashtable
    # Fetch Share DeploymentConfig, modify security context in memory, and apply
    $dcShare = oc get dc share -n $project -o json | ConvertFrom-Json
    if (-not $dcShare.spec.template.spec.securityContext) {
        $dcShare.spec.template.spec | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
    }
    $dcShare.spec.template.spec.securityContext.runAsUser = 0
    $dcShare.spec.template.spec.securityContext.fsGroup = 0

    foreach ($container in $dcShare.spec.template.spec.containers) {
        if ($container.name -eq "share") {
            if (-not $container.securityContext) {
                $container | Add-Member -MemberType NoteProperty -Name "securityContext" -Value (@{})
            }
            $container.securityContext.allowPrivilegeEscalation = $true
            $container.securityContext.runAsUser = 0
        }
    }
    $dcShare | ConvertTo-Json -Depth 20 | oc apply -f -

    # Trigger rollout for Share
    oc rollout latest dc/share -n $project

    # Wait for Share to finish rolling out and starting
    Write-Host "[INFO] Waiting for Share pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/share -n $project --timeout=300s



    #NOT YET WORKING!!!!!!
    # Create the license directory inside the container/PVC if it doesn't exist and copy it over
    Write-Host "[INFO] Finding active Alfresco pod..." -ForegroundColor Cyan
    $alfrescoPod = (oc get pods -n contain-dvl --no-headers | Where-Object { $_ -match "alfresco" -and $_ -match "Running" } | Select-Object -First 1).Split()[0]

    if (-not $alfrescoPod) {
        Write-Host "[ERROR] Could not find a running Alfresco pod!" -ForegroundColor Red
    } else {
        Write-Host "[INFO] Found Alfresco pod: $alfrescoPod" -ForegroundColor Cyan
    
        $localLicense = "D:\contAIn\alfresco-university\license\acs\Alfresco-ent23-EvidenBelgiumNVExpiration2026-09-19.lic"
    
        if (Test-Path $localLicense) {
            Write-Host "[INFO] Uploading license to the correct singular 'license' directory..." -ForegroundColor Cyan

            $localLicense = "D:\contAIn\alfresco-university\license\acs\Alfresco-ent23-EvidenBelgiumNVExpiration2026-09-19.lic"
            $fileBytes = [System.IO.File]::ReadAllBytes($localLicense)
            $encodedContent = [Convert]::ToBase64String($fileBytes)

            # Note the change from 'licenses' to 'license' (singular)
            $remoteCommand = "mkdir -p /usr/local/tomcat/alf_data/license && echo '$encodedContent' | base64 -d > /usr/local/tomcat/alf_data/license/Alfresco-ent23.lic"

            oc exec -n contain-dvl $alfrescoPod -- sh -c "$remoteCommand"

            Write-Host "[INFO] Restarting Alfresco pod so it registers the new license..." -ForegroundColor Yellow
            oc delete pod $alfrescoPod -n contain-dvl
        } else {
            Write-Host "[ERROR] Local license file not found on local path: $localLicense" -ForegroundColor Red
        }
    }
 }


# --- STAGE 4 Routes ---
if ($PODS -contains "Routes") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOROUTES\'
    Write-Host "[INFO] Deploying individual route manifests..." -ForegroundColor Cyan
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Retrieving generated route URLs..." -ForegroundColor Yellow
    oc get routes -l app=alfresco -n $project
}

# --- STAGE 5: Redis---
if ($PODS -contains "Redis") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\Redis'
    Write-Host "[INFO] Starting recreation of Redis..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
    
    Write-Host "[INFO] Ensuring Redis UI Route exists..." -ForegroundColor Cyan
    oc expose service redis --name=redis-ui --port=8001 --target-port=8001 --dry-run=client -o yaml | oc apply -f - -n contain-dvl
}

# --- STAGE 6: MongoDB ---
if ($PODS -contains "MongoDB") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\MongoDB'
    Write-Host "[INFO] Starting recreation of MongoDB..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for MongoDB pod to be ready..." -ForegroundColor Yellow
    
    # Loop until the pod status is 'Running' and ready (timeout after ~90 seconds)
    $maxTries = 30
    $tries = 0
    $podRunning = $false

    while (-not $podRunning -and $tries -lt $maxTries) {
        Start-Sleep -Seconds 3
        $tries++
        
        # Check if any pod matching the service is running
        $podStatus = oc get pods -l io.kompose.service=mongodb -n $project --no-headers 2>$null
        if ($podStatus -match "Running" -and $podStatus -match "1/1") {
            $podRunning = $true
        } else {
            Write-Host "[INFO] Waiting for pod startup ($tries/$maxTries)..." -ForegroundColor DarkGray
        }
    }

if ($podRunning) {
        Write-Host "[SUCCESS] MongoDB pod is running! Starting port-forward in background..." -ForegroundColor Green
        
        # Start port-forwarding as a background job so the script can continue
        Start-Job -ScriptBlock {
            param($proj)
            oc port-forward svc/mongodb 27017:27017 -n $proj
        } -ArgumentList $project
        
        Write-Host "[INFO] Port-forward is running in the background. You can now use MongoDB Compass." -ForegroundColor Cyan
    }
    else {
        Write-Host "[ERROR] MongoDB pod failed to start in time. Check 'oc get pods -n $project' for details." -ForegroundColor Red
    }
}

if ($PODS -contains "RHBC") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHBC'
    Write-Host "[INFO] Starting deployment of Red Hat Business Central..." -ForegroundColor Cyan
    
    # Save global error preference
    $oldErrorPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    # 1. Apply the manifests safely
    $applyOutput = oc apply -R -f $folder -n $project 2>&1
    if ($LASTEXITCODE -ne 0) {
        $ErrorActionPreference = $oldErrorPreference
        Write-Error "[ERROR] Failed to apply manifests:`n$applyOutput"
        exit 1
    } else {
        Write-Host "[INFO] Manifests applied successfully." -ForegroundColor Green
    }
    
    # 2. Trigger a safe restart using scale down/up for DeploymentConfig compatibility
    Write-Host "[INFO] Restarting Business Central DeploymentConfig..." -ForegroundColor Yellow
    
    # Scale down to 0
    $scaleDownOutput = oc scale dc/business-central --replicas=0 -n $project 2>&1
    Start-Sleep -Seconds 5
    
    # Scale back up to 1 to force a clean pod recreation with your configuration
    $scaleUpOutput = oc scale dc/business-central --replicas=1 -n $project 2>&1
    $rolloutOutput = "$scaleDownOutput`n$scaleUpOutput"

    if ($LASTEXITCODE -ne 0) {
        $ErrorActionPreference = $oldErrorPreference
        Write-Error "[ERROR] Restart failed:`n$rolloutOutput"
        exit 1
    } else {
        Write-Host "[INFO] Business Central successfully restarted." -ForegroundColor Green
    }

    # Restore global error preference
    $ErrorActionPreference = $oldErrorPreference


    if ($LASTEXITCODE -ne 0) {
        $ErrorActionPreference = $oldErrorPreference
        Write-Error "[ERROR] Rollout restart failed:`n$rolloutOutput"
        exit 1
    } else {
        Write-Host "[INFO] Rollout restart triggered." -ForegroundColor Green
    }

    # Restore global error preference before moving to the next section
    $ErrorActionPreference = $oldErrorPreference

    # 3. Create or ensure a secure Edge route with HTTP-to-HTTPS redirect exists
    $routeExists = oc get route business-central -n $project --ignore-not-found --wait=false 2>$null
    if (-not $routeExists) {
        Write-Host "[INFO] Exposing Business Central as a secure edge route with HTTP redirect..." -ForegroundColor Cyan
        oc create route edge business-central --service=business-central --port=8080 --insecure-policy='Redirect' -n $project 2>&1 | Out-Null
    } else {
        $routeType = oc get route business-central -n $project -o jsonpath='{.spec.tls.termination}' 2>$null
        $redirectPolicy = oc get route business-central -n $project -o jsonpath='{.spec.tls.insecureEdgeTerminationPolicy}' 2>$null

        if ($routeType -ne "edge" -or $redirectPolicy -ne "Redirect") {
            Write-Host "[INFO] Updating/recreating route to ensure Edge termination and Redirect policy..." -ForegroundColor Yellow
            oc delete route business-central -n $project --ignore-not-found --wait=false 2>&1 | Out-Null
            oc create route edge business-central --service=business-central --port=8080 --insecure-policy='Redirect' -n $project 2>&1 | Out-Null
        } else {
            Write-Host "[INFO] Secure Edge route with Redirect policy already exists." -ForegroundColor Green
        }
    }
}

# --- STAGE 7: RHKIE---
if ($PODS -contains "RHKIE") {

    Write-Host "[INFO] Cleaning up and linking correct image pull secrets..." -ForegroundColor Cyan
    $global:ErrorActionPreference = "Continue"
    oc secret unlink default containgithubrepos alfrescoquayio dockerhub-secret -n $project
    oc secret link default containgithubrepos alfrescoquayio dockerhub-secret --for=pull -n $project
    $global:ErrorActionPreference = "Stop"

    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHKIE'
    Write-Host "[INFO] Starting recreation of RH KIE..." -ForegroundColor Cyan
    
    $global:ErrorActionPreference = "Continue"
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
    
    Write-Host "[INFO] Waiting for kie-server deployment to complete..." -ForegroundColor Cyan
    oc rollout status dc/kie-server -n $project --timeout=180s
    
    $global:ErrorActionPreference = "Stop"

    #Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/kie-server 57000:57000 -n $project"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/kie-server 8080:8080 -n $project"
    
    $global:ErrorActionPreference = "Continue"
    oc expose svc/business-central --port=8080 -n $project
    $global:ErrorActionPreference = "Stop"
}

# --- STAGE 8: Receiver ---
if ($PODS -contains "Receiver") {

    #First push new image to GH
    $container  = "receiver"
    $localRepo  = "ils-" + $container              # Local Docker repository name in Docker Desktop (ils-receiver)
    $remoteRepo = "ils-" + $container              # Exact name for your GitHub package repository
    $localTag   = "latest"                         # Local Docker tag
    $targetTag  = "v1.0"                           # Target tag for GHCR

    # ==========================================
    # 2. BUILD FRESH DOCKER IMAGE LOCALLY
    # ==========================================
    
    $PSNativeCommandUseErrorActionPreference = $false
    Write-Host "[INFO] Building fresh Docker image via Docker Compose (no-cache)..." -ForegroundColor Cyan

    $oldPreference = $ErrorActionPreference
    $ErrorActionPreference = 'SilentlyContinue'

    try {
        # Pointing to the root 'ils' folder where docker-compose.yml actually lives
        cmd /c "docker compose -f D:\contAIn\OpenSourceJava\ils\receiver\docker-compose.yml build --no-cache receiver"
    } finally {
        $ErrorActionPreference = $oldPreference
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Docker build failed!" -ForegroundColor Red
        exit 1
    }

    $localImage = "$localRepo`:latest"
    $ghcrImage  = "ghcr.io/$($ghUser.ToLower())/$($remoteRepo.ToLower()):$targetTag"

    Write-Host "[INFO] Tagging local image to $ghcrImage..." -ForegroundColor Cyan
    docker tag $localImage $ghcrImage

    Write-Host "[INFO] Pushing fresh image to GitHub..." -ForegroundColor Green
    docker push $ghcrImage

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Docker push failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "[SUCCESS] Image successfully pushed to GHCR: $ghcrImage" -ForegroundColor Green


    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Receiver\'
    Write-Host "[INFO] Starting recreation of receiver..." -ForegroundColor Cyan
    
    $global:ErrorActionPreference = "Continue"
    
    # Deleting and re-applying handles the fresh deployment automatically because of imagePullPolicy: Always
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for receiver pod to be ready..." -ForegroundColor Yellow
    oc rollout status dc/receiver -n $project --timeout=180s

    $global:ErrorActionPreference = "Stop"

    # Check if the route already exists; if not, create it
    $routeCheck = oc get route receiver -n $project --ignore-not-found 
    if (-not $routeCheck) {
        Write-Host "[INFO] Exposing receiver service with an OpenShift Route..." -ForegroundColor Cyan
        oc expose service receiver --port=8080 -n $project
    }

    # Retrieve the route host and open Swagger UI
    $routeHost = oc get route receiver -n $project -o jsonpath='{.spec.host}'
    $swaggerUrl = "http://${routeHost}/swagger-ui/index.html"

    Write-Host "[SUCCESS] Receiver Swagger UI is ready!" -ForegroundColor Green
    Write-Host "URL: $swaggerUrl" -ForegroundColor Cyan
    Start-Process $swaggerUrl
}

# --- STAGE 9: AlfrescoQueuePoller---
if ($PODS -contains "AlfrescoQueuePoller") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSqueuepollers\Alfrescoqueuepoller\'
    Write-Host "[INFO] Starting recreation of Alfresco queupoller..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}

# --- STAGE 10: SharepointQueuePoller---
if ($PODS -contains "SharepointQueuePoller") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSqueuepollers\SharepointQueuePoller\'
    Write-Host "[INFO] Starting recreation of SharepointQueuePoller queupoller..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}



# --- STAGE 11: MigrationQueuePoller---
if ($PODS -contains "MigrationQueuePoller") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSqueuepollers\MigrationQueuePoller\'
    Write-Host "[INFO] Starting recreation of MigrationQueuePoller queupoller..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}



# --- STAGE 12: GraphDB---
if ($PODS -contains "GraphDB") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\GraphDB'
    Write-Host "[INFO] Starting recreation of GraphDB service..." -ForegroundColor Cyan
    
    # Delete existing resources safely
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    
    # Apply the correct Deployment/Service/PVC manifests
    oc apply -R -f $folder -n $project
    
    # Check if the route already exists before trying to expose it to prevent script failure
    $routeExists = oc get route graphdb-service -n $project --ignore-not-found
    if (-not $routeExists) {
        Write-Host "[INFO] Exposing GraphDB service route..." -ForegroundColor Cyan
        oc expose service graphdb-service --port=7200 -n $project
    } else {
        Write-Host "[INFO] GraphDB route already exists, skipping exposure." -ForegroundColor Green
    }
}

# --- STAGE 13: VocBench3 ---
if ($PODS -contains "VocBench3") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\VocBench3'
    Write-Host "[INFO] Starting recreation of VocBench3 service..." -ForegroundColor Cyan
    
    # 1. Delete old deployment and resources first to release volume locks
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false

    # 2. Force-clean the specific PVC if it exists (handles stuck Terminating states)
    $pvcExists = oc get pvc vocbench-pvc-new -n $project --ignore-not-found --wait=false
    if ($pvcExists) {
        Write-Host "[INFO] Cleaning up old PVC: vocbench-pvc-new" -ForegroundColor Yellow
        $patchPath = "$env:TEMP\pvc-patch.json"
        '{"metadata":{"finalizers":null}}' | Out-File -FilePath $patchPath -Encoding utf8 -NoNewline
        
        try {
            oc patch pvc vocbench-pvc-new -n $project --type=merge --patch-file="$patchPath" 2>&1 | Out-Null
        } catch {
            # Catch block handles cases where the PVC is already partially deleted
        }
        
        Remove-Item -Path $patchPath -Force -ErrorAction SilentlyContinue
        oc delete pvc vocbench-pvc-new -n $project --grace-period=0 --force --ignore-not-found --wait=false
    }

    # 3. Deploy fresh manifests
    oc apply -R -f $folder -n $project
}

# --- STAGE 14: SharePointNotificationService ---
if ($PODS -contains "SharePointNotification") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Sharepoint\NotificationService\'
    Write-Host "[INFO] Starting recreation of Sharepoint notification service..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for notificationservice to become ready..." -ForegroundColor Yellow
    oc rollout status dc/notificationservice -n $project --timeout=120s

    Write-Host "[INFO] Ensuring public secured route for notificationservice webhooks..." -ForegroundColor Cyan
    
    # 1. Create the route if it doesn't already exist
    if (-not (oc get route notificationservice -n $project --ignore-not-found --wait=false)) {
        oc expose service notificationservice --port=5001 -n $project
    }

    # 2. Patch the route using escaped quotes so oc.exe receives valid JSON
    $patch = '{"spec":{"tls":{"termination":"edge","insecureEdgeTerminationPolicy":"Redirect"}}}'.Replace('"', '\"')
    oc patch route notificationservice --type=merge -p $patch -n $project | Out-Null

    Start-Sleep -Seconds 2
    
    # 3. Fetch and display the final secured public URL
    $publicUrl = & oc get route notificationservice -n $project -o jsonpath='{.spec.host}' 2>$null
    if ($publicUrl) {
        Write-Host "[SUCCESS] Secured Webhook endpoint ready at: https://$publicUrl" -ForegroundColor Green
    } else {
        Write-Warning "[WARNING] Route was created, but failed to fetch the public host automatically. Check 'oc get routes -n $project'."
    }

    # 4. Repeatable Local Bridge Setup (Kill old port-forwards and spin up a fresh one)
    Write-Host "[INFO] Cleaning up any existing local port-forwards on port 5001..." -ForegroundColor Cyan
    Get-Process -Name "oc" -ErrorAction SilentlyContinue | Where-Object { $_.CommandLine -like "*port-forward*5001:5001*" } | Stop-Process -Force -ErrorAction SilentlyContinue

    Write-Host "[INFO] Starting fresh background port-forward for ngrok bridge..." -ForegroundColor Cyan
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/notificationservice 5001:5001 -n $project"

    Write-Host "[SUCCESS] Stage 14 complete. Local port-forward active on port 5001. Ready for ngrok!" -ForegroundColor Green
}


# --- STAGE 15: SharePointSubscription---
if ($PODS -contains "SharePointSubscription") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Sharepoint\Subscriptionservice\'
    Write-Host "[INFO] Starting recreation of Sharepoint subscription service..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}


# --- STAGE 16: TaxonomyService ---
if ($PODS -contains "TaxonomyService") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Taxonomy\Taxonomyservice\'
    Write-Host "[INFO] Starting recreation of TaxonomyService..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}


# --- STAGE 17: TaxonomyCacher ---
if ($PODS -contains "TaxonomyCacher") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Taxonomy\Taxonomycacher\'
    Write-Host "[INFO] Starting recreation of Taxonomycacher..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found --wait=false
    oc apply -R -f $folder -n $project
}

# --- STAGE 18: Ollama ---
if ($PODS -like "*ollama*") {
    Write-Host "[INFO] Processing Ollama deployment..." -ForegroundColor Cyan
    
    # 1. Ruim oude versies op (geen losse check nodig dankzij --ignore-not-found)
    oc delete all -l app=ollama -n $project --ignore-not-found --wait=true

    # 2. Maak de applicatie aan inclusief de vereiste /tmp mappen voor OpenShift rechten
    oc new-app ollama/ollama:latest `
        --name=ollama `
        -e HOME=/tmp `
        -e OLLAMA_MODELS=/tmp/models `
        -e OLLAMA_HOST=0.0.0.0:11434 `
        -n $project

    # 3. Wacht netjes tot de pod daadwerkelijk 'Running' is
    Write-Host "[INFO] Waiting for Ollama pod to be ready..." -ForegroundColor Cyan
    oc rollout status deployment/ollama -n $project --watch=true

    # 4. Trek automatisch het llama3.2 model binnen
    Write-Host "[INFO] Pulling llama3.2 model into Ollama..." -ForegroundColor Cyan
    oc exec deployment/ollama -n $project -- ollama pull llama3.2
} else {
    Write-Host "[INFO] Skipping Ollama (not selected in `$PODS)..." -ForegroundColor Gray
}



oc delete pod --field-selector=status.phase==Succeeded -n contain-dvl


# --- STAGE 19: Docker Cleanup to Prevent WSL Disk Bloat ---
Write-Host "[INFO] Cleaning up Docker build cache and dangling layers..." -ForegroundColor Cyan

# Verwijdert de tijdelijke build cache die tijdens het bouwen van images wordt aangemaakt
docker builder prune -f --all 2>$null

# Verwijdert losse, niet-gekoppelde image-lagen en gestopte containers
docker image prune -f 2>$null
docker container prune -f 2>$null

Write-Host "[SUCCESS] Docker disk cleanup completed!" -ForegroundColor Green

Write-Host "[SUCCESS] Completed deployment!" -ForegroundColor Green