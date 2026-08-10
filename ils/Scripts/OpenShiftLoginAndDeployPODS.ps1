$server= 'https://api.ocp0.anders-nogic.atos.net:6443'
$token = 'sha256~rkPPNZ6-XeP0sPwhsgTMxSP3ikdt5yM8OK1Y3cf4xzE'
$Project = 'contain-dvl'
$Folder = "";
oc login --token=$token --server=$server


$PODS = @(
    "Postress",
    "Alfresco"
    #"SubcriberService",
    #"NotificationService"
)


if ($PODS -contains "Postgress" -or $PODS -contains "postgres") {
    $folder = 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\ILSTools\Postgres'
    Write-Host "[INFO] PostgreSQL found in collection. Starting recreation..." -ForegroundColor Cyan
    #Recreate Postgres
    # 1. Delete old PVC
    Write-Host "-> Deleting PVC: postgress" -ForegroundColor Yellow
    oc delete -R -f $folder -n $project --ignore-not-found --selector!=persistentvolumeclaim
    #oc delete pvc postgress-pvc -n contain-dvl --ignore-not-found
    # 2. Apply fresh PVC YAML manifest
    #Write-Host "-> Applying PostgreSQL PVC manifest..." -ForegroundColor Yellow
  #  oc apply -f 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\ILSTools\Postgres\OpenShiftPostgressPVC.yaml' -n contain-dvl
   # Write-Host "-> Deleting existing PostgreSQL pod..." -ForegroundColor Yellow
   # oc delete pod -l app=postgres -n contain-dvl --ignore-not-found
    Write-Host "[SUCCESS] PostgreSQL recreation commands executed." -ForegroundColor Green
  #  oc apply -R -f $folder -n $project
    Write-Host "[INFO] Starting port-forward for PostgreSQL..." -ForegroundColor Cyan
  #  oc port-forward deployment/postgres 5432:5432 -n $project

}


if ($PODS -contains "Alfresco" -or $PODS -contains "alfresco") {
    $folder = 'C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\Alfresco\'
    Write-Host "[INFO] Alfresco found in collection. Starting recreation..." -ForegroundColor Cyan
    #Recreate ALfresco
    
    

    # Delete existing resources first (ignoring PVCs to preserve data, or including them if you want a total factory reset)
    oc delete -R -f C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\Alfresco\ -n contain-dvl --ignore-not-found

    # Apply fresh manifests
    oc delete -R -f C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\Alfresco\ -n contain-dvl --ignore-not-found

    oc apply -R -f $folder -n $project
    oc port-forward deployment/postgres 5432:5432 -n $project
}
