-- =====================================================================================
-- ⛔ LOS DOS PRESTAMOS EMERGENTES CON SEGURO DE INCENDIO QUE NO LES CORRESPONDE
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- ⛔ LO QUE EL 179 YA RESOLVIO, y no hay que volver a preguntarlo:
--
--   BLOQUE 1 VACIO — y no significa "no hay problema". Significa que la carga 449 REVIRTIO:
--   IncomeException es @ApplicationException(rollback = true), asi que cuando
--   contabilizarAplicacion lanzo, se deshizo TODO — incluidos los CRD.PGPR que el bloque 1
--   buscaba. La evidencia no podia estar ahi. Anotarlo: un bloque vacio despues de un
--   rollback no prueba nada, y casi lo leo al reves.
--
--   BLOQUE 2 CONFIRMA LA HIPOTESIS:
--       tipo 1 | producto 2 EMERGENTE | 2 prestamos | 16 cuotas | $75,53
--
--   O sea: hay DOS prestamos EMERGENTES (tipo 1, que no es hipotecario ni prendario) con
--   seguro de incendio cargado en 16 de sus cuotas. Ese dato NO deberia existir: el seguro
--   de incendio es de garantia real, y un emergente no la tiene.
--
--   ⛔ LA MIGRACION DE LA FASE 3 NO CREO ESTE DEFECTO: LO DESTAPO. Antes, el seguro que se
--   grababa venia del PARAMETRO del archivo (que para un emergente venia en 0). Desde la
--   migracion, el motor lo toma del SALDO REAL DE LA CUOTA (DTPRVLSI) — y ahi estaba el
--   dato malo desde siempre, esperando. El asiento no tiene cuenta donde ponerlo y revienta.
--
-- QUE RESUELVE ESTE SCRIPT: el pedido del usuario — «necesito saber el idasoprep de los 2
-- prestamos». PRSTIDAS es el numero de operacion del prestamo en ASOPREP (⚠️ NO confundir
-- con Aporte.idAsoprep, que es APRTIDAS y es otra cosa).
--
-- BLOQUE 3 DEL 179: dio ORA-00937 y es un error MIO — volvi a mezclar un agregado con una
-- subconsulta escalar sin GROUP BY, la misma equivocacion del 168 y del 171. Aca abajo va
-- reescrito con subconsultas en el FROM, que es la forma correcta. Tercera vez: si vuelvo a
-- escribir SUM() al lado de un (SELECT ...) en la misma lista, esta mal.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240


-- =====================================================================================
-- BLOQUE 1 — ⛔ LOS DOS PRESTAMOS, CON SU IDASOPREP Y SU PARTICIPE
-- =====================================================================================
SELECT  p.PRSTCDGO                                          AS PRESTAMO,
        p.PRSTIDAS                                          AS IDASOPREP,
        e.ENTDRLPC                                          AS ROL_PETRO,
        e.ENTDNMID                                          AS CEDULA,
        SUBSTR(e.ENTDRZNS, 1, 40)                           AS PARTICIPE,
        pd.PRDCCDGO                                         AS ID_PRODUCTO,
        SUBSTR(pd.PRDCNMBR, 1, 25)                          AS PRODUCTO,
        pd.TPPRCDGO                                         AS TIPO_PRESTAMO,
        p.PRSTIDST                                          AS ESTADO,
        COUNT(d.DTPRCDGO)                                   AS CUOTAS_CON_SEGURO,
        ROUND(SUM(NVL(d.DTPRVLSI, 0)), 2)                   AS SEGURO_INDEBIDO
FROM    CRD.DTPR d
JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
JOIN    CRD.ENTD e  ON e.ENTDCDGO  = p.ENTDCDGO
WHERE   NVL(d.DTPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
GROUP   BY p.PRSTCDGO, p.PRSTIDAS, e.ENTDRLPC, e.ENTDNMID, e.ENTDRZNS,
          pd.PRDCCDGO, pd.PRDCNMBR, pd.TPPRCDGO, p.PRSTIDST
ORDER   BY SEGURO_INDEBIDO DESC;


-- =====================================================================================
-- BLOQUE 2 — Las 16 cuotas, una por una
--
-- Sirve para decidir el arreglo: si el seguro esta en cuotas YA PAGADAS, ademas de limpiar
-- el dato hay que ver si alguna vez se le cobro de mas al participe. Si esta solo en cuotas
-- pendientes, alcanza con poner DTPRVLSI en 0 antes de reprocesar.
-- =====================================================================================
SELECT  d.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS NRO_CUOTA,
        d.DTPRESTD                                          AS ESTADO_CUOTA,
        CASE d.DTPRESTD
            WHEN 1 THEN 'Pendiente' WHEN 2 THEN 'Activa'  WHEN 3 THEN 'Emitida'
            WHEN 4 THEN 'PAGADA'    WHEN 5 THEN 'En mora' WHEN 6 THEN 'PARCIAL'
            WHEN 7 THEN 'Cancelada anticipada' WHEN 8 THEN 'Vencida' ELSE 'Otro/NULL'
        END                                                 AS NOMBRE_ESTADO,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD')                   AS VENCIMIENTO,
        NVL(d.DTPRVLSI, 0)                                  AS SEGURO_INDEBIDO,
        NVL(d.DTPRTTLL, 0)                                  AS TOTAL_CUOTA
FROM    CRD.DTPR d
JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
WHERE   NVL(d.DTPRVLSI, 0) > 0
AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
ORDER   BY d.PRSTCDGO, d.DTPRNMCT;


-- =====================================================================================
-- BLOQUE 3 — ⚠️ ¿SE LE COBRO ESE SEGURO ALGUNA VEZ AL PARTICIPE?
--
-- Reemplaza el bloque 3 del 179, que dio ORA-00937 por un error mio.
-- Mira TODOS los pagos historicos de esos dos prestamos, de cualquier carga.
--
-- Como leerlo:
--   SEGURO_COBRADO = 0  -> el dato malo nunca se cobro. Se limpia y listo.
--   SEGURO_COBRADO > 0  -> se le cobro de mas al participe en alguna carga anterior, y eso
--                          ya no es solo limpiar un dato: hay que devolverselo.
-- =====================================================================================
SELECT  t.PRESTAMO,
        t.IDASOPREP,
        t.SEGURO_EN_CUOTAS,
        NVL(g.SEGURO_COBRADO, 0)                            AS SEGURO_COBRADO,
        NVL(g.PAGOS, 0)                                     AS PAGOS_HISTORICOS
FROM (
    SELECT  p.PRSTCDGO                          AS PRESTAMO,
            p.PRSTIDAS                          AS IDASOPREP,
            ROUND(SUM(NVL(d.DTPRVLSI,0)), 2)    AS SEGURO_EN_CUOTAS
    FROM    CRD.DTPR d
    JOIN    CRD.PRST p  ON p.PRSTCDGO  = d.PRSTCDGO
    JOIN    CRD.PRDC pd ON pd.PRDCCDGO = p.PRDCCDGO
    WHERE   NVL(d.DTPRVLSI, 0) > 0
    AND     NVL(pd.TPPRCDGO, -1) NOT IN (2, 3)
    GROUP   BY p.PRSTCDGO, p.PRSTIDAS
) t
LEFT JOIN (
    SELECT  g2.PRSTCDGO                         AS PRESTAMO,
            ROUND(SUM(NVL(g2.PGPRVLSI,0)), 2)   AS SEGURO_COBRADO,
            COUNT(*)                            AS PAGOS
    FROM    CRD.PGPR g2
    WHERE   NVL(g2.PGPRANUL, 0) = 0
    GROUP   BY g2.PRSTCDGO
) g ON g.PRESTAMO = t.PRESTAMO
ORDER   BY t.SEGURO_EN_CUOTAS DESC;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
