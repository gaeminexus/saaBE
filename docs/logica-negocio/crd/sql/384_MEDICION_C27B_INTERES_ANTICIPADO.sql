-- =====================================================================================
-- 384 — MEDICIÓN C27b (D-172): interés cobrado antes de su devengo (SOLO LECTURA)
-- Equipo omen-saa-1 · 2026-11-24 · Prueba 8 del diseño (D5): un caso borde por bloque, con su resultado ESPERADO.
-- Diseño: docs/logica-negocio/crd/C27b-DISENO-INTERES-ANTICIPADO.md · Propiedades: docs/logica-negocio/crd/C27b-PROPIEDADES.md
--
-- No modifica nada. Es reproducible a un corte pasado: se cambian las tres fechas del CTE PARAMS (se repite en cada
-- bloque, copiar el mismo valor en todos) y se corre bloque por bloque. Reproduce la MISMA regla del código:
--   * universo por fecha: PRSTFCHA < corte+1 y PRSTIDST fuera de (0,1,6,7,12,14); «salió» = cancelado terminado al corte;
--   * pago vigente = NVL(PGPRANUL,0)=0 (nunca PGPRESTD);
--   * ④  = DTPRINTR completo de las cuotas del mes que abre − cobrado antes de la ancla (si salió: solo el cobrado
--          con ancla <= PGPRFCHA < corte+1);
--   * ④b = DTPRINTR de las cuotas del mes que se cierra de préstamos concedidos en ese mes;
--   * ⑪ objetivo = Σ PGPRINPG vigente con ancla <= PGPRFCHA < corte+1 de cuotas que vencen desde el día 1 del mes siguiente.
--
-- PARAMS: CORTE = último día del mes que se cierra · ANCLA = fechaCorte de la corrida de arranque
--         (saa.cartera.anclaArranque) · CORTE_TRANS = último corte ANTERIOR a saa.cartera.devengoCompletoDesde.
-- =====================================================================================


-- BLOQUE 1 — ④ por tipo de préstamo: devengo del mes que abre (el que hace el paso ④ nuevo).
-- Esperado: coincide con la línea «INTERES ORDINARIO por cobrar» del asiento ④ de la corrida, por tipo (la mora no entra).
WITH params AS (
    SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL
),
cuotas AS (
    SELECT pr.TPPRCDGO AS tipo, d.DTPRCDGO, d.DTPRINTR, p.PRSTCDGO,
           (SELECT NVL(SUM(CASE WHEN g.PGPRFCHA < pa.ancla THEN NVL(g.PGPRINPG,0) ELSE 0 END),0)
              FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0) AS cobrado_pre,
           (SELECT NVL(SUM(CASE WHEN g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1 THEN NVL(g.PGPRINPG,0) ELSE 0 END),0)
              FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0) AS cobrado_post,
           CASE WHEN NVL(p.PRSTIDST,0) IN (3, 4, 5, 9, 13)
                 AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO
                                  AND NVL(cx.DTPRESTD,0) NOT IN (4, 7, 9))
                 AND NOT EXISTS (SELECT 1 FROM CRD.DTPR cy WHERE cy.PRSTCDGO = p.PRSTCDGO
                                  AND cy.DTPRESTD = 4 AND cy.DTPRFCPG >= pa.corte)
                 AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO
                                  AND NVL(cz.PGPRANUL,0) = 0 AND cz.PGPRFCHA >= pa.corte + 1)
                THEN 1 ELSE 0 END AS salio
      FROM params pa
      JOIN CRD.PRST p ON p.PRSTFCHA < pa.corte + 1 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
      JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
      JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND NVL(d.DTPRESTD,0) <> 9
     WHERE TRUNC(d.DTPRFCVN) BETWEEN pa.corte + 1 AND LAST_DAY(pa.corte + 1)
       AND pr.TPPRCDGO IS NOT NULL
)
SELECT tipo, COUNT(*) AS cuotas, SUM(CASE WHEN salio = 1 THEN 1 ELSE 0 END) AS cuotas_de_prestamos_que_salieron,
       ROUND(SUM(CASE WHEN salio = 1 THEN cobrado_post ELSE GREATEST(NVL(DTPRINTR,0) - cobrado_pre, 0) END), 2) AS devengo_4
  FROM cuotas
 GROUP BY tipo ORDER BY tipo;
-- (La condonación de plazo vencido D25 —préstamos en 8 con cuotas posteriores a su declaración— no se reproduce aquí:
--  si hay préstamos en 8 con PLVN vigente, restar a mano su interés futuro o comparar contra el asiento.)


-- BLOQUE 2 — ④b por tipo: primera cuota de los préstamos concedidos en el mes que se cierra.
-- Esperado: coincide con el asiento ④b (subproceso 11) de la corrida.
WITH params AS (SELECT DATE '2026-10-31' AS corte FROM DUAL)
SELECT pr.TPPRCDGO AS tipo, COUNT(DISTINCT p.PRSTCDGO) AS prestamos, COUNT(*) AS cuotas,
       ROUND(SUM(NVL(d.DTPRINTR,0)), 2) AS devengo_4b
  FROM params pa
  JOIN CRD.PRST p ON p.PRSTFCHA >= TRUNC(pa.corte, 'MM') AND p.PRSTFCHA < pa.corte + 1
                 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND NVL(d.DTPRESTD,0) <> 9
 WHERE TRUNC(d.DTPRFCVN) BETWEEN TRUNC(pa.corte, 'MM') AND pa.corte
   AND pr.TPPRCDGO IS NOT NULL
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;


-- BLOQUE 3 — ⑪ objetivo por tipo al corte.
-- Esperado: objetivo(t) − asentado por los ⑪ vigentes anteriores − base de transición = el asiento ⑪ del corte (por tipo).
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT pr.TPPRCDGO AS tipo, COUNT(*) AS pagos, ROUND(SUM(NVL(g.PGPRINPG,0)), 2) AS objetivo_11
  FROM params pa
  JOIN CRD.PGPR g ON NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1
  JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO AND NVL(d.DTPRESTD,0) <> 9
                 AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.corte, 'MM'), 1)
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO AND p.PRSTFCHA < pa.corte + 1
                 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE pr.TPPRCDGO IS NOT NULL
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;


-- BLOQUE 3b — BASE DE TRANSICIÓN: el objetivo del ⑪ al CORTE_TRANS (último corte anterior a devengoCompletoDesde).
-- Es lo que el cuadre contrasta con lo que dejó su asiento manual C27b; el valor acordado va a la propiedad
-- saa.cartera.anticipadoBaseTransicion = «1:valor;2:valor;3:valor».
WITH params AS (SELECT DATE '2026-09-30' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT pr.TPPRCDGO AS tipo, ROUND(SUM(NVL(g.PGPRINPG,0)), 2) AS objetivo_en_corte_transicion
  FROM params pa
  JOIN CRD.PGPR g ON NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1
  JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO AND NVL(d.DTPRESTD,0) <> 9
                 AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.corte, 'MM'), 1)
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO AND p.PRSTFCHA < pa.corte + 1
                 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
  JOIN CRD.PRDC pr ON pr.PRDCCDGO = p.PRDCCDGO
 WHERE pr.TPPRCDGO IS NOT NULL
 GROUP BY pr.TPPRCDGO ORDER BY pr.TPPRCDGO;


-- BLOQUE 3c — Lo YA asentado por los ⑪ vigentes (después de la primera corrida con el código nuevo), por cuenta.
-- Esperado: 0 filas antes del primer cierre nuevo; después, la Σ por cuenta 1.4.02.xx que usa el código.
SELECT d.DTASCNTA AS cuenta, SUM(NVL(d.DTASDBEE,0)) - SUM(NVL(d.DTASHBRR,0)) AS asentado_11
  FROM CRD.ANCC a
  JOIN CRD.CRCT c ON c.CRCTCDGO = a.CRCTCDGO
  JOIN CNT.ASNT s ON s.ASNTCDGO = a.ANCCASNT
  JOIN CNT.DTAS d ON d.ASNTCDGO = s.ASNTCDGO
 WHERE a.ANCCTPOO = 12 AND a.ANCCIDST = 1 AND c.CRCTIDST = 2 AND s.ASNTESTD = 1
   AND d.DTASCNTA LIKE '1.4.02%'
 GROUP BY d.DTASCNTA ORDER BY d.DTASCNTA;


-- =====================================================================================
-- CASOS BORDE — uno por bloque, con su resultado esperado. CORTE = DATE '2026-10-31', ANCLA = DATE '2025-05-31'.
-- =====================================================================================

-- CASO 1 — La cuota que vence el DÍA del corte.
-- Esperado: aporta 0 al ④ y 0 al ⑪ de ESTE corte (vence el último día del mes que se cierra: pertenece al mes que
-- ya se devengó en el ④ anterior y NO es «mes siguiente» para el ⑪). Las dos columnas deben dar 0.
WITH params AS (SELECT DATE '2026-10-31' AS corte FROM DUAL)
SELECT d.PRSTCDGO, d.DTPRNMCT, TRUNC(d.DTPRFCVN) AS vence, d.DTPRINTR,
       CASE WHEN TRUNC(d.DTPRFCVN) BETWEEN pa.corte + 1 AND LAST_DAY(pa.corte + 1) THEN 'ENTRA' ELSE 'no entra' END AS en_4_de_este_corte,
       CASE WHEN TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.corte, 'MM'), 1) THEN 'ENTRA' ELSE 'no entra' END AS en_11_de_este_corte
  FROM params pa JOIN CRD.DTPR d ON TRUNC(d.DTPRFCVN) = pa.corte
 WHERE ROWNUM <= 20;

-- CASO 2 — Cuota pagada dos meses antes (ya pagada cuando abre su mes).
-- Esperado: entra al ④ con DTPRINTR completo MENOS lo cobrado antes de la ancla (si se pagó después de la ancla, el
-- completo). Antes del arreglo la cuota pagada no entraba (el filtro «pendiente» la sacaba). Columna devenga_4 > 0.
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT d.PRSTCDGO, d.DTPRNMCT, TRUNC(d.DTPRFCVN) AS vence, d.DTPRESTD, TRUNC(d.DTPRFCPG) AS pagada, d.DTPRINTR,
       (SELECT NVL(SUM(g.PGPRINPG),0) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0
           AND g.PGPRFCHA < pa.ancla) AS cobrado_antes_ancla,
       GREATEST(NVL(d.DTPRINTR,0) - (SELECT NVL(SUM(g.PGPRINPG),0) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO
           AND NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA < pa.ancla), 0) AS devenga_4
  FROM params pa JOIN CRD.DTPR d ON d.DTPRESTD = 4 AND TRUNC(d.DTPRFCVN) BETWEEN pa.corte + 1 AND LAST_DAY(pa.corte + 1)
                                AND d.DTPRFCPG < ADD_MONTHS(pa.corte, -1) + 1
 WHERE ROWNUM <= 20;

-- CASO 3 — Cancelado después de pagar cuotas futuras (préstamo que SALIÓ del universo).
-- Esperado: aporta al ④ solo lo cobrado de esas cuotas (columna cobrado_hasta_corte) y NO el DTPRINTR completo; su
-- interés cobrado de cuotas que vencen desde el mes siguiente entra al ⑪ (columna cobrado_para_11).
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT p.PRSTIDAS AS operacion, p.PRSTIDST AS estado, d.DTPRNMCT, TRUNC(d.DTPRFCVN) AS vence, d.DTPRINTR,
       NVL(SUM(CASE WHEN g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1 THEN g.PGPRINPG END),0) AS cobrado_hasta_corte,
       NVL(SUM(CASE WHEN g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1
                     AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.corte, 'MM'), 1) THEN g.PGPRINPG END),0) AS cobrado_para_11
  FROM params pa
  JOIN CRD.PRST p ON p.PRSTIDST IN (3, 4, 5, 13) AND p.PRSTFCHA < pa.corte + 1
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND TRUNC(d.DTPRFCVN) > pa.corte
  JOIN CRD.PGPR g ON g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0
 WHERE NOT EXISTS (SELECT 1 FROM CRD.DTPR cx WHERE cx.PRSTCDGO = p.PRSTCDGO AND NVL(cx.DTPRESTD,0) NOT IN (4, 7, 9))
   AND NOT EXISTS (SELECT 1 FROM CRD.PGPR cz WHERE cz.PRSTCDGO = p.PRSTCDGO AND NVL(cz.PGPRANUL,0) = 0 AND cz.PGPRFCHA >= pa.corte + 1)
 GROUP BY p.PRSTIDAS, p.PRSTIDST, d.DTPRNMCT, d.DTPRFCVN, d.DTPRINTR
HAVING NVL(SUM(g.PGPRINPG),0) > 0
 ORDER BY 1, 3;

-- CASO 4 — Cobro anticipado ANULADO.
-- Esperado: sus pagos (PGPRANUL = 1) NO suman al objetivo del ⑪ ni restan al ④. Columna interes_anulado > 0 y
-- columna interes_vigente = lo que sí cuenta. Si el cobro se anuló después del cierre, el ⑪ del mes siguiente baja solo.
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT p.PRSTIDAS AS operacion, d.DTPRNMCT, TRUNC(d.DTPRFCVN) AS vence,
       SUM(CASE WHEN NVL(g.PGPRANUL,0) <> 0 THEN g.PGPRINPG ELSE 0 END) AS interes_anulado,
       SUM(CASE WHEN NVL(g.PGPRANUL,0) = 0 THEN g.PGPRINPG ELSE 0 END)  AS interes_vigente
  FROM params pa
  JOIN CRD.PGPR g ON g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1
  JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.corte, 'MM'), 1)
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 GROUP BY p.PRSTIDAS, d.DTPRNMCT, d.DTPRFCVN
HAVING SUM(CASE WHEN NVL(g.PGPRANUL,0) <> 0 THEN g.PGPRINPG ELSE 0 END) > 0
 ORDER BY 1, 2;

-- CASO 5 — Operación 65860, cuota 94. (Cambiar la operación y la cuota para otros casos.)
-- Esperado: el cuadre dice el valor; esta consulta muestra, para esa cuota, el DTPRINTR, lo cobrado antes y después
-- de la ancla, lo anulado y qué aporta al ④ y al ⑪ del corte.
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla, 65860 AS operacion, 94 AS cuota FROM DUAL)
SELECT p.PRSTIDAS AS operacion, p.PRSTIDST AS estado, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence, d.DTPRESTD,
       TRUNC(d.DTPRFCPG) AS pagada, d.DTPRINTR,
       NVL(SUM(CASE WHEN NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA <  pa.ancla THEN g.PGPRINPG END),0) AS cobrado_antes_ancla,
       NVL(SUM(CASE WHEN NVL(g.PGPRANUL,0) = 0 AND g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1 THEN g.PGPRINPG END),0) AS cobrado_ancla_a_corte,
       NVL(SUM(CASE WHEN NVL(g.PGPRANUL,0) <> 0 THEN g.PGPRINPG END),0) AS anulado
  FROM params pa
  JOIN CRD.PRST p ON p.PRSTIDAS = pa.operacion
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND d.DTPRNMCT = pa.cuota
  LEFT JOIN CRD.PGPR g ON g.DTPRCDGO = d.DTPRCDGO
 GROUP BY p.PRSTIDAS, p.PRSTIDST, d.DTPRNMCT, d.DTPRFCVN, d.DTPRESTD, d.DTPRFCPG, d.DTPRINTR;

-- CASO 6 — Operación 66940: abono a capital de septiembre de 2025 (esperado 84,75).
-- Esperado: Σ DTPRINTR de las cuotas historizadas (HDTP) con vencimiento hasta el límite de devengo, MENOS Σ DTPRINTR de
-- las cuotas nuevas del mismo rango = el reverso (84,75). El límite = último día del mes siguiente al corte de la última
-- corrida EJECUTADA antes del abono (aquí: corte de agosto de 2025 → límite 30-sep-2025). Este reverso solo existe si
-- esa corrida tiene corte >= saa.cartera.devengoCompletoDesde; antes de esa fecha el código no lo emite.
WITH params AS (SELECT 66940 AS operacion, DATE '2025-09-30' AS limite FROM DUAL),
abono AS (
    SELECT e.EVPRCDGO, e.PRSTCDGO, e.EVPRFCHA
      FROM params pa JOIN CRD.PRST p ON p.PRSTIDAS = pa.operacion
      JOIN CRD.EVPR e ON e.PRSTCDGO = p.PRSTCDGO AND e.EVPRTPOO = 'ABONO_CAPITAL' AND e.EVPRESTD = 1
)
SELECT a.EVPRCDGO, TRUNC(a.EVPRFCHA) AS fecha_abono,
       (SELECT NVL(SUM(h.DTPRINTR),0) FROM CRD.HDTP h WHERE h.EVPRCDGO = a.EVPRCDGO
           AND TRUNC(h.DTPRFCVN) <= pa.limite) AS interes_antes,
       (SELECT NVL(SUM(d.DTPRINTR),0) FROM CRD.DTPR d WHERE d.PRSTCDGO = a.PRSTCDGO
           AND d.DTPRNMCT >= (SELECT MIN(h.DTPRNMCT) FROM CRD.HDTP h WHERE h.EVPRCDGO = a.EVPRCDGO)
           AND TRUNC(d.DTPRFCVN) <= pa.limite) AS interes_despues,
       (SELECT NVL(SUM(h.DTPRINTR),0) FROM CRD.HDTP h WHERE h.EVPRCDGO = a.EVPRCDGO AND TRUNC(h.DTPRFCVN) <= pa.limite)
     - (SELECT NVL(SUM(d.DTPRINTR),0) FROM CRD.DTPR d WHERE d.PRSTCDGO = a.PRSTCDGO
           AND d.DTPRNMCT >= (SELECT MIN(h.DTPRNMCT) FROM CRD.HDTP h WHERE h.EVPRCDGO = a.EVPRCDGO)
           AND TRUNC(d.DTPRFCVN) <= pa.limite) AS reverso_esperado_84_75
  FROM params pa, abono a;

-- CASO 7 — Préstamo nuevo con la primera cuota en su mes.
-- Esperado: la cuota entra al ④b del corte de su mes (devenga_4b = DTPRINTR) y NO estuvo en el ④ del mes anterior
-- (el préstamo tenía PRSTFCHA posterior al corte anterior, columna en_universo_corte_anterior = 'NO').
WITH params AS (SELECT DATE '2026-10-31' AS corte FROM DUAL)
SELECT p.PRSTIDAS AS operacion, TRUNC(p.PRSTFCHA) AS concedido, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence, d.DTPRINTR AS devenga_4b,
       CASE WHEN p.PRSTFCHA < TRUNC(pa.corte, 'MM') THEN 'SI' ELSE 'NO' END AS en_universo_corte_anterior
  FROM params pa
  JOIN CRD.PRST p ON p.PRSTFCHA >= TRUNC(pa.corte, 'MM') AND p.PRSTFCHA < pa.corte + 1
                 AND NVL(p.PRSTIDST,0) NOT IN (0, 1, 6, 7, 12, 14)
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND NVL(d.DTPRESTD,0) <> 9
                 AND TRUNC(d.DTPRFCVN) BETWEEN TRUNC(pa.corte, 'MM') AND pa.corte
 WHERE ROWNUM <= 20
 ORDER BY 1, 3;

-- CASO 8 — Precancelado DESPUÉS del corte y ANTES de correr el cierre de ese mes.
-- Esperado: el préstamo sigue en el universo por fecha (la operación es posterior al corte), pero sus cuotas del mes que
-- abre ya están en estado 7: el ④ las devenga SOLO por lo cobrado (columna devenga_4 = cobrado_ancla_a_corte, normalmente
-- 0), no el DTPRINTR completo (columna interes_completo). Si el cierre corrió ANTES de la precancelación, devengó
-- completo y el reverso de la precancelación lo corrige (lineasReversoCuotasCanceladas).
WITH params AS (SELECT DATE '2026-10-31' AS corte, DATE '2025-05-31' AS ancla FROM DUAL)
SELECT p.PRSTIDAS AS operacion, p.PRSTIDST AS estado, d.DTPRNMCT AS cuota, TRUNC(d.DTPRFCVN) AS vence, d.DTPRESTD,
       d.DTPRINTR AS interes_completo,
       (SELECT NVL(SUM(g.PGPRINPG),0) FROM CRD.PGPR g WHERE g.DTPRCDGO = d.DTPRCDGO AND NVL(g.PGPRANUL,0) = 0
           AND g.PGPRFCHA >= pa.ancla AND g.PGPRFCHA < pa.corte + 1) AS devenga_4,
       (SELECT MAX(e.EVPRFCHA) FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
           AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION')) AS fecha_operacion
  FROM params pa
  JOIN CRD.PRST p ON p.PRSTFCHA < pa.corte + 1 AND NVL(p.PRSTIDST,0) IN (3, 4)
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO AND d.DTPRESTD = 7
                 AND TRUNC(d.DTPRFCVN) BETWEEN pa.corte + 1 AND LAST_DAY(pa.corte + 1)
 WHERE EXISTS (SELECT 1 FROM CRD.EVPR e WHERE e.PRSTCDGO = p.PRSTCDGO AND e.EVPRESTD = 1
                  AND e.EVPRTPOO IN ('PRECANCELACION', 'ACUERDO_CONDONACION') AND e.EVPRFCHA >= pa.corte + 1)
   AND ROWNUM <= 20
 ORDER BY 1, 3;


-- CASO 9 — Precancelación por VENTANILLA (CBCR) con cuotas futuras de un mes ya devengado (lote 1b).
-- Esperado: por cada préstamo, el reverso esperado = Σ DTPRINTR de las cuotas en estado 7 que esa precancelación cerró
-- (vencimiento posterior a la fecha de pago, entre el 1.º del mes siguiente al corte de saa.cartera.devengoCompletoDesde y
-- el límite = último día del mes siguiente a la última corrida EJECUTADA) menos lo cobrado. Ese valor tiene que estar como
-- D 5.1.02 / H 1.4.02 dentro del asiento definitivo (CBCRASN2) del cobro: columna reverso_en_asiento. Si el cobro se reversa,
-- el asiento y el reverso se anulan juntos. PARAMS: DESDE = devengoCompletoDesde; LIMITE = último día del mes siguiente al
-- corte de la última corrida EJECUTADA.
WITH params AS (SELECT DATE '2026-10-31' AS desde, DATE '2026-12-31' AS limite FROM DUAL),
cuotas AS (
    SELECT e.EVPRCDGO, e.PRSTCDGO, TRUNC(e.EVPRFCHA) AS fecha_evento, SUM(NVL(d.DTPRINTR, 0)) AS interes_no_cobrado,
           COUNT(*) AS cuotas
      FROM params pa
      JOIN CRD.EVPR e ON e.EVPRTPOO = 'PRECANCELACION' AND e.EVPRESTD = 1
      JOIN CRD.DTPR d ON d.PRSTCDGO = e.PRSTCDGO AND d.DTPRESTD = 7 AND TRUNC(d.DTPRFCVN) > TRUNC(e.EVPRFCHA)
                     AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.desde, 'MM'), 1) AND TRUNC(d.DTPRFCVN) <= pa.limite
     WHERE EXISTS (SELECT 1 FROM CRD.DCBC x WHERE x.EVPRCDGO = e.EVPRCDGO)
     GROUP BY e.EVPRCDGO, e.PRSTCDGO, TRUNC(e.EVPRFCHA)
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.EVPRCDGO AS evento, c.fecha_evento, c.cuotas,
       ROUND(c.interes_no_cobrado, 2) AS reverso_esperado,
       (SELECT ROUND(NVL(SUM(NVL(l.DTASDBEE, 0)), 0), 2)
          FROM CRD.DCBC x JOIN CRD.CBCR b ON b.CBCRCDGO = x.CBCRCDGO
          JOIN CNT.DTAS l ON l.ASNTCDGO = b.CBCRASN2
         WHERE x.EVPRCDGO = c.EVPRCDGO AND l.DTASDSCR LIKE '%reverso del devengo del inter_s de cuotas canceladas%') AS reverso_en_asiento
  FROM cuotas c JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
 ORDER BY c.fecha_evento, c.EVPRCDGO;

-- CASO 10 — Acuerdo de condonación con cuotas futuras de un mes ya devengado (lote 1b).
-- Esperado: igual que el caso 9, pero el reverso va dentro del asiento de la condonación (EVPR.EVPRNMAS), junto con el
-- cruce de aportes si es 100 % aportes. Las cuotas exigibles (vencimiento <= fecha del acuerdo) NO entran: su interés va en el
-- desglose y se paga o se condona.
WITH params AS (SELECT DATE '2026-10-31' AS desde, DATE '2026-12-31' AS limite FROM DUAL),
cuotas AS (
    SELECT e.EVPRCDGO, e.PRSTCDGO, e.EVPRNMAS, TRUNC(e.EVPRFCHA) AS fecha_acuerdo, SUM(NVL(d.DTPRINTR, 0)) AS interes_no_cobrado,
           COUNT(*) AS cuotas
      FROM params pa
      JOIN CRD.EVPR e ON e.EVPRTPOO = 'ACUERDO_CONDONACION' AND e.EVPRESTD = 1
      JOIN CRD.DTPR d ON d.PRSTCDGO = e.PRSTCDGO AND d.DTPRESTD = 7 AND TRUNC(d.DTPRFCVN) > TRUNC(e.EVPRFCHA)
                     AND TRUNC(d.DTPRFCVN) >= ADD_MONTHS(TRUNC(pa.desde, 'MM'), 1) AND TRUNC(d.DTPRFCVN) <= pa.limite
     GROUP BY e.EVPRCDGO, e.PRSTCDGO, e.EVPRNMAS, TRUNC(e.EVPRFCHA)
)
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, c.EVPRCDGO AS evento, c.fecha_acuerdo, c.cuotas, c.EVPRNMAS AS asiento_evento,
       ROUND(c.interes_no_cobrado, 2) AS reverso_esperado,
       (SELECT ROUND(NVL(SUM(NVL(l.DTASDBEE, 0)), 0), 2) FROM CNT.DTAS l
         WHERE l.ASNTCDGO = c.EVPRNMAS AND l.DTASDSCR LIKE '%reverso del devengo del inter_s de cuotas canceladas%') AS reverso_en_asiento
  FROM cuotas c JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
 ORDER BY c.fecha_acuerdo, c.EVPRCDGO;

-- BLOQUE 4 de detección — DOBLE REVERSO por re-ejecución del cierre (restricción operativa, ver C27b-PROPIEDADES.md).
-- Se reversa una corrida (REVERSADA) y se vuelve a ejecutar DESPUÉS de una cancelación que ya había reversado su devengo:
-- el ④ nuevo ve la cuota en 7 y devenga solo lo cobrado, y el reverso de la cancelación (hecho contra el ④ viejo) queda vivo.
-- Esta consulta lista los asientos ACTIVOS de reverso de devengo (cancelaciones y abonos, por la descripción que arma
-- DevengoCompletoServiceImpl) con una corrida REVERSADA anterior a su fecha en el mes anterior y una corrida EJECUTADA vigente
-- del mismo período registrada DESPUÉS del asiento. Cada fila es un posible doble reverso a revisar a mano antes de
-- re-ejecutar o de cerrar el mes.
SELECT a.ASNTCDGO AS asiento, a.ASNTNMRO AS numero_asiento, TRUNC(a.ASNTFCHA) AS fecha_asiento,
       r.CRCTCDGO AS corrida_reversada, r.CRCTANOO AS anio, r.CRCTMESS AS mes, TRUNC(r.CRCTFCCR) AS corte_reversada,
       n.CRCTCDGO AS corrida_nueva, n.CRCTFCRG AS registro_corrida_nueva,
       (SELECT ROUND(SUM(NVL(d.DTASDBEE, 0)), 2) FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO
           AND d.DTASDSCR LIKE '%reverso del devengo del inter_s%') AS reverso
  FROM CNT.ASNT a
  JOIN CRD.CRCT r ON r.CRCTIDST = 3 AND r.CRCTFCCR < a.ASNTFCHA AND r.CRCTFCCR >= ADD_MONTHS(TRUNC(a.ASNTFCHA, 'MM'), -1) - 1
  JOIN CRD.CRCT n ON n.CRCTANOO = r.CRCTANOO AND n.CRCTMESS = r.CRCTMESS AND n.CRCTIDST = 2
                 AND n.CRCTFCRG > a.ASNTFCHA
 WHERE a.ASNTESTD = 1
   AND EXISTS (SELECT 1 FROM CNT.DTAS d WHERE d.ASNTCDGO = a.ASNTCDGO AND d.DTASDSCR LIKE '%reverso del devengo del inter_s%')
 ORDER BY a.ASNTFCHA, a.ASNTCDGO;
