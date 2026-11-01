-- =====================================================================================
-- 358 — SOLO LECTURA. omen-saa-1, 2026-11-01. Plan cuentas a plantilla, P3 (modo arranque).
-- En el modo arranque, el ajuste de bandas del cierre de cartera usa todavía «madre + '.25'» escrito en
-- el código. La propuesta es derivar esa cuenta de la banda de MÁS días de cada madre en CRD.BNDP,
-- sin plantilla ni literal. Antes de cambiarlo hay que confirmar que da la misma cuenta.
-- LECTURA: cada madre con UNA sola fila, RESULTADO = 'IGUAL' y EXISTE_LA_25 = 1. Si una madre sale
-- 'DISTINTA' o con varias filas, el arranque se deja como está.
-- Consulta propuesta por omen-saa-1-be (nombres de columna tomados de las entidades; si Oracle
-- rechaza alguno, pasar el error).
-- =====================================================================================

WITH bandas AS (
  SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR AS TIPO_CARTERA, b.BNDPNMRO AS NUM_BANDA, b.BNDPCNTD AS PERIODOS,
         n.PLNNCNTA AS CUENTA, SUBSTR(n.PLNNCNTA, 1, 6) AS MADRE
    FROM CRD.BNDP b
    JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO
    JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO
   WHERE b.BNDPESTD = 1 AND c.CBPRESTD = 1
     AND (c.CBPRFCFN IS NULL OR c.CBPRFCFN >= TRUNC(SYSDATE))
     AND n.PJRQCDGO = 1236
     AND n.PLNNCNTA LIKE '1.3.__.%' AND n.PLNNCNTA NOT LIKE '1.3.99%'),
maxb AS (SELECT CBPRCDGO, MAX(NUM_BANDA) AS NUM_MAX FROM bandas GROUP BY CBPRCDGO)
SELECT x.MADRE,
       x.CUENTA AS CUENTA_BANDA_MAS_DIAS,
       x.MADRE || '.25' AS CUENTA_ACTUAL_EN_EL_CODIGO,
       CASE WHEN x.CUENTA = x.MADRE || '.25' THEN 'IGUAL' ELSE 'DISTINTA' END AS RESULTADO,
       COUNT(*) AS CONFIGURACIONES,
       (SELECT COUNT(*) FROM CNT.PLNN p WHERE p.PJRQCDGO = 1236 AND p.PLNNCNTA = x.MADRE || '.25') AS EXISTE_LA_25
  FROM bandas x JOIN maxb m ON m.CBPRCDGO = x.CBPRCDGO AND m.NUM_MAX = x.NUM_BANDA
 GROUP BY x.MADRE, x.CUENTA
 ORDER BY x.MADRE, x.CUENTA;

-- Complemento: todas las bandas de cada madre, para mirarlas a ojo.
SELECT SUBSTR(n.PLNNCNTA, 1, 6) AS MADRE, n.PLNNCNTA AS CUENTA, MIN(b.BNDPNMRO) AS NUM_MIN, MAX(b.BNDPNMRO) AS NUM_MAX,
       MAX(b.BNDPCNTD) AS PERIODOS_MAX, COUNT(*) AS FILAS
  FROM CRD.BNDP b JOIN CNT.PLNN n ON n.PLNNCDGO = b.PLNNCDGO
 WHERE b.BNDPESTD = 1 AND n.PJRQCDGO = 1236
   AND n.PLNNCNTA LIKE '1.3.__.%' AND n.PLNNCNTA NOT LIKE '1.3.99%'
 GROUP BY SUBSTR(n.PLNNCNTA, 1, 6), n.PLNNCNTA
 ORDER BY 1, 2;
