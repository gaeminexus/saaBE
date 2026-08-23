-- =====================================================
-- MODULO: RHH - ABRIL YA CERRADO: LA ULTIMA DEL MES
-- DESCRIPCION: Solo lectura. Se corre con el PRDN 41 en estado 7.
-- FECHA: 2026-08-23
-- PARAMETRO: ninguno
-- =====================================================
-- LOS ACUMULADOS SE ESCRIBEN SOLO AL CERRAR, y cerrarPeriodo es el unico
-- sitio que los toca. Por eso esta verificacion no podia ir antes.
-- =====================================================


-- =====================================================
-- CONTROL 1: LA CABECERA CERRADA Y SU OBSERVACION.
-- PRDNOBSR tiene que decir EXACTAMENTE:
--   Calculado sin contabilizacion (carga historica).
-- Es lo que escribe contabilizarRol. Si dijera otra cosa, cerrarPeriodo lo
-- piso con un aviso de novedades del IESS sin declarar, y abril NO debe
-- avisar: sus NVIS mas cercanas son del 6 de marzo.
-- Los tres campos de asiento en NULO.
-- =====================================================
SELECT PRDNCDGO, PRDNESTD AS ESTADO, PRDNMODO AS MODO, PRDNNMEM AS EMPLEADOS,
       PRDNASNT AS ASNT_ROL, PRDNASPR AS ASNT_PROVISION, PRDNASPG AS ASNT_PAGO,
       PRDNOBSR AS OBSERVACION,
       CASE WHEN PRDNESTD = 7
             AND PRDNASNT IS NULL AND PRDNASPR IS NULL AND PRDNASPG IS NULL
             AND PRDNOBSR = 'Calculado sin contabilizacion (carga historica).'
            THEN 'OK' ELSE '*** REVISAR ***' END AS VEREDICTO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 4;


-- =====================================================
-- CONTROL 2: LOS ACMN DEL PERIODO, POR TIPO.
-- Esperado: 6 tipos --1, 2, 3, 5, 8 y 10-- con 20 filas cada uno = 120.
-- El tipo 9 RETENCION_IR NO debe aparecer: Robayo ya no retiene.
-- Suma del tipo 8 APORTE_PERSONAL = 1.942,93. Tipo 10 = 600 dias.
-- =====================================================
SELECT a.ACMNTPAC AS TIPO,
       CASE a.ACMNTPAC WHEN 1 THEN 'IMPONIBLE IESS' WHEN 2 THEN 'GRAVADO IR'
                       WHEN 3 THEN 'BASE 13o'       WHEN 4 THEN 'BASE 14o'
                       WHEN 5 THEN 'BASE FDO RESERVA' WHEN 8 THEN 'APORTE PERSONAL'
                       WHEN 9 THEN 'RETENCION IR'   WHEN 10 THEN 'DIAS TRABAJADOS'
                       ELSE 'OTRO' END AS QUE_ES,
       COUNT(*) AS FILAS, COUNT(DISTINCT a.MPLDCDGO) AS PERSONAS,
       SUM(a.ACMNVLOR) AS VALOR, SUM(a.ACMNDIAS) AS DIAS
  FROM RHH.ACMN a
  JOIN RHH.PRDN p ON p.PRDNCDGO = a.PRDNCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
 GROUP BY a.ACMNTPAC
 ORDER BY 1;


-- =====================================================
-- CONTROL 3: LAS MISMAS PERSONAS EN ACMN QUE EN NMNA.
-- Los dos MINUS tienen que salir VACIOS. Es lo que delata que el cierre
-- dejo fuera a alguien, o que acumulo a quien no tenia nomina.
-- =====================================================
SELECT 'EN NMNA Y SIN ACMN' AS PROBLEMA, MPLDCDGO FROM (
    SELECT n.MPLDCDGO FROM RHH.NMNA n JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
     WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
    MINUS
    SELECT a.MPLDCDGO FROM RHH.ACMN a JOIN RHH.PRDN p ON p.PRDNCDGO = a.PRDNCDGO
     WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4)
UNION ALL
SELECT 'EN ACMN Y SIN NMNA', MPLDCDGO FROM (
    SELECT a.MPLDCDGO FROM RHH.ACMN a JOIN RHH.PRDN p ON p.PRDNCDGO = a.PRDNCDGO
     WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4
    MINUS
    SELECT n.MPLDCDGO FROM RHH.NMNA n JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
     WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 4);


-- =====================================================
-- CONTROL 4: EL TOTAL DEL ANIO, DESGLOSADO.
-- Un conteo sin desglosar da un numero que no se parece a nada y parece
-- un fallo. Esperado: 132 + 132 + 120 + 120 del periodo, y 46 SIN periodo
-- --34 de la apertura y 12 de los cuatro finiquitos, 3 cada uno--. 550.
-- Las 46 sin periodo NO se mueven este mes: abril no tiene salidas.
-- =====================================================
SELECT NVL(TO_CHAR(p.PRDNMSEE), 'SIN PERIODO') AS MES,
       COUNT(*) AS FILAS, COUNT(DISTINCT a.MPLDCDGO) AS PERSONAS
  FROM RHH.ACMN a
  LEFT JOIN RHH.PRDN p ON p.PRDNCDGO = a.PRDNCDGO
 GROUP BY ROLLUP(p.PRDNMSEE)
 ORDER BY p.PRDNMSEE NULLS LAST;


-- =====================================================
-- CONTROL 5: NINGUN ASIENTO DE RRHH. Base anotada en 8179.
-- El censo TOTAL de CNT.ASNT no vale: otros modulos escriben en paralelo,
-- y durante el cierre de febrero nacieron cinco asientos ajenos. Lo que
-- salga aqui no puede ser de RRHH; se comprueba mirando usuario y modulo.
-- =====================================================
SELECT ASNTCDGO, ASNTFCHA, ASNTNMRO, ASNTUSRO, SUBSTR(ASNTOBSR, 1, 80) AS OBSERVACION
  FROM CNT.ASNT WHERE ASNTCDGO > 8179 ORDER BY ASNTCDGO;
