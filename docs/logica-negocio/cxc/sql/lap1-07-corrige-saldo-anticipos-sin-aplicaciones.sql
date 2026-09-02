/* ============================================================================
   lap1-07  CORRIGE EL SALDO DE LOS ANTICIPOS QUE NUNCA SE CRUZARON
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxc
   ============================================================================

   ⚠️ ESTE SI MODIFICA DATOS. Leer los tres bloques antes de correrlo.

   QUE DICEN LOS DATOS DE lap1-06
   ------------------------------
   Titular 133 (Anibal Eduardo Maldonado Martinez), dos anticipos, los dos de
   173,50 y los dos con CERO aplicaciones — ni activas ni reversadas:

     id 4  (7857720)  valor 173,50   saldo 347,00   <- 173,50 DE MAS
     id 3  (192164)   valor 173,50   saldo   0,00   <- 173,50 DE MENOS

   Un anticipo sin ninguna aplicacion tiene, por definicion, saldo = valor. Los
   dos estan mal, y en sentidos opuestos por el mismo monto.

   QUE QUEDO REFUTADO, y conviene anotarlo
   ---------------------------------------
   La hipotesis era que un reverso habia devuelto saldo que nunca se desconto
   (AplicacionPagoCxcServiceImpl:1002 suma sin comprobar que el consumo
   existiera). **El BLOQUE 2 de lap1-06 vino VACIO**: no hay ninguna fila en
   CBR.APLC con APLCANTO apuntando a estos anticipos. Nadie los cruzo nunca,
   asi que ese codigo no se ejecuto. La hipotesis era razonable y es falsa.

   ⛔ EL MECANISMO QUE ESCRIBIO ESOS SALDOS SIGUE SIN IDENTIFICARSE
   ---------------------------------------------------------------
   Descartados por lectura de codigo y por los datos:
     - el alta (setSaldo(valor) al crear) — deja saldo = valor, correcto;
     - el cruce y su reverso — no hay aplicaciones, no corrieron;
     - la devolucion (setSaldo(saldo - valorPago)) — marca ANTCAPLC = 1 y los
       dos anticipos tienen ANTCAPLC = 0, asi que tampoco corrio.

   **Esto es una correccion de DATOS, no un arreglo del defecto.** Si el
   mecanismo sigue vivo, los saldos se vuelven a torcer. Correr este script
   deja la pantalla bien hoy; encontrar al escritor queda pendiente.
   ============================================================================ */


/* ============================================================================
   BLOQUE 0 · CONTROL ANTES — leer la salida antes de seguir
   ----------------------------------------------------------------------------
   Esperado: exactamente las dos filas de arriba, con CERO aplicaciones cada
   una.

   ⛔ Si alguna trae APLICACIONES > 0, NO SIGAS: ese anticipo si fue cruzado y
      su saldo correcto NO es su valor. Pará y avisá.
   ============================================================================ */
SELECT a.ID            AS id_anticipo,
       a.NUMERODOC     AS numero,
       a.VALOR         AS valor,
       a.ANTCSALD      AS saldo_actual,
       a.ANTCAPLC      AS aplicado,
       a.ESTADO        AS estado,
       (SELECT COUNT(*) FROM CBR.APLC p WHERE p.APLCANTO = a.ID) AS aplicaciones
  FROM CBR.ANTC a
 WHERE a.ID IN (3, 4)
 ORDER BY a.ID;


/* ============================================================================
   BLOQUE 1 · La correccion
   ----------------------------------------------------------------------------
   Pone el saldo igual al valor SOLO en anticipos que no tienen ninguna
   aplicacion. La condicion NOT EXISTS es la salvaguarda: si mañana alguno de
   estos dos tuviera un cruce, el UPDATE no lo toca.

   Se acota por ID a proposito. NO se corre sobre toda la tabla: el BLOQUE 3
   de lap1-06 puede tener otros casos cuya causa no sea la misma, y un UPDATE
   masivo los taparia junto con estos.
   ============================================================================ */
UPDATE CBR.ANTC a
   SET a.ANTCSALD = a.VALOR
 WHERE a.ID IN (3, 4)
   AND a.ANTCSALD <> a.VALOR
   AND NOT EXISTS (SELECT 1 FROM CBR.APLC p WHERE p.APLCANTO = a.ID);

COMMIT;


/* ============================================================================
   BLOQUE 2 · CONTROL DESPUES
   ----------------------------------------------------------------------------
   Esperado:
     - las dos filas con SALDO_ACTUAL = 173,50
     - TOTAL_DEL_TITULAR = 347,00, que es lo que la pantalla debe mostrar en
       "SALDO A FAVOR (ANTICIPOS)" en vez de 520,50
   ============================================================================ */
SELECT a.ID        AS id_anticipo,
       a.NUMERODOC AS numero,
       a.VALOR     AS valor,
       a.ANTCSALD  AS saldo_actual
  FROM CBR.ANTC a
 WHERE a.ID IN (3, 4)
 ORDER BY a.ID;

SELECT ROUND(SUM(NVL(a.ANTCSALD, 0)), 2) AS total_del_titular
  FROM CBR.ANTC a
 WHERE a.TITULAR = 133
   AND a.ESTADO <> 3;


/* ============================================================================
   REVERSO · COMENTADO A PROPOSITO
   ----------------------------------------------------------------------------
   Deja los saldos como estaban antes de este script. Solo tiene sentido si el
   BLOQUE 2 muestra algo inesperado.

   -- UPDATE CBR.ANTC SET ANTCSALD = 347   WHERE ID = 4;
   -- UPDATE CBR.ANTC SET ANTCSALD = 0     WHERE ID = 3;
   -- COMMIT;
   ============================================================================ */
