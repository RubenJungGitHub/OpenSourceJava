
#------------------------------------------------------------------
$token  = 'sha256~rFhYDrItm6LsQJxMi6vuPoawo4SNITpMlHt4hBb94go'
$server = 'https://api.ocp0.anders-nogic.atos.net:6443'
$project = "contain-dvl"

cls
# 1. Log into OpenShift FIRST so all subsequent 'oc' commands target the correct cluster and context
Write-Host "[INFO] Logging into OpenShift cluster..." -ForegroundColor Cyan
oc login --token=$token --server=$server

$project = "contain-dvl"
#List all pods in project
oc get pods -n contain-dvl
$manifestPath = "C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\Alfresco\alfresco-deploymentconfig.yaml"


#Restart POD.
oc scale dc/alfresco --replicas=0 -n contain-dvl
#Wait 5 secs 
Start-Sleep -Seconds 5
oc scale dc/alfresco --replicas=1 -n contain-dvl


# 1. Delete the incorrect secret
#oc delete secret alfrescoquayio -n $project --ignore-not-found=true

# 2. Recreate the secret precisely using your working local Docker config
#oc create secret generic alfrescoquayio `
#  --from-file=.dockerconfigjson="$HOME\.docker\config.json" `
#  --type=kubernetes.io/dockerconfigjson `
#  -n $project

oc get deployment alfresco -n contain-dvl

#oc create secret docker-registry alfrescoquayiocredentials `
#  --docker-server=quay.io `
#  --docker-username="alfresco+partner_hsi37983_atos_nederland_bv" `
#  --docker-password="CA2YQB59N337ZN642NHK6QO3ZJV04JOL7JOC3C0HB6UCVXP10E94C9TWF18UQI8G" `
#  -n $project --dry-run=client -o yaml | oc apply -f -

# 2. Ensure the secret is explicitly linked for image pulling on both service accounts
oc secrets link default alfrescoquayiocredentials --for=pull -n $project
oc secrets link builder alfrescoquayiocredentials --for=pull -n $project 2>$null

# 3. Patch the default service account so it automatically includes this secret for any new pods
#oc patch sa default --type=json -p="[{`"op`"": `"add`", `"path`": `/imagePullSecrets/-`, `"value`": {`"name`": `"alfrescoquayio`"}}]" -n $project 2>$null


# 4. Clean up the failed deployment config and pods
oc delete dc/alfresco -n $project --ignore-not-found=true

# 5. Re-apply the manifest and trigger a clean rollout
oc secrets link default alfrescoquayio --for=pull -n $project
oc rollout latest dc/alfresco -n $project
oc get pods -l io.kompose.service=alfresco -n $project -w



# 1. Check if the deployment controller created the new deployment version yet
oc get dc alfresco -n $project

# 2. Check for any deployer pods currently running or completed
oc get pods -n $project --selector=openshift.io/deployer-pod-name.name=alfresco

# 3. Check events in the namespace to see if it's struggling to pull or schedule
oc get events -n $project --sort-by='.metadata.creationTimestamp' | Select-Object -Last 15


PS D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco> oc get pod -l io.kompose.service=alfresco -n contain-dvl -o jsonpath="{.items[0].spec.containers[0].ports[*].containerPort}"


# 1. Get the latest Share pod name and store it in a variable and check repos connectivity.
$sharePod = (oc get pods -n contain-dvl -l io.kompose.service=share --sort-by='.metadata.creationTimestamp' -o jsonpath="{.items[-1].metadata.name}")
# 2. Display the pod name
Write-Host "[INFO] Latest Share Pod: $sharePod" -ForegroundColor Cyan

# 3. List the environment variables for that pod
oc set env pod/$sharePod --list -n contain-dvl

oc exec $sharePod -n contain-dvl -- curl -v http://alfresco:8082/alfresco/s/api/server

oc exec $sharePod -n contain-dvl -- curl --max-time 5 -I http://alfresco:8082/alfresco/

#Check KIE containers
$kiePod = (oc get pods -n contain-dvl -l io.kompose.service=kie-server --no-headers | Select-Object -First 1).Split()[0]
oc exec $kiePod -n contain-dvl -- curl -s -u admin:admin http://localhost:8080/kie-server/services/rest/server/containers


#Get Pod tail logs 
cls
#$PodName = "kie-server"
#$PodName = "Alfresco"
#$PodName = "alfrescoqueuepoller-1-c8rjw"
#$PodName = "migrationqueuepoller-1-k9z87"
#$POdName = "activemq-1-glwfv"
$POdName = "receiver-1-8x87d"
oc project "contain-dvl"
#$PodName = "Share"

Write-Host "[INFO] Searching for pod matching: $PodName..." -ForegroundColor Yellow

# Find the first running pod matching your search term (ignoring deploys/builds)
$podLine = (oc get pods --no-headers -n contain-dvl | Where-Object { $_ -match $PodName -and $_ -notmatch "deploy|build" } | Select-Object -First 1)

if ($podLine) {
    $pod = $podLine.Split()[0]
    Write-Host "[INFO] Found pod: $pod" -ForegroundColor Cyan
    Write-Host "----------------------------------------"
    oc logs $pod --tail=100 -n contain-dvl
} else {
    Write-Host "[ERROR] No matching running pod found for '$SearchTerm'." -ForegroundColor Red
}

oc get pods -l io.kompose.service=$PodName
$pod = (oc get pods --no-headers | Where-Object { $_ -match $podname -and $_ -notmatch "deploy|build" } | Select-Object -First 1).Split()[0]; oc logs $pod --tail=100
Write-Host "[INFO] Found pod ... " $pod -ForegroundColor Cyan

oc describe deployment $pod -n contain-dvl
oc get events -n contain-dvl --sort-by='.metadata.creationTimestamp'
oc get routes -n contain-dvl



#Restart pod
cls
$PodName = "alfrescoqueuepoller-1-c8rjw"
oc get pods -l io.kompose.service=$PodName

# 1. View logs of the current active pod first
$pod = (oc get pods --no-headers -l io.kompose.service=$PodName | Where-Object { $_ -match "Running" } | Select-Object -First 1).Split()[0]
if ($pod) { oc logs $pod --tail=100 }

# 2. Trigger a proper rollout restart on the parent DeploymentConfig
oc rollout restart dc/$PodName -n contain-dnv


oc get imagestreams -n contain-dvl