-- ============================================================================
-- 64 - AJUSTE DE JULIO: LLEVAR LOS DATOS A LO QUE SE PAGO
-- ==
-- Este es el ejecutable de AJUSTE-JULIO-2026.md. El .md explica POR QUE; este
-- archivo es lo que se corre. Si los dos discrepan, gana el .md y hay que
-- corregir este.
-- ==
-- ⚠ ESTE SCRIPT ESCRIBE SOBRE DATOS DE NOMINA. Leer el .md antes de correrlo.
-- ==
-- CUANDO SE CORRE: con julio CALCULADO y en estado 3, DESPUES del primer
-- contraste y ANTES de aprobar. cerrarPeriodo escribe los ACMN a partir de
-- NMNA: si el ajuste entra antes del cierre los acumulados salen coherentes
-- solos; si entra despues, quedan acumulados que no corresponden a la nomina
-- y NADA LO AVISA.
-- ==
-- PRECONDICION: el primer contraste dio +31,43 y el bloque 1 trajo 17 filas.
-- Verificado en produccion el 2026-08-25. El ajuste esta calculado sobre ese
-- numero: aplicarlo sobre otro lo empeora en vez de arreglarlo.
-- ==
-- QUE HACE: cuatro renglones, y solo cuatro.
--   aporte personal  Caiza 45,55 -> 44,03   Nieto 85,05 -> 82,21
--                    Pardo 66,15 -> 57,33
--   fondo de reserva Munoz 45,82 -> 45,81
-- Mas los totales de sus cuatro nominas y la cabecera del periodo.
-- ==
-- QUE NO HACE, y es deliberado: no toca los 183,26 de Viteri, ni la
-- composicion de las vacaciones, ni los 700,10 de Calderon -los tres dejan el
-- liquido igual-, ni registra los 44,60 de OTROS, que son la otra mitad de
-- junio.
-- ==
-- RESULTADO ESPERADO: julio queda en +44,60 EXACTO sobre el cliente, espejo
-- del -44,60 de junio. Ni 31,43 ni cero.
-- ============================================================================


-- ############################################################################
-- PASO 1 - CONTROL ANTES. GUARDA ESTA SALIDA.
-- Es lo unico que permite deshacer el ajuste si algo sale mal.
-- NO SIGAS AL PASO 2 SIN HABERLA COPIADO A ALGUN SITIO.
-- ############################################################################

-- 1A. Los cuatro renglones que se van a tocar, tal como los dejo el motor.
-- ESPERADO: 45,55 / 85,05 / 66,15 en el aporte, y 45,82 en el fondo de Munoz.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS,
       c.CPNMALTR AS CONCEPTO, c.CPNMNMBR AS NOMBRE,
       r.RNGLCDGO AS RENGLON, r.RNGLVLRO AS VALOR_MOTOR
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND ( (c.CPNMALTR = 20 AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164'))
      OR (c.CPNMALTR =  7 AND m.MPLDIDNT = '1717649873') )
 ORDER BY c.CPNMALTR, m.MPLDAPLL;

-- 1B. Los totales de las cuatro nominas afectadas.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS, n.NMNACDGO AS NOMINA,
       n.NMNATING AS INGRESOS, n.NMNATDSC AS DESCUENTOS, n.NMNANETO AS NETO
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164','1717649873')
 ORDER BY m.MPLDAPLL;

-- 1C. La cabecera del periodo.
-- EL ESTADO TIENE QUE SER 3. Con el periodo aprobado o cerrado, PARAR.
-- ESPERADO: PRDN 62, ingresos 21.298,96, descuentos 4.983,83, neto 16.315,13.
SELECT PRDNCDGO, PRDNESTD AS ESTADO, PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS,
       PRDNTTNT AS NETO, PRDNTTPT AS PATRONAL
  FROM RHH.PRDN WHERE PRDNANOO = 2026 AND PRDNMSEE = 7;


-- ############################################################################
-- PASO 2 - LOS UPDATE. Los tres bloques, o ninguno.
-- Un renglon sin su total es el punto 9, y la cabecera no lo delata sola.
-- El COMMIT esta al final, UNA sola vez.
-- ############################################################################

-- 2A. El aporte personal, al valor que se desconto de verdad.
UPDATE RHH.RNGL r
   SET r.RNGLVLRO = CASE (SELECT m.MPLDIDNT FROM RHH.NMNA n
                            JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
                           WHERE n.NMNACDGO = r.NMNACDGO)
                      WHEN '1753528379' THEN 44.03
                      WHEN '1723962849' THEN 82.21
                      WHEN '1726657164' THEN 57.33
                    END
 WHERE r.RNGLCDGO IN (
        SELECT r2.RNGLCDGO FROM RHH.RNGL r2
          JOIN RHH.NMNA n ON n.NMNACDGO = r2.NMNACDGO
          JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
          JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
          JOIN RHH.CPNM c ON c.CPNMCDGO = r2.CPNMCDGO
         WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7 AND c.CPNMALTR = 20
           AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164'));
-- ESPERADO: 3 filas actualizadas.

-- 2A bis. El fondo de reserva de Munoz: 45,81, que es lo que su rol le pago.
UPDATE RHH.RNGL r SET r.RNGLVLRO = 45.81
 WHERE r.RNGLCDGO IN (
        SELECT r2.RNGLCDGO FROM RHH.RNGL r2
          JOIN RHH.NMNA n ON n.NMNACDGO = r2.NMNACDGO
          JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
          JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
          JOIN RHH.CPNM c ON c.CPNMCDGO = r2.CPNMCDGO
         WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7 AND c.CPNMALTR = 7
           AND m.MPLDIDNT = '1717649873');
-- ESPERADO: 1 fila actualizada.

-- 2B. Los totales de NMNA, recalculados DESDE LOS RENGLONES, no a mano.
-- Sumar desde RNGL es lo que garantiza que cabecera y detalle no diverjan: si
-- un UPDATE de 2A no hubiera entrado, esto lo arrastra y el control del paso 3
-- lo delata, en vez de taparlo con una resta escrita a mano.
UPDATE RHH.NMNA n
   SET n.NMNATING = (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND NVL(r.RNGLTPCN, c.CPNMTPCN) = 1),
       n.NMNATDSC = (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND NVL(r.RNGLTPCN, c.CPNMTPCN) = 2),
       n.NMNANETO = (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND NVL(r.RNGLTPCN, c.CPNMTPCN) = 1)
                  - (SELECT NVL(SUM(r.RNGLVLRO),0) FROM RHH.RNGL r
                       JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
                      WHERE r.NMNACDGO = n.NMNACDGO AND NVL(r.RNGLTPCN, c.CPNMTPCN) = 2)
 WHERE n.NMNACDGO IN (
        SELECT n2.NMNACDGO FROM RHH.NMNA n2
          JOIN RHH.PRDN p ON p.PRDNCDGO = n2.PRDNCDGO
          JOIN RHH.MPLD m ON m.MPLDCDGO = n2.MPLDCDGO
         WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
           AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164','1717649873'));
-- ESPERADO: 4 filas actualizadas.

-- 2C. La cabecera del periodo, recalculada DESDE LAS NOMINAS.
-- PRDNTTPT no se toca: el ajuste no altera ningun aporte patronal.
UPDATE RHH.PRDN p
   SET p.PRDNTTIN = (SELECT NVL(SUM(n.NMNATING),0) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO),
       p.PRDNTTDS = (SELECT NVL(SUM(n.NMNATDSC),0) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO),
       p.PRDNTTNT = (SELECT NVL(SUM(n.NMNANETO),0) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO)
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7;
-- ESPERADO: 1 fila actualizada.

COMMIT;


-- ############################################################################
-- PASO 3 - CONTROL DESPUES.
-- ############################################################################

-- 3A. Los cuatro renglones. ESPERADO: 44,03 / 82,21 / 57,33 y 45,81.
SELECT m.MPLDIDNT AS CEDULA, m.MPLDAPLL AS APELLIDOS,
       c.CPNMALTR AS CONCEPTO, r.RNGLVLRO AS VALOR_AJUSTADO
  FROM RHH.RNGL r
  JOIN RHH.NMNA n ON n.NMNACDGO = r.NMNACDGO
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
  JOIN RHH.CPNM c ON c.CPNMCDGO = r.CPNMCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND ( (c.CPNMALTR = 20 AND m.MPLDIDNT IN ('1753528379','1723962849','1726657164'))
      OR (c.CPNMALTR =  7 AND m.MPLDIDNT = '1717649873') )
 ORDER BY c.CPNMALTR, m.MPLDAPLL;

-- 3B. Que cabecera y detalle NO hayan divergido, en NINGUNA nomina del mes.
-- Es el punto 9, y no lo delata ningun total solo. ESPERADO: CERO FILAS.
SELECT m.MPLDIDNT AS CEDULA, n.NMNATING, n.NMNATDSC, n.NMNANETO,
       n.NMNATING - n.NMNATDSC AS NETO_RECALCULADO
  FROM RHH.NMNA n
  JOIN RHH.PRDN p ON p.PRDNCDGO = n.PRDNCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = n.MPLDCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7
   AND ABS(n.NMNANETO - (n.NMNATING - n.NMNATDSC)) > 0.005;

-- 3C. Que la cabecera del periodo cuadre con la suma de sus nominas.
-- ESPERADO: las tres diferencias en 0.
SELECT p.PRDNTTIN - (SELECT SUM(n.NMNATING) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO) AS DIF_INGRESOS,
       p.PRDNTTDS - (SELECT SUM(n.NMNATDSC) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO) AS DIF_DESCUENTOS,
       p.PRDNTTNT - (SELECT SUM(n.NMNANETO) FROM RHH.NMNA n WHERE n.PRDNCDGO = p.PRDNCDGO) AS DIF_NETO
  FROM RHH.PRDN p WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 7;

-- 3D. La cabecera ya ajustada.
-- ESPERADO: descuentos 4.983,83 - 13,18 = 4.970,65
--           ingresos  21.298,96 -  0,01 = 21.298,95
--           neto      16.315,13 + 13,17 = 16.328,30
-- Y 16.328,30 - 16.283,70 del cliente = +44,60 EXACTO.
SELECT PRDNCDGO, PRDNESTD AS ESTADO, PRDNTTIN AS INGRESOS, PRDNTTDS AS DESCUENTOS,
       PRDNTTNT AS NETO
  FROM RHH.PRDN WHERE PRDNANOO = 2026 AND PRDNMSEE = 7;


-- ############################################################################
-- PASO 4 - Y EL CONTRASTE OTRA VEZ, que es la verificacion de verdad.
-- ==
-- CTRL_PARAM ya esta en 2026 . 7: NO hay que moverlo.
-- Correr CONTRASTE_MES_CONTRA_ROL_REAL.sql, bloque 4 primero, luego 3, luego
-- 1 y 2, y el 1B con sus dos consultas.
-- ==
-- LO QUE TIENE QUE HABER CAMBIADO:
--   BLOQUE 1: de 17 filas a 13. Desaparecen las TRES del concepto 20 y la del
--             concepto 7 de Munoz. Las otras 13 siguen ahi.
--   BLOQUE 2: el liquido del periodo en +44,60 EXACTO sobre el del cliente.
--             Ni 31,43 ni cero. Cero seria la senal de que se ajusto de mas.
--   EL RESTO NO SE MUEVE: las seis de vacaciones, la de Viteri, la de Calderon
--             y las cinco de OTROS siguen ahi, porque son composicion o son la
--             mitad de junio.
-- ==
-- SI NO DA +44,60 EXACTO: deshacer con los valores del paso 1 y parar.
-- ############################################################################
