-- =====================================================================================
-- e2-74 — Por qué el pago 699 (ZOOM COMMUNICATIONS, 173,31) no se puede aprobar por
--         transferencia: «les falta la cuenta bancaria de destino del beneficiario».
-- SOLO LECTURA. No modifica nada. Equipo omen-saa-2, 2026-09-29.
--
-- La guarda (PagoProgramadoServiceImpl:1440-1461) NO mira si el titular TIENE una cuenta:
-- mira si el PAGO la tiene guardada (PGS.PGTR.PGTRCTBN), o si trae completo el beneficiario
-- ocasional (PGTRBFBC + PGTRBFTP + PGTRBFCT). La cuenta del pago se fija AL REGISTRAR el
-- egreso (registro-egreso → idCuentaDestinoTitular) y después no hay pantalla ni endpoint que
-- la cambie. Hipótesis: la cuenta se le cargó al titular DESPUÉS de registrar el pago
-- (mismo caso que el pago 394, estado del equipo §42).
--
-- Nombres de columna copiados de las entidades PagoProgramado, CuentaBancariaTitular,
-- Egreso y Titular, no escritos de memoria.
-- =====================================================================================

-- BLOQUE 1 — El pago 699 tal como está grabado
-- ESPERADO SI LA HIPÓTESIS ES CIERTA: PGTRCTBN nulo, y PGTRBFBC/PGTRBFTP/PGTRBFCT nulos.
SELECT 'BLOQUE 1 - pago' AS bloque,
       p.PGTRCDGO, p.PGTRESTD, p.PGTRVLOR, p.PGTRFCRG,
       p.PGTRTTLR, t.TTLRIDNT, NVL(t.TTLRRZSC, t.TTLRNMBR) AS titular,
       p.PGTRCTBN, p.PGTRBFBC, p.PGTRBFTP, p.PGTRBFCT,
       p.PGTREGRS, e.EGRSESTD, e.EGRSFCRG, e.EGRSTTLR
  FROM PGS.PGTR p
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = p.PGTRTTLR
  LEFT JOIN TSR.EGRS e ON e.EGRSCDGO = p.PGTREGRS
 WHERE p.PGTRCDGO = 699;

-- BLOQUE 2 — Las cuentas bancarias que tiene HOY el titular del pago
-- Mirar CTBNFCRG contra el PGTRFCRG del bloque 1: si la cuenta es posterior, está confirmada.
-- Mirar también CTBNIDNT/CTBNNMBR: si es la cuenta de un colaborador, acá aparece su cédula/nombre.
SELECT 'BLOQUE 2 - cuentas del titular' AS bloque,
       c.CTBNCDGO, c.BEXTCDGO, c.CTBNTPCT, c.CTBNNMCT, c.CTBNTPID, c.CTBNIDNT, c.CTBNNMBR,
       c.CTBNESTD, c.CTBNFCRG, c.CTBNUSAR
  FROM TSR.CTBN c
 WHERE c.TTLRCDGO = (SELECT p.PGTRTTLR FROM PGS.PGTR p WHERE p.PGTRCDGO = 699)
 ORDER BY c.CTBNCDGO;

-- BLOQUE 3 — La familia: pagos POR_APROBAR (PGTRESTD = 0) sin cuenta de destino ni beneficiario
-- ocasional cuyo titular SÍ tiene hoy alguna cuenta activa. Todos van a chocar con el mismo error
-- si se intentan aprobar por transferencia.
-- ESPERADO: al menos el 699. Si aparecen más, el defecto no es de este pago sino del flujo.
SELECT 'BLOQUE 3 - familia' AS bloque,
       p.PGTRCDGO, p.PGTRVLOR, p.PGTRFCRG, p.PGTREGRS,
       t.TTLRIDNT, NVL(t.TTLRRZSC, t.TTLRNMBR) AS titular,
       (SELECT COUNT(*) FROM TSR.CTBN c
         WHERE c.TTLRCDGO = p.PGTRTTLR AND NVL(c.CTBNESTD, 1) <> 0) AS cuentas_activas_hoy
  FROM PGS.PGTR p
  JOIN TSR.TTLR t ON t.TTLRCDGO = p.PGTRTTLR
 WHERE p.PGTRESTD = 0
   AND p.PGTRCTBN IS NULL
   AND (p.PGTRBFBC IS NULL OR p.PGTRBFTP IS NULL OR p.PGTRBFCT IS NULL)
   AND EXISTS (SELECT 1 FROM TSR.CTBN c
                WHERE c.TTLRCDGO = p.PGTRTTLR AND NVL(c.CTBNESTD, 1) <> 0)
 ORDER BY p.PGTRCDGO;
