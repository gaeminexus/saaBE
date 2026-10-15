-- =====================================================================================
-- e2-105 — Pago 769 (anticipo #181): segunda ronda. SOLO LECTURA. Equipo omen-saa-2 · 2026-10-15.
--
-- e2-104 salió LIMPIO: pago, anticipo (estado 2 = APROBADO), configuración de nómina, línea 14 de la plantilla
-- de rol (1.4.03.10.02) y concepto «Anticipo de sueldo» (22) están bien. Y 11 anticipos se confirmaron antes
-- con asiento. Así que no falta configuración: lo que falla es algo que se GRABA durante la confirmación.
-- Lo que se graba, en orden (PagoProgramadoServiceImpl.contabilizarPagoAnticipoEmpleado):
--   CNT.ASNT + CNT.DTAS (asiento, secuencias SQ_ASNTCDGO / SQ_DTASCDGO, período del 13/10/2026)
--   TSR.MVCB (movimiento bancario, secuencia SQ_MVCBCDGO)
--   RHH.ANTE (estado PAGADO) → RHH.DSRC (descuento, IDENTITY) → RHH.CTDS (cuota, IDENTITY) → RHH.ANTE (EN_DESCUENTO)
-- Sospecha principal: una secuencia/IDENTITY atrasada respecto del MAX de su tabla (ORA-00001 en el INSERT),
-- típico después de cargar datos con IDs explícitos (migración de RRHH, copias entre bases).
-- El primer bloque que no dé lo ESPERADO es la causa.
-- =====================================================================================

-- BLOQUE 1 — Secuencias IDENTITY de RRHH. ESPERADO: proximo_valor > max_en_tabla en las tres.
-- Si alguna tiene proximo_valor <= max_en_tabla → ÉSA ES LA CAUSA (el INSERT choca con una PK existente).
SELECT 'BLOQUE 1 - identity rhh' AS bloque, i.TABLE_NAME, i.COLUMN_NAME, i.GENERATION_TYPE, s.LAST_NUMBER AS proximo_valor,
       CASE i.TABLE_NAME
            WHEN 'DSRC' THEN (SELECT NVL(MAX(DSRCCDGO), 0) FROM RHH.DSRC)
            WHEN 'CTDS' THEN (SELECT NVL(MAX(CTDSCDGO), 0) FROM RHH.CTDS)
            WHEN 'ANTE' THEN (SELECT NVL(MAX(ANTECDGO), 0) FROM RHH.ANTE)
       END AS max_en_tabla
  FROM ALL_TAB_IDENTITY_COLS i
  JOIN ALL_SEQUENCES s ON s.SEQUENCE_OWNER = i.OWNER AND s.SEQUENCE_NAME = i.SEQUENCE_NAME
 WHERE i.OWNER = 'RHH' AND i.TABLE_NAME IN ('DSRC', 'CTDS', 'ANTE');

-- BLOQUE 2 — Secuencias clásicas del asiento y del movimiento bancario. ESPERADO: proximo_valor > max_en_tabla.
-- (Los demás pagos sí se confirman, así que aquí se espera todo bien; se revisa para descartar.)
SELECT 'BLOQUE 2 - secuencias cnt/tsr' AS bloque, s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER AS proximo_valor,
       CASE s.SEQUENCE_NAME
            WHEN 'SQ_ASNTCDGO' THEN (SELECT NVL(MAX(ASNTCDGO), 0) FROM CNT.ASNT)
            WHEN 'SQ_DTASCDGO' THEN (SELECT NVL(MAX(DTASCDGO), 0) FROM CNT.DTAS)
            WHEN 'SQ_MVCBCDGO' THEN (SELECT NVL(MAX(MVCBCDGO), 0) FROM TSR.MVCB)
       END AS max_en_tabla
  FROM ALL_SEQUENCES s
 WHERE (s.SEQUENCE_OWNER = 'CNT' AND s.SEQUENCE_NAME IN ('SQ_ASNTCDGO', 'SQ_DTASCDGO'))
    OR (s.SEQUENCE_OWNER = 'TSR' AND s.SEQUENCE_NAME = 'SQ_MVCBCDGO');

-- BLOQUE 3 — ¿Ya existe un descuento «ANTE-181» o cuotas colgadas de un intento anterior? ESPERADO: 0 filas.
SELECT 'BLOQUE 3 - descuento previo' AS bloque, d.DSRCCDGO, d.MPLDCDGO, d.DSRCNMRO, d.DSRCVLOR, d.DSRCESTD, d.DSRCFCHR,
       (SELECT COUNT(*) FROM RHH.CTDS c WHERE c.DSRCCDGO = d.DSRCCDGO) AS cuotas
  FROM RHH.DSRC d
 WHERE d.DSRCNMRO = 'ANTE-181';

-- BLOQUE 4 — Período contable de octubre 2026 de la empresa del pago. ESPERADO: una fila, PRDOESTD distinto de 2
-- (2 = MAYORIZADO). Sin fila o en 2 → no se puede generar el asiento.
SELECT 'BLOQUE 4 - periodo octubre' AS bloque, p.PRDOCDGO, p.PRDONMBR, p.PRDOMSSS, p.PRDOANNN, p.PRDOESTD
  FROM CNT.PRDO p
 WHERE p.PJRQCDGO = (SELECT PGTRPJRQ FROM PGS.PGTR WHERE PGTRCDGO = 769)
   AND p.PRDOMSSS = 10 AND p.PRDOANNN = 2026;

-- BLOQUE 5 — Los 11 anticipos que sí se confirmaron: cuándo, y si quedaron con descuento y cuotas.
-- Sirve para ver si el último confirmado es anterior a una migración/copia de datos (refuerza el BLOQUE 1).
SELECT 'BLOQUE 5 - anticipos confirmados' AS bloque, p.PGTRCDGO, p.PGTRIDOR AS anticipo, p.PGTRFRSP, a.ASNTFCHA,
       an.ANTEESTD, an.DSRCCDGO,
       (SELECT COUNT(*) FROM RHH.CTDS c WHERE c.DSRCCDGO = an.DSRCCDGO) AS cuotas
  FROM PGS.PGTR p
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = p.PGTRASNT
  LEFT JOIN RHH.ANTE an ON an.ANTECDGO = p.PGTRIDOR
 WHERE p.PGTRORGN = 'RHH_ANTICIPO_EMPLEADO'
 ORDER BY p.PGTRCDGO;

-- BLOQUE 6 — El empleado 56. ESPERADO: existe, de la misma empresa del pago.
SELECT 'BLOQUE 6 - empleado' AS bloque, e.MPLDCDGO, e.MPLDNMBR, e.MPLDAPLL, e.MPLDESTD, e.PJRQCDGO
  FROM RHH.MPLD e
 WHERE e.MPLDCDGO = 56;

-- =====================================================================================
-- Si el BLOQUE 1 o el 2 salen atrasados, la corrección es sincronizar esa secuencia (te paso el script aparte,
-- con control antes/después). NO hace falta tocar el pago 769: queda «En archivo» y se vuelve a confirmar.
-- =====================================================================================
