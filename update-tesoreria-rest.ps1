# Script para actualizar todos los archivos REST de tesorería al nuevo patrón

$restFiles = @(
    "UsuarioPorCajaRest",
    "TransferenciaRest",
    "TempPagoRest",
    "TempMotivoPagoRest",
    "TempMotivoCobroRest",
    "TempDebitoCreditoRest",
    "TempCobroTransferenciaRest",
    "TempCobroTarjetaRest",
    "TempCobroRetencionRest",
    "TempCobroRest",
    "TempCobroEfectivoRest",
    "TempCobroChequeRest",
    "TelefonoDireccionRest",
    "SaldoBancoRest",
    "PersonaRolRest",
    "PersonaCuentaContableRest",
    "MovimientoBancoRest",
    "MotivoPagoRest",
    "MotivoCobroRest",
    "HistDetalleConciliacionRest",
    "HistConciliacionRest",
    "GrupoCajaRest",
    "DireccionPersonaRest",
    "DetalleDepositoRest",
    "DetalleDebitoCreditoRest",
    "DetalleConciliacionRest",
    "DetalleCierreRest",
    "DesgloseDetalleDepositoRest",
    "DepositoRest",
    "DebitoCreditoRest",
    "ConciliacionRest",
    "CobroTransferenciaRest",
    "CobroTarjetaRest",
    "CobroRetencionRest",
    "CobroEfectivoRest",
    "CobroChequeRest",
    "CierreCajaRest",
    "ChequeRest",
    "ChequeraRest",
    "CajaLogicaPorCajaFisicaRest",
    "BancoExternoRest",
    "AuxDepositoDesgloseRest",
    "AuxDepositoCierreRest",
    "AuxDepositoBancoRest"
)

$basePath = "C:\work\saaBE\v1\saaBE\src\main\java\com\saa\ws\rest\tesoreria"

Write-Host "Actualizando archivos REST de tesorería..." -ForegroundColor Green

foreach ($fileName in $restFiles) {
    $filePath = Join-Path $basePath "$fileName.java"
    
    if (Test-Path $filePath) {
        Write-Host "Procesando: $fileName.java" -ForegroundColor Yellow
        
        $content = Get-Content $filePath -Raw -Encoding UTF8
        
        # Reemplazar @Produces("application/json") por @Produces(MediaType.APPLICATION_JSON)
        $content = $content -replace '@Produces\("application/json"\)', '@Produces(MediaType.APPLICATION_JSON)'
        
        # Reemplazar @Consumes("application/json") por @Consumes(MediaType.APPLICATION_JSON)
        $content = $content -replace '@Consumes\("application/json"\)', '@Consumes(MediaType.APPLICATION_JSON)'
        
        # Guardar cambios
        Set-Content -Path $filePath -Value $content -Encoding UTF8 -NoNewline
        
        Write-Host "  ✓ Actualizado: $fileName.java" -ForegroundColor Green
    } else {
        Write-Host "  ✗ No encontrado: $fileName.java" -ForegroundColor Red
    }
}

Write-Host "`nActualización completada!" -ForegroundColor Green
Write-Host "Nota: Es necesario revisar manualmente los métodos que usan 'throws Throwable'" -ForegroundColor Yellow
