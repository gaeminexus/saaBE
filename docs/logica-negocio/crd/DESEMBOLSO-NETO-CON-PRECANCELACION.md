# Desembolso neto con precancelación del préstamo anterior (C16, D-021)

2026-10-28 · rama `cuadre/c16-desembolso-neto` · amplía §1.6 de `CIERRE-CARTERA-A-FECHA-DE-CORTE.md` (H6, D-018).
Decisión D-021 del líder (expediente `cuadreSuper/01-seguimiento/DECISIONES-Y-PARTIDAS.md`): los préstamos de junio 2025
sin asiento de entrega se contabilizan ampliando el motor de desembolsos; manda que **B17 = estructuras G**.
Hallazgo: `cuadreSuper/03-trabajo/cartera/resultados/02-desembolsos-2025-06-analisis.md`.

## 1. Regla

ASOPREP no transfirió el bruto: descontó la **precancelación del préstamo anterior del mismo partícipe** y transfirió el
neto en un lote bancario. La precancelación del cliente es:

> capital pendiente al corte anterior (31-may-2025, = "por vencer" de la G48 de mayo) + **interés completo** + **seguro de
> desgravamen** de la cuota del mes (junio) del préstamo anterior.

Por ítem: **bruto (monto del préstamo) = neto + capitalCancelado + interesCancelado + seguroCancelado + Σ otrosDescuentos**,
al centavo (`PrestamoServiceImpl.diferenciaCuadreDesembolsoNeto`). Si no cuadra, el ítem se rechaza (cada ítem en su
transacción: nada queda a medias).

## 2. Endpoint

`POST /SaaBE/rest/prst/contabilizarDesembolso` (uno) y `POST /SaaBE/rest/prst/contabilizarDesembolsos` (lote). Ítem:

| Campo | |
|---|---|
| `idPrestamo`, `fechaContable`, `idCuentaBancaria`, `referencia` | como en C15 |
| `valor` | **NETO** transferido (lo que dice el banco). Sin descuentos sigue siendo el monto (C15 sin cambios) |
| `idPrestamoCancelado` | opcional: préstamo anterior cancelado con este desembolso |
| `capitalCancelado`, `interesCancelado`, `seguroCancelado` | opcionales: si faltan se calculan; si vienen tienen que coincidir con el cálculo (±0,005) |
| `otrosDescuentos` | opcional: `[ { "concepto", "valor", "cuenta" } ]` (cuenta del plan, texto). En junio 2025 no hay |

Ejemplo (lote real de junio 2025; cuerpo completo en `cuadreSuper/03-trabajo/cartera/resultados/02-lote-desembolsos-2025-06-body.json`):

```json
{ "idEmpresa": 1236, "idUsuario": 38, "usuario": "USUARIO 1",
  "items": [
    { "idPrestamo": 7975, "fechaContable": "2025-06-09", "idCuentaBancaria": 418, "valor": 3731.13,
      "referencia": "PACIFICO AHO 2035544",
      "idPrestamoCancelado": 5429, "capitalCancelado": 21585.65, "interesCancelado": 161.89, "seguroCancelado": 21.33 },
    { "idPrestamo": 7979, "fechaContable": "2025-06-09", "idCuentaBancaria": 418, "valor": 8000.00,
      "referencia": "PACIFICO AHO 2035544" } ] }
```

7975: 25.500,00 = 3.731,13 + 21.585,65 + 161,89 + 21,33. 7979 no tenía anterior: neto = bruto.

**Cálculo de los componentes** (`ProcesoPagoPrestamoService.calcularPrecancelacionConPrestamo`, solo lectura), con la
cuota del préstamo anterior que vence en el mes de `fechaContable` ("cuota del mes"):

- interés = `DTPRINTR`; seguro = `DTPRDSGR + DTPRVLSI`; mora a la fecha (si > 0 se rechaza: la regla no la tiene).
- capital, según la tabla:
  - `TABLA_VIGENTE` (hay cuotas futuras pendientes): capital de la cuota del mes + capital de las futuras.
  - `TABLA_MIGRADA` (futuras saldadas sin pago o inexistentes — los 22 de junio 2025): `max(DTPRCPTL + DTPRSLDO,
    DTPRTTLL − DTPRINTR − DTPRDSGR − DTPRVLSI − DTPRMRAA)` de la cuota del mes, la misma regla con la que se reconstruyó
    la G48 de mayo 2025 (verificado: coincide en los 22).

**Rechazos** (antes de escribir nada): préstamo anterior de otro partícipe (`ENTDCDGO`) o el mismo; en estado 5, 6, 7,
9, 12, 13 o nunca aprobado; con un evento (`CRD.EVPR`) vigente del mes o posterior; con pagos vigentes (`CRD.PGPR`) en la
cuota del mes o posteriores; con una de esas cuotas ya liquidada (4/7) con `DTPRFCPG <= fecha` ("cancelado a esa fecha
por otro medio"); con cuotas impagas de meses anteriores; con más o menos de una cuota en el mes; préstamo nuevo no
aprobado o sustitución (como C15); componentes enviados distintos a los calculados; período contable cerrado.

## 3. Orden de operaciones (todo con `fechaContable`, en la transacción del ítem)

| # | Qué | Motor | Asiento |
|---|---|---|---|
| 1 | Entrega del préstamo nuevo por el **bruto** | `contabilizarEntrega` (sin cambios) | D cartera por banda (+ cuentas de orden) / H **socios por pagar** (plantilla 34 aux 8; 9/13 aux 9) por el bruto |
| 2 | **Precancelación** del anterior pagada con el préstamo nuevo | `ProcesoPagoPrestamoService.precancelarConPrestamoNuevo` | D **socios por pagar** (misma línea de la plantilla de entrega del nuevo) / H `haberDesdePagos`: capital por banda, interés (plantilla de aplicación), seguro de desgravamen; + cierre de apertura del camino directo (D 2.3.02.10 / H 1.4.05.10 por lo que la apertura del mes abrió); + reverso de la provisión de intereses en `CRD.MVIC` (`reversarPorPagos`) |
| 3 | Otros descuentos (si hay) | `contabilizarDescuentosDesembolso` | D socios por pagar / H cuenta de cada descuento |
| 4 | Orden de pago y egreso por el **neto** desde `idCuentaBancaria` | `registrarPagoDeOrigenExterno` (débito automático, como C15) | D socios por pagar / H banco por el neto, con su movimiento bancario y `referencia` |

Socios por pagar del préstamo nuevo queda en cero: H bruto − D precancelación − D otros − D neto = 0.

**Cuenta puente elegida:** no existe en el SAA un "pago de precancelación con otro préstamo" fuera del circuito de
sustitución (novación/reestructuración, que cancela el original en estado 5/13 con otro asiento y no cobra la cuota del
mes). Lo que hace hoy la entrega es acreditar **socios por pagar** por el bruto y la orden de desembolso la debita al pagar;
la novación ya usa esa misma línea por el neto (plantillas 2/3 aux 15). Por eso la precancelación se paga contra
**socios por pagar** del préstamo nuevo: no toca banco, cierra exactamente lo que la entrega dejó pendiente y el banco solo
ve el neto.

**Qué deja la precancelación** (lo mismo que `precancelar`, con tres diferencias conservadoras):

- `CRD.EVPR` tipo `PRECANCELACION` por el total, fecha = `fechaContable`, observación con la marca
  `[D-021 préstamo nuevo <id>]` (idempotencia) y número de asiento.
- Cuota del mes: si la migración la dejó saldada sin pago (estado 4, `DTPRFCPG` nula, sin PGPR), primero vuelve a pendiente
  desde sus pagos (`recalcularCuotaDesdePagos`, la regla D-019 ampliada del reproceso Petro) y su mora se recalcula a la
  fecha; después el motor (`aplicarPagoACuota`) la paga **completa** (diferencia 1: `precancelar` no cobra el interés de
  una cuota que todavía no vence; la regla del cliente sí) → PGPR con capital/interés/desgravamen, cuota PAGADA con
  `DTPRFCPG = fecha`.
- Capital futuro (el que no está en la cuota del mes): PGPR tipo PRECANCELACION en `PGPRSLOT` sobre la cuota del mes
  (ancla) y `DTPRSLOT` de la ancla (si la migración ya dejó ese mismo saldo ahí, no se vuelve a sumar).
- Cuotas futuras: las pendientes pasan a CANCELADA_ANTICIPADA (7); **todas** las futuras quedan con `DTPRFCPG = fecha`
  (diferencia 2: `precancelar` las deja sin fecha). Sin esa fecha la regla "al corte" del cierre y de la G48 (`DTPRESTD IN
  (4,7) AND DTPRFCPG <= corte`) las contaría como impagas y el préstamo seguiría en la cartera de la G48 mientras la
  contabilidad ya lo dio de baja.
- Préstamo anterior → CANCELADO_ANTICIPADO (4) (diferencia 3: admite la cartera migrada que ya está en 4 sin ningún
  registro de su cancelación) y huella en la observación de los dos préstamos.

En los 22 préstamos de junio 2025 hay tres formas de tabla migrada, todas `TABLA_MIGRADA`; el capital futuro se toma
**una sola vez** (si `DTPRSLOT` de la cuota del mes y la suma de las cuotas futuras en 7 son ambos > 0 y difieren, se
rechaza):

- **A** — 15 con la tabla cortada en junio (`DTPRCPTL` = capital de la cuota, `DTPRSLDO = DTPRSLOT` = resto): la cuota
  se paga completa y el resto va como capital futuro en `PGPRSLOT`. Como no hay cuotas en 7, el reparto por banda y el
  cierre de apertura (`ContabilizacionIndividualCreditoServiceImpl`) lo tratan como "capital futuro fuera de la tabla"
  con vencimiento el mes siguiente a la ancla — antes ese caso lanzaba excepción; el caso normal (hay cuotas en 7) no
  cambia.
- **B** — 6 con la cuota de junio "por el total" (`DTPRCPTL` = todo el capital pendiente, `DTPRTTLL` = capital + interés
  + seguro; 4745, 5429, 5774, 6165, 7110, 7352) y las futuras ya en 7 sin fecha: la cuota se paga completa (todo el
  capital en `PGPRCPPG`), no hay capital futuro y las futuras solo reciben la fecha de pago. `DTPRSLOT` de la cuota
  queda como lo dejó la migración (ningún reporte ni la regla al corte lo leen; la G48 usa `PGPRSLOT`).
- **C** — 4116: `DTPRCPTL` = capital propio (130,93), `DTPRSLOT` = Σ futuras en 7 (3.612,24) y `DTPRTTLL` = capital
  propio + futuro + interés + seguro (3.774,94). Capital = 130,93 + 3.612,24 = 3.743,17. Antes de pagar se descuenta el
  futuro de `DTPRTTLL` (si no, el motor aplicaba solo los componentes, 162,70, y dejaba la cuota PARCIAL: fue el rechazo
  `MONTO_NO_COINCIDE ... no quedó PAGADA` del primer lote); la cuota se paga con sus componentes y el futuro va como
  PGPR en `PGPRSLOT` sobre ella, bandado por las cuotas en 7 — el registro normal de `precancelar`.

**Por qué B17 = G:** el asiento acredita el capital al 31-may del préstamo anterior de las cuentas de cartera de su familia
(Q) y el préstamo queda liquidado al corte (todas sus cuotas desde junio en 4/7 con fecha ≤ 30-jun), así que la G48, el
CCPM y el ⑨ (ajuste de bandas a saldo) ya no lo cuentan. La diferencia que la migración dejó entre el módulo y Delta en
estos préstamos (15: el módulo solo veía la cuota; 7: veía el capital dos veces) estaba en la `.25` de la familia desde el
arranque; con la precancelación ambos lados lo bajan a cero y esa diferencia se reduce en lo mismo.

## 4. Idempotencia

Re-enviar el lote no duplica nada: la entrega por `PRSTASEN`; la precancelación por el evento vigente con la marca del
préstamo nuevo (si existe se devuelve y se validan contra él los componentes enviados); otros descuentos por la marca
`[D-021 otros descuentos del desembolso: asiento N]` en la observación del nuevo; la orden por `PRSTIDPG` u orden vigente
del origen `CRD_DESEMBOLSO_PRESTAMO`.

## 5. Respuesta del lote

```json
{ "items": [ { "idPrestamo": 7975, "bruto": 25500.0, "neto": 3731.13, "idPrestamoCancelado": 5429,
               "capitalCancelado": 21585.65, "interesCancelado": 161.89, "seguroCancelado": 21.33, "otrosDescuentos": 0.0,
               "idAsientoEntrega": 0, "idEventoPrecancelacion": 0, "idAsientoPrecancelacion": 0,
               "idsPagoPrestamoPrecancelacion": [0], "idPagoProgramado": 0, "entregaGenerada": true,
               "precancelacionGenerada": true, "pagoGenerado": true, "ok": true, "mensaje": "..." } ],
  "controlPorReferencia": [ { "referencia": "PACIFICO AHO 2035544", "items": 18, "itemsOk": 18,
                              "sumaNetos": 72799.95, "sumaNetosOk": 72799.95 } ],
  "totales": { "bruto": 231500.0, "neto": 108157.0, "capitalCancelado": 122304.69, "interesCancelado": 0, "...": 0,
               "items": 29, "ok": 29, "error": 0 } }
```

Esperado en junio 2025: 29 ítems ok; netos por referencia 2035544 = 72.799,95, 6096206 = 8.258,34, 6096385 = 27.098,71
(total 108.157,00); capital cancelado 122.304,69; interés + seguro 1.038,31; bruto 231.500,00.

## 6. SELECT de control (solo lectura)

```sql
-- 1. Por préstamo del lote: bruto, precancelación (del EVPR/PGPR), neto (orden de pago) y cuadre.
WITH lote AS (SELECT p.PRSTCDGO, p.PRSTMNSL bruto, p.PRSTASEN, p.PRSTIDPG FROM CRD.PRST p
              WHERE p.PRSTCDGO BETWEEN 7969 AND 7999 AND p.PRSTIDAS IS NOT NULL),
ev AS (SELECT l.PRSTCDGO id_nuevo, e.PRSTCDGO id_cancelado, e.EVPRCDGO, e.EVPRNMAS asiento_prec, e.EVPRVLRR total_prec
       FROM lote l JOIN CRD.EVPR e ON e.EVPRTPOO = 'PRECANCELACION' AND e.EVPRFCAN IS NULL
        AND e.EVPROBSR LIKE '%D-021 pr_stamo nuevo ' || l.PRSTCDGO || ']%'),
pg AS (SELECT g.EVPRCDGO, SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRSLOT,0)) capital, SUM(NVL(g.PGPRINPG,0)) interes,
              SUM(NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0)) seguro
       FROM CRD.PGPR g WHERE NVL(g.PGPRANUL,0) = 0 GROUP BY g.EVPRCDGO)
SELECT l.PRSTCDGO id, l.bruto, ev.id_cancelado, NVL(pg.capital,0) capital, NVL(pg.interes,0) interes, NVL(pg.seguro,0) seguro,
       o.PGTRCDGO orden, o.PGTRVLOR neto, o.PGTRRFBN referencia, l.PRSTASEN asiento_entrega, ev.asiento_prec,
       ROUND(l.bruto - NVL(o.PGTRVLOR,0) - NVL(pg.capital,0) - NVL(pg.interes,0) - NVL(pg.seguro,0), 2) dif
FROM lote l LEFT JOIN ev ON ev.id_nuevo = l.PRSTCDGO LEFT JOIN pg ON pg.EVPRCDGO = ev.EVPRCDGO
LEFT JOIN PGS.PGTR o ON o.PGTRCDGO = l.PRSTIDPG
ORDER BY l.PRSTCDGO;

-- 2. Suma de netos por referencia bancaria = débito del extracto (72.799,95 / 8.258,34 / 27.098,71).
SELECT o.PGTRRFBN referencia, COUNT(*) ordenes, SUM(o.PGTRVLOR) suma_netos
FROM CRD.PRST p JOIN PGS.PGTR o ON o.PGTRCDGO = p.PRSTIDPG
WHERE p.PRSTCDGO BETWEEN 7969 AND 7999 AND p.PRSTIDAS IS NOT NULL
GROUP BY o.PGTRRFBN ORDER BY 1;

-- 3. Los préstamos cancelados NO están en la cartera vigente al corte (misma regla que la G48 al corte,
--    CierreCarteraDaoServiceImpl.universo/pendiente/pagos con corte 2025-06-30). Esperado: 0 filas.
WITH P AS (SELECT DATE '2025-06-30' CORTE FROM DUAL),
canc AS (SELECT e.PRSTCDGO FROM CRD.EVPR e WHERE e.EVPRTPOO = 'PRECANCELACION' AND e.EVPRFCAN IS NULL
         AND e.EVPROBSR LIKE '%[D-021 pr_stamo nuevo %')
SELECT d.PRSTCDGO, d.DTPRNMCT, TRUNC(d.DTPRFCVN) vto, d.DTPRESTD, d.DTPRFCPG,
       GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) capital_al_corte
FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO JOIN canc c ON c.PRSTCDGO = d.PRSTCDGO CROSS JOIN P
LEFT JOIN (SELECT DTPRCDGO, SUM(CASE WHEN NVL(PGPRSLOT,0) > 0 THEN PGPRSLOT ELSE NVL(PGPRCPPG,0) END) cap
           FROM CRD.PGPR, P WHERE NVL(PGPRANUL,0) = 0 AND PGPRFCHA < P.CORTE + 1 GROUP BY DTPRCDGO) g
       ON g.DTPRCDGO = d.DTPRCDGO
WHERE p.PRSTFCHA < P.CORTE + 1 AND NVL(p.PRSTIDST,0) NOT IN (0,1,6,7,12)
  AND NOT (NVL(d.DTPRESTD,0) IN (4,7) AND NVL(d.DTPRFCPG, P.CORTE + 1) < P.CORTE + 1)
  AND GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) > 0.004;

-- 4. Y tampoco "vigentes al corte" para el reproceso Petro (D-019, PrestamoDaoServiceImpl
--    .selectByEntidadYProductoVigentesAlCorte): esperado 0 filas.
SELECT e.PRSTCDGO FROM CRD.EVPR e
WHERE e.EVPRTPOO = 'PRECANCELACION' AND e.EVPRFCAN IS NULL AND e.EVPROBSR LIKE '%[D-021 pr_stamo nuevo %'
  AND EXISTS (SELECT 1 FROM CRD.DTPR d WHERE d.PRSTCDGO = e.PRSTCDGO
              AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4,7) OR d.DTPRFCPG IS NULL
                   OR d.DTPRFCPG > TIMESTAMP '2025-06-30 23:59:59'));

-- 5. Totales del lote: capital 122.304,69; interés + seguro 1.038,31; 22 eventos.
SELECT COUNT(DISTINCT e.EVPRCDGO) eventos, SUM(NVL(g.PGPRCPPG,0) + NVL(g.PGPRSLOT,0)) capital,
       SUM(NVL(g.PGPRINPG,0) + NVL(g.PGPRDSGR,0) + NVL(g.PGPRVLSI,0)) interes_seguro
FROM CRD.EVPR e JOIN CRD.PGPR g ON g.EVPRCDGO = e.EVPRCDGO AND NVL(g.PGPRANUL,0) = 0
WHERE e.EVPRTPOO = 'PRECANCELACION' AND e.EVPRFCAN IS NULL AND e.EVPROBSR LIKE '%[D-021 pr_stamo nuevo %';
```

## 7. Pendiente / a vigilar

1. **Reproceso Petro de junio:** si el rol de junio también descontó la cuota de junio de alguno de los 22 préstamos
   cancelados, ese valor ya no tiene cuota donde aplicarse (la cuota quedó pagada con el desembolso): saldrá como valor sin
   destino / excedente (D-010). Correr el lote de desembolsos **antes** del reproceso Petro de junio (paso 1 del orden del
   mes) y revisar esos casos.
2. Las precancelaciones normales del motor (`precancelar`, CBCR) siguen dejando las cuotas en 7 **sin** `DTPRFCPG`: en un
   mes re-procesado "al corte" esas cuotas cuentan como impagas en la G48. No se cambió aquí (afecta a todo el motor);
   revisar antes de re-procesar meses con precancelaciones reales.
3. No hay suite de pruebas que corra sin base; la regla de cuadre es una función pura
   (`PrestamoServiceImpl.diferenciaCuadreDesembolsoNeto`). Probar de punta a punta en `saa-oracle-23ai` con el WAR.
