# Builds a clean zip for handoff. Run from project root:  .\build-release-zip.ps1
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$zipPath = Join-Path (Split-Path $root -Parent) 'java-developer-test-submission.zip'
$stage = Join-Path $env:TEMP ('java-test-stage-' + [Guid]::NewGuid().ToString('N'))

Write-Host "Project root: $root"
Write-Host "Output zip:   $zipPath"

$dataJson = Join-Path $root 'java-backend\data\data.json'
if (Test-Path $dataJson) {
  Remove-Item $dataJson -Force
  Write-Host "Removed stale java-backend\data\data.json"
}

if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory -Path $stage | Out-Null

$rob = robocopy $root $stage /E /XD node_modules target .git .idea .vscode data /XF *.log /NFL /NDL /NJH /NJS 2>&1
if ($LASTEXITCODE -ge 8) { throw "robocopy failed with code $LASTEXITCODE" }

if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Compress-Archive -Path "$stage\*" -DestinationPath $zipPath -Force
Remove-Item $stage -Recurse -Force

Add-Type -AssemblyName System.IO.Compression.FileSystem
$z = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
$names = $z.Entries | ForEach-Object { $_.FullName }
$z.Dispose()

$must = @(
  'docker-compose.yml',
  'START_HERE.txt',
  'README.md',
  'DOCKER.md',
  'docker-up.bat',
  'java-backend\Dockerfile',
  'node-backend\Dockerfile',
  'react-frontend\Dockerfile',
  'react-frontend\nginx.conf'
)
Write-Host ""
Write-Host "Zip checks:"
foreach ($m in $must) {
  $ok = $names | Where-Object { $_ -eq $m -or $_ -eq ($m -replace '\\','/') }
  if ($ok) { Write-Host "  OK  $m" -ForegroundColor Green }
  else { Write-Host "  MISSING $m" -ForegroundColor Red }
}

$bad = $names | Where-Object { $_ -match 'node_modules|\\\\target\\\\|/target/|data\\\\data\.json' }
if ($bad) {
  Write-Host "WARNING: unwanted paths in zip:" -ForegroundColor Yellow
  $bad | ForEach-Object { Write-Host "  $_" }
} else {
  Write-Host "  OK  no node_modules / target / data.json in zip" -ForegroundColor Green
}

$len = (Get-Item $zipPath).Length
Write-Host ""
Write-Host "Created: $zipPath"
Write-Host ("Size: {0:N1} KB" -f ($len/1KB))
Write-Host ""
Write-Host "Tell client: install Docker Desktop, unzip, read START_HERE.txt, run docker-up.bat"
