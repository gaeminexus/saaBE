-- =====================================================================
-- e2-73 — Que le paso al desglose de la factura 343 (001-001-000002868)
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-25
--
-- ✅ SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
--
-- EL CASO: el usuario reporta que la 2868 «no se esta desglosando
--   correctamente entre base 0 y base 15».
--
-- EL VALOR CONOCIDO CONTRA EL QUE HAY QUE CONTRASTAR — y existe, que es lo
--   que hace este diagnostico barato. El BLOQUE 4 del e2-63, corrido el
--   2026-09-21, midio esta misma factura:
--       343 · 001-001-000002868 · SUBTOTAL 1.521,17 · SUBCERO 1.441,17
--                                 gravada 80,00 · VIVA 12,00
--   y coincide con el Excel del contador (80,00 al 15% con 12,00 de IVA,
--   total 1.533,17). O sea que el 21-09 estaba BIEN.
--
-- LA HIPOTESIS: algo la toco DESPUES del 21-09, y hay un candidato con
--   nombre — el **e2-66**. Ese script corre sobre TODAS las facturas de
--   agosto que tengan alguna linea con CODIGOIVASRI 6 (no objeto) o 7
--   (exento) y les REESCRIBE SUBNOOBJ y SUBEXENT desde el detalle.
--
--   Si la 343 tenia una linea asi, ahora tiene SUBNOOBJ o SUBEXENT
--   distintos de cero, y como el ATS calcula
--       baseImpGrav = SUBTOTAL - SUBCERO - SUBNOOBJ - SUBEXENT
--   su base gravada dejo de ser 80,00 y una parte se fue a "no objeto" o
--   "exento". Eso se leeria exactamente como «no se desglosa bien entre
--   base 0 y base 15».
--
--   ⚠️ Ojo: si el detalle DE VERDAD tiene lineas no objeto/exento, el
--   e2-66 no se equivoco — estaria declarando mejor que antes. Lo que hay
--   que mirar es si esas lineas existen de verdad o si el codigo 6/7 quedo
--   mal puesto en el detalle. El BLOQUE 1 lo contesta linea por linea.
--
-- Columnas verificadas contra las entidades:
--   PGS.FCTC -> ID, NUMERO, SUBTOTAL, SUBCERO, SUBNOOBJ, SUBEXENT,
--               SUBTOTAL5, SUBTOTAL8, VIVA, VIVA5, VIVA8, TOTAL, FCTCESIN
--   PGS.DFCC -> FACTURA, DESCRIPCION, BASEIMPONIBLE, CODIGOIVASRI,
--               PORCENTAJEIVA, VALORIVA
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LA CABECERA HOY, y lo que el ATS declararia con ella.
-- COMO LEERLO, contra lo medido el 21-09 (SUBCERO 1.441,17 / gravada 80):
--   · GRAVADA_HOY = 80,00 y NO_OBJETO = EXENTO = 0  -> la cabecera esta
--     bien y el problema no esta aca: mirar el XML o la pantalla.
--   · NO_OBJETO o EXENTO distintos de cero  -> los toco el e2-66, y la
--     gravada bajo en esa misma cantidad. Ir al BLOQUE 1.
--   · SUBCERO distinto de 1.441,17 -> la toco otra cosa; avisar al arbitro
--     con esta salida.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - cabecera hoy' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0)  AS subtotal,
       NVL(f.SUBCERO,0)   AS base_0,
       NVL(f.SUBNOOBJ,0)  AS no_objeto,
       NVL(f.SUBEXENT,0)  AS exento,
       NVL(f.SUBTOTAL5,0) AS base_5,
       NVL(f.SUBTOTAL8,0) AS base_8,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS gravada_hoy,
       NVL(f.VIVA,0)      AS iva,
       NVL(f.TOTAL,0)     AS total,
       NVL(f.FCTCESIN,0)  AS es_intermediario
  FROM PGS.FCTC f
 WHERE f.NUMERO = '001-001-000002868';


-- ---------------------------------------------------------------------
-- BLOQUE 1 — EL DETALLE, LINEA POR LINEA. Es el que explica el porque.
-- ESPERADO si la factura es lo que el Excel del contador dice: lineas al
--   0% por 1.441,17 y lineas gravadas por 80,00 con 12,00 de IVA. NINGUNA
--   con codigo 6 ni 7.
-- Si aparece alguna con codigo 6 o 7, esa es la que el e2-66 movio — y hay
--   que decidir si el codigo esta bien puesto en el detalle o no.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - detalle' AS bloque,
       d.ID AS id_linea,
       SUBSTR(d.DESCRIPCION,1,60) AS descripcion,
       d.CODIGOIVASRI AS codigo_sri,
       CASE d.CODIGOIVASRI WHEN 0 THEN '0%' WHEN 6 THEN 'NO OBJETO'
            WHEN 7 THEN 'EXENTO' WHEN 4 THEN 'GRAVADA 15%'
            ELSE 'codigo ' || NVL(TO_CHAR(d.CODIGOIVASRI),'(nulo)') END AS que_es,
       NVL(d.BASEIMPONIBLE,0) AS base,
       NVL(d.PORCENTAJEIVA,0) AS porcentaje,
       NVL(d.VALORIVA,0)      AS iva_linea
  FROM PGS.DFCC d
 WHERE d.FACTURA IN ( SELECT f.ID FROM PGS.FCTC f WHERE f.NUMERO = '001-001-000002868' )
 ORDER BY d.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — El detalle sumado por codigo, para cuadrarlo de un vistazo
--            contra la cabecera del BLOQUE 0.
-- ESPERADO: la suma por codigo 0 deberia dar 1.441,17 y la gravada 80,00.
--   Si la cabecera y el detalle NO cuadran, el problema es cual de los dos
--   esta mal — y eso lo decide el documento fisico, no el sistema.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - detalle sumado por codigo' AS bloque,
       NVL(TO_CHAR(d.CODIGOIVASRI),'(nulo)') AS codigo_sri,
       COUNT(*) AS lineas,
       SUM(NVL(d.BASEIMPONIBLE,0)) AS base,
       SUM(NVL(d.VALORIVA,0))      AS iva
  FROM PGS.DFCC d
 WHERE d.FACTURA IN ( SELECT f.ID FROM PGS.FCTC f WHERE f.NUMERO = '001-001-000002868' )
 GROUP BY NVL(TO_CHAR(d.CODIGOIVASRI),'(nulo)')
 ORDER BY 2 DESC;
