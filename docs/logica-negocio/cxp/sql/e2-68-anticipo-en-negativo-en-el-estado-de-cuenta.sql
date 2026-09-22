-- =====================================================================
-- e2-68 — El anticipo que el Estado de Cuenta muestra en -1.200,00
--          y que la consulta de anticipos da bien en 0,00
-- Modulo: cxp / pagos  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-22
--
-- ⚠️ LOS BLOQUES 0 a 3 SON SOLO LECTURA. El BLOQUE 4 ESCRIBE y esta
--    COMENTADO: no se descomenta hasta leer la salida de los anteriores.
--    Su COMMIT tambien va comentado — sin el, el UPDATE no queda guardado.
--
-- EL CASO (reportado por el usuario, 2026-09-22, con dos capturas):
--   Proveedor RAMIREZ MOLINA LEONARDO DAVID, RUC 0914298443001.
--   · Estado de Cuenta de Titular -> "SALDO A FAVOR (ANTICIPOS): -1.200,00"
--   · Consulta de Anticipos       -> "SALDO DE ANTICIPOS: $0.00"  (correcto)
--   Historial: 23/07/2026 anticipo de 1.200,00 (Confirmado) y 27/07/2026
--   un movimiento de -1.200,00 rotulado "Cruce" contra la factura
--   001-100-000000036.
--
-- DE DONDE SALE EL NUMERO, medido en el codigo el 2026-09-22:
--   El Estado de Cuenta es FRONTEND: arma la pantalla con selectByCriteria
--   de cada entidad. Para los anticipos toma la fila de PGS.ANTP y hace
--       saldoPendiente = fila.saldo          (estado-cuenta-titular.service.ts:291)
--       resumen.saldoAnticipos += saldoPendiente   (component.ts:136)
--   O sea: el -1.200,00 es, literalmente, la suma de ANTPSALD de las filas
--   de ese proveedor que pasan el filtro. NO es un calculo: es el dato.
--
--   Y el filtro excluye los estados 3 (ANULADO) y 4 (MIGRADO):
--       estadosAnulados: [3, 4]              (service.ts:190)
--
--   MIGRADO = 4 es, textual en com.saa.rubros.EstadoAnticipoProveedor:
--     "Movimiento negativo historico de un cruce anterior al 2026-08-20,
--      cuando el cruce se registraba como una fila negativa en la propia
--      tabla de anticipos. Se conserva como historial; las pantallas no lo
--      leen porque el cruce vive ahora en PGS.APLP."
--   Y la migracion (docs/logica-negocio/pagos/MIGRACION-CRUCES-ANTICIPO.md)
--   deja esas filas con ANTPESTD = 4 y ANTPSALD = 0.
--
-- LA HIPOTESIS, que estos bloques confirman o tumban: la fila negativa del
--   27/07 NO quedo normalizada por esa migracion — le falta el ANTPESTD = 4
--   y/o tiene ANTPSALD = -1200 en vez de 0. Por eso el Estado de Cuenta la
--   suma y la consulta de anticipos no.
--
-- ⛔ SI EL BLOQUE 0 MUESTRA OTRA COSA —por ejemplo que la fila negativa SI
--    tiene ANTPESTD = 4, o que el -1200 esta en el ANTPSALD del anticipo
--    POSITIVO— entonces la causa es otra y este script NO se corre:
--    avisale al arbitro con la salida pegada. El BLOQUE 4 solo es valido
--    para el caso que describe la hipotesis.
--
-- Columnas verificadas contra la entidad AnticipoProveedor, no de memoria:
--   PGS.ANTP -> ANTPCDGO (id), ANTPTTLR (titular), ANTPPJRQ (empresa),
--               ANTPFANT (fecha), ANTPNDOC (num documento), ANTPVLOR (valor),
--               ANTPSALD (saldo), ANTPOBSR (observacion), ANTPESTD (estado),
--               ANTPASNT (asiento)
--   PGS.APLP -> APLPANTP (anticipo de origen), APLPTDPG, APLPESTD
--   TSR.TTLR -> TTLRCDGO, TTLRIDNT, TTLRNMBR
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LAS FILAS DEL PROVEEDOR DEL CASO. Solo lectura.
-- COMO LEERLO: se esperan (al menos) dos filas.
--   · la de 23/07 con ANTPVLOR = 1200 y ANTPESTD = 2 (CONFIRMADO).
--     Su ANTPSALD deberia ser 0: el anticipo ya se consumio.
--   · la de 27/07 con ANTPVLOR = -1200, que es el cruce historico.
--     Deberia tener ANTPESTD = 4 y ANTPSALD = 0. Si tiene otro estado
--     o un ANTPSALD distinto de 0, ESA es la causa del -1.200,00.
-- La columna SUMA_QUE_VE_EL_ESTADO_DE_CUENTA dice exactamente con cuanto
-- entra cada fila al total de la pantalla.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - filas del proveedor del caso' AS bloque,
       a.ANTPCDGO AS id,
       TO_CHAR(a.ANTPFANT,'YYYY-MM-DD') AS fecha,
       a.ANTPNDOC AS num_documento,
       a.ANTPVLOR AS valor,
       a.ANTPSALD AS saldo,
       a.ANTPESTD AS estado,
       CASE a.ANTPESTD WHEN 1 THEN 'INGRESADO' WHEN 2 THEN 'CONFIRMADO'
            WHEN 3 THEN 'ANULADO' WHEN 4 THEN 'MIGRADO'
            ELSE 'DESCONOCIDO (' || a.ANTPESTD || ')' END AS que_es,
       CASE WHEN NVL(a.ANTPESTD,-1) IN (3,4) THEN 0 ELSE NVL(a.ANTPSALD,0) END
            AS suma_que_ve_el_estado_de_cuenta,
       a.ANTPASNT AS asiento,
       SUBSTR(a.ANTPOBSR,1,120) AS observacion
  FROM PGS.ANTP a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE REPLACE(t.TTLRIDNT, ' ', '') = '0914298443001'
 ORDER BY a.ANTPFANT, a.ANTPCDGO;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Donde vive el cruce REAL de ese anticipo (PGS.APLP).
-- ESPERADO: una aplicacion de tipo 4 contra la factura 001-100-000000036
--   por 1.200,00. Es la que sostiene que el saldo disponible real es 0 y
--   la que hace que la consulta de anticipos muestre $0.00.
--
-- ⚠️ SE BUSCA POR TRES CAMINOS A PROPOSITO, y esto salvo el bloque: PGS.APLP
--    tiene DOS FK a PGS.ANTP y ninguna esta garantizada en este caso.
--    Medido en la entidad AplicacionPagoCxp el 2026-09-22:
--      · APLPANTP (anticipo) — textual: "Queda nulo en el cruce por valor
--        contra el saldo de anticipos, que es el MECANISMO ESTANDAR".
--      · APLPANTO (anticipoOrigen) — "en las aplicaciones de tipo 4 creadas
--        DESDE 2026-08-20 siempre viene informado... Queda nulo en los
--        cruces anteriores que la migracion no pudo atribuir".
--    El cruce de este caso es del 27/07/2026, o sea ANTERIOR a esa fecha:
--    es perfectamente posible que las DOS columnas esten nulas. Por eso se
--    busca ademas por la FACTURA. Mirar solo APLPANTP habria devuelto CERO
--    filas y llevado a la conclusion falsa de que el cruce no existe.
--
-- COMO LEERLO: si aparece la aplicacion por cualquiera de los tres
--   caminos, el cruce real existe y el BLOQUE 4 es seguro. Si NO aparece
--   por ninguno, PARAR y avisar al arbitro: marcar la fila negativa como
--   MIGRADO sin que exista la APLP dejaria el cruce sin respaldo y el
--   saldo del anticipo quedaria disponible cuando no lo esta.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - el cruce en APLP' AS bloque,
       ap.APLPCDGO AS id_aplicacion,
       ap.APLPANTP AS anticipo,
       ap.APLPANTO AS anticipo_origen,
       ap.APLPFCTC AS factura,
       ap.APLPMAPL AS monto_aplicado,
       ap.APLPFAPL AS fecha_aplicacion,
       ap.APLPESTD AS estado_aplicacion,
       ap.APLPTDPG AS tipo_pago,
       CASE WHEN ap.APLPANTP IS NOT NULL THEN 'por APLPANTP'
            WHEN ap.APLPANTO IS NOT NULL THEN 'por APLPANTO'
            ELSE 'solo por la factura' END AS encontrado
  FROM PGS.APLP ap
 WHERE ap.APLPANTP IN ( SELECT a.ANTPCDGO FROM PGS.ANTP a
                          JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
                         WHERE REPLACE(t.TTLRIDNT, ' ', '') = '0914298443001' )
    OR ap.APLPANTO IN ( SELECT a.ANTPCDGO FROM PGS.ANTP a
                          JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
                         WHERE REPLACE(t.TTLRIDNT, ' ', '') = '0914298443001' )
    OR ap.APLPFCTC IN ( SELECT f.ID FROM PGS.FCTC f
                          JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
                         WHERE REPLACE(t.TTLRIDNT, ' ', '') = '0914298443001' )
 ORDER BY ap.APLPCDGO;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — ⭐ CONTAR LA FAMILIA antes de arreglar el ejemplar.
-- Todas las filas NEGATIVAS de PGS.ANTP que NO estan marcadas como
-- MIGRADO/ANULADO: cada una es un proveedor cuyo Estado de Cuenta esta
-- mostrando un saldo a favor negativo que no existe.
-- ESPERADO: si solo aparece la del caso, es un ejemplar suelto. Si
--   aparecen muchas, la migracion del 2026-08-20 no corrio para CXP y el
--   arreglo es de familia, no de fila — eso lo decide el arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - la familia: negativos sin marcar' AS bloque,
       a.ANTPCDGO AS id,
       TO_CHAR(a.ANTPFANT,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS identificacion, t.TTLRNMBR AS proveedor,
       a.ANTPVLOR AS valor, a.ANTPSALD AS saldo, a.ANTPESTD AS estado
  FROM PGS.ANTP a
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE NVL(a.ANTPVLOR,0) < 0
   AND NVL(a.ANTPESTD,-1) NOT IN (3,4)
 ORDER BY a.ANTPFANT, a.ANTPCDGO;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — El otro sintoma posible, por si el BLOQUE 2 sale vacio:
-- filas POSITIVAS con saldo negativo (un anticipo consumido de mas).
-- ESPERADO: CERO filas. Cada una tambien restaria en el Estado de Cuenta.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - positivos con saldo negativo' AS bloque,
       a.ANTPCDGO AS id, TO_CHAR(a.ANTPFANT,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS identificacion, t.TTLRNMBR AS proveedor,
       a.ANTPVLOR AS valor, a.ANTPSALD AS saldo, a.ANTPESTD AS estado
  FROM PGS.ANTP a
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE NVL(a.ANTPVLOR,0) > 0
   AND NVL(a.ANTPSALD,0) < 0
 ORDER BY a.ANTPCDGO;


-- =====================================================================
-- ⭐ SALIDA REAL DE LOS BLOQUES 0 a 3 — corrida por el usuario el 2026-09-22
--
--   BLOQUE 0, dos filas:
--     id 2 · 23/07 · "COMPROBANTE 202026070055" · valor  1200 · saldo  1200
--            estado 2 (CONFIRMADO) · asiento 7590
--     id 4 · 27/07 · "Factura N° 001-100-000000036" · valor -1200 · saldo -2400
--            estado 2 (CONFIRMADO) · asiento 8030 · obs "Cruce con factura..."
--     Suma que ve el Estado de Cuenta: 1200 + (-2400) = -1200. Exacto.
--
--   BLOQUE 1: la aplicacion del cruce EXISTE — id 96, tipo 4, estado 1
--     (activa), 1.200,00 contra la factura 167, fecha 27/07, y con
--     APLPANTP = 4: apunta al MOVIMIENTO NEGATIVO, no al anticipo real.
--     APLPANTO vacio, que es lo que el javadoc de la entidad describe como
--     "los cruces anteriores que la migracion no pudo atribuir".
--     (Las otras filas del bloque son pagos y retenciones normales de otras
--     facturas del proveedor, tipos 1 y 3: ninguna es cruce de anticipo.)
--
--   BLOQUE 2: UNA sola fila, la id 4. Es un ejemplar suelto, no una familia.
--   BLOQUE 3: vacio.
--
-- ⛔ LO QUE ESTO CORRIGIO DE LA HIPOTESIS: no es una fila la que esta mal,
--    son LAS DOS.
--      · La id 4 es el movimiento negativo historico: le falta el
--        ANTPESTD = 4 y su ANTPSALD tiene -2400, que es el "saldo global
--        acumulado" del esquema viejo (MIGRACION-CRUCES-ANTICIPO.md lo
--        documenta como el significado ANTERIOR de esa columna), no un
--        saldo disponible.
--      · La id 2 es el anticipo REAL y quedo con ANTPSALD = 1200, o sea el
--        sistema cree que sigue disponible entero — y no lo esta: la
--        aplicacion 96 lo consumio completo.
--    Corregir solo la negativa dejaria el Estado de Cuenta en +1.200,00,
--    que tambien es falso. Por eso el BLOQUE 4 toca las dos.
-- =====================================================================


-- =====================================================================
-- BLOQUE 4 — LA CORRECCION. ⛔ COMENTADA. Leer los bloques 0 a 3 primero.
--
-- QUE HACE: deja las dos filas como las habria dejado la migracion del
-- 2026-08-20: el movimiento negativo marcado MIGRADO (4) con saldo 0, y el
-- anticipo real con su saldo disponible REAL, que es 0 porque la
-- aplicacion 96 lo consumio entero.
--
-- POR QUE NO AFECTA CONTABILIDAD NI SALDOS, que es lo que el usuario pidio:
--   · NO toca ningun asiento: no se escribe ANTPASNT ni se modifica CNT.
--     El asiento del anticipo y el del cruce quedan exactamente como estan.
--   · NO toca el saldo real disponible: ese sale de PGS.APLP (BLOQUE 1) y
--     ya da 0 — por eso la consulta de anticipos muestra $0.00 y esta bien.
--   · NO borra la fila: se conserva como historial, que es para lo que
--     existe el estado MIGRADO.
--   · Lo unico que cambia es la CLASIFICACION del movimiento historico,
--     que es lo que el Estado de Cuenta mira para decidir si lo suma.
--
-- POR QUE EL SALDO DE LA id 2 VA A 0 Y NO SE INVENTA NADA: el anticipo es
--   de 1.200,00 y la aplicacion 96 (tipo 4, ACTIVA, del 27/07) aplico
--   1.200,00 contra la factura 167. Disponible = 1200 - 1200 = 0. Es el
--   mismo numero que la pantalla de Consulta de Anticipos ya muestra
--   ("SALDO DE ANTICIPOS $0.00") y que el usuario confirmo como correcto.
--
-- ⚠️ Cada UPDATE lleva su guarda de signo: el de la fila negativa exige
--    ANTPVLOR < 0 y el de la positiva exige ANTPVLOR > 0. Si los ids se
--    pegaran cambiados, ninguno de los dos toca nada.
--
-- -- 4.1 — el movimiento negativo historico pasa a MIGRADO, saldo 0
-- UPDATE PGS.ANTP
--    SET ANTPESTD = 4,          -- MIGRADO: movimiento negativo historico
--        ANTPSALD = 0
--  WHERE ANTPCDGO = 4
--    AND NVL(ANTPVLOR,0) < 0;   -- guarda: nunca una fila positiva
--
-- -- 4.2 — el anticipo REAL conserva su estado CONFIRMADO y queda con su
-- --       saldo disponible real. NO se toca ANTPESTD: sigue siendo un
-- --       anticipo confirmado, solo que ya consumido.
-- UPDATE PGS.ANTP
--    SET ANTPSALD = 0
--  WHERE ANTPCDGO = 2
--    AND NVL(ANTPVLOR,0) > 0;   -- guarda: nunca la fila del cruce
--
-- -- Control posterior, ANTES del COMMIT: volver a correr el BLOQUE 0.
-- -- ESPERADO:
-- --   id 2 -> saldo 0, estado 2 (CONFIRMADO), asiento 7590 intacto
-- --   id 4 -> saldo 0, estado 4 (MIGRADO),    asiento 8030 intacto
-- --   y la columna SUMA_QUE_VE_EL_ESTADO_DE_CUENTA en 0 en las dos filas.
-- --   Esa suma es, literalmente, lo que va a mostrar la pantalla.
--
-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA.
-- COMMIT;
--
-- DESPUES DEL COMMIT, mirar las DOS pantallas:
--   · Estado de Cuenta de Titular -> "Saldo a favor (anticipos)" debe decir 0,00
--   · Consulta de Anticipos       -> debe SEGUIR diciendo $0.00, y el historial
--     debe seguir mostrando las dos filas (la 4 se conserva como historial;
--     MIGRADO no la borra).
--   Si la Consulta de Anticipos cambiara, avisar al arbitro: significa que
--   esa pantalla lee ANTPSALD de una forma que no se midio.
--
-- REVERSO (comentado): devuelve las dos filas EXACTAMENTE a como estaban,
-- con los valores que imprimio el BLOQUE 0 el 2026-09-22.
--   UPDATE PGS.ANTP SET ANTPESTD = 2, ANTPSALD = -2400 WHERE ANTPCDGO = 4;
--   UPDATE PGS.ANTP SET ANTPESTD = 2, ANTPSALD =  1200 WHERE ANTPCDGO = 2;
--   COMMIT;
-- =====================================================================


-- =====================================================================
-- APARTE — una mejora de trazabilidad que este script NO hace, y es
-- decision del usuario.
--
-- La aplicacion 96 tiene APLPANTP = 4 (apunta al movimiento negativo) y
-- APLPANTO vacio. El esquema nuevo espera lo contrario: APLPANTO con el
-- anticipo REAL (la id 2), que segun el javadoc de AplicacionPagoCxp "es
-- lo que permite saber con exactitud que abonos hay que deshacer al anular
-- un anticipo".
--
-- O sea: si algun dia se anulara el anticipo 2, el sistema no sabria que
-- este cruce salio de el. Se arregla con
--     UPDATE PGS.APLP SET APLPANTO = 2 WHERE APLPCDGO = 96;
-- pero eso TOCA UNA APLICACION DE PAGO, que es otra cosa que tocar una
-- clasificacion de historial, y no hace falta para el sintoma que el
-- usuario reporto. Queda planteado, sin correr.
-- =====================================================================
