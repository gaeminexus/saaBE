-- =====================================================================================
-- 340 — SOLO LECTURA. omen-saa-1, 2026-10-31.
-- Hipótesis de los 0,10 de la carga ARCH 510: el control recibido = aplicado
-- (validarRecibidoIgualAplicado) suma los aportes de la carga con
-- AporteDaoServiceImpl.selectByCarga = «where a.idAsoprep = :idCarga» (CRD.APRT.APRTIDAS). Ese
-- campo también lo traen aportes viejos o migrados con otro significado. El intento de la 510 se
-- revirtió entero, así que TODA fila que hoy tenga APRTIDAS = 510 es AJENA a la carga ARCH y el
-- control la está sumando. Esperado si la hipótesis es cierta: filas que suman 0,10.
-- =====================================================================================

SELECT a.APRTCDGO, a.ENTDCDGO, e.ENTDRZNS, e.FLLLCDGO, a.TPAPCDGO, a.APRTVLRR, a.APRTPRDV,
       a.APRTTPMV, a.APRTFCTR, a.APRTFCRG, a.APRTUSRG, a.CRARCDGO, a.APRTIDST, a.APRTGLSA
  FROM CRD.APRT a
  LEFT JOIN CRD.ENTD e ON e.ENTDCDGO = a.ENTDCDGO
 WHERE a.APRTIDAS = 510
 ORDER BY a.APRTCDGO;

SELECT COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL FROM CRD.APRT a WHERE a.APRTIDAS = 510;

-- ¿Pasa lo mismo con otros números de carga? (APRTIDAS que no coincide con la carga de la fila)
SELECT a.APRTIDAS, COUNT(*) AS FILAS, SUM(a.APRTVLRR) AS TOTAL
  FROM CRD.APRT a
 WHERE a.APRTIDAS IS NOT NULL
   AND (a.CRARCDGO IS NULL OR a.CRARCDGO <> a.APRTIDAS)
 GROUP BY a.APRTIDAS
 ORDER BY a.APRTIDAS DESC
 FETCH FIRST 30 ROWS ONLY;
