-- =====================================================================
-- e2-33 — Cuanto dar de baja de la provision de vacaciones por agosto 2026
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. Devuelve los importes; el asiento se registra en la pantalla
--    de asientos de CNT (dos lineas). No se inserta en CNT.ASNT/DTAS por script
--    a proposito: saveSingle asigna periodo contable, numero y numero alterno
--    con reglas propias, y replicarlas a mano es la clase de cosa que hoy ya
--    fallo dos veces por columnas inventadas.
--
-- 🔴 POR QUE EXISTE
--   El usuario quiere cerrar agosto YA con la provision de vacaciones dada de
--   baja por los dias que se tomaron en agosto, y desplegar despues la
--   correccion del sistema. En 8/2026 no hay renglon de vacaciones (e2-30/e2-32),
--   asi que el valor no existe en el rol: hay que calcularlo como lo habria
--   calculado el sistema y asentarlo a mano.
--
-- LA FORMULA, COPIADA DEL CODIGO (AcreditacionVacacionesServiceImpl.valorDiaVacaciones)
--   valor = dias_de_la_solicitud x tarifa_dia
--   tarifa_dia = SUM(PVNM.PVNMBSCL tipo 3) / SUM(ACMN.ACMNDIAS tipo 10)
--                sobre los periodos CERRADOS de los 12 meses que terminan en el
--                mes de la solicitud (para agosto: 2025-09 .. 2026-08, y de esos
--                estan cerrados solo 2026-01 .. 2026-07).
--   ⚠️ Con menos de 12 meses de historia el sistema ADEMAS pondera esa tarifa
--   con la del saldo de apertura (RHH.SLDV) por dias. Eso NO se replica aca:
--   es un promedio ponderado con reglas propias, y para un asiento de cierre
--   la tarifa de ventana es defendible. Puede diferir por centavos de lo que
--   el sistema calcule el dia que se aprueben las solicitudes por pantalla.
-- =====================================================================


-- =====================================================================
-- 1 — Por empleado: dias tomados en agosto, tarifa y valor a dar de baja.
-- =====================================================================
-- Toma TODAS las solicitudes que tocan agosto, aprobadas o no (SLCTAPRB): la
-- baja es por las vacaciones efectivamente tomadas, no por el tramite.
-- ⚠️ Si una solicitud cruza de julio a agosto o de agosto a septiembre, cuenta
--    con todos sus dias: ajustar a mano los dias de esa fila si hace falta.
-- ⚠️ Si este punto devuelve 0 filas, no hay solicitudes cargadas: pasarle al
--    arbitro la lista (empleado, dias) y se calcula con el punto 2.
WITH ventana AS (
  SELECT p.PRDNCDGO
    FROM RHH.PRDN p
   WHERE p.PRDNESTD = 7
     AND (p.PRDNANOO * 100 + p.PRDNMSEE) BETWEEN 202509 AND 202608
),
base_emp AS (
  SELECT v.MPLDCDGO, SUM(NVL(v.PVNMBSCL, 0)) AS base_ventana
    FROM RHH.PVNM v
    JOIN ventana w ON w.PRDNCDGO = v.PRDNCDGO
   WHERE v.PVNMTPPR = 3
   GROUP BY v.MPLDCDGO
),
dias_emp AS (
  SELECT a.MPLDCDGO, SUM(NVL(a.ACMNDIAS, 0)) AS dias_ventana
    FROM RHH.ACMN a
    JOIN ventana w ON w.PRDNCDGO = a.PRDNCDGO
   WHERE a.ACMNTPAC = 10
   GROUP BY a.MPLDCDGO
),
tarifa AS (
  SELECT b.MPLDCDGO, b.base_ventana, d.dias_ventana,
         CASE WHEN NVL(d.dias_ventana, 0) > 0
              THEN ROUND(b.base_ventana / d.dias_ventana, 4) ELSE 0 END AS tarifa_dia
    FROM base_emp b
    LEFT JOIN dias_emp d ON d.MPLDCDGO = b.MPLDCDGO
)
SELECT '1 - baja por empleado' AS control,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       s.SLCTCDGO AS solicitud, s.SLCTFCHD AS desde, s.SLCTFCHH AS hasta,
       s.SLCTAPRB AS aprobada,
       s.SLCTDIAS AS dias,
       t.base_ventana, t.dias_ventana, t.tarifa_dia,
       ROUND(s.SLCTDIAS * t.tarifa_dia, 2) AS valor_a_dar_de_baja
  FROM RHH.SLCT s
  JOIN RHH.MPLD e ON e.MPLDCDGO = s.MPLDCDGO
  LEFT JOIN tarifa t ON t.MPLDCDGO = s.MPLDCDGO
 WHERE s.SLCTFCHD <= DATE '2026-08-31'
   AND s.SLCTFCHH >= DATE '2026-08-01'
 ORDER BY empleado;


-- =====================================================================
-- 2 — La tarifa diaria de TODOS los empleados, por si hay que calcular a mano
--     para alguien que no tiene solicitud cargada. valor = dias x tarifa_dia.
-- =====================================================================
WITH ventana AS (
  SELECT p.PRDNCDGO
    FROM RHH.PRDN p
   WHERE p.PRDNESTD = 7
     AND (p.PRDNANOO * 100 + p.PRDNMSEE) BETWEEN 202509 AND 202608
),
base_emp AS (
  SELECT v.MPLDCDGO, SUM(NVL(v.PVNMBSCL, 0)) AS base_ventana
    FROM RHH.PVNM v
    JOIN ventana w ON w.PRDNCDGO = v.PRDNCDGO
   WHERE v.PVNMTPPR = 3
   GROUP BY v.MPLDCDGO
),
dias_emp AS (
  SELECT a.MPLDCDGO, SUM(NVL(a.ACMNDIAS, 0)) AS dias_ventana
    FROM RHH.ACMN a
    JOIN ventana w ON w.PRDNCDGO = a.PRDNCDGO
   WHERE a.ACMNTPAC = 10
   GROUP BY a.MPLDCDGO
)
SELECT '2 - tarifa diaria por empleado' AS control,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       b.base_ventana, d.dias_ventana,
       CASE WHEN NVL(d.dias_ventana, 0) > 0
            THEN ROUND(b.base_ventana / d.dias_ventana, 4) ELSE 0 END AS tarifa_dia
  FROM base_emp b
  LEFT JOIN dias_emp d ON d.MPLDCDGO = b.MPLDCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = b.MPLDCDGO
 ORDER BY empleado;


-- =====================================================================
-- 3 — EL ASIENTO. Se registra a mano en CNT con el TOTAL del punto 1.
-- =====================================================================
-- Fecha: 31/08/2026 (mismo mes que el rol). Tipo: RECURSOS HUMANOS.
-- Observacion sugerida: "Baja de provision de vacaciones gozadas 8/2026 (manual)".
--
--   Cuenta      Nombre                              Debe        Haber
--   2.5.14      Provision vacaciones por pagar      TOTAL
--   4.3.01.05   Gasto sueldos y salarios                        TOTAL
--
-- Por que contra 4.3.01.05 y no otra: el rol de agosto ya mando ese sueldo de
-- vacaciones integro a gasto de sueldos (es donde cae un renglon de vacaciones
-- cuando no se reconoce como rol 36). La baja de la provision lo saca de ahi:
-- el gasto ya se reconocio mes a mes al provisionar, no corresponde dos veces.
-- Es exactamente el asiento que el sistema va a generar solo desde septiembre.
-- =====================================================================
