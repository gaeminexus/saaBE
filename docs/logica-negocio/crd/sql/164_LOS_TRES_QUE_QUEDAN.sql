-- =====================================================================================
-- LOS TRES QUE QUEDAN BLOQUEANDO LA CARGA 449 — que ve la pantalla vs que ve el proceso
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- LOS TRES, del mensaje de error de produccion:
--   Rol 10228 PALACIOS MARQUEZ KERVIN (PE): $46,36  — CODIGO PETRO NO COINCIDE
--   Rol 9753  ZAMBRANO CAMACHO JOSE LUIS (PE): $422,89 — MONTO INCONSISTENTE
--   Rol 4885  SOLANO MIJAS SIMON (PH): $389,22 — CODIGO PETRO NO COINCIDE
--
-- LO QUE REPORTA EL USUARIO:
--   * PALACIOS aparece en pantalla como COBRANZA (no bloqueante).
--   * ZAMBRANO no aparece en NINGUNA lista: ni bloqueante ni no bloqueante.
--
-- LO QUE YA SE SABE, y este script confirma o corrige:
--   PALACIOS: su novedad NVPC es tipo 13 con diferencia -4,50 -> COBRANZA es la
--   clasificacion CORRECTA de esa novedad. Pero el proceso lo bloquea por el campo
--   PLANO PXCA.PXCANVCA = 4, que la pantalla NO MUESTRA en ningun lado. Los dos tienen
--   razon a la vez: la pantalla clasifica bien lo que ve, y no ve todo lo que bloquea.
--
--   ZAMBRANO: sin explicar. Deberia salir como BLOQUEANTE (tipo 13 con diferencia
--   POSITIVA da BLOQUEANTE). Que no aparezca en ninguna lista es lo que hay que entender.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los tres bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — TODAS las novedades de los tres, con su familia calculada
--
-- La columna FAMILIA replica NovedadParticipeCarga.getFamilia() -> clasificar():
--   BLOQUEANTE  si tipo ∈ (1,2,3,4,7,9,10,11,12,13,18,19,20,22) Y (dif IS NULL O dif >= 0)
--   COBRANZA    si dif < -1.00
--   INFORMATIVA en cualquier otro caso
--
-- Como leerlo:
--   * VISIBLE_EN_PANTALLA dice si la pantalla la carga: hoy filtra tipoNovedad > 3.
--     Si alguna novedad de estos tres tiene tipo <= 3, ESA es la razon de que no se vea.
--   * Si ZAMBRANO tiene una fila BLOQUEANTE y VISIBLE = SI, entonces el problema no es
--     el filtro de tipo y hay que mirar la pantalla (filtro activo, paginacion).
-- =====================================================================================
SELECT  p.PXCACDPT                                      AS ROL,
        SUBSTR(p.PXCANMBR, 1, 26)                       AS PARTICIPE,
        n.NVPCCDGO                                      AS ID_NOVEDAD,
        n.NVPCTPNV                                      AS TIPO,
        n.NVPCMNES                                      AS ESPERADO,
        n.NVPCMNRC                                      AS RECIBIDO,
        n.NVPCMNDF                                      AS DIFERENCIA,
        CASE
            WHEN n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                 AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0) THEN 'BLOQUEANTE'
            WHEN n.NVPCMNDF IS NOT NULL AND n.NVPCMNDF < -1    THEN 'COBRANZA'
            ELSE 'INFORMATIVA'
        END                                             AS FAMILIA,
        CASE WHEN NVL(n.NVPCTPNV,0) > 3 THEN 'SI' ELSE 'NO — filtro tipoNovedad > 3' END
                                                        AS VISIBLE_EN_PANTALLA,
        n.NVPCESTD                                      AS ESTADO,
        n.NVPCCDCA                                      AS CARGA_DE_LA_NOVEDAD
FROM    CRD.NVPC n
JOIN    CRD.PXCA p ON p.PXCACDGO = n.PXCACDGO
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT IN (10228, 9753, 4885)
ORDER   BY p.PXCACDPT, n.NVPCCDGO;


-- =====================================================================================
-- BLOQUE 2 — Por que los bloquea el PROCESO: las tres fuentes juntas
--
-- Como leerlo: FUENTE_BLOQUEO dice de donde sale el bloqueo de cada uno.
--   'PXCANVCA' o 'PXCANVFN' -> campo PLANO, que la pantalla NO muestra en ningun lado.
--   'NVPC'                  -> una novedad, que la pantalla SI deberia mostrar.
-- =====================================================================================
SELECT  p.PXCACDPT                                      AS ROL,
        SUBSTR(p.PXCANMBR, 1, 26)                       AS PARTICIPE,
        p.PXCADSDO                                      AS DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA_PLANA,
        p.PXCANVFN                                      AS NOV_FINANC_PLANA,
        CASE
            WHEN p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVCA'
            WHEN p.PXCANVFN IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22) THEN 'PXCANVFN'
            WHEN EXISTS (SELECT 1 FROM CRD.NVPC n
                          WHERE n.PXCACDGO = p.PXCACDGO
                            AND n.NVPCTPNV IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
                            AND (n.NVPCMNDF IS NULL OR n.NVPCMNDF >= 0)) THEN 'NVPC'
            ELSE 'NINGUNA'
        END                                             AS FUENTE_BLOQUEO,
        (SELECT COUNT(*) FROM CRD.NVPC n WHERE n.PXCACDGO = p.PXCACDGO) AS NOVEDADES,
        NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
              JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
             WHERE n2.PXCACDGO = p.PXCACDGO), 0)        AS AFECTADO,
        ROUND(p.PXCADSDO
              - NVL((SELECT SUM(a.AVPCVAFA) FROM CRD.AVPC a
                      JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
                     WHERE n2.PXCACDGO = p.PXCACDGO), 0), 2) AS SIN_DESTINO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT IN (10228, 9753, 4885)
ORDER   BY p.PXCACDPT;


-- =====================================================================================
-- BLOQUE 3 — ZAMBRANO en detalle: TODOS sus registros de la carga
--
-- El rol 9753 puede tener MAS DE UN registro en la carga (un PXCA por producto). Si el
-- que bloquea es uno y la novedad que se ve en pantalla cuelga de OTRO, esa seria la
-- explicacion de que "no aparece": aparece, pero asociada al otro registro.
--
-- Como leerlo: si salen dos o mas filas de PXCA para el rol 9753, mirar cual tiene
-- NOVEDADES = 0 y descuento > 0 — ese es el que bloquea sin nada que mostrar.
-- =====================================================================================
SELECT  p.PXCACDGO                                      AS ID_PXCA,
        d.DTCACDGO                                      AS ID_DETALLE,
        d.DTCACDPP                                      AS PRODUCTO_PETRO,
        p.PXCADSDO                                      AS DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA,
        p.PXCANVFN                                      AS NOV_FINANC,
        (SELECT COUNT(*) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO)                AS NOVEDADES,
        (SELECT MIN(n.NVPCTPNV) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO)                AS TIPO_MIN,
        (SELECT MAX(n.NVPCMNDF) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO)                AS DIF_MAX
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     p.PXCACDPT = 9753
ORDER   BY p.PXCACDGO;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
