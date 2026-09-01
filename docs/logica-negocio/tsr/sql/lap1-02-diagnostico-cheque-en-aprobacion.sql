/* ============================================================================
   lap1-02  POR QUE NO APARECE LA OPCION "CHEQUE" EN APROBACION DE PAGOS
   Equipo lap-saa-1 (laptop)  ·  2026-09-01  ·  modulo tsr
   ============================================================================

   QUE ES ESTO
   -----------
   SOLO LECTURA. Ni un INSERT, ni un UPDATE, ni DDL. Seguro de correr de corrido
   en local y en produccion.

   EL SINTOMA
   ----------
   En Tesoreria -> Procesos -> Aprobacion de pagos solo aparecen "Transferencia"
   y "Debito automatico". La opcion "Cheque" no se muestra.

   ESTO NO ES UN DEFECTO, ES UNA CONDICION DE DATOS
   ------------------------------------------------
   La pantalla oculta la opcion a proposito cuando la cuenta bancaria de origen
   elegida no maneja chequera:

     aprobacion-pagos.component.html:176   @if (cuentaManejaChequera) { ...Cheque... }
     aprobacion-pagos.component.ts:109     Number(cuentaSeleccionada()?.manejaChequera) === 1
     aprobacion-pagos.component.ts:212-213 si estaba en CHEQUE y la cuenta no maneja,
                                           vuelve solo a TRANSFERENCIA

   Y el backend valida lo mismo, con un mensaje que dice donde arreglarlo:

     PagoProgramadoServiceImpl:1936-1942
       "La cuenta bancaria '...' no maneja chequeras. Activela en
        Tesoreria -> Cuentas bancarias para pagar con cheque."

   O sea: la opcion depende de la CUENTA DE ORIGEN que se elige arriba, no del
   pago. Cambiando de cuenta a una que si maneje chequera, la opcion aparece.

   DOS CONDICIONES, NO UNA
   -----------------------
   1. La cuenta tiene que tener CNBCCHQR = 1        -> lo mide el BLOQUE 1
   2. Tiene que haber cheques ACTIVO disponibles     -> lo mide el BLOQUE 2

   La 1 es la que oculta el radio button. La 2 no lo oculta: deja elegir cheque
   y despues falla al aprobar, porque no hay numero que asignar. Por eso se
   miden las dos.
   ============================================================================ */


/* ============================================================================
   BLOQUE 1  ·  Que cuentas manejan chequera
   ----------------------------------------------------------------------------
   QUE MIRAR: la cuenta que estas eligiendo en la pantalla de aprobacion tiene
   que salir con MANEJA_CHEQUERA = 'SI'. Si sale 'NO' o 'NULO', ese es el motivo
   por el que no ves la opcion.

   El estado de la cuenta se muestra al lado porque una cuenta inactiva puede no
   aparecer en el combo, que es un motivo distinto para "no la veo".
   ============================================================================ */
SELECT c.CNBCCDGO                 AS id_cuenta,
       b.BNCONMBR                 AS banco,
       c.CNBCNMRO                 AS numero_cuenta,
       c.CNBCCHQR                 AS cnbcchqr,
       CASE WHEN c.CNBCCHQR = 1 THEN 'SI'
            WHEN c.CNBCCHQR IS NULL THEN 'NULO'
            ELSE 'NO'
       END                        AS maneja_chequera,
       c.CNBCESTD                 AS estado_cuenta
  FROM TSR.CNBC c
  LEFT JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
 ORDER BY maneja_chequera DESC, b.BNCONMBR, c.CNBCNMRO;


/* ============================================================================
   BLOQUE 2  ·  Cheques disponibles por cuenta
   ----------------------------------------------------------------------------
   Replica exactamente la consulta que usa el sistema para tomar el siguiente
   cheque (ChequeDaoServiceImpl.selectMinChequeActivoPorCuenta:117-124):

     - cheque en estado ACTIVO (rubro 26, detalle 1) -> DTCHRZZA = 1
     - la chequera NO puede estar ANULADA(6) ni TERMINADA(4)
     - una chequera con estado NULL o SOLICITADA(3) SI cuenta: es legado o un
       estado intermedio, y esconder sus cheques seria incorrecto

   QUE MIRAR: la cuenta que vas a usar tiene que traer DISPONIBLES > 0. Si trae
   0 con MANEJA_CHEQUERA = 'SI', la opcion Cheque te va a aparecer en pantalla y
   va a fallar al aprobar — hay que cargar una chequera primero.
   ============================================================================ */
SELECT c.CNBCCDGO                        AS id_cuenta,
       b.BNCONMBR                        AS banco,
       c.CNBCNMRO                        AS numero_cuenta,
       COUNT(d.DTCHCDGO)                 AS disponibles,
       MIN(d.DTCHNMRO)                   AS proximo_numero
  FROM TSR.CNBC c
  LEFT JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
  LEFT JOIN TSR.CHQR q ON q.CNBCCDGO = c.CNBCCDGO
                      AND (q.CHQRRZZA IS NULL OR q.CHQRRZZA NOT IN (4, 6))
  LEFT JOIN TSR.DTCH d ON d.CHQRCDGO = q.CHQRCDGO
                      AND d.DTCHRZZA = 1
 WHERE c.CNBCCHQR = 1
 GROUP BY c.CNBCCDGO, b.BNCONMBR, c.CNBCNMRO
 ORDER BY disponibles DESC, b.BNCONMBR;


/* ============================================================================
   BLOQUE 3  ·  Detalle de las chequeras cargadas
   ----------------------------------------------------------------------------
   Solo hace falta si el BLOQUE 2 dio 0 disponibles y queres ver por que.
   Muestra cada chequera con el reparto de estados de sus cheques.

   Estados de cheque (rubro 26):
     1 ACTIVO (disponible) · 2 ANULADO · 3 GENERADO · 4 IMPRESO
     5 DANIADO · 6 ENTREGADO
   Estados de chequera: 3 SOLICITADA · 4 TERMINADA · 6 ANULADA
   ============================================================================ */
SELECT q.CHQRCDGO                                                AS id_chequera,
       c.CNBCNMRO                                                AS numero_cuenta,
       q.CHQRNMRO                                                AS numero_chequera,
       q.CHQRCMNZ                                                AS desde,
       q.CHQRFNLZ                                                AS hasta,
       q.CHQRRZZA                                                AS estado_chequera,
       COUNT(d.DTCHCDGO)                                         AS total_cheques,
       SUM(CASE WHEN d.DTCHRZZA = 1 THEN 1 ELSE 0 END)           AS activos,
       SUM(CASE WHEN d.DTCHRZZA = 3 THEN 1 ELSE 0 END)           AS generados,
       SUM(CASE WHEN d.DTCHRZZA = 4 THEN 1 ELSE 0 END)           AS impresos,
       SUM(CASE WHEN d.DTCHRZZA = 6 THEN 1 ELSE 0 END)           AS entregados,
       SUM(CASE WHEN d.DTCHRZZA = 2 THEN 1 ELSE 0 END)           AS anulados
  FROM TSR.CHQR q
  JOIN TSR.CNBC c ON c.CNBCCDGO = q.CNBCCDGO
  LEFT JOIN TSR.DTCH d ON d.CHQRCDGO = q.CHQRCDGO
 GROUP BY q.CHQRCDGO, c.CNBCNMRO, q.CHQRNMRO, q.CHQRCMNZ, q.CHQRFNLZ, q.CHQRRZZA
 ORDER BY c.CNBCNMRO, q.CHQRNMRO;


/* ============================================================================
   COMO SE ARREGLA  ·  NO HACE FALTA SQL
   ----------------------------------------------------------------------------
   Si el BLOQUE 1 muestra la cuenta con MANEJA_CHEQUERA distinto de 'SI', se
   activa DESDE LA PANTALLA:

       Tesoreria -> Cuentas bancarias -> editar la cuenta -> marcar que maneja
       chequera

   Es lo que dice el propio mensaje del backend, y es el camino correcto: la
   pantalla graba el campo y deja el rastro de auditoria. Un UPDATE a mano
   haria lo mismo sin ese rastro, asi que se deja anotado y comentado solo por
   si la pantalla no estuviera disponible:

   -- UPDATE TSR.CNBC SET CNBCCHQR = 1 WHERE CNBCCDGO = <id_cuenta>;
   -- COMMIT;

   Si ademas el BLOQUE 2 da 0 disponibles, activar el campo NO alcanza: hay que
   cargar una chequera con sus cheques desde Tesoreria -> Chequeras. Sin eso la
   opcion aparece y la aprobacion falla al buscar numero libre.
   ============================================================================ */
