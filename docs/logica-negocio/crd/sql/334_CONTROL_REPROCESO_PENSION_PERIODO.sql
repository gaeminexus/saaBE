-- 334 — Control de POST /pgpc/reprocesarPeriodo (C20, pensión complementaria de un mes pasado, 2026-10-28)
-- Doc: docs/logica-negocio/crd/API-REPROCESO-PENSION-PERIODO.md
-- Solo SELECT. Cambiar el período (2025 / 6, '6/2025', fechas) según el mes re-procesado.
-- El cruce rol vs SAA por jubilado con los valores del rol va en el expediente (cuadreSuper), no aquí:
-- 03-trabajo/meses/2025-06/sql/14b-control-reproceso-pension-junio.sql.

-- 1) Por jubilado: PGPC del período, neto (negativo de pensión), seguro, cruce con préstamos, devengo y pago.
--    Identidad que tiene que cerrar: neto + seguro + cruzado + sin_cruzar = pensión (PGPCVLRR); sin_cruzar es lo
--    que el rol descontó y no se pudo cruzar (queda en el aporte 23) y sale en "novedades" de la respuesta.
SELECT g.ENTDCDGO, g.PGPCCDGO, g.PGPCVLRR pension, g.PGPCVLSG seguro, g.PGPCESTD estado,
       (SELECT -SUM(a.APRTVLRR) FROM CRD.APRT a WHERE a.ENTDCDGO = g.ENTDCDGO AND a.TPAPCDGO = 23
           AND a.APRTGLSA = 'PAGO PENSION COMPLEMENTARIA 6/2025 - Entidad ' || g.ENTDCDGO || ' (REPROCESO C20)') neto,
       (SELECT -SUM(a.APRTVLRR) FROM CRD.APRT a WHERE a.ENTDCDGO = g.ENTDCDGO AND a.TPAPCDGO = 23
           AND a.APRTGLSA = 'PAGO PENSION COMPLEMENTARIA 6/2025 - Entidad ' || g.ENTDCDGO || ' (REPROCESO C20) - SEGURO MEDICO') seguro_aprt,
       (SELECT ROUND(SUM(e.EVPRVLRR), 2) FROM CRD.EVPR e JOIN CRD.PRST p ON p.PRSTCDGO = e.PRSTCDGO
           WHERE p.ENTDCDGO = g.ENTDCDGO AND e.EVPRFCAN IS NULL
             AND e.EVPROBSR LIKE 'Rol pensi%n complementaria 6/2025 (re-proceso C20)%') cruzado,
       g.PGPCNMDV asiento_devengo, g.PGPCIDPG orden_pago, g.PGPCNMAS asiento_pago, TO_CHAR(g.PGPCFCPG, 'YYYY-MM-DD') fecha_pago
FROM CRD.PGPC g
WHERE g.PGPCANNO = 2025 AND g.PGPCMESS = 6
ORDER BY g.ENTDCDGO;

-- 2) Totales del período. Para junio 2025 (lote 14): 208 PGPC, pensión 146.217,18, seguro 450,40,
--    neto 134.517,50 (los netos positivos del rol; 6572 y 6674 quedan en 0).
--    Estados de PGPC: 1 REGISTRADA (neto > 0 sin orden todavía: el paso del pago falló), 2 EN_PAGO (orden
--    pendiente), 3 PAGADA (orden confirmada, o neto 0).
SELECT t.*,
       (SELECT -ROUND(SUM(a.APRTVLRR), 2) FROM CRD.APRT a WHERE a.TPAPCDGO = 23
           AND a.APRTGLSA LIKE 'PAGO PENSION COMPLEMENTARIA 6/2025 - Entidad % (REPROCESO C20)') neto,
       (SELECT ROUND(SUM(e.EVPRVLRR), 2) FROM CRD.EVPR e WHERE e.EVPRFCAN IS NULL
           AND e.EVPROBSR LIKE 'Rol pensi%n complementaria 6/2025 (re-proceso C20)%') cruzado
FROM (SELECT COUNT(*) pgpc, ROUND(SUM(g.PGPCVLRR), 2) pension, ROUND(SUM(g.PGPCVLSG), 2) seguro,
             SUM(CASE WHEN g.PGPCESTD = 3 THEN 1 ELSE 0 END) pagadas, SUM(CASE WHEN g.PGPCESTD = 2 THEN 1 ELSE 0 END) en_pago,
             SUM(CASE WHEN g.PGPCESTD = 1 THEN 1 ELSE 0 END) registradas
      FROM CRD.PGPC g
      WHERE g.PGPCANNO = 2025 AND g.PGPCMESS = 6) t;

-- 3) Asiento de DEVENGO (plantilla 35), por cuenta. Esperado al 30-jun-2025: D 2.1.02.25.01 = neto + seguro;
--    H 2.3.01.10.03 = neto; H 2.3.90.90.06 = seguro. Una sola fecha: el fin del período.
SELECT d.DTASCNTA cuenta, ROUND(SUM(d.DTASDBEE), 2) debe, ROUND(SUM(d.DTASHBRR), 2) haber,
       COUNT(DISTINCT a.ASNTCDGO) asientos, TO_CHAR(MIN(a.ASNTFCHA), 'YYYY-MM-DD') desde, TO_CHAR(MAX(a.ASNTFCHA), 'YYYY-MM-DD') hasta
FROM CRD.PGPC g
JOIN CNT.ASNT a ON a.ASNTCDGO = g.PGPCNMDV
JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE g.PGPCANNO = 2025 AND g.PGPCMESS = 6 AND a.ASNTESTD <> 2
GROUP BY d.DTASCNTA
ORDER BY d.DTASCNTA;

-- 4) Asientos del CRUCE (pago con aportes de pensión contra préstamos, plantilla 21), por cuenta. Esperado:
--    D cuenta individual de pensión (aporte 23) = cruzado / H cartera (capital, interés, mora, seguros).
SELECT d.DTASCNTA cuenta, ROUND(SUM(d.DTASDBEE), 2) debe, ROUND(SUM(d.DTASHBRR), 2) haber,
       COUNT(DISTINCT a.ASNTCDGO) asientos, TO_CHAR(MIN(a.ASNTFCHA), 'YYYY-MM-DD') desde, TO_CHAR(MAX(a.ASNTFCHA), 'YYYY-MM-DD') hasta
FROM CRD.EVPR e
JOIN CNT.ASNT a ON a.ASNTCDGO = e.EVPRNMAS
JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE e.EVPRFCAN IS NULL AND e.EVPROBSR LIKE 'Rol pensi%n complementaria 6/2025 (re-proceso C20)%' AND a.ASNTESTD <> 2
GROUP BY d.DTASCNTA
ORDER BY d.DTASCNTA;

-- 5) PAGO: órdenes de CxP de los PGPC del período, por estado, cuenta de origen, fecha del banco y referencia.
--    Junio 2025: CONFIRMADAS 185 x 418 al 2025-09-29 (Ru-858739) 118.183,81 y 2 x 417 al 2025-10-22
--    (TW 9005701 1.476,10; CHQ 8043 58,60); 19 pendientes de aprobación 14.798,99.
SELECT t.PGTRESTD estado, t.PGTRCNBC cuenta, TO_CHAR(t.PGTRFRSP, 'YYYY-MM-DD') fecha_banco, t.PGTRRFBN referencia,
       COUNT(*) ordenes, ROUND(SUM(t.PGTRVLOR), 2) valor, COUNT(t.PGTRASNT) con_asiento
FROM PGS.PGTR t
WHERE t.PGTRORGN = 'CRD_PENSION_COMPLEMENTARIA'
  AND t.PGTRIDOR IN (SELECT g.PGPCCDGO FROM CRD.PGPC g WHERE g.PGPCANNO = 2025 AND g.PGPCMESS = 6)
GROUP BY t.PGTRESTD, t.PGTRCNBC, TO_CHAR(t.PGTRFRSP, 'YYYY-MM-DD'), t.PGTRRFBN
ORDER BY 1, 2, 3;

-- 6) Asiento del PAGO (CxP), por cuenta y fecha. Esperado: D 2.3.01.10.03 / H banco, en la fecha del banco.
SELECT TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') fecha, d.DTASCNTA cuenta, ROUND(SUM(d.DTASDBEE), 2) debe,
       ROUND(SUM(d.DTASHBRR), 2) haber, COUNT(DISTINCT a.ASNTCDGO) asientos
FROM PGS.PGTR t
JOIN CNT.ASNT a ON a.ASNTCDGO = t.PGTRASNT
JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE t.PGTRORGN = 'CRD_PENSION_COMPLEMENTARIA'
  AND t.PGTRIDOR IN (SELECT g.PGPCCDGO FROM CRD.PGPC g WHERE g.PGPCANNO = 2025 AND g.PGPCMESS = 6)
  AND a.ASNTESTD <> 2
GROUP BY TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD'), d.DTASCNTA
ORDER BY 1, 2;

-- 7) Préstamos reabiertos por D-019 durante el cruce (hoy estaban cancelados).
SELECT p.PRSTCDGO, p.ENTDCDGO, p.PRSTIDST estado_hoy,
       REGEXP_SUBSTR(p.PRSTOBSR, 'REPROCESO PGPC 2025-06 entidad [0-9]+[^|]*') marca
FROM CRD.PRST p
WHERE p.PRSTOBSR LIKE '%REPROCESO PGPC 2025-06%'
ORDER BY p.PRSTCDGO;
