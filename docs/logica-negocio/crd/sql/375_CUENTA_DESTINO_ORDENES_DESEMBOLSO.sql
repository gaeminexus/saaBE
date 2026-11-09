-- =====================================================================================
-- 375 — URGENTE. omen-saa-1-arb, 2026-11-09. Cuenta de destino de las órdenes de desembolso de préstamo.
-- Causa (verificada en código): PrestamoServiceImpl.armaBeneficiarioPrestamo arma el beneficiario de la orden
-- CRD_DESEMBOLSO_PRESTAMO solo con nombre y cédula, NUNCA con la cuenta bancaria del socio (CRD.CNBP). La bandeja de
-- aprobación muestra «Sin cuenta de destino» y la aprobación la rechaza. El código se corrige aparte (EAR siguiente);
-- este script completa las órdenes ya creadas, con la cuenta ACTIVA ÚNICA del socio.
-- Solo toca órdenes de desembolso POR APROBAR (0) o REGISTRADAS (1) sin datos de cuenta. Correr bloque por bloque.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES: órdenes afectadas y cuántas cuentas activas tiene cada socio.
-- ESPERADO: las órdenes 1087 y 1088 (y cualquier otra de desembolso sin cuenta) con CUENTAS_ACTIVAS = 1.
-- Si alguna sale con 0 o con más de 1: esa NO se completa (bloque 2 la salta); registrar/dejar una sola cuenta activa
-- del socio en Créditos y volver a correr este script.
SELECT g.PGTRCDGO, g.PGTRESTD, g.PGTRIDOR AS PRESTAMO, p.ENTDCDGO, g.PGTRBFNM, g.PGTRVLOR,
       g.PGTRBFBC, g.PGTRBFTP, g.PGTRBFCT,
       (SELECT COUNT(*) FROM CRD.CNBP c WHERE c.ENTDCDGO = p.ENTDCDGO AND c.CNBPIDST = 1) AS CUENTAS_ACTIVAS
FROM   PGS.PGTR g
JOIN   CRD.PRST p ON p.PRSTCDGO = g.PGTRIDOR
WHERE  g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
AND    g.PGTRESTD IN (0, 1)
AND    g.PGTRCNBC IS NULL
AND    (g.PGTRBFBC IS NULL OR g.PGTRBFTP IS NULL OR g.PGTRBFCT IS NULL)
ORDER  BY g.PGTRCDGO;

-- 0.2 La cuenta que se va a poner (revisar banco, tipo y número contra el certificado del socio).
SELECT g.PGTRCDGO, c.CNBPCDGO, c.BEXTCDGO, c.CNBPTPCN, c.CNBPNMRO
FROM   PGS.PGTR g
JOIN   CRD.PRST p ON p.PRSTCDGO = g.PGTRIDOR
JOIN   CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO AND c.CNBPIDST = 1
WHERE  g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
AND    g.PGTRESTD IN (0, 1)
AND    g.PGTRCNBC IS NULL
AND    (g.PGTRBFBC IS NULL OR g.PGTRBFTP IS NULL OR g.PGTRBFCT IS NULL)
ORDER  BY g.PGTRCDGO;

-- BLOQUE 1 — RESPALDO
CREATE TABLE PGS.PGTR_BK375 AS
SELECT * FROM PGS.PGTR
WHERE  PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO' AND PGTRESTD IN (0, 1);

-- BLOQUE 2 — Completar banco, tipo y número con la cuenta ACTIVA ÚNICA del socio
UPDATE PGS.PGTR g
   SET (g.PGTRBFBC, g.PGTRBFTP, g.PGTRBFCT) =
       (SELECT c.BEXTCDGO, c.CNBPTPCN, c.CNBPNMRO
        FROM   CRD.PRST p
        JOIN   CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO AND c.CNBPIDST = 1
        WHERE  p.PRSTCDGO = g.PGTRIDOR)
 WHERE g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
   AND g.PGTRESTD IN (0, 1)
   AND g.PGTRCNBC IS NULL
   AND (g.PGTRBFBC IS NULL OR g.PGTRBFTP IS NULL OR g.PGTRBFCT IS NULL)
   AND (SELECT COUNT(*) FROM CRD.PRST p JOIN CRD.CNBP c ON c.ENTDCDGO = p.ENTDCDGO AND c.CNBPIDST = 1
        WHERE p.PRSTCDGO = g.PGTRIDOR) = 1;
-- ESPERADO: filas actualizadas = órdenes del bloque 0 con CUENTAS_ACTIVAS = 1. Si difiere: ROLLBACK y avisar.
COMMIT;

-- BLOQUE 3 — CONTROL DESPUÉS (ESPERADO: 0 filas; si sale alguna es un socio sin cuenta activa única)
SELECT g.PGTRCDGO, g.PGTRIDOR, g.PGTRBFNM
FROM   PGS.PGTR g
WHERE  g.PGTRORGN = 'CRD_DESEMBOLSO_PRESTAMO'
AND    g.PGTRESTD IN (0, 1)
AND    g.PGTRCNBC IS NULL
AND    (g.PGTRBFBC IS NULL OR g.PGTRBFTP IS NULL OR g.PGTRBFCT IS NULL);
-- 3.2 Las corregidas (ESPERADO: banco, tipo y número llenos).
SELECT g.PGTRCDGO, g.PGTRBFBC, g.PGTRBFTP, g.PGTRBFCT
FROM   PGS.PGTR g JOIN PGS.PGTR_BK375 b ON b.PGTRCDGO = g.PGTRCDGO
WHERE  b.PGTRBFCT IS NULL OR b.PGTRBFBC IS NULL OR b.PGTRBFTP IS NULL;

-- BLOQUE 4 — Cuando los desembolsos estén confirmados:
-- DROP TABLE PGS.PGTR_BK375;

-- REVERSO (comentado):
-- UPDATE PGS.PGTR g SET (g.PGTRBFBC, g.PGTRBFTP, g.PGTRBFCT) =
--        (SELECT b.PGTRBFBC, b.PGTRBFTP, b.PGTRBFCT FROM PGS.PGTR_BK375 b WHERE b.PGTRCDGO = g.PGTRCDGO)
--  WHERE g.PGTRCDGO IN (SELECT PGTRCDGO FROM PGS.PGTR_BK375);
-- COMMIT;
