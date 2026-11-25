-- =====================================================================================
-- 393 — MEDICIÓN (SOLO LECTURA): rutas de archivo ya grabadas que quedarían FUERA de saa.upload.dir (lote 9, #10 FileRest/FileService)
-- Equipo omen-saa-1 · 2026-11-25. No modifica nada. Se puede correr en producción.
--
-- Para qué: FileRest va a rechazar con 400 toda ruta que, normalizada, quede fuera de las raíces permitidas (saa.upload.dir más
-- saa.files.raicesAdicionales, lista separada por «;»). Este SQL lista las rutas YA grabadas que hoy se sirven por /file/view, /file/download,
-- /file/info o /file/delete y que quedarían fuera. Con el resultado se llena saa.files.raicesAdicionales ANTES de activar el control.
--
-- PARAMS: RAIZ = el valor real de saa.upload.dir en producción (con «/» al final; ver standalone.xml / system-properties). Cambiarlo en el
-- CTE `params` de cada bloque. Se comparan rutas normalizadas (las «\» pasan a «/», sin distinguir mayúsculas).
-- Una ruta RELATIVA (sin «/» ni «X:» al inicio) también se lista: FileRest la resolvería contra la raíz.
-- Columnas verificadas en las entidades (model/...): CRAR.CRARRTAS, GNAP.GNAPRTAA, CBCR.CBCRRTRS, PGAP.PGAPRTRS, PGPR.PGPRRTRS, RVSG.RVSGRTRS,
-- ENTD.ENTDURFL, ADJN.ADJNURLA (firma de contrato, certificados bancarios, autorizaciones de garante), ADNV.ADNVURLA (adjuntos de novedades),
-- NVDD.NVDDARCH. Las de otros módulos (PTCH, LTPG, RDPG, SLOF) van en el bloque 3 solo como información.
-- =====================================================================================


-- BLOQUE 1 — Por tabla y carpeta: cuántas rutas quedan fuera (para llenar saa.files.raicesAdicionales).
WITH params AS (SELECT '/opt/saa-uploads/' AS raiz FROM DUAL),
rutas AS (
    SELECT 'CRD.CRAR (carga Petro)'            AS origen, CRARRTAS AS ruta FROM CRD.CRAR WHERE CRARRTAS IS NOT NULL
    UNION ALL SELECT 'CRD.GNAP (archivo generado Petro)', GNAPRTAA FROM CRD.GNAP WHERE GNAPRTAA IS NOT NULL
    UNION ALL SELECT 'CRD.CBCR (comprobante de cobro)',   CBCRRTRS FROM CRD.CBCR WHERE CBCRRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.PGAP (respaldo pago aporte)',   PGAPRTRS FROM CRD.PGAP WHERE PGAPRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.PGPR (respaldo pago préstamo)', PGPRRTRS FROM CRD.PGPR WHERE PGPRRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.RVSG (respaldo seguro)',        RVSGRTRS FROM CRD.RVSG WHERE RVSGRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.ENTD (logo)',                   ENTDURFL FROM CRD.ENTD WHERE ENTDURFL IS NOT NULL
    UNION ALL SELECT 'CRD.ADJN (firmas, certificados, garante)', ADJNURLA FROM CRD.ADJN WHERE ADJNURLA IS NOT NULL
    UNION ALL SELECT 'CNT.ADNV (adjuntos de novedades)',  ADNVURLA FROM CNT.ADNV WHERE ADNVURLA IS NOT NULL
    UNION ALL SELECT 'CNT.NVDD (archivo de novedad)',     NVDDARCH FROM CNT.NVDD WHERE NVDDARCH IS NOT NULL
),
norm AS (
    SELECT origen, ruta, LOWER(REPLACE(ruta, '\', '/')) AS rn FROM rutas
)
SELECT n.origen,
       CASE WHEN INSTR(n.rn, '/', -1) > 0 THEN SUBSTR(n.ruta, 1, INSTR(REPLACE(n.ruta, '\', '/'), '/', -1)) ELSE '(relativa, sin carpeta)' END AS carpeta,
       COUNT(*) AS rutas_fuera, MIN(n.ruta) AS ejemplo
  FROM norm n, params pa
 WHERE n.rn NOT LIKE LOWER(REPLACE(pa.raiz, '\', '/')) || '%'
 GROUP BY n.origen, CASE WHEN INSTR(n.rn, '/', -1) > 0 THEN SUBSTR(n.ruta, 1, INSTR(REPLACE(n.ruta, '\', '/'), '/', -1)) ELSE '(relativa, sin carpeta)' END
 ORDER BY n.origen, rutas_fuera DESC;


-- BLOQUE 2 — Las rutas con «..» o con separadores raros (lo que el control rechazaría siempre, esté donde esté la raíz).
WITH rutas AS (
    SELECT 'CRD.CRAR' AS origen, CRARRTAS AS ruta FROM CRD.CRAR WHERE CRARRTAS IS NOT NULL
    UNION ALL SELECT 'CRD.GNAP', GNAPRTAA FROM CRD.GNAP WHERE GNAPRTAA IS NOT NULL
    UNION ALL SELECT 'CRD.CBCR', CBCRRTRS FROM CRD.CBCR WHERE CBCRRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.PGAP', PGAPRTRS FROM CRD.PGAP WHERE PGAPRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.PGPR', PGPRRTRS FROM CRD.PGPR WHERE PGPRRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.RVSG', RVSGRTRS FROM CRD.RVSG WHERE RVSGRTRS IS NOT NULL
    UNION ALL SELECT 'CRD.ENTD', ENTDURFL FROM CRD.ENTD WHERE ENTDURFL IS NOT NULL
    UNION ALL SELECT 'CRD.ADJN', ADJNURLA FROM CRD.ADJN WHERE ADJNURLA IS NOT NULL
    UNION ALL SELECT 'CNT.ADNV', ADNVURLA FROM CNT.ADNV WHERE ADNVURLA IS NOT NULL
    UNION ALL SELECT 'CNT.NVDD', NVDDARCH FROM CNT.NVDD WHERE NVDDARCH IS NOT NULL
)
SELECT origen, ruta FROM rutas WHERE ruta LIKE '%..%' OR ruta LIKE '%//%'
 ORDER BY origen;


-- BLOQUE 3 — INFORMATIVO, otros módulos (no se tocan en este lote): las rutas de TSR, CXP y RHH que hoy también salen por /file/*.
WITH params AS (SELECT '/opt/saa-uploads/' AS raiz FROM DUAL),
rutas AS (
    SELECT 'TSR.PTCH (adjuntos caja chica)' AS origen, PTCHPATH AS ruta FROM TSR.PTCH WHERE PTCHPATH IS NOT NULL
    UNION ALL SELECT 'PGS.LTPG (lote de pago)', LTPGPATH FROM PGS.LTPG WHERE LTPGPATH IS NOT NULL
    UNION ALL SELECT 'RHH.RDPG (orden de pago nómina)', RDPGRTAR FROM RHH.RDPG WHERE RDPGRTAR IS NOT NULL
    UNION ALL SELECT 'RHH.SLOF (salida oficial)', SLOFRUTA FROM RHH.SLOF WHERE SLOFRUTA IS NOT NULL
)
SELECT r.origen, COUNT(*) AS total,
       SUM(CASE WHEN LOWER(REPLACE(r.ruta, '\', '/')) LIKE LOWER(REPLACE(pa.raiz, '\', '/')) || '%' THEN 0 ELSE 1 END) AS fuera_de_la_raiz,
       MIN(CASE WHEN LOWER(REPLACE(r.ruta, '\', '/')) LIKE LOWER(REPLACE(pa.raiz, '\', '/')) || '%' THEN NULL ELSE r.ruta END) AS ejemplo_fuera
  FROM rutas r, params pa
 GROUP BY r.origen ORDER BY r.origen;
