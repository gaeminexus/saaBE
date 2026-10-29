# Cierres bloqueantes: cartera y período contable (C17)

**Proyecto:** cuadre Superintendencia ASOPREP (requerimiento del líder del 28-oct-2026).
**Rama:** `cuadre/c17-cierres-bloqueantes` (desde `main` = `e31e0bb1`). **Estado:** compilado
(`mvn -q compile` = 0), sin desplegar ni probar en WildFly. DDL pendiente de correr:
`sql/C17-01-cierre-contable-y-bitacora.sql` (va ANTES del WAR). Control: `sql/C17-02-control-estado-cierres.sql`.

## 1. Estado de partida (levantamiento)

| Pieza | Qué había | Problema |
|---|---|---|
| `CNT.PRDO.PRDOESTD` (rubro 22) | 1 activo, 2 mayorizado, 3 desmayorizado, 4 CERRADO (constante sin quien lo asigne) | "Mayorizado" no bloqueaba asientos nuevos (`validacionAsiento` solo miraba el 4); la cadena de saldos (`selectAnteriorMayorizado`, desmayorización) depende de `estado = 2`, así que reusar el 4 rompía saldos |
| `AsientoServiceImpl.saveSingle` | punto central de los motores (`AsientoContableServiceImpl.grabarAsiento`) y del REST | validaba solo estado 4 |
| `anulaAsiento` | anula; si mayorizado, reversa con fecha de HOY | sin noción de "cierre"; los documentos de CxC/CxP anulaban el asiento con `em.merge` por fuera |
| Cartera | `CRD.CRCT` (corrida PREPARADA/EJECUTADA/REVERSADA), `POST /cierrecartera/reversar/{id}` | el mes cerrado no bloqueaba nada; reversar no tenía orden ni motivo obligatorio |
| CRD | `validarFechaAfectacion` (CBCR, precancelación, abono con aportes, acuerdos) → `verificaPeriodoAbierto` | solo contable; el resto de procesos de cartera sin validación |
| Reportes | G40–G51 (`/ejrc/ejecutar`), CPRM/CJBM/CCPM (`/ejcc/ejecutar`) | se generaban para cualquier mes |

Base de trabajo al 28-oct-2026: ningún período contable cerrado; 2026-01 mayorizado; corridas de cartera
EJECUTADAS: **2025-05 (arranque), 2026-07, 2026-08, 2026-09** → con este WAR esos cuatro meses quedan
**cerrados para cartera**.

## 2. Modelo de estados

**Contable** (por empresa y mes): `ABIERTO` / `CERRADO`. Cerrado = `PRDOCNCR = 1` (columna nueva) o el estado
legado 4. Es **independiente** de la mayorización: cerrar exige el período MAYORIZADO y lo deja MAYORIZADO;
así no se toca la cadena de saldos. Fecha y usuario: `PRDOFCCN`, `PRDOUSCN`.

**Cartera** (por empresa y mes): cerrado = existe corrida `CRD.CRCT` con `CRCTIDST = 2` (EJECUTADA) y
`CRCTESTD = 1`. No hay columna nueva: la corrida ES el cierre. Reabrir = reversar la corrida.

**Bitácora:** `CNT.HCPR` (una fila por cierre/reapertura, contable o cartera, con usuario, IP, motivo,
corrida/período y si fue fuera de orden). Solo INSERT.

Servicio central: `EstadoCierreService` (`cnt`). Todo "¿está cerrado?" pasa por él o por
`CierrePeriodoListener.contableCerrado(Periodo)`.

## 3. Reglas

1. **Mes cerrado para cartera** (A): ningún movimiento de cartera ni de cuentas individuales con fecha de
   negocio en ese mes. La fecha que manda es la **fecha de afectación** (o, donde no existe, la fecha del
   movimiento: pago, devolución, carga, período de pensión, corte). La fecha de pago real de un cobro
   tardío puede ser de un mes cerrado: eso lo resuelve la afectación en el mes abierto (mecanismo de cobro
   tardío existente), no se bloquea.
2. **Período contable cerrado** (B): ningún asiento nuevo, editado, con líneas nuevas o eliminado desde
   ningún módulo. Tres barreras: (a) validación temprana en el proceso de módulo; (b) `AsientoService`
   (`saveSingle`, `validacionAsiento`, reverso, detalle); (c) `CierrePeriodoListener` JPA
   `@PrePersist/@PreRemove` en `Asiento` y `DetalleAsiento`. `IncomeException` es
   `@ApplicationException(rollback = true)`: si la barrera central salta, el contenedor revierte todo lo
   que el proceso de módulo hubiera escrito en la misma transacción.
3. **Anulaciones** de algo de un período contable cerrado: no se anula ni se borra el original; se genera un
   asiento de reverso en el **primer período abierto posterior** (no cerrado ni mayorizado; dentro de él
   hoy si hoy cae ahí, si no su primer día) o en la fecha que indique el usuario (tiene que estar abierta).
   Original y reverso quedan enlazados por `ASNTASRV` en los dos sentidos; el original queda REVERSADO con
   el rastro (`ASNTUSAN`, `ASNTFCAN`, `ASNTMTAN`). El movimiento del módulo se marca anulado como siempre y
   su asiento original apunta al reverso.
4. **Anulaciones de cartera**: mes abierto para cartera → sin restricción nueva. Mes cerrado para cartera con
   contabilidad abierta → bloquea (anular cambiaría la contabilidad de un mes cuya cartera ya se informó:
   reabrir la cartera). Mes cerrado para las dos → se permite; el efecto va en la fecha del reverso, cuyo mes
   tiene que estar abierto para cartera (`validarAnulacionCartera`).
5. **Orden**: se cierra del más antiguo al más reciente (ningún mes posterior cerrado del mismo tipo; si ya
   hay cierres, el mes anterior cerrado — el arranque de cartera está exento). Solo se reabre el **último**
   mes cerrado (cascada). Contable de M exige cartera de M cerrada. Reabrir cartera de M exige contable de
   M y de M+1 abiertos (el cierre de cartera de M asienta en el último día de M y en el primer día de M+1).
6. **Parámetro de re-proceso**: `-Dsaa.cierres.permitirFueraDeOrden=true` (system property del WildFly,
   apagado por defecto) desactiva SOLO las reglas de orden del punto 5 (cerrar y reabrir); queda
   `HCPRFRDN = 1` en la bitácora. Los bloqueos por mes cerrado nunca se desactivan.
7. **Nada depende de la fecha de hoy**: el bloqueo es por estado explícito. Un mes abierto se trabaja aunque
   haya meses posteriores con datos.

## 4. Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/prdo/estado/{idEmpresa}/{anio}/{mes}` | `EstadoPeriodo`: `estado` ABIERTO/CERRADO (contable), `definitivo`, `leyenda` (DEFINITIVO / PRELIMINAR), fecha/usuario de cierre, `estadoCartera`, corrida |
| POST | `/prdo/cerrar?idEmpresa&anio&mes&usuario&ip&motivo` | Cierre contable (exige mayorizado + cartera cerrada + orden) |
| POST | `/prdo/reabrir?idEmpresa&anio&mes&usuario&ip&motivo` | Reapertura contable (último cerrado; motivo obligatorio) |
| POST | `/cierrecartera/ejecutar` | (existente) ahora valida orden y períodos contables abiertos; registra bitácora |
| POST | `/cierrecartera/reabrir?idEmpresa&anio&mes&usuario&ip&motivo` | Reapertura de cartera = reverso de la corrida EJECUTADA del mes |
| POST | `/cierrecartera/reversar/{idCorrida}` | (existente) ahora es una reapertura: motivo obligatorio, mismas reglas, bitácora |
| POST | `/asnt/anular/{idAsiento}?usuario&motivo&fechaReverso` | Anulación manual con la regla del punto 3 |
| GET | `/prdo/bitacora/{idEmpresa}?tipo=CONTABLE\|CARTERA` | Bitácora `CNT.HCPR` de la empresa, la más reciente primero (solo lectura; `tipo` opcional). Rama `cuadre/c17-fe-soporte`, para la pantalla de cierre de períodos del frontend |

| GET | `/prdo/controlCierre/{idEmpresa}/{anio}/{mes}` | Semáforo de controles de cierre (solo lectura). Ver `CONTROL-CIERRE-MES.md` |

Permisos de las pantallas (saaFE `main` desde el pase a producción): nodos 1558–1561 de `SCP.PJRQ`, script
`docs/logica-negocio/seguridad/sql/C17-FE-01-permisos-cierres.sql`, más el 1412 (CIERRE DE CARTERA) que ya
existía. Qué asignar a cada rol: `CONTROL-CIERRE-MES.md` §6.

Errores (prefijo del mensaje): `PERIODO_CERRADO`, `CARTERA_CERRADA`, `REPORTE_MES_NO_CERRADO`,
`CIERRE_FUERA_DE_ORDEN`. `/cbcr`, `/prst` y `/ejrc` los responden 400; `/prdo/cerrar|reabrir|estado` y
`/cierrecartera/reabrir` responden 400 ante cualquier regla de negocio.

Flujo mensual: operar el mes → `POST /cierrecartera/ejecutar` → generar G40–G51 / EJCC → mayorizar →
`POST /prdo/cerrar` → EEFF definitivos. Para corregir: `POST /prdo/reabrir` (si estaba cerrado) →
`POST /cierrecartera/reabrir` → corregir → volver a cerrar. Con meses posteriores cerrados, en cascada
desde el más reciente.

## 5. Matriz módulo × operación × control

Leyenda: **CC** = `validarMovimientoCartera` (cartera + contable abiertos), **CA** = solo cartera,
**CT** = solo contable, **AN** = `validarAnulacionCartera`, **★** = barrera central (`AsientoService` +
listener JPA), **REV** = anulación por reverso en período abierto.

| Módulo | Operación | Fecha | Control |
|---|---|---|---|
| CRD CBCR | registrar / editar y reenviar | afectación | CT + CA (en `validarFechaAfectacion`) |
| CRD CBCR | aprobar, procesar | afectación | CC |
| CRD CBCR | anular procesado, reversar proceso | afectación del cobro | AN + REV |
| CRD pagos | pagar cuota / múltiples (REST directo) | fechaPago | CC (en `PrestamoRest`; desde CBCR lo cubre CBCR) |
| CRD pagos | pago con aportes | fechaPago | CC |
| CRD pagos | precancelación, abono con aportes, acuerdo de condonación | afectación | CT + CA (`validarFechaAfectacion`) |
| CRD pagos | abono a capital | afectación o fecha | CC |
| CRD pagos | anular operación (evento) | fecha del evento | AN + REV |
| CRD préstamo | aprobar (desembolso, novación, reestructuración, refinanciación) | fechaContable o hoy | CC |
| CRD préstamo | desembolso histórico | fechaContable | CC |
| CRD préstamo | anular | hoy / fecha de inicio | CC(hoy) + AN |
| Petro | carga de archivo | mes de afectación | CC |
| Petro | aplicar pagos (incluye modo re-proceso), Fase 2 legacy | mes de afectación | CC |
| Petro | re-proceso de carga | fin del mes de afectación | CC |
| Petro | confirmar recepción / reversar recepción | mes de afectación | CC / AN |
| Devoluciones | registrar, beneficiarios / anular | fecha | CC / AN + REV |
| Pensión complementaria | seguro, pensiones (lote e individual) | período (anio, mes) | CC |
| Mora persistida | diaria y por préstamo | fecha de corte | CA |
| Aportes | registrar (REST), CRUD (alta/estado), jubilación / reversar | fecha | CC / CA(hoy) + AN |
| Recepción valor de seguro | registrar, aprobar / anular | fecha | CC / AN + REV |
| Plazo vencido | declarar / revertir | corte | CC / CA(hoy) + AN |
| Cierre de cartera | ejecutar / reabrir | corte y corte+1 | orden + CT / orden + CT(M, M+1) |
| CNT | asiento manual (REST), cargador C5, detalle de asiento | fecha del asiento | ★ + CT en C5 |
| CNT | mayorizar / desmayorizar | período | rechaza períodos cerrados |
| CNT | anular (`/asnt/anular`, `anulaAsiento`) | — | REV |
| Nómina | rol, provisiones, pago | fecha contable / acreditación | CT + ★ |
| Nómina | descontabilizar | — | bloquea en cerrado (reabrir): descontabilizar es para recontabilizar en el mismo período |
| Tesorería | ingresos, egresos | fecha | CT + ★ |
| CxP | respuesta del banco, confirmación manual | fechaPago / respuesta | CT + ★ |
| CxP / CxC | demás procesos que contabilizan (documentos, aplicaciones, anticipos, caja chica) | fecha del documento | ★ |
| CxC / CxP | anular factura, NC, ND, retención v1/v2, liquidación, bandeja de documentos | — | REV (`anulaAsientoDeDocumento`) |
| Todas | anulaciones vía `anulaAsiento` (cobros, pagos, devoluciones, Petro, nómina, tesorería, CxP, cierre de cartera) | — | REV |
| RPR | G40–G51, CPRM/CJBM/CCPM (ejecutar y regenerar) | mes | exige cartera cerrada |
| EEFF / B17 | balances | mes | `estadoPeriodo` (DEFINITIVO / PRELIMINAR); `validarReporteFinanciero` disponible |

### No cubierto (y por qué)

- **Documentos de seguro** (`DocumentoSeguroServiceImpl.distribuir/anular`): cambian el seguro de cuotas
  futuras y no tienen una fecha de negocio que caiga en un mes cerrado; sus asientos los cubre ★.
- **CRUD genéricos de `PGPR`, `EVPR`, `DTPR`, `DVAP`, `PGPC`**: los usan por dentro los motores; validar ahí
  rompería flujos internos. Los procesos de negocio que los usan ya validan.
- **`em.merge` de `DetalleAsiento`** en `PagoProgramadoServiceImpl` (descripción) y carga histórica: son
  UPDATE de texto; el listener no intercepta UPDATE (ver §3, regla 2). Riesgo bajo.
- **Reportes G y EJCC sin empresa**: validan "cualquier empresa con el mes cerrado" (ASOPREP opera con una).
- **Formato de EEFF definitivo/preliminar**: lo hace `cuadre/c19-balances-oficiales` consumiendo
  `estadoPeriodo`.
- Anulaciones que hoy reversan con `reversionAsiento(id, LocalDate.now())` explícito (anulación de préstamo,
  pagos CxP sin fecha): el reverso cae hoy, que siempre es un período abierto en operación normal; si hoy
  estuviera cerrado, falla con `PERIODO_CERRADO`.

## 6. Impacto en el re-proceso (jun-2025 → nov-2026)

- **Cuatro meses ya quedan cerrados para cartera** al subir el WAR (2025-05, 2026-07, 2026-08, 2026-09): no
  admiten cobros, cargas Petro, devoluciones, etc. con afectación en esos meses hasta reabrirlos. 2025-05
  es el ancla: debe quedar cerrado.
- **Cerrar 2025-06 con las corridas de 2026 vivas viola el orden** ("hay un mes posterior cerrado"). Dos
  salidas: (a) reabrir 2026-09 → 2026-08 → 2026-07 en cascada (reversa sus asientos de cierre en la base de
  trabajo; coherente con D-026 "base limpia") o (b) arrancar el WildFly del re-proceso con
  `-Dsaa.cierres.permitirFueraDeOrden=true`. Recomendado (a) en la base de trabajo; (b) solo si se quiere
  conservar esas corridas como referencia.
- Los G40–G51 de un mes del re-proceso solo se generan después de su cierre de cartera; si el G no cuadra,
  se reabre la cartera, se corrige y se vuelve a cerrar.
- El cierre contable exige el período mayorizado: mayorizar en orden desde 2025-05 (la mayorización toma el
  primer período no mayorizado; 2026-01 ya figura mayorizado en la base).

## 7. Riesgos para producción

- Al desplegar, jul/ago/sep-2026 quedan cerrados para cartera: un cobro tardío con afectación en esos meses
  ahora falla (`CARTERA_CERRADA`); hay que afectarlo en octubre o reabrir.
- `POST /cierrecartera/reversar` ahora exige motivo y orden: el frontend que lo llame sin motivo recibirá error.
- El WAR sin el DDL C17-01 falla al leer `Periodo` (ORA-00904). Orden: DDL → WAR.
- Anulaciones de documentos de CxC/CxP de un período cerrado ya no dejan el asiento ANULADO: generan reverso.
- Sin prueba en WildFly todavía: falta el ciclo cerrar → intentar operar (bloqueo) → anular (reverso) →
  reabrir en cascada en la base de trabajo.

## 8. Historial de la desmayorización (D-043, 29-oct-2026)

`MayorizacionServiceImpl.desmayorizacion` (procesos 3 y 4):

| Qué | Antes | Ahora |
|---|---|---|
| Copia de saldos por cuenta (`CNT.MYRH`/`DTMH`) | siempre | siempre |
| Copia de asientos y líneas (`CNT.ASNH`/`DTAH`) | siempre | **solo si la mayorización que se deshace sostuvo un cierre contable definitivo** |
| Desvincular los asientos de la mayorización (`ASNT.MYRZCDGO = null`) | siempre | siempre |
| Respaldo anterior del período (`PRDODSMY`/`PRDODMCR`) | se borraba | se borra, **salvo que sea la foto de un cierre contable** (se conserva) |

**Cómo se sabe que hubo cierre.** Al desmayorizar el período ya no puede estar cerrado (`exigeNingunoCerrado`:
hay que reabrirlo, y `reabrir` limpia `PRDOCNCR`, `PRDOFCCN` y `PRDOUSCN`). Por eso no sirve mirar el período:
se lee la bitácora `CNT.HCPR`, que nunca se actualiza ni se borra. La mayorización sostuvo un cierre si hay una
fila `HCPRTPOO = 'CONTABLE'`, `HCPRACCN = 'CIERRE'` del período (`PRDOCDGO`, o empresa/año/mes si la fila no lo
trae) con `HCPRFCHA >= MYRZFCHA`: cerrar exige el período MAYORIZADO, así que ese cierre se hizo sobre esa
mayorización. Un respaldo anterior es foto de un cierre si hay un cierre entre su `MYRHFCHA` (copia de
`MYRZFCHA`) y la fecha de la mayorización que ahora se deshace. Por defensa también copia si el período sigue
cerrado (bandera C17 o estado legado 4), aunque `exigeNingunoCerrado` ya lo impide. Los cierres anteriores a
C17 no dejaron bitácora y no cuentan.

Ejemplo (base de trabajo, 29-oct): mayo y junio 2025 se mayorizaron a las 06:11 (MYRZ 2300/2301), se cerraron
a las 06:11:50 (HCPR 41/42), se reabrieron a las 07:39 (HCPR 43/44) y se desmayorizaron (MYRH 1004/1003, con
asientos). Con esta regla una desmayorización sin cierre de por medio copia solo saldos, y la de un período
cerrado y reabierto copia también los asientos y no borra la foto anterior de otro cierre.

`HistAsiento.ASNHOBSR` pasa a `length = 2000` (igual que `ASNTOBSR`); la columna ya se amplió en la base
(D-043, DDL aplicado el 29-oct: `ALTER TABLE CNT.ASNH MODIFY ASNHOBSR VARCHAR2(2000)`; va a producción antes
del WAR).

**Período DESMAYORIZADO = abierto.** `PeriodoServiceImpl.verificaPeriodoAbierto` (CBCR al registrar y procesar,
precancelación, abono con aportes, pensión complementaria, `/prdo/verificaPeriodoAbierto`) acepta ACTIVO (1) y
DESMAYORIZADO (3); sigue rechazando MAYORIZADO (2), CERRADO (4) y el cierre C17. Es la misma regla de los
asientos (`AsientoServiceImpl` solo rechaza MAYORIZADO y CERRADO) y de la mayorización (toma ACTIVO y
DESMAYORIZADO). Antes el re-proceso de un mes desmayorizado obligaba a pasar `PRDOESTD` a 1 a mano
(`44b`/`44c` del expediente).
