-- =====================================================================================================================================
-- Lote 12 (VCN #6) - PASE: el contador de notas de crédito (CBR.NXPE, tipo 04) quedó atrasado respecto de lo emitido.
-- Medido en producción con omen1-01 (bloque 6b): contador menor que el máximo emitido => la próxima NC repetiría un número ya usado.
-- CORRER ANTES de que alguien emita una nota de crédito. NO hace COMMIT: el usuario revisa los controles y confirma a mano.
-- Solo se toca CBR.NXPE (una fila); no se toca ningún comprobante.
-- =====================================================================================================================================
SET SERVEROUTPUT ON;

-- 0. Estado de partida: la fila del contador y el máximo emitido por ambiente (el contador no distingue ambiente: pruebas y producción comparten fila).
SELECT n.ID, n.PTOEMISION, n.TIPODOC, n.NUMACTUAL FROM CBR.NXPE n WHERE n.TIPODOC = '04' ORDER BY n.PTOEMISION;
SELECT PTOEMISION, AMBIENTE, COUNT(*) AS emitidas, MAX(TO_NUMBER(SECUENCIAL)) AS maximo
  FROM CBR.NTCR WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$') GROUP BY PTOEMISION, AMBIENTE ORDER BY PTOEMISION, AMBIENTE;

-- 1. Respaldo de las filas que se van a tocar (tabla nueva; si ya existe, el CREATE falla y se detiene: no se pisa un respaldo anterior).
CREATE TABLE CBR.NXPE_RESP_OMEN1_02 AS SELECT n.*, SYSTIMESTAMP AS RESPALDADO_EN FROM CBR.NXPE n WHERE n.TIPODOC = '04';

-- 2. Corrección con controles: por cada punto de emisión con contador atrasado, NUMACTUAL = máximo emitido (cualquier ambiente) + 1.
--    Máximo de cualquier ambiente a propósito: un hueco es inofensivo, un número repetido no.
DECLARE
    v_max   NUMBER;
    v_filas NUMBER := 0;
BEGIN
    FOR r IN (SELECT n.ID, n.PTOEMISION, n.NUMACTUAL FROM CBR.NXPE n WHERE n.TIPODOC = '04' FOR UPDATE) LOOP
        SELECT NVL(MAX(TO_NUMBER(SECUENCIAL)), 0) INTO v_max FROM CBR.NTCR
         WHERE PTOEMISION = r.PTOEMISION AND REGEXP_LIKE(SECUENCIAL, '^[0-9]+$');
        IF r.NUMACTUAL < v_max + 1 THEN
            UPDATE CBR.NXPE SET NUMACTUAL = v_max + 1 WHERE ID = r.ID;
            v_filas := v_filas + 1;
            DBMS_OUTPUT.PUT_LINE('Punto ' || r.PTOEMISION || ': contador ' || r.NUMACTUAL || ' -> ' || (v_max + 1));
        ELSE
            DBMS_OUTPUT.PUT_LINE('Punto ' || r.PTOEMISION || ': contador ' || r.NUMACTUAL || ' ya es >= ' || (v_max + 1) || ' (sin cambio)');
        END IF;
    END LOOP;
    -- Control: no se debe haber tocado más de un punto con el contador atrasado (la medición dio uno).
    IF v_filas > 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Se modificaron ' || v_filas || ' filas y se esperaba 1: haga ROLLBACK y revise.');
    END IF;
END;
/

-- 3. Verificación (debe dar 0 filas atrasadas) y revisión antes de confirmar.
SELECT n.PTOEMISION, n.NUMACTUAL AS contador, m.maximo, CASE WHEN n.NUMACTUAL >= m.maximo + 1 THEN 'OK' ELSE 'ATRASADO' END AS estado
  FROM CBR.NXPE n
  LEFT JOIN (SELECT PTOEMISION, MAX(TO_NUMBER(SECUENCIAL)) AS maximo FROM CBR.NTCR WHERE REGEXP_LIKE(SECUENCIAL, '^[0-9]+$') GROUP BY PTOEMISION) m
    ON m.PTOEMISION = n.PTOEMISION
 WHERE n.TIPODOC = '04';

-- Si todo está bien:  COMMIT;     Si algo no cuadra:  ROLLBACK;
-- Para deshacer después de confirmar: UPDATE CBR.NXPE n SET NUMACTUAL = (SELECT r.NUMACTUAL FROM CBR.NXPE_RESP_OMEN1_02 r WHERE r.ID = n.ID)
--                                      WHERE n.ID IN (SELECT ID FROM CBR.NXPE_RESP_OMEN1_02);
