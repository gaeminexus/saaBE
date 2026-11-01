-- =====================================================================================
-- 343 — CORRECCIÓN DE DATOS de los aportes de la carga ARCH 510 (octubre 2026).
-- omen-saa-1, 2026-10-31. Orden del usuario: «arreglemos la carga de arch».
--
-- Dos arreglos, los mismos que se hicieron en la 492 (crd/sql/337 y 338):
--
-- A. Las afectaciones manuales a aporte de BENITEZ (AVPC 697: 54,30 jubilación, tipo 9; AVPC
--    700: 21,72 cesantía, tipo 11) se grabaron con aplicarAfectacionAAporte: UNA fila sin mes de
--    devengo (APRTPRDV nulo, tipo de movimiento EXCEDENTE_PETRO = 8). Cada una es exactamente UN
--    mes de su vigencia (54,30 y 21,72 por mes), así que no hay que partirlas: se les asigna el
--    PRIMER mes incompleto de su tipo desde 2025-06, con la misma regla que
--    distribuirAportePorDevengo (aportado por período efectivo < aporte mensual de la vigencia).
--    Solo se toca una fila si su monto es igual al aporte mensual vigente de su tipo.
-- B. La glosa: en producción todavía corre el código que pone siempre el mes de la carga
--    ("Mes 10/2026"). Toda fila de la 510 cuyo devengo no es octubre pasa a "Mes {devengo}
--    (pagado en 10/2026)".
--
-- No cambia montos ni el total por carga y tipo: el asiento no se toca. Seguro de correr dos
-- veces: A solo actúa sobre filas sin devengo; B, sobre glosas que todavía dicen "Mes 10/2026 -".
-- =====================================================================================

-- ---------------------------------------------------------------------
-- CONTROL 0 — aportes de la carga 510, con su devengo y glosa actuales.
-- ---------------------------------------------------------------------
SELECT e.ENTDRLPC AS ROL, e.ENTDRZNS, a.APRTCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV, a.APRTTPMV,
       a.APRTGLSA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 510
 ORDER BY e.ENTDRLPC, a.TPAPCDGO, a.APRTPRDV NULLS FIRST;

SELECT a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a WHERE a.APRTIDAS = 510 GROUP BY a.TPAPCDGO ORDER BY 1;

-- ---------------------------------------------------------------------
-- BLOQUE A — mes de devengo para los aportes sin mes de la carga 510.
-- ---------------------------------------------------------------------
DECLARE
  v_mensual  NUMBER;
  v_mes      DATE;
  v_aportado NUMBER;
  v_asignado DATE;
BEGIN
  FOR r IN (SELECT a.APRTCDGO, a.ENTDCDGO, a.TPAPCDGO, a.APRTVLRR
              FROM CRD.APRT a
             WHERE a.APRTIDAS = 510 AND a.APRTPRDV IS NULL) LOOP
    -- aporte mensual vigente de ese tipo (contrato activo, vigencia activa)
    BEGIN
      SELECT v.VGCNMNTO INTO v_mensual
        FROM CRD.VGCN v
        JOIN CRD.CNTR k ON k.CNTRCDGO = v.CNTRCDGO AND k.CNTRESTD = 1
       WHERE k.ENTDCDGO = r.ENTDCDGO AND v.TPAPCDGO = r.TPAPCDGO AND v.VGCNIDST = 1
         AND v.VGCNFCIN <= DATE '2026-10-31'
         AND (v.VGCNFCFN IS NULL OR v.VGCNFCFN >= DATE '2026-10-31')
         AND ROWNUM = 1;
    EXCEPTION WHEN NO_DATA_FOUND THEN
      v_mensual := NULL;
    END;

    IF v_mensual IS NULL OR ABS(v_mensual - r.APRTVLRR) > 0.001 THEN
      DBMS_OUTPUT.PUT_LINE('APRT ' || r.APRTCDGO || ': monto ' || r.APRTVLRR
        || ' distinto del aporte mensual ' || NVL(TO_CHAR(v_mensual), 'sin vigencia')
        || ' -> NO se toca (avisar al árbitro).');
      CONTINUE;
    END IF;

    -- primer mes incompleto desde 2025-06 (sin contar la propia fila)
    v_asignado := NULL;
    v_mes := DATE '2025-06-01';
    WHILE v_mes <= DATE '2027-12-01' AND v_asignado IS NULL LOOP
      SELECT NVL(SUM(a.APRTVLRR), 0) INTO v_aportado
        FROM CRD.APRT a
       WHERE a.ENTDCDGO = r.ENTDCDGO AND a.TPAPCDGO = r.TPAPCDGO
         AND a.APRTCDGO <> r.APRTCDGO
         AND (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                   WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) = v_mes;
      IF v_mensual - v_aportado > 0.01 THEN
        v_asignado := v_mes;
      END IF;
      v_mes := ADD_MONTHS(v_mes, 1);
    END LOOP;

    IF v_asignado IS NULL THEN
      DBMS_OUTPUT.PUT_LINE('APRT ' || r.APRTCDGO || ': no se encontró mes incompleto -> NO se toca.');
      CONTINUE;
    END IF;

    UPDATE CRD.APRT
       SET APRTPRDV = v_asignado, APRTTPMV = 1
     WHERE APRTCDGO = r.APRTCDGO AND APRTPRDV IS NULL;
    DBMS_OUTPUT.PUT_LINE('APRT ' || r.APRTCDGO || ' (tipo ' || r.TPAPCDGO || ', ' || r.APRTVLRR
      || ') -> devengo ' || TO_CHAR(v_asignado, 'YYYY-MM'));
  END LOOP;
  COMMIT;
END;
/

-- ---------------------------------------------------------------------
-- BLOQUE B — glosa con el mes de devengo (todas las filas de la 510 que no son de octubre).
-- ---------------------------------------------------------------------
UPDATE CRD.APRT a
   SET a.APRTGLSA = REPLACE(a.APRTGLSA, 'Mes 10/2026 -',
                            'Mes ' || TO_CHAR(a.APRTPRDV, 'FMMM/YYYY') || ' (pagado en 10/2026) -')
 WHERE a.APRTIDAS = 510
   AND a.APRTPRDV IS NOT NULL
   AND a.APRTPRDV <> DATE '2026-10-01'
   AND a.APRTGLSA LIKE '%Mes 10/2026 -%';

COMMIT;

-- ---------------------------------------------------------------------
-- CONTROL 1 — después: ninguna fila sin devengo (salvo las que el BLOQUE A avisó), cada glosa
--             con su mes, y el total por tipo IGUAL al del CONTROL 0.
-- ---------------------------------------------------------------------
SELECT e.ENTDRLPC AS ROL, e.ENTDRZNS, a.APRTCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV, a.APRTTPMV,
       a.APRTGLSA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 510
 ORDER BY e.ENTDRLPC, a.TPAPCDGO, a.APRTPRDV;

SELECT a.TPAPCDGO, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a WHERE a.APRTIDAS = 510 GROUP BY a.TPAPCDGO ORDER BY 1;

SELECT COUNT(*) AS SIN_DEVENGO FROM CRD.APRT a WHERE a.APRTIDAS = 510 AND a.APRTPRDV IS NULL;

-- REVERSO (comentado)
-- A: (usar los APRTCDGO que el BLOQUE A imprimió, o los que en el CONTROL 0 tenían APRTPRDV nulo)
--    UPDATE CRD.APRT SET APRTPRDV = NULL, APRTTPMV = 8
--     WHERE APRTIDAS = 510 AND APRTCDGO IN (<aprt1>, <aprt2>);
-- B: UPDATE CRD.APRT a SET a.APRTGLSA = REPLACE(a.APRTGLSA,
--        'Mes ' || TO_CHAR(a.APRTPRDV, 'FMMM/YYYY') || ' (pagado en 10/2026) -', 'Mes 10/2026 -')
--     WHERE a.APRTIDAS = 510 AND a.APRTGLSA LIKE '%(pagado en 10/2026)%';
-- COMMIT;
