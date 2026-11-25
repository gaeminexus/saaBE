-- =====================================================================================
-- omen1-06 — MEDICIÓN (SOLO LECTURA) de lo ya afectado por los defectos #3, #4, #5 y #13 del informe de VCN (lote 11, RRHH dinero)
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción.
-- Columnas tomadas de las entidades (model/rhh): PYIR, ACMN, NMNA, PRDN, LQDC, LQBS, MPLD, CNTE.
-- Constantes: ACMN tipo 2 = GRAVADO_IR, 9 = RETENCION_IR · NMNA estado 5 ANULADA, 6 EXCLUIDA · PRDN estado 1 ABIERTO … 7 CERRADO ·
--   LQDC estado 6 = ANULADA · LQBS tipo 1 = D13, 2 = D14, 3 = FR; estado 1 = calculada, 2 = pagada · MPLD estado 4 = CESANTE.
-- PARAM: AÑO = 2026 (cambiar en cada bloque).
-- =====================================================================================

-- ============================ #3 RENTA ===============================================
-- 3a. ¿Importa en dinero? Retención de renta acumulada del año por empleado (ACMN tipo 9). En producción hasta agosto fue 0 en las 24 personas
-- (ESTADO-RRHH.md): si sigue en 0, los defectos (a), (b) y (c) no han costado dinero a nadie, solo evitan que cueste cuando alguien pase la fracción básica.
SELECT COUNT(*) AS empleados_con_retencion, ROUND(NVL(SUM(v), 0), 2) AS retencion_total_anio
  FROM (SELECT a.MPLDCDGO, SUM(NVL(a.ACMNVLOR, 0)) AS v FROM RHH.ACMN a WHERE a.ACMNANOO = 2026 AND a.ACMNTPAC = 9 GROUP BY a.MPLDCDGO HAVING SUM(NVL(a.ACMNVLOR, 0)) > 0);

-- 3b. (a) Proyecciones vigentes hechas desde un mes distinto del último período calculado: la retención de ese mes salió de una proyección de otro mes.
SELECT p.MPLDCDGO AS empleado, p.PYIRMSDS AS proyeccion_desde_mes, p.PYIRRTEM AS retencion_mensual, p.PYIRMSRS AS meses_restantes, p.PYIRIMPG AS impuesto_a_pagar,
       (SELECT MAX(d.PRDNMSEE) FROM RHH.NMNA n JOIN RHH.PRDN d ON d.PRDNCDGO = n.PRDNCDGO WHERE n.MPLDCDGO = p.MPLDCDGO AND d.PRDNANOO = p.PYIRANOO
           AND d.PRDNESTD >= 3 AND n.NMNAESTD NOT IN (5, 6)) AS ultimo_mes_calculado
  FROM RHH.PYIR p WHERE p.PYIRANOO = 2026 AND p.PYIRVGNT = 'S'
   AND p.PYIRMSDS <> NVL((SELECT MAX(d.PRDNMSEE) FROM RHH.NMNA n JOIN RHH.PRDN d ON d.PRDNCDGO = n.PRDNCDGO WHERE n.MPLDCDGO = p.MPLDCDGO AND d.PRDNANOO = p.PYIRANOO
           AND d.PRDNESTD >= 3 AND n.NMNAESTD NOT IN (5, 6)), p.PYIRMSDS)
 ORDER BY p.PYIRRTEM DESC;

-- 3c. (c) Quienes entran o salen en el año con una proyección que cuenta más meses que los del contrato.
SELECT p.MPLDCDGO AS empleado, c.CNTEFCHI AS contrato_desde, c.CNTEFCHF AS contrato_hasta, p.PYIRMSDS AS desde_mes, p.PYIRMSRS AS meses_restantes_proyectados,
       CASE WHEN c.CNTEFCHF IS NOT NULL AND EXTRACT(YEAR FROM c.CNTEFCHF) = 2026 THEN EXTRACT(MONTH FROM c.CNTEFCHF) - p.PYIRMSDS + 1 ELSE 12 - GREATEST(p.PYIRMSDS, CASE WHEN EXTRACT(YEAR FROM c.CNTEFCHI) = 2026 THEN EXTRACT(MONTH FROM c.CNTEFCHI) ELSE 1 END) + 1 END AS meses_del_contrato,
       p.PYIRRTEM AS retencion_mensual
  FROM RHH.PYIR p JOIN RHH.CNTE c ON c.MPLDCDGO = p.MPLDCDGO
 WHERE p.PYIRANOO = 2026 AND p.PYIRVGNT = 'S'
   AND ((EXTRACT(YEAR FROM c.CNTEFCHI) = 2026 AND EXTRACT(MONTH FROM c.CNTEFCHI) > p.PYIRMSDS) OR (c.CNTEFCHF IS NOT NULL AND EXTRACT(YEAR FROM c.CNTEFCHF) = 2026))
 ORDER BY p.MPLDCDGO;

-- ============================ #4 FINIQUITO ===========================================
-- 4a. Remuneración pendiente pagada dos veces: liquidaciones (no anuladas) de quien YA tiene nómina vigente en el mes de la salida
-- (el finiquito cuenta los días 1..día de salida aunque el rol de ese mes ya los pagó). dias_rol = NMNADITR de esa nómina.
SELECT q.LQDCCDGO AS liquidacion, q.MPLDCDGO AS empleado, q.LQDCFCHS AS salida, q.LQDCESTD AS estado_liq, d.PRDNMSEE AS mes_rol, d.PRDNESTD AS estado_periodo,
       n.NMNAESTD AS estado_nomina, n.NMNADITR AS dias_rol, c.CNTESLRB AS salario_base,
       ROUND(c.CNTESLRB * LEAST(EXTRACT(DAY FROM q.LQDCFCHS), 30) / 30, 2) AS remuneracion_pendiente_aprox
  FROM RHH.LQDC q
  JOIN RHH.NMNA n ON n.MPLDCDGO = q.MPLDCDGO AND n.NMNAESTD NOT IN (5, 6)
  JOIN RHH.PRDN d ON d.PRDNCDGO = n.PRDNCDGO AND d.PRDNANOO = EXTRACT(YEAR FROM q.LQDCFCHS) AND d.PRDNMSEE = EXTRACT(MONTH FROM q.LQDCFCHS)
  JOIN RHH.CNTE c ON c.CNTECDGO = q.CNTECDGO
 WHERE NVL(q.LQDCESTD, 0) <> 6
 ORDER BY q.LQDCFCHS DESC;

-- 4b. Nóminas vigentes de personas liquidadas en períodos que EMPIEZAN después de su salida (siguen en los períodos abiertos).
SELECT q.LQDCCDGO AS liquidacion, q.MPLDCDGO AS empleado, q.LQDCFCHS AS salida, d.PRDNCDGO AS periodo, d.PRDNANOO AS anio, d.PRDNMSEE AS mes, d.PRDNESTD AS estado_periodo,
       n.NMNAESTD AS estado_nomina, n.NMNANETO AS neto
  FROM RHH.LQDC q JOIN RHH.NMNA n ON n.MPLDCDGO = q.MPLDCDGO AND n.NMNAESTD NOT IN (5, 6)
  JOIN RHH.PRDN d ON d.PRDNCDGO = n.PRDNCDGO AND d.PRDNFCHI > q.LQDCFCHS
 WHERE NVL(q.LQDCESTD, 0) <> 6
 ORDER BY q.LQDCFCHS DESC, d.PRDNFCHI;

-- ============================ #5 FONDO DE RESERVA ====================================
-- 5a. Filas pagadas (estado 2) cuyo valor ya no coincide con lo pagado: señal de una fila pisada por un recálculo después de pagarla. Todos los tipos
-- (el defecto del FR es el mismo que el D13/D14 tenían antes de A0). valor_pagado = LQBSVLPG.
SELECT l.LQBSCDGO, l.MPLDCDGO AS empleado, l.LQBSTPBN AS tipo, l.LQBSANOO AS anio, l.LQBSVLRR AS valor, l.LQBSVLPG AS valor_pagado, ROUND(l.LQBSVLRR - NVL(l.LQBSVLPG, 0), 2) AS diferencia,
       l.LQBSODBS AS orden, l.LQBSFCPG AS fecha_pago
  FROM RHH.LQBS l WHERE l.LQBSESTD = 2 AND ABS(NVL(l.LQBSVLRR, 0) - NVL(l.LQBSVLPG, 0)) > 0.01 ORDER BY l.LQBSTPBN, ABS(l.LQBSVLRR - NVL(l.LQBSVLPG, 0)) DESC;
-- 5b. FR en una orden y sin pagar (el recálculo las pisa hoy): cuántas hay.
SELECT l.LQBSANOO AS anio, COUNT(*) AS filas_en_orden_sin_pagar, ROUND(SUM(NVL(l.LQBSVLRR, 0)), 2) AS valor
  FROM RHH.LQBS l WHERE l.LQBSTPBN = 3 AND l.LQBSODBS IS NOT NULL AND NVL(l.LQBSESTD, 0) <> 2 GROUP BY l.LQBSANOO ORDER BY 1;

-- ============================ #13 FILAS OBSOLETAS ====================================
-- 13a. Beneficios calculados, sin pagar y sin orden de personas que ya no corresponden (CESANTE o sin contrato en el año): son las que se retirarían.
SELECT l.LQBSCDGO, l.MPLDCDGO AS empleado, e.MPLDESTD AS estado_empleado, l.LQBSTPBN AS tipo, l.LQBSANOO AS anio, l.LQBSVLRR AS valor, l.LQBSANTC AS anticipado, l.LQBSVLPG AS valor_pagado
  FROM RHH.LQBS l JOIN RHH.MPLD e ON e.MPLDCDGO = l.MPLDCDGO
 WHERE NVL(l.LQBSESTD, 0) <> 2 AND l.LQBSODBS IS NULL
   AND (e.MPLDESTD = 4 OR NOT EXISTS (SELECT 1 FROM RHH.CNTE c WHERE c.MPLDCDGO = l.MPLDCDGO AND c.CNTEFCHI <= DATE '2026-11-30'
                                        AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-01-01')))
 ORDER BY l.LQBSTPBN, l.MPLDCDGO;
-- 13b. Posible doble pago: personas con finiquito (no anulado) que además tienen un D13 o D14 del mismo año, sea cual sea su estado. Si estado = 2 o hay orden,
-- ya salió dinero por la vía del beneficio Y por el finiquito: revisar contra el rubro del décimo en el acta.
SELECT q.LQDCCDGO AS liquidacion, q.MPLDCDGO AS empleado, q.LQDCFCHS AS salida, l.LQBSTPBN AS tipo, l.LQBSANOO AS anio, l.LQBSESTD AS estado_beneficio, l.LQBSODBS AS orden,
       l.LQBSVLRR AS valor, l.LQBSVLPG AS valor_pagado
  FROM RHH.LQDC q JOIN RHH.LQBS l ON l.MPLDCDGO = q.MPLDCDGO AND l.LQBSTPBN IN (1, 2) AND l.LQBSANOO IN (EXTRACT(YEAR FROM q.LQDCFCHS), EXTRACT(YEAR FROM q.LQDCFCHS) + 1)
 WHERE NVL(q.LQDCESTD, 0) <> 6 AND l.LQBSFCHI <= q.LQDCFCHS + 1 AND l.LQBSFCHF >= q.LQDCFCHS
 ORDER BY q.LQDCFCHS DESC, l.LQBSTPBN;
