-- =====================================================================================
-- e3-18 — Orden de desembolso de préstamo «Sin cuenta de destino» aunque el socio sí tiene cuenta
-- Equipo omen-saa-3 · 2026-11-11 · Caso: préstamo 8530 (bandeja de Aprobación de pagos)
--
-- Por qué pasa: al APROBAR el préstamo, la orden de desembolso (PGS.PGTR) COPIA en ese momento
-- banco / tipo / número de la cuenta activa única del socio (CRD.CNBP) a PGTRBFBC / PGTRBFTP /
-- PGTRBFCT (PrestamoServiceImpl.armaBeneficiarioPrestamo). Es una foto: si la cuenta se registró o
-- se corrigió DESPUÉS de aprobar, o si a la cuenta le faltaba el banco o el tipo, la orden queda
-- sin cuenta y la bandeja la marca, aunque hoy el socio sí tenga cuenta.
-- Como el beneficiario no es un titular de TSR, la bandeja no ofrece «Asignar cuenta».
--
-- PASO 1 y 2 son SOLO LECTURA: mandar el resultado al árbitro.
-- PASO 3 (UPDATE) va COMENTADO: correrlo solo cuando el árbitro lo confirme con el resultado.
-- =====================================================================================

-- PASO 1 — La orden del préstamo 8530 y las cuentas del socio.
SELECT g.PGTRCDGO, g.PGTRESTD AS ESTADO_PAGO, g.PGTRIDOR AS PRESTAMO,
       g.PGTRBFBC AS ORDEN_BANCO, g.PGTRBFTP AS ORDEN_TIPO, g.PGTRBFCT AS ORDEN_CUENTA,
       c.CNBPCDGO, c.CNBPIDST AS ESTADO_CUENTA,
       c.BEXTCDGO AS SOCIO_BANCO, c.CNBPTPCN AS SOCIO_TIPO, c.CNBPNMRO AS SOCIO_CUENTA
  FROM PGS.PGTR g
  JOIN CRD.PRST p ON p.PRSTCDGO = g.PGTRIDOR
  LEFT JOIN CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO
 WHERE g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
   AND g.PGTRIDOR = 8530
 ORDER BY c.CNBPCDGO;

-- Cómo leerlo:
--  * ORDEN_* vacíos y una sola fila con ESTADO_CUENTA = 1 y SOCIO_BANCO / SOCIO_TIPO / SOCIO_CUENTA
--    llenos  -> la cuenta se registró o corrigió después de aprobar. Se arregla con el PASO 3.
--  * SOCIO_BANCO o SOCIO_TIPO vacíos -> primero completar la cuenta del socio en la pantalla del
--    partícipe; después el PASO 3.
--  * Dos o más filas con ESTADO_CUENTA = 1 -> dejar una sola activa; después el PASO 3.

-- PASO 2 — ¿Hay más órdenes de desembolso pendientes en la misma situación?
SELECT g.PGTRCDGO, g.PGTRESTD AS ESTADO_PAGO, g.PGTRIDOR AS PRESTAMO,
       g.PGTRBFBC AS ORDEN_BANCO, g.PGTRBFTP AS ORDEN_TIPO, g.PGTRBFCT AS ORDEN_CUENTA
  FROM PGS.PGTR g
 WHERE g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
   AND g.PGTRCTBN IS NULL
   AND (g.PGTRBFBC IS NULL OR g.PGTRBFTP IS NULL OR g.PGTRBFCT IS NULL)
 ORDER BY g.PGTRCDGO;

-- PASO 3 — (COMENTADO) Copiar a la orden la cuenta activa única del socio.
-- Solo órdenes de desembolso sin cuenta, y solo si el socio tiene EXACTAMENTE una cuenta activa completa.
-- Respaldo primero:
-- CREATE TABLE CRD.BK_E3_18_PGTR AS
--   SELECT * FROM PGS.PGTR WHERE PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO' AND PGTRIDOR IN (8530);
--
-- UPDATE PGS.PGTR g
--    SET (g.PGTRBFBC, g.PGTRBFTP, g.PGTRBFCT) =
--        (SELECT c.BEXTCDGO, c.CNBPTPCN, TRIM(c.CNBPNMRO)
--           FROM CRD.PRST p JOIN CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO
--          WHERE p.PRSTCDGO = g.PGTRIDOR AND c.CNBPIDST = 1)
--  WHERE g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
--    AND g.PGTRIDOR IN (8530)
--    AND g.PGTRCTBN IS NULL
--    AND (SELECT COUNT(*) FROM CRD.PRST p JOIN CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO
--          WHERE p.PRSTCDGO = g.PGTRIDOR AND c.CNBPIDST = 1
--            AND c.BEXTCDGO IS NOT NULL AND c.CNBPTPCN IS NOT NULL AND c.CNBPNMRO IS NOT NULL) = 1;
-- -- Esperado: 1 fila. Si sale 0, la cuenta del socio no está completa o hay más de una activa.
-- COMMIT;
