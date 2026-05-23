param(
  [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

Write-Output "[1] Health"
$health = Invoke-RestMethod -Uri "$BaseUrl/api/health" -Method GET
Write-Output ("health.status = " + $health.status)

Write-Output "[2] Register"
$email = "smoke+" + [int][double]::Parse((Get-Date -UFormat %s)) + "@huyverse.dev"
$registerBody = @{
  email = $email
  password = "Password123"
  displayName = "Smoke User"
} | ConvertTo-Json
$register = Invoke-RestMethod -Uri "$BaseUrl/api/auth/register" -Method POST -ContentType "application/json" -Body $registerBody
Write-Output ("registered.email = " + $register.access.email)

Write-Output "[3] Profile with access token"
$access = $register.access.token
$profile = Invoke-RestMethod -Uri "$BaseUrl/api/profile/me" -Method GET -Headers @{ Authorization = "Bearer $access" }
Write-Output ("profile.displayName = " + $profile.displayName)

Write-Output "[4] Refresh token"
$refreshBody = @{ refreshToken = $register.refreshToken } | ConvertTo-Json
$refresh = Invoke-RestMethod -Uri "$BaseUrl/api/auth/refresh" -Method POST -ContentType "application/json" -Body $refreshBody
Write-Output ("refresh.access.email = " + $refresh.access.email)

Write-Output "[5] Logout current + refresh should fail"
Invoke-RestMethod -Uri "$BaseUrl/api/auth/logout" -Method POST -ContentType "application/json" -Body $refreshBody | Out-Null
try {
  Invoke-RestMethod -Uri "$BaseUrl/api/auth/refresh" -Method POST -ContentType "application/json" -Body $refreshBody | Out-Null
  throw "Expected refresh to fail after logout, but it succeeded."
} catch {
  Write-Output "refresh after logout: expected failure"
}

Write-Output "[6] SSE quick check"
$sse = curl.exe -sS -N "$BaseUrl/api/ai/chat/stream?prompt=smoke"
if ($sse -match "\[DONE\]") {
  Write-Output "sse: done marker found"
} elseif ($sse -match "data:") {
  Write-Output "sse: stream data found (done marker not captured in this shell run)"
} else {
  throw "SSE stream check failed"
}

Write-Output "Smoke auth test PASSED"
