# Start Firebase Storage emulator
# Usage: Open PowerShell in repository root and run: .\scripts\start-emulator.ps1

param()

# Ensure firebase-tools is available
if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    Write-Error "firebase CLI not found. Install it with: npm install -g firebase-tools"
    exit 1
}

# Change to repository root (assumes script is in repo/scripts)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location (Resolve-Path "$scriptDir\..")

Write-Host "Starting Firebase Storage emulator (Storage port 9199)..."
firebase emulators:start --only storage
