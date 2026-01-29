# Script para probar el endpoint de reportes
# Uso: .\test-reporte-endpoint.ps1

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Test de Endpoint de Reportes" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:8080/SaaBE/rest/rprt"

# Test 1: Ping (Health Check)
Write-Host "[1/3] Probando Health Check (ping)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/ping" -Method Get
    Write-Host "OK - Servicio activo: $($response.mensaje)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Listar Modulos
Write-Host "[2/3] Obteniendo modulos disponibles..." -ForegroundColor Yellow
try {
    $modulos = Invoke-RestMethod -Uri "$baseUrl/modulos" -Method Get
    Write-Host "OK - Modulos encontrados: $($modulos -join ', ')" -ForegroundColor Green
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Generar Reporte de Prueba
Write-Host "[3/3] Generando reporte de prueba..." -ForegroundColor Yellow
try {
    $body = @{
        modulo = "test"
        nombreReporte = "reporte_prueba"
        formato = "PDF"
        parametros = @{
            empresaId = 1
            fechaDesde = "2026-01-01"
            fechaHasta = "2026-01-31"
        }
    } | ConvertTo-Json

    $outputFile = "reporte_prueba_$(Get-Date -Format 'yyyyMMdd_HHmmss').pdf"
    
    Invoke-RestMethod -Uri "$baseUrl/generar" -Method Post -ContentType "application/json" -Body $body -OutFile $outputFile
    
    Write-Host "OK - Reporte generado: $outputFile" -ForegroundColor Green
    Write-Host "  Abriendo archivo..." -ForegroundColor Gray
    Start-Process $outputFile
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.ErrorDetails.Message) {
        Write-Host "  Detalles: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
}
Write-Host ""

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Pruebas completadas" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
