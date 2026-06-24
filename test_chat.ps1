# Integration test script for Cafe Manager Chat APIs

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

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "STARTING CHAT REAL-TIME REST & WORKFLOW TEST" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

$ErrorActionPreference = "Stop"

# 1. Login as Admin
Write-Host "1. Logging in as admin..."
$adminLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "admin"; password = "admin123" }
$adminToken = $adminLogin.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "   Admin login successful."

# 2. Login as Staff
Write-Host "2. Logging in as staff..."
$staffLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "staff"; password = "123456" }
$staffToken = $staffLogin.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
Write-Host "   Staff login successful."

# 3. Create a general chat room as admin
Write-Host "3. Creating a general chat room..."
$roomRequest = @{
    roomName = "Họp Giao Ban Hằng Ngày"
    targetRole = "STAFF"
}
$room = Post-Json "$baseUrl/api/v1/chat/rooms" $roomRequest $adminHeaders
$roomId = $room.roomId
Write-Host "   Created Room ID: $roomId, Name: $($room.roomName)"

# 4. List rooms for Staff user
Write-Host "4. Listing rooms for staff..."
$staffRooms = Get-Json "$baseUrl/api/v1/chat/rooms" $staffHeaders
Write-Host "   Staff is participant in rooms count: $($staffRooms.Length)"
$found = $false
foreach ($r in $staffRooms) {
    if ($r.roomId -eq $roomId) {
        $found = $true
        Write-Host "   Found room 'Họp Giao Ban Hằng Ngày' in staff's room list."
    }
}
if (-not $found) {
    throw "Room not found in staff's room list!"
}

# 5. List rooms for Admin user
Write-Host "5. Listing rooms for admin..."
$adminRooms = Get-Json "$baseUrl/api/v1/chat/rooms" $adminHeaders
Write-Host "   Admin is participant in rooms count: $($adminRooms.Length)"

# 6. Read message history of the room (should be empty initially)
Write-Host "6. Reading message history for roomId: $roomId"
$history = Get-Json "$baseUrl/api/v1/chat/rooms/$roomId/messages" $staffHeaders
Write-Host "   History total elements: $($history.totalElements)"

# 7. Mark messages as read
Write-Host "7. Marking room $roomId messages as read..."
Put-Json "$baseUrl/api/v1/chat/rooms/$roomId/read" @{} $staffHeaders
Write-Host "   Marked as read successfully."

Write-Host "=============================================" -ForegroundColor Green
Write-Host "CHAT INTEGRATION REST TEST COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
