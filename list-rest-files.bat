@echo off
echo Actualizando archivos REST de tesoreria...
echo.

REM Lista de archivos REST de tesoreria que necesitan actualizacion
set FILES=UsuarioPorCajaRest TransferenciaRest TempPagoRest TempMotivoPagoRest TempMotivoCobroRest TempDebitoCreditoRest TempCobroTransferenciaRest TempCobroTarjetaRest TempCobroRetencionRest TempCobroRest TempCobroEfectivoRest TempCobroChequeRest TelefonoDireccionRest SaldoBancoRest PersonaRolRest PersonaCuentaContableRest MovimientoBancoRest MotivoPagoRest MotivoCobroRest HistDetalleConciliacionRest HistConciliacionRest GrupoCajaRest DireccionPersonaRest DetalleDepositoRest DetalleDebitoCreditoRest DetalleConciliacionRest DetalleCierreRest DesgloseDetalleDepositoRest DepositoRest DebitoCreditoRest ConciliacionRest CobroTransferenciaRest CobroTarjetaRest CobroRetencionRest CobroEfectivoRest CobroChequeRest CierreCajaRest ChequeRest ChequeraRest CajaLogicaPorCajaFisicaRest AuxDepositoDesgloseRest AuxDepositoCierreRest AuxDepositoBancoRest

set BASE_PATH=C:\work\saaBE\v1\saaBE\src\main\java\com\saa\ws\rest\tesoreria

for %%f in (%FILES%) do (
    echo Procesando: %%f.java
)

echo.
echo Proceso completado.
pause
