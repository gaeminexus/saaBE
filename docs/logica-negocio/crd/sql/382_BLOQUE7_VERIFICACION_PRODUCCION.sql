-- =====================================================================================
-- 382 — Bloque 7 del encargo «contabilidad de producción correcta» (24-nov-2026): verificación en la base de
-- PRODUCCIÓN. SOLO LECTURA: ningún bloque escribe. SQL puro (cliente JDBC). omen-saa-1-arb.
--
-- Las propiedades JVM no se leen con SQL. Van con jboss-cli en el servidor de producción:
--   jboss-cli.bat -c --command="/core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)" > props.txt
--   findstr /i "saa.cartera" props.txt
-- Esperado:
--   saa.cartera.alCorte                = siempre
--   saa.cartera.corteInclusivo         = (NO debe aparecer; si aparece, C42 no rige)
--   saa.cartera.corteExclusivoDesde    = (sin definir = 2025-08-01)
--   saa.cartera.moraNoCobradaHasta     = (NO debe aparecer en producción)
--   saa.cartera.devengoMoraCierre, saa.cartera.inicioDevengoMora (2025-05-31),
--   saa.cartera.neteoPorPrestamo, saa.cartera.moraNoDevengadaAIngreso (true) = anotar lo que diga
-- =====================================================================================

-- 1. Plantillas 25 (acuerdo de condonación) y 37 (provisión ⑧ y bandas ⑨) de crédito, con sus líneas y cuentas.
--    ESPERADO: la 25 con su auxiliar 70; la 37 con los auxiliares 82, 83, 84 y 85. Anotar la cuenta de la 25/70:
--    si es 4.1.01.30 (inversiones), falta el def9-07 del cuadre.
SELECT p.PLNSCDAL ALTERNO, p.PLNSNMBR PLANTILLA, p.PLNSESTD EST_PLANTILLA,
       d.DTPLCDGO, d.DTPLAXL1 AUX1, d.DTPLMVMN MOV, d.DTPLESTD EST_LINEA,
       c.PLNNCNTA CUENTA, c.PLNNNMBR NOMBRE_CUENTA
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL IN (25, 37)
 ORDER BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLCDGO;

-- 2. Línea 70 de la 25 sola. ESPERADO tras def9-07: cuenta 4.8.90.90.45.
SELECT d.DTPLCDGO, d.DTPLAXL1, c.PLNNCNTA, c.PLNNNMBR
  FROM CNT.PLNS p JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE p.PJRQCDGO = 1236 AND p.PLNSCDAL = 25 AND d.DTPLAXL1 = 70;

-- 3. ¿Existe la cuenta 4.8.90.90.45 (def9-07)? ESPERADO: 1 fila.
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNESTD FROM CNT.PLNN WHERE REPLACE(PLNNCNTA, '.', '') = '489090' || '45';

-- 4. CK_ANCC_TPOO: hoy debe admitir 1..10 (sql/346). C27b va a necesitar 11 y 12.
SELECT constraint_name, search_condition_vc FROM all_constraints
 WHERE owner = 'CRD' AND table_name = 'ANCC' AND constraint_name = 'CK_ANCC_TPOO';

-- 5. DDL de C39-C41 (rama mergeada en 897d6eb4), que va ANTES del WAR. ESPERADO hoy: 0 filas en los dos
--    (todavía no se corren); después de C39-01 el CHECK admite CONFIRMADO y PENDIENTE.
SELECT table_name FROM all_tables WHERE owner = 'RPR' AND table_name = 'SSTG';
SELECT column_name FROM all_tab_columns WHERE owner = 'RPR' AND table_name = 'SG45' AND column_name = 'SG45TPPR';
SELECT search_condition_vc FROM all_constraints WHERE owner = 'RPR' AND table_name = 'SG45' AND constraint_name = 'CK_SG45_ORGN';

-- 6. Scripts del cuadre que el EAR anterior (cancelados-1124) pedía antes del WAR. ESPERADO: todas con fila.
SELECT 'LNNV' t, COUNT(*) n FROM all_tables WHERE owner = 'CNT' AND table_name = 'LNNV' UNION ALL
SELECT 'HCNV', COUNT(*) FROM all_tables WHERE owner = 'CNT' AND table_name = 'HCNV' UNION ALL
SELECT 'NTNV', COUNT(*) FROM all_tables WHERE owner = 'CNT' AND table_name = 'NTNV' UNION ALL
SELECT 'HCNVTPAP', COUNT(*) FROM all_tab_columns WHERE owner = 'CNT' AND table_name = 'HCNV' AND column_name = 'HCNVTPAP' UNION ALL
SELECT 'HCNVCNDN', COUNT(*) FROM all_tab_columns WHERE owner = 'CNT' AND table_name = 'HCNV' AND column_name = 'HCNVCNDN' UNION ALL
SELECT 'HDTP.EVPRCDGO NULL', COUNT(*) FROM all_tab_columns WHERE owner = 'CRD' AND table_name = 'HDTP' AND column_name = 'EVPRCDGO' AND nullable = 'Y';

-- U1–U7 del §4 del pase: no están en este repo (son del PASE-A-PRODUCCION del cuadre). Las controla el cuadre.
