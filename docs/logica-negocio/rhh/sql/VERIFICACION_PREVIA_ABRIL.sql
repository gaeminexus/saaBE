-- =====================================================
-- MODULO: RHH - LA PUERTA DE ABRIL: LAS CINCO COMPROBACIONES PREVIAS
-- DESCRIPCION: Solo lectura. Nada de esto escribe. Se corre entero antes de
--              que el frontend toque la pantalla de Periodos.
-- FECHA: 2026-08-23
-- PARAMETRO: :EMPRESA -- 1236
-- =====================================================
-- POR QUE ESTAS CINCO Y NO OTRAS. Cada una tapa un fallo que ya paso una vez:
--   1. La ficha de Mendez -> los 218,22 del punto 14.
--   2. CTRL_PARAM         -> el contraste vacio que se lee como exito.
--   3. La base de asientos-> el censo total de CNT.ASNT que cuenta asientos ajenos.
--   4. Los periodos       -> el sql/49 corriendo con marzo abierto.
--   5. El detector del 14 -> el recalculo que reescribe un mes en vez de reproducirlo.
-- =====================================================


-- =====================================================
-- 1. LA FICHA DE MENDEZ TORRES. Tiene que salir UNA fila: 482 / 1 / 40.
--    Si salen DOS, el UPDATE del sql/49 filtra solo por MPLDCDGO y toco los
--    dos contratos: parar y avisar antes de calcular nada.
-- =====================================================
SELECT m.MPLDIDNT, c.CNTECDGO, c.CNTESLRB AS SUELDO, c.CNTEJRND AS JORNADA,
       c.CNTEHRSM AS HORAS, c.CNTEESTD AS ESTADO_CONTRATO,
       CASE WHEN c.CNTESLRB = 482 AND c.CNTEJRND = 1 AND c.CNTEHRSM = 40
            THEN 'OK' ELSE '*** SQL/49 NO APLICADO: PARAR ***' END AS VEREDICTO
  FROM RHH.CNTE c JOIN RHH.MPLD m ON m.MPLDCDGO = c.MPLDCDGO
 WHERE m.MPLDIDNT = '1004350904' AND m.PJRQCDGO = :EMPRESA;


-- =====================================================
-- 2. EL PARAMETRO DEL CONTRASTE. Tiene que decir 2026 / 4.
--    Con el parametro en otro mes TODOS los bloques salen vacios y eso
--    PARECE UN EXITO. Es la primera trampa del instrumento.
-- =====================================================
SELECT ANIO, MES,
       CASE WHEN ANIO = 2026 AND MES = 4
            THEN 'OK' ELSE '*** NO CONTRASTAR TODAVIA ***' END AS VEREDICTO
  FROM RHH.CTRL_PARAM;


-- =====================================================
-- 3. LA BASE DE ASIENTOS. Se anota AHORA, no al final.
--    El censo total de CNT.ASNT no vale en produccion: otros modulos escriben
--    en paralelo. Durante el cierre de febrero nacieron cinco asientos ajenos.
-- =====================================================
SELECT MAX(ASNTCDGO) AS BASE_ASIENTOS FROM CNT.ASNT;


-- =====================================================
-- 4. LOS PERIODOS DE 2026, con sus codigos REALES de esta base.
--    Enero, febrero y marzo en estado 7. Abril no debe existir todavia.
--    Los codigos de produccion no son los de local: alli enero es 1,
--    febrero 2 y marzo 21. Nunca filtrar por el codigo, siempre por ANOO/MSEE.
-- =====================================================
SELECT PRDNANOO AS ANIO, PRDNMSEE AS MES, PRDNCDGO, PRDNESTD AS ESTADO,
       PRDNMODO AS MODO, PRDNFCHI, PRDNFCHF,
       CASE WHEN PRDNESTD = 7 THEN 'CERRADO' ELSE '*** NO CERRADO ***' END AS VEREDICTO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;


-- =====================================================
-- 5. EL DETECTOR DEL PUNTO 14 (plan §4 bis).
--    AHORA DEBE SACAR TRES FILAS: Mendez Torres en enero, febrero y marzo,
--    241,00 del mes contra 482,00 de hoy. ESO NO ES UN FALLO: es la prueba
--    de que esos tres meses YA NO SE PUEDEN RECALCULAR, porque saldrian con
--    un sueldo que nunca se pago, sin error y sin aviso.
--    Si sacara a alguien MAS QUE MENDEZ, eso si es hallazgo: parar y reportar.
-- =====================================================
SELECT p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES, m.MPLDIDNT, m.MPLDAPLL,
       r.RNGLVLRO AS SUELDO_DEL_MES, c.CNTESLRB AS SUELDO_DE_HOY
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
  JOIN RHH.CNTE c ON c.MPLDCDGO = m.MPLDCDGO
  JOIN RHH.CPNM k ON k.CPNMCDGO = r.CPNMCDGO
 WHERE k.CPNMALTR = 1
   AND n.NMNADITR = 30
   AND r.RNGLVLRO <> c.CNTESLRB
 ORDER BY 1, 2, 3;
