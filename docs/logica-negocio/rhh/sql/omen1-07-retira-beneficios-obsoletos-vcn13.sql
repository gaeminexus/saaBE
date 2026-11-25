-- =====================================================================================
-- omen1-07 — RETIRO AUTORIZADO de beneficios sociales obsoletos (VCN #13, lote 11). REVISABLE: NO está ejecutado en ninguna base. SIN COMMIT.
-- Equipo omen-saa-1 · 2026-11-25.
--
-- Por qué es un SQL y no el generador: RHH.LQBS no tiene un estado de anulación ni columnas de usuario, fecha y motivo de retiro (solo 1 = calculada y
-- 2 = pagada). Por decisión del árbitro el código NO borra: generarDecimoTercero / generarDecimoCuarto / generarFondosReserva solo CUENTAN estas filas y
-- devuelven `retirables` e `idsRetirables`. Este script las retira con respaldo, y lo corre el usuario después de revisar el BLOQUE 1.
--
-- Criterio (el mismo del código, BeneficioSocialServiceImpl.conRetirables): fila calculada (estado distinto de 2), SIN orden de pago (LQBSODBS nulo),
-- SIN valor pagado (LQBSVLPG nulo o 0), NO anticipada (LQBSANTC distinto de 'S'), del tipo y año indicados, de una persona SIN contrato activo en el año
-- (CESANTE o sin contrato que cruce el año). Quien pasó a MENSUALIZADO sigue con contrato activo y no entra.
-- PARAMS (cambiar en cada bloque): TIPO 1 = décimo tercero, 2 = décimo cuarto, 3 = fondo de reserva · AÑO.
-- «Contrato activo en el año» es EXACTAMENTE el de ContratoEmpleadoDaoServiceImpl.selectActivosEnPeriodo (el que usa el generador): inicio <= 31-dic,
-- fin nulo o >= 1-ene, y (sin fecha de terminación y empleado no CESANTE) o (fecha de terminación posterior al 31-dic). Quien se liquidó en el año
-- tiene fecha de terminación dentro del año y por eso NO está activo: su fila es la obsoleta.
-- =====================================================================================

-- BLOQUE 1 — Filas retirables (solo lectura). Revisar una por una antes del BLOQUE 2.
SELECT l.LQBSCDGO, l.MPLDCDGO AS empleado, e.MPLDESTD AS estado_empleado, l.LQBSTPBN AS tipo, l.LQBSANOO AS anio, l.LQBSVLRR AS valor, l.LQBSVLMN AS valor_mensualizado,
       CASE WHEN e.MPLDESTD = 4 THEN 'CESANTE' ELSE 'SIN_CONTRATO_ACTIVO' END AS motivo
  FROM RHH.LQBS l JOIN RHH.MPLD e ON e.MPLDCDGO = l.MPLDCDGO
 WHERE l.LQBSTPBN = 1 AND l.LQBSANOO = 2026
   AND NVL(l.LQBSESTD, 0) <> 2 AND l.LQBSODBS IS NULL AND NVL(l.LQBSVLPG, 0) = 0 AND NVL(l.LQBSANTC, 'N') <> 'S'
   AND NOT EXISTS (SELECT 1 FROM RHH.CNTE c WHERE c.MPLDCDGO = l.MPLDCDGO AND c.CNTEFCHI <= DATE '2026-12-31' AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-01-01') AND ((c.CNTEFCTR IS NULL AND NVL(e.MPLDESTD, 0) <> 4) OR c.CNTEFCTR > DATE '2026-12-31'))
 ORDER BY l.MPLDCDGO;

-- BLOQUE 2 — Respaldo de las filas a retirar (DDL: crea la tabla y hace commit implícito de nada pendiente). Cambiar TIPO y AÑO igual que arriba.
CREATE TABLE RHH.BK_LQBS_C13_20261125 AS
SELECT l.*, SYSDATE AS RESPALDADO, 'C13 retiro de obsoleta (VCN #13)' AS MOTIVO
  FROM RHH.LQBS l JOIN RHH.MPLD e ON e.MPLDCDGO = l.MPLDCDGO
 WHERE l.LQBSTPBN = 1 AND l.LQBSANOO = 2026
   AND NVL(l.LQBSESTD, 0) <> 2 AND l.LQBSODBS IS NULL AND NVL(l.LQBSVLPG, 0) = 0 AND NVL(l.LQBSANTC, 'N') <> 'S'
   AND NOT EXISTS (SELECT 1 FROM RHH.CNTE c WHERE c.MPLDCDGO = l.MPLDCDGO AND c.CNTEFCHI <= DATE '2026-12-31' AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-01-01') AND ((c.CNTEFCTR IS NULL AND NVL(e.MPLDESTD, 0) <> 4) OR c.CNTEFCTR > DATE '2026-12-31'));
SELECT COUNT(*) AS filas_respaldadas FROM RHH.BK_LQBS_C13_20261125;   -- debe coincidir con el BLOQUE 1 (y con `retirables` del generador)

-- BLOQUE 3 — Retiro (ESCRIBE; sin commit). Solo lo respaldado y que SIGUE cumpliendo el criterio.
DELETE FROM RHH.LQBS l
 WHERE l.LQBSCDGO IN (SELECT b.LQBSCDGO FROM RHH.BK_LQBS_C13_20261125 b)
   AND NVL(l.LQBSESTD, 0) <> 2 AND l.LQBSODBS IS NULL AND NVL(l.LQBSVLPG, 0) = 0;

-- BLOQUE 4 — Controles posteriores (antes de COMMIT).
SELECT COUNT(*) AS quedan_de_las_respaldadas FROM RHH.LQBS l WHERE l.LQBSCDGO IN (SELECT b.LQBSCDGO FROM RHH.BK_LQBS_C13_20261125 b);   -- debe ser 0
SELECT COUNT(*) AS pagadas_u_ordenadas_intactas FROM RHH.LQBS l WHERE l.LQBSESTD = 2 OR l.LQBSODBS IS NOT NULL;   -- no debe bajar respecto de antes
-- COMMIT;    -- solo si el BLOQUE 4 es el esperado
-- ROLLBACK;  -- si algo no cuadra

-- =====================================================================================
-- REVERSO (comentado; solo si ya se hizo COMMIT y hay que deshacer): reinserta las filas desde el respaldo.
-- INSERT INTO RHH.LQBS SELECT <las columnas de RHH.LQBS, sin RESPALDADO ni MOTIVO> FROM RHH.BK_LQBS_C13_20261125;
--   (listar las columnas de RHH.LQBS explícitamente: SELECT column_name FROM all_tab_columns WHERE owner='RHH' AND table_name='LQBS' ORDER BY column_id)
-- El respaldo se conserva; borrarlo (DROP TABLE RHH.BK_LQBS_C13_20261125) solo cuando el usuario lo decida.
-- =====================================================================================
