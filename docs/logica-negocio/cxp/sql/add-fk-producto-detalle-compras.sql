-- =====================================================================
-- FK real: no permitir borrar un ProductoPago que esta en uso en una
-- factura de compra o una nota de credito de compra
-- Esquema: PGS
-- Fecha:  2026-08-28
-- Autor:  arbitro
--
-- PARA QUE
--   docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §6.7: DFCC.PRODUCTO
--   y DTCC.PRODUCTO son Long planos sin FK -- eso permitio borrar un
--   ProductoPago que 16 lineas de 5 facturas (ids 122,159,189,190,191)
--   ya usaban, dejandolas huerfanas. Decision del usuario (2026-08-28):
--   quiere el control EN BASE y EN BACKEND, los dos. Este script es la
--   mitad de base; la mitad de backend la escribe el agente BE aparte
--   (validacion previa con mensaje claro, antes de que llegue a chocar
--   con esta FK).
--
-- QUE HACE
--   Agrega la FK que falta en las dos tablas que SI tienen el problema:
--     PGS.DFCC.PRODUCTO -> PGS.PRDP.ID  (factura de compra)
--     PGS.DTCC.PRODUCTO -> PGS.PRDP.ID  (nota de credito de compra)
--   Mas su indice (las FK sin indice bloquean lecturas al borrar el
--   padre; convencion del repo: FK con indice).
--
--   NO toca PGS.DLCM.PRODUCTO (liquidacion de compra): esa YA es un FK
--   real (@ManyToOne @JoinColumn) desde la ola de aprobacion de pagos,
--   verificado en el codigo -- nada que hacer ahi.
--   NO toca notas de debito de compra (DTDC): esa tabla no tiene columna
--   de producto, son lineas de descripcion libre -- no aplica.
--
-- POR QUE NO ROMPE NADA A PESAR DE LOS 5 HUERFANOS YA EXISTENTES
--   Se crea con ENABLE NOVALIDATE: Oracle NO revisa las filas ya
--   existentes al crear la constraint (las 5 facturas huerfanas se
--   quedan tal cual, como pidio el usuario -- "si no estan anuladas,
--   dejalas como estan"), pero SI la aplica desde ahora en adelante:
--   cualquier DELETE de un PGS.PRDP referenciado falla con ORA-02292,
--   y cualquier INSERT/UPDATE nuevo en DFCC.PRODUCTO/DTCC.PRODUCTO debe
--   apuntar a un producto que exista.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) confirmar que la FK todavia no existe en ninguna de las dos
--   (b) recordatorio de las 5 facturas huerfanas conocidas -- deben
--       seguir siendo exactamente esas 5, ninguna mas nueva
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DFCC','DTCC') AND CONSTRAINT_TYPE = 'R';
-- Debe devolver 0 filas antes de correr el bloque 1.

SELECT 'DFCC' AS TABLA, DF.FACTURA AS ID_DOCUMENTO, DF.PRODUCTO AS PRODUCTO_INEXISTENTE
  FROM PGS.DFCC DF
 WHERE NOT EXISTS (SELECT 1 FROM PGS.PRDP P WHERE P.ID = DF.PRODUCTO)
UNION ALL
SELECT 'DTCC' AS TABLA, DT.NOTACREDITO AS ID_DOCUMENTO, DT.PRODUCTO AS PRODUCTO_INEXISTENTE
  FROM PGS.DTCC DT
 WHERE NOT EXISTS (SELECT 1 FROM PGS.PRDP P WHERE P.ID = DT.PRODUCTO)
ORDER BY 1, 2;
-- Esperado en DFCC: exactamente las 16 lineas de las 5 facturas conocidas
-- (122,159,189,190,191). Si aparece algo en DTCC o filas nuevas en DFCC,
-- avisar antes de seguir -- el universo de huerfanos creció.

-- ---------------------------------------------------------------------
-- BLOQUE 1: FK con indice, ENABLE NOVALIDATE
-- ---------------------------------------------------------------------
CREATE INDEX PGS.IDX_DFCC_PRODUCTO ON PGS.DFCC(PRODUCTO);
ALTER TABLE PGS.DFCC ADD CONSTRAINT FK_DFCC_PRODUCTO
    FOREIGN KEY (PRODUCTO) REFERENCES PGS.PRDP(ID) ENABLE NOVALIDATE;

CREATE INDEX PGS.IDX_DTCC_PRODUCTO ON PGS.DTCC(PRODUCTO);
ALTER TABLE PGS.DTCC ADD CONSTRAINT FK_DTCC_PRODUCTO
    FOREIGN KEY (PRODUCTO) REFERENCES PGS.PRDP(ID) ENABLE NOVALIDATE;

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   (a) las dos FK deben existir, ENABLED, y NOVALIDATED (no VALIDATED --
--       es a proposito, por los 5 huerfanos que se quedan como estan)
--   (b) los indices deben quedar en el schema PGS (no en el de la sesion)
-- ---------------------------------------------------------------------
SELECT TABLE_NAME, CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME IN ('DFCC','DTCC') AND CONSTRAINT_TYPE = 'R';
-- STATUS=ENABLED, VALIDATED=NOT VALIDATED en las dos filas.

SELECT INDEX_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_INDEXES
 WHERE INDEX_NAME IN ('IDX_DFCC_PRODUCTO','IDX_DTCC_PRODUCTO');
-- TABLE_OWNER debe ser PGS en ambas.

COMMIT;

-- ---------------------------------------------------------------------
-- BLOQUE 3: reverso (comentado a proposito)
-- ---------------------------------------------------------------------
-- ALTER TABLE PGS.DFCC DROP CONSTRAINT FK_DFCC_PRODUCTO;
-- DROP INDEX PGS.IDX_DFCC_PRODUCTO;
-- ALTER TABLE PGS.DTCC DROP CONSTRAINT FK_DTCC_PRODUCTO;
-- DROP INDEX PGS.IDX_DTCC_PRODUCTO;
-- COMMIT;
