$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"
$token  = 'sha256~IfQlNQnkzr_w8irRqUdFynXxfvZ_Z6pY7fmDRqt1Hmg'

#------------------------------------------------------------------
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

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

# Define which components to process in this run
$PODS = @(
    #"ActiveMQ",
    #"Postgres"
    #"Alfresco"
    #"Routes"
    #"AlfrescoQueuePoller"
    #"MongoDB"
    #"REDIS"
    #"RHBC"
    "RHKIE"
    #"Receiver"
)

# --- STAGE 1: PostgreSQL Infrastructure ---
if ($PODS -contains "Postgres" -or $PODS -contains "postgres") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\AFFRESCOPOSTGRESSFIRST\'
    Write-Host "[INFO] Starting recreation of PostgreSQL..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for PostgreSQL pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/postgres -n $project --timeout=120s
    #Foreward port
    oc port-forward svc/postgres 5432:5432 -n contain-dvl
}

# --- STAGE 2: ActiveMQ Infrastructure ---
if ($PODS -contains "ActiveMQ") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOACTIVEMQFIRST\'
    Write-Host "[INFO] Starting recreation of ActiveMQ..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for ActiveMQ pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/activemq -n $project --timeout=120s

    # Give ActiveMQ a brief buffer to fully open its NIO socket listener
    Write-Host "[INFO] Sleeping 15 seconds to allow ActiveMQ network listener to bind..." -ForegroundColor Yellow
    Start-Sleep -Seconds 15
}

# --- STAGE 3: Core Alfresco & Search Services ---
if ($PODS -contains "Alfresco" -or $PODS -contains "alfresco") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Starting recreation of Core Alfresco stack..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
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
    
    oc delete -R -f $shareFolder -n $project --ignore-not-found
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
}


# --- STAGE 4: Core Alfresco & Search Services ---
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
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
    
    Write-Host "[INFO] Ensuring Redis UI Route exists..." -ForegroundColor Cyan
    oc expose service redis --name=redis-ui --port=8001 --target-port=8001 --dry-run=client -o yaml | oc apply -f - -n contain-dvl
}

# --- STAGE 6: MongoDB ---
if ($PODS -contains "MongoDB") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\MongoDB'
    Write-Host "[INFO] Starting recreation of MongoDB..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
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
        Write-Host "[SUCCESS] MongoDB pod is running! Opening port-forward..." -ForegroundColor Green
        Write-Host "[NOTE] This will block the terminal. Keep this window open while using MongoDB Compass." -ForegroundColor Cyan
        
        # Execute port-forward (this will lock the script session here, which is intended for port forwarding)
        oc port-forward svc/mongodb 27017:27017 -n $project
    } else {
        Write-Host "[ERROR] MongoDB pod failed to start in time. Check 'oc get pods -n $project' for details." -ForegroundColor Red
    }
}



# --- STAGE 7: RHBC---
if ($PODS -contains "RHBC") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHBC'
    Write-Host "[INFO] Starting recreation of RH BC..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
    
    # Get the exact name of your stuck business-central pod
    $podName = (oc get pods -l io.kompose.service=business-central -n contain-dvl -o jsonpath='{.items[0].metadata.name}')

    # Force delete it with a grace period of 0 seconds
    oc delete pod $podName -n contain-dvl --force --grace-period=0
    oc apply -R -f $folder -n $project
    oc expose svc/business-central --port=8080 -n contain-dvl
 }


 # --- STAGE 7: RHKIE---
if ($PODS -contains "RHKIE") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHKIE'
    Write-Host "[INFO] Starting recreation of RH KIE..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
        oc apply -R -f $folder -n $project
    oc expose svc/business-central --port=8080 -n contain-dvl
    }



# --- STAGE 8: Receiver---
if ($PODS -contains "Receiver") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Receiver\'
    Write-Host "[INFO] Starting recreation of receiver..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}


# --- STAGE 9: AlfrescoQueuePoller---
if ($PODS -contains "AlfrescoQueuePoller") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSqueuepollers\Alfrescoqueuepoller\'
    Write-Host "[INFO] Starting recreation of Alfresco queupoller..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}




oc delete pod --field-selector=status.phase==Succeeded -n contain-dvl
Write-Host "[SUCCESS] Completed deployment!" -ForegroundColor Green