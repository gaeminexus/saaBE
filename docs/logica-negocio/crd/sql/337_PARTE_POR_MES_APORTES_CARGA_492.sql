-- =====================================================================================
-- 337 — CORRECCIÓN DE DATOS: parte por mes de devengo los 5 aportes de la carga 492 que
-- quedaron en UNA sola fila sin mes (afectaciones manuales 688-692 a cesantía).
-- omen-saa-1, 2026-10-31. Insumo: salida del crd/sql/336.
--
-- Por qué: una afectación manual a aporte se aplicó con aplicarAfectacionAAporte, que crea una
-- fila APRT por todo el monto, con APRTPRDV = NULL y tipo de movimiento EXCEDENTE_PETRO (8). El
-- reparto automático (distribuirAportePorDevengo) crea una fila por mes. Sin el mes no se sabe a
-- qué período corresponde cada valor.
--
-- Reparto (misma regla que distribuirAportePorDevengo): desde el mes incompleto más antiguo
-- desde 2025-06, llenando hasta el aporte mensual de la vigencia (tipo 11, cesantía), y lo que
-- sobre se anticipa a los meses siguientes. Calculado con la salida del 336:
--
--   APRT 1039420  rol 1625   66,95/mes  334,75 = jul 63,40 + ago/sep/oct/nov 66,95 + dic 3,55
--   APRT 1041221  rol 6127   23,25/mes   69,75 = jul 2,50 + ago 23,25 + sep 23,25 + oct 20,75
--   APRT 1040770  rol 10036  51,00/mes  102,00 = jun 46,21 + jul 51,00 + ago 4,79
--   APRT 1040616  rol 10950  31,00/mes  124,00 = jul/ago/sep/oct 31,00
--   APRT 1040615  rol 60274 111,45/mes  222,90 = dic-2025 111,45 + ene-2026 111,45
--
-- Qué hace: la fila original pasa a ser el PRIMER mes (valor, valor pagado, devengo, tipo
-- APORTE_MENSUAL = 1) y se insertan las demás copiando todo lo demás de la original (entidad,
-- contrato, tipo, carga, fecha de caja, glosa, usuario, estado). Lo mismo con su pago CRD.PGAP.
-- El total por carga y tipo de aporte NO cambia: el asiento de la carga no se toca.
--
-- Seguro de correr de corrido: el BLOQUE 1 solo actúa sobre una fila que siga sin devengo y con
-- el monto esperado. Si se corre dos veces, la segunda no hace nada.
-- =====================================================================================

-- ---------------------------------------------------------------------
-- CONTROL 0 — antes. Esperado: 5 filas sin devengo, una PGAP por cada una, mismos montos.
-- ---------------------------------------------------------------------
SELECT a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV, a.APRTTPMV,
       (SELECT COUNT(*) FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO) AS PGAP_FILAS,
       (SELECT SUM(p.PGAPVLRR) FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO) AS PGAP_TOTAL
  FROM CRD.APRT a
 WHERE a.APRTCDGO IN (1039420, 1041221, 1040770, 1040616, 1040615)
 ORDER BY a.APRTCDGO;

-- Total de aportes de la carga 492 por tipo (tiene que ser IGUAL en el CONTROL 2).
SELECT a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTIDAS = 492
 GROUP BY a.TPAPCDGO
 ORDER BY a.TPAPCDGO;

-- ---------------------------------------------------------------------
-- BLOQUE 1 — partir por mes.
-- ---------------------------------------------------------------------
DECLARE
  TYPE t_parte IS RECORD (aprt NUMBER, total NUMBER, mes DATE, monto NUMBER);
  TYPE t_partes IS TABLE OF t_parte;
  v_partes t_partes := t_partes(
    t_parte(1039420, 334.75, DATE '2026-07-01', 63.40),
    t_parte(1039420, 334.75, DATE '2026-08-01', 66.95),
    t_parte(1039420, 334.75, DATE '2026-09-01', 66.95),
    t_parte(1039420, 334.75, DATE '2026-10-01', 66.95),
    t_parte(1039420, 334.75, DATE '2026-11-01', 66.95),
    t_parte(1039420, 334.75, DATE '2026-12-01', 3.55),
    t_parte(1041221, 69.75,  DATE '2026-07-01', 2.50),
    t_parte(1041221, 69.75,  DATE '2026-08-01', 23.25),
    t_parte(1041221, 69.75,  DATE '2026-09-01', 23.25),
    t_parte(1041221, 69.75,  DATE '2026-10-01', 20.75),
    t_parte(1040770, 102.00, DATE '2026-06-01', 46.21),
    t_parte(1040770, 102.00, DATE '2026-07-01', 51.00),
    t_parte(1040770, 102.00, DATE '2026-08-01', 4.79),
    t_parte(1040616, 124.00, DATE '2026-07-01', 31.00),
    t_parte(1040616, 124.00, DATE '2026-08-01', 31.00),
    t_parte(1040616, 124.00, DATE '2026-09-01', 31.00),
    t_parte(1040616, 124.00, DATE '2026-10-01', 31.00),
    t_parte(1040615, 222.90, DATE '2025-12-01', 111.45),
    t_parte(1040615, 222.90, DATE '2026-01-01', 111.45)
  );
  v_actual   NUMBER := NULL;
  v_procesa  BOOLEAN := FALSE;
  v_ok       NUMBER;
  v_pgap     NUMBER;
  v_nuevo    NUMBER;
  v_primero  BOOLEAN;
  v_suma     NUMBER;
BEGIN
  -- Las partes de cada fila tienen que sumar su total (protección contra un error de tipeo).
  FOR i IN 1 .. v_partes.COUNT LOOP
    v_suma := 0;
    FOR j IN 1 .. v_partes.COUNT LOOP
      IF v_partes(j).aprt = v_partes(i).aprt THEN v_suma := v_suma + v_partes(j).monto; END IF;
    END LOOP;
    IF ABS(v_suma - v_partes(i).total) > 0.001 THEN
      RAISE_APPLICATION_ERROR(-20001, 'Las partes del APRT ' || v_partes(i).aprt || ' suman '
        || v_suma || ' y no ' || v_partes(i).total);
    END IF;
  END LOOP;

  FOR i IN 1 .. v_partes.COUNT LOOP
    IF v_actual IS NULL OR v_actual <> v_partes(i).aprt THEN
      v_actual  := v_partes(i).aprt;
      v_primero := TRUE;
      -- ¿Sigue intacta? (sin devengo y con el monto total)
      SELECT COUNT(*) INTO v_ok FROM CRD.APRT
       WHERE APRTCDGO = v_actual AND APRTPRDV IS NULL AND ABS(APRTVLRR - v_partes(i).total) < 0.001;
      v_procesa := (v_ok = 1);
      IF NOT v_procesa THEN
        DBMS_OUTPUT.PUT_LINE('APRT ' || v_actual || ': ya corregido o con otro monto, se omite.');
      ELSE
        SELECT COUNT(*) INTO v_ok FROM CRD.PGAP WHERE APRTCDGO = v_actual;
        IF v_ok <> 1 THEN
          RAISE_APPLICATION_ERROR(-20002, 'El APRT ' || v_actual || ' tiene ' || v_ok
            || ' filas en CRD.PGAP (se esperaba 1): avisar antes de seguir.');
        END IF;
        SELECT PGAPCDGO INTO v_pgap FROM CRD.PGAP WHERE APRTCDGO = v_actual;
      END IF;
    END IF;

    IF v_procesa THEN
      IF v_primero THEN
        UPDATE CRD.APRT
           SET APRTVLRR = v_partes(i).monto, APRTVLPG = v_partes(i).monto, APRTSLDO = 0,
               APRTPRDV = v_partes(i).mes, APRTTPMV = 1
         WHERE APRTCDGO = v_actual;
        UPDATE CRD.PGAP SET PGAPVLRR = v_partes(i).monto WHERE PGAPCDGO = v_pgap;
        v_primero := FALSE;
      ELSE
        INSERT INTO CRD.APRT (FLLLCDGO, ENTDCDGO, CNTRCDGO, TPAPCDGO, CRARCDGO, APRTIDDV, APRTFCTR,
                              APRTGLSA, APRTVLRR, APRTVLPG, APRTSLDO, APRTIDAS, APRTFCRG, APRTUSRG,
                              APRTIDST, APRTPRDV, APRTTPMV)
        SELECT FLLLCDGO, ENTDCDGO, CNTRCDGO, TPAPCDGO, CRARCDGO, APRTIDDV, APRTFCTR,
               APRTGLSA, v_partes(i).monto, v_partes(i).monto, 0, APRTIDAS, APRTFCRG, APRTUSRG,
               APRTIDST, v_partes(i).mes, 1
          FROM CRD.APRT WHERE APRTCDGO = v_actual;
        SELECT MAX(APRTCDGO) INTO v_nuevo FROM CRD.APRT
         WHERE ENTDCDGO = (SELECT ENTDCDGO FROM CRD.APRT WHERE APRTCDGO = v_actual)
           AND APRTIDAS = 492 AND APRTPRDV = v_partes(i).mes AND APRTCDGO <> v_actual;
        INSERT INTO CRD.PGAP (FLLLCDGO, APRTCDGO, PGAPVLRR, PGAPFCCN, PGAPNMAS, PGAPCNCP, PGAPFCRG,
                              PGAPUSRG, PGAPIDST, PGPRCDGO, PGAPRTRS, PGAPFCPG)
        SELECT FLLLCDGO, v_nuevo, v_partes(i).monto, PGAPFCCN, PGAPNMAS, PGAPCNCP, PGAPFCRG,
               PGAPUSRG, PGAPIDST, PGPRCDGO, PGAPRTRS, PGAPFCPG
          FROM CRD.PGAP WHERE PGAPCDGO = v_pgap;
      END IF;
    END IF;
  END LOOP;
  COMMIT;
  DBMS_OUTPUT.PUT_LINE('Listo.');
EXCEPTION WHEN OTHERS THEN
  ROLLBACK;
  RAISE;
END;
/

-- ---------------------------------------------------------------------
-- CONTROL 1 — después: cada partícipe con una fila por mes y su PGAP del mismo valor.
--             Esperado: 19 filas, ninguna con APRTPRDV nulo, DIFERENCIA = 0 en todas.
-- ---------------------------------------------------------------------
SELECT e.ENTDRLPC AS ROL, a.APRTCDGO, a.APRTPRDV, a.APRTVLRR, a.APRTTPMV,
       (SELECT SUM(p.PGAPVLRR) FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO) AS PGAP,
       a.APRTVLRR - NVL((SELECT SUM(p.PGAPVLRR) FROM CRD.PGAP p WHERE p.APRTCDGO = a.APRTCDGO), 0) AS DIFERENCIA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 492
   AND e.ENTDRLPC IN (1625, 60274, 10950, 10036, 6127)
   AND a.TPAPCDGO = 11
 ORDER BY e.ENTDRLPC, a.APRTPRDV;

-- ---------------------------------------------------------------------
-- CONTROL 2 — el total por tipo de la carga 492 tiene que ser IGUAL al del CONTROL 0
--             (solo cambia la cantidad de filas).
-- ---------------------------------------------------------------------
SELECT a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTIDAS = 492
 GROUP BY a.TPAPCDGO
 ORDER BY a.TPAPCDGO;

-- =====================================================================================
-- REVERSO (comentado). Vuelve a una fila por partícipe: borra las filas insertadas (y sus PGAP)
-- y devuelve a la original su monto total, sin devengo y con tipo 8.
-- =====================================================================================
-- DELETE FROM CRD.PGAP WHERE APRTCDGO IN (
--   SELECT a.APRTCDGO FROM CRD.APRT a
--    WHERE a.APRTIDAS = 492 AND a.TPAPCDGO = 11
--      AND a.ENTDCDGO IN (SELECT ENTDCDGO FROM CRD.APRT WHERE APRTCDGO IN (1039420,1041221,1040770,1040616,1040615))
--      AND a.APRTCDGO NOT IN (1039420,1041221,1040770,1040616,1040615)
--      AND a.APRTFCRG = (SELECT o.APRTFCRG FROM CRD.APRT o WHERE o.ENTDCDGO = a.ENTDCDGO AND o.APRTCDGO IN (1039420,1041221,1040770,1040616,1040615)));
-- DELETE FROM CRD.APRT a
--  WHERE a.APRTIDAS = 492 AND a.TPAPCDGO = 11
--    AND a.ENTDCDGO IN (SELECT ENTDCDGO FROM CRD.APRT WHERE APRTCDGO IN (1039420,1041221,1040770,1040616,1040615))
--    AND a.APRTCDGO NOT IN (1039420,1041221,1040770,1040616,1040615)
--    AND a.APRTFCRG = (SELECT o.APRTFCRG FROM CRD.APRT o WHERE o.ENTDCDGO = a.ENTDCDGO AND o.APRTCDGO IN (1039420,1041221,1040770,1040616,1040615));
-- UPDATE CRD.APRT SET APRTVLRR = 334.75, APRTVLPG = 334.75, APRTPRDV = NULL, APRTTPMV = 8 WHERE APRTCDGO = 1039420;
-- UPDATE CRD.APRT SET APRTVLRR = 69.75,  APRTVLPG = 69.75,  APRTPRDV = NULL, APRTTPMV = 8 WHERE APRTCDGO = 1041221;
-- UPDATE CRD.APRT SET APRTVLRR = 102.00, APRTVLPG = 102.00, APRTPRDV = NULL, APRTTPMV = 8 WHERE APRTCDGO = 1040770;
-- UPDATE CRD.APRT SET APRTVLRR = 124.00, APRTVLPG = 124.00, APRTPRDV = NULL, APRTTPMV = 8 WHERE APRTCDGO = 1040616;
-- UPDATE CRD.APRT SET APRTVLRR = 222.90, APRTVLPG = 222.90, APRTPRDV = NULL, APRTTPMV = 8 WHERE APRTCDGO = 1040615;
-- UPDATE CRD.PGAP p SET p.PGAPVLRR = (SELECT a.APRTVLRR FROM CRD.APRT a WHERE a.APRTCDGO = p.APRTCDGO)
--  WHERE p.APRTCDGO IN (1039420,1041221,1040770,1040616,1040615);
-- COMMIT;
