-- =====================================================================================
-- 350 — SOLO LECTURA. omen-saa-1, 2026-11-01. Insumo para decidir la corrección de los estados 12 y 13
-- en el motor de saldos (PrestamoDaoServiceImpl.selectVigentesByEntidad y
-- PrestamoServiceImpl.calcularSaldosEnLote excluyen solo los estados 3, 4 y 5; no conocen el
-- 12 ANULADO ni el 13 CANCELADO_POR_REESTRUCTURACION). Efecto posible: un préstamo anulado o
-- reestructurado cuenta como deuda vigente (devolución de aportes, /movil).
-- =====================================================================================

-- 1. Cuántos préstamos hay en 12 y 13, y cuántos tienen cuotas sin pagar (las que el motor sumaría)
SELECT p.PRSTIDST AS ESTADO, COUNT(*) AS PRESTAMOS,
       SUM(CASE WHEN EXISTS (SELECT 1 FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO
                              AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))) THEN 1 ELSE 0 END) AS CON_CUOTAS_PENDIENTES
  FROM CRD.PRST p
 WHERE p.PRSTIDST IN (12, 13)
 GROUP BY p.PRSTIDST;

-- 2. Detalle: préstamo, entidad, estado y saldo de capital pendiente según sus cuotas
SELECT p.PRSTCDGO, p.ENTDCDGO, e.ENTDRZNS, p.PRSTIDST AS ESTADO,
       (SELECT NVL(SUM(d.DTPRCPTL), 0) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO
         AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))) AS CAPITAL_CUOTAS_PENDIENTES
  FROM CRD.PRST p
  JOIN CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
 WHERE p.PRSTIDST IN (12, 13)
 ORDER BY p.PRSTIDST, p.PRSTCDGO;

-- 3. Reestructurados (13) cuyo partícipe tiene además un préstamo vigente (riesgo de contar dos veces)
SELECT p.PRSTCDGO AS PRESTAMO_13, p.ENTDCDGO,
       (SELECT LISTAGG(v.PRSTCDGO, ',') WITHIN GROUP (ORDER BY v.PRSTCDGO) FROM CRD.PRST v
         WHERE v.ENTDCDGO = p.ENTDCDGO AND v.PRSTIDST IN (2, 11) ) AS VIGENTES_DEL_PARTICIPE
  FROM CRD.PRST p
 WHERE p.PRSTIDST = 13;
