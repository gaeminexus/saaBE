-- =====================================================================================
-- 228 — El catálogo CRD.ESPR y cómo se reparten hoy los estados de las entidades
-- FECHA: 2026-09-16   EQUIPO: omen-saa-1
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Tres SELECT, sin parámetros. Se corre de corrido.
--
-- POR QUÉ: el CSV del informe mensual de partícipes trae «Estado 1» Y «Estado 2». El «Estado 2»
-- no encaja con el diagnóstico: con el código viejo, el estado de alterno 2 (CESANTE) era el
-- único que se resolvía bien, porque su PK también vale 2 según
-- docs/logica-negocio/crd/MIGRACION-ESTADO-PARTICIPE.md. Que salga como número significa que el
-- catálogo de producción NO es igual a esa tabla: o la fila de PK 2 ya no existe, o su nombre
-- está vacío, o los alternos cambiaron.
--
-- Esto hay que saberlo ANTES de correr el 227, porque ese UPDATE resuelve el nombre por el
-- alterno (ESPRCDEX): si el catálogo no tiene alternos completos, deja filas sin corregir.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- BLOQUE 1 — El catálogo completo, tal como está hoy
--
-- Comparar contra la tabla de la migración: PK 10=ACTIVO(alt 1) · 2=CESANTE(2) ·
-- 30=JUBILADO COMPLEMENTARIO(3) · 23=CESANTE DESAFILIADO(4) · 40=CESANTE FALLECIDO(5) ·
-- 41=JUBILADO APORTANTE(6) · 42=JUBILADO PASIVO(7) · 62=ACTIVO EN MORA(8) · 63=NUEVO(9).
-- Un ESPRCDEX nulo o un ESPRNMBR vacío explican por sí solos un «Estado N».
-- =====================================================================================
SELECT  p.ESPRCDGO          AS pk,
        p.ESPRNMBR          AS nombre,
        p.ESPRCDEX          AS codigo_alterno,
        p.ESPRIDST          AS estado_del_registro
FROM    CRD.ESPR p
ORDER BY p.ESPRCDEX NULLS LAST, p.ESPRCDGO;


-- =====================================================================================
-- BLOQUE 2 — Qué guarda hoy CRD.ENTD.ENTDIDST, y si resuelve por alterno o por PK
--
-- Esperado tras la migración del 11-08: todas las filas resuelven por ALTERNO.
-- Si alguna resuelve sólo por PK, la migración no terminó y hay que decidir antes de tocar nada.
-- =====================================================================================
SELECT  e.ENTDIDST                        AS valor_en_entidad,
        COUNT(*)                          AS entidades,
        MAX(pa.ESPRNMBR)                  AS nombre_por_alterno,
        MAX(pk.ESPRNMBR)                  AS nombre_por_pk
FROM    CRD.ENTD e
LEFT JOIN CRD.ESPR pa    ON pa.ESPRCDEX = e.ENTDIDST
LEFT JOIN CRD.ESPR pk    ON pk.ESPRCDGO = e.ENTDIDST
GROUP BY e.ENTDIDST
ORDER BY 1;


-- =====================================================================================
-- BLOQUE 3 — Las filas del informe que hoy muestran un número, y si el 227 las va a resolver
-- =====================================================================================
SELECT  c.CPRMSTEN                        AS estado_en_el_informe,
        COUNT(*)                          AS filas,
        SUM(CASE WHEN pa.ESPRNMBR IS NOT NULL THEN 1 ELSE 0 END) AS resuelve_por_alterno
FROM    RPR.CPRM c
JOIN    CRD.ENTD e       ON e.ENTDCDGO = c.ENTDCDGO
LEFT JOIN CRD.ESPR pa    ON pa.ESPRCDEX = e.ENTDIDST
GROUP BY c.CPRMSTEN
ORDER BY 1;
