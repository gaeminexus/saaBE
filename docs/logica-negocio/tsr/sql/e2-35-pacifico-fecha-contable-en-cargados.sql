-- =====================================================================
-- e2-35 — Pasar los extractos de Pacifico YA CARGADOS a fecha contable
-- Modulo: TSR  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ⚠️ NO ES SOLO LECTURA. Sobrescribe DEXBFTRN. Correr el BLOQUE 0 primero,
--    LEERLO, y recien despues decidir.
--
-- 🔴 POR QUE EXISTE
--   Decision del usuario: en Banco Pacifico la conciliacion debe ir por la
--   FECHA CONTABLE, no por la fecha real. El parser ya se corrigio, pero eso
--   solo aplica a lo que se cargue de ahora en adelante: las filas ya
--   importadas siguen con la fecha real en DEXBFTRN, que es el campo que TODO
--   el motor de conciliacion usa (match por cercania de fechas, rango del
--   grupo, partidas en transito y la validacion de periodo al importar).
--
--   La fecha contable de esas filas SI esta guardada, en DEXBFCNT: Pacifico es
--   el unico de los once parsers que la puebla. Por eso se puede corregir.
--
-- 🔴 ESTE SCRIPT PIERDE INFORMACION SI NO SE HACE BIEN
--   DEXBFTRN se sobrescribe con DEXBFCNT, y la fecha real NO tiene otra columna
--   donde vivir: se perderia. Por eso el BLOQUE 1 primero la anexa a la
--   descripcion -exactamente como hace el parser nuevo con las filas que
--   importe de aca en adelante-, y solo despues sobrescribe la fecha. Asi las
--   filas viejas y las nuevas quedan iguales.
--
-- ⛔ ESTO NO ES OBLIGATORIO. Alternativa perfectamente valida: no correrlo y
--   que el cambio rija de septiembre en adelante. Agosto quedaria conciliado
--   con el criterio viejo, que es coherente consigo mismo. Lo decide el
--   usuario, y el BLOQUE 0 le da los numeros para decidir.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES. Correr esto solo, LEER, y pegar el resultado.
-- =====================================================================

-- 0.1 Cuantas filas de Pacifico hay y cuantas cambiarian de fecha.
--     Si "cambian" es 0, no hay nada que hacer: no correr el resto.
SELECT '0.1 - alcance' AS control,
       COUNT(*)                                                        AS filas_pacifico,
       SUM(CASE WHEN d.DEXBFCNT IS NULL THEN 1 ELSE 0 END)             AS sin_fecha_contable,
       SUM(CASE WHEN d.DEXBFCNT IS NOT NULL
                 AND d.DEXBFCNT <> d.DEXBFTRN THEN 1 ELSE 0 END)       AS cambian,
       MIN(d.DEXBFTRN) AS primera_fecha, MAX(d.DEXBFTRN) AS ultima_fecha
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1;

-- 0.2 🔴 EL CONTROL QUE DECIDE: filas donde la fecha contable cae en un MES
--     DISTINTO al de la fecha real. Esas quedarian con un PRDOCDGO que ya no
--     corresponde al rango de su fecha nueva.
--
--     Nada se rompe automaticamente -ninguna consulta recalcula el periodo a
--     partir de la fecha; todas filtran por el PRDOCDGO guardado-, pero la fila
--     va a verse rara en pantalla: fecha de un mes, periodo de otro. Si esto
--     devuelve filas, PARAR Y AVISAR antes de correr el BLOQUE 1: hay que
--     decidir caso por caso si esa fila debe cambiar de periodo, y eso NO lo
--     hace este script.
--     ESPERADO: 0 filas.
SELECT '0.2 - cruzan de mes' AS control,
       d.DEXBCDGO, b.BNCONMBR AS banco, c.CNBCNMRO AS cuenta,
       d.DEXBFTRN AS fecha_real, d.DEXBFCNT AS fecha_contable,
       p.PRDONMBR AS periodo_asignado, p.PRDOINCO AS periodo_desde, p.PRDOFNN AS periodo_hasta,
       d.DEXBDBTO AS debito, d.DEXBCRDT AS credito, d.DEXBDSCR AS descripcion
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = d.PRDOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1
   AND d.DEXBFCNT IS NOT NULL
   AND TRUNC(d.DEXBFCNT, 'MM') <> TRUNC(d.DEXBFTRN, 'MM')
 ORDER BY d.DEXBFTRN;

-- 0.3 Cuantas de las que cambian ya estan conciliadas o declaradas en transito.
--     No impide correr el script -el anclaje de grupos y partidas es por FK a
--     la fila, no por fecha, verificado 2026-09-08-, pero conviene saber cuanto
--     de lo ya trabajado se ve afectado.
SELECT '0.3 - ya trabajadas' AS control,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.GCEX x JOIN TSR.GRCC g ON g.GRCCCDGO = x.GRCCCDGO
                              WHERE x.DEXBCDGO = d.DEXBCDGO AND g.GRCCESTD = 1)
                THEN 1 ELSE 0 END)                                     AS ya_conciliadas,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM TSR.DTCN t
                              WHERE t.DTCNIDEX = d.DEXBCDGO AND t.DTCNESTD = 1)
                THEN 1 ELSE 0 END)                                     AS declaradas_en_transito
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1
   AND d.DEXBFCNT IS NOT NULL
   AND d.DEXBFCNT <> d.DEXBFTRN;

-- 0.4 Que no haya ya un mes de conciliacion CERRADO que contenga estas filas.
--     ESPERADO: 0. Si devuelve algo, PARAR: cambiar fechas por debajo de un mes
--     cerrado altera un cierre que ya se dio por bueno y firmado.
SELECT '0.4 - meses cerrados afectados' AS control,
       p.PRDONMBR AS periodo, ct.CTEBUSCR AS cerrado_por, ct.CTEBFCCR AS cerrado_el,
       COUNT(*) AS filas_afectadas
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = d.PRDOCDGO
  JOIN TSR.CTEB ct ON ct.PJRQCDGO = p.PJRQCDGO AND ct.PRDOCDGO = p.PRDOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1
   AND d.DEXBFCNT IS NOT NULL
   AND d.DEXBFCNT <> d.DEXBFTRN
   AND ct.CTEBCRRE = 1
 GROUP BY p.PRDONMBR, ct.CTEBUSCR, ct.CTEBFCCR;


-- =====================================================================
-- BLOQUE 1 — CORREGIR. ⛔ CONDICIONES OBLIGATORIAS
-- =====================================================================
--   ✅ El 0.1 devolvio "cambian" > 0
--   ✅ El 0.2 devolvio 0 filas (ninguna cruza de mes)
--   ✅ El 0.4 devolvio 0 filas (ningun mes cerrado afectado)
--   ❌ Si alguna no se cumple, NO CORRER. Avisar con el resultado.

-- 1.1 PRESERVAR LA FECHA REAL EN LA DESCRIPCION, ANTES de sobrescribirla.
--     Es lo mismo que hace el parser nuevo con las filas que importe de aca en
--     adelante, asi que viejas y nuevas quedan iguales.
--     DEXBDSCR es VARCHAR2(500) -verificado en la entidad-: se corta a 500 por
--     si alguna descripcion ya venia larga.
--     El "NOT LIKE '%Fecha real:%'" hace la sentencia REPETIBLE: correrla dos
--     veces no duplica el texto.
UPDATE TSR.DEXB d
   SET d.DEXBDSCR = SUBSTR(
           NVL(d.DEXBDSCR, '') || ' | Fecha real: ' || TO_CHAR(d.DEXBFTRN, 'DD/MM/YYYY'),
           1, 500)
 WHERE d.DEXBESTD = 1
   AND d.DEXBFCNT IS NOT NULL
   AND d.DEXBFCNT <> d.DEXBFTRN
   AND NVL(d.DEXBDSCR, '') NOT LIKE '%Fecha real:%'
   AND d.CNBCCDGO IN (SELECT c.CNBCCDGO FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
                       WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%');

-- 1.2 Y RECIEN AHORA la fecha de conciliacion pasa a ser la contable.
UPDATE TSR.DEXB d
   SET d.DEXBFTRN = d.DEXBFCNT
 WHERE d.DEXBESTD = 1
   AND d.DEXBFCNT IS NOT NULL
   AND d.DEXBFCNT <> d.DEXBFTRN
   AND d.CNBCCDGO IN (SELECT c.CNBCCDGO FROM TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
                       WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%');

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROLES DESPUES. Correrlos SIEMPRE.
-- =====================================================================

-- 2.1 ESPERADO: cambian = 0 (ya no queda ninguna con las dos fechas distintas).
SELECT '2.1 - ya no quedan diferencias' AS control,
       COUNT(*) AS filas_pacifico,
       SUM(CASE WHEN d.DEXBFCNT IS NOT NULL
                 AND d.DEXBFCNT <> d.DEXBFTRN THEN 1 ELSE 0 END) AS cambian_debe_ser_cero,
       SUM(CASE WHEN d.DEXBDSCR LIKE '%Fecha real:%' THEN 1 ELSE 0 END) AS con_fecha_real_preservada
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1;

-- 2.2 Que ninguna fila haya quedado con la fecha fuera del rango de su periodo.
--     ESPERADO: 0. Es la contracara del 0.2, medida despues del cambio.
SELECT '2.2 - fecha fuera del periodo asignado' AS control,
       d.DEXBCDGO, d.DEXBFTRN AS fecha, p.PRDONMBR AS periodo,
       p.PRDOINCO AS desde, p.PRDOFNN AS hasta
  FROM TSR.DEXB d
  JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
  JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  JOIN CNT.PRDO p ON p.PRDOCDGO = d.PRDOCDGO
 WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%'
   AND d.DEXBESTD = 1
   AND (d.DEXBFTRN < p.PRDOINCO OR d.DEXBFTRN > p.PRDOFNN);


-- =====================================================================
-- BLOQUE 3 — OPCIONAL: refrescar el rango de fechas de los grupos.
-- =====================================================================
-- GRCCFCMN/GRCCFCMX se escriben al conciliar y NUNCA se leen en el backend
-- (verificado 2026-09-08): son informativos. Pero el reporte RPRT_CNCL_CNTA los
-- imprime como "rango del grupo", asi que sin esto un grupo tocado por el
-- BLOQUE 1 va a mostrar el rango viejo. Es cosmetico; no correrlo no rompe nada.
UPDATE TSR.GRCC g
   SET (g.GRCCFCMN, g.GRCCFCMX) = (
        SELECT MIN(d.DEXBFTRN), MAX(d.DEXBFTRN)
          FROM TSR.GCEX x JOIN TSR.DEXB d ON d.DEXBCDGO = x.DEXBCDGO
         WHERE x.GRCCCDGO = g.GRCCCDGO)
 WHERE g.GRCCESTD = 1
   AND EXISTS (SELECT 1
                 FROM TSR.GCEX x
                 JOIN TSR.DEXB d ON d.DEXBCDGO = x.DEXBCDGO
                 JOIN TSR.CNBC c ON c.CNBCCDGO = d.CNBCCDGO
                 JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
                WHERE x.GRCCCDGO = g.GRCCCDGO
                  AND UPPER(b.BNCONMBR) LIKE '%PACIFICO%');

COMMIT;


-- =====================================================================
-- ⚠️ NO HAY BLOQUE DE REVERSO, Y ESTA DICHO A PROPOSITO
-- =====================================================================
-- Despues del 1.2 la fecha real ya no esta en ninguna columna: quedo solo como
-- texto dentro de DEXBDSCR ("| Fecha real: DD/MM/YYYY"). Se puede reconstruir
-- leyendo ese texto, pero no es un UPDATE trivial ni seguro de escribir a
-- ciegas. Si hace falta revertir, avisar y se escribe con el caso concreto a la
-- vista.
--
-- La forma barata de tener reverso: sacar un respaldo ANTES de correr el
-- BLOQUE 1, por ejemplo
--   CREATE TABLE TSR.DEXB_BKP_20260908 AS
--   SELECT DEXBCDGO, DEXBFTRN, DEXBFCNT, DEXBDSCR FROM TSR.DEXB
--    WHERE CNBCCDGO IN (SELECT c.CNBCCDGO FROM TSR.CNBC c JOIN TSR.BNCO b
--                        ON b.BNCOCDGO = c.BNCOCDGO WHERE UPPER(b.BNCONMBR) LIKE '%PACIFICO%');
-- Queda a criterio del usuario; en una base de produccion, recomendado.
-- =====================================================================
