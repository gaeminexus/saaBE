-- =====================================================================
-- e2-41 — PGS.PGTR: un pago programado puede pagar una LIQUIDACION DE COMPRA
-- Modulo: cxp / pagos  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-14
--
-- ⚠️ NO ES LECTURA. Agrega una columna, una FK y un indice.
--
-- QUE HACE
--   Agrega PGS.PGTR.PGTRLQCC -> FK a PGS.LQCC(ID). Hoy un pago programado
--   solo puede apuntar a una factura (PGTRFCTC), a un egreso (PGTREGRS), a
--   un anticipo (PGTRANTP) o a un origen externo (PGTRORGN/PGTRIDOR). No hay
--   donde guardar que un pago es de una liquidacion de compra, y por eso la
--   Solicitud de pago nunca pudo ofrecerlas.
--
--   Diseño: docs/logica-negocio/cxp/PLAN-PAGO-LIQUIDACION-Y-NOTA-VENTA.md
--
-- LA NOTA DE VENTA NO NECESITA COLUMNA
--   Se graba como PGS.FCTC con TIPOCOMPROBANTE = '02' (FacturaCompraServiceImpl),
--   asi que se paga por PGTRFCTC igual que una factura.
--
-- ES SEGURO CORRERLO CON EL SISTEMA ARRIBA
--   Columna nullable: no reescribe filas. Ninguna fila existente la usa.
--   Mismo schema (PGS -> PGS): NO hace falta GRANT REFERENCES.
--
-- ⛔ ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES del WAR que mapee PGTRLQCC. Si el WAR sube primero,
--   PagoProgramado mapea una columna que no existe: ORA-00904 en TODA
--   lectura de pagos — la bandeja de aprobacion, los lotes, el archivo del
--   banco, la solicitud. Todo el circuito de pagos de todos los modulos.
--   Y el mapeo no se mergea a main hasta que esto este corrido (registro de
--   reservas §7).
--
-- REVERSO al final, COMENTADO.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 La columna NO debe existir. ESPERADO: 0 filas.
--     Si devuelve 1 fila, NO seguir: avisar al arbitro.
SELECT 'BLOQUE 0.1 - PGTRLQCC no existe' AS bloque, column_name
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'PGTR' AND column_name = 'PGTRLQCC';

-- 0.2 Las FK de documento/origen de PGTR deben ser NULLABLE = 'Y': son
--     excluyentes entre si. ESPERADO: las cuatro en 'Y'.
--     Si alguna sale 'N', PARAR (es el ORA-01400 del e2-05 otra vez).
SELECT 'BLOQUE 0.2 - FKs excluyentes' AS bloque, column_name, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'PGTR'
   AND column_name IN ('PGTRFCTC','PGTREGRS','PGTRANTP','PGTRIDOR')
 ORDER BY nullable, column_name;

-- 0.3 La tabla destino tiene ID como PK (una FK exige PK o UNIQUE).
--     ESPERADO: 1 fila con constraint_type = 'P' sobre la columna ID.
SELECT 'BLOQUE 0.3 - PK de LQCC' AS bloque, c.constraint_name, c.constraint_type, cc.column_name
  FROM all_constraints c
  JOIN all_cons_columns cc
    ON cc.owner = c.owner AND cc.constraint_name = c.constraint_name
 WHERE c.owner = 'PGS' AND c.table_name = 'LQCC'
   AND c.constraint_type IN ('P','U');

-- 0.4 Linea base de pagos. Anotar los numeros: el 3.4 debe dar igual.
SELECT 'BLOQUE 0.4 - linea base' AS bloque,
       COUNT(*) AS total_pagos,
       SUM(CASE WHEN PGTRFCTC IS NOT NULL THEN 1 ELSE 0 END) AS de_factura
  FROM PGS.PGTR;

-- 0.5 Las liquidaciones que se van a pagar tienen su documento CXP.
--     ESPERADO: filas con LQCCEPAG 1 o 2 y TIENE_ASIENTO = 'SI'.
--     Si una de las que hay que pagar NO aparece aca, falta pulsar
--     "Generar documento CXP" en la liquidacion antes de poder pagarla.
SELECT 'BLOQUE 0.5 - LQCC por pagar' AS bloque,
       q.ID, q.NUMERO, q.TITULAR, q.TOTAL, q.ESTADO, q.LQCCEPAG,
       CASE WHEN q.ASIENTO IS NULL THEN 'NO' ELSE 'SI' END AS tiene_asiento
  FROM PGS.LQCC q
 WHERE NVL(q.LQCCEPAG, 1) <> 3
 ORDER BY q.ID DESC;


-- =====================================================================
-- BLOQUE 1 -- LA COLUMNA
-- =====================================================================

ALTER TABLE PGS.PGTR ADD (PGTRLQCC NUMBER NULL);

COMMENT ON COLUMN PGS.PGTR.PGTRLQCC IS
    'Liquidacion de compra (PGS.LQCC) que paga este pago. Excluyente con PGTRFCTC, PGTREGRS, PGTRANTP y el origen externo: un pago paga UN documento';


-- =====================================================================
-- BLOQUE 2 -- LA FK Y EL INDICE
-- =====================================================================

ALTER TABLE PGS.PGTR ADD CONSTRAINT FK_PGTR_LQCC
    FOREIGN KEY (PGTRLQCC) REFERENCES PGS.LQCC (ID);

-- Prefijo de schema tambien en el indice (la trampa del e2-04).
CREATE INDEX PGS.IX_PGTR_LQCC ON PGS.PGTR (PGTRLQCC);


-- =====================================================================
-- BLOQUE 3 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 3.1 ESPERADO: 1 fila, NUMBER, 'Y'.
SELECT 'BLOQUE 3.1 - columna' AS bloque, column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'PGTR' AND column_name = 'PGTRLQCC';

-- 3.2 ESPERADO: FK_PGTR_LQCC / ENABLED / LQCC.
SELECT 'BLOQUE 3.2 - FK' AS bloque, c.constraint_name, c.status, r.table_name AS referencia
  FROM all_constraints c
  LEFT JOIN all_constraints r
         ON r.owner = c.r_owner AND r.constraint_name = c.r_constraint_name
 WHERE c.owner = 'PGS' AND c.table_name = 'PGTR'
   AND c.constraint_name = 'FK_PGTR_LQCC';

-- 3.3 ESPERADO: owner PGS (no el schema del ejecutor).
SELECT 'BLOQUE 3.3 - indice' AS bloque, owner, index_name
  FROM all_indexes
 WHERE table_name = 'PGTR' AND index_name = 'IX_PGTR_LQCC';

-- 3.4 ESPERADO: los mismos numeros que el 0.4, y CON_LIQUIDACION = 0.
SELECT 'BLOQUE 3.4 - nada cambio' AS bloque,
       COUNT(*) AS total_pagos,
       SUM(CASE WHEN PGTRFCTC IS NOT NULL THEN 1 ELSE 0 END) AS de_factura,
       SUM(CASE WHEN PGTRLQCC IS NOT NULL THEN 1 ELSE 0 END) AS con_liquidacion
  FROM PGS.PGTR;


-- =====================================================================
-- REVERSO -- COMENTADO. Solo si hay que deshacer ANTES de que exista
-- algun pago con PGTRLQCC. Si el 3.4 de una corrida posterior da
-- CON_LIQUIDACION > 0, NO revertir: esos pagos quedarian sin documento.
-- Y el WAR que mapea PGTRLQCC debe bajarse ANTES de correr esto.
-- =====================================================================
-- DROP INDEX PGS.IX_PGTR_LQCC;
-- ALTER TABLE PGS.PGTR DROP CONSTRAINT FK_PGTR_LQCC;
-- ALTER TABLE PGS.PGTR DROP COLUMN PGTRLQCC;
