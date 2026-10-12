-- =====================================================================================
-- e2-96 — Anticipo y garantía registrados al cliente equivocado. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-12.
--
-- Pedido de contabilidad (correo de Ana Lalangui, 2026-10-08, «Su ayuda corrigiendo un cliente»):
--   - El 08/09/2026 se registró un ANTICIPO POR ARRIENDO al cliente RUC 1792458560001, y en AGOSTO una
--     GARANTÍA al mismo cliente.
--   - El 06/10/2026 se emitió la FACTURA al cliente RUC 1793221709001.
--   - Los nombres son casi iguales y los RUC distintos: el anticipo y la garantía eran del 1793221709001.
--   - Pide dejar el saldo a favor en el 1793221709001 y eliminar el 1792458560001.
--
-- Este script NO corrige nada. Mide qué cuelga de cada titular para decidir cómo moverlo:
--   - si el anticipo vive en CBR.ANTC (anticipo de cliente) o en TSR.INGR (ingreso de tesorería, como
--     pasó con BLUEBODY en el e2-90);
--   - si sus asientos usan una cuenta contable PROPIA del cliente (PRCC) — en ese caso mover el titular
--     no alcanza y hace falta reclasificar en contabilidad;
--   - qué otras tablas apuntan al titular equivocado, porque NO se puede borrar mientras alguna lo haga.
-- Columnas copiadas de Titular (TTLR), AnticipoCliente (ANTC), Ingreso (INGR), Factura (FCTR),
-- AplicacionPagoCxc (APLC), PersonaRol (PRRL), PersonaCuentaContable (PRCC), DocumentoCobro (DCMC),
-- Cobro (CBRO), Asiento (ASNT), DetalleAsiento (DTAS) y PlanCuenta (PLNN).
-- =====================================================================================

-- BLOQUE 0 — Los dos titulares. ESPERADO: 2 filas, una por RUC. Si sale MÁS de una por RUC, hay además un
-- duplicado (§25 del estado: el RUC con espacio crea titulares duplicados) y hay que avisar antes de seguir.
SELECT 'BLOQUE 0 - titulares' AS bloque, t.TTLRCDGO, '[' || t.TTLRIDNT || ']' AS identificacion,
       t.TTLRRZSC, t.TTLRNMBR, t.TTLRCLNT, t.TTLRPRVD, t.TTLRESTD
  FROM TSR.TTLR t
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
 ORDER BY t.TTLRIDNT, t.TTLRCDGO;

-- BLOQUE 1 — Anticipos de cliente (CBR.ANTC) de los dos. ESPERADO: el anticipo del 08/09 y,
-- posiblemente, la garantía de agosto, en el 1792458560001.
SELECT 'BLOQUE 1 - anticipos de cliente' AS bloque, TRIM(t.TTLRIDNT) AS ruc, x.ID, x.ESTADO,
       x.FECHAANTICIPO, x.NUMERODOC, x.VALOR, x.ANTCSALD, x.ANTCREFR, x.ANTCAPLC,
       a.ASNTCDGO, a.ASNTNMAL, a.ASNTESTD, x.OBSERVACION
  FROM CBR.ANTC x
  JOIN TSR.TTLR t ON t.TTLRCDGO = x.TITULAR
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = x.ASIENTO
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
 ORDER BY t.TTLRIDNT, x.FECHAANTICIPO;

-- BLOQUE 2 — Ingresos de tesorería (TSR.INGR) de los dos. ESPERADO: si el anticipo o la garantía no
-- salieron en el BLOQUE 1, tienen que estar acá.
SELECT 'BLOQUE 2 - ingresos de tesoreria' AS bloque, TRIM(t.TTLRIDNT) AS ruc, i.INGRCDGO, i.INGRESTD,
       i.INGRFCHA, i.INGRVLOR, i.INGRREFR, i.INGRDSCR, a.ASNTCDGO, a.ASNTNMAL, a.ASNTESTD
  FROM TSR.INGR i
  JOIN TSR.TTLR t ON t.TTLRCDGO = i.INGRTTLR
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = i.INGRASNT
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
 ORDER BY t.TTLRIDNT, i.INGRFCHA;

-- BLOQUE 3 — Facturas de venta (CBR.FCTR) de los dos. ESPERADO: la del 06/10 en el 1793221709001 y
-- NINGUNA en el 1792458560001 (si tiene alguna, el titular no se puede eliminar).
SELECT 'BLOQUE 3 - facturas de venta' AS bloque, TRIM(t.TTLRIDNT) AS ruc, f.ID, f.NUMERO, f.FECHA,
       f.TOTAL, f.ESTADO, f.ESTADOEMISION
  FROM CBR.FCTR f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
 ORDER BY t.TTLRIDNT, f.FECHA;

-- BLOQUE 4 — Aplicaciones (CBR.APLC) sobre esas facturas o esos anticipos. ESPERADO: ninguna que cruce
-- un anticipo del 1792458560001 contra una factura del 1793221709001 (no se puede, son titulares distintos).
SELECT 'BLOQUE 4 - aplicaciones' AS bloque, ap.APLCCDGO, ap.APLCESTD, ap.APLCTDPG, ap.APLCMAPL, ap.APLCFAPL,
       ap.APLCFCTR, ap.APLCANTC, ap.APLCOBSR
  FROM CBR.APLC ap
 WHERE ap.APLCFCTR IN (SELECT f.ID FROM CBR.FCTR f JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
                        WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001'))
    OR ap.APLCANTC IN (SELECT x.ID FROM CBR.ANTC x JOIN TSR.TTLR t ON t.TTLRCDGO = x.TITULAR
                        WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001'))
 ORDER BY ap.APLCCDGO;

-- BLOQUE 5 — Roles y cuentas contables por titular (PRRL / PRCC). ESPERADO: decide si la contabilidad
-- también se mueve. Si cada uno tiene su PROPIA cuenta de anticipos (PRCCTPOO de anticipo con un PLNNCDGO
-- distinto), los asientos del BLOQUE 6 cargaron la cuenta del titular equivocado.
SELECT 'BLOQUE 5 - roles y cuentas' AS bloque, TRIM(t.TTLRIDNT) AS ruc, r.PRRLCDGO, r.PRRLESTD,
       c.PRCCCDGO, c.PRCCTPOO, c.PRCCCLPR, c.PRCCSLIN, p.PLNNCNTA, p.PLNNNMBR
  FROM TSR.PRRL r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
  LEFT JOIN TSR.PRCC c ON c.PRRLCDGO = r.PRRLCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
 ORDER BY t.TTLRIDNT, c.PRCCTPOO;

-- BLOQUE 6 — Las líneas de los asientos de los BLOQUES 1 y 2. ESPERADO: ver qué cuentas tocaron. Si alguna
-- es la cuenta propia del 1792458560001 (BLOQUE 5), hay que reclasificar; si todas son cuentas genéricas
-- (banco, anticipos recibidos, garantías), el movimiento es solo de auxiliar y la contabilidad no cambia.
SELECT 'BLOQUE 6 - lineas de asiento' AS bloque, a.ASNTNMAL, a.ASNTFCHA, a.ASNTESTD, d.DTASCDGO,
       p.PLNNCNTA, p.PLNNNMBR, d.DTASDBEE, d.DTASHBRR, d.DTASDSCR
  FROM CNT.DTAS d
  JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  LEFT JOIN CNT.PLNN p ON p.PLNNCDGO = d.PLNNCDGO
 WHERE a.ASNTCDGO IN (
        SELECT x.ASIENTO FROM CBR.ANTC x JOIN TSR.TTLR t ON t.TTLRCDGO = x.TITULAR
         WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001')
        UNION
        SELECT i.INGRASNT FROM TSR.INGR i JOIN TSR.TTLR t ON t.TTLRCDGO = i.INGRTTLR
         WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001'))
 ORDER BY a.ASNTFCHA, a.ASNTNMAL, d.DTASCDGO;

-- BLOQUE 7 — Otros documentos que también apuntan al titular (cobros y documentos de cobro). ESPERADO:
-- conteos; todo lo que no sea 0 en el 1792458560001 impide eliminarlo.
SELECT 'BLOQUE 7 - otras referencias' AS bloque, 'CBR.DCMC' AS tabla, TRIM(t.TTLRIDNT) AS ruc, COUNT(*) AS filas
  FROM CBR.DCMC d JOIN TSR.TTLR t ON t.TTLRCDGO = d.PRSNCDGO
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001') GROUP BY TRIM(t.TTLRIDNT)
UNION ALL
SELECT 'BLOQUE 7 - otras referencias', 'TSR.CBRO', TRIM(t.TTLRIDNT), COUNT(*)
  FROM TSR.CBRO c JOIN TSR.TTLR t ON t.TTLRCDGO = c.PRSNCDGO
 WHERE TRIM(t.TTLRIDNT) IN ('1792458560001', '1793221709001') GROUP BY TRIM(t.TTLRIDNT);

-- BLOQUE 8 — Todas las tablas con FK hacia TSR.TTLR, para no olvidar ninguna referencia al eliminar.
-- ESPERADO: la lista de tablas/columnas. Las que no estén en los bloques 1 a 7 se revisan antes de borrar.
SELECT 'BLOQUE 8 - FKs hacia TTLR' AS bloque, c.OWNER, c.TABLE_NAME, cc.COLUMN_NAME, c.CONSTRAINT_NAME
  FROM ALL_CONSTRAINTS c
  JOIN ALL_CONS_COLUMNS cc ON cc.OWNER = c.OWNER AND cc.CONSTRAINT_NAME = c.CONSTRAINT_NAME
  JOIN ALL_CONSTRAINTS r ON r.OWNER = c.R_OWNER AND r.CONSTRAINT_NAME = c.R_CONSTRAINT_NAME
 WHERE c.CONSTRAINT_TYPE = 'R' AND r.OWNER = 'TSR' AND r.TABLE_NAME = 'TTLR'
 ORDER BY c.OWNER, c.TABLE_NAME;
