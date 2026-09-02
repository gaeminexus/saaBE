/* ============================================================================
   lap1-06  ANTICIPOS DE CLIENTE CON SALDO MAYOR AL VALOR
   Equipo lap-saa-1 (laptop)  ·  2026-09-02  ·  modulo cxc
   ============================================================================

   SOLO LECTURA. Ni un INSERT, ni un UPDATE, ni DDL.

   EL SINTOMA
   ----------
   En Estado de cuenta de titular, el anticipo 7857720 muestra:

       VALOR 173,50   ·   USADO -173,50   ·   DISPONIBLE 347,00

   y por eso el total de "Saldo a favor" sale 520,50 en vez de 347,00.

   LA PANTALLA NO ESTA MAL — verificado en el frontend
   ---------------------------------------------------
   estado-cuenta-titular.service.ts:240-241
       disponible = fila.saldo          (lo que manda el backend)
       usado      = |valor| - saldo

   Con valor 173,50 y saldo 347,00 da usado = -173,50. La aritmetica es
   correcta; el dato de entrada no. **El defecto es que CBR.ANTC.ANTCSALD
   vale 347,00 en un anticipo cuyo VALOR es 173,50** — exactamente el doble.

   QUE HAY QUE AVERIGUAR
   ---------------------
   Que escribio ese saldo. Leyendo el codigo se descartan dos sospechosos:
     - saveSingle solo asigna saldo=valor en el ALTA (esNuevo), no al editar.
     - revertirAplicacion RECHAZA reversar dos veces una aplicacion ya
       REVERSADA, asi que el reverso duplicado por esa via esta bloqueado.

   Queda el reverso de una aplicacion cuyo APLCANTO apunta a este anticipo
   SIN que ese anticipo lo hubiera consumido: AplicacionPagoCxcServiceImpl:1002
   hace saldo = saldo + montoAplicado sin comprobar que el consumo existiera.
   Los cruces anteriores al 2026-08-20 no tenian APLCANTO, asi que si alguno
   lo recibio despues por migracion o backfill, su reverso devuelve un saldo
   que nunca se habia descontado. **Es una hipotesis: estos bloques la
   confirman o la descartan.**
   ============================================================================ */


/* ============================================================================
   BLOQUE 1 · Todos los anticipos de cliente con el saldo inconsistente
   ----------------------------------------------------------------------------
   Tres formas de estar mal, y conviene distinguirlas:
     SALDO > VALOR  -> se devolvio saldo de mas (el caso reportado)
     SALDO < 0      -> se consumio de mas
     SALDO IS NULL  -> nunca se inicializo

   QUE MIRAR: cuantos hay ademas del 7857720. Si es uno solo, es un incidente;
   si son varios, es un proceso.
   ============================================================================ */
SELECT a.ID              AS id_anticipo,
       a.NUMERODOC       AS numero,
       a.FECHAANTICIPO   AS fecha,
       a.VALOR           AS valor,
       a.ANTCSALD        AS saldo,
       ROUND(NVL(a.ANTCSALD,0) - NVL(a.VALOR,0), 2) AS exceso,
       a.ANTCAPLC        AS aplicado,
       a.ESTADO          AS estado,
       CASE WHEN a.ANTCSALD IS NULL          THEN 'SALDO NULO'
            WHEN a.ANTCSALD > a.VALOR        THEN 'SALDO MAYOR AL VALOR'
            WHEN a.ANTCSALD < 0              THEN 'SALDO NEGATIVO'
       END               AS problema
  FROM CBR.ANTC a
 WHERE a.ANTCSALD IS NULL
    OR a.ANTCSALD > a.VALOR
    OR a.ANTCSALD < 0
 ORDER BY ABS(NVL(a.ANTCSALD,0) - NVL(a.VALOR,0)) DESC;


/* ============================================================================
   BLOQUE 2 · La historia completa del anticipo reportado
   ----------------------------------------------------------------------------
   Todas las aplicaciones que dicen haber salido de el (APLCANTO), activas y
   reversadas.

   QUE MIRAR:
     - Si hay UNA aplicacion REVERSADA y ninguna activa, y su monto es 173,50:
       la hipotesis se confirma — se devolvio un saldo que nunca se desconto,
       o se descontó del saldo global y no de este anticipo.
     - Si hay DOS reversadas por el mismo monto: hubo doble devolucion por
       dos aplicaciones distintas apuntando al mismo anticipo.
     - Si no hay ninguna: el saldo se escribio desde otro lado y hay que
       buscar en otra parte.

   APLCESTD: 1 = activo, 2 = reversado.
   ============================================================================ */
SELECT p.APLCCDGO   AS id_aplicacion,
       p.APLCANTO   AS anticipo_origen,
       p.APLCFCTR   AS factura,
       p.APLCTDPG   AS tipo_doc_pago,
       p.APLCMAPL   AS monto_aplicado,
       p.APLCFAPL   AS fecha_aplicacion,
       p.APLCESTD   AS estado_aplicacion
  FROM CBR.APLC p
 WHERE p.APLCANTO IN (SELECT a.ID FROM CBR.ANTC a WHERE a.NUMERODOC = '7857720')
 ORDER BY p.APLCCDGO;


/* ============================================================================
   BLOQUE 3 · El cuadre que deberia dar, anticipo por anticipo
   ----------------------------------------------------------------------------
   Para CADA anticipo con problema: el saldo que tiene contra el saldo que
   deberia tener segun sus aplicaciones ACTIVAS.

       saldo esperado = valor - SUM(monto de aplicaciones ACTIVAS)

   Las reversadas no cuentan, que es justamente el punto: si el saldo real
   supera al esperado, se devolvio de mas.

   QUE MIRAR: la columna DIFERENCIA. Si coincide con el monto de una
   aplicacion reversada del BLOQUE 2, queda demostrado el mecanismo.
   ============================================================================ */
SELECT a.ID                                          AS id_anticipo,
       a.NUMERODOC                                   AS numero,
       a.VALOR                                       AS valor,
       a.ANTCSALD                                    AS saldo_real,
       NVL(ap.aplicado_activo, 0)                    AS aplicado_activo,
       ROUND(a.VALOR - NVL(ap.aplicado_activo, 0), 2) AS saldo_esperado,
       ROUND(NVL(a.ANTCSALD,0) - (a.VALOR - NVL(ap.aplicado_activo, 0)), 2) AS diferencia,
       NVL(ap.cant_activas, 0)                       AS aplicaciones_activas,
       NVL(ap.cant_reversadas, 0)                    AS aplicaciones_reversadas
  FROM CBR.ANTC a
  LEFT JOIN (SELECT APLCANTO,
                    SUM(CASE WHEN APLCESTD = 1 THEN APLCMAPL ELSE 0 END) AS aplicado_activo,
                    SUM(CASE WHEN APLCESTD = 1 THEN 1 ELSE 0 END)        AS cant_activas,
                    SUM(CASE WHEN APLCESTD = 2 THEN 1 ELSE 0 END)        AS cant_reversadas
               FROM CBR.APLC
              WHERE APLCANTO IS NOT NULL
              GROUP BY APLCANTO) ap ON ap.APLCANTO = a.ID
 WHERE a.ANTCSALD IS NULL
    OR a.ANTCSALD > a.VALOR
    OR a.ANTCSALD < 0
    OR ABS(NVL(a.ANTCSALD,0) - (a.VALOR - NVL(ap.aplicado_activo, 0))) >= 0.01
 ORDER BY ABS(NVL(a.ANTCSALD,0) - (a.VALOR - NVL(ap.aplicado_activo, 0))) DESC;


/* ============================================================================
   BLOQUE 4 · Cuanto se desvia el total del titular
   ----------------------------------------------------------------------------
   El numero que ve el usuario en la tarjeta "SALDO A FAVOR (ANTICIPOS)",
   comparado con el que corresponde. Del caso reportado se espera:
   saldo sumado 520,50 contra esperado 347,00.
   ============================================================================ */
SELECT t.TTLRCDGO                                       AS id_titular,
       t.TTLRNMBR                                       AS titular,
       COUNT(*)                                         AS anticipos,
       ROUND(SUM(NVL(a.ANTCSALD,0)), 2)                 AS saldo_que_muestra,
       ROUND(SUM(a.VALOR - NVL(ap.aplicado_activo, 0)), 2) AS saldo_correcto,
       ROUND(SUM(NVL(a.ANTCSALD,0)) - SUM(a.VALOR - NVL(ap.aplicado_activo, 0)), 2) AS desvio
  FROM CBR.ANTC a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.TITULAR
  LEFT JOIN (SELECT APLCANTO,
                    SUM(CASE WHEN APLCESTD = 1 THEN APLCMAPL ELSE 0 END) AS aplicado_activo
               FROM CBR.APLC
              WHERE APLCANTO IS NOT NULL
              GROUP BY APLCANTO) ap ON ap.APLCANTO = a.ID
 WHERE a.ESTADO <> 3
 GROUP BY t.TTLRCDGO, t.TTLRNMBR
HAVING ABS(SUM(NVL(a.ANTCSALD,0)) - SUM(a.VALOR - NVL(ap.aplicado_activo, 0))) >= 0.01
 ORDER BY ABS(SUM(NVL(a.ANTCSALD,0)) - SUM(a.VALOR - NVL(ap.aplicado_activo, 0))) DESC;


/* ============================================================================
   ⛔ NO CORREGIR CON UN UPDATE ANTES DE LEER ESTO
   ----------------------------------------------------------------------------
   Es tentador cerrar esto con
       UPDATE CBR.ANTC SET ANTCSALD = VALOR - <aplicado activo> ...
   y estaria mal hacerlo ahora, por dos razones:

   1. Si el mecanismo sigue vivo en el codigo, el saldo se vuelve a inflar en
      el proximo reverso y el UPDATE solo esconde el sintoma.
   2. El BLOQUE 3 puede mostrar casos donde la diferencia NO se explica por
      una aplicacion reversada. Esos son otro defecto y un UPDATE masivo los
      taparia junto con los demas.

   Primero se lee el resultado, despues se decide.
   ============================================================================ */
