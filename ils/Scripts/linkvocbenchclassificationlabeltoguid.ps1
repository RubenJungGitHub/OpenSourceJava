$PSVersionTable.PSVersion


#DIT WERKT PAS VANAF PW 7!!!!!
# 1. Verbind als Admin
$conn = Connect-PnPOnline -Url "https://lls6.sharepoint.com/sites/SP-EventReceivers-Test" -ClientId "a9fe380e-3b39-4368-8cef-a9851829e3d0" -Tenant "9a1b5f77-1f1a-40ac-b1a1-38617300f02a" -Interactive -ReturnConnection

# 2. Check of NoScript aanstaat
$site = Get-PnPTenantSite -Url "https://lls6.sharepoint.com/sites/SP-EventReceivers-Test"
Write-Host "NoScript status: $($site.DenyAddAndCustomizePages)"

# 3. Indien "Enabled", zet hem uit (DIT IS DE OPLOSSING VOOR VEEL GEEF-FOUTEN)
if ($site.DenyAddAndCustomizePages -eq "Enabled") {
    Set-PnPTenantSite -Url "https://lls6.sharepoint.com/sites/SP-EventReceivers-Test" -NoScriptSite:$false
    Write-Host "NoScript uitgeschakeld, even 10 minuten wachten..."
}

$myGuid = [guid]"0fa2753e-5f6f-4816-b00e-a8c280681a87" 

# 2. Update het veld via de actieve verbinding
Set-PnPField -Identity "VocBenchClassificationLabel" -Connection $conn -Values @{ClientSideComponentId = $myGuid}

# 3. Forceer de update in de SharePoint database
$field = Get-PnPField -Identity "VocBenchClassificationLabel" -Connection $conn
$field.UpdateAndPushChanges($true)
Invoke-PnPQuery -Connection $conn

# 4. Controleer of het nu wel goed staat
$field = Get-PnPField -Identity "VocBenchClassificationLabel" -Connection $conn
$field | Select-Object Title, InternalName, ClientSideComponentId

pause