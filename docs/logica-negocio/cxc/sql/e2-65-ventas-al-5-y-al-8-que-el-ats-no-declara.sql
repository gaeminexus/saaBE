-- =====================================================================
-- e2-65 — ¿Hay ventas al 5% o al 8%? El ATS de ventas las IGNORA
-- Modulo: cxc / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- POR QUE EXISTE — lo levanto `omen-saa-2-be` al medir el lado ventas del
-- ATS (BE-9a), y NO tiene nada que ver con el frente de clasificacion por
-- tarifa: es un defecto vivo, por su cuenta.
--
--   `acumularVenta` (GeneradorAtsServiceImpl:~563) alimenta la linea de
--   <detalleVentas> asi:
--       baseGravada <- Factura.SUBTOTAL
--       base0       <- Factura.SUBCERO
--       montoIva    <- Factura.VIVA
--   y NO mira SUBTOTAL5, SUBTOTAL8, VIVA5 ni VIVA8.
--
--   Pero la EMISION si las reparte: la pantalla de facturas de ingreso
--   (facturas-ingreso.component.ts:580-612) manda 0 -> subcero, 5 ->
--   subtotal5, 8 -> subtotal8, el resto -> subtotal; y el XML que se le
--   envia al SRI declara esas tarifas.
--
--   Y `baseImpGrav` del ATS es "base imponible tarifa IVA DIFERENTE de 0%",
--   asi que el 5% y el 8% DEBEN estar ahi dentro.
--
--   Conclusion: toda venta emitida al 5% o al 8% tiene su base y su IVA
--   FUERA del ATS. No declarada en absoluto: ni en gravada, ni en 0%.
--
-- QUE CONTESTA ESTE SCRIPT: si eso paso alguna vez, en que periodos y por
--   cuanto. Si da CERO filas, el defecto es real pero no afecto a ninguna
--   declaracion y el arreglo puede ir sin apuro. Si da filas, hay
--   declaraciones incompletas y el orden de prioridad cambia.
--
-- ⚠️ NO CONFUNDIR CON EL CUADRE 104: ahi el 5% SI se trata aparte
--    (casillas 425/435/445), asi que el 104 y el ATS pueden estar dando
--    numeros distintos para el mismo periodo por esta misma causa.
--    El 8% no tiene casilla en el 104.
--
-- Columnas copiadas de las entidades: Factura (CBR.FCTR: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, SUBTOTAL5, SUBTOTAL8, VIVA, VIVA5, VIVA8,
--   ESTADO, COMPRADOR, FACTURADOR), Titular (TSR.TTLR: TTLRCDGO, TTLRIDNT,
--   TTLRNMBR).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LA PREGUNTA, en una fila por periodo.
-- ESPERADO: idealmente CERO filas. Cada fila es base e IVA que el ATS de
--           ese mes NO declaro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - ventas al 5% u 8% por periodo' AS bloque,
       TO_CHAR(f.FECHA, 'YYYY-MM') AS periodo,
       COUNT(*) AS facturas,
       SUM(NVL(f.SUBTOTAL5,0)) AS base_5,
       SUM(NVL(f.VIVA5,0))     AS iva_5,
       SUM(NVL(f.SUBTOTAL8,0)) AS base_8,
       SUM(NVL(f.VIVA8,0))     AS iva_8,
       SUM(NVL(f.SUBTOTAL5,0) + NVL(f.SUBTOTAL8,0)) AS base_no_declarada,
       SUM(NVL(f.VIVA5,0) + NVL(f.VIVA8,0))         AS iva_no_declarado
  FROM CBR.FCTR f
 WHERE NVL(f.SUBTOTAL5,0) <> 0
    OR NVL(f.SUBTOTAL8,0) <> 0
    OR NVL(f.VIVA5,0)     <> 0
    OR NVL(f.VIVA8,0)     <> 0
 GROUP BY TO_CHAR(f.FECHA, 'YYYY-MM')
 ORDER BY periodo DESC;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Una por una, para poder contrastarlas contra el anexo.
-- ESPERADO: la lista de las facturas afectadas, con su cliente.
--   Si el BLOQUE 0 dio cero filas, este tambien y no hay nada que hacer.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - detalle' AS bloque,
       f.ID, f.NUMERO, f.FECHA, f.ESTADO,
       t.TTLRIDNT AS identificacion_cliente, t.TTLRNMBR AS cliente,
       NVL(f.SUBTOTAL,0)  AS subtotal_gravado_que_declara_el_ats,
       NVL(f.SUBCERO,0)   AS base_0,
       NVL(f.SUBTOTAL5,0) AS base_5,  NVL(f.VIVA5,0) AS iva_5,
       NVL(f.SUBTOTAL8,0) AS base_8,  NVL(f.VIVA8,0) AS iva_8,
       NVL(f.VIVA,0)      AS iva_que_declara_el_ats
  FROM CBR.FCTR f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE NVL(f.SUBTOTAL5,0) <> 0
    OR NVL(f.SUBTOTAL8,0) <> 0
    OR NVL(f.VIVA5,0)     <> 0
    OR NVL(f.VIVA8,0)     <> 0
 ORDER BY f.FECHA DESC, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control de contexto: cuantas ventas hay en total y cuantas
--            usan cada columna. Sirve para saber si las de 5%/8% son un
--            caso raro o algo habitual.
-- ESPERADO: informativo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - contexto' AS bloque,
       COUNT(*) AS facturas_de_venta,
       COUNT(CASE WHEN NVL(f.SUBCERO,0)   <> 0 THEN 1 END) AS con_base_0,
       COUNT(CASE WHEN NVL(f.SUBTOTAL5,0) <> 0 THEN 1 END) AS con_base_5,
       COUNT(CASE WHEN NVL(f.SUBTOTAL8,0) <> 0 THEN 1 END) AS con_base_8,
       COUNT(CASE WHEN NVL(f.VIVA,0)      <> 0 THEN 1 END) AS con_iva
  FROM CBR.FCTR f;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Lo mismo para agosto 2026, que es el periodo que se declara.
-- ESPERADO: si da cero, el anexo de agosto no esta afectado por esto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - agosto 2026' AS bloque,
       COUNT(*) AS facturas,
       SUM(NVL(f.SUBTOTAL5,0) + NVL(f.SUBTOTAL8,0)) AS base_no_declarada,
       SUM(NVL(f.VIVA5,0) + NVL(f.VIVA8,0))         AS iva_no_declarado
  FROM CBR.FCTR f
 WHERE f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ( NVL(f.SUBTOTAL5,0) <> 0 OR NVL(f.SUBTOTAL8,0) <> 0
      OR NVL(f.VIVA5,0) <> 0 OR NVL(f.VIVA8,0) <> 0 );
