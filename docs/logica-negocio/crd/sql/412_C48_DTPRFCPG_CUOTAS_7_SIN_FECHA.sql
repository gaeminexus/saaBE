-- =====================================================================================================================================
-- 412 - C48: fecha de pago (DTPRFCPG) de las cuotas en CANCELADA_ANTICIPADA (7) que el acuerdo, la precancelación o la sustitución dejaron en NULL.
-- Equipo omen-saa-1 - 2026-11-27 - REVISABLE: NO está ejecutado en ninguna base.
--
-- Defecto: AcuerdoCondonacionServiceImpl y ProcesoPagoPrestamoServiceImpl.precancelar ponían DTPRFCPG = NULL al pasar la cuota a 7. Las lecturas al corte
-- (NOT (DTPRESTD IN (4,7) AND NVL(DTPRFCPG, :corteSig) < :corteSig)) tratan una cuota 7 sin fecha como NO liquidada, para siempre: el libro de devengo, la
-- provisión por cuota, las bandas y la G48 la leen viva. El código ya fecha las cuotas nuevas (lote 27); este script arregla las que quedaron de antes.
--
-- !!! SE CORRE SOLO CON EL VISTO BUENO DEL CUADRE. No es una operación (no crea asientos, pagos ni eventos), pero CAMBIA LA LECTURA AL CORTE DE MESES PASADOS:
-- !!! las cuotas dejan de contarse como vivas en los cortes posteriores a la fecha del evento. NO se regeneran las G ya presentadas.
--
-- Qué hace: a cada cuota 7 con DTPRFCPG nula le pone la fecha (EVPRFCHA) del evento VIGENTE de PRECANCELACION o ACUERDO_CONDONACION de su préstamo, SOLO si el
-- préstamo tiene exactamente UN evento vigente de esos tipos y ninguno vigente de NOVACION / REESTRUCTURACION. Si tiene más de uno, ninguno, o lo cerró una
-- sustitución, NO se toca: queda en la lista del bloque 0c para que el cuadre decida (no se inventa una fecha).
-- Orden: 0 controles (solo lectura) -> A respaldo (ESCRIBE) -> B UPDATE (ESCRIBE) -> 1 controles -> COMMIT (comentado) -> REVERSO (comentado).
-- =====================================================================================================================================

-- BLOQUE 0a - Controles previos (solo lectura): cuotas 7 sin fecha por préstamo y mes del evento.
SELECT p.PRSTCDGO AS prestamo, p.PRSTIDAS AS operacion, e.EVPRTPOO AS tipo_evento, TO_CHAR(e.EVPRFCHA, 'YYYY-MM') AS mes_evento,
       COUNT(DISTINCT d.DTPRCDGO) AS cuotas_7_sin_fecha
  FROM CRD.DTPR d
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN CRD.EVPR e ON e.PRSTCDGO = p.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
                      AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION', 'NOVACION', 'REESTRUCTURACION')
 WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL
 GROUP BY p.PRSTCDGO, p.PRSTIDAS, e.EVPRTPOO, TO_CHAR(e.EVPRFCHA, 'YYYY-MM')
 ORDER BY p.PRSTCDGO, mes_evento;

-- BLOQUE 0b - Resumen: cuántas cuotas y préstamos se tocan con este script y cuántos quedan fuera.
SELECT CASE WHEN x.sust > 0 THEN 'FUERA: lo cerró una sustitución (NOVACION/REESTRUCTURACION)'
            WHEN x.eventos = 0 THEN 'FUERA: ningún evento vigente de PRECANCELACION/ACUERDO_CONDONACION'
            WHEN x.eventos > 1 THEN 'FUERA: más de un evento vigente (' || x.eventos || ')'
            ELSE 'SE TOCA: un solo evento vigente' END AS clasificacion,
       COUNT(DISTINCT x.prestamo) AS prestamos, SUM(x.cuotas) AS cuotas
  FROM (SELECT d.PRSTCDGO AS prestamo, COUNT(DISTINCT d.DTPRCDGO) AS cuotas,
               (SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
                   AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION')) AS eventos,
               (SELECT COUNT(*) FROM CRD.EVPR s WHERE s.PRSTCDGO = d.PRSTCDGO AND NVL(s.EVPRESTD, 1) = 1
                   AND s.EVPRTPOO IN ('NOVACION', 'REESTRUCTURACION')) AS sust
          FROM CRD.DTPR d WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL GROUP BY d.PRSTCDGO) x
 GROUP BY CASE WHEN x.sust > 0 THEN 'FUERA: lo cerró una sustitución (NOVACION/REESTRUCTURACION)'
               WHEN x.eventos = 0 THEN 'FUERA: ningún evento vigente de PRECANCELACION/ACUERDO_CONDONACION'
               WHEN x.eventos > 1 THEN 'FUERA: más de un evento vigente (' || x.eventos || ')'
               ELSE 'SE TOCA: un solo evento vigente' END
 ORDER BY 1;

-- BLOQUE 0c - Lista aparte (los que NO se tocan): préstamo, operación, cuotas y eventos vigentes que lo cerraron.
SELECT d.PRSTCDGO AS prestamo, p.PRSTIDAS AS operacion, COUNT(DISTINCT d.DTPRCDGO) AS cuotas_7_sin_fecha,
       (SELECT LISTAGG(e.EVPRTPOO || ' ' || TO_CHAR(e.EVPRFCHA, 'YYYY-MM-DD'), '; ') WITHIN GROUP (ORDER BY e.EVPRFCHA)
          FROM CRD.EVPR e WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
           AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION', 'NOVACION', 'REESTRUCTURACION')) AS eventos_vigentes
  FROM CRD.DTPR d JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL
   AND ((SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
            AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION')) <> 1
        OR EXISTS (SELECT 1 FROM CRD.EVPR s WHERE s.PRSTCDGO = d.PRSTCDGO AND NVL(s.EVPRESTD, 1) = 1
            AND s.EVPRTPOO IN ('NOVACION', 'REESTRUCTURACION')))
 GROUP BY d.PRSTCDGO, p.PRSTIDAS
 ORDER BY d.PRSTCDGO;

-- BLOQUE A - Respaldo (ESCRIBE). Todas las cuotas 7 sin fecha, con su DTPRFCPG de antes (nulo). Si la tabla ya existe, el CREATE falla y no se sigue.
CREATE TABLE CRD.BK_DTPR_FCPG_C48_20261127 AS
SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRFCPG, SYSTIMESTAMP AS RESPALDO_EN
  FROM CRD.DTPR d WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL;
SELECT COUNT(*) AS respaldadas FROM CRD.BK_DTPR_FCPG_C48_20261127;

-- BLOQUE B - UPDATE (ESCRIBE; sin COMMIT). Solo préstamos con exactamente UN evento vigente PRECANCELACION/ACUERDO_CONDONACION y sin sustitución vigente.
UPDATE CRD.DTPR d
   SET d.DTPRFCPG = (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e
                      WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
                        AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION'))
 WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL
   AND d.DTPRCDGO IN (SELECT b.DTPRCDGO FROM CRD.BK_DTPR_FCPG_C48_20261127 b)
   AND (SELECT COUNT(*) FROM CRD.EVPR e WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
           AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION')) = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.EVPR s WHERE s.PRSTCDGO = d.PRSTCDGO AND NVL(s.EVPRESTD, 1) = 1
           AND s.EVPRTPOO IN ('NOVACION', 'REESTRUCTURACION'));

-- BLOQUE 1 - Controles posteriores (solo lectura, dentro de la misma sesión, antes del COMMIT).
-- 1a. Esperado: las cuotas "SE TOCA" del bloque 0b ya no aparecen; quedan solo las de la lista 0c.
SELECT COUNT(*) AS cuotas_7_sin_fecha_que_quedan FROM CRD.DTPR d WHERE d.DTPRESTD = 7 AND d.DTPRFCPG IS NULL;
-- 1b. Esperado: 0 filas (ninguna cuota fechada con una fecha distinta de la de su evento vigente).
SELECT d.DTPRCDGO, d.PRSTCDGO, d.DTPRFCPG
  FROM CRD.DTPR d JOIN CRD.BK_DTPR_FCPG_C48_20261127 b ON b.DTPRCDGO = d.DTPRCDGO
 WHERE d.DTPRFCPG IS NOT NULL
   AND d.DTPRFCPG <> (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = d.PRSTCDGO AND NVL(e.EVPRESTD, 1) = 1
                         AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION'));
-- 1c. Cuotas fechadas por mes de fecha (para el cuadre): cuántas pasan a liquidadas en cada corte.
SELECT TO_CHAR(d.DTPRFCPG, 'YYYY-MM') AS mes, COUNT(*) AS cuotas, COUNT(DISTINCT d.PRSTCDGO) AS prestamos
  FROM CRD.DTPR d JOIN CRD.BK_DTPR_FCPG_C48_20261127 b ON b.DTPRCDGO = d.DTPRCDGO
 WHERE d.DTPRFCPG IS NOT NULL GROUP BY TO_CHAR(d.DTPRFCPG, 'YYYY-MM') ORDER BY 1;

-- COMMIT;   -- solo con el visto bueno del cuadre y después de revisar 1a, 1b y 1c. Si algo no cuadra: ROLLBACK;

-- REVERSO (después del COMMIT; devuelve DTPRFCPG a lo que tenía, que era NULL, desde el respaldo):
-- UPDATE CRD.DTPR d SET d.DTPRFCPG = (SELECT b.DTPRFCPG FROM CRD.BK_DTPR_FCPG_C48_20261127 b WHERE b.DTPRCDGO = d.DTPRCDGO)
--  WHERE d.DTPRCDGO IN (SELECT b.DTPRCDGO FROM CRD.BK_DTPR_FCPG_C48_20261127 b);
-- COMMIT;
-- DROP TABLE CRD.BK_DTPR_FCPG_C48_20261127;   -- solo cuando ya no haga falta el respaldo
