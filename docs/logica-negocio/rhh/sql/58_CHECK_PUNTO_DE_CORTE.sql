-- ============================================================================
-- 58 - CHECK DE MESES CERRADOS: LAS PROVISIONES DE FONDOS DE RESERVA
-- ==
-- Reescrito el 2026-08-25, con junio ya cerrado. La primera version comparaba
-- todo contra 183,26 y solo levantaba los meses 1 a 5: los dos numeros estaban
-- QUEMADOS. Junio vale 30,54 sobre base 366,67, asi que ampliarla habria dado
-- una alarma falsa, y julio traera un tercer valor distinto.
-- ==
-- QUE MIDE. Que ningun mes ya cerrado se haya recalculado. Con el WAR del 22 y
-- el 10, recalcular un mes cambia o BORRA su provision de fondo de reserva:
-- calcularPeriodo llama a eliminaByPeriodo ANTES de reescribir, y
-- generaProvision no escribe nada cuando el valor es 0. Y no toca el liquido,
-- porque PVNM no esta en el rol: ningun total lo delata. Esta consulta es la
-- unica que lo ve.
-- ==
-- COMO SE MANTIENE. Al cerrar cada mes se anade una linea a la lista ESPERADO
-- del bloque 1 Y el mes al NOT IN del bloque 1 BIS. SON LAS DOS, y el bloque
-- 1 BIS existe precisamente para avisar si alguien se olvida de la primera.
-- ==
-- Solo lee. No modifica nada. Se corre en PRODUCCION y en LOCAL.
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - Cada mes cerrado contra lo que cerro. LA LISTA ES EL CONTRATO.
-- ==
-- El LEFT JOIN contra la lista es deliberado: si un mes se recalculo, su fila
-- no cambia de valor, SE VA. Un GROUP BY a secas la omitiria en silencio.
-- ==
-- ESPERADO: todas las filas en INTACTO.
--   enero a mayo   1 persona   183,26   sobre base 2 200,00  (mes completo)
--   junio          1 persona    30,54   sobre base   366,67  (5 dias desde el
--                                                             aniversario)
-- La base de junio es distinta porque el fondo de reserva se devenga desde el
-- aniversario: Viteri cumplio el ano el 25-06 y le corresponden 5 dias.
-- ============================================================================
WITH ESPERADO (MES, PERSONAS, VALOR) AS (
    SELECT 1, 1, 183.26 FROM DUAL UNION ALL
    SELECT 2, 1, 183.26 FROM DUAL UNION ALL
    SELECT 3, 1, 183.26 FROM DUAL UNION ALL
    SELECT 4, 1, 183.26 FROM DUAL UNION ALL
    SELECT 5, 1, 183.26 FROM DUAL UNION ALL
    SELECT 6, 1,  30.54 FROM DUAL
)
SELECT e.MES,
       COUNT(x.PVNMCDGO)                     AS FILAS,
       COUNT(DISTINCT x.MPLDCDGO)            AS PERSONAS,
       e.PERSONAS                            AS PERSONAS_ESP,
       NVL(SUM(x.PVNMVLOR), 0)               AS VALOR,
       e.VALOR                               AS VALOR_ESP,
       CASE
         WHEN COUNT(x.PVNMCDGO) = 0                          THEN 'ALARMA - MES RECALCULADO O VACIO'
         WHEN COUNT(DISTINCT x.MPLDCDGO) <> e.PERSONAS       THEN 'ALARMA - CAMBIO EL NUMERO DE PERSONAS'
         WHEN ROUND(NVL(SUM(x.PVNMVLOR), 0), 2) <> e.VALOR   THEN 'ALARMA - CAMBIO EL VALOR'
         ELSE                                                     'INTACTO'
       END                                   AS VEREDICTO
  FROM ESPERADO e
  LEFT JOIN (SELECT p.PRDNMSEE AS MES, v.PVNMCDGO, v.MPLDCDGO, v.PVNMVLOR
               FROM RHH.PVNM v
               JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
              WHERE p.PRDNANOO = 2026
                AND v.PVNMTPPR = 4) x
    ON x.MES = e.MES
 GROUP BY e.MES, e.PERSONAS, e.VALOR
 ORDER BY e.MES;

-- ============================================================================
-- BLOQUE 1 BIS - QUE NO HAYA APARECIDO UN MES QUE LA LISTA NO CONTEMPLA.
-- ==
-- El bloque 1 solo puede ver los meses que alguien escribio en su lista. Un
-- mes con provision de fondo de reserva que NO este en ella no saldria por
-- ningun lado: el control seria ciego justo donde deja de estar mantenido.
-- ==
-- ESPERADO: vacio, hasta que julio cierre. Cuando julio cierre, esta consulta
-- devolvera julio, y eso es el AVISO de que hay que anadir su linea al
-- bloque 1, no una alarma.
-- ============================================================================
SELECT p.PRDNMSEE AS MES_NO_CONTEMPLADO,
       COUNT(*) AS FILAS, COUNT(DISTINCT v.MPLDCDGO) AS PERSONAS,
       SUM(v.PVNMVLOR) AS VALOR
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
 WHERE p.PRDNANOO = 2026
   AND v.PVNMTPPR = 4
   AND p.PRDNMSEE NOT IN (1, 2, 3, 4, 5, 6)
 GROUP BY p.PRDNMSEE
 ORDER BY 1;

-- ============================================================================
-- BLOQUE 2 - Quien es y sobre que base, para que la fila diga un nombre.
-- ESPERADO: seis filas, las seis VITERI LOPEZ. Enero a mayo con base 2 200,00
-- y 183,26; junio con base 366,67 y 30,54.
-- ============================================================================
SELECT p.PRDNMSEE AS MES, e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       v.PVNMBSCL AS BASE, v.PVNMVLOR AS VALOR
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = v.MPLDCDGO
 WHERE p.PRDNANOO = 2026
   AND v.PVNMTPPR = 4
 ORDER BY p.PRDNMSEE;

-- ============================================================================
-- BLOQUE 3 - Estado de los periodos del ano, para situar la lectura.
-- ESPERADO: enero a junio en estado 7 CERRADO. Julio aun no existe.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNMSEE AS MES, PRDNESTD AS ESTADO, PRDNOBSR
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;

-- ============================================================================
-- BLOQUE 4 - Sobre que mes apunta el instrumento de contraste.
-- Tras cerrar junio debe quedar en 2026 . 6, y no se toca hasta julio.
-- ============================================================================
SELECT ANIO, MES FROM RHH.CTRL_PARAM;
