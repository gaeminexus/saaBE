-- =====================================================================================
-- ⭐ POR QUE NO SE GENERAN LOS Gs — el candidato numero uno, comprobable HOY sin desplegar
-- FECHA: 2026-09-08   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 219 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los cuatro bloques son SELECT. Segundos.
--
-- =====================================================================================
-- CONTEXTO
--
--   Al generar los G40-G51 de agosto 2026 la pantalla mostro:
--       "could not prepare statement [JI031070: Transaction cannot proceed:
--        STATUS_MARKED_ROLLBACK] [update RPR.EJRD set ...]"
--
--   Ese mensaje NO es el error: es el sintoma. La transaccion ya venia marcada para
--   rollback, y lo que reventó fue el intento de dejar constancia del fallo. El error real
--   quedo tapado. Eso se corrige en codigo (ya despachado) y necesita el WAR.
--
--   ⭐ PERO EL CANDIDATO SE PUEDE COMPROBAR AHORA, SIN DESPLEGAR NADA. De los doce
--     generadores, **uno solo lanza una excepcion explicita**: GeneracionG48ServiceImpl:159.
--     Y lo hace por una razon de PARAMETRIZACION, no por un defecto:
--
--       "No se puede generar el G48: N producto(s) sin configuracion de calificacion de
--        riesgo vigente al {fecha} en CRD.CFCR: {lista}. Configure la calificacion de esos
--        productos antes de generar el reporte."
--
--   Si el BLOQUE 1 devuelve filas, esa es —con altisima probabilidad— la causa, y se
--   arregla desde la pantalla de configuracion de calificacion de riesgo. **Sin WAR, sin
--   script de datos, sin tocar codigo.**
--
--   ⚠️ Si el BLOQUE 1 sale VACIO, el G48 no es la causa y hay que esperar el WAR para que
--     el error real se haga visible. Cualquiera de los otros once puede estar lanzando una
--     RuntimeException no intencional (un dato faltante, un null), y esas marcan rollback
--     igual que una excepcion explicita. No se puede adivinar cual: hay que verlo.
--
-- =====================================================================================
-- LA CONSULTA ES LA MISMA QUE CORRE EL SISTEMA, traducida a SQL
--   ConfiguracionCalificacionRiesgoDaoServiceImpl.selectProductosSinConfiguracionVigente:67
--
--       select p from Producto p where not exists (
--           select 1 from ConfiguracionCalificacionRiesgo c
--            where c.producto = p and c.estado = 1
--              and c.fechaInicio <= :fecha
--              and (c.fechaFin is null or c.fechaFin >= :fecha))
--
--   La fecha que usa es el ULTIMO DIA DEL MES generado: para agosto 2026, 2026-08-31.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — ⭐ LA RESPUESTA: productos SIN configuracion vigente al 2026-08-31.
--
-- Si devuelve filas: esa es la causa. Cada fila es un producto que hay que configurar.
-- Si devuelve 0 filas: el G48 no es el problema, avisame.
-- =====================================================================================

SELECT p.PRDCCDGO                        AS PRODUCTO,
       p.PRDCNMBR                        AS NOMBRE,
       p.PRDCESTD                        AS ESTADO_PRODUCTO,
       CASE p.PRDCESTD WHEN 1 THEN 'ACTIVO' WHEN 0 THEN 'INACTIVO'
            ELSE TO_CHAR(p.PRDCESTD) END AS ESTADO_NOMBRE,
       (SELECT COUNT(*) FROM CRD.CFCR c2 WHERE c2.PRDCCDGO = p.PRDCCDGO)
                                         AS CONFIGS_QUE_TIENE,
       (SELECT COUNT(*) FROM CRD.PRST pr WHERE pr.PRDCCDGO = p.PRDCCDGO)
                                         AS PRESTAMOS_DEL_PRODUCTO
  FROM CRD.PRDC p
 WHERE NOT EXISTS (SELECT 1
                     FROM CRD.CFCR c
                    WHERE c.PRDCCDGO = p.PRDCCDGO
                      AND c.CFCRESTD = 1
                      AND c.CFCRFCIN <= DATE '2026-08-31'
                      AND (c.CFCRFCFN IS NULL OR c.CFCRFCFN >= DATE '2026-08-31'))
 ORDER BY p.PRDCCDGO;


-- =====================================================================================
-- BLOQUE 2 — ⚠️ EL MATIZ QUE PUEDE AHORRAR TRABAJO INUTIL.
--
-- La consulta del sistema recorre **TODOS** los productos de CRD.PRDC: no filtra por
-- estado del producto ni por empresa. O sea que **un producto INACTIVO, viejo o de prueba,
-- sin cartera y sin configuracion, tambien hace fallar el G48 entero**.
--
-- Este bloque separa los que importan de los que no. Si todos los del BLOQUE 1 salen como
-- 'inactivo y sin cartera', el problema no es de parametrizacion sino de que la consulta
-- es demasiado amplia — y eso se corrige en codigo, no configurando productos muertos.
--
-- ⛔ NO configures a mano productos que no se usan solo para callar el error: estarias
--    metiendo parametria falsa en la base para tapar un defecto de la consulta. Si este
--    bloque muestra ese caso, avisame.
-- =====================================================================================

SELECT CASE WHEN p.PRDCESTD = 1 AND NVL(x.PRESTAMOS, 0) > 0
                 THEN '1. ACTIVO con cartera — HAY QUE CONFIGURARLO'
            WHEN p.PRDCESTD = 1
                 THEN '2. activo pero sin cartera — revisar si se usa'
            WHEN NVL(x.PRESTAMOS, 0) > 0
                 THEN '3. inactivo pero CON cartera — mirar con cuidado'
            ELSE '4. inactivo y sin cartera — NO deberia frenar el reporte'
       END                                AS CLASIFICACION,
       COUNT(*)                           AS PRODUCTOS,
       LISTAGG(p.PRDCCDGO, ', ') WITHIN GROUP (ORDER BY p.PRDCCDGO) AS CUALES
  FROM CRD.PRDC p
  LEFT JOIN (SELECT pr.PRDCCDGO, COUNT(*) AS PRESTAMOS
               FROM CRD.PRST pr GROUP BY pr.PRDCCDGO) x
    ON x.PRDCCDGO = p.PRDCCDGO
 WHERE NOT EXISTS (SELECT 1
                     FROM CRD.CFCR c
                    WHERE c.PRDCCDGO = p.PRDCCDGO
                      AND c.CFCRESTD = 1
                      AND c.CFCRFCIN <= DATE '2026-08-31'
                      AND (c.CFCRFCFN IS NULL OR c.CFCRFCFN >= DATE '2026-08-31'))
 GROUP BY CASE WHEN p.PRDCESTD = 1 AND NVL(x.PRESTAMOS, 0) > 0
                    THEN '1. ACTIVO con cartera — HAY QUE CONFIGURARLO'
               WHEN p.PRDCESTD = 1
                    THEN '2. activo pero sin cartera — revisar si se usa'
               WHEN NVL(x.PRESTAMOS, 0) > 0
                    THEN '3. inactivo pero CON cartera — mirar con cuidado'
               ELSE '4. inactivo y sin cartera — NO deberia frenar el reporte'
          END
 ORDER BY 1;


-- =====================================================================================
-- BLOQUE 3 — QUE CONFIGURACION HAY HOY, y por que alguna pudo quedar fuera de vigencia.
--
-- Ojo con las que tienen CFCRFCFN (fecha fin) anterior al 31 de agosto: esas EXISTEN pero
-- ya vencieron, y para el sistema el producto "no tiene configuracion". Es la causa mas
-- facil de pasar por alto, porque en la pantalla la configuracion se ve.
-- =====================================================================================

SELECT c.CFCRCDGO                          AS CONFIG,
       c.PRDCCDGO                          AS PRODUCTO,
       p.PRDCNMBR                          AS NOMBRE_PRODUCTO,
       c.CFCRNMBR                          AS NOMBRE_CONFIG,
       c.PJRQCDGO                          AS EMPRESA,
       c.CFCRESTD                          AS ESTADO,
       TO_CHAR(c.CFCRFCIN, 'YYYY-MM-DD')   AS VIGENTE_DESDE,
       TO_CHAR(c.CFCRFCFN, 'YYYY-MM-DD')   AS VIGENTE_HASTA,
       CASE WHEN c.CFCRESTD <> 1                       THEN 'NO cuenta: estado distinto de activo'
            WHEN c.CFCRFCIN >  DATE '2026-08-31'       THEN 'NO cuenta: empieza despues del corte'
            WHEN c.CFCRFCFN IS NOT NULL
                 AND c.CFCRFCFN < DATE '2026-08-31'    THEN 'NO cuenta: YA VENCIO antes del corte'
            ELSE 'vigente al 2026-08-31' END           AS SIRVE_PARA_AGOSTO
  FROM CRD.CFCR c
  JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
 ORDER BY c.PRDCCDGO, c.CFCRFCIN;


-- =====================================================================================
-- BLOQUE 4 — CONTROL: como quedo la ultima corrida de los Gs.
--
-- Si la transaccion se revirtio entera, es probable que no haya quedado NADA de esta
-- corrida. Lo que aparezca es de corridas anteriores. Sirve para ver si alguna vez quedo
-- registrado el motivo real de un fallo.
-- =====================================================================================

SELECT e.EJRCCDGO                          AS EJECUCION,
       e.EJRCMESS                          AS MES,
       e.EJRCANOO                          AS ANIO,
       e.EJRCESTD                          AS ESTADO,
       e.EJRCUSRO                          AS USUARIO,
       TO_CHAR(e.EJRCFCGN, 'YYYY-MM-DD')   AS FECHA,
       SUBSTR(e.EJRCOBSR, 1, 120)          AS OBSERVACIONES
  FROM RPR.EJRC e
 ORDER BY e.EJRCANOO DESC, e.EJRCMESS DESC, e.EJRCCDGO DESC
 FETCH FIRST 10 ROWS ONLY;

SELECT d.EJRDCDGO                          AS DETALLE,
       d.EJRCCDGO                          AS EJECUCION,
       d.EJRDTPRP                          AS REPORTE,
       d.EJRDESTD                          AS ESTADO,
       d.EJRDCNRG                          AS REGISTROS,
       SUBSTR(d.EJRDNVDD, 1, 150)          AS NOVEDADES
  FROM RPR.EJRD d
 WHERE d.EJRDNVDD IS NOT NULL
 ORDER BY d.EJRDCDGO DESC
 FETCH FIRST 20 ROWS ONLY;
