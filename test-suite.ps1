$ErrorActionPreference = 'Continue'
$java = 'http://localhost:8080'
$node = 'http://localhost:3000'
$pass = 0
$fail = 0
$failures = @()

function Assert-Eq($label, $expected, $actual) {
    if ($expected -eq $actual) {
        Write-Host ("  [PASS] " + $label + "  (" + $actual + ")") -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("  [FAIL] " + $label + "  expected=" + $expected + " actual=" + $actual) -ForegroundColor Red
        $script:fail++
        $script:failures += $label
    }
}

function Get-Status($method, $url, $body) {
    try {
        $params = @{ Method = $method; Uri = $url; UseBasicParsing = $true; ErrorAction = 'Stop' }
        if ($body) {
            $params.Body = $body
            $params.ContentType = 'application/json'
        }
        $r = Invoke-WebRequest @params
        return [int]$r.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return -1
    }
}

function Get-Json($method, $url, $body) {
    try {
        $params = @{ Method = $method; Uri = $url; UseBasicParsing = $true; ErrorAction = 'Stop' }
        if ($body) {
            $params.Body = $body
            $params.ContentType = 'application/json'
        }
        $r = Invoke-WebRequest @params
        return $r.Content | ConvertFrom-Json
    } catch {
        return $null
    }
}

Write-Host ""
Write-Host "================ PHASE 1: Setup + GET endpoints ================" -ForegroundColor Cyan
Assert-Eq "GET /health (Java)"             200 (Get-Status GET "$java/health")
Assert-Eq "GET /health (Node chain)"       200 (Get-Status GET "$node/health")
Assert-Eq "GET /api/users (Java)"          200 (Get-Status GET "$java/api/users")
Assert-Eq "GET /api/users (Node)"          200 (Get-Status GET "$node/api/users")
Assert-Eq "GET /api/users/1"               200 (Get-Status GET "$java/api/users/1")
Assert-Eq "GET /api/users/999 (not found)" 404 (Get-Status GET "$java/api/users/999")
Assert-Eq "GET /api/tasks"                 200 (Get-Status GET "$java/api/tasks")
Assert-Eq "GET /api/tasks?status=pending"  200 (Get-Status GET "$java/api/tasks?status=pending")
Assert-Eq "GET /api/tasks?userId=2"        200 (Get-Status GET "$java/api/tasks?userId=2")
Assert-Eq "GET /api/stats"                 200 (Get-Status GET "$java/api/stats")

Write-Host ""
Write-Host "================ PHASE 2.1: POST /api/users ================" -ForegroundColor Cyan
$u1 = Get-Json POST "$java/api/users" '{"name":"Karan","email":"karan@test.com","role":"developer"}'
Assert-Eq "Valid user -> 201"              201 (Get-Status POST "$java/api/users" '{"name":"Alice","email":"alice@test.com","role":"qa"}')
Assert-Eq "Returned user has id"           $true ($null -ne $u1 -and $u1.id -gt 0)
Assert-Eq "Missing fields -> 400"          400 (Get-Status POST "$java/api/users" '{"name":"X"}')
Assert-Eq "Invalid email -> 400"           400 (Get-Status POST "$java/api/users" '{"name":"X","email":"bad","role":"r"}')
Assert-Eq "Empty body -> 400"              400 (Get-Status POST "$java/api/users" '{}')
Assert-Eq "Malformed JSON -> 400"          400 (Get-Status POST "$java/api/users" 'not-json')
Assert-Eq "Via Node gateway -> 201"        201 (Get-Status POST "$node/api/users" '{"name":"Bob","email":"bob2@test.com","role":"dev"}')

$users = Get-Json GET "$java/api/users"
Assert-Eq "New users appear in GET"        $true ($users.users.email -contains 'karan@test.com')

Write-Host ""
Write-Host "================ PHASE 2.2: POST /api/tasks ================" -ForegroundColor Cyan
$t1 = Get-Json POST "$java/api/tasks" '{"title":"Write tests","status":"pending","userId":1}'
Assert-Eq "Valid task -> 201"              201 (Get-Status POST "$java/api/tasks" '{"title":"Another","status":"in-progress","userId":2}')
Assert-Eq "Returned task has id"           $true ($null -ne $t1 -and $t1.id -gt 0)
Assert-Eq "Bad status -> 400"              400 (Get-Status POST "$java/api/tasks" '{"title":"X","status":"bogus","userId":1}')
Assert-Eq "Unknown userId -> 400"          400 (Get-Status POST "$java/api/tasks" '{"title":"X","status":"pending","userId":999}')
Assert-Eq "Missing title -> 400"           400 (Get-Status POST "$java/api/tasks" '{"status":"pending","userId":1}')
Assert-Eq "Via Node gateway -> 201"        201 (Get-Status POST "$node/api/tasks" '{"title":"Node task","status":"completed","userId":1}')

Write-Host ""
Write-Host "================ PHASE 2.3: PUT /api/tasks/{id} ================" -ForegroundColor Cyan
Assert-Eq "Partial update (status) -> 200"  200 (Get-Status PUT "$java/api/tasks/1" '{"status":"completed"}')
Assert-Eq "Partial update (title)  -> 200"  200 (Get-Status PUT "$java/api/tasks/1" '{"title":"New title"}')
Assert-Eq "Bad status -> 400"               400 (Get-Status PUT "$java/api/tasks/1" '{"status":"bogus"}')
Assert-Eq "Unknown userId -> 400"           400 (Get-Status PUT "$java/api/tasks/1" '{"userId":999}')
Assert-Eq "Non-existent id -> 404"          404 (Get-Status PUT "$java/api/tasks/9999" '{"status":"completed"}')
Assert-Eq "Via Node gateway -> 200"         200 (Get-Status PUT "$node/api/tasks/2" '{"status":"pending"}')

$t = Get-Json GET "$java/api/tasks?userId=1"
Assert-Eq "Task 1 title persisted"          $true ($t.tasks | Where-Object { $_.id -eq 1 -and $_.title -eq 'New title' } | ForEach-Object { $true })

Write-Host ""
Write-Host "================ PHASE 2.4: Request logging ================" -ForegroundColor Cyan
Invoke-WebRequest -Uri "$java/api/users" -UseBasicParsing | Out-Null
Start-Sleep -Milliseconds 500
Assert-Eq "Detailed health endpoint -> 200" 200 (Get-Status GET "$java/health/detailed")

Write-Host ""
Write-Host "================ PHASE 3: Persistence + Enhanced health ================" -ForegroundColor Cyan
$detailed = Get-Json GET "$java/health/detailed"
Assert-Eq "Detailed health has uptimeMs"    $true ($null -ne $detailed.uptimeMs)
Assert-Eq "Detailed health has memory"      $true ($null -ne $detailed.memory.usedMb)
Assert-Eq "Detailed health has data counts" $true ($detailed.data.users -ge 3 -and $detailed.data.tasks -ge 3)
Assert-Eq "data/data.json exists"           $true (Test-Path "$PSScriptRoot\java-backend\data\data.json")

Write-Host ""
Write-Host "================ PHASE 5: Metrics + Rate limiting ================" -ForegroundColor Cyan
Assert-Eq "GET /api/metrics -> 200"         200 (Get-Status GET "$java/api/metrics")
$m = Get-Json GET "$java/api/metrics"
Assert-Eq "Metrics totalRequests > 0"       $true ($m.totalRequests -gt 0)
Assert-Eq "Metrics has requestsByRoute"     $true ($null -ne $m.requestsByRoute)
Assert-Eq "Metrics has uptimeSeconds"       $true ($m.uptimeSeconds -ge 0)

Write-Host ""
Write-Host "================ RESULTS ================" -ForegroundColor Cyan
Write-Host ("PASS: " + $pass) -ForegroundColor Green
$failColor = if ($fail -eq 0) { 'Green' } else { 'Red' }
Write-Host ("FAIL: " + $fail) -ForegroundColor $failColor
if ($fail -gt 0) {
    Write-Host ""
    Write-Host "Failed tests:" -ForegroundColor Red
    $failures | ForEach-Object { Write-Host ("  - " + $_) -ForegroundColor Red }
    exit 1
}
exit 0
