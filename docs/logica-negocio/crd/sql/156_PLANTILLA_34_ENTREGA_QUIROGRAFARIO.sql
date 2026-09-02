-- =====================================================================================
-- PLANTILLA CONTABLE 34 — ENTREGA DE PRESTAMO QUIROGRAFARIO
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT SI ESCRIBE. Inserta 1 fila en CNT.PLNS y 8 en CNT.DTPL.
--    Lleva controles ANTES y DESPUES, y el bloque de reverso al final, comentado.
--
-- QUE CREA: la plantilla del asiento de entrega de un prestamo QUIROGRAFARIO, que no
-- existia. Es la decision D7 del alcance: las de prendario (alterno 9) e hipotecario
-- (13) existen, la de quirografario hay que crearla con el mismo patron.
--
-- DE DONDE SALEN LOS NUMEROS: del script 153, corrido en produccion el 2026-09-01.
-- Nada de este script esta inventado ni copiado a ciegas.
--
-- ⛔ POR QUE LOS AUXILIARES VAN 1..8 Y NO SE COPIAN DE LA 9:
--    El bloque 2 del 153 lo midio y la respuesta es concluyente:
--
--      alterno 9  -> 9 lineas, AUX1 de 1 a 9, 9 distintos   => POSICIONALES
--      alterno 13 -> 9 lineas, AUX1 de 1 a 9, 9 distintos   => POSICIONALES
--      alterno 21 -> 45 lineas, AUX1 de 3 a 60, 39 distintos => semanticos
--
--    En una plantilla posicional el auxiliar NO significa nada por si mismo: es el
--    orden de la linea. Copiar el "7" de la 9 a la 34 no traeria "documentos en
--    garantia", traeria "la septima linea". Por eso la 34 numera 1..8 segun su propio
--    orden, y por eso tiene 8 y no 9 lineas.
--
--    Esta es la trampa que aviso el equipo A: un auxiliar mal mapeado deja el asiento
--    MAL CLASIFICADO Y CUADRADO IGUAL, o sea sin ninguna senal de error.
--
-- ⛔ POR QUE 8 LINEAS Y NO 9 — decision del usuario, 2026-09-01:
--    La 9 y la 13 llevan DOS cuentas de orden al HABER: 7.4.01.05 DOCUMENTOS EN
--    GARANTIA (en las dos) mas la del bien -- 7.4.01.10 VEHICULOS en la prendaria,
--    7.4.01.15 BIENES INMUEBLES en la hipotecaria.
--    Un quirografario no tiene bien en garantia, pero SI tiene pagare. Se conserva
--    7.4.01.05 y se omite la del bien. El hecho de que 7.4.01.05 este en las DOS
--    plantillas mientras la del bien cambia es lo que dice que el documento es comun a
--    todo credito y el bien es especifico.
--
-- RESERVA: alterno 34, anotado en REGISTRO-RESERVAS-EQUIPOS.md §2c el 2026-08-31 y
--          verificado LIBRE en produccion por el bloque 4 del 153.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET SERVEROUTPUT ON


-- =====================================================================================
-- CONTROL 0 — ANTES DE EJECUTAR. Los tres tienen que pasar.
-- =====================================================================================

-- 0.1  El alterno 34 tiene que estar LIBRE. Esperado: 0 filas.
--      Si devuelve algo, PARAR: alguien lo tomo desde el 2026-09-01.
SELECT  p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PLNSESTD
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 34;

-- 0.2  Las 8 cuentas tienen que existir y estar ACTIVAS. Esperado: 8 filas, todas
--      con PLNNESTD = 1. Si falta alguna o alguna esta inactiva, PARAR.
SELECT  c.PLNNCDGO, c.PLNNCNTA, c.PLNNNMBR, c.PLNNESTD
FROM    CNT.PLNN c
WHERE   c.PLNNCDGO IN (10279, 10280, 10281, 10282, 10283, 10457, 9979, 10398)
ORDER   BY c.PLNNCNTA;

-- 0.3  La plantilla 9 tiene que existir: de ella se copian empresa y sistema, para no
--      inventarlos. Esperado: 1 fila.
SELECT  p.PLNSCDGO, p.PLNSCDAL, p.PJRQCDGO AS EMPRESA, p.PLNSSSTM AS SISTEMA
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 9;

-- 0.4  La secuencia tiene que existir. ⚠️ Esto NO es la regla derogada del registro:
--      alli se comprobo que SCP.SQ_PRBRCDGO/SQ_PDTRCDGO no existen y que nada las
--      ejercita. Aca es distinto y hay que mirarlo: CNT.PLNS y CNT.DTPL SI se crean
--      desde la aplicacion — PlantillaRest y DetallePlantillaRest tienen @POST y @PUT —
--      asi que la secuencia se usa de verdad.
--      Si estas dos filas aparecen, el script las usa y no hay nada que sincronizar.
--      Si NO aparecen, PARAR Y AVISAR: habria que insertar con MAX+1 y este script no
--      es el correcto.
SELECT  s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER
FROM    ALL_SEQUENCES s
WHERE   s.SEQUENCE_OWNER = 'CNT'
AND     s.SEQUENCE_NAME IN ('SQ_PLNSCDGO', 'SQ_DTPLCDGO');


-- =====================================================================================
-- EJECUCION
--
-- Se usa la secuencia (NEXTVAL), no claves explicitas. Asi no queda nada que
-- sincronizar despues: es la aplicacion y este script pidiendo numeros a la misma
-- fuente. Es la forma segura cuando la secuencia SI se ejercita, que es este caso.
-- =====================================================================================

-- La cabecera. Empresa y sistema se COPIAN de la plantilla 9, no se escriben a mano.
INSERT INTO CNT.PLNS (PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO, PLNSOBSR, PLNSSSTM)
SELECT  CNT.SQ_PLNSCDGO.NEXTVAL,
        'CRD ENTREGA DE PRESTAMO QUIROGRAFARIO',
        34,
        1,
        p9.PJRQCDGO,
        'Asiento de entrega de prestamo quirografario. Creada 2026-09-01 (decision D7). Auxiliares POSICIONALES 1..8, como la 9 y la 13.',
        p9.PLNSSSTM
FROM    CNT.PLNS p9
WHERE   p9.PLNSCDAL = 9;

-- Las 8 lineas. El PLNSCDGO se resuelve por el alterno 34, recien insertado.
-- MVMN: 1 = DEBE, 2 = HABER.

-- DEBE — los cinco tramos de plazo de la cartera quirografaria (1.3.01.xx)
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10279, 'DE 1 A 30 DIAS',      1, 1, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10280, 'DE 31 A 90 DIAS',     1, 2, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10281, 'DE 91 A 180 DIAS',    1, 3, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10282, 'DE 181 A 360 DIAS',   1, 4, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10283, 'DE MAS DE 360 DIAS',  1, 5, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

-- DEBE — cuenta de orden
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10457, 'CARTERA DE CREDITOS', 1, 6, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

-- HABER — el pagare (unica cuenta de garantia del quirografario) y el socio
INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO,  9979, 'DOCUMENTOS EN GARANTIA', 2, 7, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

INSERT INTO CNT.DTPL (DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN, DTPLAXL1,
                      DTPLAXL2, DTPLAXL3, DTPLAXL4, DTPLAXL5, DTPLESTD)
SELECT  CNT.SQ_DTPLCDGO.NEXTVAL, p.PLNSCDGO, 10398, 'SOCIOS POR PAGAR',       2, 8, 0,0,0,0, 1
FROM    CNT.PLNS p WHERE p.PLNSCDAL = 34;

COMMIT;


-- =====================================================================================
-- CONTROL 1 — DESPUES DE EJECUTAR. Los tres tienen que pasar.
-- =====================================================================================

-- 1.1  La plantilla quedo creada. Esperado: 1 fila, PLNSESTD = 1.
SELECT  p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PLNSESTD, p.PJRQCDGO, p.PLNSSSTM
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 34;

-- 1.2  Las 8 lineas, en orden, con su cuenta. Esperado: exactamente 8 filas,
--      AUX1 de 1 a 8 sin huecos, seis con MVMN=1 y dos con MVMN=2.
SELECT  d.DTPLAXL1                        AS AUX1,
        d.DTPLMVMN                        AS MVMN_1DEBE_2HABER,
        c.PLNNCNTA                        AS CUENTA,
        c.PLNNNMBR                        AS NOMBRE_CUENTA,
        d.DTPLDSCR                        AS DESCRIPCION,
        d.DTPLESTD                        AS ESTADO
FROM    CNT.PLNS p
JOIN    CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
JOIN    CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE   p.PLNSCDAL = 34
ORDER   BY d.DTPLAXL1;

-- 1.3  Contraste contra la 9 y la 13: la 34 debe tener 8 lineas donde ellas tienen 9,
--      y sus auxiliares deben ir 1..8 igual de correlativos que los de ellas.
--      Esperado: 34 -> 8 lineas, MIN 1, MAX 8.
SELECT  p.PLNSCDAL                        AS ALTERNO,
        COUNT(*)                          AS LINEAS,
        MIN(d.DTPLAXL1)                   AS MIN_AUX1,
        MAX(d.DTPLAXL1)                   AS MAX_AUX1,
        SUM(CASE WHEN d.DTPLMVMN = 1 THEN 1 ELSE 0 END) AS DEBES,
        SUM(CASE WHEN d.DTPLMVMN = 2 THEN 1 ELSE 0 END) AS HABERES
FROM    CNT.PLNS p
JOIN    CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
WHERE   p.PLNSCDAL IN (9, 13, 34)
GROUP   BY p.PLNSCDAL
ORDER   BY p.PLNSCDAL;


-- =====================================================================================
-- REVERSO — COMENTADO. Descomentar SOLO si hay que deshacer.
--
-- Borra primero el detalle y despues la cabecera, por la FK. No toca la secuencia:
-- dejar huecos en una secuencia es inofensivo, y "devolverla" si romperia a quien haya
-- insertado en el medio.
-- =====================================================================================
--
-- DELETE FROM CNT.DTPL
--  WHERE PLNSCDGO IN (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 34);
--
-- DELETE FROM CNT.PLNS
--  WHERE PLNSCDAL = 34;
--
-- COMMIT;
--
-- Control del reverso: las dos consultas deben devolver 0 filas.
-- SELECT COUNT(*) FROM CNT.PLNS p WHERE p.PLNSCDAL = 34;
-- SELECT COUNT(*) FROM CNT.DTPL d
--  JOIN CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO WHERE p.PLNSCDAL = 34;


-- =====================================================================================
-- DESPUES DE ESTO
--
-- La plantilla queda creada y SIN USAR: no hay todavia ningun proceso que la invoque.
-- La usa el asiento de entrega del prestamo, que va junto con el desembolso hacia
-- tesoreria — ver §6 de PLAN-CICLO-OTORGAMIENTO.md. Crearla antes es deliberado: es el
-- insumo que ese frente necesita para arrancar, y crearla no cambia el comportamiento
-- de nada que hoy corra.
-- =====================================================================================
