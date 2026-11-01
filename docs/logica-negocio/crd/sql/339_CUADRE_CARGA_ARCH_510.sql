-- =====================================================================================
-- 339 — SOLO LECTURA. omen-saa-1, 2026-10-31.
-- Carga ARCH 510: el proceso abortó con «recibido 866,55, aplicado 866,65 (préstamos 479,14 +
-- aportes 387,51), 0,10 aplicados de más», sin ningún partícipe descuadrado en el mensaje. El
-- intento se revirtió: no hay pagos grabados de esa corrida, así que se cruza lo que la carga
-- TRAE con lo que se mandó (generación ARCH 173) y con las afectaciones manuales.
-- Pasar al árbitro la salida de los 5 bloques.
-- =====================================================================================

-- 1. Cabecera y transferencias registradas
SELECT c.CRARCDGO, c.FLLLCDGO, c.CRARMSAF, c.CRARANAF, c.CRARESTD, c.CRARTTDS AS TOTAL_DESCONTADO,
       c.CRARFCAC AS CONFIRMA_CONTAB
  FROM CRD.CRAR c WHERE c.CRARCDGO = 510;

SELECT t.TRCRCDGO, t.TRCRVLRR, t.TRCRIDST, t.TRCRFCHA, t.TRCRNMRO
  FROM CRD.TRCR t WHERE t.CRARCDGO = 510 ORDER BY t.TRCRCDGO;

-- 2. Lo que la carga trae, por partícipe y producto (lo que armó el lector de ARCH)
SELECT p.PXCACDPT AS ROL, p.PXCANMBR, d.DTCACDPP AS PRODUCTO, p.PXCACDGO, p.PXCADSCT AS MONTO, p.PXCAESTD
  FROM CRD.PXCA p JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 510
 ORDER BY p.PXCACDPT, d.DTCACDPP;

SELECT d.DTCACDPP AS PRODUCTO, d.DTCATTDS, d.DTCATTDO, COUNT(p.PXCACDGO) AS FILAS, SUM(p.PXCADSCT) AS SUMA_FILAS
  FROM CRD.DTCA d LEFT JOIN CRD.PXCA p ON p.DTCACDGO = d.DTCACDGO
 WHERE d.CRARCDGO = 510
 GROUP BY d.DTCACDPP, d.DTCATTDS, d.DTCATTDO;

-- 3. Lo que se mandó a ARCH en octubre (generación 173), por partícipe y producto
SELECT e.ENTDNMID AS CEDULA, e.ENTDRLPC AS ROL, e.ENTDRZNS, x.PDGACDPT AS PRODUCTO, x.PRSTCDGO,
       x.PDGAMNEN AS ENVIADO
  FROM CRD.PDGA x
  JOIN CRD.DTGA d ON d.DTGACDGO = x.DTGACDGO
  JOIN CRD.ENTD e ON e.ENTDCDGO = x.ENTDCDGO
 WHERE d.GNAPCDGO = 173
 ORDER BY e.ENTDNMID, x.PDGACDPT;

-- 4. Novedades y afectaciones manuales de la carga 510
SELECT p.PXCACDPT AS ROL, d.DTCACDPP AS PRODUCTO, n.NVPCCDGO, n.NVPCTPNV, n.NVPCMNES, n.NVPCMNRC,
       n.NVPCMNDF, n.NVPCESTD, SUBSTR(n.NVPCDSCR, 1, 200) AS DESCRIPCION
  FROM CRD.NVPC n
  JOIN CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 510
 ORDER BY p.PXCACDPT;

SELECT p.PXCACDPT AS ROL, f.AVPCCDGO, f.NVPCCDGO, f.PRSTCDGO, f.DTPRCDGO, f.TPAPCDGO, f.AVPCVAFA, f.AVPCESTD
  FROM CRD.AVPC f
  JOIN CRD.NVPC n ON n.NVPCCDGO = f.NVPCCDGO
  JOIN CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
  JOIN CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
 WHERE d.CRARCDGO = 510
 ORDER BY p.PXCACDPT, f.AVPCCDGO;

-- 5. Aporte mensual esperado (vigencias activas) de los partícipes de ARCH
SELECT e.ENTDRLPC AS ROL, e.ENTDRZNS, v.TPAPCDGO, v.VGCNFCIN, v.VGCNFCFN, v.VGCNMNTO
  FROM CRD.VGCN v
  JOIN CRD.CNTR k ON k.CNTRCDGO = v.CNTRCDGO AND k.CNTRESTD = 1
  JOIN CRD.ENTD e ON e.ENTDCDGO = k.ENTDCDGO
 WHERE e.FLLLCDGO = 2 AND v.VGCNIDST = 1
 ORDER BY e.ENTDRLPC, v.TPAPCDGO;
