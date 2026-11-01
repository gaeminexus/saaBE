-- =====================================================================================
-- 363 — SEMILLA + DDL. omen-saa-1, 2026-11-01. Esquema de parámetros de producto de pago (sin resolver por cuenta).
--   A) rubro 237 (CRD_PARAMETROS_CONTABILIDAD): alterno 4 = producto de la DEVOLUCIÓN del B8 → 1222 (DEVDESC);
--      alterno 5 = producto del DESEMBOLSO de préstamos → 976. (Los alternos 2 = 517 y 3 = 516 ya los sembró 361.)
--   B) RHH.CFNM: columna nueva CFNMPRPG = producto de pago de la nómina por empleado → 345 (NOMINA).
-- El código verifica: parámetro presente, producto existe y ACTIVO, y cuenta del grupo = cuenta de la plantilla.
--
-- ORDEN Y MOMENTO:
--   - Bloques 0, 1 y 2: SE PUEDEN CORRER YA (el EAR de hoy no lee ni el rubro 237 alterno 4/5 ni CFNMPRPG).
--     ⛔ El bloque 2 (DDL de CFNM) TIENE que estar antes del EAR único: sin la columna, toda lectura de RHH.CFNM
--        con el EAR nuevo da ORA-00904 (nómina entera caída).
--   - Bloque 4 (activar el 1222): SOLO EN EL DESPLIEGUE del EAR único, justo antes o después de desplegarlo.
--     ⛔ Activarlo antes detiene los desembolsos del código de hoy (dos productos activos en 2.3.90.90.10).
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 Rubro 237: los alternos 4 y 5 NO deben existir con otro uso. (!) Si existen, PARAR y pasar la salida.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO WHERE r.PRBRALTR = 237 ORDER BY d.PDTRALTR;
-- 0.2 Productos: 976 activo, 1222 (debe estar INACTIVO, ESTADO 0), 345 activo.
SELECT p.ID, p.CODIGO, p.NOMBRE, p.ESTADO FROM PGS.PRDP p WHERE p.ID IN (976, 1222, 345) ORDER BY p.ID;
-- 0.3 La columna CFNMPRPG no existe todavía. ESPERADO: 0 filas.
SELECT column_name FROM all_tab_columns WHERE owner = 'RHH' AND table_name = 'CFNM' AND column_name = 'CFNMPRPG';


-- BLOQUE 1 — (ESCRIBE) parámetros 4 y 5 del rubro 237.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'PRODUCTO DE PAGO DE LA DEVOLUCION DE DESCUENTO PETRO/ARCH (PGS.PRDP)', 1222, NULL, 4, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 237
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 4);
-- (!) "1 fila insertada".
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT (SELECT NVL(MAX(PDTRCDGO), 0) + 1 FROM SCP.PDTR), r.PRBRCDGO,
       'PRODUCTO DE PAGO DEL DESEMBOLSO DE PRESTAMOS (PGS.PRDP)', 976, NULL, 5, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 237
   AND NOT EXISTS (SELECT 1 FROM SCP.PDTR d WHERE d.PRBRCDGO = r.PRBRCDGO AND d.PDTRALTR = 5);
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — (DDL + ESCRIBE) columna CFNMPRPG y su valor.
ALTER TABLE RHH.CFNM ADD (CFNMPRPG NUMBER);
COMMENT ON COLUMN RHH.CFNM.CFNMPRPG IS 'Producto de pago (PGS.PRDP) del pago de nomina por empleado; se verifica contra la cuenta de la linea 50 de la plantilla de pago';
UPDATE RHH.CFNM SET CFNMPRPG = 345 WHERE PJRQCDGO = 1236 AND CFNMPRPG IS NULL;
-- (!) "N filas actualizadas" (una por configuración de nómina de la empresa).
COMMIT;


-- BLOQUE 3 — CONTROL DESPUÉS
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRN, d.PDTRESTD
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO WHERE r.PRBRALTR = 237 AND d.PDTRALTR IN (2, 3, 4, 5) ORDER BY 2;
-- ESPERADO: 2 = 517, 3 = 516, 4 = 1222, 5 = 976.
SELECT CFNMCDGO, PJRQCDGO, CFNMPRPG FROM RHH.CFNM;
-- ESPERADO: CFNMPRPG = 345 en la(s) de la empresa 1236.


-- BLOQUE 4 — ⛔ NO CORRER AHORA. EN EL DESPLIEGUE DEL EAR ÚNICO:
-- UPDATE PGS.PRDP SET ESTADO = 1 WHERE ID = 1222 AND ESTADO = 0;
-- COMMIT;


-- REVERSO (comentado):
-- DELETE FROM SCP.PDTR d WHERE d.PDTRALTR IN (4, 5) AND d.PRBRCDGO = (SELECT r.PRBRCDGO FROM SCP.PRBR r WHERE r.PRBRALTR = 237);
-- ALTER TABLE RHH.CFNM DROP COLUMN CFNMPRPG;   -- solo con un EAR que no la mapee
-- COMMIT;
