-- =====================================================================================
-- ¿EXISTEN VIGENCIAS DE CONTRATO SUPERPUESTAS? — el caso que el fix de rendimiento cambia
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA: al corregir la tormenta de consultas del devengo de aportes (commit
-- 5dbd9c1), el agente marco una divergencia de comportamiento y la marco bien:
--
--   * ANTES: selectVigenteEnFecha usa getSingleResult(). Si para un mismo contrato y tipo
--     de aporte hubiera DOS vigencias ACTIVAS cubriendo la misma fecha, lanza
--     NonUniqueResultException y el proceso se detiene.
--   * AHORA: el filtro en memoria del cache toma LA PRIMERA, en silencio.
--
--   El pedido del arreglo era "resultado identico, consulta por consulta menos". Esta es
--   la unica diferencia que quedo, y hay que decidirla con datos y no con opiniones.
--
-- ⛔ POR QUE NO ALCANZA CON "no deberia poder pasar": este proyecto lleva encontradas varias
--    cosas que "no deberian pasar" y pasaban — campos muertos que filtraban pantallas, un
--    DTPRTTLL migrado que no cuadraba con sus componentes, aportes duplicados. La diferencia
--    entre "lanza" y "toma la primera en silencio" es la diferencia entre enterarse y no
--    enterarse. Si el dato existe, el silencio le cobra de mas o de menos a un participe.
--
-- COMO LEER EL RESULTADO:
--   BLOQUE 1 sin filas -> el caso es teorico. Igual conviene que falle fuerte, pero no
--                         hay nada que corregir en los datos.
--   BLOQUE 1 con filas -> ⛔ el caso ES real. Hay que corregir esos datos Y hacer que el
--                         codigo falle en vez de elegir en silencio.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220


-- =====================================================================================
-- BLOQUE 1 — ⛔ EL QUE DECIDE: vigencias ACTIVAS superpuestas, mismo contrato y mismo tipo
--
-- Dos vigencias se superponen cuando cada una empieza antes de que termine la otra.
-- VGCNFCFN nulo = vigencia abierta (sin fin), asi que se trata como "hasta el infinito".
-- =====================================================================================
SELECT  a.CNTRCDGO                                          AS CONTRATO,
        a.TPAPCDGO                                          AS TIPO_APORTE,
        a.VGCNCDGO                                          AS VIGENCIA_A,
        TO_CHAR(a.VGCNFCIN,'YYYY-MM-DD')                    AS A_DESDE,
        TO_CHAR(a.VGCNFCFN,'YYYY-MM-DD')                    AS A_HASTA,
        a.VGCNMNTO                                          AS A_MONTO,
        b.VGCNCDGO                                          AS VIGENCIA_B,
        TO_CHAR(b.VGCNFCIN,'YYYY-MM-DD')                    AS B_DESDE,
        TO_CHAR(b.VGCNFCFN,'YYYY-MM-DD')                    AS B_HASTA,
        b.VGCNMNTO                                          AS B_MONTO
FROM    CRD.VGCN a
JOIN    CRD.VGCN b
        ON  b.CNTRCDGO = a.CNTRCDGO
        AND b.TPAPCDGO = a.TPAPCDGO
        AND b.VGCNCDGO > a.VGCNCDGO
WHERE   NVL(a.VGCNIDST, 0) = 1
AND     NVL(b.VGCNIDST, 0) = 1
AND     a.VGCNFCIN <= NVL(b.VGCNFCFN, DATE '9999-12-31')
AND     b.VGCNFCIN <= NVL(a.VGCNFCFN, DATE '9999-12-31')
ORDER   BY a.CNTRCDGO, a.TPAPCDGO, a.VGCNCDGO;


-- =====================================================================================
-- BLOQUE 2 — El panorama, para dimensionar
--
-- Cuantas vigencias activas hay por contrato y tipo. Si MAXIMO_POR_COMBINACION es 1, el
-- bloque 1 tiene que haber salido vacio: son dos formas de preguntar lo mismo y sirven
-- para controlarse entre si.
-- =====================================================================================
SELECT  COUNT(*)                                            AS COMBINACIONES,
        MAX(t.CUANTAS)                                      AS MAXIMO_POR_COMBINACION,
        SUM(CASE WHEN t.CUANTAS > 1 THEN 1 ELSE 0 END)      AS CON_MAS_DE_UNA
FROM (
    SELECT  v.CNTRCDGO, v.TPAPCDGO, COUNT(*) AS CUANTAS
    FROM    CRD.VGCN v
    WHERE   NVL(v.VGCNIDST, 0) = 1
    GROUP   BY v.CNTRCDGO, v.TPAPCDGO
) t;


-- =====================================================================================
-- FIN. Pegar la salida de los dos bloques.
-- =====================================================================================
