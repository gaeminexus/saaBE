-- =====================================================================================
-- e2-91 — Condonaciones de septiembre 2026: el gasto pasa de 4.1.01.30 a 4.8.02.1.
-- ⚠️ EL BLOQUE 1 ESCRIBE (y el BLOQUE 3, opcional). Equipo omen-saa-2 · 2026-10-06.
--
-- Pedido de contabilidad (correo de Ana Lalangui, 2026-10-05, «ASIENTOS CONTABLES MES DE SEPTIEMBRE 2026»,
-- revisado con Steven): cuatro asientos de condonación de préstamos cargaron el gasto al Debe de
-- 4.1.01.30 «INVERSIONES RENTA VARIABLE SECTOR NO FINANCIERO PRIVADO», y van a 4.8.02.1 «PÉRDIDA Y
-- CONDONACIÓN DE PRÉSTAMOS» (cuenta de movimiento creada el 2026-10-05):
--     CRE-2026-09-0541   acuerdo 10, préstamo 7239     4,94
--     CRE-2026-09-0006   acuerdo 3,  préstamo 4767    87,29
--     CRE-2026-09-0138   acuerdo 9,  préstamo 3943   818,14
--     CRE-2026-09-0003   acuerdo 1,  préstamo 6477 11.841,62      TOTAL 12.751,99
-- Solo se cambia la CUENTA de esas cuatro líneas (PLNNCDGO, más su copia en texto DTASCNTA/DTASNMCT). Los
-- montos, el Haber y los demás asientos no se tocan: el asiento sigue cuadrado.
--
-- CAUSA: los genera AcuerdoCondonacionServiceImpl (crd) con la línea aux1 = 70 (GASTO_CONDONACION_PRESTAMOS)
-- de la plantilla alterno 25 (COBRO_INDIVIDUAL_PRESTAMO). Esa línea apunta a 4.1.01.30: si no se corrige,
-- el próximo acuerdo vuelve a caer ahí → BLOQUE 3, opcional.
--
-- ⛔ PERÍODO: si septiembre está MAYORIZADO (2) o CERRADO (4), el BLOQUE 1 NO hace nada: los saldos de la
--    mayorización quedarían desfasados de los asientos. Primero desmayorizar septiembre, correr esto, y
--    volver a mayorizar.
-- Columnas copiadas de Asiento (ASNT), DetalleAsiento (DTAS), PlanCuenta (PLNN), Periodo (PRDO),
-- Plantilla (PLNS) y DetallePlantilla (DTPL).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES.
-- 0.1 ESPERADO: 4 filas, una por asiento, Debe = el monto del correo, Haber 0, asiento ACTIVO (1).
SELECT 'BLOQUE 0.1 - lineas a mover' AS bloque, a.ASNTCDGO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS fecha, a.ASNTESTD,
       d.DTASCDGO, p.PLNNCNTA, d.DTASDBEE, d.DTASHBRR, a.PJRQCDGO AS empresa
  FROM CNT.ASNT a
  JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE a.ASNTNMAL IN ('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003')
   AND p.PLNNCNTA = '4.1.01.30'
 ORDER BY a.ASNTNMAL;

-- 0.2 La cuenta destino. ESPERADO: 1 fila por empresa, tipo MOVIMIENTO (2), estado 1.
SELECT 'BLOQUE 0.2 - cuenta destino' AS bloque, p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR, p.PLNNTPOO, p.PLNNESTD,
       p.PJRQCDGO AS empresa
  FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.8.02.1';

-- 0.3 El período de septiembre. ESPERADO: estado ACTIVO (1) o DESMAYORIZADO (3). Con 2 o 4, el BLOQUE 1 se niega.
SELECT 'BLOQUE 0.3 - periodo' AS bloque, r.PRDOCDGO, r.PJRQCDGO AS empresa, r.PRDOMSSS, r.PRDOANNN, r.PRDOESTD
  FROM CNT.PRDO r WHERE r.PRDOANNN = 2026 AND r.PRDOMSSS = 9;

-- 0.4 ¿Hay MÁS condonaciones en 4.1.01.30 (otros meses, otros acuerdos)? Informativo: este script solo mueve
-- las cuatro del correo. Si aparecen otras, contabilidad decide.
SELECT 'BLOQUE 0.4 - otras condonaciones en 4.1.01.30' AS bloque, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS fecha,
       a.ASNTESTD, d.DTASDBEE, SUBSTR(d.DTASDSCR, 1, 60) AS descripcion
  FROM CNT.ASNT a
  JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
  JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNNCNTA = '4.1.01.30'
   AND UPPER(d.DTASDSCR) LIKE '%CONDONACI%'
   AND a.ASNTNMAL NOT IN ('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003')
 ORDER BY a.ASNTFCHA;

-- BLOQUE 1 — El cambio de cuenta. Todo o nada: si una sola guarda falla, no se toca ninguna línea.
DECLARE
    TYPE t_txt IS TABLE OF VARCHAR2(30);
    TYPE t_num IS TABLE OF NUMBER;
    v_asientos t_txt := t_txt('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003');
    v_montos   t_num := t_num(4.94, 87.29, 818.14, 11841.62);
    v_id_asiento NUMBER; v_estado NUMBER; v_empresa NUMBER; v_mes NUMBER; v_anio NUMBER;
    v_dtas NUMBER; v_debe NUMBER; v_haber NUMBER; v_lineas NUMBER;
    v_destino NUMBER; v_dest_cnta VARCHAR2(50); v_dest_nmbr VARCHAR2(100);
    v_tipo NUMBER; v_estado_cta NUMBER; v_periodo NUMBER;
    v_movidas NUMBER := 0;
BEGIN
    -- Primero se validan las cuatro; recién después se escribe.
    FOR i IN 1 .. v_asientos.COUNT LOOP
        SELECT a.ASNTCDGO, a.ASNTESTD, a.PJRQCDGO, EXTRACT(MONTH FROM a.ASNTFCHA), EXTRACT(YEAR FROM a.ASNTFCHA)
          INTO v_id_asiento, v_estado, v_empresa, v_mes, v_anio
          FROM CNT.ASNT a WHERE a.ASNTNMAL = v_asientos(i);
        IF v_estado <> 1 THEN
            RAISE_APPLICATION_ERROR(-20001, v_asientos(i) || ': estado ' || v_estado || ', no ACTIVO. No se toca nada.');
        END IF;

        SELECT COUNT(*) INTO v_lineas
          FROM CNT.DTAS d JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
         WHERE d.ASNTCDGO = v_id_asiento AND p.PLNNCNTA = '4.1.01.30';
        IF v_lineas <> 1 THEN
            RAISE_APPLICATION_ERROR(-20002, v_asientos(i) || ': ' || v_lineas || ' lineas en 4.1.01.30 (se esperaba 1). '
                || 'Ya se movio o el asiento es distinto al del correo. No se toca nada.');
        END IF;
        SELECT d.DTASCDGO, NVL(d.DTASDBEE, 0), NVL(d.DTASHBRR, 0) INTO v_dtas, v_debe, v_haber
          FROM CNT.DTAS d JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
         WHERE d.ASNTCDGO = v_id_asiento AND p.PLNNCNTA = '4.1.01.30';
        IF ABS(v_debe - v_montos(i)) > 0.005 OR v_haber <> 0 THEN
            RAISE_APPLICATION_ERROR(-20003, v_asientos(i) || ': Debe ' || v_debe || ' / Haber ' || v_haber
                || ', el correo dice Debe ' || v_montos(i) || '. No se toca nada.');
        END IF;

        SELECT COUNT(*) INTO v_lineas FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.8.02.1' AND p.PJRQCDGO = v_empresa;
        IF v_lineas <> 1 THEN
            RAISE_APPLICATION_ERROR(-20004, 'La cuenta 4.8.02.1 aparece ' || v_lineas || ' veces en la empresa '
                || v_empresa || ' (se esperaba 1). No se toca nada.');
        END IF;
        SELECT p.PLNNTPOO, p.PLNNESTD INTO v_tipo, v_estado_cta
          FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.8.02.1' AND p.PJRQCDGO = v_empresa;
        IF v_tipo <> 2 OR v_estado_cta <> 1 THEN
            RAISE_APPLICATION_ERROR(-20005, 'La cuenta 4.8.02.1 tiene tipo ' || v_tipo || ' y estado ' || v_estado_cta
                || ': tiene que ser de MOVIMIENTO (2) y ACTIVA (1). No se toca nada.');
        END IF;

        SELECT MAX(r.PRDOESTD) INTO v_periodo
          FROM CNT.PRDO r WHERE r.PJRQCDGO = v_empresa AND r.PRDOMSSS = v_mes AND r.PRDOANNN = v_anio;
        IF v_periodo IN (2, 4) THEN
            RAISE_APPLICATION_ERROR(-20006, 'El periodo ' || v_mes || '/' || v_anio || ' esta '
                || CASE v_periodo WHEN 2 THEN 'MAYORIZADO' ELSE 'CERRADO' END
                || ': desmayorizarlo antes, o los saldos quedan desfasados. No se toca nada.');
        END IF;
    END LOOP;

    -- Pasaron las cuatro: se escribe.
    FOR i IN 1 .. v_asientos.COUNT LOOP
        SELECT a.ASNTCDGO, a.PJRQCDGO INTO v_id_asiento, v_empresa FROM CNT.ASNT a WHERE a.ASNTNMAL = v_asientos(i);
        SELECT p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR INTO v_destino, v_dest_cnta, v_dest_nmbr
          FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.8.02.1' AND p.PJRQCDGO = v_empresa;
        UPDATE CNT.DTAS d
           SET d.PLNNCDGO = v_destino, d.DTASCNTA = v_dest_cnta, d.DTASNMCT = v_dest_nmbr
         WHERE d.ASNTCDGO = v_id_asiento
           AND d.PLNNCDGO IN (SELECT p.PLNNCDGO FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.1.01.30');
        v_movidas := v_movidas + SQL%ROWCOUNT;
    END LOOP;

    IF v_movidas <> 4 THEN
        RAISE_APPLICATION_ERROR(-20007, 'Se movieron ' || v_movidas || ' lineas (se esperaban 4). Se deshace todo.');
    END IF;
END;
/

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS.
-- 2.1 ESPERADO: CERO filas (ninguna de las cuatro sigue en 4.1.01.30).
SELECT 'BLOQUE 2.1 - restantes en 4.1.01.30' AS bloque, a.ASNTNMAL, d.DTASDBEE
  FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE a.ASNTNMAL IN ('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003')
   AND p.PLNNCNTA = '4.1.01.30';
-- 2.2 ESPERADO: 4 filas en 4.8.02.1, total 12.751,99.
SELECT 'BLOQUE 2.2 - ahora en 4.8.02.1' AS bloque, a.ASNTNMAL, d.DTASCNTA, d.DTASNMCT, d.DTASDBEE
  FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE a.ASNTNMAL IN ('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003')
   AND p.PLNNCNTA = '4.8.02.1'
 ORDER BY a.ASNTNMAL;
-- 2.3 ESPERADO: cada asiento sigue cuadrado (diferencia 0).
SELECT 'BLOQUE 2.3 - cuadre' AS bloque, a.ASNTNMAL, SUM(NVL(d.DTASDBEE, 0)) AS debe, SUM(NVL(d.DTASHBRR, 0)) AS haber,
       SUM(NVL(d.DTASDBEE, 0)) - SUM(NVL(d.DTASHBRR, 0)) AS diferencia
  FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
 WHERE a.ASNTNMAL IN ('CRE-2026-09-0541', 'CRE-2026-09-0006', 'CRE-2026-09-0138', 'CRE-2026-09-0003')
 GROUP BY a.ASNTNMAL ORDER BY a.ASNTNMAL;

-- BLOQUE 3 — OPCIONAL, para que no se repita: la línea «gasto por condonación» de la plantilla alterno 25.
-- 3.1 Ver primero. ESPERADO: una línea activa por empresa, hoy en 4.1.01.30.
SELECT 'BLOQUE 3.1 - plantilla' AS bloque, s.PLNSCDGO, s.PLNSNMBR, s.PJRQCDGO AS empresa, t.DTPLCDGO,
       t.DTPLAXL1, t.DTPLESTD, p.PLNNCNTA, p.PLNNNMBR, t.DTPLDSCR
  FROM CNT.PLNS s
  JOIN CNT.DTPL t ON t.PLNSCDGO = s.PLNSCDGO
  JOIN CNT.PLNN p ON p.PLNNCDGO = t.PLNNCDGO
 WHERE s.PLNSCDAL = 25 AND t.DTPLAXL1 = 70;
-- 3.2 El cambio. Es CONFIGURACIÓN que usa crédito (omen-saa-1): se avisa a su árbitro. Para correrlo, quitar
-- el comentario. Solo cambia la línea que hoy está en 4.1.01.30.
-- UPDATE CNT.DTPL t
--    SET t.PLNNCDGO = (SELECT p.PLNNCDGO FROM CNT.PLNN p
--                       WHERE p.PLNNCNTA = '4.8.02.1'
--                         AND p.PJRQCDGO = (SELECT s.PJRQCDGO FROM CNT.PLNS s WHERE s.PLNSCDGO = t.PLNSCDGO))
--  WHERE t.DTPLAXL1 = 70 AND t.DTPLESTD = 1
--    AND t.PLNSCDGO IN (SELECT s.PLNSCDGO FROM CNT.PLNS s WHERE s.PLNSCDAL = 25)
--    AND t.PLNNCDGO IN (SELECT p.PLNNCDGO FROM CNT.PLNN p WHERE p.PLNNCNTA = '4.1.01.30');
-- COMMIT;
-- Después, repetir el 3.1: ESPERADO la línea en 4.8.02.1.

-- REVERSO (comentado): mismo UPDATE del BLOQUE 1 con las cuentas invertidas, sobre las mismas cuatro líneas:
-- UPDATE CNT.DTAS d SET d.PLNNCDGO = <PLNNCDGO de 4.1.01.30 de la empresa>, d.DTASCNTA = '4.1.01.30',
--        d.DTASNMCT = 'INVERSIONES RENTA VARIABLE SECTOR NO FINANCIERO PRIVADO'
--  WHERE d.DTASCDGO IN (<los cuatro DTASCDGO del BLOQUE 0.1>);
