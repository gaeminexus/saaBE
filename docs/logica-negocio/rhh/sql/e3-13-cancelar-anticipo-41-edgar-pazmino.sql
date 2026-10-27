-- =====================================================================================
-- e3-13 — Datos: dejar CANCELADO el anticipo 41 (Edgar Pazmiño, 0909917759) y ANULADO su
--          descuento recurrente 41, que nunca generó cuotas
-- Equipo omen-saa-3 · 2026-10-27 · Solo UPDATE de dos filas, sin DDL
--
-- Qué pasó (medido el 2026-10-27):
--   - Anticipo 41: 200, fecha 05-08, pagado el 05-08 (PGTR 322, ref. 11469416, asiento 9446),
--     registrado en el SAA el 07-09 con «Confirmación manual». Quedó EN_DESCUENTO (4), saldo 200.
--   - Al confirmarse ese pago se creó el DSRC 41 (400, 1 cuota, VIGENTE) SIN ninguna cuota en
--     RHH.CTDS: la nómina nunca lo cobró y nunca lo va a cobrar.
--   - Lo que sí se cobró fue el DSRC 21, cargado a mano el 25-08 por 400 en 2 cuotas (CTDS 21 y
--     22), descontadas en las nóminas de agosto y septiembre (períodos CERRADOS). Está CANCELADO.
--   - El DSRC 21 no está ligado al anticipo, por eso el anticipo nunca bajó su saldo.
--   - El contador confirmó el 2026-10-27 que los valores descontados son correctos.
-- Corrección: anticipo 41 a CANCELADO (5) con saldo 0; DSRC 41 a ANULADO (4) con saldo 0.
-- No se toca el DSRC 21, ni las cuotas, ni el pago, ni asientos.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES (se espera exactamente lo descrito arriba)
-- 0.1 Anticipo 41: ANTEESTD = 4, ANTESLDD = 200, DSRCCDGO = 41, PGTRCDGO = 322
SELECT ANTECDGO, MPLDCDGO, ANTEVLOR, ANTESLDD, ANTEESTD, DSRCCDGO, PGTRCDGO, ANTEOBSR
  FROM RHH.ANTE WHERE ANTECDGO = 41;

-- 0.2 DSRC 41: DSRCESTD = 1, DSRCSLDD = 400, DSRCCTPG = 0
SELECT DSRCCDGO, MPLDCDGO, DSRCVLOR, DSRCSLDD, DSRCNMCT, DSRCCTPG, DSRCESTD
  FROM RHH.DSRC WHERE DSRCCDGO = 41;

-- 0.3 El DSRC 41 NO tiene cuotas: CERO filas. Si trae filas, NO correr el BLOQUE 1: avisar.
SELECT CTDSCDGO, CTDSESTD FROM RHH.CTDS WHERE DSRCCDGO = 41;

-- 0.4 Nada de nómina referencia una cuota del DSRC 41: CERO
SELECT COUNT(*) AS RENGLONES
  FROM RHH.RNGL r
 WHERE r.RNGLRFTB = 'RHH.CTDS'
   AND r.RNGLRFID IN (SELECT CTDSCDGO FROM RHH.CTDS WHERE DSRCCDGO = 41);

-- BLOQUE 1 — CORRECCIÓN (cada UPDATE debe decir «1 fila actualizada»)
UPDATE RHH.ANTE
   SET ANTEESTD = 5,
       ANTESLDD = 0,
       ANTEOBSR = SUBSTR(CASE WHEN ANTEOBSR IS NULL THEN '' ELSE ANTEOBSR || ' | ' END
                  || 'e3-13 2026-10-27: cancelado; se descontó con el descuento recurrente 21 '
                  || '(400 en 2 cuotas, nóminas ago y sep 2026), valores confirmados por el contador', 1, 2000)
 WHERE ANTECDGO = 41
   AND ANTEESTD = 4;

UPDATE RHH.DSRC
   SET DSRCESTD = 4,
       DSRCSLDD = 0
 WHERE DSRCCDGO = 41
   AND DSRCESTD = 1
   AND DSRCCTPG = 0
   AND NOT EXISTS (SELECT 1 FROM RHH.CTDS WHERE DSRCCDGO = 41);

COMMIT;

-- BLOQUE 2 — CONTROL DESPUÉS
-- 2.1 Anticipo 41: ANTEESTD = 5, ANTESLDD = 0
SELECT ANTECDGO, ANTEVLOR, ANTESLDD, ANTEESTD, ANTEOBSR FROM RHH.ANTE WHERE ANTECDGO = 41;
-- 2.2 DSRC 41: DSRCESTD = 4, DSRCSLDD = 0
SELECT DSRCCDGO, DSRCVLOR, DSRCSLDD, DSRCESTD FROM RHH.DSRC WHERE DSRCCDGO = 41;
-- 2.3 Edgar ya no tiene anticipos ni descuentos de anticipo vivos: CERO
SELECT COUNT(*) AS VIVOS
  FROM RHH.ANTE a JOIN RHH.MPLD m ON m.MPLDCDGO = a.MPLDCDGO
 WHERE m.MPLDIDNT = '0909917759' AND a.ANTEESTD IN (1, 2, 3, 4);

-- REVERSO (comentado) — vuelve exactamente al estado medido en el BLOQUE 0
-- UPDATE RHH.ANTE SET ANTEESTD = 4, ANTESLDD = 200,
--        ANTEOBSR = NULLIF(REGEXP_REPLACE(ANTEOBSR, '( \| )?e3-13 2026-10-27:.*$', ''), '')
--  WHERE ANTECDGO = 41 AND ANTEESTD = 5;
-- UPDATE RHH.DSRC SET DSRCESTD = 1, DSRCSLDD = 400 WHERE DSRCCDGO = 41 AND DSRCESTD = 4;
-- COMMIT;
