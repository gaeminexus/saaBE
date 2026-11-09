# DISEÑO — PDF del simulador de novación, igual que los otros simuladores

omen-saa-1-arb · 2026-11-08 · Pedido del usuario: «la novación no se pasa a pdf como los otros simuladores, corregir
para que se pase a pdf como los otros simuladores». Ejecutores: omen-saa-1-be (BE) y omen-saa-1-fe (FE).

## Hoy (verificado)
- `POST /prst/simulacion/reporte` (`PrestamoRest:1331`) arma el PDF para `tipo` CREDITO_NUEVO (`RPRT_SMLC_NUEV`),
  ABONO_CAPITAL (`RPRT_SMLC_ABON`) y REESTRUCTURACION (`RPRT_SMLC_RSTR`): recalcula con el servicio del simulador y
  llena con `generarReporteDesdeColeccion("crd", nombre, parametros, tabla, "PDF")`. El FE no manda la tabla.
- `POST /prst/simularNovacion` → `ResultadoSimulacionNovacion` {original{idPrestamo, idAsoprep, producto,
  cuotaActual, vs, desgloseVs}, simulacion (ResultadoSimulacionCreditoNuevo), valorLiquido, evaluacion}. Sin PDF.
- FE: `simulador-credito.component.ts:179 exportarPdf()` y `simulador-prestamo` tienen el botón «Exportar PDF»;
  `simulador-novacion.component` no.

## BE
1. `SolicitudReporteSimulacion`: campo `novacion` (`SolicitudSimulacionNovacion`); tipo `NOVACION`.
2. `generarReporteSimulacion`: `case "NOVACION"` → `simulacionPrestamoService.simularNovacion(...)`, tabla =
   `resultado.getSimulacion().getTablaProyectada()`, reporte `RPRT_SMLC_NOVC`. Parámetros: los mismos del
   CREDITO_NUEVO (monto, tasa, plazo, tipo de amortización, fecha de inicio, totales, valor de cuota) más los de
   la novación: préstamo original (número Asoprep), producto, cuota actual, valor de sustitución (saldo que se
   cancela), valor líquido a recibir y, si viene, la evaluación (capacidad y cupo: cumple / no cumple / no
   evaluable). Actualizar los mensajes de «tipo debe ser…».
3. `RPRT_SMLC_NOVC.jrxml`: copiar `RPRT_SMLC_NUEV.jrxml` (mismo estilo, logo, tabla) y agregar un bloque
   «Préstamo a novar» con esos datos. Sintaxis compacta, sin `fontName="Tahoma"`, estilos con nombre.
   `compilar-jasper.bat` + `verificar-fill-jasper.bat`, y un PDF real por `generarReporteDesdeColeccion` buscando
   un texto del cuerpo (CLAUDE.md, trampas de reportes). No compartir PDFs con datos reales.
4. Actualizar `crd/API-CICLO-OTORGAMIENTO.md` (simulador de novación: PDF) y `PLAN-SIMULADORES-PRESTAMOS.md` §7.

## FE
- En `simulador-novacion`, botón «Exportar PDF» igual al de crédito nuevo (mismas banderas exportandoPdf/errorPdf,
  `guardarArchivo(blob, 'simulacion-novacion-<idAsoprep>.pdf')`, `mensajeReporteFallido`), habilitado solo con
  resultado. Manda `{tipo:'NOVACION', novacion:{idPrestamoOriginal, monto, plazo, …mismos campos que simula},
  nombreSocio, identificacionSocio}`. Agregar `novacion` al modelo `SolicitudReporteSimulacion`.
