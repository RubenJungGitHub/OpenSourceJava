$quayUser = "alfresco+partner_hsi37983_atos_nederland_bv"
$quayPass = "CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G"



#------------------------------------------------------------------
$token  = 'sha256~bz1St_C8qKLTT5kT9zanScjL_bsEgre9vL-8PhnWSg4'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

$localTag   = "latest"               # Local Docker tag


cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server

# Instellingen
$pvcYamlPath = "D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco\shared-file-store-volume-persistentvolumeclaim.yaml"
$dcName = "shared-file-store"

Write-Host "=== Stap 1: Zorg dat we in project contain-dvl zitten ===" -ForegroundColor Cyan
oc project contain-dvl

Write-Host "=== Stap 2: Schaal DeploymentConfig '$dcName' omlaag naar 0 ===" -ForegroundColor Cyan
oc scale dc/$dcName --replicas=0

Write-Host "=== Stap 3: Wacht even totdat de pod volledig is gestopt ===" -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host "=== Stap 4: Verwijder de oude Persistent Volume Claim ===" -ForegroundColor Cyan
oc delete -f $pvcYamlPath --ignore-not-found=true

Write-Host "=== Stap 5: Pas de nieuwe PVC toe (met 20Gi capaciteit) ===" -ForegroundColor Green
oc apply -f $pvcYamlPath

Write-Host "=== Stap 6: Schaal DeploymentConfig '$dcName' weer omhoog naar 1 ===" -ForegroundColor Cyan
oc scale dc/$dcName --replicas=1

Write-Host "=== Klaar! De PVC is vernieuwd en de service is herstart. ===" -ForegroundColor Green