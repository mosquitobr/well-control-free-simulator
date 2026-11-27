<#
.SYNOPSIS
    Executa testes e gera build/instalador do Controle de Poço 2 para Windows 11.
#>
[CmdletBinding()]
param(
    [switch]$Msi,
    [switch]$Exe
)

$ErrorActionPreference = "Stop"
$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) {
    Write-Host "[ERRO] JDK 21 necessário para build nativo no Windows 11. Instale com: winget install EclipseAdoptium.Temurin.21.JDK" -ForegroundColor Red
    exit 1
}

Write-Host "==> 1/3 Executando testes (:core:test e :desktopApp:test)..." -ForegroundColor Cyan
& .\gradlew.bat :core:test :desktopApp:test --no-daemon --stacktrace

Write-Host "==> 2/3 Gerando Uber JAR e app-image Windows 11..." -ForegroundColor Cyan
& .\gradlew.bat :desktopApp:packageUberJarForCurrentOS :desktopApp:createDistributable --no-daemon --stacktrace

if ($Msi) {
    Write-Host "==> 3/3 Gerando instalador MSI para Windows 11..." -ForegroundColor Cyan
    & .\gradlew.bat :desktopApp:packageMsi --no-daemon --stacktrace
}
if ($Exe) {
    Write-Host "==> 3/3 Gerando instalador EXE para Windows 11..." -ForegroundColor Cyan
    & .\gradlew.bat :desktopApp:packageExe --no-daemon --stacktrace
}

Write-Host "[OK] Build Windows 11 concluído em desktopApp\build\compose\" -ForegroundColor Green
