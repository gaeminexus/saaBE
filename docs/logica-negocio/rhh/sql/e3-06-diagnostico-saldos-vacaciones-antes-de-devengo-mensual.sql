-- =====================================================================================
-- e3-06 — Saldos de vacaciones actuales, antes de pasar a devengo mensual (1,25 días/mes)
-- Equipo omen-saa-3 · 2026-09-30 · SOLO LECTURA (ningún INSERT/UPDATE/DELETE)
--
-- Por qué: el usuario decidió que la acreditación de vacaciones sea parametrizable (15 días
-- al cumplir el año, o 1,25 por mes) y que ASOPREP use 1,25 por mes. Antes de diseñar la
-- migración hay que ver cómo están hoy TODAS las filas de RHH.SLDV. Las 7 filas de 2026 que
-- creó la acreditación del 2026-08-27 son del modelo por aniversario (15 días completos,
-- periodo desde el 2026-01-01), y el devengo mensual las recalcularía. Pegar la salida de
-- los 3 bloques.
-- =====================================================================================

-- BLOQUE 1 — Parámetros de vacaciones por año (días base, año del adicional, tope, caducidad)
SELECT p.PRNMANOO, p.PJRQCDGO, p.PRNMDIVC, p.PRNMANVC, p.PRNMMXVC, p.PRNMCDVC
  FROM RHH.PRNM p
 ORDER BY p.PJRQCDGO, p.PRNMANOO;

-- BLOQUE 2 — Todas las filas de saldo de los colaboradores activos, con su fecha de ingreso
SELECT m.MPLDCDGO                      AS ID,
       m.MPLDAPLL || ' ' || m.MPLDNMBR AS NOMBRE,
       m.MPLDFCIN                      AS INGRESO,
       s.SLDVANOO                      AS ANIO,
       s.SLDVFCHI                      AS DESDE,
       s.SLDVFCHF                      AS HASTA,
       s.SLDVASGN                      AS ASIGNADOS,
       s.SLDVDIAD                      AS ADICIONALES,
       s.SLDVDIAR                      AS ARRASTRADOS,
       s.SLDVUSDO                      AS USADOS,
       s.SLDVPNDE                      AS PENDIENTES,
       s.SLDVCDCD                      AS CADUCADO,
       NVL(s.SLDVAPRT, 'N')            AS APERTURA,
       s.SLDVFCHR                      AS CREADA,
       s.SLDVUSRR                      AS POR
  FROM RHH.MPLD m
  LEFT JOIN RHH.SLDV s ON s.MPLDCDGO = m.MPLDCDGO
 WHERE m.MPLDESTD = 1
 ORDER BY m.MPLDFCIN, m.MPLDCDGO, s.SLDVANOO;

-- BLOQUE 3 — Solicitudes de vacaciones de 2026, en cualquier estado (lo que ya se consumió del saldo)
SELECT c.SLCTCDGO, c.MPLDCDGO, c.SLCTESTD, c.SLCTAPRB, c.SLCTFCHD, c.SLCTFCHH, c.SLCTDIAS
  FROM RHH.SLCT c
 WHERE c.SLCTFCHD >= DATE '2026-01-01'
 ORDER BY c.MPLDCDGO, c.SLCTFCHD;
