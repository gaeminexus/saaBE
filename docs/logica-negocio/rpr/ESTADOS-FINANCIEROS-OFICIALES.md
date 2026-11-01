# Estados financieros oficiales (C19)

**2026-10-28 · tarea C19.** Responde a las observaciones del área financiera (correo del 8-oct-2026 sobre los
estados de sep-2026) y al pedido del líder de un formato oficial con logo y leyenda PRELIMINAR/DEFINITIVO.
Antecedente: [../cnt/DISENO-FORMATO-FORMAL-BALANCES.md](../cnt/DISENO-FORMATO-FORMAL-BALANCES.md).

## 1. Observaciones y cómo quedan

| Observación | Solución |
|---|---|
| (a) El Estado de Resultados se descarga como "Balance General" | Título oficial por tipo de reporte (`CNT.RPRT.RPRTTTLO`, C19-04; sin la columna, traducción del nombre). Archivo `titulo_AAAA-MM[_preliminar].pdf/.xlsx` (Content-Disposition). En el FE, el PDF interno también se llama como el estado y su período. |
| (b) En Excel no aparece el encabezado institucional | Excel generado por el backend con el mismo encabezado del PDF: logo, razón social, RUC, nombre del estado, período en letras, moneda. (El CSV del FE ya traía razón social/RUC/período desde saaFE `660dc54`.) |
| (c) Sobran "Nivel" y "Saldo Anterior" | No salen por defecto; parametrizable en la solicitud (`mostrarNivel`, `mostrarSaldoAnterior`). El nivel se ve en la sangría. |
| (d) Sumatoria final sin valor y falta el resultado | Se quita la suma de grupos de nivel 1. Al final: **UTILIDAD DEL EJERCICIO** o **PÉRDIDA DEL EJERCICIO**; en el estado de situación además "TOTAL PASIVO + PATRIMONIO + RESULTADO" y, solo si no cierra, una línea de **DESCUADRE** (activo − pasivo − patrimonio − resultado). |
| Logo de ASOPREP | El empaquetado `rep/img/logo_aso.jpeg`; punto de configuración: rubro 250 detalle 13 (ruta de archivo en el servidor). |
| PRELIMINAR / DEFINITIVO | `EstadoPeriodoConsulta` → `EstadoCierreService.estadoPeriodo` de C17: DEFINITIVO solo con el cierre contable marcado (`CNT.PRDO.PRDOCNCR = 1`). PRELIMINAR: leyenda roja en el encabezado, marca de agua y sufijo `_preliminar` en el archivo. |

## 2. Otras funcionalidades agregadas

- **Comparativo**: `MES_ANTERIOR`, `CIERRE_ANIO_ANTERIOR` (dic del año anterior, el comparativo NIIF del estado de
  situación), `MISMO_MES_ANIO_ANTERIOR`; columnas comparativo · actual · variación.
- **Firmas**: representante legal y contador general (nombre, cargo, registro CPA) del rubro 250; nombres en
  blanco = se firma a mano (decisión: no se siembran nombres hasta que ASOPREP los confirme).
- **Notas** al pie (texto libre).
- **Cifras** en dólares, dos decimales, negativos entre paréntesis; corte a 6 dígitos (Res. SBS-2013-0507,
  parametrizable); sin filas en cero (parametrizable).
- **Trazabilidad**: pie con usuario, fecha, estado del período e ids de ejecución de CNT.DTMT usados.

## 3. Cómo se arma

`EstadoFinancieroOficialServiceImpl`:
1. Genera el balance con el motor de siempre (`TempReportesService.reporteRangoFecha`, igual que
   `/dtmt/generarBalance`), y otro para el comparativo; lee sus filas y **las borra al final** (misma transacción).
2. Filtra por dígitos y ceros, ordena por cuenta, sangría por nivel, negrita en grupos.
3. Resultado del ejercicio = ingresos − gastos: si el reporte trae las cuentas de nivel 1 de ingresos (5) y gastos
   (4) — estado de resultados — de sus filas; si no — estado de situación — Σ(haber − debe) de 4 y 5 en la misma
   ventana (acumulado: desde el inicio de la contabilidad, igual que la ecuación del B17 1−2−3−(5−4)). Supone que el
   cierre anual de resultados está asentado; si no, la línea de descuadre lo muestra.
4. Llena `rep/cnt/RPRT_ESTADO_FINANCIERO_OFICIAL.jasper` desde la colección de `FilaEstadoFinanciero`
   (`ReporteService.generarReporteDesdeColeccion`); Excel con `IS_IGNORE_PAGINATION` y sin marca de agua ni pie.

## 4. Endpoints

- `POST /esfo/encabezado` → razón social, RUC, título, período, moneda, leyenda, estado del período, firmas,
  nombre de archivo.
- `POST /esfo/datos` → encabezado + títulos de columnas A/B/C + filas + resultado + diferencia de la ecuación.
- `POST /esfo/imprimir` → PDF (por defecto) o `formato: "EXCEL"`.
- Cuerpo: `{empresa, codigoAlterno, fechaInicio?, fechaFin, acumulacion? (0|1, def. 1), nivelMaximo?,
  eliminarSaldosCero?, mostrarSaldoAnterior?, mostrarNivel?, comparativo?, notas?, formato?, usuario?}`.
- FE (saaFE rama `cuadre/c19-balances-oficiales`): botones "Estado oficial PDF/Excel" y selector de comparativo
  en Balance General.

## 5. Parámetros (rubro 250, `CntParametroEstadoFinanciero`, semilla C19-04)

1 razón social · 2 RUC · 3/4 nombre/cargo representante · 5/6/7 nombre/cargo/registro contador · 8/9/10/11/12
cuentas de nivel 1 de ingresos/gastos/activo/pasivo/patrimonio · 13 ruta del logo · 14 dígitos máximos ·
15 línea de moneda. Sin el rubro todo tiene valor por defecto.

## 6. Pendiente

Estado de cambios en el patrimonio, flujo de efectivo NIIF (mismo motor de estructuras, falta el formato con el
mapeo) y notas estructuradas. Probar `/esfo` desplegado (el Jasper se verificó con verificar-fill y llenado con
datos de sep-2026 fuera del servidor).
