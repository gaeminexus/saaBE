-- =====================================================================
-- e2-61 — Las columnas que faltan para clasificar una compra por tarifa
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ⚠️ DDL. VA ANTES DEL WAR que lo acompaña: las entidades van a mapear
--    estas columnas, y sin ellas toda lectura del documento muere con
--    ORA-00904.
--
-- ⚠️ El e2-55 (PGS.FCTC.SUBNOOBJ) VA PRIMERO. Este script NO lo repite.
--
-- POR QUE EXISTE — decision del usuario del 2026-09-21: el frente de
-- clasificacion por tarifa se hace COMPLETO, no el minimo para agosto.
-- Diseño en docs/logica-negocio/cxp/PLAN-CLASIFICACION-POR-TARIFA-COMPRAS.md
--
-- LO QUE HAY HOY, medido el 2026-09-21:
--   PGS.FCTC : SUBTOTAL, SUBCERO, SUBTOTAL5, SUBTOTAL8 (las dos ultimas
--              EXISTEN Y NADIE LAS LLENA), VIVA, VIVA5, VIVA8, VICE
--              + SUBNOOBJ, que crea el e2-55
--   PGS.NTCC : SUBTOTAL, SUBCERO, VIVA  <- y nada mas
--   PGS.NTDC : SUBTOTAL, SUBCERO, VIVA  <- y nada mas
--   PGS.LQCC : SUBTOTAL, SUBCERO, VIVA  <- y nada mas
--
--   O sea: no hay columna de EXENTO en ninguna de las cuatro, y las notas
--   de credito, de debito y las liquidaciones no tienen ni no objeto ni
--   5% ni 8%. El ATS las declara a las tres con baseNoGraIva y baseImpExe
--   en 0.00 fijo.
--
-- TABLA 17 DEL SRI (codigoPorcentaje), que es el mapa del reparto:
--   0 = 0%  ·  4 = 15%  ·  5 = 5%  ·  6 = No objeto  ·  7 = Exento
--   8 = 8%  ·  2/3/10 = 12%/14%/13% (historicos, van a gravada)
--
-- INVARIANTE (el mismo del e2-55, extendido):
--   SUBTOTAL = total sin impuestos, TODO incluido
--   SUBCERO  = base 0%      ·  SUBNOOBJ = base no objeto
--   SUBEXENT = base exenta  ·  SUBTOTAL5 = base 5%  ·  SUBTOTAL8 = base 8%
--   gravada  = SUBTOTAL - SUBCERO - SUBNOOBJ - SUBEXENT - SUBTOTAL5 - SUBTOTAL8
--   Todas las columnas nuevas nacen en 0: con 0 el comportamiento es
--   IDENTICO al de hoy, y un WAR viejo las ignora.
--
-- ⛔ ESTE SCRIPT NO RECALCULA NADA DE LO YA CARGADO. Solo crea columnas
--    vacias. El recalculo historico va en un script aparte, DESPUES de
--    medir con el e2-59 y de ver el antes/despues -- porque cambia numeros
--    de periodos ya declarados.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC),
--   NotaCreditoCompra (PGS.NTCC), NotaDebitoCompra (PGS.NTDC),
--   LiquidacionCompraCompra (PGS.LQCC).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control previo: que columnas existen ya en cada tabla.
-- ESPERADO: FCTC con SUBCERO, SUBTOTAL5, SUBTOTAL8 y SUBNOOBJ (esta ultima
--           solo si el e2-55 ya corrio). NTCC/NTDC/LQCC solo con SUBCERO.
-- ⛔ Si SUBNOOBJ no aparece en FCTC, PARAR: falta correr el e2-55 primero.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - columnas existentes' AS bloque,
       c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND c.TABLE_NAME IN ('FCTC','NTCC','NTDC','LQCC')
   AND c.COLUMN_NAME IN ('SUBTOTAL','SUBCERO','SUBNOOBJ','SUBEXENT','SUBTOTAL5','SUBTOTAL8','VIVA')
 ORDER BY c.TABLE_NAME, c.COLUMN_NAME;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — DDL. Todas aditivas, con default 0.
-- ESPERADO: "Table altered" en cada una. Si alguna dice que la columna ya
--           existe, saltear esa linea y seguir.
-- NOTA: el DDL en Oracle hace autocommit. Los UPDATE de otros scripts NO.
-- ---------------------------------------------------------------------

-- Factura de compra: le falta solo el exento (SUBNOOBJ lo pone el e2-55)
ALTER TABLE PGS.FCTC ADD (SUBEXENT NUMBER DEFAULT 0);

-- Nota de credito de compra
ALTER TABLE PGS.NTCC ADD (SUBNOOBJ NUMBER DEFAULT 0);
ALTER TABLE PGS.NTCC ADD (SUBEXENT NUMBER DEFAULT 0);
ALTER TABLE PGS.NTCC ADD (SUBTOTAL5 NUMBER DEFAULT 0);
ALTER TABLE PGS.NTCC ADD (SUBTOTAL8 NUMBER DEFAULT 0);

-- Nota de debito de compra
ALTER TABLE PGS.NTDC ADD (SUBNOOBJ NUMBER DEFAULT 0);
ALTER TABLE PGS.NTDC ADD (SUBEXENT NUMBER DEFAULT 0);
ALTER TABLE PGS.NTDC ADD (SUBTOTAL5 NUMBER DEFAULT 0);
ALTER TABLE PGS.NTDC ADD (SUBTOTAL8 NUMBER DEFAULT 0);

-- Liquidacion de compra (documento CXP)
ALTER TABLE PGS.LQCC ADD (SUBNOOBJ NUMBER DEFAULT 0);
ALTER TABLE PGS.LQCC ADD (SUBEXENT NUMBER DEFAULT 0);
ALTER TABLE PGS.LQCC ADD (SUBTOTAL5 NUMBER DEFAULT 0);
ALTER TABLE PGS.LQCC ADD (SUBTOTAL8 NUMBER DEFAULT 0);


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Comentarios, para que dentro de seis meses se sepa que es
--            cada columna sin leer este archivo.
-- ---------------------------------------------------------------------
COMMENT ON COLUMN PGS.FCTC.SUBEXENT IS
  'Base EXENTA de IVA (ATS baseImpExe). Tabla 17 del SRI, codigoPorcentaje 7. No incluida en SUBCERO ni en SUBNOOBJ; si en SUBTOTAL.';
COMMENT ON COLUMN PGS.NTCC.SUBNOOBJ IS 'Base NO OBJETO de IVA (ATS baseNoGraIva). codigoPorcentaje 6.';
COMMENT ON COLUMN PGS.NTCC.SUBEXENT IS 'Base EXENTA de IVA (ATS baseImpExe). codigoPorcentaje 7.';
COMMENT ON COLUMN PGS.NTCC.SUBTOTAL5 IS 'Base tarifa 5%. codigoPorcentaje 5.';
COMMENT ON COLUMN PGS.NTCC.SUBTOTAL8 IS 'Base tarifa 8%. codigoPorcentaje 8.';
COMMENT ON COLUMN PGS.NTDC.SUBNOOBJ IS 'Base NO OBJETO de IVA (ATS baseNoGraIva). codigoPorcentaje 6.';
COMMENT ON COLUMN PGS.NTDC.SUBEXENT IS 'Base EXENTA de IVA (ATS baseImpExe). codigoPorcentaje 7.';
COMMENT ON COLUMN PGS.NTDC.SUBTOTAL5 IS 'Base tarifa 5%. codigoPorcentaje 5.';
COMMENT ON COLUMN PGS.NTDC.SUBTOTAL8 IS 'Base tarifa 8%. codigoPorcentaje 8.';
COMMENT ON COLUMN PGS.LQCC.SUBNOOBJ IS 'Base NO OBJETO de IVA (ATS baseNoGraIva). codigoPorcentaje 6.';
COMMENT ON COLUMN PGS.LQCC.SUBEXENT IS 'Base EXENTA de IVA (ATS baseImpExe). codigoPorcentaje 7.';
COMMENT ON COLUMN PGS.LQCC.SUBTOTAL5 IS 'Base tarifa 5%. codigoPorcentaje 5.';
COMMENT ON COLUMN PGS.LQCC.SUBTOTAL8 IS 'Base tarifa 8%. codigoPorcentaje 8.';


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Control posterior: las trece columnas nuevas, todas en 0.
-- ESPERADO: las cuatro tablas con su juego completo, y CON_VALOR = 0 en
--           todas (este script no llena nada).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - columnas creadas' AS bloque,
       c.TABLE_NAME, COUNT(*) AS columnas_de_tarifa,
       LISTAGG(c.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY c.COLUMN_NAME) AS cuales
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND c.TABLE_NAME IN ('FCTC','NTCC','NTDC','LQCC')
   AND c.COLUMN_NAME IN ('SUBCERO','SUBNOOBJ','SUBEXENT','SUBTOTAL5','SUBTOTAL8')
 GROUP BY c.TABLE_NAME
 ORDER BY c.TABLE_NAME;


-- =====================================================================
-- DESPUES: desplegar el WAR que mapea estas columnas, y recien despues
-- correr el script de recalculo historico (todavia sin escribir: sale
-- despues de leer el e2-59).
--
-- REVERSO — comentado. Las columnas nacen vacias, asi que borrarlas no
-- pierde ningun dato, pero SOLO se pueden borrar con un WAR que no las
-- mapee:
--   ALTER TABLE PGS.FCTC DROP COLUMN SUBEXENT;
--   ALTER TABLE PGS.NTCC DROP (SUBNOOBJ, SUBEXENT, SUBTOTAL5, SUBTOTAL8);
--   ALTER TABLE PGS.NTDC DROP (SUBNOOBJ, SUBEXENT, SUBTOTAL5, SUBTOTAL8);
--   ALTER TABLE PGS.LQCC DROP (SUBNOOBJ, SUBEXENT, SUBTOTAL5, SUBTOTAL8);
-- =====================================================================
