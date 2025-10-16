<#
run-all.ps1
Start frontend (React + Vite) and backend (Spring Boot) each in a new PowerShell window.

Usage:
  - Open PowerShell and run:  .\scripts\run-all.ps1
  - Or right-click and "Run with PowerShell"

Environment variables (optional):
  $env:FRONTEND_CMD  - overrides frontend command (default: npm run dev)
  $env:BACKEND_CMD   - overrides backend command (default: mvn spring-boot:run)
  $env:FRONTEND_DIR  - overrides frontend folder name (default: frontend)
  $env:BACKEND_DIR   - overrides backend folder name (default: backend)
#>

# --- CONFIG (can be overridden by environment) ---
$frontendDir = if ($env:FRONTEND_DIR) { $env:FRONTEND_DIR } else { "frontend" }
$backendDir  = if ($env:BACKEND_DIR)  { $env:BACKEND_DIR  } else { "backend" }

$frontendCmd = if ($env:FRONTEND_CMD) { $env:FRONTEND_CMD } else { "npm run dev --host" }
$backendCmd  = if ($env:BACKEND_CMD)  { $env:BACKEND_CMD }  else { "mvn spring-boot:run" }

# --- resolve paths relative to script ---
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
$root = Resolve-Path (Join-Path $scriptDir "..")
$frontendPath = Join-Path $root.Path $frontendDir
$backendPath  = Join-Path $root.Path $backendDir

Write-Host "Project root: $($root.Path)"
Write-Host "Frontend path: $frontendPath"
Write-Host "Backend path : $backendPath"
Write-Host ""

if (-not (Test-Path $frontendPath)) {
  Write-Error "Frontend path not found: $frontendPath"
  exit 1
}
if (-not (Test-Path $backendPath)) {
  Write-Error "Backend path not found: $backendPath"
  exit 1
}

# Start frontend in a new PowerShell window
Write-Host "Starting frontend: $frontendCmd"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$frontendPath`"; $frontendCmd" -WindowStyle Normal

# Start backend in a new PowerShell window
Write-Host "Starting backend: $backendCmd"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd `"$backendPath`"; $backendCmd" -WindowStyle Normal

Write-Host ""
Write-Host "Both started in separate windows. Close those windows or press Ctrl+C inside them to stop the servers."
