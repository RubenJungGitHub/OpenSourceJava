$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"
$token  = 'sha256~Ef6Iy10zbJSomF27ubQApUZngRyHBT7vZ1VTghkZz_k'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"
#------------------------------------------------------------------

# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server


Write-Host "[INFO] Logging into Quay.io..." -ForegroundColor Cyan
$quayPass | docker login quay.io -u $quayUser --password-stdin

# 3. Create the secret natively using OpenShift's registry secret generator (so the Web Console shows the username/server correctly)
Write-Host "[INFO] Creating/updating OpenShift image pull secret..." -ForegroundColor Cyan
oc create secret docker-registry alfrescoquayio `
  --docker-server=quay.io `
  --docker-username=$quayUser `
  --docker-password=$quayPass `
  -n $project --dry-run=client -o yaml | oc apply -f -

# 4. Link the secret to the default service account for pulling images
oc secrets link default alfrescoquayio --for=pull -n $project
oc patch sa default --type=json -p='[{"op": "add", "path": "/imagePullSecrets/-", "value": {"name": "alfrescoquayio"}}]' -n $project 2>$null

$PODS = @(
    #"Postress",
    #"ActiveMQ",
    "Alfresco"
)

if ($PODS -contains "Postress" -or $PODS -contains "postgres") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\AFFRESCOPOSTGRESSFIRST\'
    Write-Host "[INFO] Alfresco found in collection. Starting recreation of postgres..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for PostgreSQL pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/postgres -n $project --timeout=120s
}

# Force the share deployment config to run explicitly as root (UID 0)
$patch = '{"spec":{"template":{"spec":{"securityContext":{"runAsUser":0},"containers":[{"name":"share","securityContext":{"allowPrivilegeEscalation":true,"runAsUser":0}}]}}}}'
oc patch dc/share -n $project --type=merge -p $patch

if ($PODS -contains "ActiveMQ") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\ALFRESCOACTIVEMQFIRST\'
    Write-Host "[INFO] ActiveMQ found in collection. Starting recreation of ActiveMQ..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for ActiveMQ pod to become ready..." -ForegroundColor Yellow
    oc rollout status dc/activemq -n $project --timeout=120s
}



if ($PODS -contains "Alfresco" -or $PODS -contains "alfresco") {
    $folder = 'D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Alfresco found in collection. Starting recreation..." -ForegroundColor Cyan
    
    oc delete -R -f $folder -n $project --ignore-not-found
    oc apply -R -f $folder -n $project

    # Force the deployment config template to explicitly recognize the pull secret
    $patch = '{"spec":{"template":{"spec":{"imagePullSecrets":[{"name":"alfrescoquayio"}]}}}}'
    oc patch dc/alfresco -n $project --type=merge -p $patch 2>$null
    oc patch dc/share -n $project --type=merge -p $patch 2>$null

    # Trigger rollout
    oc rollout latest dc/alfresco -n $project
    oc rollout latest dc/share -n $project
}