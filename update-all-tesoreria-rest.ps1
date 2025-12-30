# Script para actualizar TODOS los archivos REST de tesorería al nuevo patrón
$ErrorActionPreference = "Stop"

$basePath = "C:\work\saaBE\v1\saaBE\src\main\java\com\saa\ws\rest\tesoreria"
$filesUpdated = 0
$filesSkipped = 0

Write-Host "=== Actualizando archivos REST de Tesorería ===" -ForegroundColor Cyan
Write-Host ""

# Obtener todos los archivos *Rest.java
$files = Get-ChildItem -Path $basePath -Filter "*Rest.java"

foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $modified = $false
    
    # Verificar si tiene el patrón antiguo
    if ($content -match 'throws Throwable' -or $content -match '@Produces\("application/json"\)' -or $content -match '@Consumes\("application/json"\)') {
        Write-Host "Actualizando: $($file.Name)" -ForegroundColor Yellow
        
        # Reemplazar @Produces("application/json") por @Produces(MediaType.APPLICATION_JSON)
        if ($content -match '@Produces\("application/json"\)') {
            $content = $content -replace '@Produces\("application/json"\)', '@Produces(MediaType.APPLICATION_JSON)'
            $modified = $true
        }
        
        # Reemplazar @Consumes("application/json") por @Consumes(MediaType.APPLICATION_JSON)
        if ($content -match '@Consumes\("application/json"\)') {
            $content = $content -replace '@Consumes\("application/json"\)', '@Consumes(MediaType.APPLICATION_JSON)'
            $modified = $true
        }
        
        if ($modified) {
            Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
            $filesUpdated++
            Write-Host "  ✓ Actualizado" -ForegroundColor Green
        }
    } else {
        $filesSkipped++
    }
}

Write-Host ""
Write-Host "=== Resumen ===" -ForegroundColor Cyan
Write-Host "Archivos actualizados: $filesUpdated" -ForegroundColor Green
Write-Host "Archivos ya actualizados: $filesSkipped" -ForegroundColor Gray
Write-Host ""
Write-Host "¡Actualización completada!" -ForegroundColor Green
