-- =====================================================================
-- CXP: cerrar el hueco que permite borrar un producto en uso
-- Modulo: PGS
-- Fecha:  2026-08-27
-- Autor:  orquestador (verificado contra la BD local, copia de produccion)
--
-- EL PROBLEMA
--   PGS.DFCC (detalle de factura de compra) tiene UNA sola FK declarada,
--   la que apunta a la factura. La columna PRODUCTO no tiene ninguna, asi
--   que borrar un producto que ya esta en uso no encuentra resistencia.
--   Ya paso: hay 16 lineas en 5 facturas apuntando a productos que no
--   existen.
--
--   Dos puntas contribuyen y la FK cierra las dos:
--     - ProductoPagoServiceImpl.remove borra sin comprobar referencias
--       (la causa real de las 16 actuales).
--     - DetalleFacturaCompraServiceImpl.saveSingle inserta el producto sin
--       verificar que exista (podria crear huerfanas nuevas).
--
-- POR QUE **NO** SE CORRIGE EL HISTORICO
--   Se investigo si bastaba con reapuntar esas 16 lineas a un producto de
--   reemplazo. No basta, y el dato es concluyente: el MISMO producto se
--   contabilizo en DOS cuentas distintas.
--
--     producto 133 y 136 (tintas Epson)
--       factura 122 -> asiento 7492 -> 4.4.06.05 MATERIAL DE OFICINA
--       factura 159 -> asiento 7529 -> 4.4.03.15 MANTENIMIENTO DE
--                                      MOBILIARIOS Y EQUIPOS
--
--   Es decir: los productos se reclasificaron de grupo en algun momento
--   entre las dos facturas. Restaurarlos a UN grupo reproduce un asiento y
--   contradice el otro. Ademas los cinco asientos ya estan generados y
--   cuatro de las cinco facturas estan pagadas.
--
--   Por eso el historico se deja como esta -el asiento es correcto y ya
--   esta contabilizado- y la FK se crea con ENABLE NOVALIDATE: protege de
--   aqui en adelante sin exigir reescribir el pasado. Es la razon por la
--   que existe NOVALIDATE.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) hoy debe haber 1 sola FK en DFCC (la de FACTURA)
--   (b) las 16 lineas huerfanas, para dejar constancia del numero exacto
--       en el momento de ejecutar. Si en produccion no son 16, anotarlo:
--       significa que se siguieron borrando productos en uso.
-- ---------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'DFCC' AND CONSTRAINT_TYPE = 'R';

SELECT COUNT(*) AS LINEAS_HUERFANAS, COUNT(DISTINCT FACTURA) AS FACTURAS
  FROM PGS.DFCC D
 WHERE D.PRODUCTO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM PGS.PRDP P WHERE P.ID = D.PRODUCTO);

-- Detalle, para el registro
SELECT D.FACTURA, F.NUMERO, TO_CHAR(F.FECHA,'YYYY-MM-DD') AS FECHA,
       D.PRODUCTO AS PRODUCTO_INEXISTENTE, SUBSTR(D.DESCRIPCION,1,50) AS DESCRIPCION
  FROM PGS.DFCC D
  JOIN PGS.FCTC F ON F.ID = D.FACTURA
 WHERE D.PRODUCTO IS NOT NULL
   AND NOT EXISTS (SELECT 1 FROM PGS.PRDP P WHERE P.ID = D.PRODUCTO)
 ORDER BY D.FACTURA, D.ID;

-- ---------------------------------------------------------------------
-- BLOQUE 1: la FK, sin validar el historico
--   NOVALIDATE = las filas que YA existen no se revisan; toda insercion o
--   actualizacion a partir de ahora si. Y con la FK viva, Oracle rechaza
--   borrar un producto referenciado, que es lo que hay que impedir.
--   PRODUCTO admite nulo y la FK lo permite: una linea sin producto sigue
--   siendo valida.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.DFCC ADD CONSTRAINT FK_DFCC_PRDP
    FOREIGN KEY (PRODUCTO) REFERENCES PGS.PRDP(ID) ENABLE NOVALIDATE;

-- El indice no lo crea Oracle solo en una FK, y sin el, borrar un producto
-- obliga a un full scan de DFCC. Prefijar el schema: sin prefijo el indice
-- queda en el schema de la SESION y luego da ORA-01408.
CREATE INDEX PGS.IDX_DFCC_PRODUCTO ON PGS.DFCC(PRODUCTO);

COMMENT ON COLUMN PGS.DFCC.PRODUCTO IS 'Producto de pago (PGS.PRDP). FK creada el 2026-08-27 con NOVALIDATE: 16 lineas historicas apuntan a productos borrados y no se corrigen porque el mismo producto se contabilizo en cuentas distintas segun la factura';

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   (a) deben verse 2 FK: la de factura y FK_DFCC_PRDP
--   (b) FK_DFCC_PRDP debe salir STATUS=ENABLED y VALIDATED=NOT VALIDATED
--   (c) el indice debe pertenecer a la tabla de PGS (filtrar por
--       TABLE_OWNER, no por OWNER)
-- ---------------------------------------------------------------------
SELECT CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'DFCC' AND CONSTRAINT_TYPE = 'R'
 ORDER BY CONSTRAINT_NAME;

SELECT OWNER, INDEX_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_INDEXES
 WHERE TABLE_OWNER = 'PGS' AND TABLE_NAME = 'DFCC' AND INDEX_NAME = 'IDX_DFCC_PRODUCTO';

-- Prueba de que la puerta quedo cerrada: este DELETE debe fallar con
-- ORA-02292 (registro secundario encontrado). Ejecutarlo y NO confirmar.
-- SELECT ID FROM PGS.PRDP WHERE ID IN (SELECT PRODUCTO FROM PGS.DFCC WHERE ROWNUM = 1);
-- DELETE FROM PGS.PRDP WHERE ID = <ese id>;   -- debe dar ORA-02292
-- ROLLBACK;
