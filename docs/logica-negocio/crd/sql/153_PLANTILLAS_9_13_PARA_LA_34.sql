-- =====================================================================================
-- DETALLE DE LAS PLANTILLAS 9 Y 13 — insumo para escribir la plantilla 34
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- POR QUE EXISTE: reemplaza al BLOQUE 2 del script 151, que fallo en produccion con
--
--     ORA-00904: "C"."PLNNCDCT": identificador no valido
--
-- La causa: el 151 pedia c.PLNNCDCT y esa columna NO EXISTE. En CNT.PLNN la cuenta
-- contable es PLNNCNTA (entidad PlanCuenta, campo cuentaContable). El nombre estaba
-- inventado, no verificado contra la entidad.
--
-- Verificadas UNA POR UNA contra las entidades JPA antes de escribir esto:
--   CNT.PLNS  -> PLNSCDGO, PLNSNMBR, PLNSCDAL, PLNSESTD, PJRQCDGO   (Plantilla)
--   CNT.DTPL  -> DTPLCDGO, PLNSCDGO, PLNNCDGO, DTPLDSCR, DTPLMVMN,
--                DTPLAXL1..DTPLAXL5, DTPLFCIN, DTPLFCFN, DTPLESTD   (DetallePlantilla)
--   CNT.PLNN  -> PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNESTD            (PlanCuenta)
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cuatro bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 250


-- =====================================================================================
-- BLOQUE 1 — El detalle completo de las plantillas 9 (prendario) y 13 (hipotecario)
--
-- Es lo que hay que leer para escribir la 34 (entrega quirografaria) SIN copiar
-- auxiliares a ciegas.
--
-- ⚠️ AVISO HEREDADO DEL EQUIPO A, y es la trampa que este bloque viene a evitar:
--    de las plantillas de CRD **solo la 21 esta renumerada al catalogo semantico**;
--    la 25, la 27 y la 29 usan auxiliares POSICIONALES. Si la 9 y la 13 tambien son
--    posicionales, copiar sus numeros a la 34 deja el asiento MAL CLASIFICADO
--    Y CUADRADO IGUAL — o sea, sin ninguna senal de error.
--
-- Como leerlo:
--   * MVMN: 1 = DEBE, 2 = HABER. Cada plantilla tiene que tener al menos uno de cada.
--   * AUX1..AUX5: si los numeros son 1,2,3,4... correlativos y arrancan en 1 por
--     plantilla, son POSICIONALES. Si son codigos dispersos (43, 21, 15...), son
--     semanticos. Esa es la pregunta que decide como se escribe la 34.
--   * VIGENTE_HASTA con fecha = linea ya cerrada, no copiarla.
-- =====================================================================================
SELECT  p.PLNSCDAL                                       AS ALTERNO,
        p.PLNSNMBR                                       AS PLANTILLA,
        d.DTPLCDGO                                       AS ID_LINEA,
        d.DTPLMVMN                                       AS MVMN_1DEBE_2HABER,
        d.DTPLAXL1                                       AS AUX1,
        d.DTPLAXL2                                       AS AUX2,
        d.DTPLAXL3                                       AS AUX3,
        d.DTPLAXL4                                       AS AUX4,
        d.DTPLAXL5                                       AS AUX5,
        c.PLNNCDGO                                       AS ID_CUENTA,
        c.PLNNCNTA                                       AS CUENTA,
        c.PLNNNMBR                                       AS NOMBRE_CUENTA,
        d.DTPLDSCR                                       AS DESCRIPCION_LINEA,
        d.DTPLESTD                                       AS ESTADO_LINEA,
        TO_CHAR(d.DTPLFCFN, 'YYYY-MM-DD')                AS VIGENTE_HASTA
FROM    CNT.PLNS p
JOIN    CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
JOIN    CNT.PLNN c ON c.PLNNCDGO = d.PLNNCDGO
WHERE   p.PLNSCDAL IN (9, 13)
ORDER   BY p.PLNSCDAL, d.DTPLMVMN, d.DTPLAXL1;


-- =====================================================================================
-- BLOQUE 2 — ¿Los auxiliares son posicionales o semanticos? El control directo
--
-- Como leerlo: si MIN_AUX1 = 1 y MAX_AUX1 = LINEAS (o sea 1..n sin huecos), los
-- auxiliares de esa plantilla son POSICIONALES y NO se pueden copiar a la 34.
-- Si MAX_AUX1 es mucho mayor que LINEAS, son codigos del catalogo semantico.
-- =====================================================================================
SELECT  p.PLNSCDAL                                       AS ALTERNO,
        COUNT(*)                                         AS LINEAS,
        MIN(d.DTPLAXL1)                                  AS MIN_AUX1,
        MAX(d.DTPLAXL1)                                  AS MAX_AUX1,
        COUNT(DISTINCT d.DTPLAXL1)                       AS AUX1_DISTINTOS
FROM    CNT.PLNS p
JOIN    CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
WHERE   p.PLNSCDAL IN (9, 13, 21, 25, 27, 29)
GROUP   BY p.PLNSCDAL
ORDER   BY p.PLNSCDAL;

-- La 21 esta renumerada al catalogo semantico y la 25/27/29 son posicionales: sirven
-- de referencia para reconocer cada patron a simple vista.


-- =====================================================================================
-- BLOQUE 3 — Las cuentas de la familia quirografaria, para los IDs de la plantilla 34
--
-- Esperado: las cinco de 1.3.01.% (por vencer, no devengado, vencido, judicial,
-- reestructurado, segun el plan de cuentas) mas las de ingreso y la transitoria.
-- =====================================================================================
SELECT  c.PLNNCDGO                                       AS ID_CUENTA,
        c.PLNNCNTA                                       AS CUENTA,
        c.PLNNNMBR                                       AS NOMBRE,
        c.PLNNESTD                                       AS ESTADO
FROM    CNT.PLNN c
WHERE   c.PLNNCNTA LIKE '1.3.01.%'
   OR   c.PLNNCNTA IN ('7.3.01.05', '7.4.01.05', '2.3.90.90.10')
ORDER   BY c.PLNNCNTA;


-- =====================================================================================
-- BLOQUE 4 — Reconfirmar que el alterno 34 sigue libre
--
-- El bloque 3 del script 151 ya dio vacio (libre) el 2026-09-01. Se repite porque
-- entre esa corrida y la creacion de la plantilla puede pasar tiempo, y la regla 2 del
-- registro de reservas pide volver a correr el control JUSTO ANTES de ejecutar.
--
-- Esperado: primera consulta 0 filas. Si devuelve algo, PARAR: alguien tomo el 34.
-- =====================================================================================
SELECT  p.PLNSCDAL AS ALTERNO, p.PLNSNMBR AS NOMBRE, p.PLNSESTD AS ESTADO
FROM    CNT.PLNS p
WHERE   p.PLNSCDAL = 34;

SELECT  MAX(p.PLNSCDAL)                                  AS MAX_ALTERNO_USADO,
        COUNT(*)                                         AS TOTAL_PLANTILLAS
FROM    CNT.PLNS p;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================
