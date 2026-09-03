-- =====================================================================================
-- SANCHEZ PRADO (rol 7508): quitar la afectacion AVPC 145, la que la pantalla volvio
-- invisible por compartir cuota con la 149 — carga 449
-- FECHA: 2026-09-03   EQUIPO: CRD / EQUIPO B
--
-- ⛔ LEER ESTO ANTES DE CORRER NADA. NO ES "BORRAR EL DUPLICADO".
--
--   No existe una fila duplicada. Sus tres afectaciones son distintas:
--
--     AVPC 145   cuota 512966   prestamo 6782    141,40
--     AVPC 149   cuota 512966   prestamo 6782    273,63   <-- misma cuota que la 145
--     AVPC 336   cuota 513050   prestamo 6782     24,56
--                                        TOTAL   439,59
--
--   Su pozo (lo que el archivo le descontó) es 406,73. El exceso es 32,86, y NINGUNA
--   fila vale 32,86 — asi que ningun DELETE por si solo deja el numero correcto.
--
--   Lo que este script hace es otra cosa: sacar la fila que el operador NO PODIA EDITAR.
--   La 145 y la 149 cuelgan de la misma cuota (512966); el dialogo dibuja un input por
--   cuota, asi que mostraba una y la otra quedaba fuera de alcance — invisible, no
--   corregible, y sumando igual. Sacandola, quedan 298,19 y la pantalla vuelve a decir
--   la verdad. Los que falten hasta el pozo se afectan DESDE LA PANTALLA, que es donde
--   se decide a que cuota va cada dolar. Eso no lo decide un SQL.
--
-- ⛔ NO CORRER LOS BLOQUES 2 Y 3 HASTA HABER LEIDO LA SALIDA DEL BLOQUE 1.
--    El bloque 1 mide; si sus numeros no son los de arriba, la base cambio desde el
--    diagnostico y hay que rehacerlo, no seguir.
--
-- ⛔ Y NO AFECTAR NADA DESDE LA PANTALLA hasta que el arbitro confirme que el guardado
--    dejo de duplicar. Hoy cada guardado puede volver a dejar filas huerfanas.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los bloques 1 y 4.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449
DEFINE ROL   = 7508


-- =====================================================================================
-- BLOQUE 1 — CONTROL ANTES. Mide, no toca nada.
--
-- 1.a  Las afectaciones manuales que tiene hoy.
--      Esperado: las tres filas de arriba, TOTAL 439,59.
-- =====================================================================================
SELECT  a.AVPCCDGO                                              AS AVPC,
        a.NVPCCDGO                                              AS NOVEDAD,
        a.PRSTCDGO                                              AS PRESTAMO,
        a.DTPRCDGO                                              AS CUOTA,
        a.TPAPCDGO                                              AS TIPO_APORTE,
        a.AVPCVAFA                                              AS VALOR_AFECTADO
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO IN (145, 149, 336)
ORDER   BY a.AVPCCDGO;

SELECT  ROUND(SUM(NVL(a.AVPCVAFA,0)), 2)                        AS TOTAL_AFECTADO_HOY
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO IN (145, 149, 336);


-- =====================================================================================
-- 1.b  Su pozo real: lo que el archivo le descontó, todos los productos.
--      Esperado: 406,73 repartido entre sus productos.
-- =====================================================================================
SELECT  p.PXCACDPT                                              AS ROL,
        SUBSTR(p.PXCANMBR,1,30)                                 AS PARTICIPE,
        p.PXCACDPR                                              AS PRODUCTO,
        ROUND(NVL(p.PXCADSDO,0), 2)                             AS DESCONTADO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT = &ROL
ORDER   BY p.PXCACDPR;

SELECT  ROUND(SUM(NVL(p.PXCADSDO,0)), 2)                        AS POZO_TOTAL
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT = &ROL;


-- =====================================================================================
-- 1.c  ⛔ EL QUE MAS IMPORTA, y es el que decide cuanto podes reafectar despues.
--
--      Pagos ya aplicados a sus prestamos en esta carga por el camino AUTOMATICO (los
--      que NO vienen de afectacion manual). El invariante real no es "manual <= pozo",
--      es "MANUAL + AUTOMATICO <= pozo" — ver la cabecera de sql/184, donde esto se
--      identifico como el origen de los 79,44 que quedaban.
--
--      Como leerlo:
--        Si devuelve 0 filas -> el tope para reafectar es 406,73 - 298,19 = 108,54.
--        Si devuelve p. ej. 57,79 -> el tope baja a 406,73 - 57,79 - 298,19 = 50,75.
--      NO reafectar sin mirar esto: es la diferencia entre cuadrar y volver a pasarse.
-- =====================================================================================
SELECT  g.PGPRCDGO                                              AS PAGO,
        g.PRSTCDGO                                              AS PRESTAMO,
        ROUND(NVL(g.PGPRVLRR,0), 2)                             AS VALOR,
        SUBSTR(g.PGPROBSR,1,60)                                 AS OBSERVACION
FROM    CRD.PGPR g
JOIN    CRD.PRST pr ON pr.PRSTCDGO = g.PRSTCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO  = pr.ENTDCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL,0) = 0
AND     e.ENTDRLPC = &ROL
ORDER   BY g.PGPRCDGO;


-- =====================================================================================
-- BLOQUE 2 — RESPALDO. Se corre ANTES del delete, y es lo que hace reversible el paso 3.
--
-- Copia la fila entera a una tabla aparte. No hace falta que yo adivine el valor de cada
-- columna para poder devolverla: se restaura desde aca.
-- =====================================================================================
CREATE TABLE CRD.AVPC_BKP_185 AS
SELECT  a.*
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO = 145;

-- Tiene que devolver 1. Si devuelve 0, PARAR: la fila 145 ya no existe y el bloque 3
-- no tiene nada que hacer.
SELECT  COUNT(*) AS FILAS_RESPALDADAS FROM CRD.AVPC_BKP_185;


-- =====================================================================================
-- BLOQUE 3 — EL DELETE.
--
-- Con guardas de valor Y cuota Y prestamo ademas del codigo: si la fila 145 no es la que
-- este script cree que es, no borra nada en vez de borrar la equivocada.
--
-- Tiene que reportar 1 fila borrada. Si reporta 0, PARAR y avisar.
-- =====================================================================================
DELETE  FROM CRD.AVPC a
WHERE   a.AVPCCDGO = 145
AND     a.DTPRCDGO = 512966
AND     a.PRSTCDGO = 6782
AND     ROUND(NVL(a.AVPCVAFA,0), 2) = 141.40;

COMMIT;


-- =====================================================================================
-- BLOQUE 4 — CONTROL DESPUES.
--
-- Esperado:
--   AVPC_RESTANTES        2   (la 149 y la 336)
--   TOTAL_AFECTADO   298,19
--   Y ninguna cuota con mas de una afectacion.
-- =====================================================================================
SELECT  a.AVPCCDGO                                              AS AVPC,
        a.NVPCCDGO                                              AS NOVEDAD,
        a.DTPRCDGO                                              AS CUOTA,
        a.AVPCVAFA                                              AS VALOR_AFECTADO
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO IN (145, 149, 336)
ORDER   BY a.AVPCCDGO;

SELECT  COUNT(*)                                                AS AVPC_RESTANTES,
        ROUND(SUM(NVL(a.AVPCVAFA,0)), 2)                        AS TOTAL_AFECTADO
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO IN (145, 149, 336);

-- Ninguna cuota suya debe quedar con mas de una afectacion. Esperado: 0 filas.
SELECT  a.NVPCCDGO                                              AS NOVEDAD,
        a.DTPRCDGO                                              AS CUOTA,
        COUNT(*)                                                AS AFECTACIONES,
        ROUND(SUM(NVL(a.AVPCVAFA,0)), 2)                        AS SUMA
FROM    CRD.AVPC a
WHERE   a.AVPCCDGO IN (149, 336)
GROUP   BY a.NVPCCDGO, a.DTPRCDGO
HAVING  COUNT(*) > 1;


-- =====================================================================================
-- BLOQUE REVERSO — COMENTADO. Devuelve la fila 145 exactamente como estaba.
--
-- Correr SOLO si hay que deshacer. Despues de esto, el total vuelve a 439,59 y el
-- exceso a 32,86.
-- =====================================================================================
-- INSERT INTO CRD.AVPC
-- SELECT * FROM CRD.AVPC_BKP_185;
--
-- COMMIT;
--
-- -- Verificacion del reverso: tiene que devolver 3 filas y 439,59.
-- SELECT COUNT(*) AS AVPC, ROUND(SUM(NVL(AVPCVAFA,0)),2) AS TOTAL
-- FROM   CRD.AVPC WHERE AVPCCDGO IN (145, 149, 336);


-- =====================================================================================
-- LIMPIEZA — COMENTADA. Correr solo cuando la carga 449 haya cerrado bien y ya no
-- haga falta poder revertir.
-- =====================================================================================
-- DROP TABLE CRD.AVPC_BKP_185;


-- =====================================================================================
-- QUE SIGUE, DESPUES DE ESTE SCRIPT
--
-- 1. Quedan 298,19 afectados de un pozo de 406,73.
-- 2. Cuanto se puede reafectar sale del bloque 1.c, NO de restar 406,73 - 298,19 a ojo.
-- 3. La reafectacion se hace DESDE LA PANTALLA, y recien cuando el arbitro confirme que
--    el guardado dejo de duplicar. Antes de eso, cada guardado puede volver a crear el
--    mismo problema que este script acaba de limpiar.
-- =====================================================================================
