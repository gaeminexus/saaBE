-- =====================================================================================
-- 325 - DDL de la ANULACION DEL PRESTAMO OTORGADO
--       Columnas nuevas en CRD.PRST + estado 12 ANULADO del prestamo + estado 9 ANULADA
--       de la cuota, en el catalogo de rubros.
-- FECHA: 2026-10-13 · EQUIPO: omen-saa-1 (crd)
-- Diseno: docs/logica-negocio/crd/DISENO-ANULACION-PRESTAMO-E-IMPRESOS.md (§5, AN7, AN9)
--
-- ⛔ ESTE SCRIPT SI ESCRIBE. ⛔ VA ANTES DEL WAR: el WAR nuevo mapea las 5 columnas y, sin
--    ellas, TODA lectura de CRD.PRST revienta con ORA-00904 (no solo la anulacion).
-- Sin comandos de SQL*Plus. Correr bloque por bloque, en orden, leyendo cada control.
-- =====================================================================================


-- ==========================================================================
-- BLOQUE 0.A - (!) Las columnas nuevas NO existen todavia
-- ==========================================================================
SELECT COLUMN_NAME
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'PRST'
   AND COLUMN_NAME IN ('PRSTASEN','PRSTASRV','PRSTUSAN','PRSTFCAN','PRSTMTAN');
-- (!) Se esperan CERO filas. Si salen todas, el bloque 1 ya se corrio: saltarlo.
--     Si salen algunas y otras no: PARAR y avisar.


-- ==========================================================================
-- BLOQUE 0.B - (!) Los dos rubros: estado del prestamo y estado de la cuota
-- ==========================================================================
-- El codigo guarda en PRSTIDST / DTPRESTD el valor ALTERNO del detalle (1..11 y 1..8).
-- Se identifican por su contenido, no por un id que nadie tiene anotado:
--   prestamo -> el rubro con un detalle alterno 7 'RECHAZADO' y uno alterno 11 'EN MORA'
--   cuota    -> el rubro con un detalle alterno 7 'CANCELADA ANTICIPADA' y uno alterno 8 'VENCIDA'
SELECT r.PRBRCDGO, r.PRBRDSCR, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PRBR r
  JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRCDGO IN (
         SELECT x.PRBRCDGO FROM SCP.PDTR x
          WHERE (x.PDTRALTR = 7 AND UPPER(x.PDTRDSCR) LIKE '%RECHAZ%')
             OR (x.PDTRALTR = 7 AND UPPER(x.PDTRDSCR) LIKE '%CANCELAD%ANTICIPAD%'))
 ORDER BY r.PRBRCDGO, d.PDTRALTR;
-- (!) Tienen que verse DOS rubros:
--     - uno con alternos 1..11 (GENERADO ... EN MORA)  -> estado del prestamo
--     - uno con alternos 1..8  (PENDIENTE ... VENCIDA) -> estado de la cuota
--     Ninguno de los dos debe tener ya un alterno 12 / 9.
-- (!) Si sale un solo rubro, mas de dos, o los alternos no calzan: PARAR y mandar la salida
--     al arbitro. Los bloques 2 y 3 se detienen solos en ese caso, pero mejor no llegar ahi.


-- ==========================================================================
-- BLOQUE 0.C - (!) Codigos libres en el bloque de omen-saa-1 (PDTR 1300-1399)
-- ==========================================================================
-- CORREGIDO 2026-10-13: SCP.SQ_PDTRCDGO NO EXISTE en la base (ORA-02289 al correr la primera
-- version). Como los scripts 70/81/83/87/97, el PK se da explicito, del bloque reservado al
-- equipo en REGISTRO-RESERVAS-EQUIPOS.md §2 (CRD EQUIPO B = omen-saa-1: 1300-1399).
SELECT NVL(MAX(PDTRCDGO), 1299) AS ULTIMO_USADO_DEL_BLOQUE,
       NVL(MAX(PDTRCDGO), 1299) + 1 AS PROXIMO,
       NVL(MAX(PDTRCDGO), 1299) + 2 AS SIGUIENTE
  FROM SCP.PDTR
 WHERE PDTRCDGO BETWEEN 1300 AND 1399;
-- (!) PROXIMO y SIGUIENTE tienen que ser <= 1399. Son los que usa el bloque 2.


-- ==========================================================================
-- BLOQUE 1 - Las 5 columnas de CRD.PRST (DDL: confirma solo, no lleva COMMIT)
-- ==========================================================================
ALTER TABLE CRD.PRST ADD (
    PRSTASEN NUMBER,           -- id del asiento de ENTREGA (lo escribe aprobar)
    PRSTASRV NUMBER,           -- id del asiento de REVERSO de la entrega (lo escribe anular)
    PRSTUSAN VARCHAR2(50),     -- usuario que anulo
    PRSTFCAN TIMESTAMP,        -- fecha y hora de la anulacion
    PRSTMTAN VARCHAR2(1000)    -- motivo de la anulacion (obligatorio en el servicio)
);

COMMENT ON COLUMN CRD.PRST.PRSTASEN IS 'Asiento contable de entrega generado al aprobar';
COMMENT ON COLUMN CRD.PRST.PRSTASRV IS 'Asiento de reverso de la entrega, generado al anular';
COMMENT ON COLUMN CRD.PRST.PRSTUSAN IS 'Usuario que anulo el prestamo otorgado';
COMMENT ON COLUMN CRD.PRST.PRSTFCAN IS 'Fecha y hora de la anulacion';
COMMENT ON COLUMN CRD.PRST.PRSTMTAN IS 'Motivo de la anulacion';


-- ==========================================================================
-- BLOQUE 2 - Estado 12 ANULADO (prestamo) y 9 ANULADA (cuota)
-- ==========================================================================
-- Toma del molde (alterno 7 de cada rubro) el rubro y el estado; PK del bloque del equipo.
-- Se detiene con error si no identifica cada rubro de forma unica, o si el alterno nuevo
-- ya existe. Correr el bloque PL/SQL completo.
DECLARE
    v_rubro_prestamo NUMBER;
    v_rubro_cuota    NUMBER;
    v_n              NUMBER;
    v_pk             NUMBER;
BEGIN
    SELECT COUNT(DISTINCT a.PRBRCDGO), MIN(a.PRBRCDGO) INTO v_n, v_rubro_prestamo
      FROM SCP.PDTR a
      JOIN SCP.PDTR b ON b.PRBRCDGO = a.PRBRCDGO
     WHERE a.PDTRALTR = 7  AND UPPER(a.PDTRDSCR) LIKE '%RECHAZ%'
       AND b.PDTRALTR = 11 AND UPPER(b.PDTRDSCR) LIKE '%MORA%';
    IF v_n <> 1 THEN
        RAISE_APPLICATION_ERROR(-20001, 'Rubro de estado del prestamo no identificado de forma unica: ' || v_n);
    END IF;

    SELECT COUNT(DISTINCT a.PRBRCDGO), MIN(a.PRBRCDGO) INTO v_n, v_rubro_cuota
      FROM SCP.PDTR a
      JOIN SCP.PDTR b ON b.PRBRCDGO = a.PRBRCDGO
     WHERE a.PDTRALTR = 7 AND UPPER(a.PDTRDSCR) LIKE '%CANCELAD%'
       AND b.PDTRALTR = 8 AND UPPER(b.PDTRDSCR) LIKE '%VENCID%';
    IF v_n <> 1 THEN
        RAISE_APPLICATION_ERROR(-20002, 'Rubro de estado de la cuota no identificado de forma unica: ' || v_n);
    END IF;

    SELECT COUNT(*) INTO v_n FROM SCP.PDTR
     WHERE (PRBRCDGO = v_rubro_prestamo AND PDTRALTR = 12)
        OR (PRBRCDGO = v_rubro_cuota    AND PDTRALTR = 9);
    IF v_n > 0 THEN
        RAISE_APPLICATION_ERROR(-20003, 'El alterno 12 (prestamo) o 9 (cuota) ya existe: no se vuelve a crear');
    END IF;

    -- PK explicito del bloque 1300-1399 (no hay secuencia).
    SELECT NVL(MAX(PDTRCDGO), 1299) + 1 INTO v_pk FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1300 AND 1399;
    IF v_pk + 1 > 1399 THEN
        RAISE_APPLICATION_ERROR(-20004, 'Bloque PDTR 1300-1399 agotado');
    END IF;

    -- Se copia del molde (alterno 7) solo PRBRCDGO y PDTRESTD. Valor numerico = alterno y
    -- valor texto = el codigo, como en los demas detalles de rubro (scripts 81/83/87/97).
    INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
    SELECT v_pk, d.PRBRCDGO, 'ANULADO', 12, 'ANULADO', 12, d.PDTRESTD
      FROM SCP.PDTR d
     WHERE d.PRBRCDGO = v_rubro_prestamo AND d.PDTRALTR = 7;

    INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
    SELECT v_pk + 1, d.PRBRCDGO, 'ANULADA', 9, 'ANULADA', 9, d.PDTRESTD
      FROM SCP.PDTR d
     WHERE d.PRBRCDGO = v_rubro_cuota AND d.PDTRALTR = 7;

    DBMS_OUTPUT.PUT_LINE('PDTR ' || v_pk || ': rubro ' || v_rubro_prestamo || ' ANULADO(12); PDTR '
                         || (v_pk + 1) || ': rubro ' || v_rubro_cuota || ' ANULADA(9)');
END;
/

COMMIT;


-- ==========================================================================
-- BLOQUE 3 - (!) CONTROL POSTERIOR
-- ==========================================================================
SELECT COLUMN_NAME, DATA_TYPE, DATA_LENGTH
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CRD' AND TABLE_NAME = 'PRST'
   AND COLUMN_NAME IN ('PRSTASEN','PRSTASRV','PRSTUSAN','PRSTFCAN','PRSTMTAN')
 ORDER BY COLUMN_NAME;
-- (!) 5 filas.

SELECT d.PRBRCDGO, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PDTR d
 WHERE (d.PDTRALTR = 12 AND d.PDTRDSCR = 'ANULADO')
    OR (d.PDTRALTR = 9  AND d.PDTRDSCR = 'ANULADA')
 ORDER BY d.PRBRCDGO;
-- (!) 2 filas, cada una en el rubro que mostro el bloque 0.B, con PDTRCDGO en 1300-1399.


-- ==========================================================================
-- BLOQUE 4 - REVERSO (comentado)
-- ==========================================================================
-- ⛔ Solo si NINGUN prestamo fue anulado todavia (si hay alguno en PRSTIDST = 12, avisar).
-- ⛔ Y con el WAR anterior desplegado: el WAR nuevo necesita las columnas.
--
-- DELETE FROM SCP.PDTR WHERE PDTRALTR = 12 AND PDTRDSCR = 'ANULADO';
-- DELETE FROM SCP.PDTR WHERE PDTRALTR = 9  AND PDTRDSCR = 'ANULADA';
-- COMMIT;
-- ALTER TABLE CRD.PRST DROP (PRSTASEN, PRSTASRV, PRSTUSAN, PRSTFCAN, PRSTMTAN);
