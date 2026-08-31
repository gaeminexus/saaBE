-- =====================================================================
-- DIAGNOSTICO: "Saldo a favor (anticipos)" muestra de mas
-- Modulo: CXC  ·  Equipo: omen-saa-3  ·  Fecha: 2026-08-31
--
-- CASO REPORTADO
--   Cliente: ANIBAL EDUARDO MALDONADO MARTINEZ
--   La tarjeta muestra 520,50 y se esperaba 347,00  ->  sobran 173,50
--
-- SOLO LECTURA. No modifica nada. Correr los cinco bloques y traer la
-- salida; con eso se sabe cual de las tres causas posibles es.
--
-- QUE SE VERIFICO POR CODIGO Y NO EXPLICA EL SINTOMA
--   La tarjeta suma ANTC.ANTCSALD de los anticipos NO anulados
--   (estado-cuenta-titular: pasaFiltros descarta los anulados ANTES de
--   sumar, y el backend al anular pone ESTADO=3 y ANTCSALD=0). O sea que
--   un anticipo anulado por la via normal aportaria 0 aunque se sumara.
--   Por eso el problema tiene que estar en los DATOS, no en el filtro:
--     (a) hay anticipos con ESTADO distinto de 3 que deberian estar
--         anulados -- el frontend solo trata el 3 como anulado;
--     (b) hay anticipos anulados con ANTCSALD != 0, anulados por una via
--         que no puso el saldo en cero (script, migracion, codigo viejo);
--     (c) el ANTCSALD de un anticipo vivo no refleja sus cruces.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1: el titular. Anotar el TTLRCDGO que salga; los demas bloques
--           lo usan.
-- ---------------------------------------------------------------------
SELECT t.TTLRCDGO, t.TTLRNMBR AS NOMBRE, t.TTLRIDNT AS IDENTIFICACION, t.TTLRESTD AS ESTADO
  FROM TSR.TTLR t
 WHERE UPPER(t.TTLRNMBR) LIKE '%MALDONADO%'
   AND UPPER(t.TTLRNMBR) LIKE '%ANIBAL%';
-- Si devuelve mas de una fila, elegir la correcta por identificacion.


-- ---------------------------------------------------------------------
-- BLOQUE 2: TODOS los anticipos del cliente, con estado y saldo.
--           Es el bloque que responde la pregunta.
--   ESTADO: 1 INGRESADO · 2 CONFIRMADO · 3 ANULADO · 4 MIGRADO
-- ---------------------------------------------------------------------
SELECT a.ID,
       a.NUMERODOC,
       a.FECHAANTICIPO,
       a.ESTADO,
       CASE a.ESTADO
            WHEN 1 THEN 'INGRESADO'
            WHEN 2 THEN 'CONFIRMADO'
            WHEN 3 THEN 'ANULADO'
            WHEN 4 THEN 'MIGRADO'
            ELSE 'DESCONOCIDO (' || a.ESTADO || ')'
       END AS ESTADO_TXT,
       a.VALOR,
       a.ANTCSALD AS SALDO,
       CASE WHEN a.ESTADO IN (1,2,4) THEN a.ANTCSALD ELSE 0 END AS SUMA_EN_LA_TARJETA,
       a.OBSERVACION
  FROM CBR.ANTC a
 WHERE a.TITULAR = &&TTLRCDGO
 ORDER BY a.FECHAANTICIPO, a.ID;

-- COMO LEER ESTA SALIDA
--   · Sumar la columna SUMA_EN_LA_TARJETA: tiene que dar 520,50 (lo que
--     se ve hoy). Si da eso, la tarjeta esta sumando lo que hay en la
--     base y el problema son los datos, no el frontend.
--   · Buscar una fila que explique los 173,50 que sobran.
--   · Si esa fila tiene ESTADO = 3 y SALDO != 0  -> causa (b): se anulo
--     sin poner el saldo en cero. OJO: con ESTADO=3 el frontend YA la
--     descarta, asi que no seria esta.
--   · Si tiene ESTADO 1, 2 o 4 pero deberia estar anulada -> causa (a):
--     el frontend solo trata el 3 como anulado. Es la hipotesis mas
--     probable: un anticipo anulado a mano o por migracion que quedo en
--     otro estado.


-- ---------------------------------------------------------------------
-- BLOQUE 3: el total que la tarjeta esta mostrando, calculado igual que
--           el frontend (suma ANTCSALD de los NO anulados).
-- ---------------------------------------------------------------------
SELECT SUM(a.ANTCSALD) AS TOTAL_QUE_MUESTRA_LA_TARJETA,
       COUNT(*)        AS CUANTOS_ANTICIPOS
  FROM CBR.ANTC a
 WHERE a.TITULAR = &&TTLRCDGO
   AND a.ESTADO <> 3;
-- Debe dar 520,50. Si da otra cosa, el desfase no esta donde se cree y
-- hay que volver a mirar: avisar al arbitro con esta salida.


-- ---------------------------------------------------------------------
-- BLOQUE 4: contraste saldo declarado vs. saldo real segun los cruces.
--           Detecta la causa (c): un anticipo vivo cuyo ANTCSALD no
--           refleja lo que ya se cruzo contra facturas.
-- ---------------------------------------------------------------------
SELECT a.ID,
       a.NUMERODOC,
       a.ESTADO,
       a.VALOR,
       a.ANTCSALD                                   AS SALDO_DECLARADO,
       NVL(cr.TOTAL_CRUZADO, 0)                     AS TOTAL_CRUZADO,
       a.VALOR - NVL(cr.TOTAL_CRUZADO, 0)           AS SALDO_ESPERADO,
       a.ANTCSALD - (a.VALOR - NVL(cr.TOTAL_CRUZADO, 0)) AS DIFERENCIA
  FROM CBR.ANTC a
  LEFT JOIN (SELECT p.APLCANTO AS ID_ANTICIPO, SUM(p.APLCMAPL) AS TOTAL_CRUZADO
               FROM CBR.APLC p
              WHERE p.APLCTDPG = 4          -- 4 = cruce por anticipo
                AND p.APLCESTD = 1          -- solo aplicaciones vigentes
                AND p.APLCANTO IS NOT NULL
              GROUP BY p.APLCANTO) cr
    ON cr.ID_ANTICIPO = a.ID
 WHERE a.TITULAR = &&TTLRCDGO
 ORDER BY a.ID;
-- DIFERENCIA distinta de 0 = ese anticipo tiene el saldo desfasado.
-- OJO: los cruces anteriores al 2026-08-20 pueden tener APLCANTO nulo
-- (la migracion no pudo atribuirlos), y ahi TOTAL_CRUZADO sale corto sin
-- que eso sea un error. Contrastar con el bloque 5 antes de concluir.


-- ---------------------------------------------------------------------
-- BLOQUE 5: los cruces de anticipo del cliente, uno por uno.
--           Sirve para descartar el falso positivo del bloque 4.
-- ---------------------------------------------------------------------
SELECT p.APLCCDGO   AS ID_APLICACION,
       p.APLCANTO   AS ANTICIPO_ORIGEN,
       p.APLCANTC   AS ANTICIPO_MOVIMIENTO,
       p.APLCMAPL   AS MONTO_APLICADO,
       p.APLCFAPL   AS FECHA_APLICACION,
       p.APLCESTD   AS ESTADO_APLICACION,
       p.APLCOBSR   AS OBSERVACION
  FROM CBR.APLC p
 WHERE p.APLCTDPG = 4
   AND (p.APLCANTO IN (SELECT ID FROM CBR.ANTC WHERE TITULAR = &&TTLRCDGO)
     OR p.APLCANTC IN (SELECT ID FROM CBR.ANTC WHERE TITULAR = &&TTLRCDGO))
 ORDER BY p.APLCFAPL, p.APLCCDGO;
-- APLCESTD = 1 vigente, 0 reversada. Un cruce reversado NO debe estar
-- descontando saldo.


-- =====================================================================
-- QUE HACER CON EL RESULTADO
--   Traer la salida de los cinco bloques al arbitro. Segun cual sea la
--   causa el arreglo es distinto, y NO conviene adivinarlo:
--     (a) estados mal  -> se corrigen los datos con un UPDATE acotado, y
--         se evalua si el frontend debe tratar mas estados como anulado.
--     (b) saldo != 0 en anulados -> UPDATE de esos saldos a 0.
--     (c) saldo desfasado en vivos -> recalculo desde los cruces.
--   Los tres son UPDATE distintos sobre filas distintas. Escribirlos
--   antes de saber cual es seria arreglar a ciegas una tabla de dinero.
-- =====================================================================
