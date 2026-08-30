-- =====================================================================
-- CXC: seguimiento de la devolucion de un anticipo de cliente
-- Esquema: CBR  |  Tabla: ANTC
-- Fecha:  2026-08-28
-- Autor:  arbitro (Opcion A del analisis de idempotencia, elegida por
--         el usuario -- ver docs/logica-negocio/ESTADO-GENERAL-TRABAJO-EN-CURSO.md,
--         frente J)
--
-- PARA QUE
--   La ola de Aprobacion de Pagos agrego el origen externo
--   CXC_DEVOLUCION_CLIENTE: un AnticipoCliente con saldo a favor puede
--   pedir que se le devuelva dinero, y eso entra al circuito unico de
--   PGS.PGTR igual que ya lo hacen CRD_DEVOLUCION_APORTE, TSR_CAJA_CHICA
--   y RHH_ANTICIPO_EMPLEADO.
--
--   Falta la mitad de vuelta: cuando ese PGS.PGTR llega a CONFIRMADO,
--   alguien tiene que descontar CBR.ANTC.ANTCSALD. El mecanismo elegido
--   es un reconciliador (mismo patron que ya usa
--   CRD.DevolucionAporteServiceImpl.sincronizarDevolucion), no un hook
--   automatico. Para que ese reconciliador sea IDEMPOTENTE -- correrlo
--   dos veces sobre el mismo pago CONFIRMADO no debe descontar el saldo
--   dos veces -- CBR.ANTC necesita dos columnas nuevas.
--
-- QUE HACE
--   Agrega a CBR.ANTC:
--     ANTCIDPG NUMBER   -- FK a PGS.PGTR.PGTRCDGO: el pago de devolucion
--                          vigente/mas reciente asociado a este anticipo.
--     ANTCAPLC NUMBER(1) DEFAULT 0 NOT NULL
--                       -- 0/1: si el efecto de ANTCIDPG (el descuento de
--                          saldo) ya se aplico.
--   Mas el GRANT REFERENCES que PGS.PGTR no tiene concedido hoy (ver
--   bloque 2 -- confirmado en la sesion del 2026-08-27 para otras FK
--   cross-schema contra la misma tabla, PGS.PGTR no tiene REFERENCES a
--   PUBLIC ni a CBR).
--
-- LIMITE ACEPTADO (decision del usuario, opcion A y no B)
--   Solo trackea LA ULTIMA devolucion por anticipo, no un historial. Si
--   se necesita auditoria de devoluciones pasadas mas adelante, hace
--   falta una tabla propia (CBR.DVCL, opcion B que no se eligio ahora).
--
-- POR QUE NO ROMPE NADA
--   Columnas nuevas, nullable/con default, sobre una tabla existente.
--   Ningun codigo hoy lee ANTCIDPG/ANTCAPLC. ANTCAPLC nace en 0 para las
--   filas existentes (ninguna tiene devolucion en curso todavia).
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) las columnas no deben existir todavia
--   (b) PGS.PGTR debe carecer de REFERENCES para CBR (si ya aparece,
--       saltar el GRANT del bloque 2 -- no falla si se repite, pero no
--       hace falta)
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CBR' AND TABLE_NAME = 'ANTC'
   AND COLUMN_NAME IN ('ANTCIDPG', 'ANTCAPLC');
-- Debe devolver 0 filas.

SELECT GRANTEE, OWNER, TABLE_NAME
  FROM DBA_TAB_PRIVS
 WHERE PRIVILEGE = 'REFERENCES' AND OWNER = 'PGS' AND TABLE_NAME = 'PGTR'
   AND GRANTEE IN ('CBR', 'PUBLIC');
-- Si no aparece CBR ni PUBLIC, el GRANT del bloque 2 hace falta.

-- ---------------------------------------------------------------------
-- BLOQUE 1: columnas nuevas
-- ---------------------------------------------------------------------
ALTER TABLE CBR.ANTC ADD (
    ANTCIDPG NUMBER,
    ANTCAPLC NUMBER(1) DEFAULT 0 NOT NULL
);

COMMENT ON COLUMN CBR.ANTC.ANTCIDPG IS 'FK a PGS.PGTR.PGTRCDGO: pago de devolucion de saldo vigente/mas reciente de este anticipo (origen externo CXC_DEVOLUCION_CLIENTE). Solo trackea la ultima, no historial.';
COMMENT ON COLUMN CBR.ANTC.ANTCAPLC IS '0/1: si el descuento de ANTCSALD por el pago ANTCIDPG ya se aplico. Lo pone en 1 el reconciliador al ver ANTCIDPG en estado CONFIRMADO.';

-- ---------------------------------------------------------------------
-- BLOQUE 2: FK cross-schema -- necesita GRANT REFERENCES directo
--   (el rol DBA no basta para crear la FK; ver docs/... referencia sobre
--   esta trampa. Ejecutar como el owner de PGS o como DBA)
-- ---------------------------------------------------------------------
GRANT REFERENCES ON PGS.PGTR TO CBR;

ALTER TABLE CBR.ANTC ADD CONSTRAINT FK_ANTC_PAGODEV
    FOREIGN KEY (ANTCIDPG) REFERENCES PGS.PGTR(PGTRCDGO);

-- Indice prefijado con el schema de la TABLA (CBR), no el de la sesion
-- que ejecuta -- CREATE INDEX sin prefijo cae en el schema de la sesion.
CREATE INDEX CBR.IDX_ANTC_IDPG ON CBR.ANTC(ANTCIDPG);

-- ---------------------------------------------------------------------
-- BLOQUE 3: control final
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, DATA_TYPE, DATA_PRECISION, DATA_SCALE, NULLABLE, DATA_DEFAULT
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'CBR' AND TABLE_NAME = 'ANTC'
   AND COLUMN_NAME IN ('ANTCIDPG', 'ANTCAPLC');

SELECT CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'CBR' AND TABLE_NAME = 'ANTC' AND CONSTRAINT_NAME = 'FK_ANTC_PAGODEV';

SELECT INDEX_NAME, TABLE_OWNER, TABLE_NAME
  FROM ALL_INDEXES
 WHERE INDEX_NAME = 'IDX_ANTC_IDPG';
-- TABLE_OWNER debe ser CBR. Si el indice quedo con OWNER distinto de
-- CBR, se creo en el schema de la sesion -- hay que dropearlo y repetir
-- el CREATE INDEX con el prefijo CBR. explicito.

COMMIT;
