-- =====================================================================
-- RRHH: recalcular el proporcional 2026 con corte al 27/08/2026
-- Modulo: RHH
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- QUE CAMBIA
--   El script 05 calculo el proporcional al 31/08/2026 (240 dias -> 10.00).
--   El contador lo quiere al dia de hoy, 27/08/2026.
--
-- LA CONVENCION DE DIAS, VERIFICADA CONTRA LOS DATOS REALES
--   dias = 30 x (mes2 - mes1) + (dia2 - dia1)
--   Reproduce al centavo los saldos que creo la migracion:
--     emp 58, ingreso 25/06/2025 -> 30x(12-6) + (31-25) = 186
--              15 x 186/360 = 7.75   (dato real: 7.75)
--     emp 53, ingreso 06/08/2025 -> 30x(12-8) + (31-6)  = 145
--              15 x 145/360 = 6.04   (dato real: 6.04)
--   Y un año completo da 30x11 + 30 = 360, como debe ser.
--
--   Del 01/01/2026 al 27/08/2026:  30x(8-1) + (27-1) = 236 dias
--   15 x 236/360 = 9.8333 -> 9.83
--
--   Katherine (58) queda en  7.75 + 9.83 = 17.58
--
-- >>> LA TRAMPA DE ESTE SCRIPT <<<
--   El script 05 YA dejo SLDVASGN = 10.00. Si se recalculara sobre ese
--   valor daria 10 x 236/360 = 6.56, que no significa nada.
--   Por eso aqui NO se parte de SLDVASGN: se reconstruye la base desde
--   RHH.PRNM (dias de vacaciones parametrizados) mas los dias
--   adicionales por antiguedad de cada fila. Asi el script es correcto
--   sin importar cuantas veces se haya recalculado antes, y se puede
--   volver a correr cambiando solo la fecha de corte.
--
-- OJO: ESTO NO ALCANZA SIN EL CAMBIO DE CODIGO
--   Mientras `acreditar` siga dando el periodo completo por aniversario,
--   la proxima corrida vuelve a inflar los saldos.
--   NO VOLVER A CORRER /sldv/acreditar HASTA QUE EL CODIGO ESTE CORREGIDO.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: estado actual y el calculo que se va a aplicar
--   Muestra, para cada fila de 2026, la base reconstruida, los dias que
--   le corresponden al 27/08 y lo que tiene hoy. Revisar antes de seguir.
-- ---------------------------------------------------------------------
SELECT S.MPLDCDGO,
       M.MPLDNMBR || ' ' || M.MPLDAPLL AS EMPLEADO,
       (P.PRNMDIVC + NVL(S.SLDVDIAD,0))                            AS BASE_ANUAL,
       (30 * (8 - 1) + (27 - 1))                                   AS DIAS_TRANSCURRIDOS,
       ROUND((P.PRNMDIVC + NVL(S.SLDVDIAD,0)) * 236 / 360, 2)      AS CORRESPONDE_AL_27AGO,
       S.SLDVASGN                                                  AS ASIGNADOS_HOY,
       S.SLDVUSDO                                                  AS USADOS,
       S.SLDVPNDE                                                  AS PENDIENTES_HOY
  FROM RHH.SLDV S
  JOIN RHH.MPLD M ON M.MPLDCDGO = S.MPLDCDGO
  JOIN RHH.PRNM P ON P.PRNMANOO = S.SLDVANOO
 WHERE S.SLDVANOO = 2026
 ORDER BY S.MPLDCDGO;

-- Defensivo: nadie debe tener dias usados. Si alguno los tiene, DETENERSE:
-- bajar los asignados por debajo de lo ya gozado deja saldo negativo.
SELECT COUNT(*) AS CON_DIAS_USADOS
  FROM RHH.SLDV WHERE SLDVANOO = 2026 AND NVL(SLDVUSDO,0) > 0;

-- ---------------------------------------------------------------------
-- BLOQUE 1: la correccion
--   236 dias = 30 x (8 - 1) + (27 - 1), del 01/01/2026 al 27/08/2026.
--   Para cambiar la fecha de corte mas adelante, recalcular ese numero
--   con la misma formula y reemplazarlo en las dos lineas.
-- ---------------------------------------------------------------------
UPDATE RHH.SLDV S
   SET S.SLDVASGN = (SELECT ROUND((P.PRNMDIVC + NVL(S.SLDVDIAD,0)) * 236 / 360, 2)
                       FROM RHH.PRNM P WHERE P.PRNMANOO = S.SLDVANOO),
       S.SLDVPNDE = (SELECT ROUND((P.PRNMDIVC + NVL(S.SLDVDIAD,0)) * 236 / 360, 2)
                       FROM RHH.PRNM P WHERE P.PRNMANOO = S.SLDVANOO)
                    - NVL(S.SLDVUSDO,0)
 WHERE S.SLDVANOO = 2026
   AND NVL(S.SLDVUSDO,0) = 0;

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   (a) los 7 deben quedar con 9.83 asignados y 9.83 pendientes
--   (b) el total de 2026 debe pasar de 70.00 a 68.81
--   (c) Katherine (58) debe quedar en 7.75 + 9.83 = 17.58
-- ---------------------------------------------------------------------
SELECT S.MPLDCDGO, S.SLDVASGN AS ASIGNADOS, S.SLDVPNDE AS PENDIENTES,
       TO_CHAR(S.SLDVFCHI,'YYYY-MM-DD') AS DESDE,
       TO_CHAR(S.SLDVFCHF,'YYYY-MM-DD') AS HASTA
  FROM RHH.SLDV S WHERE S.SLDVANOO = 2026 ORDER BY S.MPLDCDGO;

SELECT SLDVANOO AS ANIO, COUNT(*) AS SALDOS, SUM(SLDVPNDE) AS DIAS_PENDIENTES
  FROM RHH.SLDV GROUP BY SLDVANOO ORDER BY 1;

SELECT SUM(SLDVPNDE) AS DISPONIBLE_KATHERINE
  FROM RHH.SLDV WHERE MPLDCDGO = 58 AND NVL(SLDVCDCD,'N') <> 'S';

COMMIT;
