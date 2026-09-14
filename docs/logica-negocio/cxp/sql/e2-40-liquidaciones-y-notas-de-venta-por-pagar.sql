-- =====================================================================
-- e2-40 — ¿Qué liquidaciones de compra y notas de venta hay para pagar?
-- Modulo: cxp / pagos  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-14
--
-- ✅ SOLO LECTURA. No inserta, no actualiza, no borra, no hace COMMIT.
--    Correr entero y pegar la salida completa, CON las cabeceras.
--
-- POR QUE EXISTE
--   La pantalla CXP -> Pagos -> Solicitud de pago solo ofrece facturas.
--   Medido en el codigo (2026-09-14): PGS.PGTR tiene FK a la factura
--   (PGTRFCTC) y NINGUNA columna para una liquidacion de compra, asi que
--   pagar una liquidacion hoy no existe — no es un filtro roto.
--
--   Antes de diseñar hay que saber, contra los datos y no contra el codigo:
--     a) si PGS.LQCC (el documento CXP de la liquidacion, el que lleva la
--        cuenta por pagar) tiene filas — un comentario del frontend dice
--        que "esta vacia en produccion", y puede estar viejo;
--     b) cuantas liquidaciones emitidas (CBR.LQCS) todavia NO tienen su
--        documento CXP (no se pueden pagar hasta generarlo);
--     c) si las notas de venta (PGS.FCTC con TIPOCOMPROBANTE = '02') ya
--        existen y con que estado de pago — el codigo dice que se graban
--        como factura y deberian estar saliendo ya en el listado.
--
-- Nombres de columna copiados de las entidades, no de memoria:
--   LiquidacionCompra (CBR.LQCS), LiquidacionCompraCompra (PGS.LQCC),
--   FacturaCompra (PGS.FCTC), AplicacionPagoCxp (PGS.APLP),
--   PagoProgramado (PGS.PGTR).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control: la forma de PGS.PGTR
-- ESPERADO: PGTRFCTC con NULLABLE = 'Y' (egresos y anticipos ya la dejan
--           nula). Y NINGUNA fila para PGTRLQCC: la columna todavia no
--           existe. Si PGTRLQCC aparece, DETENERSE y avisar al arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - columnas PGTR' AS bloque,
       column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS'
   AND table_name = 'PGTR'
   AND column_name IN ('PGTRFCTC', 'PGTRLQCC')
 ORDER BY column_name;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Liquidaciones emitidas (CBR.LQCS), con o sin documento CXP
-- ESPERADO: filas con TIENE_DOC_CXP = 'SI' si alguna ya se contabilizo.
--           Las 'NO' con autorizacion son las que hoy no se pueden pagar
--           hasta pulsar "Generar documento CXP".
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - LQCS por estado' AS bloque,
       l.ESTADO,
       l.ESTADOEMISION,
       CASE WHEN l.LQCSLQCC IS NULL THEN 'NO' ELSE 'SI' END AS tiene_doc_cxp,
       CASE WHEN l.AUTORIZACION IS NULL THEN 'NO' ELSE 'SI' END AS tiene_autorizacion,
       COUNT(*)            AS cantidad,
       SUM(l.TOTAL)        AS total
  FROM CBR.LQCS l
 GROUP BY l.ESTADO, l.ESTADOEMISION,
          CASE WHEN l.LQCSLQCC IS NULL THEN 'NO' ELSE 'SI' END,
          CASE WHEN l.AUTORIZACION IS NULL THEN 'NO' ELSE 'SI' END
 ORDER BY l.ESTADO, l.ESTADOEMISION;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Documentos CXP de liquidacion (PGS.LQCC)
-- ESPERADO: si da CERO filas, el comentario del frontend tiene razon y
--           no hay nada que pagar todavia. LQCCEPAG: 1 pendiente,
--           2 parcial, 3 pagada.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - LQCC por estado' AS bloque,
       q.ESTADO,
       q.LQCCEPAG,
       CASE WHEN q.ASIENTO IS NULL THEN 'NO' ELSE 'SI' END AS tiene_asiento,
       COUNT(*)     AS cantidad,
       SUM(q.TOTAL) AS total
  FROM PGS.LQCC q
 GROUP BY q.ESTADO, q.LQCCEPAG, CASE WHEN q.ASIENTO IS NULL THEN 'NO' ELSE 'SI' END
 ORDER BY q.ESTADO, q.LQCCEPAG;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Liquidaciones CXP sin pagar del todo, con lo ya aplicado
-- ESPERADO: una fila por liquidacion no pagada. APLICADO_POR_ESTADO
--           separa las aplicaciones por APLPESTD para no suponer cual es
--           el valor de "activa". SALDO_SI_TODO_ACTIVO es una cota, no
--           el saldo oficial.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - LQCC pendientes' AS bloque,
       q.ID            AS id_lqcc,
       q.NUMERO,
       q.TITULAR,
       q.TOTAL,
       q.LQCCEPAG,
       ap.aplicado_por_estado,
       q.TOTAL - NVL(ap.aplicado_total, 0) AS saldo_si_todo_activo
  FROM PGS.LQCC q
  LEFT JOIN (SELECT g.APLPLQCC,
                    LISTAGG(g.APLPESTD || '=' || TO_CHAR(g.monto), ' | ')
                      WITHIN GROUP (ORDER BY g.APLPESTD) AS aplicado_por_estado,
                    SUM(g.monto)                         AS aplicado_total
               FROM (SELECT a.APLPLQCC, a.APLPESTD, SUM(a.APLPMAPL) AS monto
                       FROM PGS.APLP a
                      WHERE a.APLPLQCC IS NOT NULL
                      GROUP BY a.APLPLQCC, a.APLPESTD) g
              GROUP BY g.APLPLQCC) ap
         ON ap.APLPLQCC = q.ID
 WHERE NVL(q.LQCCEPAG, 1) <> 3
 ORDER BY q.ID DESC;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Tipos de comprobante en PGS.FCTC
-- ESPERADO: '01' factura y, si hay, '02' nota de venta. Sirve para saber
--           si las notas de venta ya estan en la tabla que alimenta el
--           listado de la pantalla.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - FCTC por tipo' AS bloque,
       f.TIPOCOMPROBANTE,
       f.ESTADO,
       NVL(TO_CHAR(f.FCTCEPAG), 'NULO') AS estado_pago,
       COUNT(*)     AS cantidad,
       SUM(f.TOTAL) AS total
  FROM PGS.FCTC f
 GROUP BY f.TIPOCOMPROBANTE, f.ESTADO, NVL(TO_CHAR(f.FCTCEPAG), 'NULO')
 ORDER BY f.TIPOCOMPROBANTE, f.ESTADO;


-- ---------------------------------------------------------------------
-- BLOQUE 5 — Notas de venta, una por una
-- ESPERADO: si hay filas con ESTADO activo y estado de pago distinto de 3,
--           la nota de venta DEBERIA estar saliendo hoy en el listado de
--           la pantalla (mezclada con las facturas). Si el usuario no la
--           ve, la causa es otra y hay que buscarla.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 5 - notas de venta' AS bloque,
       f.ID,
       f.NUMERO,
       f.TITULAR,
       f.FECHA,
       f.TOTAL,
       f.ESTADO,
       f.FCTCEPAG,
       (SELECT COUNT(*) FROM PGS.PGTR p WHERE p.PGTRFCTC = f.ID) AS pagos_registrados
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
 ORDER BY f.ID DESC;
