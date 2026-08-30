-- ============================================================================
-- 64_MIGRACION_CONTRATOS_VIGENCIAS.sql
-- Fase 3 del plan de devengo de aportes (docs/logica-negocio/crd/PLAN-APORTES-DEVENGO-CONTRATOS.md)
-- Fecha: 2026-08-27
--
-- SQL PURO (sin SET/DEFINE/WHENEVER) para correr en el plugin JDBC de VS Code.
-- NO SE EJECUTA POR EL AGENTE. El usuario lo revisa y lo corre bloque por bloque.
--
-- ⚠ ORDEN DE DESPLIEGUE OBLIGATORIO — LEER ANTES DE DESPLEGAR EL WAR DE ESTA OLA:
-- Este script debe ejecutarse ANTES de desplegar el WAR. Después de esta fase, el cierre
-- de cartera lee el esperado desde CRD.VGCN; si la tabla está vacía, el esperado sale 0 y
-- el neteo reversa como no cobrado todo el mes.
-- Después de correr este script, ejecutar la consulta de
-- CierreCarteraDaoServiceImpl.selectControlEsperadoHstrVsVgcn (o su equivalente SQL en el
-- bloque 4.5 de este mismo documento) para confirmar que HSTR y VGCN cuadran antes de
-- desplegar.
--
-- ⚠ HALLAZGO QUE CAMBIA EL ALCANCE FRENTE A LO ASUMIDO EN EL PLAN:
-- CRD.CNTR NO está vacía. Tiene 7.309 filas ya existentes:
--   - 7.295 cargadas en bloque el 2025-02-05 (usuario RODRIGO, aparentemente arrastradas de
--     la migración Delta21): CNTRFCIN con fechas reales (2013-2014), CNTRPRAJ ya poblado
--     (mayoría en 5) — TODAS con CNTRESTD = 10, un código que NO es el Estado.ACTIVO(1)/
--     INACTIVO(0) que usa la aplicación.
--   - 14 de prueba (usuario asoprepDev, marzo 2025, CNTRESTD = 1) que NO se cruzan con
--     ningún partícipe del universo elegible de este script.
-- El punto 1 del plan ("si tiene contrato → activarlo, si no → crearlo") ya contempla este
-- caso — la rama "ya tiene contrato" es la que dispara para casi todo el universo, no la
-- excepción — pero el efecto práctico es que este script VA A NORMALIZAR EL ESTADO DE LOS
-- ~7.309 CONTRATOS PREEXISTENTES: ~1.647 pasan de 10 a 1 (ACTIVO), el resto (10 o 1) pasa
-- a 0 (INACTIVO). No hay ningún otro lugar del código que lea CNTRESTD=10 con un
-- significado propio (se buscó en todo el repo); se asume con seguridad que es un
-- remanente de numeración de la carga original y que la Fase 3 es exactamente el momento
-- de normalizarlo al esquema de la aplicación.
--
-- ⚠ AJUSTE EN LA DETECCIÓN DE MODO (confirmado con datos el 2026-08-27, ver reporte del
-- agente en el chat): la norma es "jubilación 5% y cesantía 2% SI TIENE AMBOS aportes;
-- si tiene SOLO UNO de los dos, ese aporte se calcula al 5% (el mismo % de jubilación),
-- sea tipo 9 o tipo 11". Verificado contra los 3 grupos de la población (HSTR estado 99):
--   - AMBOS (662 filas):        jubilación vs IGFNPARJ(5%), cesantía vs IGFNPARC(2%)
--                                -> 359 CALCULADO / 303 FIJO
--   - SOLO JUBILACION (294):    jubilación vs IGFNPARJ(5%) -> 162 CALCULADO / 132 FIJO
--   - SOLO CESANTIA (1.145):    cesantía vs IGFNPARJ(5%, NO IGFNPARC)
--                                -> 644 CALCULADO / 501 FIJO
--     (si se hubiera probado incorrectamente contra IGFNPARC/2%, solo 11 de 1.145 habrían
--     calzado — el grupo más grande del padrón habría caído casi entero a FIJO)
-- Total combinado: 1.165 CALCULADO / 936 FIJO.
--
-- Alcance (D11): entidades ACTIVO(1) o ACTIVO_EN_MORA(8) con HSTR estado 99 (el más
-- reciente por HSTRFCIN DESC, HSTRCDGO DESC). VGCNFCIN = fecha de inicio del contrato o
-- 2025-06-01, la que sea MAYOR. VGCNIDHS = HSTRCDGO de origen.
-- ============================================================================


-- ============================================================================
-- BLOQUE 1 — CONTROL ANTES (solo lectura)
-- ============================================================================

-- 1.1 Universo elegible: entidades ACTIVO/ACTIVO_EN_MORA con HSTR99 vigente
SELECT COUNT(*) AS UNIVERSO_ELEGIBLE
FROM CRD.ENTD e
WHERE e.ENTDIDST IN (1, 8)
  AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99);

-- 1.2 De esas, cuantas YA tienen CNTR (y en que CNTRESTD estan hoy)
SELECT c.CNTRESTD, COUNT(*) FILAS
FROM CRD.ENTD e
JOIN CRD.CNTR c ON c.ENTDCDGO = e.ENTDCDGO
WHERE e.ENTDIDST IN (1, 8)
  AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
GROUP BY c.CNTRESTD;

-- 1.3 Cuantas NO tienen CNTR (rama "crear nuevo" del punto 1 del plan; se espera 0 o muy pocas)
SELECT COUNT(*) AS SIN_CONTRATO
FROM CRD.ENTD e
WHERE e.ENTDIDST IN (1, 8)
  AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
  AND NOT EXISTS (SELECT 1 FROM CRD.CNTR c WHERE c.ENTDCDGO = e.ENTDCDGO);

-- 1.4 Cuantos CNTR existentes (fuera del universo elegible) se van a INACTIVAR (-> CNTRESTD=0)
SELECT COUNT(*) AS A_INACTIVAR
FROM CRD.CNTR c
WHERE NOT EXISTS (
    SELECT 1 FROM CRD.ENTD e
    WHERE e.ENTDCDGO = c.ENTDCDGO
      AND e.ENTDIDST IN (1, 8)
      AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
);

-- 1.5 Total esperado a insertar en VGCN (debe cuadrar al centavo contra el control 4.1 de despues)
SELECT
    NVL(SUM(CASE WHEN h.rn = 1 THEN h.HSTRMNAJ ELSE 0 END), 0) AS TOTAL_HSTRMNAJ,
    NVL(SUM(CASE WHEN h.rn = 1 THEN h.HSTRMNAC ELSE 0 END), 0) AS TOTAL_HSTRMNAC
FROM (
    SELECT ENTDCDGO, HSTRMNAJ, HSTRMNAC,
           ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
    FROM CRD.HSTR WHERE HSTRESTD = 99
) h
JOIN CRD.ENTD e ON e.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
WHERE e.ENTDIDST IN (1, 8);

-- 1.6 Reparto CALCULADO/FIJO proyectado, por los 3 grupos (para comparar con el 4.4 de despues)
SELECT
    CASE WHEN h.HSTRMNAJ > 0 AND h.HSTRMNAC > 0 THEN 'AMBOS'
         WHEN h.HSTRMNAJ > 0 THEN 'SOLO_JUBILACION'
         ELSE 'SOLO_CESANTIA' END AS GRUPO,
    SUM(CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND h.HSTRMNAJ > 0
              AND ROUND(h.HSTRMNAJ / rmun.remuneracion, 2) = ROUND(igfn.IGFNPARJ / 100, 2)
             THEN 1 ELSE 0 END) AS JUB_CALCULADO,
    SUM(CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND h.HSTRMNAC > 0
              AND ROUND(h.HSTRMNAC / rmun.remuneracion, 2) =
                  ROUND((CASE WHEN h.HSTRMNAJ > 0 THEN igfn.IGFNPARC ELSE igfn.IGFNPARJ END) / 100, 2)
             THEN 1 ELSE 0 END) AS CES_CALCULADO,
    COUNT(*) TOTAL_GRUPO
FROM (
    SELECT ENTDCDGO, HSTRCDGO, HSTRMNAJ, HSTRMNAC,
           ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
    FROM CRD.HSTR WHERE HSTRESTD = 99
) h
JOIN CRD.ENTD e ON e.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
LEFT JOIN CRD.PRTC p ON p.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.EXTR ex ON ex.EXTRCDLA = e.ENTDNMID
CROSS JOIN (SELECT IGFNPARJ, IGFNPARC FROM CRD.IGFN WHERE ROWNUM = 1) igfn
CROSS JOIN LATERAL (SELECT COALESCE(p.PRTCRMUN, ex.EXTRSLTT) AS remuneracion FROM DUAL) rmun
WHERE e.ENTDIDST IN (1, 8)
GROUP BY CASE WHEN h.HSTRMNAJ > 0 AND h.HSTRMNAC > 0 THEN 'AMBOS'
              WHEN h.HSTRMNAJ > 0 THEN 'SOLO_JUBILACION'
              ELSE 'SOLO_CESANTIA' END;


-- ============================================================================
-- BLOQUE 2 — RESPALDO
-- ============================================================================

CREATE TABLE CRD.BKP_CNTR_20260827 AS SELECT * FROM CRD.CNTR;
CREATE TABLE CRD.BKP_VGCN_20260827 AS SELECT * FROM CRD.VGCN;  -- vacia hoy; queda por simetria y por si se re-corre luego de un intento parcial


-- ============================================================================
-- BLOQUE 3 — MIGRACION
-- ============================================================================

-- 3.1 Crear el contrato faltante para entidades elegibles que aun no tienen ninguno
--     (rama "si no tiene -> crearlo" del punto 1; se espera que inserte 0 o muy pocas filas)
INSERT INTO CRD.CNTR (ENTDCDGO, CNTRESTD, CNTRIDST, CNTRFCIN, CNTRUSRG, CNTRFCRG)
SELECT
    e.ENTDCDGO,
    1,
    1,
    COALESCE((SELECT MIN(p.PRTCFCIN) FROM CRD.PRTC p WHERE p.ENTDCDGO = e.ENTDCDGO), DATE '2025-06-01'),
    'SAA_MIGRACION_VGCN',
    SYSTIMESTAMP
FROM CRD.ENTD e
WHERE e.ENTDIDST IN (1, 8)
  AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
  AND NOT EXISTS (SELECT 1 FROM CRD.CNTR c WHERE c.ENTDCDGO = e.ENTDCDGO);

-- 3.2 Activar los contratos existentes de entidades elegibles (incluye normalizar el
--     CNTRESTD=10 legado a 1)
UPDATE CRD.CNTR c
SET CNTRESTD = 1
WHERE c.CNTRESTD <> 1
  AND EXISTS (
      SELECT 1 FROM CRD.ENTD e
      WHERE e.ENTDCDGO = c.ENTDCDGO
        AND e.ENTDIDST IN (1, 8)
        AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
  );

-- 3.3 Inactivar los contratos de entidades que NO califican (ni ACTIVO/ACTIVO_EN_MORA, o
--     sin HSTR99) -- este es el que toca el grueso de las ~7.309 filas legadas
UPDATE CRD.CNTR c
SET CNTRESTD = 0
WHERE c.CNTRESTD <> 0
  AND NOT EXISTS (
      SELECT 1 FROM CRD.ENTD e
      WHERE e.ENTDCDGO = c.ENTDCDGO
        AND e.ENTDIDST IN (1, 8)
        AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
  );

-- 3.4 Insertar vigencias de JUBILACION (tipo 9). El % de prueba es SIEMPRE IGFNPARJ,
--     tenga o no tambien cesantia (D9 + el ajuste del 2026-08-27).
INSERT INTO CRD.VGCN (
    VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN, VGCNMNTO, VGCNPRCN, VGCNRMUN,
    VGCNMODO, VGCNIDHS, VGCNOBSR, VGCNIDST, VGCNUSRG, VGCNFCRG
)
SELECT
    CRD.SQ_VGCNCDGO.NEXTVAL,
    c.CNTRCDGO,
    9,
    GREATEST(TRUNC(c.CNTRFCIN), DATE '2025-06-01'),
    NULL,
    h.HSTRMNAJ,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAJ / rmun.remuneracion, 2) = ROUND(igfn.IGFNPARJ / 100, 2)
         THEN igfn.IGFNPARJ ELSE NULL END,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAJ / rmun.remuneracion, 2) = ROUND(igfn.IGFNPARJ / 100, 2)
         THEN rmun.remuneracion ELSE NULL END,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAJ / rmun.remuneracion, 2) = ROUND(igfn.IGFNPARJ / 100, 2)
         THEN 1 ELSE 2 END,
    h.HSTRCDGO,
    'Migrado desde HSTR ' || h.HSTRCDGO || ' (Fase 3, jubilacion)',
    1,
    'SAA_MIGRACION_VGCN',
    SYSTIMESTAMP
FROM (
    SELECT ENTDCDGO, HSTRCDGO, HSTRMNAJ,
           ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
    FROM CRD.HSTR WHERE HSTRESTD = 99
) h
JOIN CRD.ENTD e ON e.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
JOIN CRD.CNTR c ON c.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.PRTC p ON p.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.EXTR ex ON ex.EXTRCDLA = e.ENTDNMID
CROSS JOIN (SELECT IGFNPARJ, IGFNPARC FROM CRD.IGFN WHERE ROWNUM = 1) igfn
CROSS JOIN LATERAL (SELECT COALESCE(p.PRTCRMUN, ex.EXTRSLTT) AS remuneracion FROM DUAL) rmun
WHERE e.ENTDIDST IN (1, 8)
  AND h.HSTRMNAJ > 0
  AND NOT EXISTS (
      SELECT 1 FROM CRD.VGCN v2
      WHERE v2.CNTRCDGO = c.CNTRCDGO AND v2.TPAPCDGO = 9 AND v2.VGCNFCFN IS NULL
  );

-- 3.5 Insertar vigencias de CESANTIA (tipo 11). El % de prueba es IGFNPARC si TAMBIEN
--     tiene jubilacion (grupo AMBOS), o IGFNPARJ si es su UNICO aporte (grupo SOLO_CESANTIA
--     -- el ajuste confirmado el 2026-08-27).
INSERT INTO CRD.VGCN (
    VGCNCDGO, CNTRCDGO, TPAPCDGO, VGCNFCIN, VGCNFCFN, VGCNMNTO, VGCNPRCN, VGCNRMUN,
    VGCNMODO, VGCNIDHS, VGCNOBSR, VGCNIDST, VGCNUSRG, VGCNFCRG
)
SELECT
    CRD.SQ_VGCNCDGO.NEXTVAL,
    c.CNTRCDGO,
    11,
    GREATEST(TRUNC(c.CNTRFCIN), DATE '2025-06-01'),
    NULL,
    h.HSTRMNAC,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAC / rmun.remuneracion, 2) =
                  ROUND((CASE WHEN h.HSTRMNAJ > 0 THEN igfn.IGFNPARC ELSE igfn.IGFNPARJ END) / 100, 2)
         THEN (CASE WHEN h.HSTRMNAJ > 0 THEN igfn.IGFNPARC ELSE igfn.IGFNPARJ END) ELSE NULL END,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAC / rmun.remuneracion, 2) =
                  ROUND((CASE WHEN h.HSTRMNAJ > 0 THEN igfn.IGFNPARC ELSE igfn.IGFNPARJ END) / 100, 2)
         THEN rmun.remuneracion ELSE NULL END,
    CASE WHEN rmun.remuneracion IS NOT NULL AND rmun.remuneracion > 0
              AND ROUND(h.HSTRMNAC / rmun.remuneracion, 2) =
                  ROUND((CASE WHEN h.HSTRMNAJ > 0 THEN igfn.IGFNPARC ELSE igfn.IGFNPARJ END) / 100, 2)
         THEN 1 ELSE 2 END,
    h.HSTRCDGO,
    'Migrado desde HSTR ' || h.HSTRCDGO || ' (Fase 3, cesantia)',
    1,
    'SAA_MIGRACION_VGCN',
    SYSTIMESTAMP
FROM (
    SELECT ENTDCDGO, HSTRCDGO, HSTRMNAJ, HSTRMNAC,
           ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
    FROM CRD.HSTR WHERE HSTRESTD = 99
) h
JOIN CRD.ENTD e ON e.ENTDCDGO = h.ENTDCDGO AND h.rn = 1
JOIN CRD.CNTR c ON c.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.PRTC p ON p.ENTDCDGO = e.ENTDCDGO
LEFT JOIN CRD.EXTR ex ON ex.EXTRCDLA = e.ENTDNMID
CROSS JOIN (SELECT IGFNPARJ, IGFNPARC FROM CRD.IGFN WHERE ROWNUM = 1) igfn
CROSS JOIN LATERAL (SELECT COALESCE(p.PRTCRMUN, ex.EXTRSLTT) AS remuneracion FROM DUAL) rmun
WHERE e.ENTDIDST IN (1, 8)
  AND h.HSTRMNAC > 0
  AND NOT EXISTS (
      SELECT 1 FROM CRD.VGCN v2
      WHERE v2.CNTRCDGO = c.CNTRCDGO AND v2.TPAPCDGO = 11 AND v2.VGCNFCFN IS NULL
  );

-- 3.6 Actualizar el espejo en CNTR desde la vigencia abierta recien creada de cada tipo
--     (CNTRPRAI = CESANTIA, CNTRPRAJ = JUBILACION -- ver la trampa documentada en CLAUDE.md)
UPDATE CRD.CNTR c
SET (CNTRMNAJ, CNTRPRAJ) = (
    SELECT v.VGCNMNTO, v.VGCNPRCN FROM CRD.VGCN v
    WHERE v.CNTRCDGO = c.CNTRCDGO AND v.TPAPCDGO = 9 AND v.VGCNFCFN IS NULL AND v.VGCNIDST = 1
)
WHERE EXISTS (
    SELECT 1 FROM CRD.VGCN v WHERE v.CNTRCDGO = c.CNTRCDGO AND v.TPAPCDGO = 9 AND v.VGCNFCFN IS NULL AND v.VGCNIDST = 1
);

UPDATE CRD.CNTR c
SET (CNTRMNAC, CNTRPRAI) = (
    SELECT v.VGCNMNTO, v.VGCNPRCN FROM CRD.VGCN v
    WHERE v.CNTRCDGO = c.CNTRCDGO AND v.TPAPCDGO = 11 AND v.VGCNFCFN IS NULL AND v.VGCNIDST = 1
)
WHERE EXISTS (
    SELECT 1 FROM CRD.VGCN v WHERE v.CNTRCDGO = c.CNTRCDGO AND v.TPAPCDGO = 11 AND v.VGCNFCFN IS NULL AND v.VGCNIDST = 1
);

COMMIT;


-- ============================================================================
-- BLOQUE 4 — CONTROL DESPUES (solo lectura)
-- ============================================================================

-- 4.1 CUADRE AL CENTAVO: SUM(HSTRMNAJ+HSTRMNAC) del universo elegible (bloque 1.5) vs
--     SUM(VGCNMNTO) de las vigencias recien creadas -- DEBEN CUADRAR EXACTO
SELECT
    NVL(SUM(CASE WHEN TPAPCDGO = 9  THEN VGCNMNTO ELSE 0 END), 0) AS TOTAL_VGCN_JUBILACION,
    NVL(SUM(CASE WHEN TPAPCDGO = 11 THEN VGCNMNTO ELSE 0 END), 0) AS TOTAL_VGCN_CESANTIA
FROM CRD.VGCN
WHERE VGCNUSRG = 'SAA_MIGRACION_VGCN';

-- 4.2 Entidades elegibles sin contrato creado (debe ser 0)
SELECT COUNT(*) AS SIN_CONTRATO_TRAS_MIGRAR
FROM CRD.ENTD e
WHERE e.ENTDIDST IN (1, 8)
  AND EXISTS (SELECT 1 FROM CRD.HSTR h WHERE h.ENTDCDGO = e.ENTDCDGO AND h.HSTRESTD = 99)
  AND NOT EXISTS (SELECT 1 FROM CRD.CNTR c WHERE c.ENTDCDGO = e.ENTDCDGO);

-- 4.3 Entidades con contrato ACTIVO pero sin ninguna vigencia abierta (debe ser 0, salvo
--     el caso legitimo de un HSTR99 con ambos montos en 0/null)
SELECT COUNT(*) AS CONTRATO_SIN_VIGENCIA
FROM CRD.CNTR c
WHERE c.CNTRESTD = 1
  AND NOT EXISTS (SELECT 1 FROM CRD.VGCN v WHERE v.CNTRCDGO = c.CNTRCDGO AND v.VGCNFCFN IS NULL);

-- 4.4 Reparto CALCULADO/FIJO real, para comparar contra la proyeccion del bloque 1.6
SELECT TPAPCDGO, VGCNMODO, COUNT(*) FILAS
FROM CRD.VGCN
WHERE VGCNUSRG = 'SAA_MIGRACION_VGCN'
GROUP BY TPAPCDGO, VGCNMODO
ORDER BY TPAPCDGO, VGCNMODO;

-- 4.5 Consulta de control HSTR vs VGCN (misma logica que
--     CierreCarteraDaoServiceImpl.selectControlEsperadoHstrVsVgcn) -- correr esto ANTES de
--     desplegar el WAR para confirmar que ambas fuentes cuadran
SELECT
    NVL(SUM(NVL(h.HSTRMNAJ,0)),0) AS JUB_HSTR, NVL(SUM(NVL(h.HSTRMNAC,0)),0) AS CES_HSTR,
    NVL(SUM(NVL(vj.VGCNMNTO,0)),0) AS JUB_VGCN, NVL(SUM(NVL(vc.VGCNMNTO,0)),0) AS CES_VGCN
FROM CRD.ENTD e
LEFT JOIN ( SELECT ENTDCDGO, HSTRMNAJ, HSTRMNAC,
                   ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY HSTRFCIN DESC, HSTRCDGO DESC) rn
            FROM CRD.HSTR WHERE HSTRESTD = 99 ) h
       ON h.ENTDCDGO = e.ENTDCDGO AND h.rn = 1
LEFT JOIN ( SELECT CNTRCDGO, ENTDCDGO,
                   ROW_NUMBER() OVER (PARTITION BY ENTDCDGO ORDER BY CNTRCDGO DESC) rn
            FROM CRD.CNTR WHERE CNTRESTD = 1 ) ca
       ON ca.ENTDCDGO = e.ENTDCDGO AND ca.rn = 1
LEFT JOIN CRD.VGCN vj ON vj.CNTRCDGO = ca.CNTRCDGO AND vj.TPAPCDGO = 9
       AND vj.VGCNFCFN IS NULL AND vj.VGCNIDST = 1
LEFT JOIN CRD.VGCN vc ON vc.CNTRCDGO = ca.CNTRCDGO AND vc.TPAPCDGO = 11
       AND vc.VGCNFCFN IS NULL AND vc.VGCNIDST = 1
WHERE e.ENTDIDST IN (1, 8);

-- 4.6 Distribucion final de CNTRESTD (para confirmar la magnitud del cambio: cuantos
--     quedaron en 1 vs 0)
SELECT CNTRESTD, COUNT(*) FROM CRD.CNTR GROUP BY CNTRESTD ORDER BY 1;


-- ============================================================================
-- BLOQUE 5 — REVERSO
-- ============================================================================

-- ⛔⛔ TODO ESTE BLOQUE VA COMENTADO A PROPOSITO. NO LO DESCOMENTES "por si acaso".
--     Corre SOLO si hay que deshacer la migracion, y descomentando linea por linea.
--     Contiene un DELETE FROM CRD.CNTR SIN WHERE: si el script se ejecuta de corrido con
--     esto activo, BORRA LA TABLA DE CONTRATOS COMPLETA (7.309 filas) y deshace en
--     silencio todo lo que el script acaba de hacer.
--
-- 5.1 Borrar SOLO lo que inserto esta migracion (identificado por VGCNUSRG)
-- DELETE FROM CRD.VGCN WHERE VGCNUSRG = 'SAA_MIGRACION_VGCN';
--
-- 5.2 Restaurar CNTR completa desde el respaldo (cubre las filas nuevas del 3.1 y los
--     UPDATE de estado/espejo de 3.2/3.3/3.6 -- se hace despues del 5.1 para no violar la FK)
-- DELETE FROM CRD.CNTR;
-- INSERT INTO CRD.CNTR OVERRIDING SYSTEM VALUE SELECT * FROM CRD.BKP_CNTR_20260827;
-- COMMIT;

-- 5.3 Limpieza de las tablas de respaldo (correr solo cuando el usuario confirme que ya
--     no las necesita)
-- DROP TABLE CRD.BKP_CNTR_20260827 PURGE;
-- DROP TABLE CRD.BKP_VGCN_20260827 PURGE;
