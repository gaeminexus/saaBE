-- =====================================================================
-- e2-71 — La factura de AIG sigue saliendo en el ATS
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-23
--
-- ✅ SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
--
-- EL CASO: el usuario reporta que «la factura de AIG me sigue saliendo en
--   el ATS».
--
-- LO QUE YA SE SABE SIN CONSULTAR NADA — y por eso este script es corto:
--   La factura de AIG **ya estaba marcada como intermediario antes del
--   e2-64**. Su BLOQUE 3 (2026-09-21) dejo escrito el universo esperado de
--   marcadas: *"las 4 que ya habia + esta = 5. En agosto: 470 (AIG), 497 y
--   498 (Empresa Electrica, confirmadas correctas por el usuario) y la 343.
--   Y una de septiembre por 158275.32"*.
--   O sea que la 470 no es un caso nuevo: es el MISMO sintoma que tuvo la
--   343, y para la 343 la causa medida fue que **la marca nunca se habia
--   guardado** (`FCTCESIN = 0`, e2-63).
--
-- ⚠️ HAY OTRA FACTURA DE AIG, Y NO ES LA MISMA: `LEVANTAMIENTO-ATS-103-104.md:520`
--   registra la **190**, `001-006-058190562`, del **2026-07-17**,
--   AIG-METROPOLITANA. Si el ATS que se esta mirando es de JULIO, la 470 no
--   tiene nada que ver y la que sale es la 190 — que **no esta marcada**.
--   Por eso el BLOQUE 0 lista TODAS las de AIG con su periodo: para no
--   arreglar la factura equivocada.
--
-- LAS CAUSAS POSIBLES, en orden de probabilidad (las tres ya vistas en la 343):
--   1. El XML del ATS es ANTERIOR a la marca -> hay que REGENERAR. Marcar
--      una factura no reescribe un anexo ya generado.
--   2. La marca no quedo guardada (el `COMMIT` comentado, §46.1).
--   3. Es otra factura de AIG, de otro periodo, que nadie marco.
--
-- ⛔ LO QUE NO ES: el filtro del generador. Esta medido y funciona —
--   `GeneradorAtsServiceImpl:312` excluye `esIntermediario = 1` de las
--   compras, y `ReporteCuadreSriServiceImpl:339` hace lo mismo en el cuadre
--   104. Si la marca esta puesta y el XML es nuevo, no sale.
--
-- ⚠️ Y OJO CON CONFUNDIR UN AVISO CON UNA LINEA DEL ANEXO: el generador
--   emite *"Factura de intermediario N excluida del ATS tiene la retencion
--   X: no se declara"* (ITEM 13b). Eso es un AVISO en la pantalla, no una
--   linea del archivo. Si lo que se esta viendo es ese texto, la factura
--   **si** esta fuera del ATS y no hay nada que corregir.
--
-- Columnas verificadas contra las entidades:
--   PGS.FCTC -> ID, NUMERO, FECHA, SUBTOTAL, ESTADO, TITULAR, FCTCESIN, FCTCPRIN
--   TSR.TTLR -> TTLRCDGO, TTLRIDNT, TTLRNMBR
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — TODAS las facturas de AIG, de todos los periodos.
-- COMO LEERLO: mirar la fila cuyo PERIODO coincida con el ATS que se esta
--   generando, y su columna ES_INTERMEDIARIO:
--     · = 1  -> esta marcada. El ATS NO la declara: hay que REGENERAR el
--               anexo (causa 1). Si ya se regenero y sigue, avisar al
--               arbitro con el numero de factura y el periodo.
--     · = 0 o nulo -> la marca NO esta puesta (causa 2 o 3). Se marca con
--               el mismo UPDATE del e2-64 cambiandole el ID, y esta vez
--               con el COMMIT descomentado.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - facturas de AIG' AS bloque,
       TO_CHAR(f.FECHA,'YYYY-MM') AS periodo,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       f.ESTADO AS estado_documento,
       NVL(f.FCTCESIN,0) AS es_intermediario,
       f.FCTCPRIN AS producto_intermediario,
       CASE WHEN f.ESTADO <> 1 THEN 'NO (documento inactivo)'
            WHEN NVL(f.FCTCESIN,0) = 1 THEN 'NO (marcada intermediario)'
            ELSE 'SI - el ATS la declara' END AS sale_en_el_ats
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE UPPER(t.TTLRNMBR) LIKE '%AIG%'
 ORDER BY f.FECHA DESC, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El universo de marcadas, para contrastarlo con lo que el
--            e2-64 dejo escrito el 2026-09-21.
-- ESPERADO, si nada se perdio: 470 (AIG), 497 y 498 (Empresa Electrica) y
--   343 en agosto, mas una de septiembre por 158.275,32. CINCO en total.
--   · Si la 470 NO aparece -> su marca se perdio: esa es la causa.
--   · Si aparecen menos de cinco -> se perdio mas de una marca, y hay que
--     mirarlas todas, no solo la de AIG.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - todas las marcadas como intermediario' AS bloque,
       TO_CHAR(f.FECHA,'YYYY-MM') AS periodo,
       f.ID, f.NUMERO, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.FCTCESIN,0) = 1
 ORDER BY f.FECHA DESC, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control de la familia: compras del periodo que el ATS SI va a
--            declarar, ordenadas por monto. Sirve para reconocer de un
--            vistazo si hay alguna otra que tampoco deberia estar.
-- Cambiar las fechas al periodo que se este declarando.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - lo que el ATS declara del periodo' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRNMBR AS proveedor, NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND ( f.FCTCESIN IS NULL OR f.FCTCESIN <> 1 )
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ORDER BY NVL(f.SUBTOTAL,0) DESC;
