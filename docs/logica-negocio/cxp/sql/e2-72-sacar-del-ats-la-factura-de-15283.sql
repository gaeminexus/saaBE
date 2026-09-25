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
-- ⭐ SALIDA DEL BLOQUE 0 Y DECISIONES DEL USUARIO — 2026-09-23
--
--   El proveedor es **EQUISUIZA** y NO tiene dos facturas: tiene CUATRO,
--   todas activas, todas 100% base 0% sin IVA, y NINGUNA marcada:
--     id 468 · 004-100-003891553 · 26/08 · 15.283,98 · asiento 8319
--     id 469 · 004-100-003891549 · 26/08 ·      14,15 · asiento 8320
--     id 191 · 004-100-003713029 · 22/07 · 14.812,85 · asiento 7561
--     id 189 · 004-100-003556736 · 03/07 · 15.909,38 · asiento 7559
--
--   Es el §29 otra vez: el usuario senalo UNA y la familia son CUATRO.
--   Contarlas antes de arreglar la que estaba a mano es lo que evito dejar
--   tres sueltas — dos de ellas de casi 15.000 cada una.
--
--   **DECISION 1 — alcance: SOLO LAS DOS DE AGOSTO (468 y 469).**
--   Las de julio ya se declararon. Sacarlas ahora cambiaria un periodo YA
--   PRESENTADO al SRI por 30.722,23 entre las dos, y eso puede obligar a
--   una sustitutiva. Mismo criterio que el usuario tomo con julio en el
--   e2-66. Quedan documentadas abajo, sin tocar.
--
--   **DECISION 2 — motivo: SI es intermediario**, el mismo caso que
--   AIG-METROPOLITANA y GENERALI, que ya estan marcadas. Son polizas que
--   ASOPREP intermedia y no son gasto propio. La marca FCTCESIN es
--   semanticamente correcta y el mecanismo es el probado.
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
--  WHERE ID = 468
--    AND NUMERO = '004-100-003891553';
--
-- -- 2.2 — la del mismo proveedor, misma serie y mismo dia, por 14,15.
-- --       CONFIRMADA por el usuario: es el mismo caso, va tambien.
-- UPDATE PGS.FCTC
--    SET FCTCESIN = 1
--  WHERE ID = 469
--    AND NUMERO = '004-100-003891549';
--
-- -- 2.3 — 🔴 LA 343 VUELVE AL ATS. Va en sentido CONTRARIO a las de arriba.
-- --        El 2026-09-21 se la marco como intermediario (e2-64). El 2026-09-25
-- --        el contador paso la lista completa de «no declaradas, no son costo
-- --        ni gasto» de agosto — AIG, las dos de EQUISUIZA y las dos de la
-- --        Electrica — y la 343 NO ESTA en esa lista. El usuario lo confirmo:
-- --        *«Esta factura debe aparecer en el ATS y no esta apareciendo»*.
-- --        Mientras siga marcada, a agosto le FALTAN 1.521,17: no es que sobre
-- --        algo, es que falta. Por eso se desmarca.
-- UPDATE PGS.FCTC
--    SET FCTCESIN = 0
--  WHERE ID = 343
--    AND NUMERO = '001-001-000002868';
--
-- ⛔ 2.4 — LAS DOS DE JULIO **NO SE TOCAN**. Decision del usuario, 2026-09-23.
--    Julio ya se declaro; sacarlas ahora cambiaria un periodo presentado por
--    30.722,23 y puede obligar a una sustitutiva. Quedan escritas para que
--    conste que se sabe que estan, no porque haya que correrlas:
--      id 191 · 004-100-003713029 · 22/07 · 14.812,85
--      id 189 · 004-100-003556736 · 03/07 · 15.909,38
--    Si algun dia el contador decide regularizar julio, es ESTE update con
--    esos dos ID — pero es una decision tributaria, no de datos.
--
-- -- Control posterior, ANTES del COMMIT: volver a correr el BLOQUE 0.
-- -- ESPERADO: ES_INTERMEDIARIO = 1 en la(s) que se marco, y TODO lo demas
-- -- igual que antes: mismo subtotal, mismo estado, mismo asiento.
--
-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA.
-- COMMIT;
--
-- DESPUES DEL COMMIT: **REGENERAR el ATS de 08/2026**. El control es doble:
--   · los secuenciales 003891553 y 003891549 NO deben aparecer
--   · el secuencial 000002868 (la 343) SI debe aparecer, con 1.441,17 de base 0%
--     y 80,00 gravado con 12,00 de IVA
-- Marcar una factura
-- no reescribe un anexo ya generado — el XML viejo va a seguir
-- mostrandola para siempre. Y al regenerar, el control es directo: el
-- secuencial 003891553 no debe aparecer ni una vez en el XML, igual que
-- hoy no aparecen 058810437, 000002868, 134308071 ni 134287687.
--
-- REVERSO (comentado): devuelve la marca a cero.
--   UPDATE PGS.FCTC SET FCTCESIN = 0 WHERE ID = <<ID>>;
--   COMMIT;
-- =====================================================================
