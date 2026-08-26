-- =====================================================================================
-- CARGA — TIPO DE ADJUNTO "CERTIFICADO BANCARIO" (CRD.TPDJ)
-- FECHA: 2026-08-25
--
-- QUE ES: una fila nueva en el catalogo CRD.TPDJ (ya existente, sin cambios de estructura).
-- NO ES DDL: la tabla CRD.TPDJ y CRD.ADJN ya existen y no se tocan. Este script solo
-- inserta el tipo de adjunto que identifica el certificado bancario de una cuenta
-- bancaria de participe (CRD.CNBP).
--
-- POR QUE HACE FALTA: CuentaBancariaParticipeServiceImpl.crearConCertificado (endpoint
-- POST /rest/cnbp/conCertificado) resuelve este tipo BUSCANDO POR NOMBRE
-- ('CERTIFICADO BANCARIO', case-insensitive) — no hay un codigo fijo en el codigo Java,
-- porque el ID real (TPDJCDGO) solo existe despues de correr este script en cada
-- ambiente. Sin esta fila, el endpoint responde
-- TIPO_ADJUNTO_CERTIFICADO_NO_CONFIGURADO (500) en cualquier intento de crear una cuenta.
--
-- NO EJECUTAR SIN REVISAR. Va como script revisable: control antes, INSERT, control
-- despues. Correr por bloques y revisar la salida de cada uno antes de seguir.
-- SQL PURO: sin comandos SQL*Plus (WHENEVER / SET / DEFINE).
--
-- SOLO EN EL BACKEND: este archivo NO se espeja a saaFE (regla del proyecto: los
-- scripts .sql viven solo en saaBE; los documentos de diseno si se espejan, los .sql no).
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROL PREVIO
-- =====================================================================================

-- 1.1 Estado actual del catalogo (para tener una foto de "antes"; no se espera ningun
--     valor en particular, solo confirmar que la tabla existe y ver que hay hoy)
SELECT TPDJCDGO, TPDJNMBR, TPDJIDST FROM CRD.TPDJ ORDER BY TPDJCDGO;

-- 1.2 Que "CERTIFICADO BANCARIO" no exista ya con otra grafia o estado
--     (esperado: 0 filas. Si devuelve algo, NO correr el INSERT — ya existe: solo hay
--     que confirmar que esa fila este ACTIVA (TPDJIDST = 1) y usarla tal cual)
SELECT TPDJCDGO, TPDJNMBR, TPDJIDST FROM CRD.TPDJ
WHERE UPPER(TPDJNMBR) LIKE '%CERTIFICADO%BANCARIO%';


-- =====================================================================================
-- 2. INSERT — 1 fila esperada
-- =====================================================================================

INSERT INTO CRD.TPDJ (TPDJNMBR, TPDJIDST)
VALUES ('CERTIFICADO BANCARIO', 1);


-- =====================================================================================
-- 3. CONTROL POSTERIOR
-- =====================================================================================

-- 3.1 La fila quedo, activa, y es UNICA (esperado: exactamente 1 fila)
SELECT TPDJCDGO, TPDJNMBR, TPDJIDST FROM CRD.TPDJ
WHERE UPPER(TPDJNMBR) = 'CERTIFICADO BANCARIO' AND TPDJIDST = 1;

-- 3.2 Anotar el TPDJCDGO que devolvio el control 3.1 — es informativo nada mas: el
--     backend no lo necesita hardcodeado (lo resuelve por nombre), pero sirve para
--     verificar despues, por ejemplo, cuantos ADJN se generaron con ese tipo:
--     SELECT COUNT(*) FROM CRD.ADJN WHERE TPDJCDGO = <el que devolvio 3.1>;


-- =====================================================================================
-- 4. COMMIT — ejecutar SOLO si el control 3.1 devolvio exactamente 1 fila.
--    Si devolvio 0 o mas de 1: ROLLBACK; y revisar.
-- =====================================================================================

COMMIT;
