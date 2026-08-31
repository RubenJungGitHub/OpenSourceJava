#------------------------------------------------------------------
$token  = 'sha256~Akt2sR_y1ii545gfs9V2MZcoe1rcGle9iKP0rms_Ub0'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server
oc delete deployment vocbench -n contain-dvl
oc delete pvc vocbench-pvc-new -n contain-dvl
oc apply -f D:\contAIn\OpenSourceJava\OpenShift\manifests\ILSTools\VocBench3\
oc logs deployment/vocbench -n contain-dvl --tail=100 -f