-- =====================================================================================
-- 371 - ESTADO 14 APROBADO del prestamo (aprobado, pendiente de desembolso)
--       Catalogo CRD.ESPS (fila ESPSCDEX = 14) + detalle de rubro (alterno 14) + CONTROL de solo lectura
--       de prestamos VIGENTES cuya orden de desembolso NO esta confirmada.
-- FECHA: 2026-11-08 · EQUIPO: omen-saa-1 (crd) · Redacta: omen-saa-1-be · Revisa: omen-saa-1-arb
-- Diseno: docs/logica-negocio/crd/DISENO-ESTADO-APROBADO-VIGENTE-AL-DESEMBOLSO.md (§7)
--
-- SIN DDL DE TABLAS: la fecha y el usuario del desembolso reusan CRD.PRST.PRSTFCAC / PRSTUSAC (ya existen,
--   entidad Prestamo, sin escritor hasta este cambio). No hay ALTER TABLE.
-- ⛔ LOS BLOQUES 2 Y 3 ESCRIBEN (INSERT). Los demas son de solo lectura. NO lo corre el codigo: lo corre el usuario.
-- El codigo NO lee estos catalogos para decidir nada (usa las constantes de com.saa.rubros.EstadoPrestamo); son
--   documentales y los usan filtros/etiquetas del FE (ESPSCDEX). Si el rubro no se identifica, se OMITE y se avisa.
-- Sin comandos de SQL*Plus. Correr bloque por bloque, en orden, leyendo cada control.
-- =====================================================================================


-- ==========================================================================
-- BLOQUE 0.A - (!) El estado 14 NO existe todavia (SOLO LECTURA)
-- ==========================================================================
SELECT ESPSCDGO, ESPSCDEX, ESPSNMBR, ESPSIDST FROM CRD.ESPS WHERE ESPSCDEX = 14 OR UPPER(ESPSNMBR) LIKE '%APROBADO%';
-- (!) Se esperan CERO filas. Si sale una con ESPSCDEX = 14, el bloque 2 ya se corrio: saltarlo.
--     Si sale otra fila 'APROBADO' con otro ESPSCDEX: PARAR y avisar al arbitro.

SELECT ESPSCDGO, ESPSCDEX, ESPSNMBR, ESPSIDST FROM CRD.ESPS ORDER BY ESPSCDGO;
-- Informativo: el catalogo CRD.ESPS NO esta alineado con PRST.PRSTIDST (el estado operativo es PRSTIDST).
-- Anotar el MAX(ESPSCDGO): el bloque 2 toma MAX + 1.


-- ==========================================================================
-- BLOQUE 0.B - Rubro de estados del prestamo en SCP (SOLO LECTURA), como el 325 (bloque 0.B)
-- ==========================================================================
SELECT r.PRBRCDGO, r.PRBRDSCR, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PRBR r
  JOIN SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
 WHERE r.PRBRCDGO IN (
         SELECT a.PRBRCDGO FROM SCP.PDTR a JOIN SCP.PDTR b ON b.PRBRCDGO = a.PRBRCDGO
          WHERE a.PDTRALTR = 7 AND UPPER(a.PDTRDSCR) LIKE '%RECHAZ%'
            AND b.PDTRALTR = 11 AND UPPER(b.PDTRDSCR) LIKE '%MORA%')
 ORDER BY r.PRBRCDGO, d.PDTRALTR;
-- (!) Se espera UN rubro con alternos 1..13 (GENERADO ... CANCELADO POR REESTRUCTURACION), sin alterno 14.
--     Si no sale ninguno (como en la base de desarrollo vieja) el bloque 3 lo OMITE y avisa: no es un error.
--     Si salen varios rubros: PARAR y mandar la salida al arbitro.


-- ==========================================================================
-- BLOQUE 0.C - Codigos libres en el bloque de omen-saa-1 (PDTR 1300-1399) (SOLO LECTURA)
-- ==========================================================================
SELECT NVL(MAX(PDTRCDGO), 1299) AS ULTIMO_USADO_DEL_BLOQUE,
       NVL(MAX(PDTRCDGO), 1299) + 1 AS PROXIMO
  FROM SCP.PDTR
 WHERE PDTRCDGO BETWEEN 1300 AND 1399;
-- (!) PROXIMO tiene que ser <= 1399.


-- ==========================================================================
-- BLOQUE 1 - RESPALDO de los dos catalogos (por si hay que volver)
-- ==========================================================================
CREATE TABLE CRD.ESPS_BK371 AS SELECT * FROM CRD.ESPS;
-- (!) El respaldo de SCP.PDTR es mas grande; solo se respaldan las filas del rubro de estados del prestamo.
CREATE TABLE SCP.PDTR_BK371 AS
  SELECT * FROM SCP.PDTR
   WHERE PRBRCDGO IN (
         SELECT a.PRBRCDGO FROM SCP.PDTR a JOIN SCP.PDTR b ON b.PRBRCDGO = a.PRBRCDGO
          WHERE a.PDTRALTR = 7 AND UPPER(a.PDTRDSCR) LIKE '%RECHAZ%'
            AND b.PDTRALTR = 11 AND UPPER(b.PDTRDSCR) LIKE '%MORA%');
-- Si el rubro no se identifica, el respaldo queda vacio (0 filas): es lo esperado en ese caso.


-- ==========================================================================
-- BLOQUE 2 - ⛔ ESCRIBE: fila APROBADO (ESPSCDEX = 14) en CRD.ESPS
-- ==========================================================================
-- No hay secuencia de ESPS: el PK es MAX + 1. ESPSIDST = 1 (activo), como las filas existentes.
DECLARE
    v_n  NUMBER;
    v_pk NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_n FROM CRD.ESPS WHERE ESPSCDEX = 14;
    IF v_n > 0 THEN
        RAISE_APPLICATION_ERROR(-20371, 'CRD.ESPS ya tiene una fila con ESPSCDEX = 14: no se vuelve a crear');
    END IF;
    SELECT NVL(MAX(ESPSCDGO), 0) + 1 INTO v_pk FROM CRD.ESPS;
    INSERT INTO CRD.ESPS (ESPSCDGO, ESPSNMBR, ESPSCDEX, ESPSIDST)
    VALUES (v_pk, 'APROBADO PENDIENTE DE DESEMBOLSO', 14, 1);
    DBMS_OUTPUT.PUT_LINE('CRD.ESPS ' || v_pk || ': APROBADO PENDIENTE DE DESEMBOLSO (14)');
END;
/

COMMIT;


-- ==========================================================================
-- BLOQUE 3 - ⛔ ESCRIBE: detalle del rubro de estados del prestamo, alterno 14 (como el 325)
-- ==========================================================================
-- Toma del molde (alterno 7) el rubro y el estado. Si el rubro no se identifica de forma unica se OMITE y se avisa
-- (el codigo no lee estos nombres; el detalle es documental). PK del bloque 1300-1399.
DECLARE
    v_rubro NUMBER;
    v_n     NUMBER;
    v_pk    NUMBER;
BEGIN
    SELECT COUNT(DISTINCT a.PRBRCDGO), MIN(a.PRBRCDGO) INTO v_n, v_rubro
      FROM SCP.PDTR a
      JOIN SCP.PDTR b ON b.PRBRCDGO = a.PRBRCDGO
     WHERE a.PDTRALTR = 7  AND UPPER(a.PDTRDSCR) LIKE '%RECHAZ%'
       AND b.PDTRALTR = 11 AND UPPER(b.PDTRDSCR) LIKE '%MORA%';
    IF v_n <> 1 THEN
        DBMS_OUTPUT.PUT_LINE('AVISO: rubro de estado del prestamo no identificado (' || v_n || '): se omite APROBADO(14) en SCP.PDTR');
        RETURN;
    END IF;

    SELECT COUNT(*) INTO v_n FROM SCP.PDTR WHERE PRBRCDGO = v_rubro AND PDTRALTR = 14;
    IF v_n > 0 THEN
        RAISE_APPLICATION_ERROR(-20372, 'El alterno 14 ya existe en el rubro ' || v_rubro || ': no se vuelve a crear');
    END IF;

    SELECT NVL(MAX(PDTRCDGO), 1299) + 1 INTO v_pk FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1300 AND 1399;
    IF v_pk > 1399 THEN
        RAISE_APPLICATION_ERROR(-20373, 'Bloque PDTR 1300-1399 agotado');
    END IF;

    INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
    SELECT v_pk, d.PRBRCDGO, 'APROBADO', 14, 'APROBADO', 14, d.PDTRESTD
      FROM SCP.PDTR d
     WHERE d.PRBRCDGO = v_rubro AND d.PDTRALTR = 7;

    DBMS_OUTPUT.PUT_LINE('PDTR ' || v_pk || ': rubro ' || v_rubro || ' APROBADO(14)');
END;
/

COMMIT;


-- ==========================================================================
-- BLOQUE 4 - (!) CONTROL POSTERIOR (SOLO LECTURA)
-- ==========================================================================
SELECT ESPSCDGO, ESPSCDEX, ESPSNMBR, ESPSIDST FROM CRD.ESPS WHERE ESPSCDEX = 14;
-- (!) 1 fila.
SELECT d.PRBRCDGO, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRESTD FROM SCP.PDTR d
 WHERE d.PDTRALTR = 14 AND d.PDTRDSCR = 'APROBADO';
-- (!) 1 fila en el rubro del bloque 0.B con PDTRCDGO en 1300-1399; o 0 filas si el bloque 3 avisó que lo omitía.
SELECT (SELECT COUNT(*) FROM CRD.ESPS) AS ESPS_AHORA, (SELECT COUNT(*) FROM CRD.ESPS_BK371) AS ESPS_RESPALDO FROM DUAL;
-- (!) ESPS_AHORA = ESPS_RESPALDO + 1.


-- ==========================================================================
-- BLOQUE 5 - CONTROL DE SOLO LECTURA ANTES DEL EAR: prestamos VIGENTES con orden de desembolso NO confirmada
-- ==========================================================================
-- Son los aprobados ANTES de este cambio cuyo dinero todavia no salio (la orden esta POR_APROBAR, REGISTRADA o EN_ARCHIVO).
-- Con el codigo nuevo seguiran VIGENTES (compatibilidad: el hook solo actua sobre los que estan en APROBADO) y su asiento de
-- entrega ya existe; al confirmarse su pago NO pasa nada en el prestamo. Se listan para que el usuario decida si quiere
-- tratarlos distinto (por ejemplo, anularlos y volver a aprobarlos). El codigo nuevo NO los toca.
-- PGTRESTD: 0 POR_APROBAR, 1 REGISTRADO, 2 EN_ARCHIVO, 3 CONFIRMADO, 4 RECHAZADO, 5 ANULADO (com.saa.rubros.EstadoPagoProgramado).
SELECT p.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, p.PRSTMNSL, p.PRSTFCAP, p.PRSTIDPG, g.PGTRESTD AS ESTADO_ORDEN, g.PGTRFRSP AS FECHA_RESPUESTA
  FROM CRD.PRST p
  JOIN PGS.PGTR g ON g.PGTRCDGO = p.PRSTIDPG
 WHERE p.PRSTIDST IN (2, 11)
   AND g.PGTRESTD IN (0, 1, 2)
 ORDER BY p.PRSTFCAP;
-- Informativo. Si sale alguna fila, avisar al arbitro antes del EAR.

-- Los VIGENTES con PRSTIDPG nulo son cartera migrada o reestructuraciones (sin orden): no entran en este control.
SELECT COUNT(*) AS VIGENTES_SIN_ORDEN FROM CRD.PRST WHERE PRSTIDST IN (2, 11) AND PRSTIDPG IS NULL;


-- ==========================================================================
-- BLOQUE 6 - REVERSO (comentado)
-- ==========================================================================
-- ⛔ Solo si NINGUN prestamo esta en PRSTIDST = 14 (si hay alguno, avisar: antes hay que anularlo o desembolsarlo).
--    Compruebalo primero:  SELECT COUNT(*) FROM CRD.PRST WHERE PRSTIDST = 14;   (esperado 0)
-- ⛔ Y con el WAR anterior desplegado (el WAR nuevo no depende de estas filas, pero el FE puede filtrar por ESPSCDEX 14).
--
-- DELETE FROM CRD.ESPS WHERE ESPSCDEX = 14 AND ESPSNMBR = 'APROBADO PENDIENTE DE DESEMBOLSO';
-- DELETE FROM SCP.PDTR WHERE PDTRALTR = 14 AND PDTRDSCR = 'APROBADO' AND PDTRCDGO BETWEEN 1300 AND 1399;
-- COMMIT;
-- DROP TABLE CRD.ESPS_BK371;
-- DROP TABLE SCP.PDTR_BK371;
