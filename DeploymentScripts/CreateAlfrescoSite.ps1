# --- Configuration ---
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }
$serverUrl = "https://alfresco-proxy-route-contain-dvl.apps.ocp0.anders-nogic.atos.net/alfresco"
$username  = "admin"
$password  = "admin"

#$sitesToProcess = @("ontobind", "ontobindarchive")
$sitesToProcess = @("Test1")

$folderCollection = @("Secret", "Internal", "Unclassified", "Confidential")

# --- 1. Get Authentication Ticket (Outside the loop so it runs once) ---
Write-Host "Requesting Authentication Ticket..." -ForegroundColor Cyan
$loginUrl  = "$serverUrl/api/-default-/public/authentication/versions/1/tickets"
$loginBody = @{
    userId   = $username
    password = $password
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
    $ticket = $loginResponse.entry.id
    Write-Host "Ticket Acquired Successfully: $ticket" -ForegroundColor Green
} catch {
    Write-Error "Login Failed: $($_.Exception.Message)"
    exit
}

foreach ($siteId in $sitesToProcess) {
    Write-Host "--- Processing Site: $siteId ---" -ForegroundColor Cyan

    try {
        $loginResponse = Invoke-RestMethod -Uri $loginUrl -Method Post -Body $loginBody -ContentType "application/json"
        $ticket = $loginResponse.entry.id
        Write-Host "Ticket Acquired: $ticket" -ForegroundColor Green
    } catch {
        Write-Error "Login Failed: $($_.Exception.Message)"; exit
    }

    # --- 2. Cleanup: Delete and Purge existing Site ---
    $cleanId = $siteId.ToLower() 
    Write-Host "Cleaning up existing site: $cleanId..." -ForegroundColor Yellow
    #$deleteUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites/$cleanId`?alf_ticket=$ticket"

   $trashUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/deleted-nodes?alf_ticket=$ticket"
   $trashList = Invoke-RestMethod -Uri $trashUrl -Method Get

    # 2. Find the entry where the name matches your site
    $target = $trashList.list.entries | Where-Object { $_.entry.name -eq $siteId.ToLower() }

    try {
        
        if ($target) {
            $purgeUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/deleted-nodes/$($target.entry.id)?alf_ticket=$ticket"
            Invoke-RestMethod -Uri $purgeUrl -Method Delete
            Write-Host "Purged '$siteId' using Node ID: $($target.entry.id)" -ForegroundColor Green
        }
    }
    catch 
    {
        Write-Host "Cleanup: No existing site found." -ForegroundColor Gray 
    }

    # --- 3. Create the "Naked" Site ---
    Write-Host "Creating site: $cleanId..." -ForegroundColor Cyan
    $createSiteUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites?alf_ticket=$ticket"
    $siteBody = @{
        id          = $cleanId
        title       = $siteId
        description = "Created via PowerShell"
        visibility  = "PUBLIC"
    } | ConvertTo-Json -Depth 10

    try {
        $siteResponse = Invoke-RestMethod -Uri $createSiteUrl -Method Post -Body $siteBody -ContentType "application/json"
        Write-Host "SUCCESS: Site Created!" -ForegroundColor Green
    } catch {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        Write-Host "ALFRESCO ERROR: $($reader.ReadToEnd())" -ForegroundColor Red
        exit
    }

    # --- 4. Add Membership (Handling the 409 Conflict) ---
    Write-Host "Ensuring admin is SiteManager..." -ForegroundColor Cyan
    $memberUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites/$cleanId/members?alf_ticket=$ticket"
    $memberBody = @{ id = "admin"; role = "SiteManager" } | ConvertTo-Json
    try {
        Invoke-RestMethod -Uri $memberUrl -Method Post -Body $memberBody -ContentType "application/json"
        Write-Host "Admin added successfully." -ForegroundColor Green
    } catch {
        if ($_.Exception.Message -like "*409*") {
            Write-Host "Note: Admin is already a member (skipping conflict)." -ForegroundColor Gray
        } else {
            Write-Warning "Membership Issue: $($_.Exception.Message)"
        }
    }

    # --- 5. Register the documentLibrary Container ---
    # THIS PART FIXES THE GUI VISIBILITY
    Write-Host "Registering 'documentLibrary' container..." -ForegroundColor Cyan
    $containerUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites/$cleanId/containers?alf_ticket=$ticket"
    $containerBody = @{ id = "documentLibrary" } | ConvertTo-Json

    $docLibId = $null
    try {
        $cResponse = Invoke-RestMethod -Uri $containerUrl -Method Post -Body $containerBody -ContentType "application/json"
        $docLibId = $cResponse.entry.id
        Write-Host "SUCCESS: Container ID Acquired: $docLibId" -ForegroundColor Green
    } catch {
        Write-Warning "Container registration noted a conflict or error. Fetching existing ID..."
        $containerData = Invoke-RestMethod -Uri $containerUrl -Method Get
        $docLibId = ($containerData.list.entries | Where-Object { $_.entry.folderId -eq "documentLibrary" }).entry.id
    }


    function CreateFolderRuleNOTWORKING {
        param (
            [string]$folderId
        )
    # 1. Use the ID of the specific folder you created (e.g., 'Secret' or 'Internal')
    # Let's assume you have a list of folder IDs from your previous loop

        $targetFolderId = $folderid
        #$ruleUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/nodes/$targetFolderId/rules?alf_ticket=$ticket"
        #$ruleUrl = "$serverUrl/api/-default-/private/alfresco/versions/1/nodes/$targetFolderId/rules?alf_ticket=$ticket"
        # We use 'workspace' and 'SpacesStore' as the standard storage locations
        $ruleUrl = "$serverUrl/s/api/node/workspace/SpacesStore/$targetFolderId/rules?alf_ticket=$ticket"


        # 2. Fix the type-name format (Must use prefix:name)
        $ruleBody = @{
            title = "Auto-Type Specialization" 
            description = "Changes the type to contain document"
            enforced = $true
            executeAsynchronously = $false 
            ruleType = "inbound"
        
            action = @{
                actionDefinitionId = "specialize-type"
                parameterValues = @{
                    # Alfresco expects "prefix:name". 
                    # If your model is 'contain', it is likely 'contain:containdocumentument' or 'cnt:document'
                    "type-name" = "contain:containdocument" 
                }
            }
        } | ConvertTo-Json -Depth 10

        Write-Host "Attaching rule to folder: $($folder.name) ($targetFolderId)..." -ForegroundColor Cyan

        try {
            $response = Invoke-RestMethod -Uri $ruleUrl -Method Post -Body $ruleBody -ContentType "application/json" -Proxy $null
            Write-Host "SUCCESS: Rule active on $($folder.name)" -ForegroundColor Green
        }
        catch {
            $errObj = $_.Exception.Response.GetResponseStream()
            if ($null -ne $errObj) {
                $reader = New-Object System.IO.StreamReader($errObj)
                Write-Host "Rule creation failed: $($reader.ReadToEnd())" -ForegroundColor Red
            } else {
                Write-Host "Rule creation failed: $($_.Exception.Message)" -ForegroundColor Red
            }
        }
    }


    function CreateFolderRuleOneTime {
        param (
            [string]$targetFolderId
        )
    # --- THE FINAL ATTEMPT: UNIVERSAL FEATURE ACTION ---
    $v1ActionUrl = "$($serverUrl)/api/-default-/public/alfresco/versions/1/action-executions?alf_ticket=$($ticket)"

    # We use 'add-features' which is a core Alfresco action
    # The 'aspect-name' parameter is used even for types in many ReST versions
    $finalBody = @{
        actionDefinitionId = "add-features"
        targetId = $targetFolderId
        params = @{
            "aspect-name" = "contain:containdocument"
        }
    } | ConvertTo-Json -Depth 10

    Write-Host "Attempting Universal Feature Injection..." -ForegroundColor Cyan

    try {
        $response = Invoke-RestMethod -Uri $v1ActionUrl -Method Post -Body $finalBody -ContentType "application/json"
        Write-Host "SUCCESS: Feature 'containdocument' applied to node!" -ForegroundColor Green
    }
    catch {
        # If this fails, we extract the list of valid names directly from your server
        Write-Host "Failed. Determining valid IDs from your server..." -ForegroundColor Yellow
    
        $listUrl = "$($serverUrl)/api/-default-/public/alfresco/versions/1/action-definitions?alf_ticket=$($ticket)"
        $validActions = Invoke-RestMethod -Uri $listUrl -Method Get
        $match = $validActions.list.entries.entry | Where-Object { $_.id -like "*specialize*" -or $_.id -like "*type*" }
    
        if ($match) {
            Write-Host "FOUND IT! Your server uses the ID: $($match.id)" -ForegroundColor Green
            # You can now swap 'specialize-type' for whatever name just printed out.
        } else {
            Write-Host "No specialization actions are exposed to the ReST API." -ForegroundColor Red
            Write-Host "Available action IDs start with: $(($validActions.list.entries.entry.id | Select-Object -First 5) -join ', ')" -ForegroundColor Gray
        }
    }
    }



    function CreateFolderRule {
        param (
            [string]$targetFolderId
        )
    # This mimics the EXACT internal call made by the Alfresco Share Document Library
    $shareActionUrl = "$($serverUrl)/s/slingshot/doclib/action/node/workspace/SpacesStore/$($targetFolderId)?alf_ticket=$($ticket)"

    $shareBody = @{
        actionDefinitionId = "specialize-type"
        parameterValues = @{
            "type-name" = "contain:containdocument"
        }
    } | ConvertTo-Json

    Write-Host "Trying the Share-Native Backdoor..." -ForegroundColor Cyan

    try {
        $response = Invoke-RestMethod -Uri $shareActionUrl -Method Post -Body $shareBody -ContentType "application/json"
        Write-Host "SUCCESS: The Share-Native API accepted the command!" -ForegroundColor Green
    }
    catch {
        Write-Host "Even the Share-Native API is blocked. This is a highly hardened environment." -ForegroundColor Red
    }
    }


# --- 3. Create a Folder inside the Site ---
$folderName = "Project-Documents"
Write-Host "Creating folder: $folderName..." -ForegroundColor Cyan

# 1. Define your folder collection
$folderCollection = @(
    "Secret",
    "Internal",
    "Unclassified",
    "Confidential"
)
$internalDocLibId ="";
# 2. Define the Base API URL for the Site's Document Library
$baseDocLibUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites/$siteId/containers/documentLibrary/children?alf_ticket=$ticket"

Write-Host "Starting bulk folder creation for site: $siteId" -ForegroundColor Cyan
Write-Host "--------------------------------------------------"
# 1. Ask the site for its containers
$containersUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/sites/$siteId/containers?alf_ticket=$ticket"

try {
    $containerData = Invoke-RestMethod -Uri $containersUrl -Method Get -Proxy $null
    
    # Find the 'documentLibrary' entry
    $docLib = $containerData.list.entries | Where-Object { $_.entry.folderId -eq "documentLibrary" -or $_.entry.id -eq "documentLibrary" }

    if ($null -ne $docLib) {
        # This is the real, internal ID we need
        $internalDocLibId = $docLib.entry.id
        Write-Host "Confirmed Document Library ID: $internalDocLibId" -ForegroundColor Green
        CreateFolderRule $internalDocLibId


        # 2. Use THIS ID to create your folders
        $folderBaseUrl = "$serverUrl/api/-default-/public/alfresco/versions/1/nodes/$internalDocLibId/children?alf_ticket=$ticket"
        
        foreach ($f in $folderCollection) {
            $body = @{ name = $f; nodeType = "cm:folder" } | ConvertTo-Json
            Invoke-RestMethod -Uri $folderBaseUrl -Method Post -Body $body -ContentType "application/json" -Proxy $null
            Write-Host "Created Folder: $f" -ForegroundColor Gray
        }
    }
}
catch {
    Write-Error "Failed to find or access the documentLibrary: $($_.Exception.Message)"
}
}

Write-Host "--- SCRIPT COMPLETE ---" -ForegroundColor Green
Write-Host "Check the Share GUI. '$cleanId' should now be selectable for rules."