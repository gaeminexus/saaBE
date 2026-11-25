-- =====================================================================================
-- 387 — MEDICIÓN (SOLO LECTURA): asientos de condonación que siguen ACTIVOS aunque el acuerdo se reversó (lote 1c)
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada. Se puede correr en producción.
--
-- El defecto: AcuerdoCondonacionServiceImpl.generarAsientoCondonacion (y, en el acuerdo 100% aportes, el cruce de aportes)
-- no guarda su asiento en el evento del acuerdo (EVPR.EVPRNMAS), así que reversar el cobro (o anular la operación) anula
-- el pago y restaura las cuotas, pero el asiento queda ACTIVO. No hay vínculo por columna: se reconoce por su observación
-- («Condonación del acuerdo N - préstamo M», «Cruce de aportes - acuerdo N - préstamo M»), que es como los genera el código.
--
-- Desde el lote 1c el asiento de la condonación queda en EVPR.EVPRNMAS (en el acuerdo 100 % aportes lleva también el
-- cruce de aportes) y reversar el cobro o anular la operación lo anula. Este script mide lo que quedó vivo ANTES del lote.
-- Procedimiento de regularización: docs/logica-negocio/crd/REGULARIZACION-ASIENTOS-CONDONACION-1C.md (sin ejecutar).
--
-- Cómo leerlo: el BLOQUE 1 es lo que hay que regularizar (asiento activo de un acuerdo que ya no está APLICADO o cuyo
-- evento está anulado). El BLOQUE 2 son acuerdos con MÁS de un asiento de condonación activo (reabierto y vuelto a aplicar
-- sin anular el primero: cuenta doble). El BLOQUE 3 es lo mismo que el 1 para el cruce de aportes del acuerdo 100% aportes.
-- Estados del acuerdo (ACCNESTD): 1 VIGENTE, 2 APLICADO, 3 ANULADO. Asiento (ASNTESTD): 1 activo, 2 anulado, 3 reversado.
-- =====================================================================================


-- BLOQUE 1 — Asientos de CONDONACIÓN activos de acuerdos reversados.
WITH ast AS (
    SELECT a.ASNTCDGO, a.ASNTNMRO, a.ASNTFCHA, a.PJRQCDGO, a.ASNTOBSR,
           TO_NUMBER(REGEXP_SUBSTR(a.ASNTOBSR, 'Condonaci.n del acuerdo ([0-9]+)', 1, 1, 'i', 1)) AS acuerdo,
           (SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO) AS valor
      FROM CNT.ASNT a
     WHERE a.ASNTESTD = 1
       AND REGEXP_LIKE(a.ASNTOBSR, '^Condonaci.n del acuerdo [0-9]+ - pr.stamo', 'i')
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.ACCNCDGO AS acuerdo, c.ACCNESTD AS estado_acuerdo,
       TRUNC(c.ACCNFCHA) AS fecha_acuerdo, ast.ASNTCDGO AS asiento, ast.ASNTNMRO AS numero_asiento,
       TRUNC(ast.ASNTFCHA) AS fecha_asiento, ROUND(ast.valor, 2) AS valor,
       e.EVPRCDGO AS evento, e.EVPRESTD AS estado_evento,
       CASE WHEN c.ACCNESTD <> 2 THEN 'ACUERDO YA NO APLICADO' ELSE 'EVENTO ANULADO' END AS motivo
  FROM ast
  JOIN CRD.ACCN c ON c.ACCNCDGO = ast.acuerdo
  JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = c.EVPRCDGO
 WHERE c.ACCNESTD <> 2 OR e.EVPRESTD = 0
 ORDER BY ast.ASNTFCHA, c.ACCNCDGO;


-- BLOQUE 2 — Acuerdos con más de un asiento de condonación ACTIVO (el primero debió anularse al reversar).
SELECT c.ACCNCDGO AS acuerdo, NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.ACCNESTD AS estado_acuerdo,
       COUNT(*) AS asientos_activos, LISTAGG(a.ASNTNMRO, ', ') WITHIN GROUP (ORDER BY a.ASNTCDGO) AS numeros_asiento
  FROM CNT.ASNT a
  JOIN CRD.ACCN c ON c.ACCNCDGO = TO_NUMBER(REGEXP_SUBSTR(a.ASNTOBSR, 'Condonaci.n del acuerdo ([0-9]+)', 1, 1, 'i', 1))
  JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
 WHERE a.ASNTESTD = 1
   AND REGEXP_LIKE(a.ASNTOBSR, '^Condonaci.n del acuerdo [0-9]+ - pr.stamo', 'i')
 GROUP BY c.ACCNCDGO, p.PRSTIDAS, p.PRSTCDGO, c.ACCNESTD
HAVING COUNT(*) > 1
 ORDER BY 1;


-- BLOQUE 3 — Asientos de CRUCE DE APORTES activos del acuerdo 100% aportes (sin CBCR) reversado.
WITH ast AS (
    SELECT a.ASNTCDGO, a.ASNTNMRO, a.ASNTFCHA,
           TO_NUMBER(REGEXP_SUBSTR(a.ASNTOBSR, 'Cruce de aportes - acuerdo ([0-9]+)', 1, 1, 'i', 1)) AS acuerdo,
           (SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO) AS valor
      FROM CNT.ASNT a
     WHERE a.ASNTESTD = 1 AND REGEXP_LIKE(a.ASNTOBSR, '^Cruce de aportes - acuerdo [0-9]+', 'i')
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.ACCNCDGO AS acuerdo, c.ACCNESTD AS estado_acuerdo,
       ast.ASNTCDGO AS asiento, ast.ASNTNMRO AS numero_asiento, TRUNC(ast.ASNTFCHA) AS fecha_asiento,
       ROUND(ast.valor, 2) AS valor, e.EVPRESTD AS estado_evento
  FROM ast
  JOIN CRD.ACCN c ON c.ACCNCDGO = ast.acuerdo AND c.CBCRCDGO IS NULL
  JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = c.EVPRCDGO
 WHERE c.ACCNESTD <> 2 OR e.EVPRESTD = 0
 ORDER BY ast.ASNTFCHA, c.ACCNCDGO;


-- BLOQUE 4 — Totales del BLOQUE 1 por mes del asiento: cuánto hay que regularizar y en qué períodos.
WITH ast AS (
    SELECT a.ASNTCDGO, a.ASNTFCHA,
           TO_NUMBER(REGEXP_SUBSTR(a.ASNTOBSR, 'Condonaci.n del acuerdo ([0-9]+)', 1, 1, 'i', 1)) AS acuerdo,
           (SELECT SUM(NVL(d.DTASDBEE, 0)) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO) AS valor
      FROM CNT.ASNT a
     WHERE a.ASNTESTD = 1 AND REGEXP_LIKE(a.ASNTOBSR, '^Condonaci.n del acuerdo [0-9]+ - pr.stamo', 'i')
)
SELECT TO_CHAR(ast.ASNTFCHA, 'YYYY-MM') AS mes_asiento, COUNT(*) AS asientos, ROUND(SUM(ast.valor), 2) AS valor_total
  FROM ast
  JOIN CRD.ACCN c ON c.ACCNCDGO = ast.acuerdo
  LEFT JOIN CRD.EVPR e ON e.EVPRCDGO = c.EVPRCDGO
 WHERE c.ACCNESTD <> 2 OR e.EVPRESTD = 0
 GROUP BY TO_CHAR(ast.ASNTFCHA, 'YYYY-MM')
 ORDER BY 1;


-- BLOQUE 5 — Acuerdos 100 % aportes (sin CBCR) con el evento ANULADO que siguen APLICADOS.
-- Es el defecto que anularOperacion no corregía: la operación se anuló, las cuotas y los pagos se restauraron, pero el
-- acuerdo quedó APLICADO y enlazado a un evento muerto. Con el lote 1c anularOperacion lo vuelve a VIGENTE; los que ya
-- existen hay que regularizarlos (ver C27b/1c: 387_REGULARIZACION_ASIENTOS_CONDONACION.md).
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.ACCNCDGO AS acuerdo, c.ACCNESTD AS estado_acuerdo,
       TRUNC(c.ACCNFCHA) AS fecha_acuerdo, ROUND(NVL(c.ACCNVLPG, 0), 2) AS valor_pagar, ROUND(NVL(c.ACCNVLCN, 0), 2) AS valor_condonar,
       e.EVPRCDGO AS evento, e.EVPRESTD AS estado_evento, TRUNC(e.EVPRFCAN) AS fecha_anulacion_evento
  FROM CRD.ACCN c
  JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
  JOIN CRD.EVPR e ON e.EVPRCDGO = c.EVPRCDGO
 WHERE c.CBCRCDGO IS NULL AND c.ACCNESTD = 2 AND e.EVPRESTD = 0
 ORDER BY c.ACCNFCHA, c.ACCNCDGO;
