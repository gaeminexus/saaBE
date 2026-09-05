-- =====================================================================================
-- 198 - Verificacion: la plantilla 21 (CNT.PLNS/DTPL), buscando el aporte 23
-- FECHA: 2026-09-05 - EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Contexto: la corrida de jubilados fallo con "El tipo de aporte 23 no tiene cuenta
-- contable parametrizada (hoy solo 9 jubilacion, 11 cesantia, 2 adicional)" --
-- ContabilizacionIndividualCreditoServiceImpl.aux1ParaTipoAporte solo mapea esos tres
-- tipos a linea de asiento (aux1 51/50/52 de la plantilla 21). Antes de proponerle al
-- usuario una cuenta nueva, hay que confirmar si el dato YA esta parametrizado con otro
-- aux1 que el codigo simplemente no lee.
-- =====================================================================================

-- =====================================================================================
-- BLOQUE 1 - TODAS las lineas de la plantilla 21, para ver el panorama completo
-- =====================================================================================
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, p.PJRQCDGO AS EMPRESA, p.PLNSESTD AS ESTADO_PLANTILLA,
       d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCDGO AS CUENTA_ID, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE,
       d.DTPLDSCR AS DESCRIPCION, d.DTPLESTD AS ESTADO_LINEA
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL = 21
ORDER  BY d.DTPLAXL1;

-- QUE MIRAR:
--   - Los aux1 conocidos y ya usados por el codigo son 50 (cesantia), 51 (jubilacion) y
--     52 (adicional) -- ver com.saa.rubros.CrdLineaAsiento.
--   - Si aparece alguna linea con AUX1 fuera de {1,2,3,4,10,20,30,40,42,43,50,51,52,60,70,90}
--     (los que ya tiene el catalogo CrdLineaAsiento), revisar su DESCRIPCION: podria ser el
--     aporte 23 ya parametrizado y sin usar, con otro numero.
--   - Si NO aparece ninguna linea fuera de ese conjunto, el dato no esta parametrizado
--     todavia y hace falta que el usuario decida la cuenta contable antes de tocar codigo.

-- =====================================================================================
-- BLOQUE 2 - Filtro directo: descripciones que mencionen pension/complementaria/23
-- =====================================================================================
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  UPPER(d.DTPLDSCR) LIKE '%PENSION%'
    OR UPPER(d.DTPLDSCR) LIKE '%COMPLEMENTARIA%'
    OR UPPER(n.PLNNNMBR) LIKE '%PENSION%';

-- =====================================================================================
-- BLOQUE 3 - Aux1 ya ocupados en la 21, para saber cual queda libre si hay que crear uno
-- =====================================================================================
SELECT DISTINCT d.DTPLAXL1 AS AUX1_OCUPADO
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
WHERE  p.PLNSCDAL = 21
ORDER  BY d.DTPLAXL1;

-- El catalogo Java (CrdLineaAsiento) hoy usa: 1,2,3,4,10,20,30,40,42,43,50,51,52,60,70,90.
-- Candidato natural si hay que crear una linea nueva para el aporte 23: 53 (mismo cluster
-- que 50/51/52, "Aplicacion de pagos y liquidaciones") -- CONFIRMAR con este bloque que 53
-- no está ya ocupado en la BD real antes de proponerlo.
