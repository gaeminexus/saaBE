-- =====================================================================================
-- 390 — MEDICIÓN (SOLO LECTURA): omisiones del control de cobro duplicado (lote 6, c45-1)
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada. Se puede correr en producción.
--
-- Cuando un cobro se procesa con `omitirControlDuplicado = true` (POST /rest/cbcr/{id}/procesar), el código deja una constancia,
-- sin tabla nueva, con el marcador fijo «[OMISION-COBRO-DUPLICADO]» al inicio del texto:
--   · en la observación del cobro (CRD.CBCR.CBCROBSR), y
--   · en la huella del préstamo (CRD.PRST.PRSTOBSR).
-- Texto: «[OMISION-COBRO-DUPLICADO] cobro N, préstamo M, evento coincidente E (TIPO), omitido por USUARIO: MOTIVO».
-- Ojo: las dos observaciones se truncan por la izquierda a 2000 bytes; una constancia muy vieja puede haberse perdido de la
-- huella del préstamo (la del cobro es más estable).
-- =====================================================================================


-- BLOQUE 1 — Cobros con omisión del control (la fuente principal, por cobro).
SELECT b.CBCRCDGO AS cobro, b.CBCRTPOO AS tipo, b.CBCRESTD AS estado, TRUNC(b.CBCRFCHA) AS fecha_pago,
       b.CBCRVLRR AS valor, b.CBCRUSPR AS usuario_proceso, TRUNC(b.CBCRFCPR) AS fecha_proceso,
       SUBSTR(b.CBCROBSR, INSTR(b.CBCROBSR, '[OMISION-COBRO-DUPLICADO]'), 600) AS constancia
  FROM CRD.CBCR b
 WHERE b.CBCROBSR LIKE '%[OMISION-COBRO-DUPLICADO]%'
 ORDER BY b.CBCRFCPR DESC, b.CBCRCDGO DESC;

-- BLOQUE 2 — Préstamos con la constancia en su huella (por préstamo).
SELECT NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, p.PRSTIDST AS estado_prestamo,
       SUBSTR(p.PRSTOBSR, INSTR(p.PRSTOBSR, '[OMISION-COBRO-DUPLICADO]'), 400) AS constancia
  FROM CRD.PRST p
 WHERE p.PRSTOBSR LIKE '%[OMISION-COBRO-DUPLICADO]%'
 ORDER BY p.PRSTCDGO;

-- BLOQUE 3 — Cobros omitidos cuyo evento coincidente sigue vigente, el que más interesa revisar: el control los dejó pasar
-- porque eran dos pagos distintos, pero si el evento suelto se anuló después, el cobro quedó solo y es lo correcto; si el evento
-- sigue vigente, hay dos pagos aplicados al mismo préstamo con la misma fecha y valor. Lista los pares.
SELECT b.CBCRCDGO AS cobro, NVL(p.PRSTIDAS, p.PRSTCDGO) AS operacion, e.EVPRCDGO AS evento_suelto, e.EVPRTPOO AS tipo_evento,
       TRUNC(e.EVPRFCHA) AS fecha_evento, ROUND(e.EVPRVLRR, 2) AS valor_evento, e.EVPRESTD AS estado_evento
  FROM CRD.CBCR b
  JOIN CRD.DCBC d ON d.CBCRCDGO = b.CBCRCDGO
  JOIN CRD.EVPR ev ON ev.EVPRCDGO = d.EVPRCDGO
  JOIN CRD.PRST p ON p.PRSTCDGO = ev.PRSTCDGO
  JOIN CRD.EVPR e ON e.PRSTCDGO = ev.PRSTCDGO AND e.EVPRCDGO <> ev.EVPRCDGO AND e.EVPRESTD = 1
                 AND TRUNC(e.EVPRFCHA) = TRUNC(ev.EVPRFCHA) AND ABS(e.EVPRVLRR - ev.EVPRVLRR) < 0.005
                 AND NOT EXISTS (SELECT 1 FROM CRD.DCBC x WHERE x.EVPRCDGO = e.EVPRCDGO)
 WHERE b.CBCROBSR LIKE '%[OMISION-COBRO-DUPLICADO]%'
 ORDER BY b.CBCRCDGO, e.EVPRCDGO;
