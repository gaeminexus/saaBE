# Actualización de `DTPRSLOT` (saldoOtros) en la última cuota pagada de préstamos CANCELADOS ANTICIPADOS

> Preparado el **2026-08-12**. Este documento es para **revisar antes de ejecutar**:
> primero se corren los controles del bloque 1 y la vista previa del bloque 2, se decide,
> y solo después se ejecutan el respaldo (bloque 3) y el `UPDATE` (bloque 4).
> Nada en los bloques 1 y 2 modifica datos.

## 1. Qué se va a hacer

En los préstamos que **hoy** están en estado **CANCELADO ANTICIPADO**
(`CRD.PRST.PRSTIDST = 4`, `com.saa.rubros.EstadoPrestamo.CANCELADO_ANTICIPADO`),
se ubica la **última cuota pagada** y se le copia el saldo de capital al campo de pago extra:

```
CRD.DTPR.DTPRSLOT  (saldoOtros)  ←  CRD.DTPR.DTPRSLCP  (saldoCapital)
```

### Regla de no sobrescritura (decisión del 2026-08-12)

**Solo se actualizan las cuotas cuyo `DTPRSLOT` está en 0 o nulo.** Si la cuota ya tiene
un valor en `saldoOtros`, se deja intacta: ese valor viene de un pago extra real
registrado en su momento y no se pisa. La condición `NVL(d.DTPRSLOT, 0) = 0` está
presente en los dos `UPDATE` del bloque 4.

### Definición exacta de "última cuota pagada"

La cuota del préstamo con el **mayor `DTPRNMCT` (numeroCuota)** entre las que tienen
**`DTPRESTD = 4`** (`EstadoCuotaPrestamo.PAGADA`).

- Se filtra **solo por estado 4**. Las cuotas en estado **7 (CANCELADA_ANTICIPADA)** —
  que son las posteriores a la cancelación — **no** cuentan como pagadas y **no** se tocan.
- No se usa `fechaPagado` para el orden: el criterio pedido es el número de cuota.
- Si un préstamo cancelado anticipadamente no tiene ninguna cuota en estado 4,
  **no se actualiza nada** (control 1.2 los lista).

### Referencias de modelo

| Concepto | Tabla / columna | Campo Java |
|---|---|---|
| Préstamo | `CRD.PRST` | `Prestamo` |
| **Estado del préstamo** | **`PRST.PRSTIDST`** | **`idEstado`** |
| Cuota | `CRD.DTPR` | `DetallePrestamo` |
| Número de cuota | `DTPR.DTPRNMCT` | `numeroCuota` |
| **Estado de la cuota** | **`DTPR.DTPRESTD`** | **`estado`** |
| Saldo de capital | `DTPR.DTPRSLCP` | `saldoCapital` |
| Saldo otros (destino) | `DTPR.DTPRSLOT` | `saldoOtros` |
| Saldo inicial de capital | `DTPR.DTPRSICP` | `saldoInicialCapital` |

Estados relevantes (`com.saa.rubros`):

| Rubro | Valor | Significado |
|---|---|---|
| `EstadoPrestamo.CANCELADO_ANTICIPADO` | 4 | Préstamo cancelado antes del plazo |
| `EstadoCuotaPrestamo.PAGADA` | 4 | Cuota pagada |
| `EstadoCuotaPrestamo.CANCELADA_ANTICIPADA` | 7 | Cuota anulada por la cancelación anticipada |

### Campos que NO se deben usar en este script

| Columna | Por qué no |
|---|---|
| `PRST.ESPSCDGO` (`estadoPrestamo`) | Es la FK al catálogo `CRD.ESPS`, no el campo que lleva el estado operativo del préstamo. |
| `DTPR.DTPRIDST` (`idEstado`) | En la cuota el estado vigente está en `DTPRESTD`; `DTPRIDST` se escribe como copia (`DetallePrestamoServiceImpl` línea 83) y puede quedar desfasado. |

El estado del préstamo lo escribe `ProcesoCargaPetroServiceImpl.setIdEstado(...)` con los
valores del rubro `EstadoPrestamo`, es decir **`PRSTIDST` sí contiene el 4**.

---

## 2. Bloque 1 — Controles previos (solo lectura)

Correr los ocho en orden y anotar los resultados. Cada uno responde una pregunta
concreta sobre qué va a pasar.

### 1.0 — Contraste `PRSTIDST` vs `ESPSCDGO` (control de campo correcto)

Confirma que se está filtrando por la columna correcta y muestra si ambas están o no
alineadas en los datos actuales. Lo que importa es la fila `PRSTIDST = 4`.

```sql
SELECT p.PRSTIDST                       AS ID_ESTADO,
       p.ESPSCDGO                       AS FK_CATALOGO_ESPS,
       esp.ESPSNMBR                     AS NOMBRE_EN_CATALOGO,
       esp.ESPSCDEX                     AS COD_ALTERNO,
       COUNT(*)                         AS PRESTAMOS
FROM   CRD.PRST p
       LEFT JOIN CRD.ESPS esp ON esp.ESPSCDGO = p.ESPSCDGO
GROUP BY p.PRSTIDST, p.ESPSCDGO, esp.ESPSNMBR, esp.ESPSCDEX
ORDER BY 1, 2;
```

Sirve además para descartar el escenario que ya se dio en `CRD.ENTD`
(ver `MIGRACION-ESTADO-PARTICIPE.md`): que la FK apunte al **PK** del catálogo mientras el
rubro usa el **código alterno**. Aquí eso no afecta al `UPDATE` — se filtra por `PRSTIDST`,
que guarda directamente el valor del rubro — pero conviene ver la foto.

### 1.1 — Universo: cuántos préstamos cancelados anticipados hay

```sql
SELECT COUNT(*) AS PRESTAMOS_CANCELADOS_ANTICIPADOS
FROM   CRD.PRST p
WHERE  p.PRSTIDST = 4;
```

### 1.2 — Préstamos cancelados anticipados SIN ninguna cuota pagada (NO se actualizan)

Si esta consulta devuelve filas, esos préstamos quedan fuera del `UPDATE`.
Vale la pena mirarlos: un préstamo cancelado anticipadamente sin ninguna cuota en
estado 4 es una inconsistencia de datos.

```sql
SELECT p.PRSTCDGO,
       p.PRSTIDAS      AS ID_ASOPREP,
       e.ENTDNMID      AS IDENTIFICACION,
       e.ENTDRZNS      AS PARTICIPE,
       pr.PRDCNMBR     AS PRODUCTO,
       p.PRSTMNSL      AS MONTO_SOLICITADO,
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS TOTAL_CUOTAS,
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD = 7) AS CUOTAS_EST_7
FROM   CRD.PRST p
       LEFT JOIN CRD.ENTD e  ON e.ENTDCDGO = p.ENTDCDGO
       LEFT JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
WHERE  p.PRSTIDST = 4
AND    NOT EXISTS (SELECT 1 FROM CRD.DTPR d
                   WHERE d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD = 4)
ORDER BY p.PRSTCDGO;
```

### 1.3 — Filas objetivo: cuántas se van a actualizar y por cuánto

Separa las últimas cuotas pagadas en las dos poblaciones: las que **sí** se actualizan
(`saldoOtros` en 0 o nulo) y las que **quedan intactas** por la regla de no sobrescritura.

```sql
SELECT CASE WHEN NVL(d.DTPRSLOT, 0) = 0
            THEN 'SE ACTUALIZA (saldoOtros 0 o nulo)'
            ELSE 'SE EXCLUYE (ya tiene saldoOtros)' END AS POBLACION,
       COUNT(*)                       AS FILAS,
       COUNT(DISTINCT d.PRSTCDGO)     AS PRESTAMOS,
       SUM(NVL(d.DTPRSLCP, 0))        AS SUMA_SALDO_CAPITAL,
       SUM(NVL(d.DTPRSLOT, 0))        AS SUMA_SALDO_OTROS_ACTUAL,
       MIN(d.DTPRSLCP)                AS MIN_SALDO_CAPITAL,
       MAX(d.DTPRSLCP)                AS MAX_SALDO_CAPITAL
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
GROUP BY CASE WHEN NVL(d.DTPRSLOT, 0) = 0
              THEN 'SE ACTUALIZA (saldoOtros 0 o nulo)'
              ELSE 'SE EXCLUYE (ya tiene saldoOtros)' END;
```

Lecturas de control:

- La suma de `FILAS` de las dos poblaciones debe ser igual a
  (control 1.1 − filas del control 1.2).
- En cada población `FILAS` debe ser igual a `PRESTAMOS`; si `FILAS > PRESTAMOS`
  hay empates de número de cuota → ver control 1.4.
- `FILAS` de la fila **SE ACTUALIZA** es el número que debe reportar el `UPDATE` del bloque 4.
- `SUMA_SALDO_OTROS_ACTUAL` de la fila **SE ACTUALIZA** debe dar 0 (por definición).

### 1.4 — Empates: préstamos con más de una cuota pagada en el mismo número de cuota

Si aparece algo aquí, el `UPDATE` tocaría **más de una fila por préstamo**.

```sql
SELECT d.PRSTCDGO,
       d.DTPRNMCT   AS NUMERO_CUOTA,
       COUNT(*)     AS FILAS_DUPLICADAS
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
GROUP BY d.PRSTCDGO, d.DTPRNMCT
HAVING COUNT(*) > 1
ORDER BY 3 DESC, 1;
```

### 1.5 — Filas a actualizar con `DTPRSLCP` nulo o en cero

Son las filas donde copiar el saldo de capital **no aporta nada** (o borraría el valor actual).
El `UPDATE` del bloque 4 ya excluye los nulos; los ceros se decide si se incluyen (ver 4.2).

```sql
SELECT CASE WHEN d.DTPRSLCP IS NULL THEN 'NULO'
            WHEN d.DTPRSLCP = 0     THEN 'CERO'
            ELSE 'CON VALOR' END        AS SITUACION_SALDO_CAPITAL,
       COUNT(*)                         AS FILAS
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
GROUP BY CASE WHEN d.DTPRSLCP IS NULL THEN 'NULO'
              WHEN d.DTPRSLCP = 0     THEN 'CERO'
              ELSE 'CON VALOR' END;
```

### 1.6 — Filas que YA tienen `DTPRSLOT` con valor (NO se tocan)

Listado de las cuotas que quedan **fuera** del `UPDATE` por la regla de no sobrescritura.
Sirve como respaldo documental de qué se dejó como estaba y cuál habría sido el valor
alterno; la columna `DIFERENCIA` muestra cuánto se aparta el valor existente del saldo
de capital, por si alguna fila amerita revisión manual aparte.

```sql
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT      AS NUMERO_CUOTA,
       d.DTPRSLOT      AS SALDO_OTROS_ACTUAL_SE_CONSERVA,
       d.DTPRSLCP      AS SALDO_CAPITAL_NO_APLICADO,
       d.DTPRSLCP - d.DTPRSLOT AS DIFERENCIA
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
AND    NVL(d.DTPRSLOT, 0) <> 0
ORDER BY ABS(d.DTPRSLCP - d.DTPRSLOT) DESC;
```

### 1.7 — Coherencia `DTPRSICP = DTPRCPTL + DTPRSLCP`

Según `Carga-Tabla-Amortizacion-Excel.md`, en la tabla de amortización se cumple
`saldoInicialCapital = capital + saldoCapital (+ saldoOtros)`. Esta consulta lista las
filas objetivo donde esa relación **no** se cumple (tolerancia 1 centavo): son cuotas
cuyos saldos ya venían inconsistentes y conviene mirarlas una por una antes de propagar
`DTPRSLCP` a otro campo.

```sql
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT   AS NUMERO_CUOTA,
       d.DTPRSICP   AS SALDO_INICIAL_CAPITAL,
       d.DTPRCPTL   AS CAPITAL,
       d.DTPRSLCP   AS SALDO_CAPITAL,
       NVL(d.DTPRSICP,0) - NVL(d.DTPRCPTL,0) - NVL(d.DTPRSLCP,0) AS DESCUADRE
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
AND    ABS(NVL(d.DTPRSICP,0) - NVL(d.DTPRCPTL,0) - NVL(d.DTPRSLCP,0)) > 0.01
ORDER BY ABS(NVL(d.DTPRSICP,0) - NVL(d.DTPRCPTL,0) - NVL(d.DTPRSLCP,0)) DESC;
```

### 1.8 — Coherencia de estados: cuotas posteriores que no están en 7

En un préstamo cancelado anticipadamente, todas las cuotas con número **mayor** al de la
última pagada deberían estar en estado 7 (CANCELADA_ANTICIPADA). Las que salgan aquí
indican préstamos cuyo cierre quedó a medias.

```sql
SELECT d.PRSTCDGO,
       d.DTPRNMCT   AS NUMERO_CUOTA,
       d.DTPRESTD   AS ESTADO_CUOTA,
       d.DTPRSLCP   AS SALDO_CAPITAL,
       d.DTPRFCVN   AS FECHA_VENCIMIENTO
FROM   CRD.DTPR d
WHERE  EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRESTD NOT IN (4, 7)
AND    d.DTPRNMCT > (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
ORDER BY d.PRSTCDGO, d.DTPRNMCT;
```

---

## 3. Bloque 2 — Vista previa fila a fila

### 2.1 — Detalle completo de lo que se va a actualizar

Es el "antes y después" de cada fila. Incluye **todas** las últimas cuotas pagadas, marcadas
con la columna `ACCION` según se actualicen o se conserven. Exportar a Excel si se quiere
revisar con el área usuaria.

```sql
SELECT CASE WHEN NVL(d.DTPRSLOT, 0) = 0 THEN 'ACTUALIZA'
            ELSE 'CONSERVA' END          AS ACCION,
       p.PRSTCDGO                       AS COD_PRESTAMO,
       p.PRSTIDAS                       AS ID_ASOPREP,
       e.ENTDNMID                       AS IDENTIFICACION,
       e.ENTDRZNS                       AS PARTICIPE,
       pr.PRDCNMBR                      AS PRODUCTO,
       f.FLLLNMBR                       AS FILIAL,
       p.PRSTMNSL                       AS MONTO_SOLICITADO,
       p.PRSTPLZO                       AS PLAZO,
       d.DTPRCDGO                       AS COD_CUOTA,
       d.DTPRNMCT                       AS NUMERO_CUOTA,
       d.DTPRFCVN                       AS FECHA_VENCIMIENTO,
       d.DTPRFCPG                       AS FECHA_PAGADO,
       d.DTPRCPTL                       AS CAPITAL,
       d.DTPRSICP                       AS SALDO_INICIAL_CAPITAL,
       d.DTPRSLCP                       AS SALDO_CAPITAL,
       d.DTPRSLOT                       AS SALDO_OTROS_ANTES,
       CASE WHEN NVL(d.DTPRSLOT, 0) = 0 THEN d.DTPRSLCP
            ELSE d.DTPRSLOT END          AS SALDO_OTROS_DESPUES,
       (SELECT COUNT(*) FROM CRD.DTPR x WHERE x.PRSTCDGO = p.PRSTCDGO)                     AS TOTAL_CUOTAS,
       (SELECT COUNT(*) FROM CRD.DTPR x WHERE x.PRSTCDGO = p.PRSTCDGO AND x.DTPRESTD = 4)  AS CUOTAS_PAGADAS,
       (SELECT COUNT(*) FROM CRD.DTPR x WHERE x.PRSTCDGO = p.PRSTCDGO AND x.DTPRESTD = 7)  AS CUOTAS_CANC_ANTICIP
FROM   CRD.DTPR d
       JOIN CRD.PRST p       ON p.PRSTCDGO = d.PRSTCDGO
       LEFT JOIN CRD.ENTD e  ON e.ENTDCDGO = p.ENTDCDGO
       LEFT JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
       LEFT JOIN CRD.FLLL f  ON f.FLLLCDGO = p.FLLLCDGO
WHERE  p.PRSTIDST = 4
AND    d.DTPRESTD = 4
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
ORDER BY ACCION, d.DTPRSLCP DESC, p.PRSTCDGO;
```

### 2.2 — Resumen por año de pago y por producto

Da la magnitud del cambio y ayuda a detectar si se está tocando data de períodos ya reportados.
Cuenta **solo las filas que se actualizan**.

```sql
SELECT EXTRACT(YEAR FROM d.DTPRFCPG)  AS ANIO_PAGO,
       pr.PRDCNMBR                    AS PRODUCTO,
       COUNT(*)                       AS CUOTAS,
       SUM(NVL(d.DTPRSLCP, 0))        AS SUMA_A_COPIAR
FROM   CRD.DTPR d
       JOIN CRD.PRST p       ON p.PRSTCDGO = d.PRSTCDGO
       LEFT JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
WHERE  p.PRSTIDST = 4
AND    d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4)
GROUP BY EXTRACT(YEAR FROM d.DTPRFCPG), pr.PRDCNMBR
ORDER BY 1, 2;
```

---

## 4. Bloque 3 — Respaldo (ejecutar antes del UPDATE)

Guarda el valor previo de **todas** las últimas cuotas pagadas, incluidas las que el `UPDATE`
no va a tocar (las que ya tienen `saldoOtros`). La columna `ES_OBJETIVO` distingue unas de
otras y es la que usan las verificaciones del bloque 5. Ajustar la fecha del nombre si se
ejecuta otro día.

```sql
CREATE TABLE CRD.BKP_DTPR_SLOT_20260812 AS
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT,
       d.DTPRSLOT   AS DTPRSLOT_ORIGINAL,
       d.DTPRSLCP   AS DTPRSLCP_ORIGINAL,
       CASE WHEN NVL(d.DTPRSLOT, 0) = 0 THEN 'S' ELSE 'N' END AS ES_OBJETIVO,
       SYSDATE      AS FECHA_RESPALDO
FROM   CRD.DTPR d
WHERE  d.DTPRESTD = 4
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4);

SELECT ES_OBJETIVO, COUNT(*) AS FILAS
FROM   CRD.BKP_DTPR_SLOT_20260812
GROUP BY ES_OBJETIVO;
```

Estos dos conteos deben coincidir exactamente con las dos poblaciones del control 1.3
(`ES_OBJETIVO = 'S'` ↔ SE ACTUALIZA, `'N'` ↔ SE EXCLUYE).

---

## 5. Bloque 4 — UPDATE

Las dos versiones llevan `NVL(d.DTPRSLOT, 0) = 0`: **nunca se pisa un `saldoOtros` existente**.

### 4.1 — Versión estándar (recomendada)

Actualiza las últimas cuotas pagadas con `saldoOtros` en 0 o nulo. Excluye además las filas
con `DTPRSLCP` nulo, para no escribir un `NULL`.

```sql
UPDATE CRD.DTPR d
SET    d.DTPRSLOT = d.DTPRSLCP
WHERE  d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0              -- <<< no sobrescribe valores existentes
AND    d.DTPRSLCP IS NOT NULL
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4);

-- Las filas afectadas deben coincidir con la población SE ACTUALIZA del control 1.3
-- (menos las que el control 1.5 reportó como DTPRSLCP nulo). Revisar ANTES de confirmar.
-- COMMIT;   ← solo después de correr las verificaciones del bloque 5
```

### 4.2 — Variante: solo cuando hay saldo de capital pendiente

Si en el control 1.5 aparecen filas en `CERO` y se decide **no** tocarlas
(porque un saldo de capital 0 significa que el préstamo terminó de pagarse normalmente
y no hubo cancelación anticipada real), usar esta versión agregando la última condición:

```sql
UPDATE CRD.DTPR d
SET    d.DTPRSLOT = d.DTPRSLCP
WHERE  d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) = 0
AND    d.DTPRSLCP IS NOT NULL
AND    d.DTPRSLCP > 0                      -- <<< única diferencia con 4.1
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4)
AND    d.DTPRNMCT = (SELECT MAX(d2.DTPRNMCT) FROM CRD.DTPR d2
                     WHERE d2.PRSTCDGO = d.PRSTCDGO AND d2.DTPRESTD = 4);
```

Con esta variante el `UPDATE` es un no-op sobre las filas de saldo de capital 0: quedan
igual que con 4.1 (`saldoOtros` seguiría en 0), así que la diferencia práctica es solo si
`DTPRSLOT` era `NULL` — con 4.1 pasa a 0, con 4.2 se queda en `NULL`.

**Ejecutar 4.1 o 4.2, no las dos.**

---

## 6. Bloque 5 — Verificación posterior (antes del COMMIT)

Las tres se apoyan en la tabla de respaldo, que es lo único que conserva el estado previo
(después del `UPDATE` ya no se puede reconocer una fila objetivo por su `DTPRSLOT`).

### 5.1 — Toda fila objetivo debe haber quedado con `DTPRSLOT = DTPRSLCP`

Debe devolver **0 filas**. Con la versión 4.2, devolverá las de saldo de capital en cero
cuyo `DTPRSLOT` original era `NULL`.

```sql
SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRSLOT, d.DTPRSLCP
FROM   CRD.BKP_DTPR_SLOT_20260812 b
       JOIN CRD.DTPR d ON d.DTPRCDGO = b.DTPRCDGO
WHERE  b.ES_OBJETIVO = 'S'
AND    b.DTPRSLCP_ORIGINAL IS NOT NULL
AND    NVL(d.DTPRSLOT, -1) <> d.DTPRSLCP;
```

### 5.2 — Ninguna fila excluida debe haber cambiado

**Control clave de la regla de no sobrescritura.** Debe devolver **0 filas**.

```sql
SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT,
       b.DTPRSLOT_ORIGINAL AS VALOR_ANTES,
       d.DTPRSLOT          AS VALOR_AHORA
FROM   CRD.BKP_DTPR_SLOT_20260812 b
       JOIN CRD.DTPR d ON d.DTPRCDGO = b.DTPRCDGO
WHERE  b.ES_OBJETIVO = 'N'
AND    NVL(d.DTPRSLOT, -1) <> NVL(b.DTPRSLOT_ORIGINAL, -1);
```

### 5.3 — Comparación global contra el respaldo

```sql
SELECT b.ES_OBJETIVO,
       COUNT(*)                                             AS FILAS_MODIFICADAS,
       SUM(NVL(d.DTPRSLOT,0) - NVL(b.DTPRSLOT_ORIGINAL,0))  AS VARIACION_TOTAL
FROM   CRD.BKP_DTPR_SLOT_20260812 b
       JOIN CRD.DTPR d ON d.DTPRCDGO = b.DTPRCDGO
WHERE  NVL(d.DTPRSLOT, -1) <> NVL(b.DTPRSLOT_ORIGINAL, -1)
GROUP BY b.ES_OBJETIVO;
```

Solo debe aparecer la fila `ES_OBJETIVO = 'S'`, y su `VARIACION_TOTAL` debe ser igual a
`SUMA_SALDO_CAPITAL` de la población SE ACTUALIZA del control 1.3.

### 5.4 — Ninguna fila fuera del alcance debe haber cambiado

Verifica que el `UPDATE` no salió del conjunto previsto (cuotas que no son la última pagada,
o de préstamos en otro estado).

```sql
SELECT COUNT(*) AS FILAS_FUERA_DE_ALCANCE
FROM   CRD.DTPR d
WHERE  d.DTPRCDGO NOT IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_SLOT_20260812)
AND    d.DTPRESTD = 4
AND    NVL(d.DTPRSLOT, 0) <> 0
AND    EXISTS (SELECT 1 FROM CRD.PRST p
               WHERE p.PRSTCDGO = d.PRSTCDGO AND p.PRSTIDST = 4);
```

> Nota: este control solo tiene sentido corriéndolo **también antes** del `UPDATE`
> (junto con el bloque 1) y comparando ambos valores; si ya había cuotas fuera del alcance
> con `DTPRSLOT <> 0`, el número no será 0 pero debe ser **idéntico** al de antes.

Si todo cuadra: `COMMIT;`

---

## 7. Bloque 6 — ROLLBACK

Si ya se hizo `COMMIT` y hay que revertir. Se limita a las filas objetivo: las excluidas
(`ES_OBJETIVO = 'N'`) nunca se tocaron y no hay que restaurarlas.

```sql
UPDATE CRD.DTPR d
SET    d.DTPRSLOT = (SELECT b.DTPRSLOT_ORIGINAL
                     FROM   CRD.BKP_DTPR_SLOT_20260812 b
                     WHERE  b.DTPRCDGO = d.DTPRCDGO)
WHERE  d.DTPRCDGO IN (SELECT DTPRCDGO FROM CRD.BKP_DTPR_SLOT_20260812
                      WHERE ES_OBJETIVO = 'S');

-- COMMIT;
```

La tabla de respaldo se conserva; borrarla solo cuando el cambio esté validado en producción.

---

## 8. Impacto conocido de `DTPRSLOT` en el sistema

Dónde se lee hoy este campo, para dimensionar el efecto del cambio:

- **`GeneracionCCPMServiceImpl`** (reporte CCPM) suma `saldoOtros` al desglose de capital
  por vencer, vía `DetallePrestamoDaoServiceImpl.selectCapitalCuotasFuturasBatch` y
  `selectCapitalCuotasDesdeInicioMesBatch`. Ambas consultas filtran por
  `fechaVencimiento >= inicio del mes de ejecución` y excluyen estado 7, así que las cuotas
  ya pagadas de meses anteriores **no** entran: para préstamos cancelados hace tiempo el
  CCPM no debería moverse. Verificar en el préstamo cuya última cuota pagada venza dentro
  del mes que se esté generando.
- **`PrestamoServiceImpl` línea 783** copia `detalle.getSaldoOtros()` a `PagoPrestamo.saldoOtros`
  al registrar un pago. Solo aplica a pagos nuevos, no a los históricos ya registrados.
- **Carga de tabla de amortización desde Excel** (`PrestamoServiceImpl`, columna C "PAGO EXTRA"):
  si se vuelve a cargar el Excel de uno de estos préstamos, `DTPRSLOT` se sobrescribe con
  lo que traiga el archivo. No debería pasar en préstamos cancelados, pero conviene saberlo.

### Advertencia sobre la invariante de la tabla de amortización

`Carga-Tabla-Amortizacion-Excel.md` documenta que
`saldoInicialCapital = capital + saldoCapital + saldoOtros`. Al dejar
`saldoOtros = saldoCapital`, en las filas actualizadas pasa a cumplirse
`saldoInicialCapital = capital + 2 × saldoOtros`, es decir **la invariante deja de valer**
en esas cuotas. Es esperable dado que el campo se está usando para registrar el capital
cancelado anticipadamente, pero cualquier proceso futuro que recalcule saldos sobre esa
fórmula debe excluir las cuotas de préstamos en estado 4.

Las filas que quedaron fuera por la regla de no sobrescritura (las que ya tenían
`saldoOtros`) **sí** conservan la invariante original; la tabla de respaldo, con
`ES_OBJETIVO = 'N'`, es la lista exacta de cuáles son.
