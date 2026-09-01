$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"



#------------------------------------------------------------------
$token  = 'sha256~eSFDzs0frKC8TEi-DTqbiYKUmjtNnzQwPABk5UGZQVM'
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


#ALLWAYS REDEPLOY POSTGRESS IF ALFRESO IS REDEPLOYED!
# Define which components to process in this run
$PODS = @(
    #-------------------------------------------
    #"Postgres",
    #"Alfresco"
    #-------------------------------------------
    #"ActiveMQ"
    #"Routes"
    #"AlfrescoQueuePoller"
    #"MongoDB"
    #"REDIS"
    #"RHBC"
    #"RHKIE"
    #"Receiver"
    #"SharePointSubscription"
    #"SharePointNotification"
    #'GraphDB'
    #'VocBench3'
    'TaxonomyService'
    #'TaxonomyCacher'
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
    Write-Host "[INFO] Starting background port-forward for PostgreSQL..." -ForegroundColor Cyan
    Start-Job -Name "PostgresPortForward" -ScriptBlock {
        oc port-forward svc/postgres 5432:5432 -n contain-dvl
    }
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



# --- STAGE 7: RHBC ---
if ($PODS -contains "RHBC") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHBC'
    Write-Host "[INFO] Starting recreation of Red Hat Business Central..." -ForegroundColor Cyan
    
    # 1. Apply the manifests from your folder first (creates DC, Services, etc.)
    oc apply -R -f $folder -n $project
    
    # 2. Check if a stuck business-central pod exists before attempting a force delete
    $podName = (oc get pods -l io.kompose.service=business-central -n $project -o jsonpath='{.items[0].metadata.name}' 2>$null)
    if ($podName) {
        Write-Host "[INFO] Cleaning up existing pod: $podName" -ForegroundColor Yellow
        oc delete pod $podName -n $project --force --grace-period=0 --ignore-not-found
    }

    # Also force deletion
    Write-Host "[INFO] Cleaning up existing Business Central pods..." -ForegroundColor Yellow
    oc delete pods -l io.kompose.service=business-central -n $project --force --grace-period=0 --ignore-not-found

    # 3. Trigger a rollout restart to ensure the DeploymentConfig spins up fresh
    oc rollout restart dc/business-central -n $project

    # 4. Create or ensure a secure Edge route with HTTP-to-HTTPS redirect exists
    $routeExists = (oc get route business-central -n $project --ignore-not-found)
    $routeType = (oc get route business-central -n $project -o jsonpath='{.spec.tls.termination}' --ignore-not-found)
    $redirectPolicy = (oc get route business-central -n $project -o jsonpath='{.spec.tls.insecureEdgeTerminationPolicy}' --ignore-not-found)

    if (-not $routeExists) {
        Write-Host "[INFO] Exposing Business Central as a secure edge route with HTTP redirect..." -ForegroundColor Cyan
        oc create route edge business-central --service=business-central --port=8080 --insecure-policy='Redirect' -n $project
    } elseif ($routeType -ne "edge" -or $redirectPolicy -ne "Redirect") {
        Write-Host "[INFO] Updating/recreating route to ensure Edge termination and Redirect policy..." -ForegroundColor Yellow
        oc delete route business-central -n $project
        oc create route edge business-central --service=business-central --port=8080 --insecure-policy='Redirect' -n $project
    } else {
        Write-Host "[INFO] Secure Edge route with Redirect policy already exists." -ForegroundColor Green
    }
}


 # --- STAGE 7: RHKIE---
if ($PODS -contains "RHKIE") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\RHKIE'
    Write-Host "[INFO] Starting recreation of RH KIE..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
        oc apply -R -f $folder -n $project
    oc expose svc/business-central --port=8080 -n contain-dvl
    }

# --- STAGE 8: Receiver ---
if ($PODS -contains "Receiver") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Receiver\'
    Write-Host "[INFO] Starting recreation of receiver..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for receiver pod to be ready..." -ForegroundColor Yellow
    oc rollout status dc/receiver -n $project --timeout=180s

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
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}


# --- STAGE 10: SharePointSubscription---
if ($PODS -contains "SharePointSubscription") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Sharepoint\Subscriptionservice\'
    Write-Host "[INFO] Starting recreation of Sharepoint subscription service..." -ForegroundColor Cyan
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}


# --- STAGE 11: GraphDB---
if ($PODS -contains "GraphDB") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\GraphDB'
    Write-Host "[INFO] Starting recreation of GraphDB service..." -ForegroundColor Cyan
    
    # Delete existing resources safely
    oc delete -R -f $folder -n $project --ignore-not-found
    
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

# --- STAGE 12: VocBench3 ---
if ($PODS -contains "VocBench3") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\VocBench3'
    Write-Host "[INFO] Starting recreation of VocBench3 service..." -ForegroundColor Cyan
    
    # 1. Delete old deployment and resources first to release volume locks
    oc delete -R -f $folder -n $project --ignore-not-found

    # 2. Force-clean the specific PVC if it exists (handles stuck Terminating states)
    $pvcExists = oc get pvc vocbench-pvc-new -n $project --ignore-not-found
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
        oc delete pvc vocbench-pvc-new -n $project --grace-period=0 --force --ignore-not-found
    }

    # 3. Deploy fresh manifests
    oc apply -R -f $folder -n $project
}

# --- STAGE 13: SharePointNotificationService ---
if ($PODS -contains "SharePointNotification") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Sharepoint\NotificationService\'
    Write-Host "[INFO] Starting recreation of Sharepoint notification service..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for notificationservice to become ready..." -ForegroundColor Yellow
    oc rollout status dc/notificationservice -n $project --timeout=120s

    Write-Host "[INFO] Ensuring public secured route for notificationservice webhooks..." -ForegroundColor Cyan
    
    # 1. Create the route if it doesn't already exist
    if (-not (oc get route notificationservice -n $project --ignore-not-found)) {
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
}



# --- STAGE 14: TaxonomyService ---
if ($PODS -contains "TaxonomyService") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Taxonomy\Taxonomyservice\'
    Write-Host "[INFO] Starting recreation of TaxonomyService..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}


# --- STAGE 15: TaxonomyCacher ---
if ($PODS -contains "TaxonomyCacher") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ILS\Taxonomy\Taxonomycacher\'
    Write-Host "[INFO] Starting recreation of Taxonomycacher..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project
}


oc delete pod --field-selector=status.phase==Succeeded -n contain-dvl
Write-Host "[SUCCESS] Completed deployment!" -ForegroundColor Green