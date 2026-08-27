-- =====================================================================
-- RRHH URGENTE: el periodo 2026 se acredito completo, debe ser proporcional
-- Modulo: RHH
-- Fecha:  2026-08-27
-- Autor:  orquestador
--
-- EL PROBLEMA
--   `acreditar` da los 15 dias COMPLETOS del periodo apenas este empieza.
--   A Katherine Pardo, que cumplio su primer año el 25/06/2026, le
--   acredito los 15 dias del periodo 25/06/2026 - 24/06/2027 con fecha de
--   corte 31/08/2026, cuando de ese periodo solo habian transcurrido dos
--   meses. Sumado a su saldo de migracion daba 22.75 dias.
--
--   El contador calcula 17.75 y tiene razon:
--     primer año cumplido (25/06/2025 - 24/06/2026)   15.00
--     proporcional        (25/06/2026 - 31/08/2026)    2.75
--                                                    -------
--                                                     17.75
--
-- EL DEFECTO DE FONDO: DOS MODELOS EN LA MISMA TABLA
--   Los 22 saldos de 2025 los creo la MIGRACION con modelo PROPORCIONAL
--   por año calendario, convencion 30/360. Verificado:
--     emp 58, ingreso 25/06/2025 -> 186 dias hasta fin de 2025
--             15 x 186/360 = 7.75  (coincide exacto con el dato)
--     emp 53, ingreso 06/08/2025 -> 145 dias hasta fin de 2025
--             15 x 145/360 = 6.04  (coincide exacto con el dato)
--   Y `acreditar` usa modelo de PERIODO COMPLETO por aniversario.
--   Mientras convivan los dos, los numeros no van a cuadrar nunca.
--
-- LA DECISION (contador, 2026-08-27)
--   Rige el PROPORCIONAL. Mas adelante sera parametrizable por empresa
--   para poder elegir entre proporcional y derecho a gozar; ese trabajo
--   va aparte y no lo cubre este script.
--
-- EL CALCULO QUE APLICA ESTE SCRIPT
--   Saldo 2026 = diasBase x (dias transcurridos del año calendario hasta
--                la fecha de corte, convencion 30/360) / 360
--   Al 31/08/2026 son 8 meses x 30 = 240 dias:  15 x 240/360 = 10.00
--   Los siete acreditados ingresaron en 2025, asi que a todos les
--   corresponde el año calendario completo hasta el corte: 10.00 dias.
--   Ninguno tiene dias adicionales por antiguedad (todos base 15).
--
--   Resultado por empleado (2025 no se toca + 2026 corregido):
--     45  7.71 + 10.00 = 17.71      53  6.04 + 10.00 = 16.04
--     56  7.75 + 10.00 = 17.75      57  7.75 + 10.00 = 17.75
--     58  7.75 + 10.00 = 17.75      62  6.88 + 10.00 = 16.88
--     65  7.75 + 10.00 = 17.75
--
--   El 17.75 de Katherine es exactamente el numero del contador.
--
-- TAMBIEN SE CORRIGE EL PERIODO DE LA FILA
--   `acreditar` grabo fechaInicio/fechaFin por ANIVERSARIO
--   (25/06/2026 - 24/06/2027). Bajo el modelo proporcional por año
--   calendario deben ser 01/01/2026 - 31/12/2026, como los de migracion.
--
-- OJO: ESTO NO ALCANZA SIN EL CAMBIO DE CODIGO
--   Arregla las 7 filas existentes. Mientras `acreditar` siga dando el
--   periodo completo, la proxima corrida vuelve a inflar los saldos.
--   NO VOLVER A CORRER /sldv/acreditar HASTA QUE EL CODIGO ESTE CORREGIDO.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: estado antes de tocar nada
--   Esperado: 7 filas de 2026 con 15 dias asignados y 15 pendientes.
-- ---------------------------------------------------------------------
SELECT S.MPLDCDGO, M.MPLDNMBR || ' ' || M.MPLDAPLL AS EMPLEADO,
       TO_CHAR(M.MPLDFCIN,'YYYY-MM-DD') AS INGRESO,
       S.SLDVASGN AS ASIGNADOS, S.SLDVDIAR AS ARRASTRADOS,
       S.SLDVUSDO AS USADOS, S.SLDVPNDE AS PENDIENTES,
       TO_CHAR(S.SLDVFCHI,'YYYY-MM-DD') AS DESDE,
       TO_CHAR(S.SLDVFCHF,'YYYY-MM-DD') AS HASTA
  FROM RHH.SLDV S JOIN RHH.MPLD M ON M.MPLDCDGO = S.MPLDCDGO
 WHERE S.SLDVANOO = 2026
 ORDER BY S.MPLDCDGO;

-- Nadie debe tener dias usados: si alguno los tiene, DETENERSE y avisar,
-- porque bajar los asignados por debajo de lo ya gozado deja saldo negativo.
SELECT COUNT(*) AS CON_DIAS_USADOS
  FROM RHH.SLDV WHERE SLDVANOO = 2026 AND NVL(SLDVUSDO,0) > 0;

-- ---------------------------------------------------------------------
-- BLOQUE 1: la correccion
--   240/360 = 8 meses transcurridos al 31/08/2026, sobre los dias base
--   de cada fila (15 para los siete, pero se calcula de SLDVASGN por si
--   alguno tuviera adicionales por antiguedad).
--   El WHERE excluye a quien tenga dias usados: defensivo.
--
--   OJO AL LEERLO: la segunda asignacion vuelve a calcular desde
--   SLDVASGN, no desde el valor recien puesto en la primera. Es
--   correcto: en un UPDATE de SQL todas las expresiones de la derecha
--   se evaluan contra la fila ORIGINAL, asi que ahi SLDVASGN todavia
--   vale 15 y las dos dan 10.00. No lo -corrijas- a SLDVPNDE = SLDVASGN.
-- ---------------------------------------------------------------------
UPDATE RHH.SLDV S
   SET S.SLDVASGN = ROUND(S.SLDVASGN * 240 / 360, 2),
       S.SLDVPNDE = ROUND(S.SLDVASGN * 240 / 360, 2) - NVL(S.SLDVUSDO,0),
       S.SLDVFCHI = DATE '2026-01-01',
       S.SLDVFCHF = DATE '2026-12-31'
 WHERE S.SLDVANOO = 2026
   AND NVL(S.SLDVUSDO,0) = 0;

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   (a) los 7 deben quedar con 10.00 asignados y 10.00 pendientes,
--       y periodo 2026-01-01 / 2026-12-31
--   (b) el total de 2026 debe pasar de 105.00 a 70.00
--   (c) Katherine (58) debe quedar en 7.75 + 10.00 = 17.75
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
