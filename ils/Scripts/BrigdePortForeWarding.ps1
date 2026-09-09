Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/mongodb 27017:27017 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/taxonomyservice 53000:53000 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/notificationservice 5001:5001 -n contain-dvl"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "oc port-forward svc/kie-server 57000:57000 -n contain-dvl"
