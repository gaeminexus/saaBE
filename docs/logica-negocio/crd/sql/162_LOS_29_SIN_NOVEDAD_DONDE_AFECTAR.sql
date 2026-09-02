-- =====================================================================================
-- ⛔ LOS 29 QUE BLOQUEAN SIN TENER DONDE REGISTRAR LA AFECTACION — carga 449
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- EL SINTOMA, del log de produccion al procesar:
--
--   Buscando novedades del participe: 257689
--   Novedades encontradas: 0
--   No se puede procesar el archivo: hay 29 registro(s) con valores descontados sin
--   destino definido. Registre en las novedades como aplicar cada valor...
--     - Rol 4148 OCAMPO CASTILLO AMPARITO (PQ): $288,39 sin aplicar de $288,39.
--       Novedad: CODIGO PETRO NO COINCIDE CON EL NOMBRE.
--
-- LA SOSPECHA: el bloqueo lo produce el campo PLANO CRD.PXCA.PXCANVCA = 4
-- (CODIGO_PETRO_NO_COINCIDE_CON_NOMBRE), que novedadesQueRequierenAfectacion evalua con
-- montoDiferencia = NULL — y NULL cuenta como BLOQUEANTE. Pero la pantalla de novedades
-- lista filas de CRD.NVPC, y si el participe NO TIENE fila NVPC, no hay nada en pantalla
-- contra lo que registrar la afectacion.
--
-- ⛔ Seria un callejon sin salida: la validacion exige algo que la pantalla no permite
--    hacer. El mensaje "Registre en las novedades como aplicar cada valor" pide una
--    accion imposible.
--
-- Este script lo confirma o lo desmiente. NO se toca nada hasta verlo.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 220

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — LA PREGUNTA: los que bloquean por campo plano, ¿tienen fila NVPC?
--
-- Como leerlo — la columna NVPC_TOTAL es la respuesta:
--   * 0  -> CONFIRMADO el callejon sin salida: no hay donde registrar la afectacion.
--           La unica salida es de codigo (o el tipo 4 deja de bloquear, o se generan las
--           filas NVPC). El operador NO puede resolverlo en pantalla.
--   * >0 -> SI hay novedades y el operador puede afectarlas. Entonces el problema es
--           otro y hay que mirar por que no las ve o por que no suman.
-- =====================================================================================
SELECT  p.PXCACDGO                                      AS ID_PXCA,
        p.PXCACDPT                                      AS ROL_PETRO,
        SUBSTR(p.PXCANMBR, 1, 34)                       AS PARTICIPE,
        p.PXCADSDO                                      AS DESCONTADO,
        p.PXCANVCA                                      AS NOV_CARGA,
        p.PXCANVFN                                      AS NOV_FINANC,
        (SELECT COUNT(*) FROM CRD.NVPC n
          WHERE n.PXCACDGO = p.PXCACDGO)                AS NVPC_TOTAL,
        (SELECT COUNT(*) FROM CRD.AVPC a
          JOIN CRD.NVPC n2 ON n2.NVPCCDGO = a.NVPCCDGO
         WHERE n2.PXCACDGO = p.PXCACDGO)                AS AFECTACIONES
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22)
ORDER   BY NVPC_TOTAL, p.PXCADSDO DESC;


-- =====================================================================================
-- BLOQUE 2 — El resumen de una linea
--
-- Como leerlo: si SIN_NINGUNA_NVPC es igual a PARTICIPES, ninguno de los que bloquean
-- tiene donde registrar la afectacion, y el monto de la columna MONTO es lo que queda
-- trabando la carga entera.
-- =====================================================================================
SELECT  COUNT(*)                                                       AS PARTICIPES,
        SUM(CASE WHEN (SELECT COUNT(*) FROM CRD.NVPC n
                        WHERE n.PXCACDGO = p.PXCACDGO) = 0
                 THEN 1 ELSE 0 END)                                    AS SIN_NINGUNA_NVPC,
        ROUND(SUM(p.PXCADSDO), 2)                                      AS MONTO
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA IN (1,2,3,4,7,9,10,11,12,13,18,19,20,22);


-- =====================================================================================
-- BLOQUE 3 — ¿Y estos partícipes existen bien en el sistema?
--
-- El tipo 4 dice "el codigo Petro no coincide con el nombre". La pregunta que decide si
-- el dinero tiene destino conocido: ¿se resuelve la entidad por el rol Petro?
--
-- Como leerlo:
--   * Si TODOS resuelven a una entidad (ID_ENTIDAD no nulo) y esa entidad tiene cartera,
--     el destino del dinero SI se conoce: el sistema sabe a quien aplicarselo. La novedad
--     dice que el NOMBRE del archivo no coincide con el del sistema — un dato de control,
--     no una duda sobre el destino.
--   * Si alguno NO resuelve, ese si es un caso real de "no se a quien aplicarlo".
-- =====================================================================================
SELECT  p.PXCACDPT                                      AS ROL_PETRO,
        SUBSTR(p.PXCANMBR, 1, 30)                       AS NOMBRE_EN_ARCHIVO,
        e.ENTDCDGO                                      AS ID_ENTIDAD,
        SUBSTR(e.ENTDRZNS, 1, 30)                       AS NOMBRE_EN_SISTEMA,
        e.ENTDNMID                                      AS CEDULA,
        (SELECT COUNT(*) FROM CRD.PRST pr
          WHERE pr.ENTDCDGO = e.ENTDCDGO
            AND pr.PRSTIDST IN (2, 8, 11))              AS PRESTAMOS_VIVOS
FROM    CRD.PXCA p
JOIN    CRD.DTCA d ON d.DTCACDGO = p.DTCACDGO
LEFT    JOIN CRD.ENTD e ON e.ENTDRLPC = p.PXCACDPT
WHERE   d.CRARCDGO = &CARGA
AND     NVL(p.PXCADSDO, 0) > 0.01
AND     p.PXCANVCA = 4
ORDER   BY PRESTAMOS_VIVOS, p.PXCACDPT;


-- =====================================================================================
-- FIN. Pegar la salida de los tres bloques.
-- =====================================================================================
