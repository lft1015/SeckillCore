param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'
$headers = @{ 'Content-Type' = 'application/json' }

function Invoke-Api([string]$Method, [string]$Path, $Body = $null, [hashtable]$ExtraHeaders = @{}) {
    $requestHeaders = @{} + $headers + $ExtraHeaders
    $params = @{ Method = $Method; Uri = "$BaseUrl$Path"; Headers = $requestHeaders }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 8) }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 200) { throw "$Method $Path returned code $($response.code): $($response.message)" }
    return $response
}

Write-Host "Checking $BaseUrl"
$login = Invoke-Api 'POST' '/api/v1/auth/login' @{ usernameOrPhone = 'demo_user'; password = 'password' }
$token = $login.data.token
if ([string]::IsNullOrWhiteSpace($token)) { throw 'Login returned no access token' }
$auth = @{ Authorization = "Bearer $token" }

$productList = Invoke-Api 'GET' '/api/v1/product/list?page=1&size=10'
$product = Invoke-Api 'GET' '/api/v1/product/detail/2000000000000000001'
$activities = Invoke-Api 'GET' '/api/v1/activity/list?page=1&size=10'
$activity = Invoke-Api 'GET' '/api/v1/activity/detail/3000000000000000002'
$page = Invoke-Api 'GET' '/api/v1/bff/seckill-page/3000000000000000002' $null $auth
$orders = Invoke-Api 'GET' '/api/v1/order/list?page=1&size=10&userId=1000000000000000001' $null $auth
$order = Invoke-Api 'GET' '/api/v1/order/detail/5000000000000000001' $null $auth
$seckillResult = Invoke-Api 'GET' '/api/seckill/result/4000000000000000002' $null $auth

[pscustomobject]@{
    loginUser = $login.data.username
    productCount = $productList.data.total
    productName = $product.data.productName
    activityCount = $activities.data.total
    runningActivity = $activity.data.status
    bffCanSeckill = $page.data.canSeckill
    orderCount = $orders.data.total
    fixtureOrderStatus = $order.data.orderStatus
    fixtureSeckillStatus = $seckillResult.data.status
} | ConvertTo-Json
