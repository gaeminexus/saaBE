-- =====================================================================================
-- e2-80 — El saldo global de anticipos del proveedor (TSR.PRCC tipo 2) no coincide con la suma de
--          los saldos de sus anticipos (PGS.ANTP). SOLO LECTURA. Equipo omen-saa-2, 2026-10-02.
--
-- Caso: Devolución de Anticipo de EXPOAUTOPARTS (1791242963001) por 23,94 (anticipo 7: 8,64 +
-- anticipo 10: 15,30). Se rechaza con «El saldo de anticipos del proveedor … es de $15.30 y no alcanza
-- para devolver $23.94». Faltan exactamente 8,64, el saldo del anticipo 7.
--
-- PRCCSLIN es un saldo GLOBAL que se mueve en paralelo al saldo de cada anticipo: sube al confirmar un
-- anticipo y baja en cada cruce. Si una operación movió uno y no el otro, quedan descuadrados. La
-- devolución (y el cruce) validan contra PRCCSLIN, por eso aparece aquí.
-- Nombres de columna copiados de PersonaCuentaContable (PRCC), PersonaRol (PRRL), AnticipoProveedor
-- (ANTP) y AplicacionPagoCxp (APLP). Rol proveedor = PRRLRZZA 2 (RolPersona.PROVEEDOR); tipo 2 = anticipos.
-- =====================================================================================

-- BLOQUE 1 — EXPOAUTOPARTS: PRCC tipo 2 contra la suma de saldos de sus anticipos CONFIRMADOS (estado 2)
-- ESPERADO SI la hipótesis es cierta: prcc_saldo 15,30 y suma_anticipos 23,94 (diferencia 8,64).
SELECT 'BLOQUE 1 - EXPOAUTOPARTS' AS bloque,
       c.PRCCCDGO, c.PJRQCDGO AS empresa, c.PRCCSLIN AS prcc_saldo,
       (SELECT NVL(SUM(a.ANTPSALD), 0) FROM PGS.ANTP a
         WHERE a.ANTPTTLR = r.PRSNCDGO AND a.ANTPPJRQ = c.PJRQCDGO AND a.ANTPESTD = 2 AND a.ANTPVLOR > 0)
         AS suma_anticipos
  FROM TSR.PRCC c
  JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
 WHERE t.TTLRIDNT = '1791242963001' AND c.PRCCTPOO = 2 AND r.PRRLRZZA = 2;

-- BLOQUE 2 — La historia de los dos anticipos: valor, saldo y lo que se les cruzó (APLP tipo 4 por
-- anticipo de origen). Para cada uno debería cumplirse: valor − cruzado = saldo.
SELECT 'BLOQUE 2 - historia' AS bloque,
       a.ANTPCDGO, a.ANTPESTD, a.ANTPFANT, a.ANTPVLOR, a.ANTPSALD, a.ANTPASNT, a.ANTPFCRG,
       (SELECT NVL(SUM(x.APLPMAPL), 0) FROM PGS.APLP x WHERE x.APLPANTO = a.ANTPCDGO AND x.APLPESTD = 1) AS cruzado_activo,
       (SELECT NVL(SUM(x.APLPMAPL), 0) FROM PGS.APLP x WHERE x.APLPANTO = a.ANTPCDGO AND x.APLPESTD <> 1) AS cruzado_reversado
  FROM PGS.ANTP a
  JOIN TSR.TTLR t ON t.TTLRCDGO = a.ANTPTTLR
 WHERE t.TTLRIDNT = '1791242963001'
 ORDER BY a.ANTPCDGO;

-- BLOQUE 3 — LA FAMILIA: todos los proveedores con PRCC tipo 2 distinto de la suma de saldos de sus
-- anticipos confirmados. Cada fila es un proveedor al que hoy un cruce o una devolución le puede fallar
-- (si el PRCC es menor) o al que le sobra saldo global sin anticipo detrás (si es mayor).
-- ESPERADO: idealmente CERO filas. Más de EXPOAUTOPARTS quiere decir que el defecto es de un flujo, no de un caso.
SELECT 'BLOQUE 3 - familia' AS bloque, t.TTLRIDNT, NVL(t.TTLRRZSC, t.TTLRNMBR) AS proveedor,
       c.PJRQCDGO AS empresa, c.PRCCSLIN AS prcc_saldo, s.suma_anticipos,
       ROUND(c.PRCCSLIN - s.suma_anticipos, 2) AS diferencia
  FROM TSR.PRCC c
  JOIN TSR.PRRL r ON r.PRRLCDGO = c.PRRLCDGO
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PRSNCDGO
  JOIN (SELECT a.ANTPTTLR, a.ANTPPJRQ, SUM(a.ANTPSALD) AS suma_anticipos
          FROM PGS.ANTP a WHERE a.ANTPESTD = 2 AND a.ANTPVLOR > 0
         GROUP BY a.ANTPTTLR, a.ANTPPJRQ) s
    ON s.ANTPTTLR = r.PRSNCDGO AND s.ANTPPJRQ = c.PJRQCDGO
 WHERE c.PRCCTPOO = 2 AND r.PRRLRZZA = 2
   AND ABS(NVL(c.PRCCSLIN, 0) - s.suma_anticipos) > 0.01
 ORDER BY ABS(NVL(c.PRCCSLIN, 0) - s.suma_anticipos) DESC;
