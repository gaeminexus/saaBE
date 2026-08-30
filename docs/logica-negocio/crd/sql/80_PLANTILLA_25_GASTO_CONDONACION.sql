-- =====================================================================================
-- PLANTILLA 25 — linea de GASTO por condonacion de prestamos (K5)
-- FECHA: 2026-08-30
--
-- Agrega UNA linea a la plantilla de CODIGO ALTERNO 25, que en la pantalla de plantillas
-- figura como:
--     "CRD COBRO INDIVIDUAL DE PRESTAMO DEPOSITADO POR PARTICIPE ASIENTO CORRELACIONADO (1)"
-- La linea es la cuenta de PERDIDA donde se reconoce lo condonado.
--
-- Cuenta indicada por el usuario: PLNNCDGO = 9743.
--
-- La aplicacion la busca por DTPLAXL1 = 70 (CrdLineaAsiento.GASTO_CONDONACION_PRESTAMOS),
-- NUNCA por codigo de cuenta. Por eso la cuenta se puede cambiar despues editando esta
-- fila —o desde la pantalla de plantillas— sin tocar codigo ni volver a desplegar.
--
-- ⚠️ NO CORRER LOS BLOQUES 2 y 3 SIN HABER MIRADO EL 1. El bloque 1 dice QUE cuenta es
-- 9743. Si no es la de perdida por condonacion, PARAR: una cuenta equivocada aca no da
-- error, da un asiento que cuadra contra la cuenta que no era, y eso se descubre
-- conciliando meses despues.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROLES PREVIOS — mirar la salida antes de seguir
-- =====================================================================================

-- 1.1 ¿Que es la cuenta 9743? Esperado: UNA fila, y que el nombre diga perdida/gasto por
--     condonacion (o el equivalente del plan de cuentas). Anotar su PJRQCDGO.
SELECT c.PLNNCDGO, c.PLNNCNTA AS NUMERO_CUENTA, c.PLNNNMBR AS NOMBRE, c.PJRQCDGO
FROM   CNT.PLNN c
WHERE  c.PLNNCDGO = 9743;

-- 1.2 ¿Existe la plantilla 25, y en que jerarquia(s)? El PJRQCDGO tiene que coincidir con
--     el de la cuenta de 1.1 — plantillas y cuentas viven por jerarquia.
SELECT s.PLNSCDGO, s.PLNSCDAL, s.PLNSNMBR, s.PJRQCDGO, s.PLNSESTD
FROM   CNT.PLNS s
WHERE  s.PLNSCDAL = 25;

-- 1.3 ¿Que lineas tiene hoy la plantilla 25? Sirve para ver el estilo de las existentes y
--     para confirmar que el AXL1 = 70 esta libre.
SELECT d.DTPLCDGO, d.DTPLDSCR, d.DTPLMVMN,
       d.DTPLAXL1, d.DTPLAXL2, d.DTPLAXL3, d.DTPLESTD,
       c.PLNNCNTA AS NUMERO_CUENTA, c.PLNNNMBR AS NOMBRE_CUENTA
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
LEFT   JOIN CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE  s.PLNSCDAL = 25
ORDER  BY d.DTPLAXL1, d.DTPLAXL2;

-- 1.4 El AXL1 = 70 NO debe existir todavia. Esperado: 0 filas.
SELECT d.DTPLCDGO, d.DTPLDSCR, d.PLNNCDGO
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
WHERE  s.PLNSCDAL = 25 AND d.DTPLAXL1 = 70;


-- =====================================================================================
-- 2. LA LINEA — solo despues de confirmar el bloque 1
-- =====================================================================================
-- DTPLMVMN = 1 (DEBE): lo condonado se reconoce como perdida. El haber son las cuentas
-- por cobrar que se dan de baja, y esas ya las resuelve el codigo por banda (K12) y por
-- tipo de prestamo; no van en la plantilla.
--
-- Idempotente por el NOT EXISTS: correrlo dos veces inserta una sola fila.

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
                      DTPLAXL1, DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT CNT.SQ_DTPLCDGO.NEXTVAL, s.PLNSCDGO, c.PLNNCDGO,
       'GASTO POR CONDONACION DE PRESTAMOS', 1, 70, 0, 0, 0, 0, 1
FROM   CNT.PLNS s, CNT.PLNN c
WHERE  s.PLNSCDAL = 25
  AND  c.PLNNCDGO = 9743
  AND  s.PJRQCDGO = c.PJRQCDGO
  AND  NOT EXISTS (
         SELECT 1 FROM CNT.DTPL x
         WHERE  x.PLNSCDGO = s.PLNSCDGO AND x.DTPLAXL1 = 70
       );

COMMIT;

-- Esperado: 1 fila insertada la primera vez, 0 en la segunda.
-- Si inserta 0 la PRIMERA vez, la causa casi siempre es que la plantilla 25 y la cuenta
-- 9743 estan en jerarquias DISTINTAS (la condicion s.PJRQCDGO = c.PJRQCDGO). Revisar 1.1
-- y 1.2 y avisar al arbitro; NO quitar esa condicion para forzar el INSERT.


-- =====================================================================================
-- 3. CONTROL POSTERIOR
-- =====================================================================================

-- 3.1 La linea quedo, con su cuenta. Esperado: 1 fila, movimiento 1 (DEBE), estado 1.
SELECT d.DTPLCDGO, d.DTPLDSCR, d.DTPLMVMN, d.DTPLAXL1, d.DTPLESTD,
       c.PLNNCNTA AS NUMERO_CUENTA, c.PLNNNMBR AS NOMBRE_CUENTA, s.PJRQCDGO
FROM   CNT.DTPL d
JOIN   CNT.PLNS s ON s.PLNSCDGO = d.PLNSCDGO
JOIN   CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE  s.PLNSCDAL = 25 AND d.DTPLAXL1 = 70;


-- =====================================================================================
-- 4. COMO SE CAMBIA LA CUENTA DESPUES  (referencia — NO ejecutar ahora)
-- =====================================================================================
-- La cuenta NO esta en el codigo: se resuelve en tiempo de ejecucion por DTPLAXL1 = 70.
-- Para cambiarla basta con apuntar esta misma fila a otra cuenta, desde la pantalla de
-- plantillas o con este UPDATE. No hace falta redesplegar el WAR.
--
-- UPDATE CNT.DTPL d
-- SET    d.PLNNCDGO = (SELECT c.PLNNCDGO FROM CNT.PLNN c
--                      WHERE  c.PLNNCNTA = '<numero de la cuenta nueva>'
--                        AND  c.PJRQCDGO = <la misma jerarquia>)
-- WHERE  d.PLNSCDGO = (SELECT s.PLNSCDGO FROM CNT.PLNS s
--                      WHERE  s.PLNSCDAL = 25 AND s.PJRQCDGO = <la misma jerarquia>)
--   AND  d.DTPLAXL1 = 70;
-- COMMIT;
