# Script para actualizar archivos REST del módulo CXP pendientes
# Fecha: 2025-12-30

$archivosRestantes = @(
    "DetalleDocumentoPagoRest.java",
    "CuotaXFinanciacionPagoRest.java",
    "ComposicionCuotaInicialPagoRest.java",
    "AprobacionXProposicionPagoRest.java",
    "AprobacionXMontoRest.java",
    "TempAprobacionXMontoRest.java",
    "TempComposicionCuotaInicialPagoRest.java",
    "TempCuotaXFinanciacionPagoRest.java",
    "TempDetalleDocumentoPagoRest.java",
    "TempDocumentoPagoRest.java",
    "TempFinanciacionXDocumentoPagoRest.java",
    "TempMontoAprobacionRest.java",
    "TempPagosArbitrariosXFinanciacionPagoRest.java",
    "TempResumenValorDocumentoPagoRest.java",
    "TempUsuarioXAprobacionRest.java",
    "TempValorImpuestoDetallePagoRest.java",
    "TempValorImpuestoDocumentoPagoRest.java"
)

Write-Host "Archivos REST del módulo CXP pendientes de actualización:" -ForegroundColor Cyan
$archivosRestantes | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
Write-Host "`nTotal: $($archivosRestantes.Count) archivos" -ForegroundColor Green
