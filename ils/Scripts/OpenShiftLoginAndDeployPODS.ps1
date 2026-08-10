$server= 'https://api.ocp0.anders-nogic.atos.net:6443'
$token = 'sha256~rkPPNZ6-XeP0sPwhsgTMxSP3ikdt5yM8OK1Y3cf4xzE'
$Project = 'contain-dvl'
$folder = "";
oc login --token=$token --server=$server


$PODS = @(
    #"Postress",
    "Alfresco"
    #"SubcriberService",
    #"NotificationService"
)


if ($PODS -contains "Postgress" -or $PODS -contains "postgres") {
    $folder = 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\ILSTools\Postgres'
    Write-Host "[INFO] PostgreSQL found in collection. Starting recreation..." -ForegroundColor Cyan
    $folder = 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\AFFRESCOPOSTGRESSFIRST\'
    Write-Host "[INFO] Alfresco found in collection. Starting recreation of postgress..." -ForegroundColor Cyan
    # Delete existing resources first (ignoring PVCs to preserve data, or including them if you want a total factory reset)
    oc delete -R -f $folder -n contain-dvl --ignore-not-found
    oc apply -R -f $folder -n $project

    Write-Host "[INFO] Waiting for PostgreSQL pod to become ready..." -ForegroundColor Yellow
    # Wait until the postgres deployment rollout succeeds
    oc rollout status dc/postgres -n $project --timeout=120s

    oc port-forward deployment/postgres 5432:5432 -n $project


}


if ($PODS -contains "Alfresco" -or $PODS -contains "alfresco") {


    $folder = 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Alfresco found in collection. Starting recreation..." -ForegroundColor Cyan
    #Recreate ALfresco
    
    

    # Delete existing resources first (ignoring PVCs to preserve data, or including them if you want a total factory reset)
    oc delete -R -f $folder -n contain-dvl --ignore-not-found
    # Apply fresh manifests
    oc apply -R -f $folder -n $project


}
