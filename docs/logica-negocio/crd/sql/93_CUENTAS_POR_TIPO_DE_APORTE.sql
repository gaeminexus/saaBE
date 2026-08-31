-- =====================================================================================
-- QUE CUENTAS EXISTEN PARA LOS 25 TIPOS DE APORTE
-- FECHA: 2026-08-31
--
-- SOLO LECTURA.
--
-- POR QUE: el usuario confirmo (2026-08-31) que **se devuelve cualquier tipo de aporte**,
-- no solo cesantia (11) y jubilacion (9). Hoy el sistema conoce la cuenta contable de
-- SOLO TRES tipos, y no por una parametrizacion propia sino por los auxiliares 50/51/52 de
-- la plantilla 21:
--     aux1 50 -> 2.1.01.05.01  APORTES PERSONALES CESANTIA
--     aux1 51 -> 2.1.02.05.01  APORTES PERSONALES JUBILACION
--     aux1 52 -> 2.1.02.15     APORTE ADICIONAL PERSONAL
-- Y la plantilla 27 (liquidacion) solo tiene las dos primeras.
--
-- CRD.TPAP no guarda ninguna cuenta: sus columnas son TPAPCDGO, TPAPNMBR, TPAPCSBC,
-- TPAPIDST y TPAPPRDP. Verificado contra com.saa.model.crd.TipoAporte.
--
-- Este script NO resuelve nada por si solo: junta la informacion para que el usuario y el
-- arbitro decidan el mapeo tipo -> cuenta. Es trabajo de contabilidad, no de codigo.
-- =====================================================================================


-- =====================================================================================
-- 1. LOS TIPOS, CON EL CODIGO DE SUBCUENTA QUE YA GUARDAN
-- =====================================================================================

-- 1.1 TPAPCSBC puede ser la pista que falta: si trae algo parecido a un codigo de cuenta,
--     el mapeo ya existe y solo hay que usarlo. Si viene NULL o con codigos internos sin
--     relacion con CNT.PLNN, hay que construir el mapeo desde cero.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPCSBC, t.TPAPIDST, t.TPAPPRDP
FROM   CRD.TPAP t
ORDER  BY t.TPAPCDGO;

-- 1.2 ¿TPAPCSBC coincide con alguna cuenta del plan? Si esta consulta trae filas, el mapeo
--     ya estaba parametrizado y nadie lo estaba usando.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPCSBC, n.PLNNCNTA, n.PLNNNMBR
FROM   CRD.TPAP t
JOIN   CNT.PLNN n ON n.PLNNCNTA = t.TPAPCSBC
ORDER  BY t.TPAPCDGO;


-- =====================================================================================
-- 2. EL PLAN DE CUENTAS DE APORTES — el lado PASIVO (el DEBE de la reclasificacion)
-- =====================================================================================

-- 2.1 Todo lo que cuelga de 2.1 (obligaciones con los participes). Es donde tienen que
--     estar las cuentas de los 25 tipos, si es que existen.
--     MIRA: ¿hay una cuenta por tipo, o varios tipos comparten cuenta?
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PJRQCDGO AS EMPRESA
FROM   CNT.PLNN n
WHERE  n.PLNNCNTA LIKE '2.1.%'
ORDER  BY n.PLNNCNTA;


-- =====================================================================================
-- 3. EL PLAN DE CUENTAS DE LIQUIDACION — el HABER de la reclasificacion
-- =====================================================================================

-- 3.1 Todo lo que cuelga de 2.3.01 (liquidaciones por pagar). Hoy conocemos dos:
--     2.3.01.05.01 cesantia y 2.3.01.10.01 jubilacion.
--     MIRA: ¿existe una cuenta de liquidacion por cada familia de aporte, o todas las
--     devoluciones caen en esas dos?
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PJRQCDGO AS EMPRESA
FROM   CNT.PLNN n
WHERE  n.PLNNCNTA LIKE '2.3.01.%'
ORDER  BY n.PLNNCNTA;


-- =====================================================================================
-- 4. DONDE MAS SE USAN HOY LAS CUENTAS DE APORTES
-- =====================================================================================

-- 4.1 Todas las lineas de plantilla que apuntan a una cuenta 2.1.% o 2.3.01.%, de
--     cualquier plantilla. Sirve para ver si otro proceso (jubilacion, cierre, Petro) ya
--     resolvio este mismo mapeo y conviene reusarlo en vez de inventar otro.
SELECT p.PLNSCDAL AS PLANTILLA_ALTERNO, p.PLNSNMBR AS PLANTILLA,
       d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA, n.PLNNNMBR
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
JOIN   CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  (n.PLNNCNTA LIKE '2.1.%' OR n.PLNNCNTA LIKE '2.3.01.%')
ORDER  BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2;


-- =====================================================================================
-- 5. CUANTO IMPORTA CADA TIPO — para priorizar
-- =====================================================================================

-- 5.1 Saldo vivo por tipo de aporte. Un tipo con saldo cero o casi cero puede que en la
--     practica no se devuelva nunca, aunque en teoria se pueda.
--     Sirve para decidir si hay que parametrizar los 25 ahora o si alcanza con los que
--     mueven plata, dejando que el resto falle con mensaje claro.
SELECT a.TPAPCDGO, t.TPAPNMBR,
       COUNT(*)        AS MOVIMIENTOS,
       SUM(a.APRTVLRR) AS SALDO
FROM   CRD.APRT a
JOIN   CRD.TPAP t ON t.TPAPCDGO = a.TPAPCDGO
GROUP  BY a.TPAPCDGO, t.TPAPNMBR
ORDER  BY SUM(a.APRTVLRR) DESC;


-- =====================================================================================
-- 6. QUE HACER CON EL RESULTADO
-- =====================================================================================
-- Pasale al arbitro las salidas de 1.1, 2.1, 3.1 y 5.1.
--
-- Con eso se decide algo que NO es una eleccion tecnica: si cada tipo de aporte tiene su
-- propia cuenta de pasivo y de liquidacion, o si varios tipos comparten (p. ej. todos los
-- de la familia cesantia van a 2.1.01.05.01 y liquidan en 2.3.01.05.01).
--
-- ⚠️ NO adivinar por el nombre del tipo. "RENDIMIENTO CESANTIA PERSONAL" puede ir a la
-- misma cuenta que "CESANTIA PERSONAL" o a una propia de rendimientos, y el asiento cuadra
-- igual en los dos casos — es el error que no se nota.
-- =====================================================================================
