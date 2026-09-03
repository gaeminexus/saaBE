-- =====================================================================================
-- ⛔ PRERREQUISITO DURO PARA COBRAR MORA EN LA CARGA PETRO
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA — decision del usuario, 2026-09-02:
--   «que se rediseñe la prelacion de la fase 3»  -> la carga Petro pasa a COBRAR MORA.
--
-- ⛔ POR QUE HAY QUE CORRER ESTO ANTES DE SUBIR EL WAR, y no despues:
--
--   `CobroPetroContableServiceImpl:850-861` contabiliza la mora por TIPO DE PRESTAMO, y si
--   no encuentra la linea en la plantilla NO la saltea: LANZA.
--
--       DetallePlantilla linea = detallePlantillaDaoService.selectByPlantillaYAuxiliares(
--               idPlantilla, CrdLineaAsiento.INTERES_MORA_POR_COBRAR, tipoPrestamo);
--       if (linea == null) {
--           throw new IncomeException("La plantilla alterno 21 no tiene la linea de interes
--                                      de mora para el tipo de prestamo " + tipo + ".");
--       }
--
--   IncomeException es @ApplicationException(rollback = true). O sea: si falta UNA linea para
--   UN tipo de prestamo, el asiento de aplicacion revienta y **se revierte la carga entera**
--   despues de los 20+ minutos de proceso. Hoy eso nunca paso porque la fase 3 jamas cobro
--   mora y `moraPorTipo` siempre quedaba vacio — el momento en que empiece a cobrarla es
--   exactamente el momento en que ese throw se vuelve alcanzable.
--
--   Es el mismo tipo de trampa que el `.jasper` faltante: compila, pasa revision, entra al
--   commit y revienta la primera vez que un usuario lo corre.
--
-- COMO LEER EL RESULTADO:
--   BLOQUE 1 sin filas          -> todo listo, se puede activar el cobro de mora.
--   BLOQUE 1 con filas          -> ⛔ NO SUBIR. Cada fila es un tipo de prestamo que hara
--                                  reventar el asiento. Hay que crear esas lineas primero;
--                                  con el resultado te mando el INSERT.
-- =====================================================================================

SET PAGESIZE 100
SET LINESIZE 200

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL QUE DECIDE: tipos de prestamo con mora pendiente y SIN linea de mora
--
-- Toma los tipos de prestamo que realmente aparecen en la carga 449 con mora pendiente en
-- sus cuotas, y verifica que la plantilla de APLICACION_PETRO (alterno 21) tenga la linea
-- de INTERES_MORA_POR_COBRAR (auxiliar1 = 20) para cada uno.
--
-- Si esta vacio, no hay nada que hacer: se puede activar.
-- =====================================================================================
SELECT  t.TPPRCDGO                                          AS TIPO_PRESTAMO,
        t.MORA_PENDIENTE,
        t.CUOTAS_CON_MORA,
        'FALTA LA LINEA DE MORA EN LA PLANTILLA 21'         AS PROBLEMA
FROM (
    SELECT  pd.TPPRCDGO                                     AS TPPRCDGO,
            ROUND(SUM(NVL(d.DTPRMRAA,0)), 2)                AS MORA_PENDIENTE,
            COUNT(DISTINCT d.DTPRCDGO)                      AS CUOTAS_CON_MORA
    FROM    CRD.DTPR d
    JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
    JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
    WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                            WHERE g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0)
    AND     NVL(d.DTPRMRAA,0) > 0
    GROUP   BY pd.TPPRCDGO
) t
WHERE   NOT EXISTS (
            SELECT  1
            FROM    CNT.DTPL dt
            JOIN    CNT.PLNS pl ON pl.PLNSCDGO = dt.PLNSCDGO
            WHERE   pl.PLNSCDAL = 21          -- PlantillasCredito.APLICACION_PETRO
            AND     dt.DTPLAXL1 = 20          -- CrdLineaAsiento.INTERES_MORA_POR_COBRAR
            AND     dt.DTPLAXL2 = t.TPPRCDGO
            AND     dt.DTPLESTD = 1           -- Estado.ACTIVO
        )
ORDER   BY t.MORA_PENDIENTE DESC;


-- =====================================================================================
-- BLOQUE 2 — El panorama: que lineas de mora SI existen hoy en la plantilla 21
--
-- Sirve para dos cosas: ver contra que cuenta se va a mandar la mora, y tener el modelo
-- exacto a copiar si el bloque 1 devolvio filas.
-- =====================================================================================
SELECT  pl.PLNSCDGO                                         AS ID_PLANTILLA,
        pl.PLNSNMBR                                         AS PLANTILLA,
        dt.DTPLCDGO                                         AS ID_LINEA,
        dt.DTPLAXL1                                         AS AUX1_CONCEPTO,
        CASE dt.DTPLAXL1
            WHEN 10 THEN 'Interes ordinario por cobrar'
            WHEN 20 THEN 'Interes de mora por cobrar'
            ELSE TO_CHAR(dt.DTPLAXL1)
        END                                                 AS CONCEPTO,
        dt.DTPLAXL2                                         AS AUX2_TIPO_PRESTAMO,
        dt.DTPLMVMN                                         AS MOVIMIENTO,
        dt.DTPLDSCR                                         AS DESCRIPCION,
        dt.DTPLESTD                                         AS ESTADO
FROM    CNT.DTPL dt
JOIN    CNT.PLNS pl ON pl.PLNSCDGO = dt.PLNSCDGO
WHERE   pl.PLNSCDAL = 21
AND     dt.DTPLAXL1 IN (10, 20)
ORDER   BY dt.DTPLAXL1, dt.DTPLAXL2;


-- =====================================================================================
-- BLOQUE 3 — Cuanta mora entra en juego, para dimensionar el cambio
--
-- Es lo que la carga 449 va a cobrar de mas si se activa la prelacion nueva. Sirve para
-- que contabilidad sepa que esperar antes de reprocesar.
-- =====================================================================================
SELECT  COUNT(DISTINCT d.DTPRCDGO)                          AS CUOTAS_CON_MORA,
        ROUND(SUM(NVL(d.DTPRMRAA,0)), 2)                    AS MORA_A_COBRAR,
        ROUND(SUM(NVL(d.DTPRINVN,0)), 2)                    AS INTERES_VENCIDO_A_COBRAR,
        COUNT(DISTINCT d.PRSTCDGO)                          AS PRESTAMOS_AFECTADOS
FROM    CRD.DTPR d
WHERE   d.DTPRCDGO IN (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
                        WHERE g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0)
AND     NVL(d.DTPRMRAA,0) > 0;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
