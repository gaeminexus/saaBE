-- =====================================================================================
-- e3-21 — Décimo tercero pagado por anticipado: columnas en RHH.LQBS, roles 37/38/39 y sus conceptos
-- Equipo omen-saa-3 · 2026-11-16 · Diseño: rhh/DISENO-DECIMO-TERCERO-ANTICIPADO.md
--
-- ⚠️ VA ANTES DEL WAR: la entidad LiquidacionBeneficioSocial mapea las 4 columnas nuevas; sin ellas, toda lectura
--    de RHH.LQBS da ORA-00904.
-- Correr el BLOQUE 0, LEER, y recién después seguir. Sin PROMPT/SET (cliente del usuario).
-- =====================================================================================

-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES
-- =====================================================================

-- 0.1 Las columnas NO deben existir. ESPERADO: 0 filas.
SELECT '0.1 - columnas ya existen?' AS control, column_name
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS'
   AND column_name IN ('LQBSANTC', 'LQBSFCCR', 'LQBSVLAJ', 'LQBSPRAJ');

-- 0.2 Rubro 221 (rol del concepto en el motor) existe y NO tiene 37/38/39. ESPERADO: 1 fila y 0 filas.
SELECT '0.2a - rubro 221' AS control, PRBRCDGO, PRBRALTR, PRBRDSCR FROM SCP.PRBR WHERE PRBRALTR = 221;
SELECT '0.2b - conflicto 37-39' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (37, 38, 39);

-- 0.3 Ningún concepto usa ya los roles 37/38/39. ESPERADO: 0 filas.
SELECT '0.3 - conceptos con rol 37-39' AS control, CPNMCDGO, PJRQCDGO, CPNMNMBR, CPNMROLM
  FROM RHH.CPNM WHERE CPNMROLM IN (37, 38, 39);

-- 0.4 Empresas que corren nómina (tienen rol 6) y conceptos a crear (3 por empresa).
--     ESPERADO: un puñado (hoy 1, la 1236 → 3). 🔴 Si dice cientos, PARAR.
SELECT '0.4 - conceptos a crear' AS control,
       COUNT(DISTINCT PJRQCDGO) AS empresas, COUNT(DISTINCT PJRQCDGO) * 3 AS total_a_insertar
  FROM RHH.CPNM WHERE CPNMROLM = 6;

-- =====================================================================
-- BLOQUE 1 — COLUMNAS DE RHH.LQBS
-- =====================================================================
ALTER TABLE RHH.LQBS ADD (
    LQBSANTC VARCHAR2(1) DEFAULT 'N',   -- 'S' = calculada por anticipado (hasta el 30-nov con roles calculados)
    LQBSFCCR DATE,                      -- fecha en que se calculó por anticipado
    LQBSVLAJ NUMBER(18,2),              -- ajuste aplicado en diciembre o en el finiquito (con signo; 0 = sin diferencia)
    LQBSPRAJ NUMBER                     -- periodo (RHH.PRDN) donde se aplicó el ajuste
);
COMMENT ON COLUMN RHH.LQBS.LQBSANTC IS 'S = decimo calculado por anticipado (e3-21)';
COMMENT ON COLUMN RHH.LQBS.LQBSFCCR IS 'Fecha del calculo anticipado';
COMMENT ON COLUMN RHH.LQBS.LQBSVLAJ IS 'Ajuste del anticipo aplicado en diciembre o en el finiquito, con signo';
COMMENT ON COLUMN RHH.LQBS.LQBSPRAJ IS 'Periodo RHH.PRDN donde se aplico el ajuste';
ALTER TABLE RHH.LQBS ADD CONSTRAINT FK_LQBS_PRAJ FOREIGN KEY (LQBSPRAJ) REFERENCES RHH.PRDN (PRDNCDGO);
ALTER TABLE RHH.LQBS ADD CONSTRAINT CK_LQBS_ANTC CHECK (LQBSANTC IN ('S', 'N'));

-- =====================================================================
-- BLOQUE 2 — ROLES 37/38/39 EN EL RUBRO 221
-- CORREGIDO 2026-11-16 en producción: SCP.SQ_PDTRCDGO está atrasada respecto de los datos y NEXTVAL chocó con un
-- PDTRCDGO ya usado. Se usan PK explícitos 1540-1542 (el siguiente libre según el usuario). Verificar antes:
--   SELECT PDTRCDGO FROM SCP.PDTR WHERE PDTRCDGO BETWEEN 1540 AND 1542;  -- ESPERADO: 0 filas
-- =====================================================================
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1540, r.PRBRCDGO, 'AJUSTE DECIMO TERCERO ANTICIPADO A FAVOR', 37, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 37);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1541, r.PRBRCDGO, 'AJUSTE DECIMO TERCERO ANTICIPADO EN CONTRA', 38, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 38);
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1542, r.PRBRCDGO, 'FINIQUITO DECIMO TERCERO PAGADO POR ANTICIPADO', 39, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 39);

-- =====================================================================
-- BLOQUE 3 — CONCEPTOS, uno de cada rol por empresa que corre nómina (copiados del rol 6, patrón e2-26)
--   37 INGRESO (1), 38 y 39 EGRESO (2). Ninguna base (el décimo no es aportable ni gravado), no patronal,
--   no provisiona, no obligatorio (lo pone el motor), no recortable, RDEP e IESS nulos.
--   CPNMCDGO es IDENTITY. Re-correr no duplica (NOT EXISTS).
-- =====================================================================
INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMTPCL, CPNMBSCL,
                      CPNMTPRL, CPNMVLRR, CPNMPRCN, CPNMFRML, CPNMIMIE, CPNMIMIR, CPNMAPFR,
                      CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT, CPNMPTRN, CPNMPRVS, CPNMOBLG,
                      CPNMRCRT, CPNMRDEP, CPNMIESS, CPNMORDN, CPNMESTD, CPNMFCHR, CPNMUSRR, CPNMROLM)
SELECT p.PJRQCDGO, 'Ajuste décimo tercero anticipado (a favor)', 'D13ANTAF',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, p.CPNMTPCL, p.CPNMBSCL, p.CPNMTPRL, 0, 0, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', NULL, NULL,
       NVL((SELECT MAX(c.CPNMORDN) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, SYSTIMESTAMP, 'e3-21', 37
  FROM RHH.CPNM p
 WHERE p.CPNMROLM = 6
   AND NOT EXISTS (SELECT 1 FROM RHH.CPNM c2 WHERE c2.PJRQCDGO = p.PJRQCDGO AND c2.CPNMROLM = 37);

INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMTPCL, CPNMBSCL,
                      CPNMTPRL, CPNMVLRR, CPNMPRCN, CPNMFRML, CPNMIMIE, CPNMIMIR, CPNMAPFR,
                      CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT, CPNMPTRN, CPNMPRVS, CPNMOBLG,
                      CPNMRCRT, CPNMRDEP, CPNMIESS, CPNMORDN, CPNMESTD, CPNMFCHR, CPNMUSRR, CPNMROLM)
SELECT p.PJRQCDGO, 'Ajuste décimo tercero anticipado (en contra)', 'D13ANTEC',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       2, p.CPNMTPCL, p.CPNMBSCL, p.CPNMTPRL, 0, 0, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', NULL, NULL,
       NVL((SELECT MAX(c.CPNMORDN) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, SYSTIMESTAMP, 'e3-21', 38
  FROM RHH.CPNM p
 WHERE p.CPNMROLM = 6
   AND NOT EXISTS (SELECT 1 FROM RHH.CPNM c2 WHERE c2.PJRQCDGO = p.PJRQCDGO AND c2.CPNMROLM = 38);

INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMTPCL, CPNMBSCL,
                      CPNMTPRL, CPNMVLRR, CPNMPRCN, CPNMFRML, CPNMIMIE, CPNMIMIR, CPNMAPFR,
                      CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT, CPNMPTRN, CPNMPRVS, CPNMOBLG,
                      CPNMRCRT, CPNMRDEP, CPNMIESS, CPNMORDN, CPNMESTD, CPNMFCHR, CPNMUSRR, CPNMROLM)
SELECT p.PJRQCDGO, 'Décimo tercero pagado por anticipado', 'D13ANTFQ',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       2, p.CPNMTPCL, p.CPNMBSCL, p.CPNMTPRL, 0, 0, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', 'N', NULL, NULL,
       NVL((SELECT MAX(c.CPNMORDN) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, SYSTIMESTAMP, 'e3-21', 39
  FROM RHH.CPNM p
 WHERE p.CPNMROLM = 6
   AND NOT EXISTS (SELECT 1 FROM RHH.CPNM c2 WHERE c2.PJRQCDGO = p.PJRQCDGO AND c2.CPNMROLM = 39);

COMMIT;

-- =====================================================================
-- BLOQUE 4 — CONTROLES DESPUÉS
-- =====================================================================
-- 4.1 ESPERADO: 4 filas.
SELECT '4.1 - columnas nuevas' AS control, column_name, data_type, data_default
  FROM all_tab_columns
 WHERE owner = 'RHH' AND table_name = 'LQBS'
   AND column_name IN ('LQBSANTC', 'LQBSFCCR', 'LQBSVLAJ', 'LQBSPRAJ');
-- 4.2 ESPERADO: 3 filas, alternos 37, 38, 39.
SELECT '4.2 - roles 37-39' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (37, 38, 39) ORDER BY d.PDTRALTR;
-- 4.3 ESPERADO: 3 por empresa; tipo 1 para el 37 y 2 para 38/39; todas las bases en N.
SELECT '4.3 - conceptos' AS control, PJRQCDGO, CPNMROLM, CPNMNMBR, CPNMTPCN AS tipo,
       CPNMIMIE||CPNMIMIR||CPNMAPFR||CPNMBSDT||CPNMBSDC||CPNMBSVC||CPNMBSUT AS bases_deben_ser_NNNNNNN, CPNMESTD
  FROM RHH.CPNM WHERE CPNMROLM IN (37, 38, 39) ORDER BY PJRQCDGO, CPNMROLM;

-- =====================================================================
-- BLOQUE 5 — REVERSO (comentado a propósito; solo con el EAR anterior desplegado)
-- =====================================================================
-- DELETE FROM RHH.CPNM WHERE CPNMROLM IN (37, 38, 39) AND CPNMUSRR = 'e3-21';
-- DELETE FROM SCP.PDTR WHERE PDTRALTR IN (37, 38, 39)
--    AND PRBRCDGO = (SELECT PRBRCDGO FROM SCP.PRBR WHERE PRBRALTR = 221);
-- ALTER TABLE RHH.LQBS DROP CONSTRAINT CK_LQBS_ANTC;
-- ALTER TABLE RHH.LQBS DROP CONSTRAINT FK_LQBS_PRAJ;
-- ALTER TABLE RHH.LQBS DROP (LQBSANTC, LQBSFCCR, LQBSVLAJ, LQBSPRAJ);
-- COMMIT;
