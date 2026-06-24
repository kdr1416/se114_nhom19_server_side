# End-to-end integration test script for Cafe Manager Shift Management APIs

$baseUrl = "http://localhost:8080"
$utf8 = [System.Text.Encoding]::UTF8

function Post-Json {
    param($url, $body, $headers)
    $bodyJson = $body | ConvertTo-Json -Compress
    $bodyBytes = $utf8.GetBytes($bodyJson)
    $allHeaders = @{ "Content-Type" = "application/json; charset=utf-8" }
    if ($headers) {
        foreach ($k in $headers.Keys) { $allHeaders[$k] = $headers[$k] }
    }
    return Invoke-RestMethod -Uri $url -Method Post -Headers $allHeaders -Body $bodyBytes
}

function Put-Json {
    param($url, $body, $headers)
    $bodyJson = $body | ConvertTo-Json -Compress
    $bodyBytes = $utf8.GetBytes($bodyJson)
    $allHeaders = @{ "Content-Type" = "application/json; charset=utf-8" }
    if ($headers) {
        foreach ($k in $headers.Keys) { $allHeaders[$k] = $headers[$k] }
    }
    return Invoke-RestMethod -Uri $url -Method Put -Headers $allHeaders -Body $bodyBytes
}

function Get-Json {
    param($url, $headers)
    $allHeaders = @{}
    if ($headers) {
        foreach ($k in $headers.Keys) { $allHeaders[$k] = $headers[$k] }
    }
    return Invoke-RestMethod -Uri $url -Method Get -Headers $allHeaders
}

function Delete-Json {
    param($url, $headers)
    $allHeaders = @{}
    if ($headers) {
        foreach ($k in $headers.Keys) { $allHeaders[$k] = $headers[$k] }
    }
    return Invoke-RestMethod -Uri $url -Method Delete -Headers $allHeaders
}

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "STARTING SHIFT MANAGEMENT INTEGRATION TEST" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$ErrorActionPreference = "Stop"

# 1. Login as Admin to setup shifts
Write-Host "1. Logging in as admin..."
$adminLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "admin"; password = "admin123" }
$adminToken = $adminLogin.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "   Admin login successful. Token acquired."

# 2. Get user profile of staff (to find staff userId)
Write-Host "2. Getting staff user details..."
$staffLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "staff"; password = "123456" }
$staffToken = $staffLogin.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
$staffMe = Get-Json "$baseUrl/api/v1/auth/me" $staffHeaders
$staffUserId = $staffMe.userId
Write-Host "   Staff user ID is: $staffUserId"

# 3. Create a shift template
Write-Host "3. Creating a shift template..."
$templateRequest = @{
    templateName = "Ca Sang Test"
    startTime = "06:00"
    endTime = "12:00"
    minStaff = 2
    isActive = $true
}
$template = Post-Json "$baseUrl/api/v1/shift-templates" $templateRequest $adminHeaders
$templateId = $template.templateId
Write-Host "   Created shift template ID: $templateId"

# 4. Create Shift 1 (06:00 - 12:00) on a unique date
# Generate a unique epoch timestamp (start of a unique day)
$epochBase = [Math]::Floor([Double](Get-Date -UFormat %s) * 1000)
$randomOffset = (Get-Random -Minimum 86400000 -Maximum 864000000) # 1 to 10 days in future
$shiftDate = $epochBase + $randomOffset
Write-Host "4. Creating Shift 1 (06:00 - 12:00) on epoch date $shiftDate..."
$shift1Request = @{
    templateId = $templateId
    shiftName = "Ca Sang Test - 2026-06-25"
    shiftDate = $shiftDate
    startTime = "06:00"
    endTime = "12:00"
}
$shift1 = Post-Json "$baseUrl/api/v1/shifts" $shift1Request $adminHeaders
$shift1Id = $shift1.shiftId
Write-Host "   Created Shift 1 ID: $shift1Id, Status: $($shift1.status)"

# 5. Create Shift 2 (11:00 - 17:00, overlapping Shift 1) on same day
Write-Host "5. Creating Shift 2 (11:00 - 17:00, overlapping Shift 1)..."
$shift2Request = @{
    templateId = $templateId
    shiftName = "Ca Chieu Test - 2026-06-25"
    shiftDate = $shiftDate
    startTime = "11:00"
    endTime = "17:00"
}
$shift2 = Post-Json "$baseUrl/api/v1/shifts" $shift2Request $adminHeaders
$shift2Id = $shift2.shiftId
Write-Host "   Created Shift 2 ID: $shift2Id, Status: $($shift2.status)"

# 6. Assign staff to Shift 1
Write-Host "6. Assigning staff (ID: $staffUserId) to Shift 1..."
$assign1 = Post-Json "$baseUrl/api/v1/shifts/$shift1Id/assign" @{ userId = $staffUserId } $adminHeaders
Write-Host "   Staff assigned successfully to Shift 1."

# 7. Try to assign staff to Shift 2 (should fail due to overlap)
Write-Host "7. Attempting to assign staff to overlapping Shift 2..."
try {
    $assign2 = Post-Json "$baseUrl/api/v1/shifts/$shift2Id/assign" @{ userId = $staffUserId } $adminHeaders
    Write-Error "Overlap check failed! Staff was incorrectly assigned to overlapping shifts."
    exit 1
} catch {
    $err = $_.Exception.Response
    Write-Host "   Overlap check passed. Assignment rejected as expected." -ForegroundColor Green
}

# 8. Publish Shift 1
Write-Host "8. Publishing Shift 1..."
$publishedShift = Put-Json "$baseUrl/api/v1/shifts/$shift1Id/publish" @{} $adminHeaders
Write-Host "   Shift 1 status: $($publishedShift.status) (Expected: PUBLISHED)"
if ($publishedShift.status -ne "PUBLISHED") {
    Write-Error "Shift status is not PUBLISHED!"
    exit 1
}

# 9. Open Shift 1 with cash (500000.0)
Write-Host "9. Opening Shift 1 with 500,000 VND cash..."
$openedShift = Put-Json "$baseUrl/api/v1/shifts/$shift1Id/open" @{ openingCash = 500000.0 } $staffHeaders
Write-Host "   Shift 1 status: $($openedShift.status) (Expected: IN_PROGRESS)"
Write-Host "   Opening Cash  : $($openedShift.openingCash) VND (Expected: 500000)"
if ($openedShift.status -ne "IN_PROGRESS" -or $openedShift.openingCash -ne 500000) {
    Write-Error "Shift open failed!"
    exit 1
}

# 10. Close Shift 1 with cash (850000.0)
Write-Host "10. Closing Shift 1 with 850,000 VND cash..."
$closedShift = Put-Json "$baseUrl/api/v1/shifts/$shift1Id/close" @{ closingCash = 850000.0 } $staffHeaders
Write-Host "    Shift 1 status: $($closedShift.status) (Expected: CLOSED)"
Write-Host "    Closing Cash  : $($closedShift.closingCash) VND (Expected: 850000)"
if ($closedShift.status -ne "CLOSED" -or $closedShift.closingCash -ne 850000) {
    Write-Error "Shift close failed!"
    exit 1
}

# 11. Get Shift report
Write-Host "11. Getting Shift 1 summary report..."
$report = Get-Json "$baseUrl/api/v1/shifts/$shift1Id/report" $adminHeaders
Write-Host "    Shift Name: $($report.shiftName)"
Write-Host "    Status    : $($report.status)"
Write-Host "    Revenue   : $($report.totalRevenue) VND"
Write-Host "    Orders    : $($report.totalOrders)"
Write-Host "    Staff count: $($report.assignedStaff.Count) (Expected: 1)"

if ($report.assignedStaff.Count -ne 1) {
    Write-Error "Report staff count incorrect!"
    exit 1
}

# 12. Clean up test data
Write-Host "12. Cleaning up shift template and cancellations..."
$cancelShift2 = Put-Json "$baseUrl/api/v1/shifts/$shift2Id/cancel" @{} $adminHeaders
$deleteTemplate = Delete-Json "$baseUrl/api/v1/shift-templates/$templateId" $adminHeaders
Write-Host "    Cleanup complete."

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "ALL SHIFT TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
