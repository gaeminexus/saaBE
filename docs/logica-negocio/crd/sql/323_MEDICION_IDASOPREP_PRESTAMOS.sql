-- =====================================================================================
-- 323 — MEDICION: el idAsoprep (CRD.PRST.PRSTIDAS) antes de que lo asigne el sistema
-- Equipo omen-saa-1 · 2026-10-13 · SOLO LECTURA (ningun bloque escribe)
--
-- Por que: decision U6 del usuario (PLAN-CICLO-OTORGAMIENTO.md §7). Desde ahora el
-- idAsoprep lo asigna "Aprobar" como MAX(PRSTIDAS) + 1, sin saltos ni perdidas, y lo
-- garantiza un indice UNICO (sql/324). Este script dice si los datos actuales lo permiten.
--
-- Correr de corrido y devolver al arbitro la salida de los 6 bloques.
-- =====================================================================================

-- BLOQUE 1 — Panorama: maximo, minimo, cuantos tienen numero, cuantos no, cuantos en 0.
SELECT COUNT(*)                                        AS TOTAL_PRESTAMOS,
       COUNT(PRSTIDAS)                                 AS CON_IDASOPREP,
       SUM(CASE WHEN PRSTIDAS IS NULL THEN 1 ELSE 0 END) AS SIN_IDASOPREP,
       SUM(CASE WHEN PRSTIDAS <= 0 THEN 1 ELSE 0 END)  AS EN_CERO_O_NEGATIVO,
       MIN(CASE WHEN PRSTIDAS > 0 THEN PRSTIDAS END)   AS MINIMO_POSITIVO,
       MAX(PRSTIDAS)                                   AS MAXIMO,
       MAX(PRSTIDAS) + 1                               AS PROXIMO_QUE_SE_ASIGNARIA
  FROM CRD.PRST;

-- BLOQUE 2 — Duplicados. El indice unico del sql/324 no se puede crear si hay alguno.
SELECT PRSTIDAS                AS IDASOPREP,
       COUNT(*)                AS VECES,
       LISTAGG(PRSTCDGO || '(est ' || PRSTIDST || ')', ', ')
         WITHIN GROUP (ORDER BY PRSTCDGO) AS PRESTAMOS
  FROM CRD.PRST
 WHERE PRSTIDAS IS NOT NULL
 GROUP BY PRSTIDAS
HAVING COUNT(*) > 1
 ORDER BY PRSTIDAS;

-- BLOQUE 3 — Prestamos NO aprobados que ya tienen numero tipeado a mano.
--   Estados: 1 GENERADO, 6 PENDIENTE_DE_APROBACION, 7 RECHAZADO.
--   Con U6.5 conservan el suyo al aprobar; si el numero es alto, puede chocar con la serie.
SELECT PRSTCDGO AS PRESTAMO, PRSTIDST AS ESTADO, PRSTIDAS AS IDASOPREP
  FROM CRD.PRST
 WHERE PRSTIDST IN (1, 6, 7)
   AND PRSTIDAS IS NOT NULL
 ORDER BY PRSTIDAS DESC;

-- BLOQUE 4 — Indices o restricciones que ya existan sobre PRSTIDAS.
SELECT i.INDEX_NAME, i.UNIQUENESS, c.COLUMN_POSITION
  FROM ALL_INDEXES i
  JOIN ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
 WHERE i.TABLE_OWNER = 'CRD' AND i.TABLE_NAME = 'PRST' AND c.COLUMN_NAME = 'PRSTIDAS';

-- BLOQUE 5 — Los 10 numeros mas altos, con su fecha y estado. Sirve para ver si el MAXIMO
--   es real o un error de tipeo: si el primero esta muy lejos del segundo, la serie nueva
--   arrancaria desde un numero que no corresponde. AVISAR antes de desplegar.
SELECT *
  FROM (SELECT PRSTIDAS AS IDASOPREP, PRSTCDGO AS PRESTAMO, PRSTIDST AS ESTADO,
               PRSTFCIN AS FECHA_INICIO
          FROM CRD.PRST
         WHERE PRSTIDAS IS NOT NULL
         ORDER BY PRSTIDAS DESC)
 WHERE ROWNUM <= 10;

-- BLOQUE 6 — Los prestamos con idAsoprep en 0 o negativo (son los que dejo el FE viejo
--   cuando el campo quedaba vacio). El indice unico tampoco acepta dos ceros.
SELECT PRSTCDGO AS PRESTAMO, PRSTIDST AS ESTADO, PRSTIDAS AS IDASOPREP, PRSTFCIN AS FECHA_INICIO
  FROM CRD.PRST
 WHERE PRSTIDAS <= 0
 ORDER BY PRSTCDGO;

-- Lectura esperada:
--  · Bloque 2 y 6 vacios -> el sql/324 crea el indice unico directo.
--  · Con filas -> el arbitro arma el 324 de otra forma (indice unico solo de la serie nueva,
--    o limpieza previa decidida por el usuario). No se corrige nada a ciegas.
--  · Bloque 5: los dos primeros deberian estar cerca. Si no, AVISAR.
