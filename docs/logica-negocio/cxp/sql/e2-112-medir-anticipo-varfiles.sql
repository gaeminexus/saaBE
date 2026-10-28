-- =====================================================================================
-- e2-112 — VARFILES - TECNICAS & ARCHIVOS S.A. (RUC 1793168450001, titular 86): Titulares muestra
--          5.000,00 de anticipos, pero el estado de cuenta no muestra ningún anticipo. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-28. Pasar la salida completa al árbitro.
--
-- Por qué pueden diferir: Titulares lee TSR.PRCC.PRCCSLIN de la cuenta tipo 2 (saldo global de
-- anticipos). El estado de cuenta NO lee ese número: lista las filas de PGS.ANTP del titular y oculta
-- las de estado 3 (anulado) y 4 (migrado). Si el 5.000 se cargó a mano como saldo inicial y nunca tuvo
-- su fila en ANTP (la migración del 2026-08-20 creó 'SALDO INICIAL MIGRADO' solo para los que ya
-- existían), el estado de cuenta no lo ve y el pago de facturas tampoco puede cruzarlo, porque el
-- cruce aplica anticipos concretos de ANTP.
-- Columnas copiadas de Titular (TSR.TTLR), PersonaRol (TSR.PRRL), PersonaCuentaContable (TSR.PRCC),
-- PlanCuenta (CNT.PLNN), AnticipoProveedor (PGS.ANTP), AplicacionPagoCxp (PGS.APLP),
-- DevolucionAnticipoProveedor/DetalleDevolucionAnticipo (PGS.DVPR/DDPR), FacturaCompra (PGS.FCTC).
-- Sin INSERT, sin UPDATE.
-- =====================================================================================

-- BLOQUE 1 — Titular y sus cuentas contables por rol. ESPERADO: la fila tipo 2 de rol proveedor (2)
-- con PRCCSLIN = 5000 y la cuenta 1.9.01.10.
SELECT 'BLOQUE 1 - cuentas' AS bloque, t.TTLRCDGO, t.TTLRIDNT, t.TTLRRZSC, r.PRRLCDGO, r.PRRLRZZA AS rol,
       c.PRCCCDGO, c.PJRQCDGO AS empresa, c.PRCCTPOO AS tipo_cuenta, c.PRCCSLIN AS saldo_inicial,
       p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.TTLR t
  JOIN TSR.PRRL r ON r.PRSNCDGO = t.TTLRCDGO
  LEFT JOIN TSR.PRCC c ON c.PRRLCDGO = r.PRRLCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE t.TTLRIDNT = '1793168450001'
 ORDER BY r.PRRLRZZA, c.PRCCTPOO;

-- BLOQUE 2 — Todos sus anticipos, en cualquier estado (1 ingresado, 2 confirmado, 3 anulado, 4 migrado).
SELECT 'BLOQUE 2 - anticipos' AS bloque, a.ANTPCDGO, a.ANTPPJRQ AS empresa, a.ANTPFANT, a.ANTPNDOC,
       a.ANTPVLOR, a.ANTPSALD, a.ANTPESTD, a.ANTPASNT, a.ANTPFCRG, SUBSTR(a.ANTPOBSR, 1, 200) AS observacion
  FROM PGS.ANTP a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE t.TTLRIDNT = '1793168450001'
 ORDER BY a.ANTPFANT, a.ANTPCDGO;

-- BLOQUE 3 — Cruces de esos anticipos (APLP tipo 4) y devoluciones (DDPR), activos o no.
SELECT 'BLOQUE 3a - cruces' AS bloque, ap.APLPCDGO, ap.APLPANTP, ap.APLPFCTC, ap.APLPMAPL, ap.APLPESTD,
       ap.APLPFAPL, ap.APLPASNT
  FROM PGS.APLP ap
  JOIN PGS.ANTP a ON a.ANTPCDGO = ap.APLPANTP
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE t.TTLRIDNT = '1793168450001'
 ORDER BY ap.APLPCDGO;
SELECT 'BLOQUE 3b - devoluciones' AS bloque, d.DVPRCDGO, d.DVPRFCHA, d.DVPRVLOR, d.DVPRESTD,
       dd.DDPRCDGO, dd.ANTPCDGO, dd.DDPRVLOR
  FROM PGS.DVPR d
  JOIN TSR.TTLR t ON t.TTLRCDGO = d.DVPRTTLR
  LEFT JOIN PGS.DDPR dd ON dd.DVPRCDGO = d.DVPRCDGO
 WHERE t.TTLRIDNT = '1793168450001'
 ORDER BY d.DVPRCDGO;

-- BLOQUE 4 — Sus facturas de compra con lo aplicado activo (las que se le van a pagar).
SELECT 'BLOQUE 4 - facturas' AS bloque, f.ID, f.NUMERO, f.FECHA, f.TOTAL, f.ESTADO,
       (SELECT NVL(SUM(x.APLPMAPL), 0) FROM PGS.APLP x WHERE x.APLPFCTC = f.ID AND x.APLPESTD = 1) AS aplicado,
       f.TOTAL - (SELECT NVL(SUM(x.APLPMAPL), 0) FROM PGS.APLP x WHERE x.APLPFCTC = f.ID AND x.APLPESTD = 1) AS saldo
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE t.TTLRIDNT = '1793168450001'
 ORDER BY f.FECHA, f.ID;
