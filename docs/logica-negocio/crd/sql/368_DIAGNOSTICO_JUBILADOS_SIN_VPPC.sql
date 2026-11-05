-- =====================================================================================================
-- 368 — DIAGNÓSTICO (SOLO LECTURA): jubilados sin configuración de pensión complementaria (VPPC) activa
-- omen-saa-1-arb · 2026-11-05 · No modifica nada.
--
-- Caso: seguro médico 11/2026 → 177 generados, 4 con error SIN_VALOR_PENSION (entidades 6486, 6518, 6572, 6578).
-- El proceso recorre TODAS las entidades con ENTDIDST = 3 (JUBILADO_COMPLEMENTARIO) y a cada una le exige
-- exactamente UNA fila en CRD.VPPC con VPPCIDST = 1. Sin ella no sabe cuánto es su pensión ni su seguro.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — Las 4 entidades: quiénes son y qué tienen en VPPC (todas las filas, activas o no).
-- Si VPPCCDGO sale vacío: nunca se les configuró la pensión (jubilados nuevos).
-- Si sale con VPPCIDST = 0: tenían configuración y la inactivaron.
-- ---------------------------------------------------------------------------------------------------
SELECT e.ENTDCDGO, e.ENTDNMID AS IDENTIFICACION, e.ENTDRZNS AS NOMBRE, e.ENTDIDST,
       v.VPPCCDGO, v.VPPCIDST, v.VPPCVLRR AS VALOR_PAGAR, v.VPPCVLSR AS VALOR_SEGURO,
       v.VPPCUSRG, v.VPPCFCRG, v.VPPCUSMD, v.VPPCCFMD
FROM   CRD.ENTD e
LEFT JOIN CRD.VPPC v ON v.ENTDCDGO = e.ENTDCDGO
WHERE  e.ENTDCDGO IN (6486, 6518, 6572, 6578)
ORDER BY e.ENTDCDGO, v.VPPCCDGO;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — ¿Cobraron pensión/seguro en meses anteriores? (si nunca, son jubilados nuevos)
-- ---------------------------------------------------------------------------------------------------
SELECT p.ENTDCDGO, p.PGPCANNO, p.PGPCMESS, p.PGPCVLPN, p.PGPCVLSG, p.PGPCVLRR, p.PGPCESTD
FROM   CRD.PGPC p
WHERE  p.ENTDCDGO IN (6486, 6518, 6572, 6578)
ORDER BY p.ENTDCDGO, p.PGPCANNO, p.PGPCMESS;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — Barrido: TODOS los jubilados complementarios con cero o más de una VPPC activa
-- (los que también van a fallar en el proceso de pensiones de fin de mes).
-- ---------------------------------------------------------------------------------------------------
SELECT e.ENTDCDGO, e.ENTDNMID AS IDENTIFICACION, e.ENTDRZNS AS NOMBRE,
       (SELECT COUNT(*) FROM CRD.VPPC v WHERE v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1) AS VPPC_ACTIVAS,
       (SELECT COUNT(*) FROM CRD.VPPC v WHERE v.ENTDCDGO = e.ENTDCDGO)                    AS VPPC_TOTAL
FROM   CRD.ENTD e
WHERE  e.ENTDIDST = 3
AND    (SELECT COUNT(*) FROM CRD.VPPC v WHERE v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1) <> 1
ORDER BY e.ENTDCDGO;
