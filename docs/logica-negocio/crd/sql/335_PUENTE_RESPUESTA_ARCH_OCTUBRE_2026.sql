-- =====================================================================================
-- 335 — PUENTE: respuesta de ARCH de OCTUBRE 2026 cargada por la pantalla de carga Petro.
-- omen-saa-1, 2026-10-31. Decisión del usuario: hacer los dos procesos hoy (Petro y ARCH) para
-- que no corra mora. La carga de ARCH «de verdad» (petro/DISENO-CARGA-RESPUESTA-ARCH.md) se
-- programa después; esto es un puente SIN CÓDIGO NUEVO.
--
-- Idea: la pantalla de carga Petro ya deja elegir la filial (ARCH = 2) y lee el formato del
-- archivo de Petrocomercial. Este script arma ese archivo a partir de NUESTRA generación ARCH de
-- octubre (CRD.GNAP 173: lo que le mandamos a ARCH, por partícipe y producto) y de lo que ARCH
-- dice que descontó. Después se sigue el flujo normal de Petro: vista previa, novedades,
-- Afectar cuotas, transferencia, procesar.
--
-- Problema que resuelve el BLOQUE 1: la carga identifica por ROL (ENTD.ENTDRLPC) y de las 7
-- entidades de ARCH, 5 no tienen rol y 2 lo tienen en 0 (salida B3 del 332). Se les asigna un
-- rol TÉCNICO = 9000000 + ENTDCDGO: 7 dígitos (cabe en las columnas 1-7 del archivo), muy por
-- encima de cualquier rol real de Petro, y no se repite porque ENTDCDGO es único.
--
-- ORDEN:  CONTROL 0 -> BLOQUE 1 (UPDATE + COMMIT) -> CONTROL 1 -> completar la respuesta de
--         ARCH en el BLOQUE 2 -> correr el BLOQUE 2 -> copiar la columna LINEA a un .txt.
-- =====================================================================================

-- ---------------------------------------------------------------------
-- CONTROL 0 — antes. Esperado: 7 entidades de filial 2, ninguna con rol válido, y
--             CHOQUES = 0 (nadie usa ya un rol del rango técnico).
-- ---------------------------------------------------------------------
SELECT e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC, 9000000 + e.ENTDCDGO AS ROL_TECNICO, e.ENTDIDST
  FROM CRD.ENTD e
 WHERE e.FLLLCDGO = 2
 ORDER BY e.ENTDNMID;

SELECT COUNT(*) AS CHOQUES
  FROM CRD.ENTD e
 WHERE e.ENTDRLPC IN (SELECT 9000000 + x.ENTDCDGO FROM CRD.ENTD x WHERE x.FLLLCDGO = 2);

-- ---------------------------------------------------------------------
-- BLOQUE 1 — asigna el rol técnico a las entidades de ARCH sin rol (nulo o 0).
-- ---------------------------------------------------------------------
UPDATE CRD.ENTD e
   SET e.ENTDRLPC = 9000000 + e.ENTDCDGO
 WHERE e.FLLLCDGO = 2
   AND NVL(e.ENTDRLPC, 0) = 0;

COMMIT;

-- ---------------------------------------------------------------------
-- CONTROL 1 — después. Esperado: las 7 con ENTDRLPC = ROL_TECNICO.
-- ---------------------------------------------------------------------
SELECT e.ENTDCDGO, e.ENTDNMID, e.ENTDRZNS, e.ENTDRLPC,
       CASE WHEN e.ENTDRLPC = 9000000 + e.ENTDCDGO THEN 'OK' ELSE 'REVISAR' END AS ESTADO
  FROM CRD.ENTD e
 WHERE e.FLLLCDGO = 2
 ORDER BY e.ENTDNMID;


-- =====================================================================================
-- BLOQUE 2 — genera el archivo. SOLO LECTURA.
--
-- ⚠️ COMPLETAR la respuesta de ARCH de octubre en «respuesta» (una fila por cédula del PDF de
--    ARCH: cédula entre comillas para no perder el 0, y el MONTO DESCONTADO con punto decimal).
--    Las filas de abajo son de EJEMPLO, NO son octubre: hay que reemplazarlas.
--
-- Reglas (decisión 2 del usuario):
--   - Descontado = lo que mandamos (±0,01): una línea por cada línea de la generación (AH, y por
--     préstamo PE/PH/PQ/PP/HS), con el monto enviado. Se aplica sola.
--   - Descontado distinto de lo que mandamos, o cédula que no mandamos: UNA sola línea AH con
--     todo lo descontado. Sale como novedad y cartera lo reparte en «Afectar cuotas».
--   - Mandado y ARCH no lo descontó (no está en «respuesta» o con 0): no va en el archivo.
-- =====================================================================================
WITH respuesta AS (
            SELECT '2100517560' AS CEDULA, 962.36 AS DESCONTADO FROM DUAL   -- EJEMPLO: reemplazar
  UNION ALL SELECT '0802289066', 189.61 FROM DUAL                           -- EJEMPLO: reemplazar
),
gen AS (                       -- generación ARCH de octubre 2026 (GNAP 173)
  SELECT e.ENTDCDGO, e.ENTDNMID AS CEDULA, e.ENTDRZNS AS NOMBRE, e.ENTDRLPC AS ROL,
         x.PDGACDPT AS PRODUCTO, x.PDGAMNEN AS ENVIADO, x.PDGANNLN AS NLINEA
    FROM CRD.PDGA x
    JOIN CRD.DTGA d ON d.DTGACDGO = x.DTGACDGO
    JOIN CRD.ENTD e ON e.ENTDCDGO = x.ENTDCDGO
   WHERE d.GNAPCDGO = 173
),
tot AS (
  SELECT CEDULA, SUM(ENVIADO) AS ENVIADO_TOTAL FROM gen GROUP BY CEDULA
),
registros AS (
  -- (a) descuento completo: tal cual la generación
  SELECT g.ROL, g.NOMBRE, g.PRODUCTO, g.ENVIADO AS MONTO, g.NLINEA
    FROM gen g
    JOIN tot t ON t.CEDULA = g.CEDULA
    JOIN respuesta r ON r.CEDULA = g.CEDULA
   WHERE ABS(r.DESCONTADO - t.ENVIADO_TOTAL) <= 0.01
  UNION ALL
  -- (b) descuento distinto de lo enviado, o cédula no enviada: una línea AH con lo descontado
  SELECT e.ENTDRLPC, e.ENTDRZNS, 'AH', r.DESCONTADO, 0
    FROM respuesta r
    JOIN CRD.ENTD e ON e.ENTDNMID = r.CEDULA AND e.FLLLCDGO = 2
    LEFT JOIN tot t ON t.CEDULA = r.CEDULA
   WHERE r.DESCONTADO > 0.01
     AND (t.CEDULA IS NULL OR ABS(r.DESCONTADO - t.ENVIADO_TOTAL) > 0.01)
),
productos AS (
  SELECT DISTINCT PRODUCTO,
         CASE PRODUCTO WHEN 'AH' THEN 1 WHEN 'HS' THEN 2 WHEN 'PE' THEN 3 WHEN 'PH' THEN 4
                       WHEN 'PQ' THEN 5 WHEN 'PP' THEN 6 ELSE 9 END AS ORDEN,
         CASE PRODUCTO WHEN 'AH' THEN 'APORTE VOLUNTARIO' WHEN 'HS' THEN 'SEGURO DE INCENDIO'
                       WHEN 'PE' THEN 'PRESTAMO EMERGENTE' WHEN 'PH' THEN 'PRESTAMO HIPOTECARIO'
                       WHEN 'PQ' THEN 'PRESTAMO QUIROGRAFARIO' WHEN 'PP' THEN 'PRESTAMO PRENDARIO'
                       ELSE PRODUCTO END AS DESCRIPCION
    FROM registros
),
cabecera AS (   -- 8 líneas de cabecera + producto + "====" (el lector salta 8 líneas tras "EP")
  SELECT p.ORDEN, n.N AS SUB, 0 AS ROL_ORDEN,
         CASE n.N
           WHEN 1 THEN '  EP PETROECUADOR'
           WHEN 2 THEN '  GESTION TALENTO HUMANO'
           WHEN 3 THEN ' '
           WHEN 4 THEN '  SUBSISTEMA DE ROL DE PAGOS          DESCUENTOS ARCH ASOPREP OCTUBRE 2026'
           WHEN 5 THEN ' ' || RPAD('*', 140, '*')
           WHEN 6 THEN '  ROL   APELLIDOS Y NOMBRES              PLAZO   SALDO ACTUAL   MESES %INTER    %SEGURO   TOT.PROPUES        R E A L  D E S C O N T A D O'
           WHEN 7 THEN '                                         INICIAL                PLAZO  ANUAL  DESGRAVAM   A DESCONTAR        CAPITAL     INTERESES   SEG.DES'
           WHEN 8 THEN ' ' || RPAD('*', 140, '*')
           WHEN 9 THEN '  ' || RPAD(p.PRODUCTO, 4) || p.DESCRIPCION
           WHEN 10 THEN '  ============================================'
         END AS LINEA
    FROM productos p
    CROSS JOIN (SELECT LEVEL AS N FROM DUAL CONNECT BY LEVEL <= 10) n
),
detalle AS (   -- columnas fijas que lee CargaArchivoPetroServiceImpl.procesarContenido
  SELECT p.ORDEN, 100 AS SUB, r.ROL AS ROL_ORDEN,
         LPAD(TO_CHAR(r.ROL), 7)                                                -- 0-7   rol
      || ' ' || RPAD(SUBSTR(r.NOMBRE, 1, 36), 36)                               -- 7-44  nombre
      || LPAD('1', 6)                                                           -- 44-50 plazo inicial
      || LPAD(TO_CHAR(r.MONTO, 'FM9999990D00', 'NLS_NUMERIC_CHARACTERS='',.'''), 11)  -- 50-61 saldo
      || LPAD('1', 4)                                                           -- 61-65 meses
      || LPAD('0,00', 5)                                                        -- 65-70 % interés
      || LPAD('0,000000', 10)                                                   -- 70-80 % seguro
      || LPAD(TO_CHAR(r.MONTO, 'FM9999990D00', 'NLS_NUMERIC_CHARACTERS='',.'''), 15)  -- 80-95 a descontar
      || LPAD(TO_CHAR(r.MONTO, 'FM9999990D00', 'NLS_NUMERIC_CHARACTERS='',.'''), 15)  -- 95-110 capital desc.
      || LPAD('0,00', 15)                                                       -- 110-125 interés desc.
      || LPAD('0,00', 15)                                                       -- 125-140 seguro desc.
      || LPAD(TO_CHAR(r.MONTO, 'FM9999990D00', 'NLS_NUMERIC_CHARACTERS='',.'''), 15)  -- 140-155 TOTAL DESCONTADO
      || LPAD('0,00', 15)                                                       -- 155-170 capital no desc.
      || LPAD('0,00', 14)                                                       -- 170-184 interés no desc.
      || LPAD('0,00', 14)                                                       -- 184-198 desgrav. no desc.
         AS LINEA
    FROM registros r
    JOIN productos p ON p.PRODUCTO = r.PRODUCTO
)
SELECT LINEA FROM (
  SELECT ORDEN, SUB, ROL_ORDEN, LINEA FROM cabecera
  UNION ALL
  SELECT ORDEN, SUB, ROL_ORDEN, LINEA FROM detalle
)
ORDER BY ORDEN, SUB, ROL_ORDEN;

-- ---------------------------------------------------------------------
-- CONTROL 2 — cuadre del archivo contra la respuesta de ARCH. Debe dar DIFERENCIA = 0.
-- (Mismo WITH respuesta de arriba: copiar las mismas filas.)
-- ---------------------------------------------------------------------
-- WITH respuesta AS ( ...las mismas filas... )
-- SELECT SUM(DESCONTADO) AS TOTAL_ARCH FROM respuesta;
-- El total de la columna TOTAL DESCONTADO del archivo tiene que ser igual a TOTAL_ARCH, y es el
-- monto de la transferencia que cartera registra en la carga.


-- =====================================================================================
-- REVERSO del BLOQUE 1 (comentado). Deja sin rol a las entidades de ARCH. Las 2 que tenían 0
-- quedan en NULL (las dos cosas significan «sin rol»). Correr SOLO si no se procesó ninguna
-- carga con estos roles.
-- =====================================================================================
-- UPDATE CRD.ENTD e SET e.ENTDRLPC = NULL
--  WHERE e.FLLLCDGO = 2 AND e.ENTDRLPC = 9000000 + e.ENTDCDGO;
-- COMMIT;
