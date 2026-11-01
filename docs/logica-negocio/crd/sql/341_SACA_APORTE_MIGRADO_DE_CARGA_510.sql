-- =====================================================================================
-- 341 — CORRECCIÓN DE DATOS: el aporte migrado 267360 deja de contarse como de la carga ARCH 510.
-- omen-saa-1, 2026-10-31. Confirmado con crd/sql/340.
--
-- Causa: el control recibido = aplicado y el asiento de la carga suman los aportes de una carga
-- por CRD.APRT.APRTIDAS (AporteDaoServiceImpl.selectByCarga / sumValorPorTipoAporteByCarga). Los
-- aportes MIGRADOS (APRTUSRG = 'BATCH', 2025-03-30) traen en APRTIDAS un número del sistema
-- anterior, con otro significado. El 267360 (CESANTIA PATRONAL MIGRADO, 0,10, de 1990) tiene
-- APRTIDAS = 510, el mismo número de la carga ARCH de octubre: el control lo sumó y la carga
-- abortó por 0,10 de más.
--
-- Qué hace: pone APRTIDAS = NULL SOLO en esa fila. No toca su valor, su entidad ni su fecha.
-- APRTIDAS no tiene otro uso para un aporte migrado (no tiene carga: CRARCDGO nulo).
-- Después de correrlo, cartera vuelve a pulsar «Procesar Archivo» en la carga 510.
-- =====================================================================================

-- CONTROL 0 — antes. Esperado: 1 fila, la 267360, 0,10.
SELECT a.APRTCDGO, a.APRTIDAS, a.CRARCDGO, a.APRTUSRG, a.APRTVLRR, a.APRTGLSA
  FROM CRD.APRT a WHERE a.APRTIDAS = 510;

-- BLOQUE 1
UPDATE CRD.APRT a
   SET a.APRTIDAS = NULL
 WHERE a.APRTCDGO = 267360
   AND a.APRTIDAS = 510
   AND a.CRARCDGO IS NULL
   AND a.APRTUSRG = 'BATCH';

COMMIT;

-- CONTROL 1 — después. Esperado: 0 filas con APRTIDAS = 510.
SELECT COUNT(*) AS FILAS_CON_510 FROM CRD.APRT a WHERE a.APRTIDAS = 510;

-- CONTROL 2 — RIESGO PARA LAS PRÓXIMAS CARGAS: aportes migrados cuyo APRTIDAS cae en el rango
-- de números de carga que vienen (511 a 700). Cada uno de estos haría abortar la carga que tome
-- ese número. Pasar al árbitro: se corrigen todos juntos, o se arregla el código (contar por
-- CRARCDGO) antes de la próxima carga.
SELECT a.APRTIDAS, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTIDAS BETWEEN 511 AND 700
   AND a.CRARCDGO IS NULL
 GROUP BY a.APRTIDAS
 ORDER BY a.APRTIDAS;

-- REVERSO (comentado)
-- UPDATE CRD.APRT SET APRTIDAS = 510 WHERE APRTCDGO = 267360 AND APRTIDAS IS NULL;
-- COMMIT;
