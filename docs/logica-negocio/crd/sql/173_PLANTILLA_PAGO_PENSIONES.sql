-- =====================================================================================
-- ⛔ PLANTILLA 35 — DEVENGO DEL PAGO MENSUAL DE PENSION COMPLEMENTARIA
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ ESTE SCRIPT ESCRIBE. Correrlo ANTES de desplegar el WAR con el pago a jubilados.
--
-- QUE LO ORIGINA: LEVANTAMIENTO-ALIMENTACION-CONTABLE-CREDITOS.md §3.1 (pago mensual de
-- pensiones) y su propio pendiente del §605: «Crear plantillas faltantes ... pensiones de
-- jubilados (21022501, 23011003, 23909006)».
--
-- ⛔ POR QUE ES PRERREQUISITO Y NO UN PASO POSTERIOR: sin esta plantilla el asiento de
--    devengo no tiene de donde salir. Es la misma trampa que la linea de mora del asiento de
--    Petro y que el .jasper faltante: compila, pasa revision, entra al commit y revienta la
--    primera vez que un usuario lo corre.
--
-- ⛔ LA 29 NO SIRVE PARA ESTO. La 29 es "CRD JUBILACION DE UN PARTICIPE": el TRASLADO de
--    cesantia y jubilacion a pension complementaria, que ocurre UNA VEZ al jubilarse. Esta es
--    el DEVENGO MENSUAL de la pension que se le paga despues. Son dos hechos distintos.
--
-- ALTERNO 35: libre. El 34 (entrega quirografario) es el ultimo usado, creado el 2026-09-01.
--
-- AUXILIARES POSICIONALES 1..4 — igual que la 29 y la 34, NO del catalogo semantico
-- CrdLineaAsiento. No reusar estos numeros en otra plantilla sin verificar contra CNT.DTPL.
--
--   aux1 = 1   DEBE    2.1.02.25.01   cuenta del jubilado -- tramo pension
--   aux1 = 2   HABER   2.3.01.10.03   pensiones complementarias por pagar
--   aux1 = 3   DEBE    2.1.02.25.01   cuenta del jubilado -- tramo seguro de salud
--   aux1 = 4   HABER   2.3.90.90.06   seguro de salud por pagar
--
-- El asiento del PAGO contra banco NO va aca: lo genera CXP/TSR con la orden de pago, igual
-- que en la devolucion de aportes. Aca solo el devengo.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- CONTROL ANTES — ⛔ PARAR SI ALGO DE ESTO NO DA LO ESPERADO
-- =====================================================================================

-- A.1 — Las tres cuentas tienen que existir y estar ACTIVAS. Deben salir 3 filas.
--       Si sale menos de 3: PARAR Y AVISAR. Falta crear la cuenta en el plan, y eso lo
--       decide contabilidad, no este script.
SELECT  c.PLNNCDGO, c.PLNNCNTA, c.PLNNNMBR, c.PLNNESTD
FROM    CNT.PLNN c
WHERE   REPLACE(c.PLNNCNTA, '.', '') IN ('21022501', '23011003', '23909006')
ORDER   BY c.PLNNCNTA;

-- A.2 — El alterno 35 tiene que estar LIBRE. Debe salir 0 filas.
--       Si sale alguna: PARAR. Alguien mas uso el 35 y hay que elegir otro numero.
SELECT  p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PLNSESTD
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 35;

-- A.3 — La plantilla 29 tiene que existir: de ella se copian empresa y sistema.
--       Debe salir 1 fila.
SELECT  p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PJRQCDGO, p.PLNSSSTM
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 29;

-- A.4 — Las secuencias tienen que existir. Deben salir 2 filas.
--       Si NO aparecen, PARAR: habria que insertar con MAX+1 y este script no es el correcto.
SELECT  s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER
FROM    ALL_SEQUENCES s
WHERE   s.SEQUENCE_OWNER = 'CNT'
AND     s.SEQUENCE_NAME IN ('SQ_PLNSCDGO', 'SQ_DTPLCDGO');


-- =====================================================================================
-- EJECUCION
--
-- Se usa la secuencia (NEXTVAL), no claves explicitas: la aplicacion y este script piden
-- numeros a la misma fuente y no queda nada que sincronizar despues.
--
-- Las cuentas se resuelven POR NUMERO DE CUENTA, no por un PLNNCDGO copiado a mano: si la
-- cuenta no existe el INSERT no inserta nada y el CONTROL DESPUES lo delata, en vez de
-- grabar una linea apuntando a una cuenta equivocada.
-- =====================================================================================

-- La cabecera. Empresa y sistema se COPIAN de la plantilla 29, no se escriben a mano.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT  CNT.SQ_PLNSCDGO.NEXTVAL,
        'CRD PAGO MENSUAL DE PENSION COMPLEMENTARIA',
        35,
        1,
        p29.PJRQCDGO,
        'Devengo mensual de la pension complementaria y su seguro de salud. Creada 2026-09-02, levantamiento contable 3.1. Auxiliares POSICIONALES 1..4, como la 29. El asiento de pago contra banco lo genera CXP/TSR, no esta plantilla.',
        p29.PLNSSSTM
FROM    CNT.PLNS p29
WHERE   p29.PLNSCDAL = 29;


-- Las 4 lineas. MVMN: 1 = DEBE, 2 = HABER.

-- aux1 = 1  DEBE  2.1.02.25.01  cuenta del jubilado, tramo pension
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'PENSION COMPLEMENTARIA JUBILADO', 1, 1, 0,0,0,0, 1
FROM    CNT.PLNS p, CNT.PLNN c
WHERE   p.PLNSCDAL = 35
AND     REPLACE(c.PLNNCNTA, '.', '') = '21022501';

-- aux1 = 2  HABER  2.3.01.10.03  pensiones complementarias por pagar
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'PENSIONES COMPLEMENTARIAS POR PAGAR', 2, 2, 0,0,0,0, 1
FROM    CNT.PLNS p, CNT.PLNN c
WHERE   p.PLNSCDAL = 35
AND     REPLACE(c.PLNNCNTA, '.', '') = '23011003';

-- aux1 = 3  DEBE  2.1.02.25.01  cuenta del jubilado, tramo seguro de salud
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'PENSION COMPLEMENTARIA JUBILADO - SEGURO DE SALUD', 1, 3, 0,0,0,0, 1
FROM    CNT.PLNS p, CNT.PLNN c
WHERE   p.PLNSCDAL = 35
AND     REPLACE(c.PLNNCNTA, '.', '') = '21022501';

-- aux1 = 4  HABER  2.3.90.90.06  seguro de salud por pagar
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, c.PLNNCDGO,
        'SEGURO DE SALUD JUBILADOS POR PAGAR', 2, 4, 0,0,0,0, 1
FROM    CNT.PLNS p, CNT.PLNN c
WHERE   p.PLNSCDAL = 35
AND     REPLACE(c.PLNNCNTA, '.', '') = '23909006';

COMMIT;


-- =====================================================================================
-- CONTROL DESPUES — ⛔ SI ESTO NO DA LO ESPERADO, EL DESPLIEGUE NO VA
-- =====================================================================================

-- D.1 — Las 4 lineas, con su cuenta. Deben salir 4 filas, aux1 1..4, dos DEBE y dos HABER.
--       Si salen MENOS de 4, alguna cuenta no existia: la linea no se inserto y hay que
--       PARAR Y AVISAR. NO desplegar con la plantilla incompleta.
SELECT  d.DTPLAXL1                        AS AUX1,
        d.DTPLMVMN                        AS MVMN_1DEBE_2HABER,
        c.PLNNCNTA                        AS CUENTA,
        c.PLNNNMBR                        AS NOMBRE_CUENTA,
        d.DTPLDSCR                        AS DESCRIPCION,
        d.DTPLESTD                        AS ESTADO
FROM    CNT.DTPL d
JOIN    CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO
JOIN    CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE   p.PLNSCDAL = 35
ORDER   BY d.DTPLAXL1;

-- D.2 — Resumen de cuadre estructural. Debe dar LINEAS=4, MIN_AUX1=1, MAX_AUX1=4,
--       DEBES=2, HABERES=2.
SELECT  COUNT(*)                          AS LINEAS,
        MIN(d.DTPLAXL1)                   AS MIN_AUX1,
        MAX(d.DTPLAXL1)                   AS MAX_AUX1,
        SUM(CASE WHEN d.DTPLMVMN = 1 THEN 1 ELSE 0 END) AS DEBES,
        SUM(CASE WHEN d.DTPLMVMN = 2 THEN 1 ELSE 0 END) AS HABERES
FROM    CNT.DTPL d
JOIN    CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO
WHERE   p.PLNSCDAL = 35;


-- =====================================================================================
-- REVERSO — comentado a proposito. Descomentar SOLO si hay que deshacer este script.
-- Borra primero el detalle y despues la cabecera; el orden inverso deja huerfanos.
-- =====================================================================================
-- DELETE FROM CNT.DTPL WHERE PLNSCDGO IN (SELECT PLNSCDGO FROM CNT.PLNS WHERE PLNSCDAL = 35);
-- DELETE FROM CNT.PLNS WHERE PLNSCDAL = 35;
-- COMMIT;
