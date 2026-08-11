
#Run from VM
docker login quay.io

$project = "contain-dvl"

oc create secret generic alfrescoquayio `
  --from-file=.dockerconfigjson="$HOME\.docker\config.json" `
  --type=kubernetes.io/dockerconfigjson `
  -n $project --dry-run=client -o yaml | oc apply -f -