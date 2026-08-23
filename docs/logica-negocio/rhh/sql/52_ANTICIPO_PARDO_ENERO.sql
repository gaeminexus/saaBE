-- =====================================================
-- MODULO: RHH - EL ANTICIPO DE PARDO CALLE, QUE NO VIENE DE LA APERTURA
-- DESCRIPCION: Crea el descuento recurrente y sus dos cuotas para el anticipo
--              de 700,00 que Pardo Calle recibe en enero de 2026.
-- ORDEN DE EJECUCION: 52   (DESPUES de aplicar los saldos de apertura,
--                           ANTES de calcular enero)
-- FECHA: 2026-08-21
-- PARAMETRO: :EMPRESA -- 1236
-- =====================================================
-- POR QUE NO ESTA EN EL CORTE, y por que eso es correcto
--
-- El corte de apertura es al 31-12-2025 y trae UN solo anticipo: el de
-- Calderon Parraga, que en local se llama "ANTICIPO DIC-2025" y arranca el
-- 31-12-2025. El de Pardo Calle es "ANTICIPO ENE-2026" y arranca el
-- 01-01-2026: **nacio despues del corte**, asi que SLAP no lo trae y no
-- falta nada en el sql/26.
--
-- Pero es real y hay que registrarlo: el rol del cliente descuenta 350,00 a
-- Pardo en enero y otros 350,00 en febrero (concepto 25 de RHH.CTRL en los
-- dos meses). Sin este descuento recurrente, enero sale 350,00 por encima en
-- su fila y otros 350,00 en febrero.
--
-- SE CREA DIRECTAMENTE, NO SE REVIERTE LA APERTURA. revertirSaldosApertura
-- no acepta filtro: deshacer para anadir una fila implicaria revertir las 57
-- y volver a aplicar, con todo lo que eso arrastra. Y ademas seria inutil,
-- porque este anticipo no pertenece al corte.
--
-- LO QUE EL MOTOR HACE CON ESTO: el paso 12 de calcularPeriodo recoge las
-- cuotas que vencen dentro del periodo (selectPendientesPorVencer) y genera
-- el renglon con origen DESCUENTO_RECURRENTE. **No hay que registrar ninguna
-- novedad de anticipo en enero ni en febrero**: registrarla ademas cobraria
-- el anticipo dos veces.
--
-- OJO CON EL PUNTO 12 DE LA LISTA: el motor aplica la cuota pero nunca la
-- marca --CTDSESTD se queda en 1 y CTDSVLDS en cero-- asi que el saldo de
-- este DSRC seguira diciendo 700,00 despues de cobrarse entero. Es un defecto
-- conocido, no afecta al calculo, y se corrige antes de agosto.
-- =====================================================

-- -----------------------------------------------------
-- CONTROL ANTES: debe haber 1 DSRC (Calderon) y 2 CTDS.
-- -----------------------------------------------------
SELECT (SELECT COUNT(*) FROM RHH.DSRC) AS DSRC,
       (SELECT COUNT(*) FROM RHH.CTDS) AS CTDS FROM DUAL;
-- Esperado: 1 · 2.  Si ya dice 2 · 4, este script ya corrio: no repetir.

SELECT d.DSRCCDGO, m.MPLDAPLL, d.DSRCNMRO, d.DSRCVLOR, d.DSRCNMCT
  FROM RHH.DSRC d JOIN RHH.MPLD m ON m.MPLDCDGO = d.MPLDCDGO;
-- Esperado: una fila, CALDERON PARRAGA, ANTICIPO DIC-2025, 700, 2 cuotas.


-- -----------------------------------------------------
-- EL ACUERDO
-- -----------------------------------------------------
INSERT INTO RHH.DSRC (
    MPLDCDGO, CPNMCDGO, DSRCTPDS, DSRCNMRO, DSRCVLOR, DSRCSLDD,
    DSRCNMCT, DSRCCTPG, DSRCVLCT, DSRCFCHI, DSRCESTD, DSRCUSRR
)
SELECT m.MPLDCDGO,
       (SELECT CPNMCDGO FROM RHH.CPNM WHERE CPNMALTR = 25),   -- Anticipo de sueldo
       9,                                                      -- tipo de saldo ANTICIPO
       'ANTICIPO ENE-2026',
       700.00, 700.00,
       2, 0, 350.00,
       DATE '2026-01-01',
       1, 'CARGA'
  FROM RHH.MPLD m
 WHERE m.MPLDIDNT = '1726657164' AND m.PJRQCDGO = :EMPRESA;


-- -----------------------------------------------------
-- LAS DOS CUOTAS
-- -----------------------------------------------------
-- Vencimientos iguales a los del anticipo de Calderon: fin de enero y fin de
-- febrero. El motor recoge la cuota cuyo vencimiento cae dentro del periodo
-- que se calcula.
INSERT INTO RHH.CTDS (
    DSRCCDGO, CTDSNMCT, CTDSFCVN, CTDSTTAL, CTDSCPTL, CTDSINTR,
    CTDSVLDS, CTDSSLDD, CTDSESTD, CTDSFCHR, CTDSUSRR
)
SELECT d.DSRCCDGO, c.NRO, c.VENCE, 350.00, 350.00, 0, 0, c.SALDO, 1, SYSDATE, 'CARGA'
  FROM RHH.DSRC d
  JOIN RHH.MPLD m ON m.MPLDCDGO = d.MPLDCDGO
 CROSS JOIN (
    SELECT 1 NRO, DATE '2026-01-31' VENCE, 350.00 SALDO FROM DUAL UNION ALL
    SELECT 2,     DATE '2026-02-28',          0.00      FROM DUAL
 ) c
 WHERE m.MPLDIDNT = '1726657164' AND m.PJRQCDGO = :EMPRESA
   AND d.DSRCNMRO = 'ANTICIPO ENE-2026';

COMMIT;


-- -----------------------------------------------------
-- CONTROL DESPUES
-- -----------------------------------------------------
SELECT (SELECT COUNT(*) FROM RHH.DSRC) AS DSRC,
       (SELECT COUNT(*) FROM RHH.CTDS) AS CTDS FROM DUAL;
-- Esperado: 2 · 4.

SELECT m.MPLDAPLL, d.DSRCNMRO, c.CTDSNMCT AS CUOTA,
       TO_CHAR(c.CTDSFCVN, 'YYYY-MM-DD') AS VENCE, c.CTDSTTAL AS TOTAL, c.CTDSESTD AS ESTADO
  FROM RHH.CTDS c
  JOIN RHH.DSRC d ON d.DSRCCDGO = c.DSRCCDGO
  JOIN RHH.MPLD m ON m.MPLDCDGO = d.MPLDCDGO
 ORDER BY m.MPLDAPLL, c.CTDSNMCT;
-- Esperado: cuatro filas, dos de CALDERON y dos de PARDO, vencimientos
-- 2026-01-31 y 2026-02-28, 350,00 cada una, todas en estado 1.

-- Y lo que enero tiene que dar despues de esto, en esas dos personas:
--   CALDERON PARRAGA  descuentos 430,57 = 66,15 + 14,42 + 350,00  liquido 269,43
--   PARDO CALLE       descuentos 416,15 = 66,15 + 350,00          liquido 283,85
