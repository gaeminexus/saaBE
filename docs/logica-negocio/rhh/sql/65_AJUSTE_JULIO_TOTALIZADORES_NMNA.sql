-- ============================================================================
-- 65 - COMPLETA EL AJUSTE DE JULIO: LOS TOTALIZADORES PROPIOS DE NMNA
-- ==
-- ⚠ ESTE SCRIPT ESCRIBE SOBRE DATOS DE NOMINA.
-- ==
-- CORRIGE UN DEFECTO DEL sql/64, destapado por el SEGUNDO CONTRASTE el
-- 2026-08-25. El 64 actualizo los renglones y los totales de cabecera
-- --NMNATING, NMNATDSC, NMNANETO-- pero NMNA guarda ADEMAS sus propios
-- totalizadores, y esos se quedaron con el valor del motor:
-- ==
--   NMNAAPPR  aporte personal      <- el que importa
--   NMNAFNRS  fondos de reserva
-- ==
-- COMO SE VIO: el bloque 3 del contraste seguia enseñando a Nieto con 85,05
-- despues del ajuste, porque ese bloque lee NMNAAPPR y no los renglones.
-- ==
-- POR QUE ES GRAVE, y no es el bloque 3: cerrarPeriodo escribe el ACMN de
-- APORTE_PERSONAL DESDE NMNAAPPR, no desde RNGL:
--     escribeAcumulado(..., RhhTipoAcumulado.APORTE_PERSONAL,
--                      nomina.getAportePersonal(), null, usuario);
-- Si julio cerrara sin esto, el acumulado del ano guardaria 85,05 / 45,55 /
-- 66,15 en vez de lo que se desconto, y el ACMN tipo 8 es JUSTO lo que se
-- reconcilia contra el IESS. El ajuste habria sido cosmetico donde mas importa.
-- ==
-- CUANDO SE CORRE: inmediatamente despues del sql/64, con julio todavia en
-- estado 3 y ANTES de aprobar. Si julio ya esta CERRADO, PARAR: habria que
-- reabrirlo, porque los ACMN ya estarian escritos mal.
-- ==
-- QUE NO TOCA: NMNABSIE, NMNABSFR, NMNAAPPT, NMNAIESC, NMNATTPT, NMNADITR.
-- El ajuste no cambio ninguna base ni ningun aporte patronal ni los dias.
-- ============================================================================


-- ############################################################################
-- PASO 1 - CONTROL ANTES, sobre LAS VEINTE nominas, no solo las cuatro.
-- Compara lo que NMNA guarda contra lo que suman sus renglones.
-- ############################################################################

-- 1A. Aporte personal: guardado contra la suma de los renglones del rol 1.
-- ESPERADO: TRES filas descuadradas, las de Caiza, Nieto y Pardo:
--   Caiza  45,55 guardado contra 44,03 en renglones
--   Nieto  85,05 contra 82,21
--   Pardo  66,15 contra 57,33
-- Cualquier OTRA persona aqui es un hallazgo: significaria que algo mas se
-- desincronizo, y hay que mirarlo antes de tocar nada.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS, n.NMNACDGO AS NOMINA,
       n.NMNAAPPR AS GUARDADO,
       (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
          JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
         WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 1) AS EN_RENGLONES
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND ABS(NVL(n.NMNAAPPR,0)
       - (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
          JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
         WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 1)) > 0.005
 ORDER BY m.MPLDAPLL;

-- 1B. Fondos de reserva: guardado contra la suma de los renglones del rol 5.
-- ESPERADO: UNA fila, Munoz, 45,82 guardado contra 45,81 en renglones.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS, n.NMNACDGO AS NOMINA,
       n.NMNAFNRS AS GUARDADO,
       (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
          JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
         WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 5) AS EN_RENGLONES
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND ABS(NVL(n.NMNAFNRS,0)
       - (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
          JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
         WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 5)) > 0.005
 ORDER BY m.MPLDAPLL;

-- 1C. Y que el periodo siga en estado 3. Si dice 7, PARAR: los ACMN ya se
-- escribieron con los valores del motor y hay que reabrir.
SELECT PRDNCDGO, PRDNESTD AS ESTADO FROM RHH.PRDN
 WHERE PRDNANOO = 2026 AND PRDNMSEE = 7;


-- ############################################################################
-- PASO 2 - EL UPDATE.
-- Se recalcula DESDE LOS RENGLONES, igual que el 64, y solo donde difiere.
-- Asi es idempotente: correrlo dos veces no hace dano, y no toca a quien ya
-- estaba bien.
-- ############################################################################

UPDATE RHH.NMNA n
   SET n.NMNAAPPR = (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 1),
       n.NMNAFNRS = (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND c.CPNMROLM = 5)
 WHERE n.NMNACDGO IN (
        SELECT n2.NMNACDGO FROM RHH.NMNA n2
          JOIN RHH.PRDN p ON p.PRDNCDGO = n2.PRDNCDGO
         WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
           AND ( ABS(NVL(n2.NMNAAPPR,0)
                     - (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                        JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                       WHERE r.NMNACDGO = n2.NMNACDGO AND c.CPNMROLM = 1)) > 0.005
              OR ABS(NVL(n2.NMNAFNRS,0)
                     - (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                        JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                       WHERE r.NMNACDGO = n2.NMNACDGO AND c.CPNMROLM = 5)) > 0.005 ) );
-- ESPERADO: 4 filas actualizadas. Caiza, Nieto y Pardo por el aporte, y Munoz
-- por el fondo de reserva.

COMMIT;


-- ############################################################################
-- PASO 3 - CONTROL DESPUES.
-- ############################################################################

-- 3A y 3B. Las mismas consultas del paso 1. ESPERADO: LAS DOS VACIAS.
-- Ya no hay ninguna nomina de julio cuyo totalizador difiera de sus renglones.

-- 3C. Los cuatro valores, a la vista.
-- ESPERADO: Caiza 44,03 . Nieto 82,21 . Pardo 57,33 . Munoz FR 45,81.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS,
       n.NMNAAPPR AS APORTE_PERSONAL, n.NMNAFNRS AS FONDOS_RESERVA,
       n.NMNATING AS INGRESOS, n.NMNATDSC AS DESCUENTOS, n.NMNANETO AS NETO
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164','1717649873')
 ORDER BY m.MPLDAPLL;

-- 3D. Y que la cabecera del periodo NO se haya movido: este script no toca
-- ingresos, descuentos ni neto.
-- ESPERADO: 21.298,95 . 4.970,65 . 16.328,30, los mismos del sql/64.
SELECT PRDNCDGO, PRDNESTD AS ESTADO, PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS,
       PRDNTTNT AS NETO
  FROM RHH.PRDN WHERE PRDNANOO = 2026 AND PRDNMSEE = 7;


-- ############################################################################
-- PASO 4 - El bloque 3 del contraste, que es donde se vio el problema.
-- ==
-- Volver a correr CONTRASTE_MES_CONTRA_ROL_REAL.sql, bloque 3.
-- ESPERADO: las mismas 20 filas CON NOMINA Y SIN PLANILLA -julio no tiene
-- planilla-, pero con los aportes YA CORREGIDOS:
--     Caiza  44,03 + 53,74  =  97,77
--     Nieto  82,21 + 100,35 = 182,56
--     Pardo  57,33 + 78,05  = 135,38
-- Los bloques 1 y 2 NO deben cambiar: siguen en 13 filas y +44,60. Este script
-- no toca renglones ni totales de cabecera.
-- ############################################################################
