-- =====================================================================================================
-- omen1-01 — ELIMINAR las 4 «cuentas bancarias» que en realidad son pólizas de inversión
-- omen-saa-1-arb · 2026-11-05 · Pedido de contabilidad. Diagnóstico: cnt/sql/c20-07.
--
--   CNBC 444 → 1.2.01.05.03.01 (Amazonas)   CNBC 445 → 1.2.01.05.03.07 (Alianza del Valle)
--   CNBC 441 → 1.2.01.05.03.28 (Pacífico)   CNBC 440 → 1.2.01.05.03.63 (Atlántida)
--
-- Por qué: al estar en bancos «reales», la regla del tipo automático trata la inversión como banco y numera
-- como EG-A la recuperación de una póliza (EG-A-2026-10-0011), y el control de cierre de bancos les pide extracto.
--
-- 26 tablas pueden apuntar a una cuenta bancaria (ingresos, movimientos, extractos, conciliaciones, saldos,
-- chequeras, cobros, pagos…). Una póliza CON historia no se puede borrar sin dejar huérfanos esos registros:
--   * BLOQUE 2 borra SOLO las que no tienen ninguna referencia;
--   * BLOQUE 3 (alternativa) deja INACTIVAS y SIN cuenta contable las que sí tienen: para el sistema es lo mismo
--     (no salen en tesorería, la regla de tipo ya no las ve, el control de bancos tampoco) y no se pierde historia.
-- Correr bloque por bloque y en orden. PEGAR la salida del BLOQUE 0 y del 1 antes del 2.
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 0 — CONTROL. Esperado: 4 filas, las cuatro con cuenta 1.2.01.05.03.xx.
-- ---------------------------------------------------------------------------------------------------
SELECT c.CNBCCDGO, c.CNBCNMRO, c.CNBCESTD, b.BNCOCDGO, b.BNCONMBR, b.BNCOREAL, n.PLNNCNTA, n.PLNNNMBR
FROM   TSR.CNBC c
JOIN   TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = c.PLNNCDGO
WHERE  c.CNBCCDGO IN (440, 441, 444, 445)
ORDER BY c.CNBCCDGO;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — REFERENCIAS: cuántos registros apuntan a cada una. Solo salen las tablas con referencias.
-- ---------------------------------------------------------------------------------------------------
SELECT CNBC, TABLA, COUNT(*) AS REGISTROS FROM (
            SELECT INGRCNBC CNBC, 'TSR.INGR ingresos'            TABLA FROM TSR.INGR WHERE INGRCNBC IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.MVCB movimientos banco'          FROM TSR.MVCB WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.EXBC extractos'                 FROM TSR.EXBC WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.DEXB detalle extracto'          FROM TSR.DEXB WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.CNCT conciliacion contable'     FROM TSR.CNCT WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.CNCL conciliacion'              FROM TSR.CNCL WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.CNCH hist conciliacion'         FROM TSR.CNCH WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.SLCB saldos banco'              FROM TSR.SLCB WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.CHQR chequeras'                 FROM TSR.CHQR WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.DBCR debitos/creditos'          FROM TSR.DBCR WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.DTDP detalle deposito'          FROM TSR.DTDP WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.ADTD aux deposito'              FROM TSR.ADTD WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.APDS aux deposito desglose'     FROM TSR.APDS WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.CTRN cobro transferencia'       FROM TSR.CTRN WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'TSR.TCTR temp cobro transferencia'  FROM TSR.TCTR WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCORGN, 'TSR.TRNS transferencia origen'      FROM TSR.TRNS WHERE CNBCORGN IN (440,441,444,445)
  UNION ALL SELECT CNBCDSTN, 'TSR.TRNS transferencia destino'     FROM TSR.TRNS WHERE CNBCDSTN IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'CRD.CBCR cobro credito'             FROM CRD.CBCR WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'CRD.RVSG recepcion valor seguro'    FROM CRD.RVSG WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'CRD.TRCR transferencia carga petro' FROM CRD.TRCR WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CNBCCDGO, 'RHH.DVAN devolucion anticipo'       FROM RHH.DVAN WHERE CNBCCDGO IN (440,441,444,445)
  UNION ALL SELECT CTBNCDGO, 'RHH.RDPG orden pago nomina'         FROM RHH.RDPG WHERE CTBNCDGO IN (440,441,444,445)
  UNION ALL SELECT PGTRCNBC, 'PGS.PGTR pago programado'           FROM PGS.PGTR WHERE PGTRCNBC IN (440,441,444,445)
  UNION ALL SELECT LTPGCNBC, 'PGS.LTPG lote pago'                 FROM PGS.LTPG WHERE LTPGCNBC IN (440,441,444,445)
  UNION ALL SELECT DVPRCNBC, 'PGS.DVPR devolucion anticipo prov'  FROM PGS.DVPR WHERE DVPRCNBC IN (440,441,444,445)
) GROUP BY CNBC, TABLA ORDER BY CNBC, TABLA;

-- Red de seguridad: llaves foráneas declaradas en la base contra TSR.CNBC (por si alguna tabla no está arriba).
SELECT c.OWNER, c.TABLE_NAME, cc.COLUMN_NAME, c.CONSTRAINT_NAME
FROM   ALL_CONSTRAINTS c
JOIN   ALL_CONSTRAINTS r  ON r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME AND r.OWNER = c.R_OWNER
JOIN   ALL_CONS_COLUMNS cc ON cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME AND cc.OWNER = c.OWNER
WHERE  c.CONSTRAINT_TYPE = 'R' AND r.OWNER = 'TSR' AND r.TABLE_NAME = 'CNBC'
ORDER BY c.OWNER, c.TABLE_NAME;

-- ---------------------------------------------------------------------------------------------------
-- RESPALDO (antes del BLOQUE 2 o 3). Esperado: 4 filas.
-- ---------------------------------------------------------------------------------------------------
CREATE TABLE TSR.BK_CNBC_POLIZAS_NOV26 AS
SELECT * FROM TSR.CNBC WHERE CNBCCDGO IN (440, 441, 444, 445);

SELECT COUNT(*) AS FILAS_RESPALDO FROM TSR.BK_CNBC_POLIZAS_NOV26;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — BORRAR las que NO tienen ninguna referencia (las del BLOQUE 1 que no aparecen).
-- Filas borradas esperadas = 4 − cuántas aparecieron en el BLOQUE 1. Si Oracle responde ORA-02292
-- (registro hijo encontrado): ROLLBACK y avisar; la tabla que la nombra no estaba en la lista.
-- ---------------------------------------------------------------------------------------------------
DELETE FROM TSR.CNBC c
WHERE  c.CNBCCDGO IN (440, 441, 444, 445)
AND NOT EXISTS (SELECT 1 FROM TSR.INGR x WHERE x.INGRCNBC = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.MVCB x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.EXBC x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.DEXB x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.CNCT x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.CNCL x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.CNCH x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.SLCB x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.CHQR x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.DBCR x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.DTDP x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.ADTD x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.APDS x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.CTRN x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.TCTR x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM TSR.TRNS x WHERE x.CNBCORGN = c.CNBCCDGO OR x.CNBCDSTN = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM CRD.CBCR x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM CRD.RVSG x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM CRD.TRCR x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM RHH.DVAN x WHERE x.CNBCCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM RHH.RDPG x WHERE x.CTBNCDGO = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM PGS.PGTR x WHERE x.PGTRCNBC = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM PGS.LTPG x WHERE x.LTPGCNBC = c.CNBCCDGO)
AND NOT EXISTS (SELECT 1 FROM PGS.DVPR x WHERE x.DVPRCNBC = c.CNBCCDGO);

COMMIT;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — ALTERNATIVA para las que tienen historia (las que el BLOQUE 2 no borró):
-- inactivas y sin cuenta contable. No se pierde ningún registro; la cuenta original queda en el respaldo.
-- Filas esperadas = las que quedaron después del BLOQUE 2.
-- ---------------------------------------------------------------------------------------------------
UPDATE TSR.CNBC c
SET    c.CNBCESTD = 0,
       c.PLNNCDGO = NULL
WHERE  c.CNBCCDGO IN (440, 441, 444, 445);

COMMIT;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 4 — CONTROL DESPUÉS. Esperado:
--   (a) las 4 ya no existen, o existen con CNBCESTD 0 y PLNNCDGO nulo;
--   (b) ninguna cuenta bancaria de banco real apunta fuera de 1.1. (cero filas).
-- ---------------------------------------------------------------------------------------------------
SELECT c.CNBCCDGO, c.CNBCESTD, c.PLNNCDGO FROM TSR.CNBC c WHERE c.CNBCCDGO IN (440, 441, 444, 445);

SELECT c.CNBCCDGO, n.PLNNCNTA, b.BNCONMBR
FROM   TSR.CNBC c JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO JOIN CNT.PLNN n ON n.PLNNCDGO = c.PLNNCDGO
WHERE  b.BNCOREAL = 1 AND n.PLNNCNTA NOT LIKE '1.1.%';

-- ---------------------------------------------------------------------------------------------------
-- REVERSO (descomentar):
-- -- de un borrado:
-- INSERT INTO TSR.CNBC SELECT * FROM TSR.BK_CNBC_POLIZAS_NOV26 b
--  WHERE NOT EXISTS (SELECT 1 FROM TSR.CNBC c WHERE c.CNBCCDGO = b.CNBCCDGO);
-- -- de la inactivación:
-- UPDATE TSR.CNBC c SET (c.CNBCESTD, c.PLNNCDGO) =
--   (SELECT b.CNBCESTD, b.PLNNCDGO FROM TSR.BK_CNBC_POLIZAS_NOV26 b WHERE b.CNBCCDGO = c.CNBCCDGO)
--  WHERE c.CNBCCDGO IN (440, 441, 444, 445);
-- COMMIT;
-- ---------------------------------------------------------------------------------------------------
