/* ============================================================================
   lap1-03  UN CHEQUE PUEDE RESPALDAR VARIOS PAGOS
   Equipo lap-saa-1 (laptop)  ·  2026-09-01  ·  modulo tsr / pagos
   ============================================================================

   QUE HACE
   --------
   Reemplaza el indice UNICO PGS.UQ_PGTR_DTCH por uno NORMAL. El indice se sigue
   necesitando —se consulta PGTRDTCH para saber que pagos respalda un cheque—;
   lo que se retira es la unicidad, que es lo que hoy impide agrupar.

   ⛔ ESTE SCRIPT VA **ANTES** DE DESPLEGAR EL WAR.
   Al reves no rompe nada, pero la pantalla ofreceria agrupar y el INSERT
   fallaria con ORA-00001 en la cara del usuario.

   ⛔ Y NO SE CORRE SOLO. Quitar la unicidad SIN el codigo nuevo deja la
   condicion de carrera de dos usuarios tomando el mismo cheque sin su red
   final. La proteccion pasa a estar en asignarAGrupo, que toma el cheque UNA
   sola vez por grupo bajo lock pesimista. Ver
   docs/logica-negocio/tsr/DISENO-UN-CHEQUE-VARIOS-PAGOS.md §5.2.

   Corre de corrido y es seguro: los dos bloques de control son SELECT y el
   reverso esta comentado.
   ============================================================================ */


/* ============================================================================
   BLOQUE 0 · CONTROL ANTES — leer la salida antes de seguir
   ----------------------------------------------------------------------------
   Esperado:
     - UQ_PGTR_DTCH aparece con UNIQUENESS = 'UNIQUE'
     - PAGOS_CON_CHEQUE_REPETIDO = 0  (tiene que ser 0: hoy es imposible que no
       lo sea, y si no lo fuera el indice unico no estaria valido)

   Se filtra por TABLE_OWNER y no por OWNER a proposito: un CREATE INDEX sin
   prefijo de schema deja el indice en el schema de la sesion, y filtrando por
   OWNER se vuelve invisible.
   ============================================================================ */
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_OWNER, i.TABLE_NAME, i.UNIQUENESS, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.TABLE_OWNER = 'PGS'
   AND i.TABLE_NAME  = 'PGTR'
   AND i.INDEX_NAME IN ('UQ_PGTR_DTCH', 'IX_PGTR_DTCH');

SELECT COUNT(*) AS pagos_con_cheque_repetido
  FROM (SELECT PGTRDTCH
          FROM PGS.PGTR
         WHERE PGTRDTCH IS NOT NULL
         GROUP BY PGTRDTCH
        HAVING COUNT(*) > 1);


/* ============================================================================
   BLOQUE 1 · Retirar la unicidad
   ----------------------------------------------------------------------------
   Se hace en dos pasos —DROP y CREATE— y no con un ALTER, porque en Oracle la
   unicidad de un indice no se modifica en el lugar.

   El CREATE va con prefijo de schema (PGS.IX_...): sin el, el indice queda en
   el schema de la sesion, ocupa igual la columna y es invisible a cualquier
   control que filtre por OWNER. Ya costo tiempo antes en este modulo.

   Se renombra a IX_ porque el nombre tiene que decir la verdad: un indice que
   se llama UQ_ y no es unico enganna a quien lo lea dentro de seis meses.
   ============================================================================ */
DROP INDEX PGS.UQ_PGTR_DTCH;

CREATE INDEX PGS.IX_PGTR_DTCH ON PGS.PGTR(PGTRDTCH);


/* ============================================================================
   BLOQUE 2 · CONTROL DESPUES
   ----------------------------------------------------------------------------
   Esperado:
     - UQ_PGTR_DTCH ya no aparece
     - IX_PGTR_DTCH aparece con UNIQUENESS = 'NONUNIQUE' y STATUS = 'VALID'
   ============================================================================ */
SELECT i.OWNER, i.INDEX_NAME, i.TABLE_OWNER, i.TABLE_NAME, i.UNIQUENESS, i.STATUS
  FROM ALL_INDEXES i
 WHERE i.TABLE_OWNER = 'PGS'
   AND i.TABLE_NAME  = 'PGTR'
   AND i.INDEX_NAME IN ('UQ_PGTR_DTCH', 'IX_PGTR_DTCH');


/* ============================================================================
   REVERSO · COMENTADO A PROPOSITO — NO EJECUTAR SIN LEER
   ----------------------------------------------------------------------------
   Volver a la unicidad SOLO es posible si NO se giro ningun cheque agrupado.
   Si ya existe uno, el CREATE UNIQUE INDEX falla con ORA-01452 (claves
   duplicadas) y la unica salida seria reversar esos pagos, que es una decision
   de negocio y no un reverso tecnico.

   Correr primero el control del BLOQUE 0: si PAGOS_CON_CHEQUE_REPETIDO > 0,
   NO intentar el reverso.

   -- DROP INDEX PGS.IX_PGTR_DTCH;
   -- CREATE UNIQUE INDEX PGS.UQ_PGTR_DTCH ON PGS.PGTR(PGTRDTCH);

   Y el WAR tiene que volver a la version anterior en el mismo movimiento: con
   el codigo nuevo y el indice unico, agrupar falla con ORA-00001.
   ============================================================================ */
