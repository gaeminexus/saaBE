-- =====================================================================================
-- e3-11 — Diagnóstico de provisiones: G48 (RPR.CG48) y resumen financiero (RPR.CCPM)
-- Equipo omen-saa-3 · 2026-10-13 · SOLO LECTURA (ningún INSERT/UPDATE/DELETE)
--
-- Por qué existe: el usuario reporta que en el resumen financiero un campo de provisiones
-- sale siempre en 0, y pide revisar si el G48 ya calcula bien las provisiones.
--
-- Lo que dice el código (verificado el 2026-10-13):
--   * CCPM.provisionConstituida (CCPMPRCN) está CABLEADA a 0
--     (GeneracionCCPMServiceImpl:83-85 y :367-368). No es un dato: es una constante.
--   * CCPM.provisionRequeridaOriginal (CCPMPRRO) usa una tabla de calificación y porcentajes
--     ESCRITA EN EL CÓDIGO (calcularCalificacion/calcularProvision, :521-582, hipotecario =
--     productos 7, 8, 21). El G48 dejó esa tabla el 2026-09-02 y ahora usa CRD.CFCR/CRD.ESCR.
--     Si alguien cambió la escala parametrizada, el G48 y el CCPM ya no calculan igual.
--   * CG48.provisionConstituida (CG48PRCN) = la provisión REQUERIDA del G48 del mes anterior
--     (no el saldo contable de la cuenta de provisión). Toma la EJRC más reciente del mes
--     anterior; si esa ejecución tiene el EJRD de G48 pero sin filas (corrida fallida), TODO
--     el mes sale con constituida 0, sin aviso. Préstamos sin idAsoprep también salen en 0.
--
-- No hay reverso: el script no modifica nada.
-- Correr cada bloque por separado y devolver los resultados al árbitro.
-- =====================================================================================

-- -------------------------------------------------------------------------------------
-- 1) Ejecuciones de los G por mes: ¿cuántas hay, y el G48 de cada una tiene filas?
--    Si la PRIMERA fila de un mes (la más reciente) tiene FILAS_CG48 = 0, el G48 del mes
--    siguiente sale con provisión constituida 0 en todas las operaciones.
-- -------------------------------------------------------------------------------------
SELECT c.EJRCANOO              AS ANIO,
       c.EJRCMESS              AS MES,
       c.EJRCCDGO              AS EJRC,
       c.EJRCFCGN              AS FECHA_EJRC,
       c.EJRCESTD              AS ESTADO_EJRC,
       d.EJRDCDGO              AS EJRD_G48,
       d.EJRDESTD              AS ESTADO_EJRD,
       d.EJRDCNRG              AS CANT_REPORTADA,
       (SELECT COUNT(*) FROM RPR.CG48 g WHERE g.CG48EJRD = d.EJRDCDGO) AS FILAS_CG48
FROM   RPR.EJRC c
LEFT JOIN RPR.EJRD d ON d.EJRCCDGO = c.EJRCCDGO AND d.EJRDTPRP = 'G48'
WHERE  c.EJRCANOO * 100 + c.EJRCMESS >= 202601
ORDER BY c.EJRCANOO DESC, c.EJRCMESS DESC, c.EJRCFCGN DESC;

-- -------------------------------------------------------------------------------------
-- 2) Totales de provisión del G48 por ejecución.
--    Esperado: CON_CONSTITUIDA > 0 en todos los meses salvo el primero que se generó.
-- -------------------------------------------------------------------------------------
SELECT c.EJRCANOO AS ANIO, c.EJRCMESS AS MES, d.EJRDCDGO AS EJRD_G48,
       COUNT(*)                                             AS OPERACIONES,
       SUM(CASE WHEN g.CG48NMOP IS NULL THEN 1 ELSE 0 END)  AS SIN_NUM_OPERACION,
       ROUND(SUM(g.CG48VSAP), 2)                            AS SUJETO_PROVISION,
       ROUND(SUM(g.CG48PRRO), 2)                            AS PROV_REQUERIDA,
       ROUND(SUM(g.CG48PRCN), 2)                            AS PROV_CONSTITUIDA,
       SUM(CASE WHEN g.CG48PRRO > 0 THEN 1 ELSE 0 END)      AS CON_REQUERIDA,
       SUM(CASE WHEN g.CG48PRCN > 0 THEN 1 ELSE 0 END)      AS CON_CONSTITUIDA
FROM   RPR.EJRC c
JOIN   RPR.EJRD d ON d.EJRCCDGO = c.EJRCCDGO AND d.EJRDTPRP = 'G48'
JOIN   RPR.CG48 g ON g.CG48EJRD = d.EJRDCDGO
WHERE  c.EJRCANOO * 100 + c.EJRCMESS >= 202601
GROUP BY c.EJRCANOO, c.EJRCMESS, d.EJRDCDGO
ORDER BY c.EJRCANOO DESC, c.EJRCMESS DESC, d.EJRDCDGO DESC;

-- -------------------------------------------------------------------------------------
-- 3) Totales de provisión del CCPM (resumen financiero) por ejecución.
--    PROV_CONSTITUIDA debe salir 0 siempre: confirma que es la constante del código.
-- -------------------------------------------------------------------------------------
SELECT e.EJCCANOO AS ANIO, e.EJCCMESS AS MES, e.EJCCCDGO AS EJCC, e.EJCCFCGN AS FECHA,
       COUNT(*)                         AS FILAS,
       ROUND(SUM(p.CCPMVSAP), 2)        AS SUJETO_PROVISION,
       ROUND(SUM(p.CCPMPRRO), 2)        AS PROV_REQUERIDA,
       ROUND(SUM(p.CCPMPRCN), 2)        AS PROV_CONSTITUIDA
FROM   RPR.EJCC e
JOIN   RPR.CCPM p ON p.CCPMEJCC = e.EJCCCDGO
WHERE  e.EJCCANOO * 100 + e.EJCCMESS >= 202601
GROUP BY e.EJCCANOO, e.EJCCMESS, e.EJCCCDGO, e.EJCCFCGN
ORDER BY e.EJCCANOO DESC, e.EJCCMESS DESC, e.EJCCFCGN DESC;

-- -------------------------------------------------------------------------------------
-- 4) Septiembre 2026: G48 contra CCPM operación por operación.
--    Las dos generaciones toman las mismas cuotas; si la calificación o la provisión
--    requerida difieren, es la tabla cableada del CCPM contra la escala CFCR/ESCR del G48.
--    Ajustar los códigos de ejecución con lo que devuelvan los bloques 2 y 3.
-- -------------------------------------------------------------------------------------
SELECT g.CG48NMOP                       AS OPERACION,
       g.CG48DDMR                       AS DIAS_G48,
       p.CCPMDDMR                       AS DIAS_CCPM,
       g.CG48CLPR                       AS CALIF_G48,
       p.CCPMCLPR                       AS CALIF_CCPM,
       ROUND(g.CG48VSAP, 2)             AS SUJETO_G48,
       ROUND(p.CCPMVSAP, 2)             AS SUJETO_CCPM,
       ROUND(g.CG48PRRO, 2)             AS PROV_REQ_G48,
       ROUND(p.CCPMPRRO, 2)             AS PROV_REQ_CCPM,
       ROUND(g.CG48PRRO - p.CCPMPRRO, 2) AS DIFERENCIA
FROM   RPR.CG48 g
JOIN   RPR.CCPM p ON p.CCPMNMOP = g.CG48NMOP
WHERE  g.CG48EJRD = (SELECT MAX(d.EJRDCDGO) FROM RPR.EJRD d JOIN RPR.EJRC c ON c.EJRCCDGO = d.EJRCCDGO
                     WHERE d.EJRDTPRP = 'G48' AND c.EJRCANOO = 2026 AND c.EJRCMESS = 9)
AND    p.CCPMEJCC = (SELECT MAX(e.EJCCCDGO) FROM RPR.EJCC e WHERE e.EJCCANOO = 2026 AND e.EJCCMESS = 9)
AND   (NVL(g.CG48CLPR, '-') <> NVL(p.CCPMCLPR, '-') OR ABS(NVL(g.CG48PRRO, 0) - NVL(p.CCPMPRRO, 0)) > 0.01)
ORDER BY ABS(NVL(g.CG48PRRO, 0) - NVL(p.CCPMPRRO, 0)) DESC;

-- -------------------------------------------------------------------------------------
-- 5) La escala vigente del G48 (CRD.CFCR/CRD.ESCR), para compararla con la tabla del CCPM:
--    general     A1 0 d 0,99% · A2 1-15 1,99% · A3 16-30 2% · B1 31-60 5% · B2 61-90 10%
--                C1 91-120 20% · C2 121-180 40% · D 181-270 60% · E >270 100%
--    hipotecario A1 0 d · A2 1-30 · A3 31-60 · B1 61-120 · B2 121-180 · C1 181-210
--                C2 211-270 · D 271-450 · E >450 (mismos porcentajes)
-- -------------------------------------------------------------------------------------
SELECT * FROM CRD.CFCR ORDER BY 1;
SELECT * FROM CRD.ESCR ORDER BY 1;
