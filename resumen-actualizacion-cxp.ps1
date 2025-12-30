# Script para generar el resumen de actualización de archivos REST
# Fecha: 2025-12-30

# Archivos del módulo CXP completados hasta ahora (18/29)
$archivosCompletados = @(
    "ValorImpuestoDocumentoPagoRest.java",
    "ValorImpuestoDetallePagoRest.java",
    "UsuarioXAprobacionRest.java",
    "MontoAprobacionRest.java",
    "DocumentoPagoRest.java",
    "ProductoPagoRest.java",
    "ProposicionPagoXCuotaRest.java",
    "PagosArbitrariosXFinanciacionPagoRest.java",
    "ResumenValorDocumentoPagoRest.java",
    "ImpuestoXGrupoPagoRest.java",
    "GrupoProductoPagoRest.java",
    "FinanciacionXDocumentoPagoRest.java",
    "DetalleDocumentoPagoRest.java",
    "CuotaXFinanciacionPagoRest.java",
    "ComposicionCuotaInicialPagoRest.java",
    "AprobacionXMontoRest.java",
    "AprobacionXProposicionPagoRest.java",
    "TempAprobacionXMontoRest.java"
)

# Archivos pendientes del módulo CXP (11/29)
$archivosPendientes = @(
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

Write-Host "=== RESUMEN DE ACTUALIZACIÓN - MÓDULO CXP ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "✅ Archivos completados: $($archivosCompletados.Count)" -ForegroundColor Green
Write-Host "⏳ Archivos pendientes: $($archivosPendientes.Count)" -ForegroundColor Yellow
Write-Host "📊 Total archivos CXP: $($archivosCompletados.Count + $archivosPendientes.Count)" -ForegroundColor White
Write-Host ""
Write-Host "Archivos pendientes:" -ForegroundColor Yellow
$archivosPendientes | ForEach-Object { Write-Host "  - $_" -ForegroundColor White }
