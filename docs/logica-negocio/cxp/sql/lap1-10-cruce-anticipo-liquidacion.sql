/* ============================================================================
   lap1-10  CRUZAR ANTICIPOS DE PROVEEDOR CONTRA LIQUIDACIONES DE COMPRA
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxp
   ============================================================================

   ⛔ VA ANTES DE DESPLEGAR EL WAR.
      Las dos columnas quedan mapeadas en las entidades. Hibernate incluye TODA
      columna @Column en el SELECT que genera, asi que si el WAR sube y no
      existen, NO falla la funcion nueva:

        - falta APLPLQCC -> revienta TODA lectura de PGS.APLP con ORA-00904, o
          sea el cruce de anticipos, el saldo de facturas y la anulacion en
          cascada de documentos de compra;
        - falta LQCCEPAG -> revienta TODA lectura de PGS.LQCC.

   Diseno: docs/logica-negocio/cxp/DISENO-CRUCE-ANTICIPO-CONTRA-LIQUIDACION.md

   Corre de corrido: los controles son SELECT y el reverso esta comentado.
   ============================================================================ */


/* ============================================================================
   BLOQUE 0 · CONTROL PREVIO — esperado: 0 filas
   ----------------------------------------------------------------------------
   Si devuelve filas, alguna columna ya existe: NO correr ese ALTER.
   ============================================================================ */
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND ( (c.TABLE_NAME = 'APLP' AND c.COLUMN_NAME = 'APLPLQCC')
      OR (c.TABLE_NAME = 'LQCC' AND c.COLUMN_NAME = 'LQCCEPAG') );


/* ============================================================================
   BLOQUE 1 · La liquidacion afectada por una aplicacion
   ----------------------------------------------------------------------------
   Nullable: la enorme mayoria de las aplicaciones afectan una FACTURA y esta
   columna queda vacia. Es excluyente con APLPFCTC — una aplicacion afecta una
   factura O una liquidacion, nunca las dos. Esa regla la sostiene el codigo,
   no un CHECK: un CHECK sobre datos historicos podria fallar al crearse si
   alguna fila vieja no lo cumpliera, y no hay forma de saberlo sin mirar.
   ============================================================================ */
ALTER TABLE PGS.APLP ADD (APLPLQCC NUMBER);

ALTER TABLE PGS.APLP ADD CONSTRAINT FK_APLP_LQCC
    FOREIGN KEY (APLPLQCC) REFERENCES PGS.LQCC (ID);

CREATE INDEX PGS.IX_APLP_LQCC ON PGS.APLP (APLPLQCC);


/* ============================================================================
   BLOQUE 2 · Estado de pago de la liquidacion
   ----------------------------------------------------------------------------
   Hoy PGS.LQCC NO tiene estado de pago — verificado columna por columna contra
   la entidad. Sin el no hay donde registrar que una liquidacion quedo parcial o
   pagada, y la pantalla no podria distinguir una ya cruzada de una pendiente.

   Espejo de FCTC.FCTCEPAG: 1 = pendiente, 2 = pago parcial, 3 = pagada.

   ⚠️ NULLABLE a proposito, igual que arriba: una columna DEFAULT n NOT NULL
   rompe TODO INSERT de la entidad (Hibernate siempre nombra la columna y el
   DEFAULT de Oracle solo actua cuando el INSERT la omite). Es la leccion de
   CBR.ANTC.ANTCAPLC del 31-08. La integridad la da el inicializador en Java.
   ============================================================================ */
ALTER TABLE PGS.LQCC ADD (LQCCEPAG NUMBER(1) DEFAULT 1);


/* ============================================================================
   BLOQUE 3 · Backfill del estado de pago de las liquidaciones existentes
   ----------------------------------------------------------------------------
   Todas las liquidaciones ya cargadas quedan en PENDIENTE (1), que es lo
   correcto: hasta hoy NINGUNA pudo tener un cruce, porque la columna que lo
   permite se acaba de crear en el BLOQUE 1. No hay historia que reconstruir.

   El DEFAULT ya cubre las filas existentes en Oracle; este UPDATE es la red
   para las que pudieran tener NULL por cualquier motivo.
   ============================================================================ */
UPDATE PGS.LQCC SET LQCCEPAG = 1 WHERE LQCCEPAG IS NULL;

COMMIT;


/* ============================================================================
   BLOQUE 4 · CONTROL DESPUES
   ----------------------------------------------------------------------------
   Esperado:
     - APLPLQCC  NUMBER      NULLABLE = 'Y'
     - LQCCEPAG  NUMBER(1)   NULLABLE = 'Y'   DATA_DEFAULT = 1
     - FK_APLP_LQCC  ENABLED
     - IX_APLP_LQCC  OWNER = 'PGS', STATUS = 'VALID'
     - APLICACIONES_CON_LIQUIDACION = 0  (ninguna todavia, es correcto)
     - LIQUIDACIONES_SIN_ESTADO     = 0
   ============================================================================ */
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND ( (c.TABLE_NAME = 'APLP' AND c.COLUMN_NAME = 'APLPLQCC')
      OR (c.TABLE_NAME = 'LQCC' AND c.COLUMN_NAME = 'LQCCEPAG') )
 ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

SELECT k.CONSTRAINT_NAME, k.CONSTRAINT_TYPE, k.STATUS, k.VALIDATED
  FROM ALL_CONSTRAINTS k
 WHERE k.OWNER = 'PGS'
   AND k.TABLE_NAME = 'APLP'
   AND k.CONSTRAINT_NAME = 'FK_APLP_LQCC';

SELECT i.OWNER, i.INDEX_NAME, i.TABLE_OWNER, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.TABLE_OWNER = 'PGS'
   AND i.TABLE_NAME = 'APLP'
   AND i.INDEX_NAME = 'IX_APLP_LQCC';

SELECT (SELECT COUNT(*) FROM PGS.APLP WHERE APLPLQCC IS NOT NULL) AS aplicaciones_con_liquidacion,
       (SELECT COUNT(*) FROM PGS.LQCC WHERE LQCCEPAG IS NULL)     AS liquidaciones_sin_estado
  FROM DUAL;


/* ============================================================================
   REVERSO · COMENTADO A PROPOSITO
   ----------------------------------------------------------------------------
   Solo es seguro si NINGUNA aplicacion tiene liquidacion: el control de arriba
   tiene que dar APLICACIONES_CON_LIQUIDACION = 0. Si hay alguna, borrar la
   columna pierde el cruce y deja el anticipo consumido sin rastro de contra que.

   Y el WAR tiene que volver a la version anterior en el mismo movimiento.

   -- DROP INDEX PGS.IX_APLP_LQCC;
   -- ALTER TABLE PGS.APLP DROP CONSTRAINT FK_APLP_LQCC;
   -- ALTER TABLE PGS.APLP DROP COLUMN APLPLQCC;
   -- ALTER TABLE PGS.LQCC DROP COLUMN LQCCEPAG;
   ============================================================================ */
