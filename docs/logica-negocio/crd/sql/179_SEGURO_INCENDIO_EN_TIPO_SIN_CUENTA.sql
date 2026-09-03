-- =====================================================================================
-- ⛔ QUIENES SON: los pagos con seguro de incendio en un tipo de prestamo sin cuenta
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA — la carga 449 se trabo con:
--   «El pago con seguro de incendio de la carga 449 es de tipo de prestamo 1, que no tiene
--    cuenta de seguro de incendio definida (solo hipotecario y prendario).»
--   CobroPetroContableServiceImpl:906
--
-- El mensaje no dice de que participe ni de que valor, porque para cuando lanza ya perdio
-- la identidad: seguroIncendioPorTipo es un Map<tipoPrestamo, total> acumulado, y el pago
-- individual quedo atras. Eso se corrige en el codigo (mensaje con participe, prestamo,
-- cuota y valor), pero este script te dice QUIENES SON AHORA, sin esperar un despliegue.
--
-- Tipos con cuenta de seguro definida: 2 = HIPOTECARIO, 3 = PRENDARIO. Cualquier otro
-- (el 1 del error) revienta el asiento.
--
-- ⛔ HIPOTESIS DE POR QUE APARECIO RECIEN AHORA, y hay que confirmarla con el bloque 2:
--    hasta la migracion de la fase 3, el seguro que se grababa venia del PARAMETRO del
--    archivo. Desde la migracion, el motor lo toma del SALDO REAL DE LA CUOTA (DTPRVLSI).
--    Si hay cuotas de prestamos NO hipotecarios/prendarios con DTPRVLSI > 0 — dato que no
--    deberia existir — el motor ahora las cobra, y el asiento no tiene donde ponerlas.
--    O sea: probablemente el defecto de datos siempre estuvo y la migracion lo destapo.
--    Si el bloque 2 devuelve filas, es eso.
--
-- COMO LEER EL RESULTADO:
--   BLOQUE 1 -> los pagos concretos que traban la carga. Con participe, prestamo y valor.
--   BLOQUE 2 -> si el problema esta en los DATOS de la cuota (DTPRVLSI en un tipo que no
--               deberia tenerlo) o solo en el pago.
--   BLOQUE 3 -> cuanto dinero es en total, para dimensionar la decision.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — ⛔ LOS PAGOS QUE TRABAN LA CARGA
-- =====================================================================================
SELECT  g.PGPRCDGO                                          AS PAGO,
        e.ENTDRLPC                                          AS ROL_PETRO,
        e.ENTDNMID                                          AS CEDULA,
        SUBSTR(e.ENTDRZNS, 1, 40)                           AS PARTICIPE,
        g.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS NRO_CUOTA,
        pd.TPPRCDGO                                         AS TIPO_PRESTAMO,
        pd.PRDCCDGO                                         AS ID_PRODUCTO,
        SUBSTR(pd.PRDCNMBR, 1, 30)                          AS PRODUCTO,
        NVL(g.PGPRVLSI, 0)                                  AS SEGURO_COBRADO,
        NVL(d.DTPRVLSI, 0)                                  AS SEGURO_DE_LA_CUOTA,
        NVL(g.PGPRVLRR, 0)                                  AS TOTAL_DEL_PAGO
FROM    CRD.PGPR g
JOIN    CRD.PRST p  ON p.PRSTCDGO  = g.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO  = p.ENTDCDGO
LEFT    JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
AND     NVL(g.PGPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
ORDER   BY NVL(g.PGPRVLSI,0) DESC;


-- =====================================================================================
-- BLOQUE 2 — ⛔ ¿ESTA EN LOS DATOS? Cuotas con seguro en tipos que no deberian tenerlo
--
-- Mira TODA la cartera, no solo la carga 449. Si devuelve filas, el defecto es de datos y
-- viene de antes: hay cuotas con DTPRVLSI > 0 en prestamos que no son hipotecarios ni
-- prendarios. La migracion de la fase 3 no lo creo, lo destapo.
-- =====================================================================================
SELECT  pd.TPPRCDGO                                         AS TIPO_PRESTAMO,
        pd.PRDCCDGO                                         AS ID_PRODUCTO,
        SUBSTR(pd.PRDCNMBR, 1, 30)                          AS PRODUCTO,
        COUNT(DISTINCT d.PRSTCDGO)                          AS PRESTAMOS,
        COUNT(*)                                            AS CUOTAS_CON_SEGURO,
        ROUND(SUM(NVL(d.DTPRVLSI, 0)), 2)                   AS SEGURO_TOTAL
FROM    CRD.DTPR d
JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
WHERE   NVL(d.DTPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
GROUP   BY pd.TPPRCDGO, pd.PRDCCDGO, pd.PRDCNMBR
ORDER   BY SEGURO_TOTAL DESC;


-- =====================================================================================
-- BLOQUE 3 — Cuanto dinero es, para dimensionar la decision
-- =====================================================================================
SELECT  COUNT(*)                                            AS PAGOS_AFECTADOS,
        COUNT(DISTINCT g.PRSTCDGO)                          AS PRESTAMOS,
        ROUND(SUM(NVL(g.PGPRVLSI, 0)), 2)                   AS SEGURO_SIN_CUENTA,
        ROUND((SELECT SUM(NVL(g2.PGPRVLSI,0)) FROM CRD.PGPR g2
                WHERE g2.CRARCDGO = &CARGA AND NVL(g2.PGPRANUL,0) = 0), 2) AS SEGURO_TOTAL_CARGA
FROM    CRD.PGPR g
JOIN    CRD.PRST p  ON p.PRSTCDGO  = g.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
AND     NVL(g.PGPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3);


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
