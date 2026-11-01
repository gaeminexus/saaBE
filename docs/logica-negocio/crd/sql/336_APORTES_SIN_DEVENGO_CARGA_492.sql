-- =====================================================================================
-- 336 — SOLO LECTURA. omen-saa-1, 2026-10-31.
-- Aportes de la carga 492 que quedaron en UNA sola fila sin mes de devengo.
--
-- Causa (verificada en CargaArchivoPetroServiceImpl.aplicarAfectacionAAporte): una afectación
-- manual (AVPC) a un tipo de aporte crea UNA fila APRT por todo el monto, con APRTPRDV = NULL y
-- tipo de movimiento EXCEDENTE_PETRO, en vez de repartir por mes como el reparto automático
-- (distribuirAportePorDevengo). Desde 364dc1c8 (31-10) esas AVPC se aplican también en filas AH;
-- en la 492 son las AVPC 688-692 (roles 1625, 60274, 10950, 10036, 6127).
--
-- Pasar al árbitro la salida de los tres bloques: con eso se escribe la corrección (partir cada
-- fila en una por mes pendiente, sin cambiar el total por carga y tipo, que es lo que usa el
-- asiento).
-- =====================================================================================

-- 1. ¿Se procesó la 492? (estado de la carga)
SELECT c.CRARCDGO, c.CRARESTD, c.CRARFCAC AS FECHA_CONFIRMA_CONTABILIDAD, c.CRARMSAF, c.CRARANAF
  FROM CRD.CRAR c
 WHERE c.CRARCDGO = 492;

-- 2. Aportes de la 492 sin mes de devengo (los que hay que partir)
SELECT a.APRTCDGO, a.ENTDCDGO, e.ENTDRLPC AS ROL, e.ENTDRZNS, a.TPAPCDGO, a.APRTVLRR, a.APRTVLPG,
       a.APRTSLDO, a.APRTPRDV, a.APRTTPMV, a.APRTFCTR, a.APRTIDST, a.APRTIDAS, a.CNTRCDGO,
       a.FLLLCDGO, a.APRTGLSA
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 492
   AND a.APRTPRDV IS NULL
 ORDER BY e.ENTDRLPC;

-- 3. Para esos partícipes: meses de devengo ya cubiertos (por periodo efectivo) desde 2025-06,
--    por tipo, para saber a qué meses le corresponde cada valor.
SELECT e.ENTDRLPC AS ROL, a.TPAPCDGO,
       (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
             WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END) AS MES,
       SUM(a.APRTVLRR) AS APORTADO, COUNT(*) AS FILAS
  FROM CRD.APRT a
  JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE e.ENTDRLPC IN (1625, 60274, 10950, 10036, 6127)
   AND a.TPAPCDGO IN (9, 11)
   AND NOT (a.APRTIDAS = 492 AND a.APRTPRDV IS NULL)
 GROUP BY e.ENTDRLPC, a.TPAPCDGO,
          (CASE WHEN a.APRTPRDV IS NOT NULL THEN a.APRTPRDV
                WHEN a.APRTVLRR > 0 THEN TRUNC(a.APRTFCTR, 'MM') END)
HAVING (CASE WHEN MAX(a.APRTPRDV) IS NOT NULL THEN MAX(a.APRTPRDV)
             ELSE MAX(TRUNC(a.APRTFCTR, 'MM')) END) >= DATE '2025-06-01'
 ORDER BY 1, 2, 3;

-- 3b. Vigencias activas (esperado mensual) de esos partícipes, por tipo.
SELECT e.ENTDRLPC AS ROL, v.TPAPCDGO, v.VGCNFCIN, v.VGCNFCFN, v.VGCNMNTO, v.VGCNIDST
  FROM CRD.VGCN v
  JOIN CRD.CNTR k ON k.CNTRCDGO = v.CNTRCDGO AND k.CNTRESTD = 1
  JOIN CRD.ENTD e ON e.ENTDCDGO = k.ENTDCDGO
 WHERE e.ENTDRLPC IN (1625, 60274, 10950, 10036, 6127)
   AND v.VGCNIDST = 1
 ORDER BY 1, 2, 3;
