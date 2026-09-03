-- =====================================================================================
-- ⛔ PRERREQUISITO: TODA BANDA DEBE TENER CUENTA CONTABLE ASIGNADA
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA — decision del usuario, 2026-09-02: el asiento de ENTREGA del prestamo
-- pasa a usar la parametrizacion dinamica de bandas (PLAN-ENTREGA-BANDAS-DINAMICAS.md).
--
-- ⛔ POR QUE HAY QUE CORRER ESTO ANTES DE SUBIR EL WAR:
--
--   ContabilizacionIndividualCreditoServiceImpl.lineaBandaCapital:203 NO saltea una banda
--   sin cuenta: LANZA.
--
--       if (banda.getIdPlanCuenta() == null) {
--           throw new IncomeException(... "la banda " + banda.getNumero() + " del producto "
--                   + idProducto + " no tiene cuenta contable asignada en CRD.BNDP.");
--       }
--
--   IncomeException es @ApplicationException(rollback = true). En el otorgamiento eso
--   significa que EL PRESTAMO NO SE ENTREGA: el usuario aprueba, el sistema revienta, y no
--   queda nada. Hoy ese throw es inalcanzable desde la entrega porque la entrega usa una
--   escalera cableada; el momento en que se conecte la parametrizacion es exactamente el
--   momento en que se vuelve alcanzable.
--
--   Misma trampa que la linea de mora de la plantilla 21 y que el .jasper faltante:
--   compila, pasa revision, entra al commit y revienta con el usuario adelante.
--
-- COMO LEER EL RESULTADO:
--   BLOQUE 1 sin filas  -> todo listo, se puede conectar la parametrizacion.
--   BLOQUE 1 con filas  -> ⛔ NO SUBIR. Cada fila es una banda que hara fallar la entrega
--                          (y el cobro, si ese producto se cobra). Hay que asignarle cuenta
--                          primero, y eso lo decide contabilidad.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 200


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL QUE DECIDE: bandas SIN cuenta contable asignada
--
-- ⚠️ Se miran TODAS las bandas, sin filtrar por vigencia de la configuracion (CBPR tiene
--    CBPRFCIN/CBPRFCFN). Es a proposito: el clasificador resuelve la vigencia contra la
--    fecha de cada operacion, y una configuracion que hoy no esta vigente puede estarlo
--    para una entrega fechada en otro momento. Filtrar aca daria un falso "todo bien".
--    Si aparecen filas de configuraciones claramente viejas, no las ignores: contrastalas
--    con el bloque 2 y avisá antes de descartarlas.
-- =====================================================================================
SELECT  cb.CBPRCDGO                                         AS ID_CONFIGURACION,
        cb.PRDCCDGO                                         AS ID_PRODUCTO,
        p.PRDCNMBR                                          AS PRODUCTO,
        b.BNDPCDGO                                          AS ID_BANDA,
        b.BNDPNMRO                                          AS NUMERO_BANDA,
        b.BNDPCNTD                                          AS PERIODOS,
        'SIN CUENTA CONTABLE - LA ENTREGA VA A FALLAR'      AS PROBLEMA
FROM    CRD.BNDP b
JOIN    CRD.CBPR cb ON cb.CBPRCDGO = b.CBPRCDGO
LEFT    JOIN CRD.PRDC p ON p.PRDCCDGO = cb.PRDCCDGO
WHERE   b.PLNNCDGO IS NULL
ORDER   BY cb.PRDCCDGO, b.BNDPNMRO;


-- =====================================================================================
-- BLOQUE 2 — El panorama: como estan configuradas las bandas hoy, producto por producto
--
-- Sirve para dos cosas: ver contra que cuentas se va a bandear la entrega, y comprobar la
-- premisa del cambio — que la cantidad de bandas NO es necesariamente cinco. Si algun
-- producto ya tiene una cantidad distinta de 5, la escalera cableada de la entrega YA esta
-- clasificando mal hoy, y esto deja de ser preventivo.
-- =====================================================================================
SELECT  cb.PRDCCDGO                                         AS ID_PRODUCTO,
        p.PRDCNMBR                                          AS PRODUCTO,
        COUNT(b.BNDPCDGO)                                   AS CANTIDAD_BANDAS,
        SUM(CASE WHEN b.PLNNCDGO IS NULL THEN 1 ELSE 0 END) AS SIN_CUENTA
FROM    CRD.CBPR cb
LEFT    JOIN CRD.BNDP b ON b.CBPRCDGO = cb.CBPRCDGO
LEFT    JOIN CRD.PRDC p ON p.PRDCCDGO = cb.PRDCCDGO
GROUP   BY cb.PRDCCDGO, p.PRDCNMBR
ORDER   BY CANTIDAD_BANDAS DESC, cb.PRDCCDGO;


-- =====================================================================================
-- BLOQUE 3 — El detalle: cada banda con su cuenta, para contrastar contra las plantillas
--
-- Como leerlo: estas son las cuentas que va a usar la entrega despues del cambio. Deberian
-- ser las MISMAS que hoy tienen las lineas aux1 1-5 de las plantillas 9, 13 y 34. Si alguna
-- difiere, el asiento de entrega va a cambiar de cuenta -- y eso hay que avisarlo a
-- contabilidad ANTES, no despues del primer prestamo entregado.
-- =====================================================================================
SELECT  cb.PRDCCDGO                                         AS ID_PRODUCTO,
        p.PRDCNMBR                                          AS PRODUCTO,
        b.BNDPNMRO                                          AS NUMERO_BANDA,
        b.BNDPCNTD                                          AS PERIODOS,
        c.PLNNCNTA                                          AS CUENTA,
        c.PLNNNMBR                                          AS NOMBRE_CUENTA
FROM    CRD.BNDP b
JOIN    CRD.CBPR cb ON cb.CBPRCDGO = b.CBPRCDGO
LEFT    JOIN CRD.PRDC p ON p.PRDCCDGO = cb.PRDCCDGO
LEFT    JOIN CNT.PLNN c ON c.PLNNCDGO = b.PLNNCDGO
ORDER   BY cb.PRDCCDGO, b.BNDPNMRO;


-- =====================================================================================
-- BLOQUE 4 — Las cuentas que usan HOY las plantillas de entrega (9, 13 y 34)
--
-- Es el otro lado de la comparacion del bloque 3. Sus aux1 1-5 son las cinco bandas
-- cableadas que este cambio reemplaza.
-- =====================================================================================
SELECT  pl.PLNSCDAL                                         AS ALTERNO,
        pl.PLNSNMBR                                         AS PLANTILLA,
        dt.DTPLAXL1                                         AS AUX1,
        dt.DTPLMVMN                                         AS MVMN_1DEBE_2HABER,
        c.PLNNCNTA                                          AS CUENTA,
        dt.DTPLDSCR                                         AS DESCRIPCION
FROM    CNT.DTPL dt
JOIN    CNT.PLNS pl ON pl.PLNSCDGO = dt.PLNSCDGO
LEFT    JOIN CNT.PLNN c ON c.PLNNCDGO = dt.PLNNCDGO
WHERE   pl.PLNSCDAL IN (9, 13, 34)
AND     dt.DTPLAXL1 BETWEEN 1 AND 5
ORDER   BY pl.PLNSCDAL, dt.DTPLAXL1;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================
