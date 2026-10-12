# PEDIDOS DE CONTABILIDAD Y FINANCIERO — correos del 2026-10-08 al 2026-10-12

**Equipo:** `omen-saa-2` · **Escrito:** 2026-10-12 por `omen-saa-2-arb`, **antes** de despachar (regla 7).
**Fuente:** el correo del usuario, revisado desde el jueves 2026-10-08. Cada pedido se contrastó contra el
código el mismo día; lo que dice «verificado» se midió en `main` al 2026-10-12, y no se tomó de la memoria.

| # | Pedido | Quién | Fecha | Módulo | Tipo de arreglo |
|---|---|---|---|---|---|
| P1 | El detalle del ATS no identifica las notas de venta | Ana | 07-10 y 12-10 | `sri`/`cxc` FE | código FE |
| P2 | No puede recargar el extracto de Pacífico (cta. cte.) y por eso no cierra el banco | Ana | 12-10 (WhatsApp desde la semana anterior) | `tsr` | despliegue + `e2-95` |
| P3 | Anticipo y garantía registrados al cliente equivocado | Ana | 08-10 | `cxc` | datos — `e2-96` primero |
| P4 | Estados financieros en Excel: nombre del archivo, encabezado, columnas y resultado del ejercicio | Jimena (financiero) | 08-10 | `cnt` FE | código FE |
| P5 | Desde octubre, lo que no toca BANCOS va como asiento de DIARIO, no como Ingreso/Egreso | Ana | 06-10, recordado el 08-10 | `tsr` (y quizá otros) | inventario primero |
| P6 | Información para el reproceso que pidió la Superintendencia (anexo de la auditora, roles 2025) | Steven | 08-10 | regulatorio | informativo |
| P7 | Excel con todas las transferencias de los bancos Internacional y Pacífico | Steven | 09-10 | — | informativo |

---

## P1 — El detalle del ATS no identifica las notas de venta

**Lo que pidió Ana:** *«El reporte del ats no me está identificando las notas de ventas, únicamente están
facturas y liquidaciones.»* El 07-10 mandó la captura: las notas de venta salían en 0 y con tipo «factura».

**Lo medido:**
- Los ceros eran de datos y ya están corregidos: `e2-93`/`e2-94`, más `e8d68ebf` para que no vuelva a pasar.
- El **XML del ATS está bien**: declara `tipoComprobante = 02` a las notas de venta.
- La **pantalla** no lo muestra. En `GeneradorAtsServiceImpl:597` todo lo que viene de `PGS.FCTC` lleva
  `origen = "FACTURA"`, por contrato (`sri/API-DETALLE-ATS.md` §3, punto 3: *«el origen es la tabla y el
  tipo se lee de `tipoComprobante`»*). Y `ats-detalle.component.html:104` muestra el **código crudo**
  (`01`, `02`, `03`) en la columna «Tipo», mientras que el desplegable muestra «FACTURA». Una nota de
  venta se ve como `02` + `FACTURA`: el usuario lee «factura».

**El arreglo (solo FE; el contrato no cambia):**
1. En `ats-detalle.component.ts`, una función pura `nombreTipoComprobante(codigo: string): string`:
   `01` Factura · `02` Nota de venta · `03` Liquidación de compra · `04` Nota de crédito ·
   `05` Nota de débito · `18` Factura (venta) · `41` Comprobante de reembolso · cualquier otro → el código tal cual.
2. La columna «Tipo» de compras muestra `código — nombre` (ej. `02 — Nota de venta`). Lo mismo en las tablas
   de ventas, anulados y retenciones que muestran `tipoComprobante` (`.html:304`, `:345`, `:389`).
3. En el desplegable, «Origen» muestra el nombre del tipo y no la tabla: `{{ nombreTipoComprobante(row.tipoComprobante) }} (ID …)`.
4. En los CSV, una columna nueva **«Tipo (nombre)»** inmediatamente después de «Tipo comprobante». Las
   columnas existentes no se renombran ni se quitan: el CSV ya se compara contra el ATS y alguien puede
   tener una plantilla armada encima.
5. Si la pantalla tiene filtros o totales por origen, deben distinguir nota de venta de factura. Si no los
   tiene, no se agregan.

**No se toca:** el backend ni el contrato. `origen` sigue siendo la tabla.

---

## P2 — Recargar el extracto de Pacífico

**Lo que pidió Ana:** no puede cerrar el Banco Pacífico (cta. cte.) porque no la deja recargar el extracto.

**Lo medido:** es el caso del `e2-95` (cuenta 419, período 4519, septiembre). El 500 sin mensaje ya se
corrigió el 08-10: `recargar`, `validar` y `confirmar` devuelven **400 con el motivo** (`f3473cab`,
`19363599`) y la pantalla lo muestra (FE `d85c946`). **Si Ana sigue viendo el error genérico, el WAR o el FE
de producción son anteriores al 08-10.**

**Lo que falta, y no es código:**
1. Desplegar el WAR y el FE de `main`.
2. Que Ana reintente: ahora la pantalla dice **cuál** de las guardas la frena.
3. Correr el `e2-95` en paralelo: reproduce las seis guardas (extracto, filas conciliadas, partidas en
   tránsito, período cerrado, conciliación verificada, archivo repetido) y dice cuál aplica sin depender
   de que la pantalla se despliegue.
4. Según la guarda, la salida puede ser **operativa** (desconciliar o reabrir antes de recargar) y no un
   defecto. **No se despacha código hasta saber cuál es.**

---

## P3 — Anticipo y garantía al cliente equivocado

**Lo que pidió Ana:** el anticipo por arriendo del 08/09 y una garantía de agosto se registraron al RUC
**1792458560001**; eran del **1793221709001**, al que se facturó el 06/10. Pide dejar el saldo a favor en el
segundo y *«eliminar del sistema sin dejar rastro»* al primero.

**Diagnóstico:** `cxc/sql/e2-96-anticipo-y-garantia-registrados-al-cliente-equivocado.sql`, de solo lectura.
Contesta lo que hace falta para escribir la corrección:
- si el anticipo y la garantía están en `CBR.ANTC` o en `TSR.INGR` (BLUEBODY, en el `e2-90`, resultó ser un ingreso de tesorería);
- si sus asientos usan una cuenta contable **propia** del cliente (`PRCC`). Si la usan, hay que reclasificar
  en contabilidad; si no, basta con cambiar el titular;
- qué más apunta al titular equivocado. Mientras algo apunte a él, no se puede borrar.

**Decisiones del usuario, después del `e2-96`:**
- *«Sin dejar rastro»* **no se recomienda.** Borrar el titular solo es posible si no le queda **ninguna**
  referencia (bloques 7 y 8). Lo seguro es moverle todo al titular correcto y dejar el equivocado
  **inactivo** (`TTLRESTD`), con la observación de por qué. El asiento original sigue siendo un hecho
  contable y no se borra.
- Si el período de agosto o el de septiembre está mayorizado o cerrado y hay que reclasificar, la
  reclasificación va con un asiento de ajuste en el período abierto, nunca reescribiendo agosto.

---

## P4 — Estados financieros en Excel (pedido de Jimena Viteri, financiero)

**Pantalla:** Contabilidad → Reportes → Balance General
(`saaFE/src/app/modules/cnt/forms/reporte-balance-general/`). El «Tipo de Reporte» (`codigoAlterno`)
elige entre los estados. El «Excel» es en realidad el **CSV** que arma el navegador en
`reporte-balance-general.component.ts:~260-345`, con `ExportService.exportToCSV`. El PDF es Jasper y
**no se toca** en este pedido.

> El ítem «Estado de Resultados» del menú (`menucontabilidad.component.ts:177-180`) apunta a
> `/menucontabilidad/reportes/estado-resultados`, ruta que **no existe** en `app.routes.ts`. Por eso el
> estado de resultados se saca desde Balance General eligiendo el tipo. Arreglar el menú no es parte de
> este pedido, pero el ejecutor debe confirmarlo y reportarlo.

| # | Observación | Arreglo |
|---|---|---|
| P4.1 | El estado de resultados se descarga con el nombre «Balance General» | El nombre del archivo sale del `nombreReporte` del tipo elegido (`reportes()`), normalizado a minúsculas, sin tildes y con `_`, más el período: `estado_de_resultados_2026-09.csv`. No se usa más el literal `balance_general_` |
| P4.2 | El Excel no trae el nombre de la entidad ni el período | Antes de la fila de títulos, un **encabezado** de 3 filas: razón social de la empresa de la sesión · RUC · `«<nombre del reporte> — del <fechaInicio> al <fechaFin>»` (o `«al <fechaFin>»` si es «A fecha de corte»), más una fila en blanco. Los datos de la empresa salen del mismo lugar del que ya los lee el FE para otros reportes: **buscarlo; si no existe, reportar y detenerse**, no inventar la clave de `localStorage` (§54.2: `userName` es una clave que nadie guarda) |
| P4.3 | Sobran las columnas «Nivel» y «Saldo Anterior» | Se quitan **del CSV**. La pantalla queda igual. Debe/Haber se mantienen cuando están visibles |
| P4.4 | La sumatoria final no sirve; falta el resultado del ejercicio | Se quita la fila `TOTALES`. Al final va **`UTILIDAD DEL EJERCICIO`** o **`PÉRDIDA DEL EJERCICIO`** con el valor absoluto. Regla: resultado = ingresos (grupo **5**) − gastos (grupo **4**), a partir de las filas de **nivel 1** de esas dos cuentas, con el «Saldo Actual». Se pone en los dos estados |

**Trampas de P4.4 que el ejecutor tiene que medir, no suponer:**
- **El signo.** Hay que leer cómo llega `valorActual` de las cuentas 4 y 5. Si los ingresos vienen
  negativos por naturaleza acreedora, la fórmula cambia. Se verifica con los números del reporte de
  septiembre en pantalla. Si no se puede determinar, se reporta y se detiene.
- **«Eliminar saldos en cero» u otro filtro** pueden ocultar las filas de nivel 1. El cálculo se hace sobre
  los datos completos que devolvió el backend, no sobre las filas filtradas.
- Si el plan de cuentas no usa 4 = gastos y 5 = ingresos, se reporta y se detiene.

---

## P5 — Asientos de DIARIO para lo que no toca BANCOS

**Lo que pidió Ana** (06-10, recordado el 08-10): *«a partir de octubre, los registros contables en los que
no intervenga la cuenta "BANCOS" deberán registrarse como asientos diarios, y no como Ingresos o
Egresos»*. Lo acordaron con el usuario, Steven y Jimena.

**Lo medido:** no hay ningún commit al respecto, y `TipoAsientos` no tiene una constante de diario. Lo que
falta saber antes de diseñar:
1. Qué **procesos** generan hoy un asiento de tipo Ingreso o Egreso **sin** una línea de banco. Candidatos
   del alcance: el egreso pagado con anticipo (`a4e1e042`, que es exactamente el caso), los cruces, la
   devolución de anticipo y la caja chica.
2. Cómo se elige hoy la plantilla y el tipo de asiento (`AsientoContableServiceImpl.generarAsiento`), y
   cuál es la plantilla y la numeración del **diario** (`DIA-AAAA-MM-NNNN` ya existe: `DIA-2026-07-0002`).
3. Si hay procesos de **`crd`** en la misma situación (condonaciones, provisión). Esos son de
   `omen-saa-1`: **se reportan, no se tocan**, y el aviso a su árbitro necesita la autorización del usuario.

**Primer paso:** un inventario de solo lectura (BE). El diseño se escribe con el inventario en la mano.

### Inventario (omen-saa-2-be, 2026-10-12; lo que dice «verificado» lo leyó en el código)

**Cómo se elige hoy el tipo.** `AsientoContableServiceImpl.generarAsiento` (l.469/478) **no usa plantilla**:
resuelve el tipo con `tipoAsientoService.codigoByAlterno(codigoAlterno, idEmpresa)`. El prefijo del número
sale de las **3 primeras letras del nombre** del tipo (`AsientoServiceImpl.asignaNumeroAlterno` ~l.239), así
que `DIA-` existe porque en BD hay un tipo cuyo nombre empieza con «DIA». `TipoAsientos` **no tiene constante
de DIARIO**. Las dos familias de constantes (legacy 0–7 y alternos modernos) **chocan en valores** (2, 4, 5 y 1).
Qué fila de BD hay detrás de cada alterno falta medirlo con un script.

**Ningún código genera `DIA-`**: son asientos manuales. El `e2-82` muestra que contabilidad ya hacía **a mano
como diario** los mismos procesos que el sistema hoy genera como Egreso (`DIA-2026-07-0002` caja chica,
`DIA-2026-07-0036` cruce de anticipo).

**Procesos del alcance que generan Ingreso/Egreso SIN línea de banco:**

| Método (`AsientoContableServiceImpl`) | Llamado desde | Tipo hoy |
|---|---|---|
| `generarAsientoEgresoContraAnticipo` (l.1048) | `tsr/EgresoServiceImpl:312` (`a4e1e042`) | 5 EGRESO_TESORERIA |
| `generarAsientoAplicacionAnticipoProveedor` (l.866) | `cxp/AplicacionPagoCxpServiceImpl:636` | 5 APLICACION_ANTICIPO_PROVEEDOR |
| `generarAsientoAplicacionAnticipoCliente` (l.909) | `cxc/AplicacionPagoCxcServiceImpl:516` | 4 APLICACION_ANTICIPO_CLIENTE |
| `generarAsientoGastoCajaChica` (l.1120) | `tsr/MovimientoCajaChicaServiceImpl:242` | 5 (fijo) |
| `generarAsientoAplicacionCajaChica` (l.1150) | `cxp/AplicacionPagoCxpServiceImpl:978` | 5 (fijo) |
| `generarAsientoAjusteCajaChica` (l.1216) | `tsr/CierreCajaChicaServiceImpl:241` | 5 (fijo) |
| `generarAsientoAnticipo` (l.562) | `cxc/AnticipoClienteServiceImpl:285` | 4 — **no determinable**: depende de si la cuenta tipo 3 del cliente es un banco |

La devolución de anticipo **sí** lleva banco: no entra. Tampoco los cobros y pagos por transferencia, los
ingresos y egresos de tesorería, la reposición de caja chica ni el anticipo a empleado.

**Preguntas para Ana antes de diseñar:**
1. ¿La regla es solo para lo que hoy sale como Ingreso/Egreso, o para **todo** asiento sin banco? (RRHH tipo 6 y
   los documentos de compra/venta tipo 2/3 tampoco llevan banco.)
2. Los reversos conservan el tipo del original: un reverso de un Egreso viejo, ¿sigue siendo Egreso?
3. El código legacy de tsr (`insertarCabeceraAsiento`: cheques, cobros, depósitos, débitos/créditos, cierre de
   caja) no se clasificó a fondo. ¿Se usa todavía?

**crd** (de `omen-saa-1`; todos usan `TipoAsientos.CREDITOS`, no Ingreso/Egreso, así que en principio quedan
fuera del pedido literal): condonaciones, cruce de aportes, jubilación, cierre de cartera, cobros
(transitorio/reparto/definitivo), Petro, contabilización de préstamos, devolución de aportes, pensión
complementaria, provisiones, recepción de seguros. Solo se nombran.

---

## P6 y P7 — Informativos

- **P6** (Steven, 08-10, «ALCANCE DE REVISIÓN CXP Y CXC»): reenvía el avance de la auditora (Silvia Ávila)
  para el reproceso de la Superintendencia, con `control asoprep.xlsx` y los roles de junio a diciembre de
  2025. Es material del frente regulatorio (`CAMBIOS-SAA.md`), que lleva `omen-saa-3`. No pide un cambio
  al sistema.
- **P7** (Steven, 09-10): Excel de todas las transferencias de los bancos Internacional y Pacífico. Parece
  insumo de la revisión de inconsistencias del 07-10. El usuario confirma para qué lo pidió.
