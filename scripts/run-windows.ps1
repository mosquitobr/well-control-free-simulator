<#
.SYNOPSIS
    Executa o Controle de Poço 2 Desktop nativamente no Windows 11 (ou delega para WSL2/WSLg com -UseWsl).
#>
[CmdletBinding()]
param(
    [switch]$UseWsl
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

if ($UseWsl) {
    $wslPath = (& wsl wslpath -a ($RootDir -replace '\\', '/')).Trim()
    & wsl bash -lc "cd '$wslPath' && ./scripts/run-wsl.sh"
    exit $LASTEXITCODE
}

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "[AVISO] JDK 21 não encontrado no Windows 11." -ForegroundColor Yellow
    Write-Host "Opções:" -ForegroundColor Cyan
    Write-Host "  1) Instalar JDK 21 nativo: winget install EclipseAdoptium.Temurin.21.JDK"
    Write-Host "  2) Executar via WSL2/WSLg: .\scripts\run-windows.ps1 -UseWsl"
    exit 1
}

Write-Host "[Controle de Poço 2] Iniciando interface Desktop (Windows 11)..." -ForegroundColor Cyan
& .\gradlew.bat :desktopApp:run --console=plain
exit $LASTEXITCODE
