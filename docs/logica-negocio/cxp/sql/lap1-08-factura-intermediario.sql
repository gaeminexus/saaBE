/* ============================================================================
   lap1-08  FACTURAS DE INTERMEDIARIO — dos columnas en PGS.FCTC
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxp
   ============================================================================

   ⛔ VA ANTES DE DESPLEGAR EL WAR.
      La entidad com.saa.model.cxp.FacturaCompra mapea las dos columnas nuevas.
      Hibernate incluye TODA columna @Column en el SELECT que genera, asi que si
      el WAR sube y las columnas no existen, NO falla la funcion nueva:

          FALLA TODA LECTURA DE PGS.FCTC CON ORA-00904.

      Es decir, la bandeja de documentos, la consulta de facturas de compra, los
      pagos y la contabilizacion. En pantallas sin ninguna relacion aparente con
      esta funcion.

   Diseno: docs/logica-negocio/cxp/DISENO-FACTURA-INTERMEDIARIO.md

   Corre de corrido: los bloques de control son SELECT y el reverso esta
   comentado.
   ============================================================================ */


/* ============================================================================
   BLOQUE 0 · CONTROL PREVIO — esperado: 0 filas
   ----------------------------------------------------------------------------
   Si devuelve filas, las columnas ya existen: NO correr el ALTER, saltar al
   BLOQUE 2.
   ============================================================================ */
SELECT c.OWNER, c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND c.TABLE_NAME = 'FCTC'
   AND c.COLUMN_NAME IN ('FCTCESIN', 'FCTCPRIN');


/* ============================================================================
   BLOQUE 1 · Las dos columnas
   ----------------------------------------------------------------------------
   ⚠️ FCTCESIN va NULLABLE a proposito, y NO es un descuido.

   Es la leccion de CBR.ANTC.ANTCAPLC del 2026-08-31: una columna
   "DEFAULT 0 NOT NULL" ROMPE TODO INSERT de la entidad, porque Hibernate
   SIEMPRE nombra la columna en el INSERT y el DEFAULT de Oracle solo actua
   cuando el INSERT la OMITE. Con la columna nombrada y en NULL, salta
   ORA-01400 y no se puede registrar ninguna factura.

   La integridad se consigue con el inicializador en Java (private Long
   esIntermediario = 0L), no endureciendo la base.

   El DEFAULT 0 se deja igual: sirve para las filas existentes y para cualquier
   INSERT hecho a mano que omita la columna.
   ============================================================================ */
ALTER TABLE PGS.FCTC ADD (FCTCESIN NUMBER(1) DEFAULT 0);

ALTER TABLE PGS.FCTC ADD (FCTCPRIN NUMBER);


/* ============================================================================
   BLOQUE 2 · La FK al producto
   ----------------------------------------------------------------------------
   PGS -> PGS: misma cuenta, asi que NO hace falta GRANT REFERENCES (que si
   haria falta cruzando de schema, y ya costo tiempo antes en este proyecto).

   El indice va con prefijo de schema: un CREATE INDEX sin prefijo deja el
   indice en el schema de la sesion, ocupa la columna igual y es invisible a
   cualquier control que filtre por OWNER.
   ============================================================================ */
ALTER TABLE PGS.FCTC ADD CONSTRAINT FK_FCTC_PRIN
    FOREIGN KEY (FCTCPRIN) REFERENCES PGS.PRDP (ID);

CREATE INDEX PGS.IX_FCTC_PRIN ON PGS.FCTC (FCTCPRIN);


/* ============================================================================
   BLOQUE 3 · CONTROL DESPUES
   ----------------------------------------------------------------------------
   Esperado:
     - FCTCESIN  NUMBER(1)  NULLABLE = 'Y'  DATA_DEFAULT = 0
     - FCTCPRIN  NUMBER     NULLABLE = 'Y'
     - FK_FCTC_PRIN  con STATUS = 'ENABLED'
     - IX_FCTC_PRIN  con OWNER = 'PGS' y STATUS = 'VALID'
     - FACTURAS_MARCADAS = 0 (ninguna factura existente queda marcada)
   ============================================================================ */
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE, c.DATA_DEFAULT
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'PGS'
   AND c.TABLE_NAME = 'FCTC'
   AND c.COLUMN_NAME IN ('FCTCESIN', 'FCTCPRIN')
 ORDER BY c.COLUMN_NAME;

SELECT k.CONSTRAINT_NAME, k.CONSTRAINT_TYPE, k.STATUS, k.VALIDATED
  FROM ALL_CONSTRAINTS k
 WHERE k.OWNER = 'PGS'
   AND k.TABLE_NAME = 'FCTC'
   AND k.CONSTRAINT_NAME = 'FK_FCTC_PRIN';

SELECT i.OWNER, i.INDEX_NAME, i.TABLE_OWNER, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.TABLE_OWNER = 'PGS'
   AND i.TABLE_NAME = 'FCTC'
   AND i.INDEX_NAME = 'IX_FCTC_PRIN';

SELECT COUNT(*) AS facturas_marcadas
  FROM PGS.FCTC
 WHERE FCTCESIN = 1;


/* ============================================================================
   REVERSO · COMENTADO A PROPOSITO
   ----------------------------------------------------------------------------
   Solo es seguro si NINGUNA factura quedo marcada como intermediario: el
   control de arriba tiene que dar FACTURAS_MARCADAS = 0. Si hay alguna,
   borrar la columna pierde el dato de por que se contabilizo asi.

   Y el WAR tiene que volver a la version anterior en el mismo movimiento: con
   el WAR nuevo y las columnas borradas, toda lectura de PGS.FCTC da ORA-00904.

   -- DROP INDEX PGS.IX_FCTC_PRIN;
   -- ALTER TABLE PGS.FCTC DROP CONSTRAINT FK_FCTC_PRIN;
   -- ALTER TABLE PGS.FCTC DROP COLUMN FCTCPRIN;
   -- ALTER TABLE PGS.FCTC DROP COLUMN FCTCESIN;
   ============================================================================ */
