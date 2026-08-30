# Actualización de la plantilla 21 para el asiento de aplicación de Petro (CRD)

**Fecha:** 2026-08-28 · **Fase:** 3a (integración contable de Petro — cobro y aplicación)
**Estado de ejecución:** ⬜ Sin ejecutar en ningún ambiente. Verificado por SELECT contra la BD LOCAL de desarrollo (`saa-oracle-23ai`) el 2026-08-28.
**Ejecución:** MANUAL, revisando los SELECT de control de cada paso. **No lo ejecuta el agente en pruebas ni en producción.**
**Diseño:** `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §3.3 y §8.3 · Catálogo: `com.saa.rubros.CrdLineaAsiento`
**Precedente:** mismo problema, mismo método, ya aplicado sobre los alternos 1/17/33 en `ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.md` (Fase 2, 2026-08-25). Este documento es su continuación sobre el alterno 21.

> Cliente: el plugin JDBC de VS Code. SQL puro — sin `SET`, `DEFINE` ni `WHENEVER`.

---

## 0. El problema que esto resuelve

El **asiento 2 de Petro** (§3.3 del levantamiento: aplicación del cobro a las cuentas reales)
necesita, además de las bandas de capital (que salen de `CRD.BNDP`, sin tocar), estas líneas
de la plantilla **alterno 21** ("CRD COBRO PETRO ASIENTO CONTABLE CORRELACIONADO CIERRE
CARTERA"): aportes por aplicar, préstamos por aplicar, aportes cesantía, aportes **jubilación**,
interés ordinario, interés de mora y seguro de desgravamen.

Verificado contra la BD local el 2026-08-28: los `DTPLAXL1` de esta plantilla son
**posicionales** (1..44 por orden de captura), igual que lo que Fase 2 ya corrigió en 1/17/33.
Además hay dos defectos de datos que bloquean directamente el asiento de Petro:

1. **Falta la línea de aportes JUBILACIÓN (`2.1.02.05.01`).** La plantilla solo tiene
   `2.1.01.05.01` (cesantía, posición 3) y `2.1.02.15` (aporte adicional personal, posición
   4) — ninguna es jubilación. Sin esa línea, un servicio que pida el papel
   `APORTES_JUBILACION` no encuentra nada y el asiento de Petro no se puede armar.
2. **Las posiciones 36 y 37 duplican la cuenta `1.4.02.05`** (interés ordinario
   quirografario) cuando una de las dos debería ser `1.4.02.10` (prendario) — defecto ya
   anotado en §8.3.2 del levantamiento, sin corregir hasta ahora. Sin corregirlo, el interés
   ordinario prendario no tiene línea propia.

Este documento fija los auxiliares al catálogo semántico `CRD_LINEA_ASIENTO`
(`com.saa.rubros.CrdLineaAsiento`), agrega la línea de jubilación que falta y corrige el
duplicado del interés prendario — **todo antes de que el servicio de Fase 3a los consuma.**

| Código | Papel | Cuenta |
|---|---|---|
| 3 | `APORTES_POR_APLICAR` | `2.3.02.05` |
| 4 | `PRESTAMOS_POR_APLICAR` | `2.3.02.10` |
| 10 | `INTERES_ORDINARIO_POR_COBRAR` | `1.4.02.xx` — dimensión en `DTPLAXL2` |
| 20 | `INTERES_MORA_POR_COBRAR` | `1.4.02.xx` — misma cuenta que el ordinario |
| 50 | `APORTES_CESANTIA` | `2.1.01.05.01` |
| 51 | `APORTES_JUBILACION` | `2.1.02.05.01` — **línea nueva, hoy no existe** |
| 52 | `APORTE_ADICIONAL_PERSONAL` | `2.1.02.15` — solo se renumera para no chocar con el 4; fuera del alcance de Petro |
| 60 | `SEGURO_DESGRAVAMEN` | `1.4.90.90.10` |

`DTPLAXL2` en las líneas de interés = `CRD.TPPR.TPPRCDGO` (`1` quirografario, `2` hipotecario,
`3` prendario), mismo criterio que Fase 2.

**Las 34 líneas de banda (posiciones 5-35, cuentas `1.3.01.xx`–`1.3.12.xx`) NO se tocan.** El
capital por banda sale de `CRD.BNDP` vía `ClasificadorBandaService`; el saneamiento completo de
esas líneas es Fase 4 del §8.3, igual que quedó explícito en el documento de Fase 2.

**Las líneas de seguro de préstamo hipotecario/prendario (posiciones 42/43, cuentas
`1.4.90.15.02`/`.03`) tampoco se tocan.** El asiento 2 de Petro (§3.3) no las menciona; quedan
pendientes para cuando se aborde el pago manual (§3.4), que sí las necesita.

---

## 1. Controles PREVIOS (no continuar si algo no cuadra)

### 1.1 La plantilla existe y está activa

```sql
SELECT PLNSCDGO, PLNSCDAL, PLNSNMBR, PLNSESTD, PJRQCDGO
FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL = 21;
```

Esperado (BD local): `1107 / 21 / CRD COBRO PETRO ASIENTO CONTABLE CORRELACIONADO CIERRE
CARTERA`, `PLNSESTD = 1`. **En producción el `PLNSCDGO` será otro**: los pasos siguientes
resuelven la plantilla por `(PLNSCDAL, PJRQCDGO)`, nunca por el id.

### 1.2 Estado actual de las líneas no-banda

```sql
SELECT d.DTPLCDGO, c.PLNNCNTA cuenta, d.DTPLDSCR,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') movimiento, d.DTPLAXL1 aux1, d.DTPLAXL2 aux2
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLAXL1 NOT BETWEEN 5 AND 35
ORDER BY d.DTPLAXL1;
```

Esperado antes de los cambios (verificado 2026-08-28): 10 líneas —
`aux1=1` `2.3.02.05` DEBE, `aux1=2` `2.3.02.10` DEBE, `aux1=3` `2.1.01.05.01` HABER,
`aux1=4` `2.1.02.15` HABER, `aux1=36` `1.4.02.05` HABER "INTERESES POR CUOTA",
`aux1=37` `1.4.02.05` HABER "INTERESES POR CUOTA" **(duplicado de 36)**,
`aux1=38` `1.4.02.15` HABER "INTERESES POR CUOTA", `aux1=39` `1.4.02.05` HABER "INTERESES
POR MORA", `aux1=40` `1.4.02.10` HABER "INTERESES POR MORA", `aux1=41` `1.4.02.15` HABER
"INTERESES POR MORA", `aux1=44` `1.4.90.90.10` HABER. Ninguna línea para `2.1.02.05.01`.

### 1.3 Las cuentas destino existen para la empresa

```sql
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR FROM CNT.PLNN
WHERE PJRQCDGO = 1236 AND PLNNCNTA IN ('2.1.02.05.01', '1.4.02.10');
```

Esperado: `10354 / 2.1.02.05.01 / APORTES PERSONALES JUBILACION` y
`9511 / 1.4.02.10 / INTERESES POR PRESTAMOS PRENDARIOS`. Sin estas dos filas el paso 2.4 y el
2.5 no tienen a dónde apuntar — **no continuar si falta alguna.**

### 1.4 La secuencia que usa el INSERT del paso 2.2 existe

```sql
SELECT SEQUENCE_OWNER, SEQUENCE_NAME FROM ALL_SEQUENCES
WHERE SEQUENCE_OWNER = 'CNT' AND SEQUENCE_NAME = 'SQ_DTPLCDGO';
```

Esperado: **1 fila** (`CNT` / `SQ_DTPLCDGO`) — verificado en la BD local el 2026-08-28. Si no
aparece, el `INSERT` del paso 2.2 falla con `ORA-02289` de forma ruidosa (no silenciosa), pero
mejor confirmarlo acá: el documento promete "no continuar si algo no cuadra".

### 1.5 Los `aux2` de partida de las seis líneas de interés (para el reverso de §5)

```sql
SELECT d.DTPLAXL1, d.DTPLAXL2 FROM CNT.DTPL d
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 IN (36, 37, 38, 39, 40, 41) ORDER BY d.DTPLAXL1;
```

Esperado y **verificado en la BD local el 2026-08-28**: las seis filas traen `DTPLAXL2 = 0`
(no `NULL`). El bloque de reverso de §5 restaura `DTPLAXL2 = 0` para las seis — coincide
exactamente con este valor de partida, no es una suposición.

---

## 2. Cambios

### 2.0 RESPALDO — antes de tocar nada (convención de la casa)

Mismo patrón que `sql/62_CORRECCION_VALOR_APORTES_CARGA.sql` §2: guarda la fila COMPLETA
(`SELECT *`) de TODAS las líneas no-banda de la plantilla, para poder reversar cualquier
columna aunque alguien corra el documento por partes y se confunda de paso — el mapeo inverso
de §5 cubre el camino feliz, esto cubre el resto.

**Cambiar `20260828` por la fecha de ejecución real** si se corre otro día.

```sql
CREATE TABLE CNT.BKP_DTPL_P21_20260828 AS
SELECT d.* FROM CNT.DTPL d
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 NOT BETWEEN 5 AND 35;

-- Verificar que el respaldo tiene EXACTAMENTE las 10 líneas de la §1.2.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CNT.BKP_DTPL_P21_20260828;
```

Esperado: **10 filas respaldadas** (las mismas de §1.2, antes de que exista la línea de
jubilación). No continuar si el conteo no da 10.

### 2.1 Renumerar por aplicar, cesantía y aporte adicional

Se ancla en la CUENTA, no en el id, mismo criterio que Fase 2:

```sql
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                   WHEN '2.3.02.05'    THEN 3
                   WHEN '2.3.02.10'    THEN 4
                   WHEN '2.1.01.05.01' THEN 50
                   WHEN '2.1.02.15'    THEN 52
                   ELSE d.DTPLAXL1 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 IN (1, 2, 3, 4);
```

Esperado: **4 filas actualizadas** la primera vez. Es idempotente: en la segunda corrida las
mismas 4 filas siguen entrando en el filtro (3 y 4 quedan dentro del rango 1-4), pero el
`CASE` las vuelve a fijar al mismo valor — no hay efecto neto ni error.

### 2.2 Agregar la línea de aportes JUBILACIÓN que falta

```sql
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
                      DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, s.PLNSCDGO, c.PLNNCDGO,
       'APORTES PERSONALES JUBILACION', 2, 51, 0, 0, 0, 0, 1
FROM CNT.PLNS s, CNT.PLNN c
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236
  AND c.PLNNCNTA = '2.1.02.05.01' AND c.PJRQCDGO = 1236
  AND NOT EXISTS (
    SELECT 1 FROM CNT.DTPL x WHERE x.PLNSCDGO = s.PLNSCDGO AND x.DTPLAXL1 = 51
  );
```

Esperado: **1 fila insertada** la primera vez, **0** si ya se aplicó (idempotente por el
`NOT EXISTS`, mismo patrón que el paso 2.5 de Fase 2).

### 2.3 Corregir el duplicado de interés ordinario y renumerar las seis líneas de interés

La posición 37 hoy apunta a `1.4.02.05` (quirografario, duplicado de la 36); pasa a apuntar a
`1.4.02.10` (prendario), que es la cuenta que le faltaba a esta plantilla.

```sql
-- 36: ordinario QUIROGRAFARIO (cuenta ya correcta, solo se renumera)
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 10, d.DTPLAXL2 = 1
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 36;

-- 37: ordinario PRENDARIO (ERRATA corregida: de 1.4.02.05 pasa a 1.4.02.10)
UPDATE CNT.DTPL d
SET d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c
                  WHERE c.PLNNCNTA = '1.4.02.10' AND c.PJRQCDGO = 1236),
    d.DTPLAXL1 = 10, d.DTPLAXL2 = 3
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 37;

-- 38: ordinario HIPOTECARIO (cuenta ya correcta, solo se renumera)
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 10, d.DTPLAXL2 = 2
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 38;

-- 39: mora QUIROGRAFARIO
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 1
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 39;

-- 40: mora PRENDARIO (cuenta ya correcta, 1.4.02.10)
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 3
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 40;

-- 41: mora HIPOTECARIO
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 20, d.DTPLAXL2 = 2
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 41;
```

Esperado: **1 fila actualizada por cada UPDATE** (6 en total) la primera vez; **0** en
corridas posteriores, porque cada `WHERE` filtra por el `DTPLAXL1` VIEJO, que deja de existir
apenas se aplica el cambio — idempotente por construcción, sin necesitar `NOT EXISTS`.

### 2.4 Renumerar el seguro de desgravamen

```sql
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = 60
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 44;
```

Esperado: **1 fila actualizada** la primera vez, **0** después.

---

## 3. Controles POSTERIORES (antes del COMMIT)

### 3.1 Las ocho líneas que necesita el asiento de aplicación de Petro, cada una con su cuenta

```sql
SELECT d.DTPLAXL1 papel, d.DTPLAXL2 tipo_prestamo,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') mov, c.PLNNCNTA cuenta, d.DTPLDSCR
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1
  AND d.DTPLAXL1 IN (3, 4, 10, 20, 50, 51, 60)
ORDER BY d.DTPLAXL1, d.DTPLAXL2;
```

Esperado, exactamente **11 filas**:

| papel | tipo | mov | cuenta |
|---|---|---|---|
| 3 | — | DEBE | 2.3.02.05 |
| 4 | — | DEBE | 2.3.02.10 |
| 10 | 1 | HABER | 1.4.02.05 |
| 10 | 2 | HABER | 1.4.02.15 |
| 10 | 3 | HABER | 1.4.02.10 |
| 20 | 1 | HABER | 1.4.02.05 |
| 20 | 2 | HABER | 1.4.02.15 |
| 20 | 3 | HABER | 1.4.02.10 |
| 50 | — | HABER | 2.1.01.05.01 |
| 51 | — | HABER | 2.1.02.05.01 |
| 60 | — | HABER | 1.4.90.90.10 |

### 3.2 Ninguna combinación repetida en toda la plantilla

```sql
SELECT d.DTPLAXL1, d.DTPLAXL2, COUNT(*) veces
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1
GROUP BY d.DTPLAXL1, d.DTPLAXL2
HAVING COUNT(*) > 1;
```

Esperado: **0 filas**. Si sale alguna, dos líneas compiten por el mismo papel y el servicio
tomaría la primera por código, con una cuenta arbitraria.

### 3.3 Ninguna línea sin cuenta

```sql
SELECT d.DTPLCDGO, d.DTPLAXL1
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1 AND d.PLNNCDGO IS NULL;
```

Esperado: **0 filas**.

### 3.4 Total de líneas de la plantilla sin cambiar (44 + 1 nueva = 45)

```sql
SELECT COUNT(*) FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236 AND d.DTPLESTD = 1;
```

Esperado: **45** (las 44 originales más la línea de jubilación del paso 2.2). Si sale un
número distinto, algo se insertó o se borró de más.

Si los cuatro controles pasan: `COMMIT;`

---

## 4. Lo que este documento NO hace

- **No toca las 34 líneas de banda** (`DTPLAXL1` 5-35, cuentas `1.3.01.xx`–`1.3.12.xx`). Esas
  cuentas salen de `CRD.BNDP` vía `ClasificadorBandaService`; el saneamiento de esas líneas es
  Fase 4 (§8.3 del levantamiento), igual que en el documento de Fase 2.
- **No toca las líneas de seguro de préstamo** (`DTPLAXL1` 42 `1.4.90.15.02` hipotecario, 43
  `1.4.90.15.03` prendario). El asiento 2 de Petro (§3.3) no las necesita; quedan pendientes
  para cuando se implemente el pago manual (§3.4), que sí las usa.
- **No define ningún papel nuevo para `APORTE_ADICIONAL_PERSONAL` (52) más allá de evitar la
  colisión de posición con `PRESTAMOS_POR_APLICAR`.** Nadie en Fase 3a lo consume.
- **No toca las plantillas 19 y 20** (cobro): son correctas tal como están, decisión del
  usuario del 2026-08-28 — ver §5.11 del levantamiento.

---

## 5. Si hay que deshacerlo

**⛔ TODO EL BLOQUE VA COMENTADO.** Regla del proyecto, de un incidente real (casi se ejecuta
un `DELETE FROM CNTR` sin `WHERE` copiando un bloque de reverso suelto). Cada `WHERE` de acá
abajo SÍ está acotado, pero un bloque ejecutable dentro de un documento que se puede copiar y
pegar por partes es el mismo riesgo. **Descomentar únicamente si hay que revertir esta
actualización completa**, después de confirmar con §1.2/§1.5 que el estado actual es
efectivamente el que dejó este documento (no una mezcla a medio aplicar) — si es una mezcla,
usar `CNT.BKP_DTPL_P21_20260828` (§2.0) en vez de este mapeo inverso.

```sql
-- UPDATE CNT.DTPL d
-- SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
--                    WHEN '2.3.02.05'    THEN 1
--                    WHEN '2.3.02.10'    THEN 2
--                    WHEN '2.1.01.05.01' THEN 3
--                    WHEN '2.1.02.15'    THEN 4
--                    ELSE d.DTPLAXL1 END
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 IN (3, 4, 50, 52);
--
-- DELETE FROM CNT.DTPL d
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                     WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 51;
--
-- -- Deshacer las seis líneas de interés, una por una (mapeo inverso exacto del paso 2.3;
-- -- la de aux1=10/aux2=3 además vuelve su PLNNCDGO a 1.4.02.05, deshaciendo la corrección
-- -- de la errata). DTPLAXL2 = 0 en las seis: verificado contra el valor de partida real en
-- -- §1.5, no es una suposición.
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 36, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 1;
--
-- UPDATE CNT.DTPL d
-- SET d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c WHERE c.PLNNCNTA = '1.4.02.05' AND c.PJRQCDGO = 1236),
--     d.DTPLAXL1 = 37, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 3;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 38, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 10 AND d.DTPLAXL2 = 2;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 39, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 1;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 40, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 3;
--
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 41, d.DTPLAXL2 = 0
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 2;
--
-- -- Deshacer el seguro de desgravamen
-- UPDATE CNT.DTPL d SET d.DTPLAXL1 = 44
-- WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 21 AND s.PJRQCDGO = 1236)
--   AND d.DTPLAXL1 = 60;
```
