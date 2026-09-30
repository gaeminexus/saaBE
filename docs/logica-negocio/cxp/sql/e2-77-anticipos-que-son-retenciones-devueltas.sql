-- =====================================================================================
-- e2-77 — De dónde salen los anticipos que igualan a una retención, en dos proveedores que ya
--         devolvieron ese dinero con un depósito. SOLO LECTURA. Equipo omen-saa-2, 2026-09-30.
--
-- Casos que reportó el usuario, con capturas del Estado de Cuenta del proveedor:
--   SISTEMAS CONTABLES JLCONTROL S.A.  (1391904274001): saldo a favor 3,05  = retención 001-001-000007294
--   EXPOAUTOPARTS CIA. LTDA.           (1791242963001): saldo a favor 23,94 = retenciones 7366 (15,30) + 7326 (8,64)
--
-- En el código NO hay nada que cree un AnticipoProveedor automáticamente desde un sobrepago o una
-- retención: los únicos «new AnticipoProveedor()» son el registro manual de anticipos
-- (AnticipoProveedorServiceImpl:108 y :284). Estos bloques dicen cómo nacieron estos.
-- Nombres de columna copiados de AnticipoProveedor, AplicacionPagoCxp, PagoProgramado y RetencionV2.
-- =====================================================================================

-- BLOQUE 1 — Los anticipos de los dos proveedores, todos sus estados
-- Mirar: ANTPESTD (1 ingresado, 2 confirmado, 3 anulado, 4 migrado), ANTPNDOC/ANTPOBSR (¿dicen
-- «retención»?), ANTPASNT (¿tiene asiento?) y ANTPFCRG (¿cuándo se creó?).
SELECT 'BLOQUE 1 - anticipos' AS bloque,
       t.TTLRIDNT, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor,
       a.ANTPCDGO, a.ANTPESTD, a.ANTPFANT, a.ANTPVLOR, a.ANTPSALD, a.ANTPNDOC,
       a.ANTPREFR, a.ANTPBANC, a.ANTPOBSR, a.ANTPASNT, a.ANTPFCRG
  FROM PGS.ANTP a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE t.TTLRIDNT IN ('1391904274001', '1791242963001')
 ORDER BY t.TTLRIDNT, a.ANTPCDGO;

-- BLOQUE 2 — Los pagos (PGTR) que crearon o pagaron esos anticipos
-- Si hay un PGTR con PGTRANTP = el anticipo, el anticipo salió del circuito de pagos, o sea que se
-- registró como un pago al proveedor. La observación suele decir por qué.
SELECT 'BLOQUE 2 - pagos de esos anticipos' AS bloque,
       p.PGTRCDGO, p.PGTRANTP, p.PGTRESTD, p.PGTRVLOR, p.PGTRORGN, p.PGTROBSR, p.PGTRFCRG
  FROM PGS.PGTR p
 WHERE p.PGTRANTP IN (SELECT a.ANTPCDGO FROM PGS.ANTP a JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
                       WHERE t.TTLRIDNT IN ('1391904274001', '1791242963001'))
 ORDER BY p.PGTRCDGO;

-- BLOQUE 3 — Las aplicaciones de las facturas de los dos proveedores (pagos, retenciones, anticipos)
-- ESPERADO SI la retención se aplicó «encima» de un pago total: tipo 1 por el total de la factura
-- y tipo 3 por la retención, con el saldo resultante negativo. O la retención SIN fila (cruce
-- rechazado).
SELECT 'BLOQUE 3 - aplicaciones' AS bloque,
       t.TTLRIDNT, f.ID AS id_factura, f.NUMESTABLECIMIENTO || '-' || f.NUMPTOEMISION || '-' || f.SECUENCIAL AS factura,
       f.TOTAL, x.APLPTDPG AS tipo, x.APLPMAPL AS monto, x.APLPESTD AS estado_aplic,
       x.APLPRTV2 AS id_retencion, x.APLPANTO AS id_anticipo_origen, x.APLPFAPL, x.APLPFCRG
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
  LEFT JOIN PGS.APLP x ON x.APLPFCTC = f.ID
 WHERE t.TTLRIDNT IN ('1391904274001', '1791242963001')
 ORDER BY t.TTLRIDNT, f.ID, x.APLPFCRG;

-- BLOQUE 4 — Las retenciones emitidas a los dos proveedores, y si tienen cruce activo
SELECT 'BLOQUE 4 - retenciones' AS bloque,
       t.TTLRIDNT, r.ID, r.NUMERO, r.FECHA, r.TOTAL, r.ESTADO, r.ESTADOEMISION, r.ASIENTO,
       (SELECT COUNT(*) FROM PGS.APLP x WHERE x.APLPRTV2 = r.ID AND x.APLPESTD = 1) AS cruces_activos
  FROM CBR.RTV2 r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
 WHERE t.TTLRIDNT IN ('1391904274001', '1791242963001')
 ORDER BY t.TTLRIDNT, r.FECHA;
