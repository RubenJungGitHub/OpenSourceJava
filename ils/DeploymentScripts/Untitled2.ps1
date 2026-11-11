cls
#------------------------------------------------------------------
$token  = 'sha256~bz1St_C8qKLTT5kT9zanScjL_bsEgre9vL-8PhnWSg4'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

oc login --token=$token --server=$server
oc project $project
$podName = (oc get pods --selector=app=alfrescoqueuepoller -o jsonpath="{.items[0].metadata.name}")
Write-Host "Gevonden pod: $podName" -ForegroundColor Cyan
oc exec $podName -- cat /proc/1/cmdline