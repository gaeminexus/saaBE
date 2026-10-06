-- =====================================================================================
-- e2-90 — BLUEBODY CIA. LTDA.: el anticipo de 1.380 no aparece en su estado de cuenta. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-06.
--
-- Hipótesis (por la pantalla): se registró como INGRESO DE TESORERÍA (TSR.INGR, asiento T-I-2026-10-0004,
-- «ANTICIPO DE FACTURA POR EL MES DE SEPTIEMBRE», Ref 488079, Debe Banco Pacífico / Haber 2.9.01.10
-- ARRIENDOS RECIBIDOS POR ANTICIPADO) y NO como ANTICIPO DE CLIENTE (CBR.ANTC). El estado de cuenta lee los
-- anticipos de CBR.ANTC (estado-cuenta-titular.service.ts, «Anticipos de cliente», /antc). Un ingreso de
-- tesorería no crea fila en ANTC: el dinero está en el banco y en la 2.9.01.10, pero no figura como saldo a
-- favor del cliente ni se puede cruzar con la factura 001-001-000000835.
-- Columnas copiadas de Ingreso (INGR), AnticipoCliente (ANTC), Asiento, PersonaRol (PRRL),
-- PersonaCuentaContable (PRCC), PlanCuenta (PLNN) y Titular (TTLR).
-- =====================================================================================

-- BLOQUE 1 — El ingreso de tesorería del asiento T-I-2026-10-0004. ESPERADO: 1 fila, 1.380, con titular
-- BLUEBODY (o nulo, si no se eligió beneficiario).
SELECT 'BLOQUE 1 - ingreso' AS bloque, i.INGRCDGO, i.INGRESTD, i.INGRVLOR, i.INGRFCHA, i.INGRREFR,
       i.INGRTTLR, NVL(t.TTLRRZSC, t.TTLRNMBR) AS titular, i.INGRPRDC, i.INGRDSCR, a.ASNTCDGO, a.ASNTNMAL
  FROM TSR.INGR i
  JOIN CNT.ASNT a ON a.ASNTCDGO = i.INGRASNT
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = i.INGRTTLR
 WHERE a.ASNTNMAL = 'T-I-2026-10-0004';

-- BLOQUE 2 — Los anticipos de cliente (CBR.ANTC) de BLUEBODY. ESPERADO según la hipótesis: ninguno del
-- 1.380 de octubre.
SELECT 'BLOQUE 2 - anticipos de cliente' AS bloque, x.ID, x.ESTADO, x.FECHAANTICIPO, x.NUMERODOC, x.VALOR,
       x.ANTCSALD, x.ANTCREFR, x.ASIENTO
  FROM CBR.ANTC x
  JOIN TSR.TTLR t ON t.TTLRCDGO = x.TITULAR
 WHERE t.TTLRIDNT = '0993120561001'
 ORDER BY x.FECHAANTICIPO;

-- BLOQUE 3 — La cuenta de anticipos del cliente (PRCC tipo 2, rol cliente) y su saldo global. Define si el
-- arreglo necesita un asiento de reclasificación: si la cuenta del cliente ES la 2.9.01.10, no; si es otra,
-- sí (Debe 2.9.01.10 / Haber la cuenta del cliente).
SELECT 'BLOQUE 3 - cuenta de anticipos del cliente' AS bloque, c.PRCCCDGO, r.PRRLRZZA AS rol, c.PRCCTPOO AS tipo,
       p.PLNNCNTA, p.PLNNNMBR, c.PRCCSLIN AS saldo_global, c.PJRQCDGO AS empresa
  FROM TSR.PRCC c
  JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE t.TTLRIDNT = '0993120561001'
 ORDER BY r.PRRLRZZA, c.PRCCTPOO;

-- BLOQUE 4 — ¿Es la primera vez? Otros ingresos de tesorería con «ANTICIPO» en el concepto, para saber si el
-- mismo error se repitió con otros clientes.
SELECT 'BLOQUE 4 - otros ingresos con ANTICIPO' AS bloque, i.INGRCDGO, i.INGRESTD, TRUNC(i.INGRFCHA) AS fecha,
       i.INGRVLOR, NVL(t.TTLRRZSC, t.TTLRNMBR) AS titular, a.ASNTNMAL, SUBSTR(i.INGRDSCR, 1, 80) AS concepto
  FROM TSR.INGR i
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = i.INGRASNT
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = i.INGRTTLR
 WHERE UPPER(i.INGRDSCR) LIKE '%ANTICIPO%'
 ORDER BY i.INGRFCHA;
