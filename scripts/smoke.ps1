# 校园二手闲置交易平台 · 冒烟测试脚本（纯标准库，无需任何第三方模块）
# 用法：
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/smoke.ps1 -BaseUrl http://127.0.0.1:8080
# 前置条件：应用已启动、演示数据已导入（sql/01-init.sql）
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

Add-Type -AssemblyName System.Net.Http

$script:passed = 0
$script:failed = 0
$script:failures = New-Object System.Collections.ArrayList

function New-Session {
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.AllowAutoRedirect = $false
    $handler.UseCookies = $true
    $handler.CookieContainer = New-Object System.Net.CookieContainer
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(20)
    return $client
}

function Get-Page([System.Net.Http.HttpClient]$client, [string]$path) {
    $resp = $client.GetAsync($BaseUrl + $path).Result
    $body = $resp.Content.ReadAsStringAsync().Result
    return [pscustomobject]@{ Status = [int]$resp.StatusCode; Body = $body }
}

function Get-Csrf([string]$html) {
    $m = [regex]::Match($html, 'name="_csrf"\s+value="([^"]+)"')
    if ($m.Success) { return $m.Groups[1].Value }
    return $null
}

function Login([System.Net.Http.HttpClient]$client, [string]$user, [string]$pwd) {
    $page = Get-Page $client '/login'
    $token = Get-Csrf $page.Body
    if (-not $token) { throw "登录页未取到 CSRF 令牌" }
    $pairs = New-Object 'System.Collections.Generic.List[System.Collections.Generic.KeyValuePair[string,string]]'
    $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new('username', $user))
    $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new('password', $pwd))
    $pairs.Add([System.Collections.Generic.KeyValuePair[string,string]]::new('_csrf', $token))
    $form = New-Object System.Net.Http.FormUrlEncodedContent -ArgumentList (, $pairs)
    $resp = $client.PostAsync($BaseUrl + '/login', $form).Result
    return [int]$resp.StatusCode
}

function Check([string]$name, [bool]$ok, [string]$detail) {
    if ($ok) {
        $script:passed++
        Write-Host ("  [PASS] " + $name) -ForegroundColor Green
    } else {
        $script:failed++
        [void]$script:failures.Add($name)
        Write-Host ("  [FAIL] " + $name + "  -> " + $detail) -ForegroundColor Red
    }
}

function Check-Status([System.Net.Http.HttpClient]$client, [string]$path, [int[]]$expect) {
    $r = Get-Page $client $path
    $ok = $expect -contains $r.Status
    Check ("GET " + $path + " 期望 " + ($expect -join '/') + " 实际 " + $r.Status) $ok ("实际 " + $r.Status)
    return $r
}

Write-Host ""
Write-Host "=== 校园二手闲置交易平台 · 冒烟测试 ===" -ForegroundColor Cyan
Write-Host ("目标地址: " + $BaseUrl)

# ---------- 一、游客视角 ----------
Write-Host ""
Write-Host "[1/4] 游客视角（未登录）"
$guest = New-Session
Check-Status $guest '/' @(200) | Out-Null
Check-Status $guest '/items' @(200) | Out-Null
Check-Status $guest '/items?keyword=%E4%B9%A6' @(200) | Out-Null
Check-Status $guest '/items?categoryId=2&minPrice=10&maxPrice=500' @(200) | Out-Null
Check-Status $guest '/item/4' @(200) | Out-Null
Check-Status $guest '/login' @(200) | Out-Null
Check-Status $guest '/register' @(200) | Out-Null
Check-Status $guest '/css/app.css' @(200) | Out-Null
Check-Status $guest '/lib/echarts.min.js' @(200) | Out-Null
Check-Status $guest '/admin/dashboard' @(302) | Out-Null
Check-Status $guest '/my-items' @(302) | Out-Null
Check-Status $guest '/user/messages' @(302) | Out-Null
$r = Get-Page $guest '/item/99'
Check ("GET /item/99 不存在的物品 -> 404/错误页 实际 " + $r.Status) ($r.Status -ge 400) ("实际 " + $r.Status)

# ---------- 二、买家视角 ----------
Write-Host ""
Write-Host "[2/4] 买家 student01"
$buyer = New-Session
$code = Login $buyer 'student01' '123456'
Check ("POST /login student01 -> 302 实际 " + $code) ($code -eq 302) ("实际 " + $code)
Check-Status $buyer '/' @(200) | Out-Null
Check-Status $buyer '/my-items' @(200) | Out-Null
Check-Status $buyer '/cart' @(200) | Out-Null
Check-Status $buyer '/user/favorites' @(200) | Out-Null
Check-Status $buyer '/user/history' @(200) | Out-Null
Check-Status $buyer '/user/profile' @(200) | Out-Null
Check-Status $buyer '/user/password' @(200) | Out-Null
Check-Status $buyer '/user/messages' @(200) | Out-Null
Check-Status $buyer '/orders/mine' @(200) | Out-Null
Check-Status $buyer '/orders/sold' @(200) | Out-Null
Check-Status $buyer '/items/publish' @(200) | Out-Null
Check-Status $buyer '/admin/dashboard' @(302) | Out-Null

# ---------- 三、卖家视角 ----------
Write-Host ""
Write-Host "[3/4] 卖家 seller01"
$seller = New-Session
$code = Login $seller 'seller01' '123456'
Check ("POST /login seller01 -> 302 实际 " + $code) ($code -eq 302) ("实际 " + $code)
Check-Status $seller '/my-items' @(200) | Out-Null
Check-Status $seller '/orders/sold' @(200) | Out-Null
Check-Status $seller '/items/publish' @(200) | Out-Null

# ---------- 四、管理员视角 ----------
Write-Host ""
Write-Host "[4/4] 管理员 admin"
$admin = New-Session
$code = Login $admin 'admin' 'admin123'
Check ("POST /login admin -> 302 实际 " + $code) ($code -eq 302) ("实际 " + $code)
Check-Status $admin '/admin/dashboard' @(200) | Out-Null
Check-Status $admin '/admin/users' @(200) | Out-Null
Check-Status $admin '/admin/items' @(200) | Out-Null
Check-Status $admin '/admin/items?status=0' @(200) | Out-Null
Check-Status $admin '/admin/items/audits' @(200) | Out-Null
Check-Status $admin '/admin/categories' @(200) | Out-Null
Check-Status $admin '/admin/orders' @(200) | Out-Null
Check-Status $admin '/admin/comments' @(200) | Out-Null
Check-Status $admin '/admin/payments' @(200) | Out-Null
Check-Status $admin '/admin/roles' @(200) | Out-Null
Check-Status $admin '/admin/menus' @(200) | Out-Null
Check-Status $admin '/admin/mail-logs' @(200) | Out-Null

foreach ($api in @('/admin/api/stat/overview', '/admin/api/stat/category', '/admin/api/stat/item-trend',
                   '/admin/api/stat/order-trend', '/admin/api/stat/order-status', '/admin/api/stat/top-sellers', '/admin/api/stat/daily')) {
    $r = Get-Page $admin $api
    $ok = ($r.Status -eq 200) -and ($r.Body -match '"success"\s*:\s*true')
    Check ("GET " + $api + " -> 200 且 success=true") $ok ("实际 " + $r.Status + " body=" + $r.Body.Substring(0, [Math]::Min(80, $r.Body.Length)))
}

# ---------- 汇总 ----------
$total = $script:passed + $script:failed
Write-Host ""
Write-Host ("=== 结果：" + $script:passed + " / " + $total + " 通过，失败 " + $script:failed + " ===") -ForegroundColor Cyan
if ($script:failed -gt 0) {
    Write-Host "失败项：" -ForegroundColor Red
    foreach ($f in $script:failures) { Write-Host ("  - " + $f) -ForegroundColor Red }
    exit 1
}
Write-Host "全部通过。" -ForegroundColor Green
exit 0