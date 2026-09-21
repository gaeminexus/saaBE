-- =====================================================================
-- e2-56 — ¿Hay compras cuyo XML trae lineas NO OBJETO (6) o EXENTO (7)?
--          Hoy el ATS las declara como GRAVADAS
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- POR QUE EXISTE — lo levanto `omen-saa-2-be` al entregar el BE-4, y es la
-- misma familia del bomberos pero por otro camino:
--
--   La carga arma la base 0% de la cabecera sumando SOLO las lineas del
--   detalle con CODIGOIVASRI = 0 (ProcesoCargaDocumentosServiceImpl:1697,
--   base0Detalle). Una linea del XML con codigoPorcentaje 6 (no objeto) o
--   7 (exento) SE GRABA en el detalle con su codigo, pero su base NO va a
--   ninguna columna de cabecera: queda dentro del SUBTOTAL y nada mas.
--
--   Y el ATS calcula gravada = SUBTOTAL - SUBCERO - SUBNOOBJ, asi que esa
--   base termina declarada como GRAVADA. Es exactamente el defecto que se
--   corrigio para el bomberos, en un proveedor distinto.
--
--   El e2-55 NO cubre esto: solo mueve las lineas de terceros, que se
--   identifican por su descripcion (BOMBERO/BASURA).
--
-- QUE CONTESTA: si el periodo que se esta declarando tiene compras asi, y
--   cuanta plata esta en la columna equivocada. Si da CERO filas, el anexo
--   de agosto no esta afectado por esto y el arreglo puede esperar.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, VIVA, ESTADO, TITULAR), DetalleFacturaCompra
--   (PGS.DFCC: FACTURA, DESCRIPCION, BASEIMPONIBLE, CODIGOIVASRI),
--   Titular (TSR.TTLR: TTLRCDGO, TTLRIDNT, TTLRNMBR).
--
-- ⚠️ Correr ANTES del e2-55, o despues tambien sirve: el e2-55 pone codigo
--    6 en las lineas de bomberos/basura, asi que DESPUES de correrlo esas
--    lineas van a aparecer aca. Por eso el BLOQUE 1 las separa: la columna
--    ES_TERCERO dice cuales ya estan cubiertas por el e2-55 y cuales no.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Resumen del periodo que se declara (agosto 2026).
-- ESPERADO: idealmente CERO filas. Cada fila es plata que el ATS declara
--           como gravada y no lo es.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - resumen agosto 2026' AS bloque,
       d.CODIGOIVASRI AS codigo_sri,
       CASE d.CODIGOIVASRI WHEN 6 THEN 'NO OBJETO' WHEN 7 THEN 'EXENTO' ELSE 'OTRO' END AS que_es,
       COUNT(DISTINCT f.ID) AS facturas,
       COUNT(*)             AS lineas,
       SUM(NVL(d.BASEIMPONIBLE,0)) AS base_en_la_columna_equivocada
  FROM PGS.DFCC d
  JOIN PGS.FCTC f ON f.ID = d.FACTURA
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(d.CODIGOIVASRI,-1) IN (6,7)
 GROUP BY d.CODIGOIVASRI
 ORDER BY d.CODIGOIVASRI;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El detalle, factura por factura, separando lo que el e2-55
--            ya cubre (bomberos/basura) de lo que no.
-- ESPERADO: si ES_TERCERO = 'SI' en todas, el e2-55 alcanza y no hay nada
--           mas que hacer. Las que digan 'NO' son el frente nuevo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - detalle' AS bloque,
       f.ID, f.NUMERO, f.FECHA,
       t.TTLRIDNT AS ruc_proveedor, t.TTLRNMBR AS proveedor,
       d.DESCRIPCION,
       d.CODIGOIVASRI AS codigo_sri,
       NVL(d.BASEIMPONIBLE,0) AS base_linea,
       NVL(f.SUBTOTAL,0) AS subtotal_factura,
       NVL(f.SUBCERO,0)  AS base_0_factura,
       CASE WHEN UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
              OR UPPER(d.DESCRIPCION) LIKE '%BASURA%' THEN 'SI' ELSE 'NO' END AS es_tercero
  FROM PGS.DFCC d
  JOIN PGS.FCTC f ON f.ID = d.FACTURA
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(d.CODIGOIVASRI,-1) IN (6,7)
 ORDER BY f.FECHA, f.ID, d.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Lo mismo, pero SIN filtro de fecha: cuanto hay en todo el
--            historico. Dice si esto es un caso aislado de agosto o una
--            deuda vieja que va a volver todos los meses.
-- ESPERADO: informativo. No hay que corregir el historico para declarar
--           agosto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - historico completo' AS bloque,
       TO_CHAR(f.FECHA, 'YYYY-MM') AS periodo,
       d.CODIGOIVASRI AS codigo_sri,
       COUNT(DISTINCT f.ID) AS facturas,
       SUM(NVL(d.BASEIMPONIBLE,0)) AS base_en_la_columna_equivocada
  FROM PGS.DFCC d
  JOIN PGS.FCTC f ON f.ID = d.FACTURA
 WHERE f.ESTADO = 1
   AND NVL(d.CODIGOIVASRI,-1) IN (6,7)
 GROUP BY TO_CHAR(f.FECHA, 'YYYY-MM'), d.CODIGOIVASRI
 ORDER BY periodo DESC, codigo_sri;
