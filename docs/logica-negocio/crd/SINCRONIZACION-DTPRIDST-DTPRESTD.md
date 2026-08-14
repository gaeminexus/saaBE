# Sincronización de `DTPRIDST` (idEstado) con `DTPRESTD` (estado) en `CRD.DTPR`

> Preparado el **2026-08-13** como parte de la Fase 0 de
> `ESPECIFICACION-SERVICIOS-PAGO-PRESTAMOS.md` (§3.2).
> Este documento es para **revisar antes de ejecutar**: primero se corren los controles del
> bloque 2, se decide, y solo después se ejecutan el respaldo (bloque 3) y el `UPDATE`
> (bloque 4). Nada en los bloques 1 y 2 modifica datos.

---

## 1. Qué se va a hacer

En `CRD.DTPR` (`DetallePrestamo`) hay **dos** columnas de estado:

| Columna | Campo Java | Rol real |
|---|---|---|
| **`DTPRESTD`** | `estado` | **Estado vigente de la cuota** (rubro `EstadoCuotaPrestamo`) |
| `DTPRIDST` | `idEstado` | Copia de `DTPRESTD` que se escribe en `DetallePrestamoServiceImpl.saveSingle`, pero que en datos históricos puede haber quedado desfasada |

El motor de pagos nuevo escribe **siempre los dos campos juntos** (regla permanente: *estados
espejo*). Para que esa regla parta de una base limpia, este script alinea de una sola vez las
filas donde hoy difieren:

```
CRD.DTPR.DTPRIDST  ←  CRD.DTPR.DTPRESTD
```

### Regla exacta

Se actualizan **solo** las filas donde:

- `NVL(DTPRIDST, -1) <> NVL(DTPRESTD, -1)` — es decir, difieren (tratando `NULL` como `-1` para
  que un `NULL` frente a un valor cuente como diferencia), **y**
- `DTPRESTD IS NOT NULL` — no se propaga un `NULL` al `idEstado`; si el estado vigente es `NULL`
  la fila se deja intacta y queda listada en el control 2.4 para revisión aparte.

`DTPRESTD` **nunca** se modifica. La dirección de la copia es fija: el estado vigente es
`DTPRESTD` y `DTPRIDST` es el que se corrige.

### Referencias de modelo

| Concepto | Tabla / columna | Campo Java |
|---|---|---|
| Cuota de préstamo | `CRD.DTPR` | `DetallePrestamo` |
| **Estado vigente de la cuota** | **`DTPR.DTPRESTD`** | **`estado`** |
| Copia del estado | `DTPR.DTPRIDST` | `idEstado` |
| Préstamo | `CRD.PRST` | `Prestamo` |
| Estado vigente del préstamo | `PRST.PRSTIDST` | `idEstado` |

Rubro `com.saa.rubros.EstadoCuotaPrestamo`:

| Valor | Significado |
|---|---|
| 1 | PENDIENTE |
| 2 | ACTIVA |
| 3 | EMITIDA |
| 4 | PAGADA |
| 5 | EN_MORA |
| 6 | PARCIAL |
| 7 | CANCELADA_ANTICIPADA |
| 8 | VENCIDA |

### Por qué importa

`selectCuotasNoPagadasByPrestamo`, `selectMinCuotaNoPagadaByPrestamo`,
`contarCuotasPendientesByPrestamo` y todo el motor de pagos filtran por **`DTPRESTD`**. Un
`DTPRIDST` desfasado no rompe esas consultas hoy, pero sí confunde a quien consulta la tabla
directamente y contradice la regla de espejo que el código nuevo mantiene. Se corrige antes de
que el motor empiece a escribir, para que cualquier diferencia posterior sea una señal real de
bug y no ruido histórico.

---

## 2. Bloque 1 — Controles previos (solo lectura)

Correr los cinco en orden y anotar los resultados.

### 2.1 — Cuántas filas están desalineadas (control principal de §3.2)

```sql
SELECT COUNT(*) AS FILAS_DESALINEADAS
FROM   CRD.DTPR
WHERE  NVL(DTPRIDST, -1) <> NVL(DTPRESTD, -1);
```

Este es el número que debe quedar en **0** después del `UPDATE` (salvo las filas con
`DTPRESTD IS NULL`, ver 2.4).

### 2.2 — Distribución de las combinaciones desalineadas

Muestra qué pares (estado, idEstado) están desalineados y en cuántas filas. Sirve para
detectar un patrón (p.ej. "todas las de estado 4 tienen idEstado 2") antes de tocar nada.

```sql
SELECT d.DTPRESTD          AS ESTADO_VIGENTE,
       d.DTPRIDST          AS ID_ESTADO_ACTUAL,
       COUNT(*)            AS CUOTAS,
       COUNT(DISTINCT d.PRSTCDGO) AS PRESTAMOS
FROM   CRD.DTPR d
WHERE  NVL(d.DTPRIDST, -1) <> NVL(d.DTPRESTD, -1)
GROUP BY d.DTPRESTD, d.DTPRIDST
ORDER BY CUOTAS DESC;
```

### 2.3 — Préstamos afectados (muestra de los 50 con más cuotas desalineadas)

```sql
SELECT p.PRSTCDGO,
       p.PRSTIDAS                AS ID_ASOPREP,
       p.PRSTIDST               AS ESTADO_PRESTAMO,
       e.ENTDNMID               AS IDENTIFICACION,
       e.ENTDRZNS               AS PARTICIPE,
       COUNT(*)                 AS CUOTAS_DESALINEADAS,
       (SELECT COUNT(*) FROM CRD.DTPR d2 WHERE d2.PRSTCDGO = p.PRSTCDGO) AS TOTAL_CUOTAS
FROM   CRD.DTPR d
       JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
       LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE  NVL(d.DTPRIDST, -1) <> NVL(d.DTPRESTD, -1)
GROUP BY p.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, e.ENTDNMID, e.ENTDRZNS
ORDER BY CUOTAS_DESALINEADAS DESC
FETCH FIRST 50 ROWS ONLY;
```

### 2.4 — Filas con `DTPRESTD` NULL (quedan FUERA del `UPDATE`)

Si esta consulta devuelve filas, son cuotas sin estado vigente: una inconsistencia de datos
propia, que este script **no** resuelve. Revisarlas aparte.

```sql
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT   AS NUMERO_CUOTA,
       d.DTPRFCVN   AS FECHA_VENCIMIENTO,
       d.DTPRIDST   AS ID_ESTADO,
       d.DTPRFCPG   AS FECHA_PAGADO
FROM   CRD.DTPR d
WHERE  d.DTPRESTD IS NULL
ORDER BY d.PRSTCDGO, d.DTPRNMCT;
```

### 2.5 — Vista previa del cambio (primeras 100 filas que se van a modificar)

```sql
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT   AS NUMERO_CUOTA,
       d.DTPRIDST   AS ID_ESTADO_ANTES,
       d.DTPRESTD   AS ID_ESTADO_DESPUES,
       d.DTPRFCPG   AS FECHA_PAGADO
FROM   CRD.DTPR d
WHERE  NVL(d.DTPRIDST, -1) <> NVL(d.DTPRESTD, -1)
AND    d.DTPRESTD IS NOT NULL
ORDER BY d.PRSTCDGO, d.DTPRNMCT
FETCH FIRST 100 ROWS ONLY;
```

---

## 3. Bloque 2 — Respaldo (ejecutar antes del `UPDATE`)

Guarda el valor previo de `DTPRIDST` de las filas que se van a tocar, para poder revertir.

```sql
CREATE TABLE CRD.BKP_DTPRIDST_20260813 AS
SELECT d.DTPRCDGO,
       d.PRSTCDGO,
       d.DTPRNMCT,
       d.DTPRIDST AS ID_ESTADO_ANTES,
       d.DTPRESTD AS ESTADO_VIGENTE,
       SYSTIMESTAMP AS FECHA_RESPALDO
FROM   CRD.DTPR d
WHERE  NVL(d.DTPRIDST, -1) <> NVL(d.DTPRESTD, -1)
AND    d.DTPRESTD IS NOT NULL;

-- Debe coincidir con el conteo del control 2.1 menos las filas con DTPRESTD NULL (control 2.4)
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_DTPRIDST_20260813;
```

---

## 4. Bloque 3 — `UPDATE` (el cambio)

```sql
UPDATE CRD.DTPR
SET    DTPRIDST = DTPRESTD
WHERE  NVL(DTPRIDST, -1) <> NVL(DTPRESTD, -1)
AND    DTPRESTD IS NOT NULL;

-- Verificar el número de filas afectadas contra el respaldo ANTES de confirmar
COMMIT;
```

---

## 5. Bloque 4 — Controles posteriores

### 5.1 — Ya no quedan filas desalineadas con estado no nulo (debe devolver 0)

```sql
SELECT COUNT(*) AS FILAS_DESALINEADAS_RESTANTES
FROM   CRD.DTPR
WHERE  NVL(DTPRIDST, -1) <> NVL(DTPRESTD, -1)
AND    DTPRESTD IS NOT NULL;
```

### 5.2 — El total desalineado restante coincide con las filas de `DTPRESTD` NULL

```sql
SELECT (SELECT COUNT(*) FROM CRD.DTPR WHERE NVL(DTPRIDST,-1) <> NVL(DTPRESTD,-1)) AS DESALINEADAS,
       (SELECT COUNT(*) FROM CRD.DTPR WHERE DTPRESTD IS NULL AND DTPRIDST IS NOT NULL) AS POR_ESTADO_NULO
FROM   DUAL;
```

Las dos cifras deben ser iguales.

### 5.3 — `DTPRESTD` no cambió (contraste contra el respaldo; debe devolver 0 filas)

```sql
SELECT b.DTPRCDGO, b.ESTADO_VIGENTE AS ANTES, d.DTPRESTD AS AHORA
FROM   CRD.BKP_DTPRIDST_20260813 b
       JOIN CRD.DTPR d ON d.DTPRCDGO = b.DTPRCDGO
WHERE  NVL(d.DTPRESTD, -1) <> NVL(b.ESTADO_VIGENTE, -1);
```

### 5.4 — Distribución final de estados (foto para el registro)

```sql
SELECT DTPRESTD AS ESTADO, DTPRIDST AS ID_ESTADO, COUNT(*) AS CUOTAS
FROM   CRD.DTPR
GROUP BY DTPRESTD, DTPRIDST
ORDER BY 1, 2;
```

---

## 6. Reversa (solo si hace falta deshacer)

```sql
UPDATE CRD.DTPR d
SET    d.DTPRIDST = (SELECT b.ID_ESTADO_ANTES
                     FROM   CRD.BKP_DTPRIDST_20260813 b
                     WHERE  b.DTPRCDGO = d.DTPRCDGO)
WHERE  EXISTS (SELECT 1 FROM CRD.BKP_DTPRIDST_20260813 b WHERE b.DTPRCDGO = d.DTPRCDGO);

COMMIT;
```

Una vez validado el resultado en producción, la tabla de respaldo puede eliminarse:

```sql
-- DROP TABLE CRD.BKP_DTPRIDST_20260813;
```

---

## 7. Después de este script

`DetallePrestamoServiceImpl.saveSingle` ya mantiene el espejo (`detalle.setIdEstado(detalle.getEstado())`)
y los generadores de tabla de amortización de `PrestamoServiceImpl` escriben ambos campos. Con
la sincronización aplicada, cualquier fila desalineada que aparezca en adelante indica una ruta
de escritura que se saltó el espejo — vale la pena investigarla, no volver a correr este `UPDATE`.
