-- ============================================================================
-- 70_CATALOGO_RUBRO_GENERACION_POR_FALTANTE.sql
-- Fase 4 del plan de devengo de aportes (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md §4.2)
-- Fecha: 2026-08-27
--
-- SQL PURO (sin SET/DEFINE/WHENEVER). NO SE EJECUTA POR EL AGENTE — el usuario lo corre.
--
-- QUE HACE: crea el rubro 242 (CRD_GENERACION_POR_FALTANTE) y su único detalle (1 =
-- GENERACION_POR_FALTANTE_ACTIVA), APAGADO (PDTRVLRN = 0). Sin esto,
-- ConfiguracionGeneracionAportesServiceImpl.actualizar lanza IncomeException ("no se
-- encontró el detalle de rubro") la primera vez que alguien intente encenderlo — leerlo
-- (porFaltanteActiva/obtenerEstado) no falla nunca, ya vuelve false/estado en blanco si el
-- detalle no existe.
--
-- ⚠ Verificado contra SCP.PRBR el 2026-08-27: 238, 239, 240 y 241 YA ESTÁN RESERVADOS por
-- otras olas en curso (SRI/CXP y particiones en tránsito de TSR) aunque ninguno tenga fila
-- en la base todavía — com.saa.rubros.Rubros SÍ los declara. 242 es el primer alterno
-- realmente libre tanto en la base como en el código. Mismo patrón de PK explícito que
-- 235/236/237 (DDL-APORTES-DEVENGO-CONTRATOS.sql bloque 5): PRBRCDGO = PRBRALTR = 242.
--
-- ⚠ Este ambiente NO tiene una secuencia SQ_PRBRCDGO/SQ_PDTRCDGO visible en ALL_SEQUENCES
-- (verificado: SCP tiene una sola secuencia en total, y no es ninguna de esas dos).
-- El bloque 3 de este script queda por las dudas — si en el ambiente donde se corra SÍ
-- existe esa secuencia, hay que sincronizarla igual que se hizo para 235-237.
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 — CONTROL ANTES
-- ============================================================================

-- 1.1 El PK explícito (242) debe estar libre en PRBR y el detalle (1141) en PDTR.
--     Esperado: 0 filas.
SELECT 'PRBR' AS TABLA, r.PRBRCDGO AS CODIGO_OCUPADO, r.PRBRDSCR AS DESCRIPCION
FROM   SCP.PRBR r WHERE r.PRBRCDGO = 242
UNION ALL
SELECT 'PDTR', d.PDTRCDGO, d.PDTRDSCR
FROM   SCP.PDTR d WHERE d.PDTRCDGO = 1141;

-- 1.2 El alterno 242 no debe existir todavía (ni con otro PK). Esperado: 0 filas.
SELECT PRBRCDGO, PRBRALTR, PRBRDSCR FROM SCP.PRBR WHERE PRBRALTR = 242;

-- 1.3 Máximos actuales, para confirmar que 242/1141 son los siguientes libres.
SELECT MAX(PRBRCDGO) AS MAX_PRBRCDGO FROM SCP.PRBR;
SELECT MAX(PDTRCDGO) AS MAX_PDTRCDGO FROM SCP.PDTR;


-- ============================================================================
-- BLOQUE 2 — CATALOGO
-- ============================================================================

-- 2.1 Rubro 242 (PK = alterno, mismo patrón que 235/236/237). PRBRTPOO copiado del rubro
--     169 (ASPNovedadesCargaArchivo), igual que se hizo para 235/236/237.
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (242, 'CRD GENERACION APORTES POR FALTANTE', SYSDATE, 242,
        NVL((SELECT MAX(r.PRBRTPOO) FROM SCP.PRBR r WHERE r.PRBRALTR = 169), 1));

-- 2.2 Detalle único, APAGADO (PDTRVLRN = 0). Encenderlo es el UPDATE del bloque 4.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (1141, 242, 'GENERACION POR FALTANTE ACTIVA (0 apagada, 1 encendida)', 0, NULL, 1, 1);

COMMIT;


-- ============================================================================
-- BLOQUE 3 — SINCRONIZAR SECUENCIA (solo si existe en el ambiente donde se corre)
-- ============================================================================

-- Si SIGUIENTE_VALOR ya es MAYOR que el PK usado (242 / 1141), la secuencia está
-- adelantada y NO SE TOCA. Si no existe ninguna fila (como en el ambiente donde se probó
-- este script el 2026-08-27), no hay nada que sincronizar.
SELECT  'SQ_PRBRCDGO' AS SECUENCIA, 242 AS PK_USADO,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PRBRCDGO') AS SIGUIENTE_VALOR
FROM    DUAL
UNION ALL
SELECT  'SQ_PDTRCDGO', 1141,
        (SELECT s.LAST_NUMBER FROM ALL_SEQUENCES s
         WHERE s.SEQUENCE_OWNER = 'SCP' AND s.SEQUENCE_NAME = 'SQ_PDTRCDGO')
FROM    DUAL;

-- Ejecutar SOLO la línea cuya secuencia haya quedado en o por debajo del PK usado, y
-- SOLO si la fila de arriba existió (si no hay secuencia, no hay nada que correr aquí).
-- ALTER SEQUENCE SCP.SQ_PRBRCDGO RESTART START WITH 243;
-- ALTER SEQUENCE SCP.SQ_PDTRCDGO RESTART START WITH 1142;


-- ============================================================================
-- BLOQUE 4 — CONTROL DESPUES
-- ============================================================================

-- 4.1 El rubro y su detalle quedaron creados. Esperado: 1 fila.
SELECT  r.PRBRALTR AS RUBRO, r.PRBRDSCR, d.PDTRALTR AS DETALLE, d.PDTRDSCR, d.PDTRVLRN, d.PDTRESTD
FROM    SCP.PRBR r
JOIN    SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE   r.PRBRALTR = 242;

-- 4.2 ENCENDER el camino nuevo de generación por faltante.
--     NO ejecutar junto con el resto: se enciende cuando el bloque 4.3 del plan (consulta
--     de comparación viejo/nuevo, ver 71_COMPARACION_GENERACION_VIEJO_VS_NUEVO.sql) ya se
--     revisó y el usuario decidió que las diferencias son aceptables. Para apagarlo, el
--     mismo UPDATE con 0. Preferir PUT /rest/cnfg/generacionPorFaltanteAh en vez de este
--     UPDATE directo: deja la huella de usuario/fecha/motivo en PDTRVLRV.
-- UPDATE SCP.PDTR SET PDTRVLRN = 1
-- WHERE  PDTRALTR = 1
-- AND    PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 242);
-- COMMIT;
