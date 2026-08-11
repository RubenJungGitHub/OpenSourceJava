# Define base output directory
$manifestsBase = "C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests"

# Define all subfolders that need to exist
$folders = @(
    "$manifestsBase\Alfresco",
    "$manifestsBase\ILSTools\Redis",
    "$manifestsBase\ILSTools\MongoDB",
    "$manifestsBase\ILSTools\RHBC",
    "$manifestsBase\ILSTools\RHKIE"
    "$manifestsBase\ILS\Receiver"
    "$manifestsBase\Sharepoint\Subscriptionservice"
    "$manifestsBase\Sharepoint\NotificationService"
    "$manifestsBase\ILSqueuepollers\Alfrescoqueuepoller"
    "$manifestsBase\ILSqueuepollers\Sharepointqueuepoller"
    "$manifestsBase\ILSqueuepollers\Migrationqueuepoller"
    "$manifestsBase\ILS\Taxonomy\TaxonomyCahcer"
    "$manifestsBase\ILS\TaxonomyService\TaxonomyService"
    "$manifestsBase\ILS\Receiver"
    "$manifestsBase\ILS\UUIDUTIL"
)

# Ensure all directories exist (creates them if missing, does nothing if they already exist)
foreach ($folder in $folders) {
    if (!(Test-Path $folder)) {
        New-Item -ItemType Directory -Force -Path $folder | Out-Null
    }
}

Write-Host "Directories verified. Starting Kompose conversions..." -ForegroundColor Cyan

# 1. Alfresco Conversion
d:\tools\kompose convert -f "C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\compose-withvolume-BaseForOpenShift.yml" --provider openshift -o "$manifestsBase\Alfresco\"

# 2. Redis Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\Scripts\docker-postgress-redis-activemq-mongodb-RHBAP\docker-compose-#4_PG_redis.yml" --provider openshift -o "$manifestsBase\ILSTools\Redis\"

# 3. MongoDB Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\Scripts\docker-postgress-redis-activemq-mongodb-RHBAP\docker-compose-#3_mongodb.yml" --provider openshift -o "$manifestsBase\ILSTools\MongoDB\"

# 4. RHBAP-BC Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\Scripts\docker-postgress-redis-activemq-mongodb-RHBAP\docker-compose-#1_RHBAP-BC.yml" --provider openshift -o "$manifestsBase\ILSTools\RHBC\"

# 5. RHBAP-KIE Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\Scripts\docker-postgress-redis-activemq-mongodb-RHBAP\docker-compose-#2_RHBAP-KIE.yml" --provider openshift -o "$manifestsBase\ILSTools\RHKIE\"


# 6. Sharepoint queuepoller Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\sharepointqueuepoller\docker-compose.yml" --provider openshift -o "$manifestsBase\ILSqueuepollers\Sharepointqueuepoller\"


# 7. Alfresco queuepoller Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\alfrescoqueuepoller\docker-compose.yml" --provider openshift -o "$manifestsBase\ILSqueuepollers\Alfrescoqueuepoller\"


# 8. Migration queuepoller Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\migrationqueuepoller\docker-compose.yml" --provider openshift -o "$manifestsBase\ILSqueuepollers\Migrationqueuepoller\"


# 9. Sharepoint subscription Conversion
d:\tools\kompose convert -f "D:\contAIn\SharePointHandler\Sharepoint\subscriber-service\docker-compose.yaml" --provider openshift -o "$manifestsBase\Sharepoint\Subscriptionservice\"


# 10. Sharepoint notification Conversion
d:\tools\kompose convert -f "D:\contAIn\SharePointHandler\Sharepoint\notification-service\docker-compose.yaml" --provider openshift -o "$manifestsBase\Sharepoint\NotificationService\"



# 11. Taxonomy cacher Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\taxonomycacher\docker-compose.yaml" --provider openshift -o "$manifestsBase\ILS\Taxonomy\Taxonomycacher\"


# 12. Taxonomy service Conversion
d:\tools\kompose convert -f "D:\contAIn\taxonomyservice\taxonomy-controllers\docker-compose.yml" --provider openshift -o "$manifestsBase\ILS\Taxonomy\Taxonomyservice\"


# 13. UUIDUTIL  service Conversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\uuidutil\docker-compose.yml" --provider openshift -o "$manifestsBase\ILS\UUIDUTIL\"


# 14. ReceiverConversion
d:\tools\kompose convert -f "D:\contAIn\OpenSourceJava\ils\receiver\docker-compose.yml" --provider openshift -o "$manifestsBase\ILS\Receiver\"


Write-Host "All conversions completed successfully!" -ForegroundColor Greenrovider openshift -o C:\Users\nl07428\Desktop\contAIn\alfresco-university\envs\sandbox\acs\developer\OpenShift\manifests\ILSTools\RHKIE\