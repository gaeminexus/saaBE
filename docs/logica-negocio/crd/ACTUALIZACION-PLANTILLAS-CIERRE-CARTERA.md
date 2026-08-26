# Actualización de plantillas contables para el cierre de cartera (CRD)

**Fecha:** 2026-08-25 · **Fase:** 2 (proceso mensual de apertura / cierre)
**Estado de ejecución:** ✅ BD LOCAL de desarrollo (docker `saa-oracle-23ai`) — 2026-08-25 · ✅ PRUEBAS y ✅ PRODUCCIÓN — 2026-08-25 (ejecutados por el usuario)
**Ejecución:** MANUAL, revisando los SELECT de control de cada paso. **No lo ejecuta el agente en pruebas ni en producción.**
**Versión ejecutable:** `sql/ACTUALIZACION-PLANTILLAS-CIERRE-CARTERA.sql` — el mismo contenido en un solo archivo SQL, en orden, con los controles y el `COMMIT` al final, y el bloque de deshacer comentado. Este documento manda: aquí está el porqué de cada cambio y el resultado esperado de cada control. **Todo el guion es idempotente** (verificado el 2026-08-25 corriéndolo dos veces contra la BD local): se puede repetir sin duplicar nada.
**Diseño:** `LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md` §3.2 y §8.3 · Catálogo: `com.saa.rubros.CrdLineaAsiento`

> Cliente: el plugin JDBC de VS Code. Este guion es **SQL puro** — sin `SET`, `DEFINE` ni
> `WHENEVER`, que ese cliente rechaza con `ORA-00900`.

---

## 0. El problema que esto resuelve

Los `DTPLAXL1` de las plantillas de CRD son hoy **posicionales**: 1, 2, 3… por orden de
captura. El `1` de la plantilla del neteo (alterno 33) es `2.3.02.05`, y el `1` de la
plantilla de la apertura (alterno 1) es `1.4.05.05`. Un servicio que pida "la línea 1"
obtiene una cuenta distinta según la plantilla — y el error es **silencioso**: el asiento
cuadra igual, con las cuentas cambiadas de lado.

Este documento fija esos auxiliares al catálogo semántico `CRD_LINEA_ASIENTO`
(`com.saa.rubros.CrdLineaAsiento`), donde un código significa lo mismo en todas las
plantillas:

| Código | Papel | Cuenta típica |
|---|---|---|
| 1 | `APORTES_POR_COBRAR` | `1.4.05.05` |
| 2 | `PRESTAMOS_POR_COBRAR` | `1.4.05.10` |
| 3 | `APORTES_POR_APLICAR` | `2.3.02.05` |
| 4 | `PRESTAMOS_POR_APLICAR` | `2.3.02.10` |
| 10 | `INTERES_ORDINARIO_POR_COBRAR` | `1.4.02.xx` — **dimensión en `DTPLAXL2`** |
| 20 | `INTERES_MORA_POR_COBRAR` | `1.4.02.xx` — misma cuenta que el ordinario (D3) |
| 30 | `INGRESO_INTERES_ORDINARIO` | `5.1.02.xx` |
| 40 | `INGRESO_INTERES_MORA` | `5.1.02.xx` — misma cuenta que el ordinario |

**`DTPLAXL2` = `CRD.TPPR.TPPRCDGO`** en las líneas de interés, porque la cuenta cambia por
familia de producto: `1` QUIROGRAFARIO → `1.4.02.05` / `5.1.02.05`; `2` HIPOTECARIO →
`.15`; `3` PRENDARIO → `.10`. Las demás líneas dejan `DTPLAXL2` en `0`, como está hoy.

**Las cuentas de banda no se tocan.** El capital por banda sale de `CRD.BNDP` (Fase 1);
estas plantillas solo cubren las líneas que NO son de banda.

---

## 1. Controles PREVIOS (no continuar si algo no cuadra)

### 1.1 Las tres plantillas existen y están activas

```sql
SELECT PLNSCDGO, PLNSCDAL, PLNSNMBR, PLNSESTD, PJRQCDGO
FROM CNT.PLNS WHERE PJRQCDGO = 1236 AND PLNSCDAL IN (1, 17, 33) ORDER BY PLNSCDAL;
```

Esperado (BD local): `1041 / 1 / CRD RG PLANILLA MENSUAL CBRO PARTICIPES`,
`1103 / 17 / CRD REGISTRO DEVENGADO DE INTERES A INGRESOS`,
`1119 / 33 / CRD NETEO DE PLANILLAS`, las tres con `PLNSESTD = 1`.
**En producción los `PLNSCDGO` serán otros**: los pasos siguientes resuelven la plantilla
por `(PLNSCDAL, PJRQCDGO)` y nunca por el id, precisamente por eso.

### 1.2 Estado actual de los auxiliares

```sql
SELECT s.PLNSCDAL alterno, d.DTPLCDGO, d.DTPLAXL1 aux1, d.DTPLAXL2 aux2,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') movimiento,
       c.PLNNCNTA cuenta, d.DTPLDSCR, d.DTPLESTD
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33)
ORDER BY s.PLNSCDAL, d.DTPLAXL1;
```

Esperado antes de los cambios: 4 líneas en el alterno 1, 9 en el 17 y 4 en el 33; todas con
`aux1` posicional 1..N y `aux2 = 0`.

### 1.3 Tipos de préstamo

```sql
SELECT TPPRCDGO, TPPRNMBR FROM CRD.TPPR ORDER BY TPPRCDGO;
```

Esperado: `1 QUIROGRAFARIO`, `2 HIPOTECARIO`, `3 PRENDARIO`, y los tipos `4` y `5`
(prendarios duplicados) **sin ningún producto asociado** — comprobar:

```sql
SELECT TPPRCDGO, COUNT(*) productos FROM CRD.PRDC GROUP BY TPPRCDGO ORDER BY 1;
```
Esperado: solo 1, 2 y 3. Si en producción hubiera productos en los tipos 4 ó 5, **hay que
agregarles sus líneas** en el paso 2.3 o el devengo fallará al llegar a ellos.

---

## 2. Cambios

### 2.1 Alterno 1 — apertura de la planilla mensual: SIN CAMBIOS

Los auxiliares posicionales de esta plantilla ya coinciden con el catálogo semántico:
`1` es `1.4.05.05` (aportes por cobrar), `2` es `1.4.05.10` (préstamos por cobrar), `3` es
`2.3.02.05` (aportes por aplicar) y `4` es `2.3.02.10` (préstamos por aplicar). **No se
ejecuta nada aquí.** Se comprueba con el control 3.1.

### 2.2 Alterno 33 — neteo de planillas: intercambiar 1↔3 y 2↔4

Aquí sí hay que renumerar: la plantilla del neteo tiene los mismos cuatro papeles pero en
el lado contrario del asiento, y hoy los numera por posición.

| `DTPLCDGO` (local) | Cuenta | Movimiento | `aux1` hoy | `aux1` nuevo | Papel |
|---|---|---|---|---|---|
| 1635 | `2.3.02.05` | DEBE | 1 | **3** | `APORTES_POR_APLICAR` |
| 1636 | `2.3.02.10` | DEBE | 2 | **4** | `PRESTAMOS_POR_APLICAR` |
| 1637 | `1.4.05.05` | HABER | 3 | **1** | `APORTES_POR_COBRAR` |
| 1638 | `1.4.05.10` | HABER | 4 | **2** | `PRESTAMOS_POR_COBRAR` |

El `UPDATE` no usa los ids: se ancla en la CUENTA, que es lo estable entre instalaciones.

```sql
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                   WHEN '2.3.02.05' THEN 3
                   WHEN '2.3.02.10' THEN 4
                   WHEN '1.4.05.05' THEN 1
                   WHEN '1.4.05.10' THEN 2
                   ELSE d.DTPLAXL1 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 33 AND s.PJRQCDGO = 1236);
```
Esperado: **4 filas actualizadas**.

### 2.3 Alterno 17 — devengo de intereses: papel + tipo de préstamo

Se fijan `aux1` (papel) y `aux2` (tipo de préstamo) de las nueve líneas existentes.

| Cuenta | Movimiento | Descripción | `aux1` | `aux2` | Papel |
|---|---|---|---|---|---|
| `1.4.02.05` | DEBE | INTERESES DE CUOTA | **10** | **1** | ordinario, quirografario |
| `1.4.02.10` | DEBE | INTERESES DE CUOTA | **10** | **3** | ordinario, prendario |
| `1.4.02.15` | DEBE | INTERESES DE CUOTA | **10** | **2** | ordinario, hipotecario |
| `1.4.02.05` | DEBE | INTERESES POR MORA | **20** | **1** | mora, quirografario |
| `1.4.02.10` | DEBE | *(errata, ver 2.4)* | **20** | **3** | mora, prendario |
| `1.4.02.15` | DEBE | INTERESES POR MORA | **20** | **2** | mora, hipotecario |
| `5.1.02.05` | HABER | QUIROGRAFARIOS | **30** | **1** | ingreso ordinario, quirografario |
| `5.1.02.10` | HABER | PRENDARIOS | **30** | **3** | ingreso ordinario, prendario |
| `5.1.02.15` | HABER | HIPOTECARIOS | **30** | **2** | ingreso ordinario, hipotecario |

Se ancla en el `aux1` posicional actual (1..9), que es el único discriminante fiable
mientras las cuentas se repiten entre el bloque de ordinario y el de mora:

```sql
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE d.DTPLAXL1
                   WHEN 1 THEN 10 WHEN 2 THEN 10 WHEN 3 THEN 10
                   WHEN 4 THEN 20 WHEN 5 THEN 20 WHEN 6 THEN 20
                   WHEN 7 THEN 30 WHEN 8 THEN 30 WHEN 9 THEN 30
                   ELSE d.DTPLAXL1 END,
    d.DTPLAXL2 = CASE d.DTPLAXL1
                   WHEN 1 THEN 1 WHEN 2 THEN 3 WHEN 3 THEN 2
                   WHEN 4 THEN 1 WHEN 5 THEN 3 WHEN 6 THEN 2
                   WHEN 7 THEN 1 WHEN 8 THEN 3 WHEN 9 THEN 2
                   ELSE d.DTPLAXL2 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 BETWEEN 1 AND 9;
```
Esperado: **9 filas actualizadas**.

> ✅ **Es idempotente. Verificado el 2026-08-25 ejecutándolo dos veces contra la BD local:
> la segunda corrida devuelve `0 rows updated`.** La razón es la última línea del `WHERE`
> (`AND d.DTPLAXL1 BETWEEN 1 AND 9`): tras la primera corrida las nueve filas quedan con
> `aux1` en 10/20/30 y dejan de entrar en el filtro, así que ni siquiera se evalúa el
> `CASE` de `DTPLAXL2`. Una versión anterior de este documento advertía lo contrario;
> era una falsa alarma. Aun así, el control 3.2 es la forma de saber si ya se aplicó.

### 2.4 Alterno 17 — corregir la errata de descripción

La línea de `1.4.02.10` del bloque de mora dice "INTERESES POR PRESTAMOS PRENDARIOS"
cuando debería decir "INTERESES POR MORA" (defecto listado en §8.3.2 del levantamiento).
La descripción no es cosmética: es la que distingue mora de ordinario en el mayor, porque
las dos comparten cuenta (decisión D3 de §9.1).

```sql
UPDATE CNT.DTPL d
SET d.DTPLDSCR = 'INTERESES POR MORA'
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 20 AND d.DTPLAXL2 = 3;
```
Esperado: **1 fila actualizada**.

### 2.5 Alterno 17 — crear las tres líneas de INGRESO POR MORA que faltan

La plantilla tiene tres líneas al DEBE por mora pero ninguna al HABER: hoy el ingreso por
mora comparte línea con el ordinario. Con cuenta compartida y descripciones distintas
(D3), hacen falta las tres líneas propias.

```sql
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
                      DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, m.plns, m.plnn, 'INGRESO POR INTERES DE MORA', 2,
       40, m.tipo, 0, 0, 0, 1
FROM (
  SELECT s.PLNSCDGO plns, c.PLNNCDGO plnn, 1 tipo
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.05' AND c.PJRQCDGO = 1236
  UNION ALL
  SELECT s.PLNSCDGO, c.PLNNCDGO, 3
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.10' AND c.PJRQCDGO = 1236
  UNION ALL
  SELECT s.PLNSCDGO, c.PLNNCDGO, 2
  FROM CNT.PLNS s, CNT.PLNN c
  WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236 AND c.PLNNCNTA = '5.1.02.15' AND c.PJRQCDGO = 1236
) m
WHERE NOT EXISTS (
  SELECT 1 FROM CNT.DTPL x
  WHERE x.PLNSCDGO = m.plns AND x.DTPLAXL1 = 40 AND x.DTPLAXL2 = m.tipo
);
```
Esperado: **3 filas insertadas** la primera vez, **0** si ya se aplicó.

> La cláusula `NOT EXISTS` se añadió en la revisión del 2026-08-25. Sin ella este `INSERT`
> era el único paso no idempotente del documento: repetirlo duplicaba las tres líneas del
> papel 40, y eso solo se detectaba en el control 3.3. Verificado ejecutando el guion
> completo por segunda vez contra la BD local: `0 rows created`.

---

## 3. Controles POSTERIORES (antes del COMMIT)

### 3.1 Los cuatro papeles de apertura y neteo, cada uno con su cuenta

```sql
SELECT s.PLNSCDAL alterno, d.DTPLAXL1 papel,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') mov, c.PLNNCNTA cuenta
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 33) AND d.DTPLESTD = 1
ORDER BY s.PLNSCDAL, d.DTPLAXL1;
```

Esperado, exactamente 8 filas:

| alterno | papel | mov | cuenta |
|---|---|---|---|
| 1 | 1 | DEBE | 1.4.05.05 |
| 1 | 2 | DEBE | 1.4.05.10 |
| 1 | 3 | HABER | 2.3.02.05 |
| 1 | 4 | HABER | 2.3.02.10 |
| 33 | 1 | HABER | 1.4.05.05 |
| 33 | 2 | HABER | 1.4.05.10 |
| 33 | 3 | DEBE | 2.3.02.05 |
| 33 | 4 | DEBE | 2.3.02.10 |

**El mismo papel, la misma cuenta, el lado contrario.** Eso es lo que se buscaba.

### 3.2 Las doce líneas del devengo, sin huecos ni duplicados

```sql
SELECT d.DTPLAXL1 papel, d.DTPLAXL2 tipo_prestamo,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER') mov, c.PLNNCNTA cuenta, d.DTPLDSCR
FROM CNT.DTPL d
JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL = 17 AND d.DTPLESTD = 1
ORDER BY d.DTPLAXL1, d.DTPLAXL2;
```
Esperado: **12 filas** — los papeles 10, 20, 30 y 40, cada uno con los tipos 1, 2 y 3.
Los papeles 10 y 20 al DEBE sobre `1.4.02.xx`; los 30 y 40 al HABER sobre `5.1.02.xx`.

### 3.3 Ninguna combinación repetida

```sql
SELECT s.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2, COUNT(*) veces
FROM CNT.DTPL d JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33) AND d.DTPLESTD = 1
GROUP BY s.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2
HAVING COUNT(*) > 1;
```
Esperado: **0 filas**. Si sale alguna, el servicio tomaría la primera por código y el
asiento saldría con una cuenta arbitraria.

### 3.4 Ninguna línea sin cuenta

```sql
SELECT s.PLNSCDAL, d.DTPLCDGO, d.DTPLAXL1
FROM CNT.DTPL d JOIN CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE s.PJRQCDGO = 1236 AND s.PLNSCDAL IN (1, 17, 33)
  AND d.DTPLESTD = 1 AND d.PLNNCDGO IS NULL;
```
Esperado: **0 filas**.

Si los cuatro controles pasan: `COMMIT;`

---

## 4. Lo que este documento NO hace

- **No toca las líneas de banda de las otras plantillas** (alternos 2–16, 21, 25, 32). El
  saneamiento completo de §8.3 —retirar de las plantillas las cuentas `1.3.xx` que ahora
  salen de `CRD.BNDP`— es Fase 4. Mientras tanto conviven los dos esquemas, pero **el
  cierre de cartera no lee esas plantillas**, así que no hay riesgo de doble asiento por
  este proceso.
- **No crea las plantillas que faltan** para los procesos de pizarra sin plantilla (asiento
  de vencidos, cambio de bandas, devengo diario de mora, entrega de préstamo
  quirografario). Los sub-procesos ①, ② y ①.1 del cierre **no necesitan plantilla**: todas
  sus cuentas salen de `CRD.BNDP` y la glosa la arma el servicio. Los demás son Fase 3 o 4.
- **No corrige las demás erratas de §8.3.2** (alterno 3 aux 7/8 duplicados, alterno 21 aux
  36/37, alterno 15, alterno 14): ninguna afecta a las tres plantillas de este proceso.

---

## 5. Si hay que deshacerlo

```sql
UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE (SELECT c.PLNNCNTA FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                   WHEN '2.3.02.05' THEN 1 WHEN '2.3.02.10' THEN 2
                   WHEN '1.4.05.05' THEN 3 WHEN '1.4.05.10' THEN 4
                   ELSE d.DTPLAXL1 END
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 33 AND s.PJRQCDGO = 1236);

DELETE FROM CNT.DTPL d
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 = 40;

UPDATE CNT.DTPL d
SET d.DTPLAXL1 = CASE d.DTPLAXL1 * 10 + d.DTPLAXL2
                   WHEN 101 THEN 1 WHEN 103 THEN 2 WHEN 102 THEN 3
                   WHEN 201 THEN 4 WHEN 203 THEN 5 WHEN 202 THEN 6
                   WHEN 301 THEN 7 WHEN 303 THEN 8 WHEN 302 THEN 9
                   ELSE d.DTPLAXL1 END,
    d.DTPLAXL2 = 0
WHERE d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
                    WHERE s.PLNSCDAL = 17 AND s.PJRQCDGO = 1236)
  AND d.DTPLAXL1 IN (10, 20, 30);
```

El `DELETE` va **antes** del último `UPDATE`: si se invierte el orden, las filas del papel
40 ya no se distinguen y el borrado se lleva lo que no debe.
