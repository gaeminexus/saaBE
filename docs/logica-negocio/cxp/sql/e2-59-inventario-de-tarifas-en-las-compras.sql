-- =====================================================================
-- e2-59 — ¿Que tarifas de IVA aparecen de verdad en las compras cargadas?
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- POR QUE EXISTE — pedido del usuario, 2026-09-21: "se habia solicitado que
-- en la reporteria de facturas compras este bien clasificado las compras
-- tarifa 0%, no objeto de IVA, exentas de IVA, tarifa 5%, tarifa 8% y
-- tarifa 15%; en la actualidad no esta bien la clasificacion".
--
-- Es cierto, y medido: la cabecera PGS.FCTC solo tiene SUBTOTAL, SUBCERO
-- (0%), SUBTOTAL5, SUBTOTAL8 -- y las dos ultimas NADIE las llena. No hay
-- columna para exento, y la de no objeto (SUBNOOBJ) la crea el e2-55 de
-- hoy. El dashboard de CXP muestra solo "Subtotal" y "Subtotal 0%".
--
-- El dato por tarifa SI existe, pero en el DETALLE: PGS.DFCC.CODIGOIVASRI,
-- que la carga graba linea por linea desde el XML.
--
-- QUE CONTESTA ESTE SCRIPT: cuales de esas tarifas aparecen realmente en
--   los datos, y cuanta plata hay en cada una. De eso depende el tamaño del
--   frente: si el 5% y el 8% no existen en la practica, se construyen
--   igual pero no urgen.
--
-- TABLA 17 DEL SRI (codigoPorcentaje), para leer la salida:
--   0 = 0%   ·  2 = 12%  ·  3 = 14%  ·  4 = 15%
--   5 = 5%   ·  6 = No objeto de impuesto
--   7 = Exento de IVA    ·  8 = 8%   ·  10 = 13%
--
-- Columnas copiadas de las entidades: DetalleFacturaCompra (PGS.DFCC:
--   FACTURA, BASEIMPONIBLE, VALORIVA, CODIGOIVASRI, DESCRIPCION),
--   FacturaCompra (PGS.FCTC: ID, FECHA, ESTADO, SUBTOTAL, SUBCERO,
--   SUBTOTAL5, SUBTOTAL8, VIVA, TIPOCOMPROBANTE).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — El inventario completo: que codigos hay y cuanto suman.
-- ESPERADO: 0 (0%) y 4 (15%) con casi todo. Lo que aparezca en 5, 6, 7 u 8
--           es lo que hoy se declara y se reporta en la columna equivocada.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - inventario historico' AS bloque,
       NVL(d.CODIGOIVASRI,-1) AS codigo_sri,
       CASE NVL(d.CODIGOIVASRI,-1)
            WHEN -1 THEN 'SIN CODIGO (nulo)'
            WHEN 0 THEN '0%'   WHEN 2 THEN '12%'  WHEN 3 THEN '14%'
            WHEN 4 THEN '15%'  WHEN 5 THEN '5%'   WHEN 6 THEN 'NO OBJETO'
            WHEN 7 THEN 'EXENTO' WHEN 8 THEN '8%' WHEN 10 THEN '13%'
            ELSE 'DESCONOCIDO' END AS tarifa,
       COUNT(DISTINCT d.FACTURA) AS facturas,
       COUNT(*) AS lineas,
       SUM(NVL(d.BASEIMPONIBLE,0)) AS base,
       SUM(NVL(d.VALORIVA,0))      AS iva
  FROM PGS.DFCC d
  JOIN PGS.FCTC f ON f.ID = d.FACTURA
 WHERE f.ESTADO = 1
 GROUP BY NVL(d.CODIGOIVASRI,-1)
 ORDER BY codigo_sri;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Lo mismo, solo agosto 2026, que es el periodo que se declara.
-- ESPERADO: define que hace falta para el anexo de agosto y que puede
--           esperar al frente completo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - agosto 2026' AS bloque,
       NVL(d.CODIGOIVASRI,-1) AS codigo_sri,
       CASE NVL(d.CODIGOIVASRI,-1)
            WHEN -1 THEN 'SIN CODIGO (nulo)'
            WHEN 0 THEN '0%'   WHEN 2 THEN '12%'  WHEN 3 THEN '14%'
            WHEN 4 THEN '15%'  WHEN 5 THEN '5%'   WHEN 6 THEN 'NO OBJETO'
            WHEN 7 THEN 'EXENTO' WHEN 8 THEN '8%' WHEN 10 THEN '13%'
            ELSE 'DESCONOCIDO' END AS tarifa,
       COUNT(DISTINCT d.FACTURA) AS facturas,
       SUM(NVL(d.BASEIMPONIBLE,0)) AS base,
       SUM(NVL(d.VALORIVA,0))      AS iva
  FROM PGS.DFCC d
  JOIN PGS.FCTC f ON f.ID = d.FACTURA
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 GROUP BY NVL(d.CODIGOIVASRI,-1)
 ORDER BY codigo_sri;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las columnas de cabecera que existen y estan vacias.
-- ESPERADO: SUBTOTAL5 y SUBTOTAL8 en 0 o nulo en TODAS las filas. Es la
--           prueba de que la clasificacion por tarifa nunca se construyo,
--           no de que se haya roto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - cabecera: columnas por tarifa' AS bloque,
       COUNT(*) AS facturas_activas,
       COUNT(CASE WHEN NVL(f.SUBCERO,0)    <> 0 THEN 1 END) AS con_subcero,
       COUNT(CASE WHEN NVL(f.SUBTOTAL5,0)  <> 0 THEN 1 END) AS con_subtotal5,
       COUNT(CASE WHEN NVL(f.SUBTOTAL8,0)  <> 0 THEN 1 END) AS con_subtotal8,
       SUM(NVL(f.SUBTOTAL5,0)) AS suma_subtotal5,
       SUM(NVL(f.SUBTOTAL8,0)) AS suma_subtotal8
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — La cabecera contra el detalle, tarifa por tarifa, en agosto.
--            Es la medida exacta de "cuanto declara mal el ATS de agosto".
-- ESPERADO: DIFERENCIA_0 y DIFERENCIA_GRAVADA en 0 si todo estuviera bien.
--           Lo que salga es lo que hay que corregir.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - cabecera vs detalle, agosto' AS bloque,
       COUNT(*) AS facturas,
       SUM(base_0_cab)      AS base_0_cabecera,
       SUM(base_0_det)      AS base_0_detalle,
       SUM(base_0_det - base_0_cab) AS diferencia_0,
       SUM(base_grav_det)   AS base_gravada_detalle,
       SUM(subtotal - base_0_cab)   AS base_gravada_que_declara_el_ats,
       SUM((subtotal - base_0_cab) - base_grav_det) AS diferencia_gravada
  FROM (
    SELECT f.ID,
           NVL(f.SUBTOTAL,0) AS subtotal,
           NVL(f.SUBCERO,0)  AS base_0_cab,
           (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_det,
           (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
               AND d.CODIGOIVASRI NOT IN (0,6,7)) AS base_grav_det
      FROM PGS.FCTC f
     WHERE f.ESTADO = 1
       AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
       AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
  );
