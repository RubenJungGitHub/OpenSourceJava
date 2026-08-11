$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"
$token  = 'sha256~Ef6Iy10zbJSomF27ubQApUZngRyHBT7vZ1VTghkZz_k'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
#------------------------------------------------------------------
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
#------------------------------------------------------------------

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
    #"Alfresco",
    "Routes"
)

# --- STAGE 1: PostgreSQL Infrastructure ---
if ($PODS -contains "Postress" -or $PODS -contains "postgres") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\AFFRESCOPOSTGRESSFIRST\'
    Write-Host "[INFO] Starting recreation of PostgreSQL..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for PostgreSQL pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/postgres -n $project --timeout=120s
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

    # Apply root security patch to Search so it can initialize safely
    $patchSearch = '{"spec":{"template":{"spec":{"securityContext":{"runAsUser":0,"fsGroup":0},"containers":[{"name":"search","securityContext":{"allowPrivilegeEscalation":true,"runAsUser":0}}]}}}}'
    oc patch dc/search -n $project --type=merge -p $patchSearch

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

    # Apply root security context patch to Share to bypass Tomcat path permission blocks
    $patchShare = '{"spec":{"template":{"spec":{"securityContext":{"runAsUser":0,"fsGroup":0},"containers":[{"name":"share","securityContext":{"allowPrivilegeEscalation":true,"runAsUser":0}}]}}}}'
    oc patch dc/share -n $project --type=merge -p $patchShare

    # Trigger rollout for Share
    oc rollout latest dc/share -n $project

    # Wait for Share to finish rolling out and starting
    Write-Host "[INFO] Waiting for Share pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/share -n $project --timeout=300s
}


# --- STAGE 3: Core Alfresco & Search Services ---
if ($PODS -contains "Routes") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOROUTES\'
    Write-Host "[INFO] Starting recreation of Alfresco routes..." -ForegroundColor Cyan
    Write-Host "[INFO] Deploying individual route manifests..." -ForegroundColor Cyan
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Retrieving generated route URLs..." -ForegroundColor Yellow
    oc get routes -l app=alfresco -n $project
}


Write-Host "[SUCCESS] Complete Alfresco enterprise stack deployment sequence finished!" -ForegroundColor Green