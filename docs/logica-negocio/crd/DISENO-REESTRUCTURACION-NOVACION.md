# DISEÑO — Reestructuración y novación de créditos

**Fecha:** 2026-10-13 · **Equipo:** `omen-saa-1` · **Árbitro:** `omen-saa-1-arb`
**Base:** el ciclo de otorgamiento (`PLAN-CICLO-OTORGAMIENTO.md`, U3–U6), la anulación
(`DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md`) y las plantillas levantadas en
`LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §7 (alternos 2–7, 10, 11, 14, 15).
**Contrato:** `API-REESTRUCTURACION-NOVACION.md`.

---

## 1. Decisiones del usuario (2026-10-13). NO re-litigar

| # | Decisión |
|---|---|
| RN1 | **Reestructurar crea un PRÉSTAMO NUEVO** del producto *RESTRUCTURADO* de la misma familia, con su propio idAsoprep, que **cancela** el original. La cartera se traslada de familia de cuentas, por ejemplo de 1.3.01 a 1.3.03. |
| RN2 | Al reestructurar se capitaliza **capital + interés vencido + mora**, y también los seguros vencidos no pagados. |
| RN3 | **Novación = crédito nuevo con dinero adicional**, del producto *NOVACION*. Con él se cancela el saldo del anterior y **la diferencia se le desembolsa al socio** por tesorería. El anterior queda **CANCELADO POR NOVACIÓN (5)**. |
| RN4 | Se reestructura un préstamo **VIGENTE o EN MORA con hasta 2 meses vencido**: la cuota vencida más antigua tiene ≤ 60 días a la fecha de la operación. Si tiene más, no se reestructura por el sistema. |

## 2. Reglas técnicas (árbitro)

| # | Regla |
|---|---|
| RN5 | **Un solo circuito, el de otorgamiento.** En Créditos → Préstamos → Ingreso se elige *Tipo de operación*: NUEVO (el de hoy), REESTRUCTURACIÓN o NOVACIÓN. Con R o N se elige el **préstamo original** del mismo partícipe. Después: guardar → generar tabla → **Aprobar**. Lo que hoy hace aprobar sigue igual (U6: el idAsoprep se asigna al aprobar). Lo nuevo es que, si el préstamo sustituye a otro, `aprobar` cancela el original y contabiliza el traslado en **la misma transacción**. |
| RN6 | **Valor de sustitución** (VS) = lo que el socio debe del original a la fecha de aprobación: capital pendiente + interés vencido + mora + seguros vencidos no pagados. Lo calcula **el mismo servicio que la precancelación** (`calcularDesgloseConceptos` / `simularPrecancelacion`), nunca una fórmula nueva. Se muestra al elegir el original, para que el operador vea el monto. **Se recalcula al aprobar**, porque entre la solicitud y la aprobación pudo vencer otra cuota o entrar un pago. |
| RN7 | **Reestructuración:** monto del nuevo = VS **exacto**. Si al aprobar el VS cambió respecto del monto de la solicitud (más de 0,01), se rechaza la aprobación con el monto nuevo: hay que regenerar la tabla. |
| RN8 | **Novación:** monto del nuevo **mayor que** VS. **Desembolso neto = monto − VS**, que va a tesorería con el producto de pago deducido de la plantilla, igual que la entrega. Si al aprobar el VS cambió, la diferencia se ajusta sola: el monto del préstamo no cambia y cambia el neto. Si el neto queda ≤ 0, se rechaza. |
| RN9 | **El producto del nuevo** tiene que ser de la **misma familia** (tipo de préstamo) que el original: un quirografario se reestructura como quirografario. Para reestructurar, el producto elegido tiene que ser un *RESTRUCTURADO*; para novar, un *NOVACION*. Se valida por el nombre del producto contra la familia, y el BE lo propone automáticamente. |
| RN10 | **Novación con atrasos:** las plantillas de novación (2 y 3) no tienen líneas de interés ni de mora. Por eso **solo se nova un préstamo sin cuotas vencidas**. Con atrasos se reestructura, o el socio se pone al día primero. ⚠️ Decidible por el usuario: si quiere novar con atrasos, hay que agregar esas líneas a las plantillas 2 y 3. |
| RN11 | **El original:** sus cuotas pendientes pasan a CANCELADA_ANTICIPADA (7); queda un `EventoPrestamo` de tipo REESTRUCTURACION o NOVACION con el VS y el enlace al préstamo nuevo; estado **5 CANCELADO_POR_NOVACION** si es novación, o **13 CANCELADO_POR_REESTRUCTURACION** (constante nueva, terminal) si es reestructuración. Sin PagoPrestamo: no entra dinero. |
| RN12 | **Enlace:** el nuevo guarda el original en `PRSTPROR` y el VS en `PRSTVLSS`. Marca `PRSTRSTR = 1` si es reestructuración, o `PRSTESNV = 1` y `PRSTMNNV = VS` si es novación. Son las columnas que ya existían sin escritor. |
| RN13 | **Anular** un préstamo que sustituyó a otro **no se permite** en esta fase: habría que resucitar el original. `motivoNoAnulable` lo rechaza con mensaje. |
| RN14 | El 13 entra a las listas de estados terminales (pagos, abonos, cobros), igual que el 12. |

## 3. Contabilidad — un asiento al aprobar

| Línea | De dónde |
|---|---|
| **DEBE** cartera del NUEVO, por banda | `CRD.BNDP` del **producto nuevo**, cuotas por vencer. Es la misma rutina que la entrega. |
| **HABER** capital del ORIGINAL, por banda | `CRD.BNDP` del **producto original**, con la clasificación de cada cuota pendiente a la fecha: por vencer o vencida. Es la misma rutina del cobro, como en la precancelación. |
| HABER interés vencido, mora y seguros capitalizados (solo reestructuración) | Las líneas de la **plantilla** de la operación: 6, 7, 11 o 15, según familia y origen (§4). |
| Cuentas de orden y HABER SOCIOS POR PAGAR por el **neto** (solo novación) | Las líneas de la plantilla 2 o 3. |

Igual que en la entrega, **las bandas nunca salen de las líneas 1–5 de la plantilla**: salen de
`CRD.BNDP`. De la plantilla salen solo las líneas que no son de banda.

## 4. Qué plantilla, según el caso

| Operación | Original | ¿Con atraso? | Alterno |
|---|---|---|---|
| Reestructuración quirografario | quirografario normal | no / sí | 4 / 6 |
| Reestructuración quirografario | quirografario renovado (novado) | no / sí | 5 / 7 |
| Reestructuración prendario | prendario | no / sí | 10 / 11 |
| Reestructuración hipotecario | hipotecario | no / sí | 14 / 15 |
| Novación quirografario | quirografario normal | (solo sin atraso, RN10) | 2 |
| Novación quirografario | quirografario reestructurado | (solo sin atraso) | 3 |
| Novación prendario o hipotecario | — | — | **No hay plantilla** → se rechaza con mensaje hasta que contabilidad la cree |

⛔ **Las posiciones `aux1` de cada línea no-banda de esas plantillas NO están levantadas** (el
levantamiento §7 solo resume). Antes de programar el asiento hace falta **la salida del
`sql/326`**, que lista línea por línea esas 10 plantillas y las bandas de los productos destino.
Hasta entonces el BE construye todo lo demás: la sustitución, el VS, las validaciones, la
cancelación del original y la pantalla. El asiento queda detrás de una guarda que **falla ruidoso**
(«falta el mapeo de la plantilla N»), igual que se hizo con el producto 976.

## 5. DDL — `sql/327`

`ALTER TABLE CRD.PRST ADD (PRSTPROR NUMBER, PRSTVLSS NUMBER(18,2))`, más el índice sobre
`PRSTPROR`. Va antes del WAR.
