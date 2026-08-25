# Carga inicial del modelo de bandas por producto (CRD.CBPR / CRD.BNDP)

**Fecha:** 2026-08-25 · **Requiere:** haber ejecutado antes `sql/DDL-BANDAS-PRODUCTO.sql`
**Estado de ejecución:** ✅ BD LOCAL de desarrollo (docker `saa-oracle-23ai`) — 2026-08-25, 28 CBPR + 143 BNDP, controles 3.1/3.2 en cero · ⬜ PRUEBAS · ⬜ PRODUCCIÓN
**Ejecución:** MANUAL, con revisión de los SELECT de control en cada paso. Mismo guion para BASE DE PRUEBAS y PRODUCCIÓN — solo cambian las variables del paso 0.
**Versión ejecutable:** `sql/CARGA-INICIAL-BANDAS-PRODUCTO.sql` — el mismo contenido de este runbook (pasos 1–3) en **SQL puro** (sin comandos SQL*Plus: sirve en el plugin de VS Code / DBeaver y también en SQL*Plus), con los valores ya incrustados para producción (empresa 1236, ids idénticos a la copia local donde se validó). Correr por bloques y ejecutar el COMMIT final solo con los controles en cero.
**Diseño:** `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §8.

> ⚠ Recordatorio SQL*Plus: no dejar líneas de comentario terminadas en `-` (se tragan la
> sentencia siguiente). Los separadores de este guion terminan en `=`.

---

## 0. Variables de la corrida

```sql
SET DEFINE ON
-- Nodo de empresa (SCP.PJRQ, nivel pgspcdgo 12). En la BD local/pruebas ASOPREP = 1236.
-- En PRODUCCIÓN verificar con el control 1.1 antes de fijarlo.
DEFINE EMPRESA = 1236
-- Inicio de vigencia de la configuración inicial (primer día del mes en que entra a regir)
DEFINE FECHA_DESDE = '2026-09-01'
-- Usuario de auditoría
DEFINE USUARIO = 'CARGA-INICIAL-BANDAS'
```

---

## 1. Controles PREVIOS (no continuar si algo no cuadra)

### 1.1 Resolver el nodo de empresa (fija `EMPRESA` con este resultado)

```sql
SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ WHERE PGSPCDGO = 12 ORDER BY PJRQCDGO;
```
Esperado (local/pruebas): `75 RAIZ EMPRESA_SIS`, `280 ASOPREP ANTERIOR`, `1236 ASOPREP` → usar **1236** (el nodo vigente, no el "ANTERIOR"). En producción, elegir el nodo ASOPREP equivalente.

### 1.2 Tablas destino vacías

```sql
SELECT (SELECT COUNT(*) FROM CRD.CBPR) CBPR, (SELECT COUNT(*) FROM CRD.BNDP) BNDP FROM dual;
```
Esperado: `0, 0`. Si no, esta carga ya corrió: NO repetir sin limpiar/revisar.

### 1.3 Productos de crédito

```sql
SELECT p.PRDCCDGO, p.PRDCNMBR, t.TPPRNMBR TIPO, p.PRDCESTD
FROM CRD.PRDC p LEFT JOIN CRD.TPPR t ON p.TPPRCDGO = t.TPPRCDGO
ORDER BY t.TPPRCDGO, p.PRDCCDGO;
```
Esperado (local/pruebas, 15 filas): quirografarios 2, 3, 4, 5, 6, 14, 15, 16, 17 · hipotecarios 7, 8, 21 · prendarios 9, 10, 22. **Si en producción hay productos distintos, ajustar el mapeo del paso 2.1 antes de cargar.** Se cargan también los inactivos (tienen cartera histórica que la reclasificación puede tocar).

### 1.4 Cuentas de bandas existentes para la empresa

```sql
SELECT PLNNCNTA, PLNNNMBR FROM CNT.PLNN
WHERE PJRQCDGO = &&EMPRESA AND PLNNESTD = 1
  AND REGEXP_LIKE(PLNNCNTA, '^1\.3\.(01|02|03|04|05|06|07|08|09|10|11|12)\.')
ORDER BY PLNNCNTA;
```
Esperado: 41 subcuentas — 5 por familia en 1.3.01/.02/.03/.04/.05/.07/.08/.09/.11 y 6 en 1.3.12.
**Conocido:** `1.3.06` (prendarios renovados) y `1.3.10` (hipotecarios renovados) existen solo como cabecera, SIN subcuentas de bandas → los productos 22 (PRENDARIO NOVACION) y 21 (HIPOTECARIO NOVACION) quedan sin configuración de POR VENCER en esta carga (ver paso 4).

---

## 2. Carga

### 2.1 Cabeceras (CRD.CBPR)

El mapeo producto → familia contable (verificado contra `CNT.PLNN` el 2026-08-25):

| Productos | Por vencer | Vencido |
|---|---|---|
| 2 EMERGENTE, 4 QUIROGRAFARIO, 6 CENAPRO, 14 EXPRESS, 15 SUST. BIESS, 16 SUST. MERCADO | 1.3.01 | 1.3.04 |
| 17 QUIROGRAFARIO NOVACION | 1.3.02 | 1.3.04 |
| 3 EMERGENTE RESTR., 5 QUIROGRAFARIO RESTR. | 1.3.03 | 1.3.04 |
| 9 PRENDARIO | 1.3.05 | 1.3.08 |
| 10 PRENDARIO RESTR. | 1.3.07 | 1.3.08 |
| 22 PRENDARIO NOVACION | ⚠ 1.3.06 sin subcuentas — pendiente | 1.3.08 |
| 7 HIPOTECARIO | 1.3.09 | 1.3.12 |
| 8 HIPOTECARIO RESTR. | 1.3.11 | 1.3.12 |
| 21 HIPOTECARIO NOVACION | ⚠ 1.3.10 sin subcuentas — pendiente | 1.3.12 |

```sql
INSERT INTO CRD.CBPR (PRDCCDGO, PJRQCDGO, CBPRTPCR, CBPRFCIN, CBPRUSRG, CBPRESTD)
SELECT m.producto, &&EMPRESA, m.tpcr, TO_DATE('&&FECHA_DESDE','YYYY-MM-DD'), '&&USUARIO', 1
FROM (
    -- ===== POR VENCER (tpcr = 1) =====
    SELECT  2 producto, 1 tpcr FROM dual UNION ALL
    SELECT  4, 1 FROM dual UNION ALL
    SELECT  6, 1 FROM dual UNION ALL
    SELECT 14, 1 FROM dual UNION ALL
    SELECT 15, 1 FROM dual UNION ALL
    SELECT 16, 1 FROM dual UNION ALL
    SELECT 17, 1 FROM dual UNION ALL
    SELECT  3, 1 FROM dual UNION ALL
    SELECT  5, 1 FROM dual UNION ALL
    SELECT  9, 1 FROM dual UNION ALL
    SELECT 10, 1 FROM dual UNION ALL
    SELECT  7, 1 FROM dual UNION ALL
    SELECT  8, 1 FROM dual UNION ALL
    -- ===== VENCIDO (tpcr = 2) — incluye 21 y 22, cuyas familias de vencido SÍ existen =====
    SELECT  2, 2 FROM dual UNION ALL
    SELECT  3, 2 FROM dual UNION ALL
    SELECT  4, 2 FROM dual UNION ALL
    SELECT  5, 2 FROM dual UNION ALL
    SELECT  6, 2 FROM dual UNION ALL
    SELECT 14, 2 FROM dual UNION ALL
    SELECT 15, 2 FROM dual UNION ALL
    SELECT 16, 2 FROM dual UNION ALL
    SELECT 17, 2 FROM dual UNION ALL
    SELECT  9, 2 FROM dual UNION ALL
    SELECT 10, 2 FROM dual UNION ALL
    SELECT 22, 2 FROM dual UNION ALL
    SELECT  7, 2 FROM dual UNION ALL
    SELECT  8, 2 FROM dual UNION ALL
    SELECT 21, 2 FROM dual
) m;
```
Esperado: **28 filas** (13 por vencer + 15 vencido).

### 2.2 Bandas (CRD.BNDP)

Patrones de bandas (períodos de 30 días; NULL = banda abierta):
`PV360` por vencer (todas las familias): 1, 2, 3, 6, NULL · `V270` quirografario vencido: 1, 2, 3, 3, NULL · `V360` prendario vencido: 1, 2, 3, 6, NULL · `VHIP` hipotecario vencido: 1, 2, 6, 3, 12, NULL.

```sql
INSERT INTO CRD.BNDP (CBPRCDGO, BNDPNMRO, BNDPCNTD, PLNNCDGO, BNDPUSRG, BNDPESTD)
SELECT c.CBPRCDGO, b.numero, b.periodos, pl.PLNNCDGO, '&&USUARIO', 1
FROM (
    -- producto, tipo cartera, familia contable, patron de bandas
    SELECT  2 producto, 1 tpcr, '1.3.01' familia, 'PV360' patron FROM dual UNION ALL
    SELECT  4, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT  6, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 14, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 15, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 16, 1, '1.3.01', 'PV360' FROM dual UNION ALL
    SELECT 17, 1, '1.3.02', 'PV360' FROM dual UNION ALL
    SELECT  3, 1, '1.3.03', 'PV360' FROM dual UNION ALL
    SELECT  5, 1, '1.3.03', 'PV360' FROM dual UNION ALL
    SELECT  9, 1, '1.3.05', 'PV360' FROM dual UNION ALL
    SELECT 10, 1, '1.3.07', 'PV360' FROM dual UNION ALL
    SELECT  7, 1, '1.3.09', 'PV360' FROM dual UNION ALL
    SELECT  8, 1, '1.3.11', 'PV360' FROM dual UNION ALL
    SELECT  2, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  3, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  4, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  5, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  6, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 14, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 15, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 16, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT 17, 2, '1.3.04', 'V270' FROM dual UNION ALL
    SELECT  9, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT 10, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT 22, 2, '1.3.08', 'V360' FROM dual UNION ALL
    SELECT  7, 2, '1.3.12', 'VHIP' FROM dual UNION ALL
    SELECT  8, 2, '1.3.12', 'VHIP' FROM dual UNION ALL
    SELECT 21, 2, '1.3.12', 'VHIP' FROM dual
) f
JOIN (
    -- patron, sufijo de cuenta, numero de banda, periodos de 30 dias (NULL = abierta)
    SELECT 'PV360' patron, '05' sufijo, 1 numero, 1 periodos FROM dual UNION ALL
    SELECT 'PV360', '10', 2, 2    FROM dual UNION ALL
    SELECT 'PV360', '15', 3, 3    FROM dual UNION ALL
    SELECT 'PV360', '20', 4, 6    FROM dual UNION ALL
    SELECT 'PV360', '25', 5, NULL FROM dual UNION ALL
    SELECT 'V270',  '05', 1, 1    FROM dual UNION ALL
    SELECT 'V270',  '10', 2, 2    FROM dual UNION ALL
    SELECT 'V270',  '15', 3, 3    FROM dual UNION ALL
    SELECT 'V270',  '20', 4, 3    FROM dual UNION ALL
    SELECT 'V270',  '25', 5, NULL FROM dual UNION ALL
    SELECT 'V360',  '05', 1, 1    FROM dual UNION ALL
    SELECT 'V360',  '10', 2, 2    FROM dual UNION ALL
    SELECT 'V360',  '15', 3, 3    FROM dual UNION ALL
    SELECT 'V360',  '20', 4, 6    FROM dual UNION ALL
    SELECT 'V360',  '25', 5, NULL FROM dual UNION ALL
    SELECT 'VHIP',  '00', 1, 1    FROM dual UNION ALL
    SELECT 'VHIP',  '05', 2, 2    FROM dual UNION ALL
    SELECT 'VHIP',  '10', 3, 6    FROM dual UNION ALL
    SELECT 'VHIP',  '15', 4, 3    FROM dual UNION ALL
    SELECT 'VHIP',  '20', 5, 12   FROM dual UNION ALL
    SELECT 'VHIP',  '25', 6, NULL FROM dual
) b ON b.patron = f.patron
JOIN CRD.CBPR c
  ON c.PRDCCDGO = f.producto AND c.CBPRTPCR = f.tpcr
 AND c.PJRQCDGO = &&EMPRESA AND c.CBPRFCFN IS NULL AND c.CBPRESTD = 1
JOIN CNT.PLNN pl
  ON pl.PLNNCNTA = f.familia || '.' || b.sufijo
 AND pl.PJRQCDGO = &&EMPRESA AND pl.PLNNESTD = 1;
```
Esperado: **143 filas** = por vencer 13 configs × 5 bandas (65) + vencido quirografario 9 × 5 (45) + vencido prendario 3 × 5 (15) + vencido hipotecario 3 × 6 (18). En todo caso la verdad la da el control 3.2: **cada configuración debe quedar con TODAS sus bandas** (5, o 6 el hipotecario vencido) — ninguna incompleta ni vacía.

`COMMIT` solo después de pasar los controles del paso 3.

---

## 3. Controles POSTERIORES (antes del COMMIT)

### 3.1 Toda configuración tiene bandas, consecutivas y con una sola abierta al final

```sql
SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR,
       COUNT(*) bandas,
       MAX(b.BNDPNMRO) ultima,
       SUM(CASE WHEN b.BNDPCNTD IS NULL THEN 1 ELSE 0 END) abiertas,
       MAX(CASE WHEN b.BNDPCNTD IS NULL THEN b.BNDPNMRO END) num_abierta
FROM CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
GROUP BY c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR
HAVING COUNT(*) <> MAX(b.BNDPNMRO)
    OR SUM(CASE WHEN b.BNDPCNTD IS NULL THEN 1 ELSE 0 END) <> 1
    OR MAX(CASE WHEN b.BNDPCNTD IS NULL THEN b.BNDPNMRO END) <> MAX(b.BNDPNMRO);
```
Esperado: **0 filas** (toda violación aparece aquí).

### 3.2 Configuraciones incompletas o vacías

```sql
SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR, COUNT(b.BNDPCDGO) bandas
FROM CRD.CBPR c LEFT JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
GROUP BY c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR
HAVING COUNT(b.BNDPCDGO) NOT IN (5, 6)
ORDER BY c.PRDCCDGO;
```
Esperado: **0 filas**. Si alguna configuración sale con 0 bandas, faltan cuentas en `CNT.PLNN` (revisar control 1.4) — hacer ROLLBACK, corregir y repetir.

### 3.3 Vista completa para revisión de contabilidad

```sql
SELECT p.PRDCNMBR producto,
       DECODE(c.CBPRTPCR, 1, 'POR VENCER', 2, 'VENCIDO') cartera,
       b.BNDPNMRO banda, NVL(TO_CHAR(b.BNDPCNTD), 'RESTO') periodos,
       30 * NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
            ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) + 1 dia_ini,
       30 * SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO) dia_fin,
       pl.PLNNCNTA cuenta, pl.PLNNNMBR nombre_cuenta
FROM CRD.CBPR c
JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
JOIN CNT.PLNN pl ON pl.PLNNCDGO = b.PLNNCDGO
ORDER BY p.PRDCCDGO, c.CBPRTPCR, b.BNDPNMRO;
```
Revisar a ojo: la banda 1 arranca en dia_ini = 1; la última dice RESTO (dia_fin NULL); las cuentas corresponden a la familia del producto.

Si todo cuadra: `COMMIT;`

---

## 4. Pendiente documentado: productos NOVACION sin bandas de por vencer

`1.3.06` (prendarios renovados) y `1.3.10` (hipotecarios renovados) no tienen subcuentas de bandas en `CNT.PLNN`. Los productos **22 PRENDARIO NOVACION** y **21 HIPOTECARIO NOVACION** quedaron SIN configuración de por vencer.

Cuando contabilidad cree las subcuentas (patrón `1.3.06.05`…`.25` y `1.3.10.05`…`.25`, mismas descripciones que 1.3.05/1.3.09), ejecutar:

```sql
INSERT INTO CRD.CBPR (PRDCCDGO, PJRQCDGO, CBPRTPCR, CBPRFCIN, CBPRUSRG, CBPRESTD)
SELECT m.producto, &&EMPRESA, 1, TO_DATE('&&FECHA_DESDE','YYYY-MM-DD'), '&&USUARIO', 1
FROM (SELECT 22 producto FROM dual UNION ALL SELECT 21 FROM dual) m;

INSERT INTO CRD.BNDP (CBPRCDGO, BNDPNMRO, BNDPCNTD, PLNNCDGO, BNDPUSRG, BNDPESTD)
SELECT c.CBPRCDGO, b.numero, b.periodos, pl.PLNNCDGO, '&&USUARIO', 1
FROM (SELECT 22 producto, '1.3.06' familia FROM dual UNION ALL
      SELECT 21, '1.3.10' FROM dual) f
JOIN (SELECT '05' sufijo, 1 numero, 1 periodos FROM dual UNION ALL
      SELECT '10', 2, 2    FROM dual UNION ALL
      SELECT '15', 3, 3    FROM dual UNION ALL
      SELECT '20', 4, 6    FROM dual UNION ALL
      SELECT '25', 5, NULL FROM dual) b ON 1 = 1
JOIN CRD.CBPR c ON c.PRDCCDGO = f.producto AND c.CBPRTPCR = 1
  AND c.PJRQCDGO = &&EMPRESA AND c.CBPRFCFN IS NULL AND c.CBPRESTD = 1
JOIN CNT.PLNN pl ON pl.PLNNCNTA = f.familia || '.' || b.sufijo
  AND pl.PJRQCDGO = &&EMPRESA AND pl.PLNNESTD = 1;
```
Repetir los controles 3.1–3.3 y `COMMIT`.

Alternativa (si el negocio lo decide): mapear temporalmente 21→1.3.09 y 22→1.3.05 (la familia del producto base). No se hizo por defecto para no mezclar saldos de familias.
