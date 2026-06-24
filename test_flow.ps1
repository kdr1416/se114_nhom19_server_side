# End-to-end integration test script for Cafe Manager Order Flow and Payment APIs

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
Write-Host "STARTING ORDER & PAYMENT FLOW INTEGRATION TEST" -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Login as Admin to set up data
Write-Host "1. Logging in as admin..."
$adminLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "admin"; password = "admin123" }
$adminToken = $adminLogin.token
$adminHeaders = @{ Authorization = "Bearer $adminToken" }
Write-Host "   Admin login successful. Token acquired."

# 2. Login as Staff for order flow operations
Write-Host "2. Logging in as staff..."
$staffLogin = Post-Json "$baseUrl/api/v1/auth/login" @{ username = "staff"; password = "123456" }
$staffToken = $staffLogin.token
$staffHeaders = @{ Authorization = "Bearer $staffToken" }
Write-Host "   Staff login successful. Token acquired."

# 3. Create a test table
Write-Host "3. Creating a test table..."
$tableRequest = @{
    tableName = "Ban Test 99"
    status = "AVAILABLE"
    capacity = 4
    area = "Khu A"
}
$table = Post-Json "$baseUrl/api/v1/tables" $tableRequest $adminHeaders
$tableId = $table.tableId
Write-Host "   Created table: $($table.tableName) with ID: $tableId"

# 4. Create a category
Write-Host "4. Creating a category..."
$categoryRequest = @{
    categoryName = "Do uong Test"
    description = "Mo ta do uong"
}
$category = Post-Json "$baseUrl/api/v1/categories" $categoryRequest $adminHeaders
$categoryId = $category.categoryId
Write-Host "   Created category: $($category.categoryName) with ID: $categoryId"

# 5. Create a product
Write-Host "5. Creating a product..."
$productRequest = @{
    categoryId = $categoryId
    productName = "Ca phe sua da Test"
    price = 25000.0
    imageUrl = ""
    isActive = $true
}
$product = Post-Json "$baseUrl/api/v1/products" $productRequest $adminHeaders
$productId = $product.productId
Write-Host "   Created product: $($product.productName) with ID: $productId, Price: $($product.price) VND"

# 6. Create an order for the table (as staff)
Write-Host "6. Creating order for Table $tableId (as staff)..."
$orderRequest = @{
    tableId = $tableId
    note = "Khong da"
}
$order = Post-Json "$baseUrl/api/v1/orders" $orderRequest $staffHeaders
$orderId = $order.orderId
Write-Host "   Created order ID: $orderId, Code: $($order.orderCode), Status: $($order.status)"

# Verify Table status is now OCCUPIED
$tableDetail = Get-Json "$baseUrl/api/v1/tables/$tableId" $staffHeaders
Write-Host "   Verifying table status: $($tableDetail.status) (Expected: OCCUPIED)"
if ($tableDetail.status -ne "OCCUPIED") {
    Write-Error "Table status was not updated to OCCUPIED!"
    exit 1
}

# 7. Add an item to the order (as staff)
Write-Host "7. Adding 2 quantity of product $productId to order $orderId..."
$itemRequest = @{
    productId = $productId
    quantity = 2
    note = "it sua"
}
$orderDetail = Post-Json "$baseUrl/api/v1/orders/$orderId/items" $itemRequest $staffHeaders
Write-Host "   Item added. Order total amount: $($orderDetail.order.totalAmount) VND (Expected: 50000)"
if ($orderDetail.order.totalAmount -ne 50000) {
    Write-Error "Total amount is incorrect!"
    exit 1
}

# Find the order item ID
$orderItemId = $orderDetail.items[0].orderItemId
Write-Host "   Order Item ID is $orderItemId"

# 8. Update item quantity to 3
Write-Host "8. Updating quantity of item $orderItemId to 3..."
$updateDetail = Put-Json "$baseUrl/api/v1/orders/$orderId/items/$orderItemId" @{ quantity = 3 } $staffHeaders
Write-Host "   Quantity updated. Order total amount: $($updateDetail.order.totalAmount) VND (Expected: 75000)"
if ($updateDetail.order.totalAmount -ne 75000) {
    Write-Error "Total amount after update is incorrect!"
    exit 1
}

# 9. Confirm the order
Write-Host "9. Confirming order $orderId..."
$confirmedOrder = Put-Json "$baseUrl/api/v1/orders/$orderId/confirm" @{} $staffHeaders
Write-Host "   Order status: $($confirmedOrder.status) (Expected: CONFIRMED)"
if ($confirmedOrder.status -ne "CONFIRMED") {
    Write-Error "Order status was not updated to CONFIRMED!"
    exit 1
}

# 10. Process payment with promotion "CAFE10K"
Write-Host "10. Processing payment for order $orderId with promotion 'CAFE10K'..."
$paymentRequest = @{
    orderId = $orderId
    paymentMethod = "CASH"
    promotionCode = "CAFE10K"
    amountReceived = 100000.0
}
$payment = Post-Json "$baseUrl/api/v1/payments" $paymentRequest $staffHeaders
Write-Host "    Payment processed successfully!"
Write-Host "    Subtotal       : $($payment.subtotal) VND"
Write-Host "    Discount Amount: $($payment.discountAmount) VND (Expected: 10000)"
Write-Host "    Total Amount   : $($payment.total) VND (Expected: 65000)"
Write-Host "    Change Returned: $($payment.change) VND (Expected: 35000)"

if ($payment.discountAmount -ne 10000 -or $payment.total -ne 65000 -or $payment.change -ne 35000) {
    Write-Error "Payment calculations are incorrect!"
    exit 1
}

# 11. Verify final order status is PAID and table is AVAILABLE
$finalOrder = Get-Json "$baseUrl/api/v1/orders/$orderId" $staffHeaders
$finalTable = Get-Json "$baseUrl/api/v1/tables/$tableId" $staffHeaders

Write-Host "11. Verifying final states..."
Write-Host "    Final Order Status: $($finalOrder.order.status) (Expected: PAID)"
Write-Host "    Final Table Status: $($finalTable.status) (Expected: AVAILABLE)"

if ($finalOrder.order.status -ne "PAID") {
    Write-Error "Order status is not PAID!"
    exit 1
}
if ($finalTable.status -ne "AVAILABLE") {
    Write-Error "Table status is not AVAILABLE!"
    exit 1
}

# 12. Clean up (Delete the created product, category, table)
Write-Host "12. Cleaning up test data..."
# Order/Payment entities exist as historical records, we clean up product/category/table
$deleteTable = Delete-Json "$baseUrl/api/v1/tables/$tableId" $adminHeaders
$deleteProduct = Delete-Json "$baseUrl/api/v1/products/$productId" $adminHeaders
$deleteCategory = Delete-Json "$baseUrl/api/v1/categories/$categoryId" $adminHeaders
Write-Host "    Cleanup complete."

Write-Host ""
Write-Host "=============================================" -ForegroundColor Green
Write-Host "ALL TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "=============================================" -ForegroundColor Green
