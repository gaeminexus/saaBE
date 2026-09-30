-- =====================================================================================
-- 301 — CRD.PLVN: terminar lo que el 300 no pudo, resolviendo los choques de numero
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- QUE PASO CON EL 300: al componer los numeros pelados, el "46" viejo quedo igual al
-- ASOPREP-FCPC-CREDITO-GR-046-2026 de una declaracion hecha despues con el WAR nuevo, y el
-- indice unico CRD.UX_PLVN_MEMORANDO rechazo el UPDATE con ORA-00001. El UPDATE fallo ENTERO
-- (Oracle revierte la sentencia completa): ninguna fila cambio. Si quedo creada
-- CRD.BKP_300_PLVN, con los valores pelados de ANTES — este script la usa como respaldo.
--
-- COMO SE RESUELVE UN CHOQUE: el numero de memorando identifica un DOCUMENTO emitido. Si el
-- mismo numero se uso dos veces, la que se conserva limpia es la VIVA (estado 1 DECLARADA o
-- 2 LIQUIDADA). La otra, si es una REVERTIDA (estado 3), queda con el compuesto mas un sufijo
-- que la distingue y dice que es la revertida: ASOPREP-FCPC-CREDITO-GR-046-2026-REV-{PLVNCDGO}.
-- No se borra nada y el documento revertido se puede seguir reimprimiendo.
--
-- ⛔ Si un choque es entre dos declaraciones VIVAS, este script NO decide: el bloque 0.2 lo
--    muestra y hay que PARAR y pasarselo al arbitro. Son dos documentos vigentes con el mismo
--    numero y eso lo resuelve el usuario, no un UPDATE.
--
-- SQL PURO. ⛔ CORRER POR BLOQUES Y LEER CADA SALIDA ANTES DE SEGUIR — el 0.2 del 300 habria
-- mostrado este choque antes del UPDATE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 El respaldo del 300 existe y tiene las filas peladas. Esperado: igual numero de filas
--     que las que todavia estan peladas en PLVN (el UPDATE del 300 no cambio nada).
SELECT (SELECT COUNT(*) FROM CRD.BKP_300_PLVN) AS EN_RESPALDO,
       (SELECT COUNT(*) FROM CRD.PLVN v WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$')) AS PELADOS_HOY
  FROM DUAL;

-- 0.2 ⛔ LOS CHOQUES, con el estado de cada lado. Una fila por pelado que choca.
--     Leer la columna QUE_HACER:
--       'SUFIJO REV'  → la pelada es una REVERTIDA: se le pone el sufijo (bloque 1).
--       'PARAR'       → la pelada esta VIVA: dos documentos vigentes con el mismo numero.
--                       NO seguir. Pasarle al arbitro esta salida.
SELECT p.PLVNCDGO  AS PELADA_ID,  p.PLVNNMPS AS PELADA_PRESTAMO,  p.PLVNESTD AS PELADA_ESTADO,
       p.PLVNNMMM  AS PELADA_NUMERO,
       c.PLVNCDGO  AS OTRA_ID,    c.PLVNNMPS AS OTRA_PRESTAMO,    c.PLVNESTD AS OTRA_ESTADO,
       c.PLVNNMMM  AS OTRA_NUMERO,
       CASE WHEN p.PLVNESTD = 3 THEN 'SUFIJO REV' ELSE 'PARAR' END AS QUE_HACER
  FROM CRD.PLVN p
  JOIN CRD.PLVN c
    ON UPPER(TRIM(c.PLVNNMMM)) = 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(p.PLVNNMMM), 3, '0') || '-'
                                  || TO_CHAR(NVL(p.PLVNFCDC, SYSDATE), 'YYYY')
 WHERE REGEXP_LIKE(TRIM(p.PLVNNMMM), '^[0-9]+$')
 ORDER BY p.PLVNCDGO;

-- 0.3 ⛔ Choques ENTRE DOS PELADAS (el mismo numero pelado dos veces en el mismo anio).
--     Esperado: 0 filas. Si devuelve algo, PARAR y pasarselo al arbitro.
SELECT 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
         || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY') AS COMPUESTO,
       COUNT(*) AS VECES, LISTAGG(v.PLVNCDGO || ':' || v.PLVNESTD, ', ') WITHIN GROUP (ORDER BY v.PLVNCDGO) AS ID_ESTADO
  FROM CRD.PLVN v
 WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$')
 GROUP BY 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
         || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY')
HAVING COUNT(*) > 1;

-- ⇒ SEGUIR SOLO SI: el 0.2 no tiene ninguna fila 'PARAR' y el 0.3 da 0 filas.


-- =====================================================================================
-- 1. LAS REVERTIDAS QUE CHOCAN: compuesto + sufijo -REV-{id}
-- =====================================================================================
UPDATE CRD.PLVN p
   SET p.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(p.PLVNNMMM), 3, '0') || '-'
                    || TO_CHAR(NVL(p.PLVNFCDC, SYSDATE), 'YYYY') || '-REV-' || p.PLVNCDGO
 WHERE REGEXP_LIKE(TRIM(p.PLVNNMMM), '^[0-9]+$')
   AND p.PLVNESTD = 3
   AND EXISTS (SELECT 1 FROM CRD.PLVN c
                WHERE c.PLVNCDGO <> p.PLVNCDGO
                  AND UPPER(TRIM(c.PLVNNMMM)) = 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(p.PLVNNMMM), 3, '0')
                                                || '-' || TO_CHAR(NVL(p.PLVNFCDC, SYSDATE), 'YYYY'));


-- =====================================================================================
-- 2. EL RESTO DE LAS PELADAS: compuesto normal (lo mismo que queria hacer el 300)
-- =====================================================================================
UPDATE CRD.PLVN v
   SET v.PLVNNMMM = 'ASOPREP-FCPC-CREDITO-GR-' || LPAD(TRIM(v.PLVNNMMM), 3, '0') || '-'
                    || TO_CHAR(NVL(v.PLVNFCDC, SYSDATE), 'YYYY')
 WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$');

COMMIT;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Ya no queda ningun numero pelado. Esperado: 0.
SELECT COUNT(*) AS PELADOS FROM CRD.PLVN v WHERE REGEXP_LIKE(TRIM(v.PLVNNMMM), '^[0-9]+$');

-- 3.2 Como quedaron todas. Esperado: ASOPREP-FCPC-CREDITO-GR-nnn-aaaa, y las revertidas que
--     chocaban con el sufijo -REV-{id}.
SELECT v.PLVNCDGO, v.PLVNNMPS AS PRESTAMO, v.PLVNESTD AS ESTADO, v.PLVNNMMM
  FROM CRD.PLVN v
 ORDER BY v.PLVNCDGO;


-- =====================================================================================
-- 4. REVERSO — COMENTADO (vuelve a los valores pelados del respaldo del 300)
-- =====================================================================================
-- UPDATE CRD.PLVN v
--    SET v.PLVNNMMM = (SELECT b.PLVNNMMM FROM CRD.BKP_300_PLVN b WHERE b.PLVNCDGO = v.PLVNCDGO)
--  WHERE v.PLVNCDGO IN (SELECT b.PLVNCDGO FROM CRD.BKP_300_PLVN b);
-- COMMIT;
--
-- Cuando ya no haga falta:  DROP TABLE CRD.BKP_300_PLVN;
