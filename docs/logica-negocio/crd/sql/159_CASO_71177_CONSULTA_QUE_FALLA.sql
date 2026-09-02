-- =====================================================================================
-- CASO 71177 — por que FALLA la consulta de cuotas/pagos de ese prestamo
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas. Se puede correr en
--    produccion en cualquier momento, tambien en horario laboral.
--
-- DE DONDE SALE ESTE NUMERO: del aviso nuevo de la pantalla de afectacion, desplegado
-- el 2026-09-02: «No se pudieron cargar las cuotas de este prestamo: Prestamo 71177».
--
-- ⛔ ESO YA NO ES "no hay prestamos": ES UNA CONSULTA QUE FALLA.
--    Hasta ayer los dos casos se veian identicos, y sobre esa ambiguedad se armaron tres
--    diagnosticos, dos equivocados. El aviso los separo. Ahora falta el POR QUE.
--
-- ⚠️ 71177 puede ser PRSTIDAS (id Asoprep) o PRSTCDGO: el banner imprime
--    `idAsoprep || codigo`, o sea el idAsoprep si existe. El bloque 1 resuelve cual es.
--
-- LAS DOS CAUSAS POSIBLES, las dos con el MISMO sintoma:
--   E1  La consulta devuelve un HTTP no-200 (500) -> catchError -> cuotas vacias.
--   E2  La consulta devuelve 200 pero Angular no puede interpretar el cuerpo (truncado,
--       enorme, no-JSON) -> HttpErrorResponse con status 200 -> el handleError compartido
--       devuelve of(null) -> el consumidor lo lee como "sin datos".
--       Ver REGISTRO-RESERVAS-EQUIPOS.md §6.1.
--
-- La hipotesis principal es E2 POR VOLUMEN: la pantalla pide TODOS los pagos del
-- prestamo sin ningun tope. Este script mide exactamente eso.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida completa de los cinco bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 230

DEFINE NUMERO = 71177


-- =====================================================================================
-- BLOQUE 1 — Identificar el prestamo: el numero es PRSTIDAS o PRSTCDGO?
--
-- Como leerlo: la fila que aparezca dice cual es. Anotar el PRSTCDGO real, que es el que
-- usan los bloques siguientes.
-- =====================================================================================
SELECT  'por PRSTIDAS' AS ENCONTRADO_POR,
        p.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST AS ESTADO, p.ENTDCDGO AS ID_PARTICIPE,
        p.PRDCCDGO AS ID_PRODUCTO, p.PRSTMNSL AS MONTO,
        TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD') AS INICIO
FROM    CRD.PRST p
WHERE   p.PRSTIDAS = &NUMERO
UNION ALL
SELECT  'por PRSTCDGO',
        p.PRSTCDGO, p.PRSTIDAS, p.PRSTIDST, p.ENTDCDGO,
        p.PRDCCDGO, p.PRSTMNSL,
        TO_CHAR(p.PRSTFCIN,'YYYY-MM-DD')
FROM    CRD.PRST p
WHERE   p.PRSTCDGO = &NUMERO;


-- =====================================================================================
-- BLOQUE 2 — EL VOLUMEN, que es la hipotesis principal
--
-- La pantalla hace DOS consultas por prestamo, las dos sin tope:
--   detallePrestamoService.selectByCriteria  -> todas las cuotas
--   pagoPrestamoService.selectByCriteria     -> TODOS los pagos
--
-- Y `PagoPrestamo` es una entidad ANCHA: se serializa entera a JSON, con sus relaciones.
--
-- Como leerlo:
--   * PAGOS en el orden de los cientos o miles -> E2 por volumen es la explicacion, y la
--     solucion es acotar la consulta de pagos, no "arreglar" nada del backend.
--   * PAGOS parecido a otros prestamos que SI cargan (bloque 4) -> el volumen no es, y
--     hay que ir al log del servidor por E1.
-- =====================================================================================
SELECT  p.PRSTCDGO                                          AS PRESTAMO,
        p.PRSTIDAS                                          AS ID_ASOPREP,
        (SELECT COUNT(*) FROM CRD.DTPR d
          WHERE d.PRSTCDGO = p.PRSTCDGO)                    AS CUOTAS,
        (SELECT COUNT(*) FROM CRD.PGPR g
          WHERE g.PRSTCDGO = p.PRSTCDGO)                    AS PAGOS,
        (SELECT COUNT(*) FROM CRD.PGPR g
          WHERE g.PRSTCDGO = p.PRSTCDGO
            AND NVL(g.PGPRANUL,0) = 0)                      AS PAGOS_VIGENTES
FROM    CRD.PRST p
WHERE   p.PRSTIDAS = &NUMERO OR p.PRSTCDGO = &NUMERO;


-- =====================================================================================
-- BLOQUE 3 — Datos que revientan un JSON: texto raro en las columnas de cadena
--
-- La otra forma de E2, sin volumen: un caracter que rompe la serializacion o el parseo
-- (comillas sin escapar, saltos de linea, caracteres de control).
--
-- Como leerlo: si alguna GLOSA/OBSERVACION trae saltos de linea o caracteres de control,
-- es candidato. Esperado normalmente: 0 filas.
-- =====================================================================================
SELECT  'PGPR' AS TABLA, g.PGPRCDGO AS ID,
        LENGTH(g.PGPROBSR) AS LARGO,
        SUBSTR(g.PGPROBSR, 1, 60) AS TEXTO
FROM    CRD.PGPR g
WHERE   (g.PRSTCDGO IN (SELECT p.PRSTCDGO FROM CRD.PRST p
                         WHERE p.PRSTIDAS = &NUMERO OR p.PRSTCDGO = &NUMERO))
AND     (REGEXP_LIKE(g.PGPROBSR, '[[:cntrl:]]') OR LENGTH(g.PGPROBSR) > 400);


-- =====================================================================================
-- BLOQUE 4 — La comparacion: cuanto pesa este prestamo contra el resto
--
-- Como leerlo: si el 71177 esta MUY por encima del maximo tipico, el volumen es la
-- causa. Si esta en el promedio, no lo es.
-- =====================================================================================
SELECT  ROUND(AVG(t.PAGOS), 1)                              AS PAGOS_PROMEDIO,
        MAX(t.PAGOS)                                        AS PAGOS_MAXIMO,
        ROUND(AVG(t.CUOTAS), 1)                             AS CUOTAS_PROMEDIO,
        MAX(t.CUOTAS)                                       AS CUOTAS_MAXIMO,
        COUNT(*)                                            AS PRESTAMOS_VIVOS
FROM    (SELECT p.PRSTCDGO,
                (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = p.PRSTCDGO) AS CUOTAS,
                (SELECT COUNT(*) FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO) AS PAGOS
         FROM   CRD.PRST p
         WHERE  p.PRSTIDST IN (2, 11)) t;

-- 4.b El top 10 por cantidad de pagos. Si el 71177 aparece aca arriba, es volumen.
--     Y ademas: TODOS los que aparezcan aca van a tener el mismo problema.
SELECT  p.PRSTCDGO                                          AS PRESTAMO,
        p.PRSTIDAS                                          AS ID_ASOPREP,
        p.PRSTIDST                                          AS ESTADO,
        (SELECT COUNT(*) FROM CRD.PGPR g
          WHERE g.PRSTCDGO = p.PRSTCDGO)                    AS PAGOS,
        (SELECT COUNT(*) FROM CRD.DTPR d
          WHERE d.PRSTCDGO = p.PRSTCDGO)                    AS CUOTAS
FROM    CRD.PRST p
WHERE   p.PRSTIDST IN (2, 11)
ORDER   BY (SELECT COUNT(*) FROM CRD.PGPR g WHERE g.PRSTCDGO = p.PRSTCDGO) DESC
FETCH FIRST 10 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 5 — El participe completo, para reproducirlo en pantalla
--
-- Como leerlo: da el codigo Petro con el que volver a abrir el dialogo, y cuantos de sus
-- prestamos cargan bien. Si el participe tiene otros prestamos que SI se muestran, el
-- problema es de ESE prestamo y no del participe.
-- =====================================================================================
SELECT  e.ENTDRLPC                                          AS CODIGO_PETRO,
        e.ENTDNMID                                          AS CEDULA,
        SUBSTR(e.ENTDRZNS,1,40)                             AS PARTICIPE,
        p.PRSTCDGO                                          AS PRESTAMO,
        p.PRSTIDAS                                          AS ID_ASOPREP,
        p.PRSTIDST                                          AS ESTADO,
        (SELECT COUNT(*) FROM CRD.PGPR g
          WHERE g.PRSTCDGO = p.PRSTCDGO)                    AS PAGOS,
        (SELECT COUNT(*) FROM CRD.DTPR d
          WHERE d.PRSTCDGO = p.PRSTCDGO
            AND (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4,7))) AS CUOTAS_PENDIENTES
FROM    CRD.PRST p
JOIN    CRD.ENTD e ON e.ENTDCDGO = p.ENTDCDGO
WHERE   p.ENTDCDGO = (SELECT MIN(x.ENTDCDGO) FROM CRD.PRST x
                       WHERE x.PRSTIDAS = &NUMERO OR x.PRSTCDGO = &NUMERO)
ORDER   BY p.PRSTCDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los cinco bloques.
-- =====================================================================================
