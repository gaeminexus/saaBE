-- =====================================================================
-- e2-72 — Sacar del ATS la factura de 15.283,98 del proveedor 1790007502001
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-23
--
-- ⚠️ LOS BLOQUES 0 y 1 SON SOLO LECTURA. El BLOQUE 2 ESCRIBE y esta
--    COMENTADO. Su COMMIT tambien: sin el, el UPDATE no queda guardado y
--    no deja ningun rastro de haber faltado (§46.1).
--
-- EL CASO — el usuario mostro el DIMM con el ATS de 08/2026 ya cargado y
--   senalo la fila que no debe declararse. Identificado por el arbitro en
--   el XML entregado (AT082026.xml, generado el 2026-09-23):
--
--     proveedor 1790007502001, dos facturas, las dos del 26/08/2026,
--     codSustento 02, tipoComprobante 01, SIN IVA y SIN retencion:
--       · 004-100-003891549  ->  baseImponible    14,15   (linea 2048)
--       · 004-100-003891553  ->  baseImponible 15.283,98  (linea 2081)  <-- la senalada
--
-- ⛔ AVERIGUACION PREVIA QUE YA SE HIZO, para que nadie la repita: el
--   reporte original decia «la factura de AIG sigue saliendo». Se verifico
--   sobre el XML real y **AIG NO ESTA**: su RUC (1790475247001) aparece
--   CERO veces, igual que R&M (0992939680001) y que los dos secuenciales
--   marcados de la Electrica. Las cuatro facturas con FCTCESIN=1 estan
--   correctamente excluidas. El filtro del generador funciona. La factura
--   a sacar es otra, de otro proveedor.
--
-- ⚠️ EL MOTIVO LO DEFINE EL USUARIO, Y CAMBIA EL TRATAMIENTO:
--   · Si es del MISMO caso que las otras cuatro —ASOPREP actua de
--     intermediario y el gasto no es propio— la marca FCTCESIN = 1 es
--     correcta y es lo que hace el BLOQUE 2. Ese es el mecanismo probado:
--     el ATS (GeneradorAtsServiceImpl:312) y el cuadre 104
--     (ReporteCuadreSriServiceImpl:339) la dejan afuera.
--   · Si el motivo fuera otro (documento de prueba, cargado por error,
--     periodo equivocado), marcarla como intermediario seria mentirle al
--     dato: diria «esto es de un tercero» cuando el problema es otro.
--     En ese caso PARAR y avisar al arbitro.
--
-- ⚠️ Y OJO CON LA SEGUNDA: la de 14,15 es del MISMO proveedor, la MISMA
--   serie y el MISMO dia. O las dos son del mismo caso o ninguna. El
--   BLOQUE 2.2 la marca tambien, COMENTADO APARTE, para que sea una
--   decision explicita y no un arrastre.
--
-- Columnas verificadas contra la entidad FacturaCompra:
--   PGS.FCTC -> ID, NUMERO, FECHA, SUBTOTAL, SUBCERO, VIVA, ESTADO,
--               TITULAR, FCTCESIN, FCTCPRIN, ASIENTO
--   TSR.TTLR -> TTLRCDGO, TTLRIDNT, TTLRNMBR
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LAS FACTURAS DEL PROVEEDOR. Solo lectura. ANOTAR ESTA SALIDA:
--            de aca salen los ID para el BLOQUE 2 y es el respaldo del
--            estado anterior.
-- ESPERADO: las dos filas del XML, con ES_INTERMEDIARIO en 0 o nulo.
--   Si alguna ya estuviera en 1, no habria que tocarla.
--   Si aparecen MAS de dos, mirar el periodo antes de marcar nada.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - facturas del proveedor' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0,
       NVL(f.VIVA,0) AS iva,
       f.ESTADO AS estado_documento,
       NVL(f.FCTCESIN,0) AS es_intermediario,
       f.ASIENTO AS asiento
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE REPLACE(t.TTLRIDNT,' ','') = '1790007502001'
 ORDER BY f.FECHA DESC, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El universo de marcadas ANTES de tocar nada. Solo lectura.
-- ESPERADO: las 5 conocidas — 343 (R&M), 470 (AIG), 497 y 498 (Electrica)
--           en agosto, y una de septiembre por 158.275,32.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - marcadas antes' AS bloque,
       TO_CHAR(f.FECHA,'YYYY-MM') AS periodo,
       f.ID, f.NUMERO, t.TTLRNMBR AS proveedor, NVL(f.SUBTOTAL,0) AS subtotal
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1 AND NVL(f.FCTCESIN,0) = 1
 ORDER BY f.FECHA DESC, f.ID;


-- =====================================================================
-- BLOQUE 2 — LA MARCA. ⛔ COMENTADO. Leer los bloques 0 y 1 primero.
--
-- Solo toca FCTCESIN. NO se toca el asiento, ni el estado del documento,
-- ni las bases, ni nada de contabilidad — mismo criterio que el e2-64 con
-- la 343, que funciono y quedo verificado en el XML de hoy.
--
-- Se filtra por NUMERO ademas de por ID: si el ID no correspondiera a esa
-- factura, el UPDATE no toca nada en vez de tocar otra cosa.
--
-- -- 2.1 — la senalada por el usuario: 15.283,98
-- UPDATE PGS.FCTC
--    SET FCTCESIN = 1
--  WHERE ID = <<ID DE LA 004-100-003891553, del BLOQUE 0>>
--    AND NUMERO = '004-100-003891553';
--
-- -- 2.2 — la del mismo proveedor, misma serie y mismo dia, por 14,15.
-- --       ⚠️ DESCOMENTAR SOLO SI EL USUARIO CONFIRMA que es el mismo caso.
-- --       O las dos son de un tercero o ninguna lo es; pero que sea una
-- --       decision, no un arrastre.
-- -- UPDATE PGS.FCTC
-- --    SET FCTCESIN = 1
-- --  WHERE ID = <<ID DE LA 004-100-003891549, del BLOQUE 0>>
-- --    AND NUMERO = '004-100-003891549';
--
-- -- Control posterior, ANTES del COMMIT: volver a correr el BLOQUE 0.
-- -- ESPERADO: ES_INTERMEDIARIO = 1 en la(s) que se marco, y TODO lo demas
-- -- igual que antes: mismo subtotal, mismo estado, mismo asiento.
--
-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA.
-- COMMIT;
--
-- DESPUES DEL COMMIT: **REGENERAR el ATS de 08/2026**. Marcar una factura
-- no reescribe un anexo ya generado — el XML viejo va a seguir
-- mostrandola para siempre. Y al regenerar, el control es directo: el
-- secuencial 003891553 no debe aparecer ni una vez en el XML, igual que
-- hoy no aparecen 058810437, 000002868, 134308071 ni 134287687.
--
-- REVERSO (comentado): devuelve la marca a cero.
--   UPDATE PGS.FCTC SET FCTCESIN = 0 WHERE ID = <<ID>>;
--   COMMIT;
-- =====================================================================
