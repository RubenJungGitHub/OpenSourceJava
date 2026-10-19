


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


#Get Pod tail logs 
cls
$PodName = "Alfresco"
oc get pods -l io.kompose.service=$PodName
$pod = (oc get pods --no-headers | Where-Object { $_ -match $podname -and $_ -notmatch "deploy|build" } | Select-Object -First 1).Split()[0]; oc logs $pod --tail=200


#REDEPLOY ALFRESCO WITH NEW CONFIG MANIFEST

CD D:\contAIn\OpenSourceJava\OpenShift\manifests\Alfresco
oc apply -f alfresco-deploymentconfig.yaml -n contain-dvl
#Deletion of old pod will initiate new opod creation on updated manifest
 oc delete pod -l io.kompose.service=alfresco -n contain-dvl
oc logs -f dc/alfresco -n contain-dvl | Select-String -Pattern "event.subsystem", "events.repo", "Updated"
oc get pods -n contain-dvl --no-headers | Where-Object { $_ -match '-deploy\s' } | ForEach-Object {
    $podName = ($_.Trim() -split '\s+')[0]
    Write-Host "Deleting $podName..."
    oc delete pod $podName -n contain-dvl
}

#List env variables
oc exec dc/alfresco -n contain-dvl -- env | Select-String JAVA_OPTS


oc scale dc/alfrescoqueuepoller --replicas=0 -n contain-dvl
oc scale dc/alfrescoqueuepoller --replicas=1 -n contain-dvl