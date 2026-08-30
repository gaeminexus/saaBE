-- =====================================================================
-- Verificacion (SOLO SELECT): cuantas filas de LQCC/NTCC/NTDC tienen
-- codSustento (*CSUS) sin resolver hoy
-- =====================================================================
-- Contexto: docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §9
-- (fase 6, extension de codSustento a LQCC/NTCC/NTDC). El agente BE no
-- tiene acceso a la base para confirmar si hace falta backfill como el
-- de FCTC (BACKFILL-SUSTENTO-TRIBUTARIO.sql). Esta consulta lo mide.
--
-- Si (b) y (c) dan 0 filas pendientes, no hace falta backfill para esos
-- documentos -- ya resuelven solos hacia adelante con la logica que
-- acaba de entregar el backend. Si dan filas, aviso al arbitro con los
-- numeros y se escribe el UPDATE correspondiente, mismo patron que
-- BACKFILL-SUSTENTO-TRIBUTARIO-UPDATE.sql.
--
-- NTDC no tiene columna de producto (NTDCCSUS existe pero no hay
-- excepcion por grupo posible -- ver §9.1): se mide igual, solo por la
-- regla base del IVA.
-- =====================================================================

-- (a) LQCC -- liquidacion de compra, estado activo
SELECT COUNT(*) AS LQCC_PENDIENTES
FROM PGS.LQCC
WHERE ESTADO = 1 AND LQCCCSUS IS NULL;

-- (b) NTCC -- nota de credito de compra, estado activo
SELECT COUNT(*) AS NTCC_PENDIENTES
FROM PGS.NTCC
WHERE ESTADO = 1 AND NTCCCSUS IS NULL;

-- (c) NTDC -- nota de debito de compra, estado activo
SELECT COUNT(*) AS NTDC_PENDIENTES
FROM PGS.NTDC
WHERE ESTADO = 1 AND NTDCCSUS IS NULL;

-- (d) Universo total activo por tabla, para tener el denominador junto
--     al numerador de arriba
SELECT 'LQCC' AS TABLA, COUNT(*) AS TOTAL_ACTIVAS FROM PGS.LQCC WHERE ESTADO = 1
UNION ALL
SELECT 'NTCC' AS TABLA, COUNT(*) AS TOTAL_ACTIVAS FROM PGS.NTCC WHERE ESTADO = 1
UNION ALL
SELECT 'NTDC' AS TABLA, COUNT(*) AS TOTAL_ACTIVAS FROM PGS.NTDC WHERE ESTADO = 1;
