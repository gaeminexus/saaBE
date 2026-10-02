-- =====================================================================================
-- e3-09 — Control de CNT.PRDO antes de desplegar el orden de periodos por fecha (C1)
-- Equipo omen-saa-3 · 2026-10-02 · SOLO LECTURA. Correr en PRODUCCIÓN y en la base del proyecto.
--
-- El WAR nuevo decide qué periodo va antes de otro por PRDOINCO (primer día) y ya no por PRDOCDGO.
-- Eso exige dos cosas en los datos. Si alguna falla, NO desplegar y avisar:
--   (a) ningún periodo con PRDOINCO vacío (un MAX/MIN lo ignoraría y "el último" cambiaría);
--   (b) ningún par de periodos de la misma empresa con el MISMO PRDOINCO (por ejemplo un periodo
--       de cierre que comparta fecha con diciembre): las consultas que buscan "el de mayor
--       PRDOINCO" esperan UNA fila y reventarían con NonUniqueResult.
-- Se esperan CERO filas en los bloques 1 y 2.
-- =====================================================================================

-- BLOQUE 1 — periodos sin primer día, o con primer día que no corresponde a su mes y año
SELECT p.PJRQCDGO, p.PRDOCDGO, p.PRDOANNN, p.PRDOMSSS, p.PRDONMBR, p.PRDOINCO, p.PRDOFNN, p.PRDOCRRE
  FROM CNT.PRDO p
 WHERE p.PRDOINCO IS NULL
    OR EXTRACT(MONTH FROM p.PRDOINCO) <> p.PRDOMSSS
    OR EXTRACT(YEAR  FROM p.PRDOINCO) <> p.PRDOANNN
 ORDER BY p.PJRQCDGO, p.PRDOANNN, p.PRDOMSSS;

-- BLOQUE 2 — dos o más periodos de la misma empresa con el mismo primer día
SELECT p.PJRQCDGO, p.PRDOINCO, COUNT(*) AS PERIODOS,
       LISTAGG(p.PRDOCDGO || ' (' || p.PRDONMBR || ', cierre=' || NVL(p.PRDOCRRE, 0) || ')', ' | ')
         WITHIN GROUP (ORDER BY p.PRDOCDGO) AS CUALES
  FROM CNT.PRDO p
 WHERE p.PRDOINCO IS NOT NULL
 GROUP BY p.PJRQCDGO, p.PRDOINCO
HAVING COUNT(*) > 1
 ORDER BY p.PJRQCDGO, p.PRDOINCO;

-- BLOQUE 3 — informativo: periodos por empresa, el primero y el último por fecha y por código.
-- Si ULTIMO_POR_FECHA <> ULTIMO_POR_CODIGO, el orden ya está desordenado hoy.
SELECT p.PJRQCDGO,
       COUNT(*)                                                            AS PERIODOS,
       MIN(p.PRDOINCO)                                                     AS PRIMERO,
       MAX(p.PRDOINCO)                                                     AS ULTIMO,
       MAX(p.PRDOCDGO) KEEP (DENSE_RANK LAST ORDER BY p.PRDOINCO)          AS ULTIMO_POR_FECHA,
       MAX(p.PRDOCDGO)                                                     AS ULTIMO_POR_CODIGO
  FROM CNT.PRDO p
 GROUP BY p.PJRQCDGO
 ORDER BY p.PJRQCDGO;
