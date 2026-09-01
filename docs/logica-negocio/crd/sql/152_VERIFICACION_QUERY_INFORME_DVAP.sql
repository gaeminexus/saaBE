-- =====================================================================================
-- VERIFICACION DE LA QUERY DEL REPORTE RPRT_INFR_DVAP (informe de necesidad de pago)
-- FECHA: 2026-09-01   EQUIPO: CRD / EQUIPO B (ciclo del credito y seguros)
--
-- ⚠️ ESTE SCRIPT NO ESCRIBE NADA. Son SELECT y nada mas: sin INSERT, sin UPDATE,
--    sin DELETE, sin DDL, sin COMMIT. Se puede correr en produccion en cualquier
--    momento, tambien en horario laboral.
--
-- PARA QUE SIRVE:
--   El agente de backend no pudo correr la query contra una base real (no hay Oracle
--   local levantado en OMEN). Valido las columnas contra las entidades JPA, pero eso
--   no prueba que la consulta devuelva lo que tiene que devolver. Este script corre
--   exactamente las tres secciones del reporte y deja ver el resultado ANTES de
--   desplegar el WAR y abrir la pantalla.
--
--   Si algo no calza contra el esquema real, aparece aca y no delante del usuario.
--
-- COMO USARLO:
--   1. Correr el BLOQUE 0 para elegir una devolucion de prueba.
--   2. Reemplazar el 0 de la linea DEFINE de abajo por el DVAPCDGO elegido.
--   3. Correr los bloques 1 a 4 y pegar la salida completa.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220
SET SERVEROUTPUT ON

-- ⬇⬇⬇  PONER ACA EL ID DE LA DEVOLUCION A PROBAR (bloque 0 ayuda a elegirlo)  ⬇⬇⬇
DEFINE ID_DEVOLUCION = 0
-- ⬆⬆⬆ -------------------------------------------------------------------- ⬆⬆⬆


-- =====================================================================================
-- BLOQUE 0 — Elegir una devolucion de prueba
--
-- Como leerlo: sirve cualquiera con DETALLES > 0. Para que el informe se vea completo
-- conviene una con CRUCES > 0 (tiene aportes aplicados a prestamos, seccion 2) y
-- PRESTAMOS_VIVOS > 0 (tiene deuda vigente, seccion 3). Si ninguna tiene las dos, no
-- es un problema: las secciones vacias simplemente no imprimen filas.
-- =====================================================================================
SELECT  v.DVAPCDGO                                        AS ID_DEVOLUCION,
        TO_CHAR(v.DVAPFCHA, 'YYYY-MM-DD')                 AS FECHA,
        v.ENTDCDGO                                        AS ID_PARTICIPE,
        e.ENTDNMID                                        AS CEDULA,
        SUBSTR(e.ENTDRZNS, 1, 40)                         AS PARTICIPE,
        v.DVAPVLRR                                        AS VALOR_TOTAL,
        v.DVAPESTD                                        AS ESTADO,
        (SELECT COUNT(*) FROM CRD.DDVA d
          WHERE d.DVAPCDGO = v.DVAPCDGO)                  AS DETALLES,
        (SELECT COUNT(*) FROM CRD.APRT a
          WHERE a.ENTDCDGO = v.ENTDCDGO
            AND a.APRTTPMV = 4)                           AS CRUCES,
        (SELECT COUNT(*) FROM CRD.PRST p
          WHERE p.ENTDCDGO = v.ENTDCDGO
            AND (p.PRSTIDST IS NULL
                 OR p.PRSTIDST NOT IN (3,4,5)))           AS PRESTAMOS_VIVOS
FROM    CRD.DVAP v
JOIN    CRD.ENTD e ON e.ENTDCDGO = v.ENTDCDGO
ORDER   BY v.DVAPCDGO DESC
FETCH FIRST 20 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 1 — LA QUERY DEL REPORTE, TAL CUAL
--
-- Es la misma consulta del .jrxml, con &ID_DEVOLUCION en lugar de $P{P_ID_DEVOLUCION}
-- y el 1 de enero del anio de la devolucion en lugar de $P{P_FECHA_DESDE} (que es lo
-- que la pantalla precarga por defecto).
--
-- Como leerlo:
--   * SECCION 1 = lo que se devuelve, por tipo de aporte. La suma de sus VALOR tiene
--     que dar exactamente el TOTAL_DEVUELTO de la columna de la derecha. Si no cuadra,
--     hay detalle de devolucion que no se esta trayendo -> PARAR Y AVISAR.
--   * SECCION 2 = aportes que el participe ya aplico a prestamos. ⚠️ VALOR tiene que
--     salir POSITIVO. En CRD.APRT estos movimientos se graban NEGATIVOS a proposito
--     (consumirAportes hace setValor(-valor)), y el reporte los muestra con ABS().
--     Si aca salen negativos, la correccion del ABS no llego al .jasper desplegado.
--   * SECCION 3 = deuda vigente. VALOR es la suma de DTPRSLDO de las cuotas y es
--     REFERENCIAL: DTPRSLDO tiene dos semanticas segun quien escribio la cuota
--     (defecto D5, ver REVISION-MOTOR-ANTES-DE-OTORGAMIENTO.md). No se espera que
--     coincida al centavo con lo que muestra la pantalla de cobros.
-- =====================================================================================
SELECT
    1 AS SECCION,
    ROW_NUMBER() OVER (ORDER BY d.DDVACDGO) AS ORDEN,
    t.TPAPNMBR AS CONCEPTO,
    CAST(NULL AS VARCHAR2(300)) AS DETALLE,
    d.DDVAVLRR AS VALOR,
    (SELECT v.DVAPVLRR FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION) AS TOTAL_DEVUELTO
FROM CRD.DDVA d
JOIN CRD.TPAP t ON t.TPAPCDGO = d.TPAPCDGO
WHERE d.DVAPCDGO = &ID_DEVOLUCION

UNION ALL

SELECT
    2 AS SECCION,
    ROW_NUMBER() OVER (ORDER BY a.APRTFCTR) AS ORDEN,
    ta.TPAPNMBR AS CONCEPTO,
    TO_CHAR(a.APRTFCTR, 'DD/MM/YYYY') || ' - ' || a.APRTGLSA AS DETALLE,
    ABS(a.APRTVLRR) AS VALOR,
    (SELECT v.DVAPVLRR FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION) AS TOTAL_DEVUELTO
FROM CRD.APRT a
JOIN CRD.TPAP ta ON ta.TPAPCDGO = a.TPAPCDGO
WHERE a.ENTDCDGO = (SELECT v.ENTDCDGO FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION)
  AND a.APRTTPMV = 4
  AND a.APRTFCTR >= TRUNC((SELECT v.DVAPFCHA FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION), 'YYYY')
  AND a.APRTFCTR <  (SELECT v.DVAPFCHA FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION) + 1

UNION ALL

SELECT
    3 AS SECCION,
    ROW_NUMBER() OVER (ORDER BY p.PRSTCDGO) AS ORDEN,
    'Prestamo No. ' || TO_CHAR(p.PRSTCDGO) AS CONCEPTO,
    CASE p.PRSTIDST
        WHEN 1  THEN 'Generado'
        WHEN 2  THEN 'Vigente'
        WHEN 6  THEN 'Pendiente de aprobacion'
        WHEN 7  THEN 'Rechazado'
        WHEN 8  THEN 'De plazo vencido'
        WHEN 9  THEN 'Cancelado por revisar'
        WHEN 10 THEN 'Vigente por revisar'
        WHEN 11 THEN 'En mora'
        ELSE 'Estado ' || NVL(TO_CHAR(p.PRSTIDST), 'no registrado')
    END AS DETALLE,
    NVL((SELECT SUM(dt.DTPRSLDO) FROM CRD.DTPR dt WHERE dt.PRSTCDGO = p.PRSTCDGO), 0) AS VALOR,
    (SELECT v.DVAPVLRR FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION) AS TOTAL_DEVUELTO
FROM CRD.PRST p
WHERE p.ENTDCDGO = (SELECT v.ENTDCDGO FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION)
  AND (p.PRSTIDST IS NULL OR p.PRSTIDST NOT IN (3,4,5))

ORDER BY 1, 2;


-- =====================================================================================
-- BLOQUE 2 — CONTROL DE CUADRE de la seccion 1
--
-- Como leerlo: DIFERENCIA tiene que ser 0.00. Cualquier otra cosa significa que la
-- suma del detalle no da el total de la cabecera, y el informe imprimiria un total
-- que no cuadra con sus propias lineas. PARAR Y AVISAR.
-- =====================================================================================
SELECT  v.DVAPCDGO                                          AS ID_DEVOLUCION,
        v.DVAPVLRR                                          AS TOTAL_CABECERA,
        NVL((SELECT SUM(d.DDVAVLRR) FROM CRD.DDVA d
              WHERE d.DVAPCDGO = v.DVAPCDGO), 0)            AS SUMA_DETALLE,
        ROUND(v.DVAPVLRR
              - NVL((SELECT SUM(d.DDVAVLRR) FROM CRD.DDVA d
                      WHERE d.DVAPCDGO = v.DVAPCDGO), 0), 2) AS DIFERENCIA
FROM    CRD.DVAP v
WHERE   v.DVAPCDGO = &ID_DEVOLUCION;


-- =====================================================================================
-- BLOQUE 3 — CONTROL DEL SIGNO de los cruces (seccion 2)
--
-- Como leerlo: NEGATIVOS deberia ser igual a TOTAL_CRUCES y POSITIVOS 0 — asi se
-- graban en la base. Lo que importa es que el reporte los imprima en positivo por el
-- ABS(). Si aca aparecieran POSITIVOS > 0, hay movimientos de tipo 4 grabados con otro
-- criterio y el ABS() los dejaria igual: no rompe nada, pero conviene saberlo.
-- =====================================================================================
SELECT  COUNT(*)                                            AS TOTAL_CRUCES,
        SUM(CASE WHEN a.APRTVLRR < 0 THEN 1 ELSE 0 END)     AS NEGATIVOS,
        SUM(CASE WHEN a.APRTVLRR > 0 THEN 1 ELSE 0 END)     AS POSITIVOS,
        ROUND(SUM(ABS(a.APRTVLRR)), 2)                      AS TOTAL_APLICADO
FROM    CRD.APRT a
WHERE   a.ENTDCDGO = (SELECT v.ENTDCDGO FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION)
  AND   a.APRTTPMV = 4;


-- =====================================================================================
-- BLOQUE 4 — Cruces REVERSADOS del participe (limitacion conocida, no es un error)
--
-- El reporte lista los movimientos con APRTTPMV = 4 y NO puede descartar los que
-- despues se reversaron: el contra-movimiento se graba aparte, con APRTTPMV = 5, y no
-- tiene ninguna FK al aporte original (ProcesoPagoPrestamoServiceImpl:1444-1456).
-- Filtrar por tipo 5 traeria ademas reversos de devoluciones y de pensiones, que no
-- son cruces. Por eso el informe lleva una leyenda que lo dice en vez de adivinar.
--
-- Como leerlo: si REVERSOS = 0, el informe de este participe no tiene ambiguedad.
-- Si es > 0, revisar a ojo si alguno corresponde a un cruce del periodo del informe.
-- =====================================================================================
SELECT  a.APRTCDGO                                          AS ID_APORTE,
        TO_CHAR(a.APRTFCTR, 'YYYY-MM-DD HH24:MI')           AS FECHA,
        a.APRTTPMV                                          AS TIPO_MOVIMIENTO,
        a.APRTVLRR                                          AS VALOR,
        SUBSTR(a.APRTGLSA, 1, 80)                           AS GLOSA
FROM    CRD.APRT a
WHERE   a.ENTDCDGO = (SELECT v.ENTDCDGO FROM CRD.DVAP v WHERE v.DVAPCDGO = &ID_DEVOLUCION)
  AND   a.APRTTPMV = 5
ORDER   BY a.APRTFCTR DESC;


-- =====================================================================================
-- FIN. Pegar la salida de los bloques 0 a 4.
-- =====================================================================================
