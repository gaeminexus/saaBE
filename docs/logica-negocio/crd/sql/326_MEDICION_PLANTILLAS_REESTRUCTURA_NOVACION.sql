-- =====================================================================================
-- 326 — MEDICION: plantillas de reestructuracion/novacion y bandas de los productos destino
-- Equipo omen-saa-1 · 2026-10-13 · SOLO LECTURA (ningun bloque escribe)
--
-- Por que: DISENO-REESTRUCTURACION-NOVACION.md §4. El asiento de la reestructuracion y de
-- la novacion toma de la plantilla las lineas que NO son de banda (interes, mora, seguros,
-- cuentas de orden, socios por pagar), y de CRD.BNDP las bandas. El levantamiento solo
-- resume las plantillas; para programar hace falta saber QUE aux1 tiene cada linea.
--
-- Correr de corrido y devolver al arbitro la salida de los 3 bloques.
-- =====================================================================================

-- BLOQUE 1 — Las 10 plantillas, linea por linea.
SELECT p.PLNSCDAL   AS ALTERNO,
       p.PLNSNMBR   AS PLANTILLA,
       p.PJRQCDGO   AS EMPRESA,
       p.PLNSESTD   AS ESTADO_PLANTILLA,
       d.DTPLAXL1   AS AUX1,
       d.DTPLAXL2   AS AUX2,
       DECODE(d.DTPLMVMN, 1, 'DEBE', 2, 'HABER', d.DTPLMVMN) AS MOVIMIENTO,
       c.PLNNCNTA   AS CUENTA,
       c.PLNNNMBR   AS NOMBRE_CUENTA,
       d.DTPLDSCR   AS DESCRIPCION,
       d.DTPLESTD   AS ESTADO_LINEA
  FROM CNT.PLNS p
  JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL IN (2, 3, 4, 5, 6, 7, 10, 11, 14, 15)
 ORDER BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLMVMN;

-- BLOQUE 2 — Productos RESTRUCTURADO / NOVACION: tipo, y si tienen bandas configuradas
--   vigentes con cuenta (sin bandas, el asiento no se puede armar).
SELECT pr.PRDCCDGO          AS ID_PRODUCTO,
       pr.PRDCNMBR          AS PRODUCTO,
       t.TPPRNMBR           AS FAMILIA,
       cb.CBPRCDGO          AS ID_CONFIG_BANDAS,
       cb.CBPRTPCR          AS TIPO_CARTERA,
       cb.CBPRFCIN          AS VIGENTE_DESDE,
       cb.CBPRFCFN          AS VIGENTE_HASTA,
       b.BNDPNMRO           AS BANDA,
       b.BNDPCNTD           AS DIAS_HASTA,
       c.PLNNCNTA           AS CUENTA_BANDA
  FROM CRD.PRDC pr
  LEFT JOIN CRD.TPPR t  ON t.TPPRCDGO = pr.TPPRCDGO
  LEFT JOIN CRD.CBPR cb ON cb.PRDCCDGO = pr.PRDCCDGO
  LEFT JOIN CRD.BNDP b  ON b.CBPRCDGO = cb.CBPRCDGO
  LEFT JOIN CNT.PLNN c  ON c.PLNNCDGO = b.PLNNCDGO
 WHERE UPPER(pr.PRDCNMBR) LIKE '%RESTRUCT%' OR UPPER(pr.PRDCNMBR) LIKE '%NOVACI%'
 ORDER BY pr.PRDCCDGO, cb.CBPRTPCR, b.BNDPNMRO;

-- BLOQUE 3 — Los productos de origen mas comunes (con cartera viva), con su familia.
--   Para confirmar que cada uno tiene su RESTRUCTURADO/NOVACION de destino.
SELECT pr.PRDCCDGO AS ID_PRODUCTO, pr.PRDCNMBR AS PRODUCTO, t.TPPRNMBR AS FAMILIA,
       (SELECT COUNT(*) FROM CRD.PRST x WHERE x.PRDCCDGO = pr.PRDCCDGO AND x.PRSTIDST IN (2, 8, 11)) AS VIVOS
  FROM CRD.PRDC pr
  LEFT JOIN CRD.TPPR t ON t.TPPRCDGO = pr.TPPRCDGO
 ORDER BY VIVOS DESC, pr.PRDCCDGO;
