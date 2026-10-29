-- =====================================================================================
-- C17-02 — Control (solo SELECT) del estado de cierres por mes: contable (CNT.PRDO) y cartera (CRD.CRCT),
--          y bitacora (CNT.HCPR). Para correr antes y despues de cada cierre o reapertura.
-- Cuadre Superintendencia, tarea C17 · 2026-10-28 · docs/logica-negocio/cnt/CIERRES-BLOQUEANTES.md
-- SQL PURO, sin escritura. Requiere el C17-01 aplicado. Empresa 1236 (ASOPREP).
-- =====================================================================================

-- 1. Estado por mes (jun-2025 -> dic-2026): mayorizacion, cierre contable y cierre de cartera.
--    ESPERADO tras cerrar un mes M: CONTABLE = 'CERRADO' (PRDOCNCR = 1, PRDOESTD = 2) y CARTERA = 'CERRADO'.
SELECT p.PRDOANNN AS anio, p.PRDOMSSS AS mes, p.PRDOCDGO,
       p.PRDOESTD AS estado_mayorizacion,
       CASE WHEN p.PRDOCNCR = 1 OR p.PRDOESTD = 4 THEN 'CERRADO' ELSE 'ABIERTO' END AS contable,
       p.PRDOUSCN AS usuario_cierre, p.PRDOFCCN AS fecha_cierre,
       CASE WHEN c.CRCTCDGO IS NOT NULL THEN 'CERRADO' ELSE 'ABIERTO' END AS cartera,
       c.CRCTCDGO AS corrida
  FROM CNT.PRDO p
  LEFT JOIN CRD.CRCT c ON c.PJRQCDGO = p.PJRQCDGO AND c.CRCTANOO = p.PRDOANNN AND c.CRCTMESS = p.PRDOMSSS
                      AND c.CRCTIDST = 2 AND c.CRCTESTD = 1
 WHERE p.PJRQCDGO = 1236 AND p.PRDOANNN * 100 + p.PRDOMSSS BETWEEN 202505 AND 202612
 ORDER BY p.PRDOANNN, p.PRDOMSSS;

-- 2. Coherencia: ningun mes con contabilidad cerrada y cartera abierta. ESPERADO: 0 filas.
SELECT p.PRDOANNN, p.PRDOMSSS
  FROM CNT.PRDO p
 WHERE p.PJRQCDGO = 1236 AND p.PRDOCNCR = 1
   AND NOT EXISTS (SELECT 1 FROM CRD.CRCT c WHERE c.PJRQCDGO = p.PJRQCDGO AND c.CRCTANOO = p.PRDOANNN
                    AND c.CRCTMESS = p.PRDOMSSS AND c.CRCTIDST = 2 AND c.CRCTESTD = 1);

-- 3. Coherencia: ningun asiento ACTIVO creado en un periodo despues de su cierre. ESPERADO: 0 filas.
SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTFCPR, p.PRDOFCCN
  FROM CNT.ASNT a JOIN CNT.PRDO p ON p.PRDOCDGO = a.PRDOCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PRDOCNCR = 1 AND a.ASNTFCPR > p.PRDOFCCN;

-- 4. Reversos de asientos de periodos cerrados (anulaciones posteriores al cierre): original y reverso.
SELECT o.ASNTCDGO AS original, o.ASNTFCHA AS fecha_original, o.ASNTESTD AS estado_original,
       o.ASNTUSAN AS usuario_anulacion, o.ASNTMTAN AS motivo,
       r.ASNTCDGO AS reverso, r.ASNTFCHA AS fecha_reverso, r.ASNTNMAL AS numero_reverso
  FROM CNT.ASNT o JOIN CNT.PRDO p ON p.PRDOCDGO = o.PRDOCDGO
  JOIN CNT.ASNT r ON r.ASNTCDGO = o.ASNTASRV
 WHERE p.PJRQCDGO = 1236 AND p.PRDOCNCR = 1 AND o.ASNTESTD = 3
 ORDER BY o.ASNTFCHA;

-- 5. Bitacora de cierres y reaperturas (la mas reciente primero).
SELECT h.HCPRCDGO, h.HCPRTPOO AS tipo, h.HCPRACCN AS accion, h.HCPRANNO AS anio, h.HCPRMESS AS mes,
       h.HCPRUSRO AS usuario, h.HCPRFCHA AS fecha, h.HCPRFRDN AS fuera_de_orden, h.CRCTCDGO, h.PRDOCDGO,
       h.HCPRMTVO AS motivo
  FROM CNT.HCPR h
 WHERE h.PJRQCDGO = 1236
 ORDER BY h.HCPRFCHA DESC;
