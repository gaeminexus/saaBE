-- =====================================================================================
-- PLANTILLAS 21 / 27 / 28 — consulta CORREGIDA
-- FECHA: 2026-08-31
--
-- SOLO LECTURA. Reemplaza las consultas 4.1 y 4.2 del script 91, que estaban mal escritas.
--
-- El error: la tabla de plantillas es CNT.PLNS (no CNT.PLNT), su alterno es PLNSCDAL
-- (no PLNTALTR), CNT.DTPL enlaza por PLNSCDGO (no PLNTCDGO) y el movimiento es DTPLMVMN
-- (no DTPLMVTO). Verificado contra com.saa.model.cnt.Plantilla y DetallePlantilla.
-- =====================================================================================


-- =====================================================================================
-- 1. LAS PLANTILLAS
-- =====================================================================================

-- 1.1 Esperado: la 21 seguro; la 27 y la 28 si existen. Anota los PLNSCDGO.
--     PLNSSSTM marca las de sistema; PLNSESTD el estado.
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, p.PLNSNMBR AS NOMBRE,
       p.PJRQCDGO AS EMPRESA, p.PLNSESTD AS ESTADO
FROM   CNT.PLNS p
WHERE  p.PLNSCDAL IN (21, 27, 28)
ORDER  BY p.PLNSCDAL;


-- =====================================================================================
-- 2. LAS LINEAS — lo que decide si se reusa la 27 o se crea una nueva
-- =====================================================================================

-- 2.1 Lineas de las tres plantillas, con su auxiliar y su cuenta.
--     DTPLMVMN: 1 = DEBE, otro = HABER (asi lo lee lineaDesdePlantilla).
--
--     QUE MIRAR, en dos puntos:
--
--     a) EN LA 21, los auxiliares 50 y 51. El agente de backend afirma que resuelven a
--        2.1.01.05.01 (cesantia) y 2.1.02.05.01 (jubilacion) — las mismas cuentas que
--        necesita el DEBE de la reclasificacion. Si es cierto, esa mitad del asiento se
--        reusa tal cual y no hay nada que crear. SI RESUELVEN A OTRA COSA, PARAR: el
--        asiento saldria mal clasificado y cuadrando igual.
--
--     b) EN LA 27, si los DTPLAXL1 son SEMANTICOS o POSICIONALES. La unica plantilla de
--        CRD confirmada como renumerada al catalogo semantico es la 21. Si la 27 trae
--        auxiliares 1,2,3,4 (posicionales), NO se puede resolver por auxiliar: hay que
--        renumerarla o crear una plantilla nueva. Es exactamente el bug que ya paso con
--        la 25 en la condonacion (aux1=10 que ahi era una banda posicional).
SELECT p.PLNSCDAL   AS ALTERNO,
       d.DTPLAXL1   AS AUX1,
       d.DTPLAXL2   AS AUX2,
       d.DTPLMVMN   AS MOVIMIENTO,
       n.PLNNCDGO   AS CUENTA_ID,
       n.PLNNCNTA   AS CUENTA,
       n.PLNNNMBR   AS CUENTA_NOMBRE,
       d.DTPLDSCR   AS DESCRIPCION,
       d.DTPLESTD   AS ESTADO
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL IN (21, 27, 28)
ORDER  BY p.PLNSCDAL, d.DTPLAXL1, d.DTPLAXL2;


-- =====================================================================================
-- 3. FOCO: solo los auxiliares 50 y 51 de la 21
-- =====================================================================================

-- 3.1 La consulta de arriba puede traer muchas filas (la 21 tiene decenas). Esta trae solo
--     lo que decide el DEBE de la reclasificacion.
--     Esperado si el agente tiene razon: 2 filas, cuentas 2.1.01.05.01 y 2.1.02.05.01
--     (PLNNCDGO 10349 y 10354, que ya sabemos que existen y son de la empresa 1236).
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL = 21
AND    d.DTPLAXL1 IN (50, 51, 52)
ORDER  BY d.DTPLAXL1;


-- =====================================================================================
-- 4. QUE HACER CON EL RESULTADO — pasarselo al arbitro
-- =====================================================================================
-- Con 3.1 se confirma o se descarta que el DEBE ya este resuelto.
-- Con 2.1 sobre la 27 se decide: reusarla, renumerarla, o crear una plantilla nueva para
-- el HABER de liquidacion (2.3.01.05.01 / 2.3.01.10.01, PLNNCDGO 10360 y 10362).
--
-- El arbitro necesita, para cerrar el codigo del backend, exactamente tres numeros:
--   - el ALTERNO de la plantilla de liquidacion,
--   - el AUX1 de la linea de cesantia,
--   - el AUX1 de la linea de jubilacion.
-- El backend ya dejo esos tres como constantes aisladas, con valores invalidos a proposito
-- (alterno 0, aux1 -50 y -51), asi que cambiarlos es una linea cada uno.
-- =====================================================================================
