-- ============================================================================
-- 58 - CHECK DEL PUNTO DE CORTE DEL 2026-08-25
-- ==
-- Es la SEGUNDA de las dos comprobaciones del punto de corte. La primera es
-- javap sobre el .class desplegado buscando baseFondosReservaProrrateada, y
-- esa no se hace desde la base.
-- ==
-- Que mide: que ningun mes cerrado -enero a mayo- se haya recalculado con el
-- WAR nuevo. Con el WAR del 22 y el 10, recalcular un mes anterior a junio
-- BORRA la provision de fondos de reserva de Viteri, los 183,26, porque antes
-- de junio no tenia derecho. calcularPeriodo llama a eliminaByPeriodo ANTES de
-- volver a escribir, y generaProvision no escribe nada cuando el valor es 0:
-- la fila no cambia de importe, DESAPARECE.
-- ==
-- Y no toca el liquido: PVNM no esta en el rol. Ningun total lo delata.
-- Esta consulta es la unica que lo ve.
-- ==
-- Solo lee. No modifica nada. Se corre en PRODUCCION y en LOCAL.
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - Provisiones de fondos de reserva de 2026, mes a mes.
-- ==
-- El LEFT JOIN contra la lista de meses es deliberado: si un mes se recalculo,
-- su fila no cambia de valor, SE VA. Un GROUP BY a secas la omitiria en
-- silencio y la salida seguiria pareciendo correcta. Asi sale FILAS = 0.
-- ==
-- ESPERADO enero a mayo: 1 fila, 1 persona, 183.26, veredicto INTACTO.
-- Junio, cuando exista, trae la base prorrateada de Viteri y NO se compara
-- contra 183,26: alli el valor correcto es otro.
-- ============================================================================
SELECT m.MES,
       COUNT(x.PVNMCDGO)                     AS FILAS,
       COUNT(DISTINCT x.MPLDCDGO)            AS PERSONAS,
       NVL(SUM(x.PVNMVLOR), 0)               AS VALOR,
       CASE
         WHEN COUNT(x.PVNMCDGO) = 1
              AND ROUND(NVL(SUM(x.PVNMVLOR), 0), 2) = 183.26 THEN 'INTACTO'
         WHEN COUNT(x.PVNMCDGO) = 0          THEN 'ALARMA - MES RECALCULADO'
         ELSE                                     'ALARMA - VALOR CAMBIADO'
       END                                   AS VEREDICTO
  FROM (SELECT 1 AS MES FROM DUAL UNION ALL
        SELECT 2 FROM DUAL UNION ALL
        SELECT 3 FROM DUAL UNION ALL
        SELECT 4 FROM DUAL UNION ALL
        SELECT 5 FROM DUAL) m
  LEFT JOIN (SELECT p.PRDNMSEE AS MES, v.PVNMCDGO, v.MPLDCDGO, v.PVNMVLOR
               FROM RHH.PVNM v
               JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
              WHERE p.PRDNANOO = 2026
                AND v.PVNMTPPR = 4) x
    ON x.MES = m.MES
 GROUP BY m.MES
 ORDER BY m.MES;

-- ============================================================================
-- BLOQUE 2 - Quien es, para que la fila diga un nombre y no un codigo.
-- ESPERADO: cinco filas, las cinco VITERI, las cinco 183,26.
-- ============================================================================
SELECT p.PRDNMSEE AS MES, e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       v.PVNMBSCL AS BASE, v.PVNMVLOR AS VALOR
  FROM RHH.PVNM v
  JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
  JOIN RHH.MPLD e ON e.MPLDCDGO = v.MPLDCDGO
 WHERE p.PRDNANOO = 2026
   AND p.PRDNMSEE BETWEEN 1 AND 5
   AND v.PVNMTPPR = 4
 ORDER BY p.PRDNMSEE;

-- ============================================================================
-- BLOQUE 3 - Las otras dos precondiciones de junio, del paragrafo 0 del guion.
-- ESPERADO: la primera con las filas de CTRL de junio que cargo el sql/50;
-- la segunda UNA fila, 0,10, Calderon.
-- ============================================================================
SELECT COUNT(*) AS CTRL_JUNIO_FILAS
  FROM RHH.CTRL
 WHERE CTRLANOO = 2026 AND CTRLMESS = 6;

SELECT *
  FROM RHH.CTRL
 WHERE CTRLALTR = 31 AND CTRLMESS = 6;

-- ============================================================================
-- BLOQUE 4 - Sobre que mes esta apuntando el instrumento de contraste.
-- No es del punto de corte, pero se mira siempre: un CTRL_PARAM atrasado da
-- verde al centimo del mes equivocado.
-- ============================================================================
SELECT ANIO, MES FROM RHH.CTRL_PARAM;
