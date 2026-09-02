-- =====================================================================================
-- ⛔ COMO QUEDARON LAS CUOTAS DESPUES DE APLICAR LA CARGA 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA — reporte del usuario, 2026-09-02:
--   «cuando el proceso aplica los pagos a los prestamos no esta aplicando el pago a la
--    mora y todas las cuotas estan quedando como pendientes. El proceso jamas deberia
--    dejar en parcial una cuota y afectar a la siguiente. Para poder pasar a la siguiente
--    cuota debe terminar de pagar la cuota anterior.»
--
-- LO QUE YA SE SABE DEL CODIGO, y hay que tenerlo presente al leer los numeros:
--
--   `totalBaseCuota` (CargaArchivoPetroServiceImpl:888) resta mora e interes vencido
--   A PROPOSITO. Su javadoc lo explica: la prelacion de la fase 3 solo reparte entre
--   desgravamen, interes, capital y seguro de incendio — NO TIENE COMPONENTE DE MORA —
--   asi que si el total incluyera la mora, jamas podria agotarse y TODA cuota vencida
--   quedaria PARCIAL en vez de PAGADA. La mora, segun ese diseño, la cobra despues el
--   motor de pagos de prestamos (MotorPagoPrestamoService), que si la tiene en su
--   prelacion.
--
--   O sea: que la carga Petro no cobre mora es DELIBERADO y esta documentado. Lo que NO
--   encaja con ese diseño es que las cuotas queden pendientes o parciales — con la mora
--   fuera del total, el pago del archivo deberia agotar la cuota y dejarla PAGADA.
--
-- ESTE SCRIPT MIDE QUE PASO DE VERDAD, antes de tocar una linea. No opinar sin esto.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los cinco bloques.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — EL PANORAMA: en que estado quedaron las cuotas que la carga toco
--
-- Solo cuotas con al menos un pago vigente de ESTA carga.
--
-- Como leerlo:
--   * Si el grueso quedo en 4 PAGADA -> el proceso hizo lo suyo y el problema es puntual.
--   * Si hay muchas en 6 PARCIAL o 1 PENDIENTE -> confirma el reporte del usuario, y el
--     bloque 2 dice por cuanto faltaron.
-- =====================================================================================
SELECT  d.DTPRESTD                                          AS ESTADO_CUOTA,
        CASE d.DTPRESTD
            WHEN 1 THEN 'Pendiente'  WHEN 2 THEN 'Activa'
            WHEN 3 THEN 'Emitida'    WHEN 4 THEN 'PAGADA'
            WHEN 5 THEN 'En mora'    WHEN 6 THEN 'PARCIAL'
            WHEN 7 THEN 'Cancelada anticipada' WHEN 8 THEN 'Vencida'
            ELSE 'Otro/NULL'
        END                                                 AS NOMBRE_ESTADO,
        COUNT(DISTINCT d.DTPRCDGO)                          AS CUOTAS,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS PAGADO_EN_ESTA_CARGA
FROM    CRD.PGPR g
JOIN    CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
GROUP   BY d.DTPRESTD
ORDER   BY CUOTAS DESC;


-- =====================================================================================
-- BLOQUE 2 — Las que NO quedaron pagadas: por cuanto falto y que componente
--
-- Como leerlo — la columna FALTA_SIN_MORA es la que decide de quien es el problema:
--   * FALTA_SIN_MORA cerca de 0 y FALTA_CON_MORA > 0 -> la cuota SI se pago completa
--     segun el diseño (base sin mora) y lo unico que queda debiendo es la mora. El
--     estado deberia ser PAGADA y no lo es: eso es un defecto de marcado de estado.
--   * FALTA_SIN_MORA > 0 -> el archivo trajo MENOS que la base de la cuota: la cuota
--     quedo genuinamente incompleta y ahi aplica la regla que pide el usuario (no
--     avanzar a la siguiente).
-- =====================================================================================
SELECT  d.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS NRO_CUOTA,
        d.DTPRESTD                                          AS ESTADO,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD')                   AS VENCIMIENTO,
        d.DTPRTTLL                                          AS TOTAL_CUOTA,
        NVL(d.DTPRMRAA,0)                                   AS MORA,
        NVL(d.DTPRINVN,0)                                   AS INTERES_VENCIDO,
        ROUND(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0), 2) AS BASE_SIN_MORA,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS PAGADO,
        ROUND(NVL(d.DTPRTTLL,0) - SUM(NVL(g.PGPRVLRR,0)), 2)                AS FALTA_CON_MORA,
        ROUND(NVL(d.DTPRTTLL,0) - NVL(d.DTPRMRAA,0) - NVL(d.DTPRINVN,0)
              - SUM(NVL(g.PGPRVLRR,0)), 2)                  AS FALTA_SIN_MORA
FROM    CRD.PGPR g
JOIN    CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
GROUP   BY d.PRSTCDGO, d.DTPRNMCT, d.DTPRESTD, d.DTPRFCVN, d.DTPRTTLL,
          d.DTPRMRAA, d.DTPRINVN
ORDER   BY FALTA_SIN_MORA DESC, d.PRSTCDGO, d.DTPRNMCT
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 3 — ⛔ LA REGLA DEL USUARIO: ¿se salteo alguna cuota?
--
-- «Para poder pasar a la siguiente cuota debe terminar de pagar la cuota anterior.»
--
-- Busca prestamos donde la carga pago una cuota MAS NUEVA mientras una MAS ANTIGUA
-- quedo sin pagar. Cada fila es una violacion de esa regla.
--
-- Como leerlo: si devuelve filas, el proceso SI salteo cuotas. Si devuelve 0, la
-- prelacion se respeto y el problema es solo de estado/mora.
-- =====================================================================================
SELECT  d.PRSTCDGO                                          AS PRESTAMO,
        d.DTPRNMCT                                          AS CUOTA_PAGADA,
        TO_CHAR(d.DTPRFCVN,'YYYY-MM-DD')                    AS VENCE,
        (SELECT MIN(a.DTPRNMCT) FROM CRD.DTPR a
          WHERE a.PRSTCDGO = d.PRSTCDGO
            AND a.DTPRNMCT < d.DTPRNMCT
            AND (a.DTPRESTD IS NULL OR a.DTPRESTD NOT IN (4,7)))  AS CUOTA_ANTERIOR_SIN_PAGAR,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS PAGADO_EN_LA_NUEVA
FROM    CRD.PGPR g
JOIN    CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0
AND     EXISTS (SELECT 1 FROM CRD.DTPR a
                 WHERE a.PRSTCDGO = d.PRSTCDGO
                   AND a.DTPRNMCT < d.DTPRNMCT
                   AND (a.DTPRESTD IS NULL OR a.DTPRESTD NOT IN (4,7)))
GROUP   BY d.PRSTCDGO, d.DTPRNMCT, d.DTPRFCVN
ORDER   BY d.PRSTCDGO, d.DTPRNMCT
FETCH FIRST 40 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — La mora: ¿se cobro algo de mora en esta carga?
--
-- Como leerlo: segun el diseño actual, MORA_COBRADA deberia ser 0 — la carga Petro no
-- tiene componente de mora en su prelacion. Si da 0, confirma el diseño; el reclamo del
-- usuario no es que el codigo falle, es que ESE DISEÑO no es el que quiere.
-- =====================================================================================
SELECT  COUNT(*)                                            AS PAGOS_DE_LA_CARGA,
        ROUND(SUM(NVL(g.PGPRVLRR,0)), 2)                    AS TOTAL_PAGADO,
        ROUND(SUM(NVL(g.PGPRCPPG,0)), 2)                    AS CAPITAL,
        ROUND(SUM(NVL(g.PGPRINPG,0)), 2)                    AS INTERES,
        ROUND(SUM(NVL(g.PGPRMRPG,0)), 2)                    AS MORA_COBRADA,
        ROUND(SUM(NVL(g.PGPRDSGR,0)), 2)                    AS DESGRAVAMEN,
        ROUND(SUM(NVL(g.PGPRVLSI,0)), 2)                    AS SEGURO_INCENDIO
FROM    CRD.PGPR g
WHERE   g.CRARCDGO = &CARGA
AND     NVL(g.PGPRANUL, 0) = 0;


-- =====================================================================================
-- BLOQUE 5 — Cuanta mora quedo pendiente en las cuotas que la carga toco
--
-- Es la exposicion real de no cobrar mora en este proceso: lo que quedo debiendo y que,
-- segun el diseño actual, tendria que cobrar despues el motor de pagos.
-- =====================================================================================
SELECT  COUNT(DISTINCT d.DTPRCDGO)                          AS CUOTAS_TOCADAS,
        SUM(CASE WHEN NVL(d.DTPRMRAA,0) > 0 THEN 1 ELSE 0 END) AS CON_MORA,
        ROUND(SUM(NVL(d.DTPRMRAA,0)), 2)                    AS MORA_PENDIENTE,
        ROUND(SUM(NVL(d.DTPRINVN,0)), 2)                    AS INTERES_VENCIDO_PENDIENTE
FROM    (SELECT DISTINCT g.DTPRCDGO FROM CRD.PGPR g
          WHERE g.CRARCDGO = &CARGA AND NVL(g.PGPRANUL,0) = 0) t
JOIN    CRD.DTPR d ON d.DTPRCDGO = t.DTPRCDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los cinco bloques.
-- =====================================================================================
