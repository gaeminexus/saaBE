/* ============================================================================
   lap1-01  DIAGNOSTICO DEL SECUENCIAL DE COMPROBANTES ELECTRONICOS  (D2b / D2c)
   Equipo lap-saa-1 (laptop)  ·  2026-09-01  ·  modulo cxc
   ============================================================================

   QUE ES ESTO
   -----------
   SOLO LECTURA. No hay un solo INSERT, UPDATE ni DDL. Es seguro correrlo de
   corrido en local y en produccion, en cualquier momento.

   PARA QUE SIRVE
   --------------
   Una liquidacion de compra salio con secuencial 000000000 y el SRI la devolvio.
   Hay que decidir cual de dos cosas es la correcta, y la decision es del usuario
   porque cambia que numero sale impreso en un comprobante fiscal:

     (A) EL DATO ESTA MAL  -> la fila de CBR.NXPE para ese punto de emision y
         tipoDoc='03' tiene NUMACTUAL = 0 y deberia tener 1. Se arregla con un
         UPDATE, sin tocar codigo.

     (B) EL CODIGO ESTA MAL -> obtenerSecuencial deberia pre-incrementar.

   LO QUE YA SE VERIFICO EN EL CODIGO, Y ACOTA LA PREGUNTA
   ------------------------------------------------------
   Los SEIS generadores tienen el metodo obtenerSecuencial COPIADO, identico, y
   los seis devuelven el valor PREVIO al incremento:

     LiquidacionCompraServiceImpl:1806   NotaCreditoServiceImpl:1603
     FacturaServiceImpl:1801             NotaDebitoServiceImpl:1719
     RetencionServiceImpl:1396           RetencionV2ServiceImpl:177

       Long numeroActual = numeracion.getNumActual();
       ... UPDATE numActual = numeroActual + 1 ...
       return String.format("%09d", numeroActual);      <-- devuelve el PREVIO

   Consecuencia: la opcion (B) implicaria que TODOS los tipos de comprobante
   estan corridos en uno, no solo la liquidacion. Basta UNA factura autorizada
   por el SRI con el secuencial correcto para descartar (B) por completo.
   Eso es exactamente lo que mide el BLOQUE 3.

   COMO LEERLO
   -----------
   Correr los bloques en orden y comparar contra lo que dice cada encabezado.
   El BLOQUE 3 es el que decide; los demas dan el contexto para interpretarlo.
   ============================================================================ */


/* ============================================================================
   BLOQUE 1  ·  Estado actual de la numeracion (CBR.NXPE)
   ----------------------------------------------------------------------------
   Una fila por punto de emision y tipo de documento. Es la tabla que consume
   obtenerSecuencial.

   TIPODOC segun el catalogo del SRI:
     01 factura · 03 liquidacion de compra · 04 nota de credito
     05 nota de debito · 06 guia de remision · 07 comprobante de retencion

   QUE MIRAR: cualquier fila con NUMACTUAL = 0 va a emitir 000000000 en su
   proximo comprobante. Esa es la fila sospechosa.
   ============================================================================ */
SELECT e.CODIGO        AS establecimiento,
       p.CODIGO        AS pto_emision,
       p.ID            AS id_pto_emision,
       n.TIPODOC       AS tipo_doc,
       n.NUMACTUAL     AS num_actual,
       CASE WHEN n.NUMACTUAL = 0 THEN '<<< EMITIRA 000000000'
            WHEN n.NUMACTUAL IS NULL THEN '<<< NULO'
            ELSE ''
       END             AS alerta
  FROM CBR.NXPE n
  JOIN CBR.PTEM p ON p.ID = n.PTOEMISION
  LEFT JOIN CBR.ESTB e ON e.ID = p.ESTABLECIMIENTO
 ORDER BY e.CODIGO, p.CODIGO, n.TIPODOC;


/* ============================================================================
   BLOQUE 2  ·  Que se emitio de verdad, por tipo de comprobante
   ----------------------------------------------------------------------------
   El maximo y el minimo secuencial realmente grabado en cada tabla de
   documentos, con el conteo. SECUENCIAL es VARCHAR2, asi que se compara como
   numero con TO_NUMBER para que 000000010 no quede antes que 000000009.

   El TO_NUMBER va con DEFAULT NULL ON CONVERSION ERROR a proposito: SECUENCIAL
   es VARCHAR2(1000) y nada impide que una fila vieja traiga texto o vacio. Sin
   esa clausula, UNA fila sucia aborta el bloque entero con ORA-01722 y el
   diagnostico no devuelve nada. Con ella, esa fila cuenta como NULL y el resto
   se mide igual.

   QUE MIRAR: si MIN_SECUENCIAL de un tipo es 000000001, ese tipo arranco bien
   y su fila de NXPE nacio en 1. Si es 000000000, arranco en 0 como la
   liquidacion.
   ============================================================================ */
SELECT 'FACTURA (01)'              AS documento, COUNT(*) AS filas,
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))  AS min_secuencial,
       MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))  AS max_secuencial
  FROM CBR.FCTR WHERE SECUENCIAL IS NOT NULL
UNION ALL
SELECT 'LIQUIDACION COMPRA (03)', COUNT(*),
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.LQCS WHERE SECUENCIAL IS NOT NULL
UNION ALL
SELECT 'NOTA CREDITO (04)', COUNT(*),
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.NTCR WHERE SECUENCIAL IS NOT NULL
UNION ALL
SELECT 'NOTA DEBITO (05)', COUNT(*),
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.NTDB WHERE SECUENCIAL IS NOT NULL
UNION ALL
SELECT 'RETENCION vieja (07)', COUNT(*),
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.RTNC WHERE SECUENCIAL IS NOT NULL
UNION ALL
SELECT 'RETENCION V2 (07)', COUNT(*),
       MIN(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.RTV2 WHERE SECUENCIAL IS NOT NULL;


/* ============================================================================
   BLOQUE 3  ·  EL QUE DECIDE  ·  Comprobantes AUTORIZADOS por el SRI
   ----------------------------------------------------------------------------
   Un comprobante que el SRI AUTORIZO es prueba de que su secuencial estaba
   bien: si estuviera corrido en uno o fuera 000000000, el SRI lo habria
   devuelto. Por eso se filtra por AUTORIZACION, que solo se puebla
   cuando el WS2 responde AUTORIZADO.

   COMO LEER EL RESULTADO
   ----------------------
     Si aparece AL MENOS UNA fila con MIN_SECUENCIAL = 1
         -> el post-incremento del codigo funciona bien cuando NUMACTUAL
            arranca en 1. La opcion (B) queda DESCARTADA.
         -> la respuesta es (A): es el DATO. Corregir con UPDATE la fila de
            NXPE que el BLOQUE 1 marco en 0.

     Si NO aparece NINGUNA fila
         -> no hay ningun comprobante autorizado todavia en esta base, asi que
            este contraste no puede decidir nada. NO ELEGIR POR DESCARTE:
            pedir al usuario un comprobante autorizado real (del portal del
            SRI) y comparar su numero impreso contra la fila de NXPE.

   ⛔ NO tocar la numeracion mientras este bloque devuelva cero filas.
   ============================================================================ */
SELECT 'FACTURA (01)'                   AS documento,
       COUNT(*)                         AS autorizados,
       MIN(TO_NUMBER(f.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))     AS min_secuencial,
       MAX(TO_NUMBER(f.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))     AS max_secuencial
  FROM CBR.FCTR f
 WHERE f.AUTORIZACION IS NOT NULL AND f.SECUENCIAL IS NOT NULL
HAVING COUNT(*) > 0
UNION ALL
SELECT 'LIQUIDACION COMPRA (03)', COUNT(*),
       MIN(TO_NUMBER(l.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(l.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.LQCS l
 WHERE l.AUTORIZACION IS NOT NULL AND l.SECUENCIAL IS NOT NULL
HAVING COUNT(*) > 0
UNION ALL
SELECT 'NOTA CREDITO (04)', COUNT(*),
       MIN(TO_NUMBER(c.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(c.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.NTCR c
 WHERE c.AUTORIZACION IS NOT NULL AND c.SECUENCIAL IS NOT NULL
HAVING COUNT(*) > 0
UNION ALL
SELECT 'NOTA DEBITO (05)', COUNT(*),
       MIN(TO_NUMBER(d.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(d.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.NTDB d
 WHERE d.AUTORIZACION IS NOT NULL AND d.SECUENCIAL IS NOT NULL
HAVING COUNT(*) > 0
UNION ALL
SELECT 'RETENCION V2 (07)', COUNT(*),
       MIN(TO_NUMBER(r.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)), MAX(TO_NUMBER(r.SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
  FROM CBR.RTV2 r
 WHERE r.AUTORIZACION IS NOT NULL AND r.SECUENCIAL IS NOT NULL
HAVING COUNT(*) > 0;


/* ============================================================================
   BLOQUE 4  ·  D2c  ·  Secuenciales quemados por intentos fallidos
   ----------------------------------------------------------------------------
   obtenerSecuencial hace el UPDATE de NUMACTUAL ANTES de saber si el SRI
   acepta, y un DEVUELTA no lanza excepcion: la transaccion commitea igual. Asi
   que cada intento fallido consume un numero que nunca se usa.

   Este bloque lo mide: compara cuanto avanzo el contador contra cuantos
   documentos existen de verdad.

     DIFERENCIA = 0  -> no se quemo ninguno
     DIFERENCIA > 0  -> esa cantidad de numeros se consumio sin comprobante

   OJO AL INTERPRETARLO: una diferencia positiva tambien puede venir de
   documentos borrados a mano o de una numeracion que arranco en un valor alto
   por migracion. Es un indicio, no una prueba.
   ============================================================================ */
SELECT p.CODIGO                                   AS pto_emision,
       n.TIPODOC                                  AS tipo_doc,
       n.NUMACTUAL                                AS num_actual,
       d.emitidos                                 AS documentos_existentes,
       d.max_sec                                  AS max_secuencial_emitido,
       (n.NUMACTUAL - NVL(d.max_sec, 0) - 1)      AS diferencia
  FROM CBR.NXPE n
  JOIN CBR.PTEM p ON p.ID = n.PTOEMISION
  LEFT JOIN (
        SELECT PTOEMISION AS pe, '01' AS td, COUNT(*) AS emitidos,
               MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR)) AS max_sec
          FROM CBR.FCTR WHERE SECUENCIAL IS NOT NULL GROUP BY PTOEMISION
        UNION ALL
        SELECT PTOEMISION, '03', COUNT(*), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
          FROM CBR.LQCS WHERE SECUENCIAL IS NOT NULL GROUP BY PTOEMISION
        UNION ALL
        SELECT PTOEMISION, '04', COUNT(*), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
          FROM CBR.NTCR WHERE SECUENCIAL IS NOT NULL GROUP BY PTOEMISION
        UNION ALL
        SELECT PTOEMISION, '05', COUNT(*), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
          FROM CBR.NTDB WHERE SECUENCIAL IS NOT NULL GROUP BY PTOEMISION
        UNION ALL
        SELECT PTOEMISION, '07', COUNT(*), MAX(TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR))
          FROM CBR.RTV2 WHERE SECUENCIAL IS NOT NULL GROUP BY PTOEMISION
       ) d ON d.pe = n.PTOEMISION AND d.td = n.TIPODOC
 ORDER BY p.CODIGO, n.TIPODOC;


/* ============================================================================
   BLOQUE 5  ·  Comprobantes ya grabados con secuencial invalido
   ----------------------------------------------------------------------------
   Un comprobante electronico con secuencial 000000000 es invalido para el SRI:
   la numeracion arranca en 1. Estos son los que ya estan en la base.

   QUE MIRAR: cuantos son y en que ESTADOEMISION estan. Si alguno tiene
   AUTORIZACION poblado, es un caso raro que hay que mirar aparte antes
   de tocar nada.
   ============================================================================ */
SELECT 'LIQUIDACION COMPRA' AS documento, ID, NUMERO, SECUENCIAL, CLAVE,
       ESTADOEMISION, AUTORIZACION
  FROM CBR.LQCS WHERE TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR) = 0
UNION ALL
SELECT 'FACTURA', ID, NUMERO, SECUENCIAL, CLAVE, ESTADOEMISION, AUTORIZACION
  FROM CBR.FCTR WHERE TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR) = 0
UNION ALL
SELECT 'NOTA CREDITO', ID, NUMERO, SECUENCIAL, CLAVE, ESTADOEMISION, AUTORIZACION
  FROM CBR.NTCR WHERE TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR) = 0
UNION ALL
SELECT 'NOTA DEBITO', ID, NUMERO, SECUENCIAL, CLAVE, ESTADOEMISION, AUTORIZACION
  FROM CBR.NTDB WHERE TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR) = 0
UNION ALL
SELECT 'RETENCION V2', ID, NUMERO, SECUENCIAL, CLAVE, ESTADOEMISION, AUTORIZACION
  FROM CBR.RTV2 WHERE TO_NUMBER(SECUENCIAL DEFAULT NULL ON CONVERSION ERROR) = 0;


/* ============================================================================
   CORRECCION PROPUESTA  ·  COMENTADA A PROPOSITO  ·  NO EJECUTAR TODAVIA
   ----------------------------------------------------------------------------
   Esto es la opcion (A), y SOLO se descomenta si el BLOQUE 3 devolvio al menos
   una fila con MIN_SECUENCIAL = 1 (o sea, si (B) quedo descartada) y el usuario
   confirmo el numero de arranque.

   Reemplazar <ID_PTO_EMISION> y <TIPODOC> por los que haya marcado el BLOQUE 1.
   Ajustar el valor de arranque: 1 si nunca se emitio nada de ese tipo, o
   max_secuencial_emitido + 1 si ya hay comprobantes autorizados.

   -- UPDATE CBR.NXPE
   --    SET NUMACTUAL = 1
   --  WHERE PTOEMISION = <ID_PTO_EMISION>
   --    AND TIPODOC = '<TIPODOC>'
   --    AND NUMACTUAL = 0;
   --
   -- COMMIT;

   CONTROL DESPUES DE EJECUTAR (volver a correr el BLOQUE 1): la columna alerta
   tiene que salir vacia en todas las filas.

   ⛔ El secuencial es numeracion fiscal. Elegir mal el valor de arranque
      reordena comprobantes ya emitidos, que es lo peor que se puede hacer aca.
      Ante la duda, parar y preguntar.
   ============================================================================ */
