-- =====================================================================
-- e2-27 — Por que el decimo cuarto sale con 210 dias y no con 360
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- 🔴 POR QUE EXISTE
--   El usuario calculo el decimo cuarto 2026 (Sierra y Amazonia) y ningun
--   empleado supera los 210 dias, cuando la ventana es 01/08/2025 - 31/07/2026
--   y deberia llegar a 360.
--
--   210 = 7 meses x 30 dias = enero a julio de 2026 EXACTAMENTE. O sea que el
--   calculo esta sumando solo lo que hay desde enero, no desde agosto.
--
--   Y NO es un defecto del calculo: se leyo el codigo antes de escribir esto.
--     - BeneficioSocialServiceImpl.calcularDecimoCuarto arma bien la ventana:
--       desde = (anio-1)/08/01, hasta = anio/07/31 para Sierra. La pantalla lo
--       confirma: muestra "01/08/2025 - 31/07/2026".
--     - AcumuladoNominaDaoServiceImpl.sumaDiasRango compara por
--       (anio * 100 + mes) BETWEEN 202508 AND 202607, que cruza bien el cambio
--       de anio.
--
--   La hipotesis, entonces, es que en RHH.ACMN NO EXISTEN filas de dias
--   trabajados para agosto a diciembre de 2025: la nomina en el sistema arranca
--   en enero de 2026, y esos cinco meses tendrian que haber entrado por la
--   carga de apertura.
--
--   Este script lo mide. Si la hipotesis es correcta, el arreglo NO es tocar el
--   calculo -sumaria dias que nadie registro- sino cargar esos acumulados de
--   apertura, que es una decision del usuario sobre datos historicos.
--
-- Tipo de acumulado 10 = DIAS_TRABAJADOS (RhhTipoAcumulado:31).
-- =====================================================================


-- =====================================================================
-- 1 — Que meses de dias trabajados existen, y cuantos dias tiene cada uno
-- =====================================================================
-- ESPERADO si la hipotesis es correcta: filas solo de 2026, meses 1 a 7.
-- 🔴 Si NO aparece ningun 2025/08 .. 2025/12, esa es la causa y esta confirmada.
SELECT '1 - meses con dias trabajados' AS control,
       a.ACMNANOO AS anio, a.ACMNMSEE AS mes,
       COUNT(*)      AS empleados,
       SUM(a.ACMNDIAS) AS suma_dias,
       AVG(a.ACMNDIAS) AS promedio_dias
  FROM RHH.ACMN a
 WHERE a.ACMNTPAC = 10
   AND a.ACMNESTD = 1
   AND ((a.ACMNANOO * 100) + a.ACMNMSEE) BETWEEN 202501 AND 202612
 GROUP BY a.ACMNANOO, a.ACMNMSEE
 ORDER BY a.ACMNANOO, a.ACMNMSEE;


-- =====================================================================
-- 2 — La ventana exacta del decimo cuarto de la Sierra, empleado por empleado
-- =====================================================================
-- Es literalmente lo que suma el calculo: (anio*100+mes) entre 202508 y 202607.
-- ESPERADO si todo estuviera bien: 360 dias para quien trabajo el año completo.
-- Lo que el usuario ve hoy: 210.
SELECT '2 - dias por empleado en la ventana' AS control,
       e.MPLDIDNT AS identificacion,
       e.MPLDAPLL || ' ' || e.MPLDNMBR AS empleado,
       COUNT(*)        AS meses_con_datos,
       MIN((a.ACMNANOO * 100) + a.ACMNMSEE) AS primer_mes,
       MAX((a.ACMNANOO * 100) + a.ACMNMSEE) AS ultimo_mes,
       SUM(a.ACMNDIAS) AS dias_que_suma_el_calculo
  FROM RHH.ACMN a
  JOIN RHH.MPLD e ON e.MPLDCDGO = a.MPLDCDGO
 WHERE a.ACMNTPAC = 10
   AND a.ACMNESTD = 1
   AND ((a.ACMNANOO * 100) + a.ACMNMSEE) BETWEEN 202508 AND 202607
 GROUP BY e.MPLDIDNT, e.MPLDAPLL, e.MPLDNMBR
 ORDER BY dias_que_suma_el_calculo DESC, empleado;


-- =====================================================================
-- 3 — ¿Hay ALGO cargado de 2025, de cualquier tipo de acumulado?
-- =====================================================================
-- Distingue dos causas muy distintas:
--   - Si no hay NADA de 2025: la carga de apertura no se hizo, o no llego a
--     esos meses. Es un tema de datos historicos.
--   - Si hay de otros tipos pero NO del tipo 10: la apertura cargo bases
--     (imponible, decimos) pero NO los dias trabajados. Es un hueco puntual de
--     la carga, y el decimo cuarto es el unico que se prorratea por dias.
SELECT '3 - acumulados de 2025 por tipo' AS control,
       a.ACMNTPAC AS tipo_acumulado,
       CASE a.ACMNTPAC
         WHEN 1 THEN 'IMPONIBLE IESS'      WHEN 2 THEN 'GRAVADO IR'
         WHEN 3 THEN 'BASE DECIMO TERCERO' WHEN 4 THEN 'BASE DECIMO CUARTO'
         WHEN 5 THEN 'BASE FONDOS RESERVA' WHEN 6 THEN 'BASE UTILIDADES'
         WHEN 7 THEN 'BASE VACACIONES'     WHEN 8 THEN 'APORTE PERSONAL'
         WHEN 9 THEN 'RETENCION IR'        WHEN 10 THEN 'DIAS TRABAJADOS'
         ELSE 'OTRO' END AS descripcion,
       COUNT(*) AS filas,
       MIN((a.ACMNANOO * 100) + a.ACMNMSEE) AS primer_mes,
       MAX((a.ACMNANOO * 100) + a.ACMNMSEE) AS ultimo_mes
  FROM RHH.ACMN a
 WHERE a.ACMNANOO = 2025
 GROUP BY a.ACMNTPAC
 ORDER BY a.ACMNTPAC;


-- =====================================================================
-- 4 — Desde cuando corre la nomina en el sistema
-- =====================================================================
-- Confirma la hipotesis por el otro lado: si el primer periodo de nomina es
-- 2026/01, no puede haber acumulados de 2025 generados por el sistema.
SELECT '4 - periodos de nomina existentes' AS control,
       p.PRDNANOO AS anio, p.PRDNMSEE AS mes, p.PRDNESTD AS estado
  FROM RHH.PRDN p
 ORDER BY p.PRDNANOO, p.PRDNMSEE;


-- =====================================================================
-- QUE SIGUE, segun lo que devuelva
-- =====================================================================
-- CASO A — El punto 1 no muestra 2025/08..2025/12 y el punto 4 arranca en
--          2026/01. Hipotesis confirmada: el calculo esta bien y lo que falta
--          son los dias trabajados de agosto a diciembre de 2025, que debian
--          entrar por la carga de apertura. NO se toca el calculo: se cargan
--          esos acumulados, y eso lo decide el usuario porque son datos
--          historicos de antes del sistema.
--
-- CASO B — El punto 3 muestra otros tipos de 2025 pero ninguno del tipo 10.
--          La apertura cargo las bases y se salteo los dias trabajados. Mismo
--          arreglo, pero ademas hay que revisar el proceso de apertura para que
--          no vuelva a pasar el año que viene.
--
-- CASO C — SI hay dias trabajados de 2025/08..2025/12 y aun asi el calculo
--          suma 210. Entonces la hipotesis es falsa y el defecto SI esta en el
--          codigo: avisar de inmediato con el resultado del punto 2, que
--          muestra el primer y ultimo mes que entra por empleado.
-- =====================================================================
