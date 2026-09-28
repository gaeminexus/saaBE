-- =====================================================================================
-- e3-03 — Por que algunos colaboradores activos no tienen saldo de vacaciones 2026
-- Equipo omen-saa-3 · 2026-09-28 · SOLO LECTURA (ningun INSERT/UPDATE/DELETE)
--
-- Reporte: en «Nueva solicitud de vacaciones» algunos trabajadores muestran solo el
-- anio 2025 (p.ej. 1715156574 COSSIO CAICEDO EIMY: 2025 asignados 3.54, sin fila 2026).
--
-- Lo que dice el codigo (AcreditacionVacacionesServiceImpl.acreditar, POST /sldv/acreditar):
--   * Es un proceso MANUAL con UNA sola fecha de corte para toda la empresa.
--   * Crea la fila del anio de la fecha de corte SOLO para quien tiene >= 1 anio de
--     servicio cumplido A ESA FECHA (desde MPLDFCIN; si es nula, desde CNTEFCHI).
--   * Quien a la fecha de corte todavia no cumplia su primer anio queda FUERA de 2026, y
--     no entra nunca mas salvo que se vuelva a correr el proceso con un corte posterior
--     a su aniversario.
-- Hipotesis: los que «solo tienen 2025» ingresaron en 2025 (su 2025 es un saldo de
-- apertura proporcional) y la acreditacion 2026 se corrio antes de su primer aniversario.
-- Este script confirma o descarta eso. Pegar la salida COMPLETA de los 4 bloques.
-- =====================================================================================

-- BLOQUE 1 — Cuando y con que se acredito 2026 (SLDVFCHR = dia en que se creo la fila)
SELECT s.SLDVFCHR           AS FECHA_CREACION,
       s.SLDVUSRR           AS USUARIO,
       NVL(s.SLDVAPRT, 'N') AS ES_APERTURA,
       COUNT(*)             AS FILAS,
       MIN(s.SLDVFCHI)      AS MIN_INICIO_PERIODO,
       MAX(s.SLDVFCHI)      AS MAX_INICIO_PERIODO
  FROM RHH.SLDV s
 WHERE s.SLDVANOO = 2026
 GROUP BY s.SLDVFCHR, s.SLDVUSRR, NVL(s.SLDVAPRT, 'N')
 ORDER BY s.SLDVFCHR;

-- BLOQUE 2 — Colaboradores ACTIVOS (MPLDESTD = 1) sin fila 2026, con su causa probable
SELECT m.MPLDCDGO                                   AS ID_EMPLEADO,
       m.MPLDIDNT                                   AS IDENTIFICACION,
       m.MPLDAPLL || ' ' || m.MPLDNMBR              AS NOMBRE,
       m.MPLDFCIN                                   AS FECHA_INGRESO,
       (SELECT MIN(c.CNTEFCHI) FROM RHH.CNTE c
         WHERE c.MPLDCDGO = m.MPLDCDGO)             AS PRIMER_CONTRATO,
       ADD_MONTHS(m.MPLDFCIN, 12)                   AS PRIMER_ANIVERSARIO,
       s25.SLDVASGN                                 AS ASIGNADOS_2025,
       s25.SLDVPNDE                                 AS PENDIENTES_2025,
       NVL(s25.SLDVAPRT, '-')                       AS APERTURA_2025,
       CASE
         WHEN m.MPLDFCIN IS NULL                        THEN 'A: SIN FECHA DE INGRESO (usa el contrato)'
         WHEN ADD_MONTHS(m.MPLDFCIN, 12) > TRUNC(SYSDATE) THEN 'B: AUN NO CUMPLE SU PRIMER ANIO'
         ELSE                                                'C: YA CUMPLIO EL ANIO Y NO SE LE ACREDITO'
       END                                          AS CAUSA
  FROM RHH.MPLD m
  LEFT JOIN RHH.SLDV s25 ON s25.MPLDCDGO = m.MPLDCDGO AND s25.SLDVANOO = 2025
 WHERE m.MPLDESTD = 1
   AND NOT EXISTS (SELECT 1 FROM RHH.SLDV s
                    WHERE s.MPLDCDGO = m.MPLDCDGO AND s.SLDVANOO = 2026)
 ORDER BY CAUSA, m.MPLDFCIN;

-- BLOQUE 3 — Resumen por causa
SELECT CASE
         WHEN m.MPLDFCIN IS NULL                        THEN 'A: SIN FECHA DE INGRESO'
         WHEN ADD_MONTHS(m.MPLDFCIN, 12) > TRUNC(SYSDATE) THEN 'B: AUN NO CUMPLE SU PRIMER ANIO'
         ELSE                                                'C: YA CUMPLIO Y NO SE LE ACREDITO'
       END      AS CAUSA,
       COUNT(*) AS EMPLEADOS
  FROM RHH.MPLD m
 WHERE m.MPLDESTD = 1
   AND NOT EXISTS (SELECT 1 FROM RHH.SLDV s
                    WHERE s.MPLDCDGO = m.MPLDCDGO AND s.SLDVANOO = 2026)
 GROUP BY CASE
         WHEN m.MPLDFCIN IS NULL                        THEN 'A: SIN FECHA DE INGRESO'
         WHEN ADD_MONTHS(m.MPLDFCIN, 12) > TRUNC(SYSDATE) THEN 'B: AUN NO CUMPLE SU PRIMER ANIO'
         ELSE                                                'C: YA CUMPLIO Y NO SE LE ACREDITO'
       END;

-- BLOQUE 4 — El caso reportado, completo
SELECT m.MPLDCDGO, m.MPLDIDNT, m.MPLDFCIN, m.MPLDESTD,
       s.SLDVANOO, s.SLDVASGN, s.SLDVUSDO, s.SLDVPNDE, s.SLDVDIAR,
       s.SLDVFCHI, s.SLDVFCHF, s.SLDVCDCD, s.SLDVAPRT, s.SLDVFCHR, s.SLDVUSRR
  FROM RHH.MPLD m
  LEFT JOIN RHH.SLDV s ON s.MPLDCDGO = m.MPLDCDGO
 WHERE m.MPLDIDNT = '1715156574'
 ORDER BY s.SLDVANOO;
