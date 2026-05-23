$ErrorActionPreference = "Stop"

Write-Host "==================================================="
Write-Host "  BOOTSTRAP NOT-FOR-TOMORROW (Windows)"
Write-Host "==================================================="

function Assert-Command($name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Missing required command: $name"
    }
}

# Node.js workspaces
Assert-Command "npm"

Write-Host "[1/4] Installing root Node.js dependencies..."
Push-Location -Path (Split-Path -Parent $MyInvocation.MyCommand.Path)
npm install
Pop-Location

Write-Host "[2/4] Installing FE dependencies..."
Push-Location -Path "FE"
npm install
Pop-Location

Write-Host "[3/4] Installing Windows (Electron) dependencies..."
Push-Location -Path "Windows"
npm install
Pop-Location

Write-Host "[4/4] Installing Mobile (Expo) dependencies..."
Push-Location -Path "Mobile"
npm install
Pop-Location

# Python venv + deps
Assert-Command "python"

$venvPath = Join-Path -Path "Py" -ChildPath ".venv"
if (-not (Test-Path -Path $venvPath)) {
    Write-Host "Creating Python virtual environment in Py/.venv..."
    python -m venv $venvPath
}

$venvPython = Join-Path -Path $venvPath -ChildPath "Scripts\python.exe"
if (Test-Path -Path "Py\requirements.txt") {
    Write-Host "Installing Python dependencies..."
    & $venvPython -m pip install -r "Py\requirements.txt"
} else {
    Write-Host "Py/requirements.txt not found, skipping Python deps."
}

Write-Host "==================================================="
Write-Host "  DONE. You can start dev scripts now."
Write-Host "==================================================="
