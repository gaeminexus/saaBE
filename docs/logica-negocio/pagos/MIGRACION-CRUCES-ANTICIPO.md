# Migración: cruces de anticipo contra un anticipo específico

**Fecha:** 2026-08-20
**Estado:** PENDIENTE DE EJECUTAR — correr los SELECT de control de cada fase y revisar el resultado antes de pasar al DML de la fase siguiente.
**Aplica a:** producción (sistema en vivo desde agosto de 2026, con anticipos ya cruzados).

---

## 0. Qué cambia y por qué hace falta migrar

Hasta ahora el cruce de un anticipo con una factura se hacía **por valor contra el
saldo global** del titular (`TSR.PRCC.PRCCSLIN`). La aplicación (`PGS.APLP` /
`CBR.APLC`) no guardaba de qué anticipo salía el dinero: su FK
`APLPANTP`/`APLCANTC` apuntaba al **movimiento negativo** que el cruce dejaba en la
propia tabla de anticipos. Al anular un anticipo no había forma de saber con
exactitud qué abonos deshacer.

El modelo nuevo:

| Antes | Ahora |
|---|---|
| El cruce descuenta el saldo global del titular | El cruce descuenta el saldo de **un anticipo concreto** (`APLPANTO`/`APLCANTO`) |
| Una aplicación por operación, cualquiera sea el origen | **Una aplicación por anticipo consumido**, cada una con su asiento |
| `ANTPSALD`/`ANTCSALD` = saldo global acumulado al momento del movimiento | `ANTPSALD`/`ANTCSALD` = **saldo disponible de ese anticipo** |
| El cruce deja una fila negativa en `ANTP`/`ANTC` | No se crean más filas negativas; el cruce vive en `APLP`/`APLC` |

Esta migración tiene que:

1. Marcar las filas negativas históricas como **estado 4 (Migrado)** para que las
   pantallas dejen de leerlas sin perder el historial.
2. Crear un **anticipo de apertura** allí donde el saldo global del titular no
   está respaldado por ninguna fila de anticipo (saldos cargados a mano al
   arrancar el sistema). Sin esto ese dinero queda fuera del control por
   anticipo y no se puede cruzar.
3. Inicializar `ANTPSALD`/`ANTCSALD` con el valor de cada anticipo.
4. **Atribuir cada cruce activo a un anticipo por FIFO**, partiendo el cruce en
   varias aplicaciones cuando abarca más de un anticipo, y descontando el saldo.
5. Verificar que la suma de saldos por anticipo cuadre con `PRCCSLIN`.

> **Nota sobre el "script de saldo inicial":** no existe. Se buscó en
> `docs/scripts/`, `docs/logica-negocio/*/sql/` y en todo el historial de git: los
> únicos scripts de anticipos son el DDL de `CBR.ANTC` y el ALTER de `PGS.PGTR`
> del circuito de pagos. Ninguno escribe `TSR.PRCC.PRCCSLIN`, y en el código los
> únicos que lo tocan son los servicios de anticipos y de aplicaciones. Si hoy
> hay titulares con saldo sin fila de anticipo, ese saldo se cargó manualmente:
> la **fase 3** es la que lo regulariza.

### Orden obligatorio

```
1. Backup de PGS.ANTP, PGS.APLP, CBR.ANTC, CBR.APLC y TSR.PRCC   (fase 1)
2. DDL:  docs/scripts/sql-cruce-anticipo-especifico.sql
3. Este documento, fase por fase, con sus controles
4. Recién entonces: desplegar el WAR nuevo
```

Desplegar el WAR **antes** de migrar deja el sistema pidiendo anticipos con saldo
que todavía está en 0, así que ningún cruce nuevo funcionaría.

---

## 1. Respaldo previo

```sql
CREATE TABLE PGS.ANTP_BK20260820 AS SELECT * FROM PGS.ANTP;
CREATE TABLE PGS.APLP_BK20260820 AS SELECT * FROM PGS.APLP;
CREATE TABLE CBR.ANTC_BK20260820 AS SELECT * FROM CBR.ANTC;
CREATE TABLE CBR.APLC_BK20260820 AS SELECT * FROM CBR.APLC;
CREATE TABLE TSR.PRCC_BK20260820 AS SELECT * FROM TSR.PRCC;
COMMIT;

-- Control: las cinco tablas deben tener filas
SELECT 'ANTP' T, COUNT(*) N FROM PGS.ANTP_BK20260820
UNION ALL SELECT 'APLP', COUNT(*) FROM PGS.APLP_BK20260820
UNION ALL SELECT 'ANTC', COUNT(*) FROM CBR.ANTC_BK20260820
UNION ALL SELECT 'APLC', COUNT(*) FROM CBR.APLC_BK20260820
UNION ALL SELECT 'PRCC', COUNT(*) FROM TSR.PRCC_BK20260820;
```

---

## 2. Fase 0 — Diagnóstico (no modifica nada)

Correr las cuatro consultas y guardar el resultado: son la foto contra la que se
verifica el final.

### 2.1 Cruces existentes

```sql
-- CXP: cruces de anticipo, activos y reversados
SELECT ap.APLPESTD                                  AS estado_aplicacion,
       COUNT(*)                                     AS cruces,
       SUM(ap.APLPMAPL)                             AS total
  FROM PGS.APLP ap
 WHERE ap.APLPTDPG = 4
 GROUP BY ap.APLPESTD
 ORDER BY 1;

-- CXC: idem
SELECT ac.APLCESTD, COUNT(*), SUM(ac.APLCMAPL)
  FROM CBR.APLC ac
 WHERE ac.APLCTDPG = 4
 GROUP BY ac.APLCESTD
 ORDER BY 1;
```

### 2.2 Movimientos negativos históricos

```sql
SELECT 'ANTP' origen, COUNT(*) filas, SUM(ANTPVLOR) total
  FROM PGS.ANTP WHERE ANTPVLOR < 0
UNION ALL
SELECT 'ANTC', COUNT(*), SUM(VALOR)
  FROM CBR.ANTC WHERE VALOR < 0;
```

### 2.3 Cuadre por titular: saldo global vs anticipos registrados

Esta es la consulta clave. `saldo_esperado` es lo que el saldo global debería
valer si todo estuviera respaldado por anticipos; la `diferencia` es el saldo de
apertura cargado a mano que la fase 3 va a regularizar.

```sql
-- ── CXP (proveedores, rol 2) ──────────────────────────────────────────────
SELECT pr.PRSNCDGO                              AS titular,
       pc.PJRQCDGO                              AS empresa,
       pc.PRCCCDGO                              AS prcc,
       NVL(pc.PRCCSLIN, 0)                      AS saldo_global,
       NVL(a.total_anticipos, 0)                AS total_anticipos,
       NVL(c.total_cruzado, 0)                  AS total_cruzado,
       NVL(a.total_anticipos, 0)
         - NVL(c.total_cruzado, 0)              AS saldo_esperado,
       NVL(pc.PRCCSLIN, 0)
         - (NVL(a.total_anticipos, 0)
            - NVL(c.total_cruzado, 0))          AS diferencia
  FROM TSR.PRCC pc
  JOIN TSR.PRRL pr ON pr.PRRLCDGO = pc.PRRLCDGO
  LEFT JOIN (SELECT ANTPTTLR, ANTPPJRQ, SUM(ANTPVLOR) total_anticipos
               FROM PGS.ANTP
              WHERE ANTPESTD = 2 AND ANTPVLOR > 0
              GROUP BY ANTPTTLR, ANTPPJRQ) a
         ON a.ANTPTTLR = pr.PRSNCDGO AND a.ANTPPJRQ = pc.PJRQCDGO
  LEFT JOIN (SELECT f.TITULAR, ap.APLPPJRQ, SUM(ap.APLPMAPL) total_cruzado
               FROM PGS.APLP ap
               JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
              WHERE ap.APLPTDPG = 4 AND ap.APLPESTD = 1
              GROUP BY f.TITULAR, ap.APLPPJRQ) c
         ON c.TITULAR = pr.PRSNCDGO AND c.APLPPJRQ = pc.PJRQCDGO
 WHERE pc.PRCCTPOO = 2
   AND pr.PRRLRZZA = 2
 ORDER BY diferencia DESC;
```

Para **CXC** es la misma consulta cambiando `pr.PRRLRZZA = 1`, `PGS.ANTP` →
`CBR.ANTC` (`TITULAR`, `EMPRESA`, `VALOR`, `ESTADO`), y `PGS.APLP`/`PGS.FCTC` →
`CBR.APLC`/`CBR.FCTR` (`APLCTDPG`, `APLCESTD`, `APLCFCTR`, `APLCPJRQ`, `APLCMAPL`).

**Cómo leer el resultado:**

| `diferencia` | Significa | Qué hace la migración |
|---|---|---|
| ≈ 0 | Todo el saldo está respaldado por anticipos | Nada en la fase 3 |
| > 0 | Hay saldo de apertura sin anticipo | Fase 3 le crea un anticipo por esa diferencia |
| < 0 | El saldo global es MENOR que lo esperado | **Revisar a mano antes de seguir**: hay cruces o consumos que no están en `APLP`/`APLC`. La fase 3 no lo toca. |

### 2.4 Anticipos confirmados por titular

```sql
SELECT ANTPTTLR, ANTPPJRQ, COUNT(*) anticipos, SUM(ANTPVLOR) valor,
       MIN(ANTPFANT) desde, MAX(ANTPFANT) hasta
  FROM PGS.ANTP
 WHERE ANTPESTD = 2 AND ANTPVLOR > 0
 GROUP BY ANTPTTLR, ANTPPJRQ
 ORDER BY 1, 2;
```

---

## 3. Fase 1 — DDL

Ejecutar `docs/scripts/sql-cruce-anticipo-especifico.sql` completo y correr sus
verificaciones del final. **No continuar si alguna columna o constraint falta.**

---

## 4. Fase 2 — Marcar los movimientos negativos como Migrado

Los cruces viejos dejaron filas negativas en la tabla de anticipos. Con el modelo
nuevo el cruce se lee de `APLP`/`APLC`, así que estas filas duplicarían la resta.
Se marcan con estado 4 en vez de borrarlas: son historial y hay FKs
(`APLPANTP`/`APLCANTC`) que apuntan a ellas.

### Control

```sql
SELECT ANTPCDGO, ANTPTTLR, ANTPPJRQ, ANTPFANT, ANTPVLOR, ANTPESTD, ANTPNDOC
  FROM PGS.ANTP WHERE ANTPVLOR < 0 ORDER BY ANTPTTLR, ANTPFANT;

SELECT ID, TITULAR, EMPRESA, FECHAANTICIPO, VALOR, ESTADO, NUMERODOC
  FROM CBR.ANTC WHERE VALOR < 0 ORDER BY TITULAR, FECHAANTICIPO;
```

### DML

```sql
UPDATE PGS.ANTP
   SET ANTPESTD = 4,
       ANTPSALD = 0,
       ANTPOBSR = SUBSTR(NVL(ANTPOBSR, '')
                  || ' | MIGRADO 2026-08-20: el cruce pasa a PGS.APLP', 1, 2000)
 WHERE ANTPVLOR < 0
   AND ANTPESTD <> 4;

UPDATE CBR.ANTC
   SET ESTADO      = 4,
       ANTCSALD    = 0,
       OBSERVACION = SUBSTR(NVL(OBSERVACION, '')
                     || ' | MIGRADO 2026-08-20: el cruce pasa a CBR.APLC', 1, 2000)
 WHERE VALOR < 0
   AND ESTADO <> 4;

COMMIT;
```

### Verificación

```sql
-- No debe quedar ninguna fila negativa fuera del estado 4
SELECT COUNT(*) FROM PGS.ANTP WHERE ANTPVLOR < 0 AND ANTPESTD <> 4;   -- 0
SELECT COUNT(*) FROM CBR.ANTC WHERE VALOR    < 0 AND ESTADO   <> 4;   -- 0
```

---

## 5. Fase 3 — Anticipo de apertura para el saldo sin respaldo

Para cada titular donde la `diferencia` de §2.3 es **positiva**, se crea un
anticipo por ese monto. **Sin asiento contable**: ese saldo ya está reflejado en
el plan de cuentas desde que se cargó; generar asiento lo duplicaría.

> Si en §2.3 aparecieron diferencias **negativas**, resolverlas a mano antes de
> correr esta fase. El bloque de abajo las ignora deliberadamente
> (`v.diferencia > 0.01`).

### Control — qué se va a crear

```sql
-- Correr la consulta de §2.3 filtrando por diferencia > 0.01 y anotar el total.
-- Esa es la suma de los anticipos de apertura que se van a insertar.
```

### DML — CXP

```sql
DECLARE
    v_creados   NUMBER := 0;
    v_total     NUMBER := 0;
BEGIN
    FOR r IN (
        SELECT pr.PRSNCDGO  AS titular,
               pc.PJRQCDGO  AS empresa,
               NVL(pc.PRCCSLIN, 0)
                 - (NVL(a.total_anticipos, 0) - NVL(c.total_cruzado, 0)) AS diferencia
          FROM TSR.PRCC pc
          JOIN TSR.PRRL pr ON pr.PRRLCDGO = pc.PRRLCDGO
          LEFT JOIN (SELECT ANTPTTLR, ANTPPJRQ, SUM(ANTPVLOR) total_anticipos
                       FROM PGS.ANTP WHERE ANTPESTD = 2 AND ANTPVLOR > 0
                      GROUP BY ANTPTTLR, ANTPPJRQ) a
                 ON a.ANTPTTLR = pr.PRSNCDGO AND a.ANTPPJRQ = pc.PJRQCDGO
          LEFT JOIN (SELECT f.TITULAR, ap.APLPPJRQ, SUM(ap.APLPMAPL) total_cruzado
                       FROM PGS.APLP ap JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
                      WHERE ap.APLPTDPG = 4 AND ap.APLPESTD = 1
                      GROUP BY f.TITULAR, ap.APLPPJRQ) c
                 ON c.TITULAR = pr.PRSNCDGO AND c.APLPPJRQ = pc.PJRQCDGO
         WHERE pc.PRCCTPOO = 2
           AND pr.PRRLRZZA = 2
    ) LOOP
        IF r.diferencia > 0.01 THEN
            INSERT INTO PGS.ANTP (ANTPCDGO, ANTPTTLR, ANTPPJRQ, ANTPFANT, ANTPFRCP,
                                  ANTPNDOC, ANTPVLOR, ANTPSALD, ANTPOBSR, ANTPESTD,
                                  ANTPUSAR, ANTPASNT, ANTPFCRG)
            VALUES (PGS.SQ_ANTPCDGO.NEXTVAL,
                    r.titular, r.empresa,
                    DATE '2026-08-01',          -- fecha de apertura: ajustar si aplica
                    DATE '2026-08-01',
                    'SALDO INICIAL MIGRADO',
                    ROUND(r.diferencia, 2),
                    ROUND(r.diferencia, 2),
                    'Anticipo de apertura creado por la migración 2026-08-20. '
                    || 'Respalda el saldo de anticipos cargado a mano; sin asiento '
                    || 'contable porque ese saldo ya estaba contabilizado.',
                    2,                          -- Confirmado
                    NULL, NULL, SYSTIMESTAMP);
            v_creados := v_creados + 1;
            v_total   := v_total + ROUND(r.diferencia, 2);
        END IF;
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('Anticipos de apertura CXP creados: ' || v_creados
                         || ' | total: ' || v_total);
END;
/
COMMIT;
```

### DML — CXC

Idéntico, con `pr.PRRLRZZA = 1`, `CBR.SQ_ANTCCDGO`, y la tabla `CBR.ANTC`:

```sql
INSERT INTO CBR.ANTC (ID, TITULAR, EMPRESA, FECHAANTICIPO, FECHARECEPCION,
                      NUMERODOC, VALOR, ANTCSALD, OBSERVACION, ESTADO,
                      USUARIO, ASIENTO, FECHAREGISTRO)
VALUES (CBR.SQ_ANTCCDGO.NEXTVAL, r.titular, r.empresa,
        DATE '2026-08-01', DATE '2026-08-01',
        'SALDO INICIAL MIGRADO',
        ROUND(r.diferencia, 2), ROUND(r.diferencia, 2),
        'Anticipo de apertura creado por la migración 2026-08-20. ...',
        2, NULL, NULL, SYSTIMESTAMP);
```

(el `FOR r IN (...)` es la consulta de §2.3 en su variante CXC).

### Verificación

```sql
SELECT COUNT(*) creados, SUM(ANTPVLOR) total
  FROM PGS.ANTP WHERE ANTPNDOC = 'SALDO INICIAL MIGRADO';

SELECT COUNT(*), SUM(VALOR)
  FROM CBR.ANTC WHERE NUMERODOC = 'SALDO INICIAL MIGRADO';
```

El total debe coincidir con la suma de diferencias positivas anotada en el control.

---

## 6. Fase 4 — Inicializar el saldo disponible de cada anticipo

`ANTPSALD` deja de ser el acumulado global y pasa a ser el saldo del propio
anticipo. Se arranca en el valor; la fase 5 descuenta los cruces.

```sql
UPDATE PGS.ANTP SET ANTPSALD = ANTPVLOR
 WHERE ANTPESTD = 2 AND ANTPVLOR > 0;

UPDATE PGS.ANTP SET ANTPSALD = 0
 WHERE ANTPESTD IN (1, 3, 4) OR ANTPVLOR <= 0;

UPDATE CBR.ANTC SET ANTCSALD = VALOR
 WHERE ESTADO = 2 AND VALOR > 0;

UPDATE CBR.ANTC SET ANTCSALD = 0
 WHERE ESTADO IN (1, 3, 4) OR VALOR <= 0;

COMMIT;
```

> Los anticipos **Ingresados (1)** quedan en 0 a propósito: todavía no tienen
> saldo acreditado (su pago no fue confirmado). El backend les pone el saldo
> cuando el pago se confirma.

### Verificación

```sql
SELECT COUNT(*) FROM PGS.ANTP WHERE ANTPESTD = 2 AND ANTPVLOR > 0 AND ANTPSALD <> ANTPVLOR;  -- 0
SELECT COUNT(*) FROM CBR.ANTC WHERE ESTADO   = 2 AND VALOR    > 0 AND ANTCSALD <> VALOR;     -- 0
```

---

## 7. Fase 5 — Atribuir cada cruce a su anticipo (FIFO)

El corazón de la migración. Por cada titular/empresa se recorren los cruces
**activos** en orden cronológico y se van consumiendo los anticipos del más
antiguo al más nuevo.

Cuando un cruce abarca **más de un anticipo**, se parte: la fila original se
queda con la primera porción y se insertan filas adicionales con el resto. La
suma de montos no cambia, así que el saldo de la factura queda igual — solo pasa
a haber una aplicación por anticipo, que es la invariante del modelo nuevo.

Los cruces **reversados** (`APLPESTD = 2`) no se atribuyen: ya devolvieron su
saldo en su momento y quedan con `APLPANTO` nulo. Está documentado y el backend
lo tolera.

### Control — cruces que se van a partir

```sql
-- Cruces activos ordenados, con el saldo de anticipos disponible acumulado:
-- los que aparezcan abarcando dos tramos son los que se van a partir.
SELECT f.TITULAR, ap.APLPPJRQ AS empresa, ap.APLPCDGO AS aplicacion,
       f.NUMERO AS factura, ap.APLPFAPL AS fecha, ap.APLPMAPL AS monto
  FROM PGS.APLP ap
  JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
 WHERE ap.APLPTDPG = 4 AND ap.APLPESTD = 1 AND ap.APLPANTO IS NULL
 ORDER BY f.TITULAR, ap.APLPPJRQ, ap.APLPFAPL, ap.APLPCDGO;

-- Anticipos disponibles en el mismo orden en que se van a consumir
SELECT ANTPTTLR AS titular, ANTPPJRQ AS empresa, ANTPCDGO AS anticipo,
       ANTPFANT AS fecha, ANTPVLOR AS valor, ANTPSALD AS saldo
  FROM PGS.ANTP
 WHERE ANTPESTD = 2 AND ANTPVLOR > 0 AND ANTPSALD > 0
 ORDER BY ANTPTTLR, ANTPPJRQ, ANTPFANT, ANTPCDGO;
```

### DML — CXP

```sql
SET SERVEROUTPUT ON;
DECLARE
    v_pendiente   NUMBER;
    v_toma        NUMBER;
    v_primera     BOOLEAN;
    v_partidos    NUMBER := 0;
    v_atribuidos  NUMBER := 0;
    v_sin_cubrir  NUMBER := 0;
BEGIN
    -- Cruces activos aún sin anticipo de origen, en orden cronológico
    FOR cr IN (
        SELECT ap.APLPCDGO, ap.APLPMAPL, ap.APLPPJRQ, ap.APLPFCTC, ap.APLPFAPL,
               ap.APLPTDPG, ap.APLPOBSR, ap.APLPESTD, ap.APLPUSAR, ap.APLPASNT,
               ap.APLPFCRG, ap.APLPANTP, f.TITULAR
          FROM PGS.APLP ap
          JOIN PGS.FCTC f ON f.ID = ap.APLPFCTC
         WHERE ap.APLPTDPG = 4
           AND ap.APLPESTD = 1
           AND ap.APLPANTO IS NULL
         ORDER BY f.TITULAR, ap.APLPPJRQ, ap.APLPFAPL, ap.APLPCDGO
    ) LOOP
        v_pendiente := cr.APLPMAPL;
        v_primera   := TRUE;

        -- Anticipos del mismo titular/empresa con saldo, del más antiguo al más nuevo
        FOR an IN (
            SELECT ANTPCDGO, ANTPSALD
              FROM PGS.ANTP
             WHERE ANTPTTLR = cr.TITULAR
               AND ANTPPJRQ = cr.APLPPJRQ
               AND ANTPESTD = 2
               AND ANTPVLOR > 0
               AND ANTPSALD > 0
             ORDER BY ANTPFANT, ANTPCDGO
             FOR UPDATE
        ) LOOP
            EXIT WHEN v_pendiente <= 0.005;

            v_toma := LEAST(an.ANTPSALD, v_pendiente);

            IF v_primera THEN
                -- La fila original se queda con la primera porción
                UPDATE PGS.APLP
                   SET APLPANTO = an.ANTPCDGO,
                       APLPMAPL = ROUND(v_toma, 2)
                 WHERE APLPCDGO = cr.APLPCDGO;
                v_primera := FALSE;
            ELSE
                -- El resto va en aplicaciones nuevas, mismo asiento y fecha
                INSERT INTO PGS.APLP (APLPCDGO, APLPPJRQ, APLPFCTC, APLPTDPG,
                                      APLPANTO, APLPANTP, APLPMAPL, APLPFAPL,
                                      APLPOBSR, APLPESTD, APLPUSAR, APLPASNT, APLPFCRG)
                VALUES (PGS.SQ_APLPCDGO.NEXTVAL, cr.APLPPJRQ, cr.APLPFCTC, 4,
                        an.ANTPCDGO, cr.APLPANTP, ROUND(v_toma, 2), cr.APLPFAPL,
                        SUBSTR(NVL(cr.APLPOBSR, '')
                          || ' | Partido por la migración 2026-08-20 desde la aplicación '
                          || cr.APLPCDGO, 1, 2000),
                        cr.APLPESTD, cr.APLPUSAR, cr.APLPASNT, cr.APLPFCRG);
                v_partidos := v_partidos + 1;
            END IF;

            UPDATE PGS.ANTP
               SET ANTPSALD = ROUND(ANTPSALD - v_toma, 2)
             WHERE ANTPCDGO = an.ANTPCDGO;

            v_pendiente  := ROUND(v_pendiente - v_toma, 2);
            v_atribuidos := v_atribuidos + 1;
        END LOOP;

        IF v_pendiente > 0.005 THEN
            -- No hubo anticipos suficientes: queda sin atribuir y se reporta.
            v_sin_cubrir := v_sin_cubrir + 1;
            DBMS_OUTPUT.PUT_LINE('SIN CUBRIR: aplicacion ' || cr.APLPCDGO
                || ' | titular ' || cr.TITULAR || ' | falta ' || v_pendiente);
        END IF;
    END LOOP;

    DBMS_OUTPUT.PUT_LINE('CXP -> porciones atribuidas: ' || v_atribuidos
        || ' | aplicaciones nuevas por partición: ' || v_partidos
        || ' | cruces sin cubrir: ' || v_sin_cubrir);
END;
/
```

**No hacer COMMIT todavía.** Correr primero las verificaciones de §7.1 en la
misma sesión; si algo no cuadra, `ROLLBACK`.

### DML — CXC

El mismo bloque, cambiando:

| CXP | CXC |
|---|---|
| `PGS.APLP` | `CBR.APLC` |
| `APLPCDGO / APLPMAPL / APLPPJRQ / APLPFCTC / APLPFAPL / APLPTDPG / APLPOBSR / APLPESTD / APLPUSAR / APLPASNT / APLPFCRG / APLPANTP / APLPANTO` | `APLCCDGO / APLCMAPL / APLCPJRQ / APLCFCTR / APLCFAPL / APLCTDPG / APLCOBSR / APLCESTD / APLCUSAR / APLCASNT / APLCFCRG / APLCANTC / APLCANTO` |
| `PGS.FCTC` | `CBR.FCTR` |
| `PGS.ANTP` (`ANTPCDGO/ANTPTTLR/ANTPPJRQ/ANTPESTD/ANTPVLOR/ANTPSALD/ANTPFANT`) | `CBR.ANTC` (`ID/TITULAR/EMPRESA/ESTADO/VALOR/ANTCSALD/FECHAANTICIPO`) |
| `PGS.SQ_APLPCDGO` | `CBR.SQ_APLCCDGO` |

> En CXC hay cruces que pueden colgar de una **liquidación** (`APLCLQCS`) en vez
> de una factura. El cursor de arriba usa `JOIN CBR.FCTR`, que los deja fuera.
> Correr esta consulta y, si devuelve filas, atribuirlas a mano:
> ```sql
> SELECT APLCCDGO, APLCLQCS, APLCMAPL, APLCFAPL
>   FROM CBR.APLC
>  WHERE APLCTDPG = 4 AND APLCESTD = 1 AND APLCANTO IS NULL AND APLCFCTR IS NULL;
> ```

### 7.1 Verificación antes del COMMIT

```sql
-- a) Ningún cruce activo debe quedar sin anticipo de origen
SELECT COUNT(*) FROM PGS.APLP WHERE APLPTDPG = 4 AND APLPESTD = 1 AND APLPANTO IS NULL;  -- 0
SELECT COUNT(*) FROM CBR.APLC WHERE APLCTDPG = 4 AND APLCESTD = 1 AND APLCANTO IS NULL;  -- 0

-- b) Ningún saldo negativo
SELECT COUNT(*) FROM PGS.ANTP WHERE ANTPSALD < -0.005;   -- 0
SELECT COUNT(*) FROM CBR.ANTC WHERE ANTCSALD < -0.005;   -- 0

-- c) El total aplicado a cada factura NO cambió (la partición conserva la suma)
SELECT COUNT(*) AS facturas_descuadradas
  FROM (SELECT ap.APLPFCTC, SUM(ap.APLPMAPL) nuevo
          FROM PGS.APLP ap WHERE ap.APLPESTD = 1 GROUP BY ap.APLPFCTC) n
  JOIN (SELECT b.APLPFCTC, SUM(b.APLPMAPL) viejo
          FROM PGS.APLP_BK20260820 b WHERE b.APLPESTD = 1 GROUP BY b.APLPFCTC) v
    ON v.APLPFCTC = n.APLPFCTC
 WHERE ABS(NVL(n.nuevo,0) - NVL(v.viejo,0)) > 0.005;     -- 0

-- d) Saldo por anticipo = valor - cruces activos atribuidos
SELECT COUNT(*) AS anticipos_descuadrados
  FROM PGS.ANTP a
  LEFT JOIN (SELECT APLPANTO, SUM(APLPMAPL) cruzado
               FROM PGS.APLP WHERE APLPTDPG = 4 AND APLPESTD = 1
              GROUP BY APLPANTO) c ON c.APLPANTO = a.ANTPCDGO
 WHERE a.ANTPESTD = 2 AND a.ANTPVLOR > 0
   AND ABS(a.ANTPSALD - (a.ANTPVLOR - NVL(c.cruzado, 0))) > 0.005;   -- 0
```

Si (a) devuelve más de 0, revisar la salida `SIN CUBRIR` del bloque: son
titulares cuyos cruces superan sus anticipos, normalmente por una diferencia
negativa de §2.3 que quedó sin resolver.

```sql
COMMIT;   -- solo si a, b, c y d dieron 0
```

---

## 8. Fase 6 — Cuadre final

```sql
-- Suma de saldos por anticipo vs saldo global de la cuenta contable, por titular
SELECT pr.PRSNCDGO AS titular, pc.PJRQCDGO AS empresa,
       NVL(pc.PRCCSLIN, 0)   AS saldo_global,
       NVL(s.suma_saldos, 0) AS suma_por_anticipo,
       NVL(pc.PRCCSLIN, 0) - NVL(s.suma_saldos, 0) AS diferencia
  FROM TSR.PRCC pc
  JOIN TSR.PRRL pr ON pr.PRRLCDGO = pc.PRRLCDGO
  LEFT JOIN (SELECT ANTPTTLR, ANTPPJRQ, SUM(ANTPSALD) suma_saldos
               FROM PGS.ANTP WHERE ANTPESTD = 2 AND ANTPVLOR > 0
              GROUP BY ANTPTTLR, ANTPPJRQ) s
         ON s.ANTPTTLR = pr.PRSNCDGO AND s.ANTPPJRQ = pc.PJRQCDGO
 WHERE pc.PRCCTPOO = 2 AND pr.PRRLRZZA = 2
   AND ABS(NVL(pc.PRCCSLIN,0) - NVL(s.suma_saldos,0)) > 0.005
 ORDER BY 5 DESC;
```

**Esta consulta debe devolver 0 filas.** Es exactamente el mismo cuadre que el
backend expone en `GET /antp/seguimiento/{idTitular}/{idEmpresa}` (campos
`cuadra`, `diferencia` y `advertencia`), así que después del despliegue se puede
verificar desde la pantalla de seguimiento sin entrar a la base.

Repetir con `pr.PRRLRZZA = 1` y `CBR.ANTC` para CXC.

---

## 9. Rollback

Mientras no se haya desplegado el WAR nuevo:

```sql
DELETE FROM PGS.APLP WHERE APLPOBSR LIKE '%Partido por la migración 2026-08-20%';
DELETE FROM PGS.ANTP WHERE ANTPNDOC = 'SALDO INICIAL MIGRADO';
DELETE FROM CBR.APLC WHERE APLCOBSR LIKE '%Partido por la migración 2026-08-20%';
DELETE FROM CBR.ANTC WHERE NUMERODOC = 'SALDO INICIAL MIGRADO';

UPDATE PGS.APLP a SET (APLPANTO, APLPMAPL) =
       (SELECT NULL, b.APLPMAPL FROM PGS.APLP_BK20260820 b WHERE b.APLPCDGO = a.APLPCDGO)
 WHERE EXISTS (SELECT 1 FROM PGS.APLP_BK20260820 b WHERE b.APLPCDGO = a.APLPCDGO);

UPDATE PGS.ANTP a SET (ANTPSALD, ANTPESTD, ANTPOBSR) =
       (SELECT b.ANTPSALD, b.ANTPESTD, b.ANTPOBSR
          FROM PGS.ANTP_BK20260820 b WHERE b.ANTPCDGO = a.ANTPCDGO)
 WHERE EXISTS (SELECT 1 FROM PGS.ANTP_BK20260820 b WHERE b.ANTPCDGO = a.ANTPCDGO);

-- Repetir el mismo par de UPDATE para CBR.APLC / CBR.ANTC
COMMIT;
```

Después del despliegue el rollback ya no es limpio: habrá cruces nuevos creados
con el modelo nuevo. En ese caso hay que revertir también el WAR.

---

## 10. Después de migrar

- Desplegar el WAR con el modelo nuevo.
- Verificar en la pantalla **Tesorería → Anticipos → Seguimiento** que el cuadre
  aparezca en verde para una muestra de titulares con cruces.
- Hacer un cruce de prueba eligiendo dos anticipos y comprobar que se generan dos
  aplicaciones con sus dos asientos.
- Anular un anticipo cruzado de prueba y comprobar que reversa exactamente sus
  abonos (`ANULACION-ANTICIPOS.md`).
- Las tablas `*_BK20260820` se pueden borrar cuando el cuadre lleve un cierre
  contable estable.
