# E2E integration test script for Cafe Manager Leave Request Approval/Rejection & Assignment Cancellation (Phase 3B)

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
Write-Host "STARTING LEAVE REQUEST PHASE 3B TESTS" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$ErrorActionPreference = "Stop"

# 1. Login as Admin
Write-Host "1. Logging in as admin..."
$adminLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "admin"; password = "admin123" }
$adminToken = $adminLogin.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "   Admin login successful. Token acquired."

# 2. Login as Staff
Write-Host "2. Logging in as staff..."
$staffLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "staff"; password = "123456" }
$staffToken = $staffLogin.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
$staffMe = Get-Json "$baseUrl/api/v1/auth/me" $staffHeaders
$staffUserId = $staffMe.userId
Write-Host "   Staff user ID is: $staffUserId"

# 3. Create a shift template
Write-Host "3. Creating a shift template..."
$templateRequest = @{
    templateName = "Ca Sang Test Leave"
    startTime = "06:00"
    endTime = "12:00"
    minStaff = 1
    isActive = $true
}
$template = Post-Json "$baseUrl/api/v1/shift-templates" $templateRequest $adminHeaders
$templateId = $template.templateId
Write-Host "   Created shift template ID: $templateId"

# 4. Generate unique epoch timestamp for shiftDate
$epochBase = [Math]::Floor([Double](Get-Date -UFormat %s) * 1000)
# Make sure we are at start of day (approximate is fine, but let's make it future)
$shiftDate = $epochBase + 86400000 * 5 # 5 days in future

Write-Host "4. Creating 3 shifts on $shiftDate..."
# Shift 1 (06:00 - 12:00) -> DRAFT
$shift1 = Post-Json "$baseUrl/api/v1/shifts" @{
    templateId = $templateId
    shiftName = "Ca 1 - DRAFT"
    shiftDate = $shiftDate
    startTime = "06:00"
    endTime = "12:00"
} $adminHeaders
$shift1Id = $shift1.shiftId

# Shift 2 (12:00 - 18:00) -> PUBLISHED
$shift2 = Post-Json "$baseUrl/api/v1/shifts" @{
    templateId = $templateId
    shiftName = "Ca 2 - PUBLISHED"
    shiftDate = $shiftDate
    startTime = "12:00"
    endTime = "18:00"
} $adminHeaders
$shift2Id = $shift2.shiftId
$publish2 = Put-Json "$baseUrl/api/v1/shifts/$shift2Id/publish" @{} $adminHeaders

# Shift 3 (18:00 - 23:59) -> IN_PROGRESS
$shift3 = Post-Json "$baseUrl/api/v1/shifts" @{
    templateId = $templateId
    shiftName = "Ca 3 - IN_PROGRESS"
    shiftDate = $shiftDate
    startTime = "18:00"
    endTime = "23:59"
} $adminHeaders
$shift3Id = $shift3.shiftId
$publish3 = Put-Json "$baseUrl/api/v1/shifts/$shift3Id/publish" @{} $adminHeaders

# Close any existing IN_PROGRESS shift first to prevent errors
try {
    $activeShifts = Get-Json "$baseUrl/api/v1/shifts?status=IN_PROGRESS" $adminHeaders
    foreach ($as in $activeShifts) {
        Write-Host "   Closing conflicting active shift $($as.shiftId)..."
        Put-Json "$baseUrl/api/v1/shifts/$($as.shiftId)/close" @{ closingCash = 1000.0 } $adminHeaders
    }
} catch {
    Write-Host "   No other active shifts to close or close failed (continuing)."
}

# Open Shift 3
$open3 = Put-Json "$baseUrl/api/v1/shifts/$shift3Id/open" @{ openingCash = 500000.0 } $staffHeaders

Write-Host "   Shifts created: Shift 1 (DRAFT, ID: $shift1Id), Shift 2 (PUBLISHED, ID: $shift2Id), Shift 3 (IN_PROGRESS, ID: $shift3Id)"

# 5. Assign staff to Shift 1, Shift 2, Shift 3
Write-Host "5. Assigning staff to all 3 shifts..."
Post-Json "$baseUrl/api/v1/shifts/$shift1Id/assign" @{ userId = $staffUserId } $adminHeaders
Post-Json "$baseUrl/api/v1/shifts/$shift2Id/assign" @{ userId = $staffUserId } $adminHeaders
Post-Json "$baseUrl/api/v1/shifts/$shift3Id/assign" @{ userId = $staffUserId } $adminHeaders
Write-Host "   Staff assigned successfully."

# 6. Staff submits leave request spanning the entire day (00:00 to 24:00)
# shiftDate is the base epoch. 06:00 is shiftDate + 6*3600*1000.
# Let's request leave from shiftDate to shiftDate + 24*3600*1000
$leaveStart = $shiftDate
$leaveEnd = $shiftDate + 24 * 3600 * 1000

Write-Host "6. Staff submitting leave request..."
$leaveRequest = Post-Json "$baseUrl/api/v1/leave-requests" @{
    startAt = $leaveStart
    endAt = $leaveEnd
    reason = "Ban viec gia dinh"
} $staffHeaders
$leaveRequestId = $leaveRequest.leaveRequestId
Write-Host "   Submitted Leave Request ID: $leaveRequestId, Status: $($leaveRequest.status)"

# 7. Test Role Verification: Staff cannot approve/reject
Write-Host "7. Testing Role Verification..."
try {
    Put-Json "$baseUrl/api/v1/leave-requests/$leaveRequestId/approve" @{ reviewNote = "Staff trying" } $staffHeaders
    Write-Error "Security breach: Staff was allowed to approve leave request!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        Write-Host "   Staff approve rejection correct: 403 Forbidden" -ForegroundColor Green
    } else {
        Write-Error "Expected 403 Forbidden, but got $statusCode"
    }
}

try {
    Put-Json "$baseUrl/api/v1/leave-requests/$leaveRequestId/reject" @{ reviewNote = "Staff trying" } $staffHeaders
    Write-Error "Security breach: Staff was allowed to reject leave request!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        Write-Host "   Staff reject rejection correct: 403 Forbidden" -ForegroundColor Green
    } else {
        Write-Error "Expected 403 Forbidden, but got $statusCode"
    }
}

# 8. Admin approves leave request. Should delete Shift 1 and Shift 2 assignments, but NOT Shift 3 assignment.
Write-Host "8. Admin approving leave request..."
$approveResponse = Put-Json "$baseUrl/api/v1/leave-requests/$leaveRequestId/approve" @{ reviewNote = "Dong y cho nghi" } $adminHeaders
Write-Host "   Approval status: $($approveResponse.status) (Expected: APPROVED)"
Write-Host "   Affected Assignments Count: $($approveResponse.affectedAssignmentCount) (Expected: 2)"

if ($approveResponse.status -ne "APPROVED") {
    Write-Error "Expected APPROVED, but got $($approveResponse.status)"
}
if ($approveResponse.affectedAssignmentCount -ne 2) {
    Write-Error "Expected 2 affected assignments, but got $($approveResponse.affectedAssignmentCount)"
}

# Verify assignment deletions
Write-Host "   Verifying assignment states..."
$assignmentsShift1 = Get-Json "$baseUrl/api/v1/shifts/$shift1Id/report" $adminHeaders
$assignmentsShift2 = Get-Json "$baseUrl/api/v1/shifts/$shift2Id/report" $adminHeaders
$assignmentsShift3 = Get-Json "$baseUrl/api/v1/shifts/$shift3Id/report" $adminHeaders

$isAssigned1 = $assignmentsShift1.assignedStaff.userId -contains $staffUserId
$isAssigned2 = $assignmentsShift2.assignedStaff.userId -contains $staffUserId
$isAssigned3 = $assignmentsShift3.assignedStaff.userId -contains $staffUserId

Write-Host "   Shift 1 (DRAFT) Staff Assigned: $isAssigned1 (Expected: False)"
Write-Host "   Shift 2 (PUBLISHED) Staff Assigned: $isAssigned2 (Expected: False)"
Write-Host "   Shift 3 (IN_PROGRESS) Staff Assigned: $isAssigned3 (Expected: True)"

if ($isAssigned1 -or $isAssigned2 -or !$isAssigned3) {
    Write-Error "Assignment state validation failed!"
}
Write-Host "   Assignment cancellation states are correct!" -ForegroundColor Green

# 9. Test conflict: Approve non-PENDING request
Write-Host "9. Testing approval of non-PENDING request..."
try {
    Put-Json "$baseUrl/api/v1/leave-requests/$leaveRequestId/approve" @{ reviewNote = "Re-approve" } $adminHeaders
    Write-Error "Conflict validation failed: Allowed approving an APPROVED request!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 409) {
        Write-Host "   Conflict verification passed: 409 Conflict" -ForegroundColor Green
    } else {
        Write-Error "Expected 409 Conflict, but got $statusCode"
    }
}

# 10. Test Rejection
Write-Host "10. Testing Rejection Flow..."
$shift4Date = $shiftDate + 86400000 # next day
# Create Shift 4 -> DRAFT
$shift4 = Post-Json "$baseUrl/api/v1/shifts" @{
    templateId = $templateId
    shiftName = "Ca 4 - DRAFT Rejection Test"
    shiftDate = $shift4Date
    startTime = "06:00"
    endTime = "12:00"
} $adminHeaders
$shift4Id = $shift4.shiftId

# Assign Staff to Shift 4
Post-Json "$baseUrl/api/v1/shifts/$shift4Id/assign" @{ userId = $staffUserId } $adminHeaders

# Submit second leave request
$leaveRequest2 = Post-Json "$baseUrl/api/v1/leave-requests" @{
    startAt = $shift4Date
    endAt = $shift4Date + 24 * 3600 * 1000
    reason = "Kiem tra tu choi"
} $staffHeaders
$leaveRequestId2 = $leaveRequest2.leaveRequestId

# Reject the request
$rejectResponse = Put-Json "$baseUrl/api/v1/leave-requests/$leaveRequestId2/reject" @{ reviewNote = "Khong duoc duyet" } $adminHeaders
Write-Host "    Rejection status: $($rejectResponse.status) (Expected: REJECTED)"
Write-Host "    Affected Assignments Count: $($rejectResponse.affectedAssignmentCount) (Expected: 0)"

if ($rejectResponse.status -ne "REJECTED" -or $rejectResponse.affectedAssignmentCount -ne 0) {
    Write-Error "Rejection verification failed!"
}

# Verify assignment for Shift 4 is NOT deleted
$assignmentsShift4 = Get-Json "$baseUrl/api/v1/shifts/$shift4Id/report" $adminHeaders
$isAssigned4 = $assignmentsShift4.assignedStaff.userId -contains $staffUserId
Write-Host "    Shift 4 Staff Assigned: $isAssigned4 (Expected: True)"

if (!$isAssigned4) {
    Write-Error "Assignment was incorrectly modified during rejection!"
}
Write-Host "    Rejection flow successfully verified!" -ForegroundColor Green

# 11. Clean up test data
Write-Host "11. Cleaning up shifts and templates..."
# Close Shift 3 (must be closed to be cleaned up or deleted, but we can close it)
try {
    Put-Json "$baseUrl/api/v1/shifts/$shift3Id/close" @{ closingCash = 500000.0 } $staffHeaders
} catch {
    Write-Host "    Shift 3 already closed or failed to close."
}
# Cancel Shift 4 & Shift 1 & Shift 2
Put-Json "$baseUrl/api/v1/shifts/$shift1Id/cancel" @{} $adminHeaders
Put-Json "$baseUrl/api/v1/shifts/$shift2Id/cancel" @{} $adminHeaders
Put-Json "$baseUrl/api/v1/shifts/$shift4Id/cancel" @{} $adminHeaders

# Delete template
Delete-Json "$baseUrl/api/v1/shift-templates/$templateId" $adminHeaders
Write-Host "    Cleanup complete."

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "ALL LEAVE REQUEST 3B TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
