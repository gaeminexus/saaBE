-- =====================================================================================
-- ¿POR QUE BARRIONUEVO NO TIENE CODIGO PETRO? — y si eso es un defecto o no
-- FECHA: 2026-09-02   EQUIPO: CRD / EQUIPO B
--
-- ⚠️ NO ESCRIBE NADA. Solo SELECT.
--
-- QUE LO ORIGINA — observacion del usuario sobre el bloque 1 del sql/180:
--   «Me llama muchisimo la atencion que para BARRIONUEVO no se muestre codigo petro. Si un
--    nombre llega sin codigo petro eso se supone que se debe solventar en la fase 1 y no
--    dejar pasar a la fase 2.»
--
-- ⛔ ANTES DE CONCLUIR NADA, UNA TRAMPA QUE PUSE YO EN EL 180 Y HAY QUE DESACTIVAR:
--
--   El bloque 1 del 180 recorre TODA LA CARTERA buscando cuotas con DTPRVLSI > 0 en tipos
--   que no son hipotecario ni prendario. NO FILTRA POR LA CARGA 449. Lo escribi asi a
--   proposito —queria saber si el dato malo existia mas alla de la carga— pero eso
--   significa que un prestamo puede aparecer ahi SIN TENER NADA QUE VER con el archivo
--   Petro que se esta procesando.
--
--   Entonces la regla de fase 1 que menciona el usuario puede no haberse violado nunca:
--   si BARRIONUEVO no vino en el archivo, la fase 1 no tenia por que verlo.
--
-- Y HAY UNA SEGUNDA RAZON LEGITIMA para no tener codigo Petro, que el propio usuario fijo
-- el 2026-09-02: «hay otras filiales, y esas se deben procesar por numero de
-- identificacion». Un participe de ARCH (filial 2) NO TIENE rol Petro, y eso es correcto,
-- no un dato faltante. `esFilialPetrocomercial` en el codigo depende de esa distincion.
--
-- ⛔ PERO SI resulta que BARRIONUEVO ES de Petrocomercial Y SI vino en la carga 449, ahi si
--    es un agujero real de la fase 1 y hay que cerrarlo. Este script lo determina.
--
-- COMO LEER EL RESULTADO:
--   BLOQUE 1  -> filial y estado de los dos. Si BARRIONUEVO es de otra filial, no hay
--                defecto: su falta de rol Petro es lo esperado.
--   BLOQUE 2  -> si cada uno vino o no en la carga 449. Si BARRIONUEVO NO vino, la fase 1
--                nunca lo evaluo y la regla no se violo.
--   BLOQUE 3  -> ⛔ el que importa de verdad: CUANTOS participes de PETROCOMERCIAL no
--                tienen rol Petro. Si son mas de cero, el agujero existe aunque este caso
--                puntual no lo pruebe.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240

DEFINE CARGA = 449


-- =====================================================================================
-- BLOQUE 1 — Los dos participes: filial, estado y sus identificadores
-- =====================================================================================
SELECT  e.ENTDCDGO                                          AS ID_ENTIDAD,
        SUBSTR(e.ENTDRZNS, 1, 40)                           AS PARTICIPE,
        e.ENTDNMID                                          AS CEDULA,
        e.ENTDRLPC                                          AS ROL_PETRO,
        e.FLLLCDGO                                          AS ID_FILIAL,
        f.FLLLNMBR                                          AS FILIAL,
        e.ENTDIDST                                          AS ESTADO
FROM    CRD.ENTD e
LEFT    JOIN CRD.FLLL f ON f.FLLLCDGO = e.FLLLCDGO
WHERE   e.ENTDCDGO IN (10108, 70607)
OR      e.ENTDNMID IN ('2100130372', '1801685437')
ORDER   BY e.ENTDCDGO;


-- =====================================================================================
-- BLOQUE 2 — ⛔ ¿VINIERON EN LA CARGA 449?
--
-- Se busca por las dos vias, porque asi las busca el proceso: por rol Petro cuando la
-- filial es Petrocomercial, por numero de identificacion en las demas.
--
-- Si un participe NO aparece aca, la fase 1 nunca lo evaluo y la regla no se violo: su
-- prestamo salio en el 180 solo porque ese script mira toda la cartera.
-- =====================================================================================
SELECT  x.PXCACDPT                                          AS CODIGO_PETRO_EN_ARCHIVO,
        SUBSTR(x.PXCANMBR, 1, 40)                           AS NOMBRE_EN_ARCHIVO,
        x.PXCACDGO                                          AS ID_FILA_ARCHIVO,
        d.CRARCDGO                                          AS CARGA
FROM    CRD.PXCA x
JOIN    CRD.DTCA d ON d.DTCACDGO = x.DTCACDGO
WHERE   d.CRARCDGO = &CARGA
AND     (x.PXCACDPT IN (SELECT e.ENTDRLPC FROM CRD.ENTD e
                         WHERE e.ENTDNMID IN ('2100130372', '1801685437')
                           AND e.ENTDRLPC IS NOT NULL)
     OR  UPPER(x.PXCANMBR) LIKE '%BARRIONUEVO%'
     OR  UPPER(x.PXCANMBR) LIKE '%ZAMBRANO LOPEZ EFRAIN%')
ORDER   BY x.PXCACDPT;


-- =====================================================================================
-- BLOQUE 3 — ⛔ EL QUE DECIDE SI HAY UN AGUJERO DE VERDAD
--
-- Participes de PETROCOMERCIAL (filial 1) SIN rol Petro. Para esa filial el rol ES el
-- identificador con el que se procesa: sin el, el participe no puede ser encontrado por el
-- archivo, y si ademas tiene cartera vigente su descuento no se aplicaria a nada.
--
-- Como leerlo:
--   0 filas  -> no hay agujero. La ausencia de rol en BARRIONUEVO se explica por su filial.
--   N filas  -> ⛔ hay N participes de Petrocomercial imposibles de identificar por el
--               archivo. Los que ademas tengan prestamos vigentes son los urgentes.
-- =====================================================================================
SELECT  e.ENTDCDGO                                          AS ID_ENTIDAD,
        SUBSTR(e.ENTDRZNS, 1, 40)                           AS PARTICIPE,
        e.ENTDNMID                                          AS CEDULA,
        e.ENTDIDST                                          AS ESTADO,
        (SELECT COUNT(*) FROM CRD.PRST p
          WHERE p.ENTDCDGO = e.ENTDCDGO
            AND NVL(p.PRSTIDST, 0) IN (2, 11))              AS PRESTAMOS_VIGENTES
FROM    CRD.ENTD e
WHERE   NVL(e.FLLLCDGO, 1) = 1
AND     e.ENTDRLPC IS NULL
ORDER   BY PRESTAMOS_VIGENTES DESC, e.ENTDCDGO
FETCH FIRST 50 ROWS ONLY;


-- =====================================================================================
-- BLOQUE 4 — El resumen del bloque 3, para dimensionar
-- =====================================================================================
SELECT  COUNT(*)                                            AS PETRO_SIN_ROL,
        SUM(CASE WHEN EXISTS (SELECT 1 FROM CRD.PRST p
                               WHERE p.ENTDCDGO = e.ENTDCDGO
                                 AND NVL(p.PRSTIDST,0) IN (2, 11))
                 THEN 1 ELSE 0 END)                         AS CON_CARTERA_VIGENTE
FROM    CRD.ENTD e
WHERE   NVL(e.FLLLCDGO, 1) = 1
AND     e.ENTDRLPC IS NULL;


-- =====================================================================================
-- FIN. Pegar la salida de los cuatro bloques.
-- =====================================================================================
