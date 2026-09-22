-- =====================================================================
-- e2-69 — La factura de prueba que sigue ofreciendose en Solicitud de Pago
--          001-501-000000203 · CUSTODIA & MAXIMA SEGURIDAD · 12.295,80
-- Modulo: cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-22
--
-- ⚠️ LOS BLOQUES 0 a 2 SON SOLO LECTURA. El BLOQUE 3 ESCRIBE y esta
--    COMENTADO. Su COMMIT tambien — sin el, el UPDATE no queda guardado.
--
-- EL CASO (usuario, 2026-09-22, con captura): en Cuentas por Pagar ->
--   Pagos -> Solicitud de pago, el combo "Documento a pagar" del proveedor
--   CUSTODIA & MAXIMA SEGURIDAD ofrece la factura 001-501-000000203 del
--   01/06/2026 por 12.295,80. Es una factura de PRUEBA. El usuario quiere
--   que deje de ofrecerse y que quede con estado PAGADA (hoy esta en
--   PAGADA PARCIAL).
--
-- ⛔ OJO CON LA FECHA: el usuario la recordo como "junio de 2025" y la
--    pantalla muestra 01/06/2026. El script trabaja por NUMERO y por
--    PROVEEDOR, nunca por fecha, asi que la discrepancia no lo afecta —
--    pero el BLOQUE 0 imprime la fecha real para que se confirme que es
--    esa y no otra.
--
-- POR QUE APARECE, medido en el codigo el 2026-09-22:
--   El combo lo arma el FRONTEND (solicitud-pago.component.ts:316-323) y
--   descarta con tres filtros:
--       .filter(d => Number(d.estado) === 1)                    // activa
--       .filter(d => d.estadoPago !== EstadoPagoFactura.PAGADA) // no pagada
--       .filter(d => !comprometidas.has(d.id))                  // sin pago vivo
--   El segundo es el que importa: **filtra por el ESTADO DE PAGO, no por el
--   saldo**. Asi que poner el estado en PAGADA la saca del combo. Los
--   valores, medidos en com.saa.rubros.EstadoPagoFactura y en el enum del
--   frontend (catalogos-aplicacion-pago.ts:59-63), coinciden:
--       1 = PENDIENTE · 2 = PAGADA_PARCIAL · 3 = PAGADA_TOTAL/PAGADA
--
-- ⚠️ LO QUE ESTO NO HACE, y hay que saberlo antes de correrlo:
--   Marcar la factura como PAGADA **no la paga**. No crea ninguna
--   aplicacion, no genera asiento y NO cambia el saldo: el saldo sale de
--   PGS.APLP. Entonces, si a esta factura le queda saldo real:
--     · desaparece del combo de Solicitud de Pago  ✅ (lo que se pide)
--     · pero SIGUE sumando saldo en el Estado de Cuenta del proveedor y en
--       los reportes de cuentas por pagar, ahora rotulada "Pagada", que se
--       lee peor que "Pagada parcial".
--   El BLOQUE 1 dice exactamente cuanto saldo es. Si da 0,00 no hay ningun
--   efecto lateral y esta opcion es la correcta. Si da distinto de 0,
--   LEER LA NOTA DE LA OPCION B antes de decidir.
--
-- ⚠️ Y una advertencia del propio codigo: el javadoc de FacturaCompra.estadoPago
--   dice que "se recalcula automaticamente al insertar o reversar una
--   AplicacionPagoCxp". O sea que este valor puesto a mano se mantiene
--   mientras nadie toque una aplicacion de pago de esta factura. Para una
--   factura de prueba que ya no se usa, eso no pasa nunca.
--
-- Columnas verificadas contra la entidad FacturaCompra, no de memoria:
--   PGS.FCTC -> ID, NUMERO, FECHA, TOTAL, SUBTOTAL, VIVA, ESTADO,
--               FCTCEPAG (estadoPago), TITULAR, ASIENTO
--   PGS.APLP -> APLPCDGO, APLPFCTC, APLPMAPL, APLPFAPL, APLPESTD, APLPTDPG
--   TSR.TTLR -> TTLRCDGO, TTLRIDNT, TTLRNMBR
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LA FACTURA. Solo lectura. ANOTAR ESTA SALIDA: es el respaldo
--            del estado anterior, y de aqui sale el ID para el BLOQUE 3.
-- ESPERADO: UNA fila, con ESTADO_PAGO = 2 (PAGADA_PARCIAL) y ESTADO = 1.
--   Si devuelve mas de una, PARAR: hay numeros repetidos y hay que elegir
--   por ID, no por numero.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - la factura' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS ruc_proveedor, t.TTLRNMBR AS proveedor,
       NVL(f.TOTAL,0) AS total, NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.VIVA,0) AS iva,
       f.ESTADO   AS estado_documento,
       f.FCTCEPAG AS estado_pago,
       CASE f.FCTCEPAG WHEN 1 THEN 'PENDIENTE' WHEN 2 THEN 'PAGADA PARCIAL'
            WHEN 3 THEN 'PAGADA' ELSE 'SIN ESTADO' END AS que_dice_hoy,
       f.ASIENTO AS asiento
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.NUMERO = '001-501-000000203'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — ⭐ EL NUMERO QUE DECIDE: cuanto se pago de verdad y cuanto
--            queda. Solo lectura.
-- ESPERADO / COMO LEERLO:
--   · SALDO_REAL = 0  -> marcarla como PAGADA es exacto. Opcion A, sin
--     ningun efecto lateral. Adelante con el BLOQUE 3.
--   · SALDO_REAL > 0  -> marcarla como PAGADA la saca del combo pero deja
--     ese saldo colgado en el Estado de Cuenta del proveedor, rotulado
--     "Pagada". Leer la OPCION B del final antes de correr el BLOQUE 3.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - cuanto se pago y cuanto queda' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.TOTAL,0) AS total_factura,
       NVL(( SELECT SUM(NVL(ap.APLPMAPL,0)) FROM PGS.APLP ap
              WHERE ap.APLPFCTC = f.ID AND NVL(ap.APLPESTD,1) = 1 ),0) AS aplicado,
       NVL(f.TOTAL,0)
         - NVL(( SELECT SUM(NVL(ap.APLPMAPL,0)) FROM PGS.APLP ap
                  WHERE ap.APLPFCTC = f.ID AND NVL(ap.APLPESTD,1) = 1 ),0) AS saldo_real
  FROM PGS.FCTC f
 WHERE f.NUMERO = '001-501-000000203';


-- BLOQUE 1b — Las aplicaciones, una por una, para ver de que son esos abonos.
SELECT 'BLOQUE 1b - aplicaciones de la factura' AS bloque,
       ap.APLPCDGO AS id_aplicacion,
       TO_CHAR(ap.APLPFAPL,'YYYY-MM-DD') AS fecha_aplicacion,
       ap.APLPMAPL AS monto_aplicado,
       ap.APLPTDPG AS tipo_pago,
       ap.APLPESTD AS estado_aplicacion
  FROM PGS.APLP ap
 WHERE ap.APLPFCTC IN ( SELECT f.ID FROM PGS.FCTC f WHERE f.NUMERO = '001-501-000000203' )
 ORDER BY ap.APLPCDGO;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — ¿Hay un pago vivo que la tenga comprometida? Solo lectura.
-- ESPERADO: CERO filas. Si aparece un pago en estado no anulado, ESE es el
--   que hay que resolver primero — y de paso explica el tercer filtro del
--   combo (`comprometidas`).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - pagos vivos sobre la factura' AS bloque,
       p.PGTRCDGO AS id_pago, p.PGTRVLOR AS valor, p.PGTRESTD AS estado_pago_programado
  FROM PGS.PGTR p
 WHERE p.PGTRFCTC IN ( SELECT f.ID FROM PGS.FCTC f WHERE f.NUMERO = '001-501-000000203' )
 ORDER BY p.PGTRCDGO;


-- =====================================================================
-- ⭐ SALIDA REAL DE LOS BLOQUES 0 a 2 — corrida por el usuario el 2026-09-22
--
--   BLOQUE 0: factura id 243 · 001-501-000000203 · 2026-06-01 ·
--     CUSTODIA & MAXIMA SEGURIDAD CUSTOMAX CIA LTDA (1792578426001) ·
--     total 12.295,80 (subtotal 10.692,00 + IVA 1.603,80) ·
--     ESTADO 1 (activa) · FCTCEPAG 2 (PAGADA PARCIAL) · asiento 7752
--   BLOQUE 1: aplicado 320,76 · SALDO REAL 11.975,04
--   BLOQUE 1b: UNA aplicacion — id 35, 07/07/2026, 320,76, tipo 3
--     (RETENCION emitida al proveedor: es el 3% de 10.692), estado 1 activa.
--     La retencion es la V2 N° 001-001-000007275, asiento CXP-2026-07-0097.
--   BLOQUE 2: 🔴 pago id 477 por 11.975,04 en estado 0 = **POR_APROBAR**.
--     NO es un pago anulado: es un pago VIVO esperando en la bandeja de
--     Tesoreria. Si alguien aprueba ese lote, sale plata real al banco por
--     una factura de prueba.
--
-- ⛔ DECISION DEL USUARIO, 2026-09-22, despues de ver estos numeros:
--   1. **Anular el pago 477** (no se pago nada; era parte de la prueba).
--      Se hace desde Tesoreria -> Aprobacion de pagos, NO por SQL.
--      Verificado en el codigo: `anularPago` solo cambia el estado a
--      ANULADO y guarda el motivo — **no toca ningun asiento ni genera
--      contabilidad** (PagoProgramadoServiceImpl:2234-2238).
--   2. **Marcar la factura como PAGADA** con el BLOQUE 3 de abajo. Es la
--      unica via que NO genera contabilidad: no crea asiento, no crea
--      aplicacion, solo cambia la marca. La contabilidad de junio (asiento
--      7752) y la de julio (la retencion) **quedan intactas**, que es la
--      condicion que puso el usuario: *"esa contabilidad ya fue generada"*.
--   3. **El saldo de 11.975,04 SE DEJA VISIBLE** en el Estado de Cuenta del
--      proveedor, rotulado "Pagada". Decision explicita del usuario: el
--      saldo existe en libros y no se inventa un abono para taparlo.
--
-- ⛔ POR ESO SE DESCARTO ANULAR LA FACTURA (la vieja "opcion B"): el
--    proceso `anularFacturaCompra` **anula el asiento contable**
--    (FacturaCompraServiceImpl:~212-225) y eso es exactamente lo que el
--    usuario no quiere. Su unica alternativa, `reversarAsiento=true`, deja
--    el asiento original intacto pero **genera una contrapartida**, o sea
--    contabilidad nueva. Ninguna de las dos sirve acá.
-- =====================================================================


-- =====================================================================
-- BLOQUE 3 — MARCARLA COMO PAGADA. ⛔ COMENTADO.
-- Es el camino elegido por el usuario y lo que la saca del combo.
-- ⚠️ ANTES de correrlo: anular el pago 477 en Tesoreria (punto 1 de la
--    decision). Si no, queda una factura "pagada" con un pago vivo que
--    puede irse al banco.
-- El ID es 243, del BLOQUE 0. Se filtra ademas por el numero como segunda
-- guarda: si el ID no correspondiera a esa factura, el UPDATE no toca nada
-- en vez de tocar otra cosa.
-- ESPERADO: "1 fila actualizada".
--
-- UPDATE PGS.FCTC
--    SET FCTCEPAG = 3                      -- PAGADA_TOTAL
--  WHERE ID = 243
--    AND NUMERO = '001-501-000000203';     -- guarda
--
-- -- Control posterior, ANTES del COMMIT: volver a correr el BLOQUE 0.
-- -- ESPERADO: ESTADO_PAGO = 3 y QUE_DICE_HOY = 'PAGADA'. Todo lo demas
-- -- igual que antes: mismo total, mismo estado de documento, mismo asiento.
--
-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA.
-- COMMIT;
--
-- REVERSO (comentado): devuelve el estado de pago a PAGADA PARCIAL, que es
-- el valor exacto que tenia antes segun el BLOQUE 0 del 2026-09-22.
--   UPDATE PGS.FCTC SET FCTCEPAG = 2 WHERE ID = 243;
--   COMMIT;
-- =====================================================================


-- =====================================================================
-- DESCARTADO — anular la factura. Queda escrito por que NO se hizo, que
-- es lo que le sirve al que lea esto dentro de seis meses.
--
-- Anularla era la via mas limpia para un documento de prueba: sale del
-- combo por el primer filtro (`estado === 1`), del Estado de Cuenta, de
-- CxP y del ATS y los cuadres del SRI de junio.
--
-- ⛔ Pero el proceso `anularFacturaCompra` **anula el asiento contable**
--    (FacturaCompraServiceImpl:~212-225: pone el asiento en ANULADO con
--    motivo, fecha y usuario) y ademas reversa los movimientos
--    relacionados — acá, la retencion de 320,76 con su propio asiento de
--    julio. El usuario lo descarto por eso, con una razon que se sostiene:
--    *"no tocar los asientos contables porque esa contabilidad ya se
--    genero"*. Junio ya esta contabilizado.
--
--    La unica variante que no borra el asiento original es
--    `reversarAsiento=true`, que lo deja intacto y **genera una
--    contrapartida** con fecha del reverso (AplicacionPagoCxpServiceImpl
--    :1534-1542, `asientoService.reversionAsiento`). Tampoco sirve: es
--    contabilidad NUEVA, y la condicion era no generar ninguna.
--
--    Por eso la marca de estado de pago es la unica via que cumple las dos
--    cosas a la vez — sacarla del combo y no tocar un solo asiento.
--
-- ⚠️ LO QUE ESTO DEJA ABIERTO, y el usuario lo sabe y lo acepto: la
--    cuenta por pagar de 11.975,04 sigue viva en libros y en el Estado de
--    Cuenta del proveedor, rotulada "Pagada". Si algun dia el contador
--    decide regularizarla, se hace con un asiento de ajuste con fecha del
--    periodo abierto, nunca tocando junio.
-- =====================================================================
