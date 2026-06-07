# Arranca el API escuchando en todas las interfaces (necesario para celular / emulador)
Set-Location $PSScriptRoot

if (Test-Path ".\venv\Scripts\Activate.ps1") {
    . .\venv\Scripts\Activate.ps1
}

Write-Host "AppEstable API -> http://0.0.0.0:8000 (LAN: http://192.168.0.3:8000)" -ForegroundColor Cyan
uvicorn main:app --reload --host 0.0.0.0 --port 8000