-- =====================================================================================
-- VERIFICACION DE LAS OBSERVACIONES DEL CLIENTE A LA APP MOVIL (2026-10-21)
-- =====================================================================================
-- Equipo: app movil ASOPREP (app1). Escrito por el arbitro omen-app-1-arb el 2026-10-22.
-- Orden: saaAPP/docs/ORDEN-OBSERVACIONES-CLIENTE-2026-10-21.md
--
-- SOLO CONSULTA. No hay un solo INSERT/UPDATE/DELETE en este archivo.
--
-- Para que sirve: la orden fija reglas a partir de las capturas del cliente. Estas
-- consultas confirman, con los datos reales, que esas reglas explican lo que se ve:
--
--   A. Prestamo 70463 (CHECA YANEZ): las cuotas 25 y 26 salen como "proximas" en la app y
--      el SAA las da por pagadas. Hipotesis: estado 4 (PAGADA) y DTPRFCPG en NULL. La app
--      decide "pagada" por la fecha de pago; el SAA, por el estado.
--   B. Mismo prestamo: el SAA muestra 31/03/2024 - 31/03/2031 y la app, 08/03/2024 -
--      08/03/2031. Hipotesis: el SAA muestra el vencimiento de la primera y de la ultima
--      cuota, y la app muestra PRSTFCIN/PRSTFCFN.
--   C. Prestamos cancelados anticipados con cuotas que no quedaron en estado pagado.
--   D. Prestamos con PRSTIDAS (idAsoprep) en NULL: la app hoy muestra el codigo interno en
--      su lugar (el "N 7958" de la captura).
--   E. Tipos de prestamo: la regla nueva del saldo disponible descuenta solo TPPRCDGO = 1
--      (quirografario). Confirmar que todos los productos EMERGENTE tienen ese tipo.
--
-- Pasame los resultados de cada bloque tal como salen. No bloquean el arranque del trabajo.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- A. Cuotas del prestamo 70463: estado, fecha de pago, total, saldo de capital
-- -------------------------------------------------------------------------------------
SELECT d.DTPRNMCT AS cuota,
       d.DTPRFCVN AS vencimiento,
       d.DTPRESTD AS estado,
       d.DTPRFCPG AS fecha_pagado,
       d.DTPRCTAA AS cuota_cap_int,
       d.DTPRTTLL AS total,
       d.DTPRSLCP AS saldo_capital,
       d.DTPRSLOT AS pago_extra
  FROM CRD.DTPR d
  JOIN CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
 WHERE p.PRSTIDAS = 70463
 ORDER BY d.DTPRNMCT;


-- -------------------------------------------------------------------------------------
-- B. Fechas del prestamo 70463 contra la primera y la ultima cuota
-- -------------------------------------------------------------------------------------
SELECT p.PRSTCDGO, p.PRSTIDAS, p.PRSTFCHA, p.PRSTFCIN, p.PRSTFCFN,
       (SELECT MIN(d.DTPRFCVN) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS primera_cuota,
       (SELECT MAX(d.DTPRFCVN) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS ultima_cuota
  FROM CRD.PRST p
 WHERE p.PRSTIDAS = 70463;


-- -------------------------------------------------------------------------------------
-- C. Prestamos cancelados (3, 4, 5, 13) por estado de cuota
--    Cuantas cuotas quedaron en cada estado. Lo esperado es ver estados 1/2/3 (pendiente,
--    activa, emitida) dentro de prestamos cancelados: es lo que la app mostraba como
--    "proximas cuotas" en un prestamo ya cancelado.
-- -------------------------------------------------------------------------------------
SELECT p.PRSTIDST AS estado_prestamo,
       d.DTPRESTD AS estado_cuota,
       COUNT(*)   AS cuotas,
       COUNT(DISTINCT p.PRSTCDGO) AS prestamos
  FROM CRD.PRST p
  JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
 WHERE p.PRSTIDST IN (3, 4, 5, 13)
 GROUP BY p.PRSTIDST, d.DTPRESTD
 ORDER BY 1, 2;


-- -------------------------------------------------------------------------------------
-- D. Prestamos sin idAsoprep (PRSTIDAS), por estado
-- -------------------------------------------------------------------------------------
SELECT p.PRSTIDST AS estado_prestamo,
       COUNT(*)   AS total,
       COUNT(CASE WHEN p.PRSTIDAS IS NULL THEN 1 END) AS sin_idasoprep
  FROM CRD.PRST p
 GROUP BY p.PRSTIDST
 ORDER BY 1;


-- -------------------------------------------------------------------------------------
-- E. Tipos de prestamo y los productos de cada uno
-- -------------------------------------------------------------------------------------
SELECT t.TPPRCDGO, t.TPPRNMBR, p.PRDCCDGO, p.PRDCNMBR
  FROM CRD.PRDC p
  LEFT JOIN CRD.TPPR t ON t.TPPRCDGO = p.TPPRCDGO
 ORDER BY t.TPPRCDGO, p.PRDCCDGO;
