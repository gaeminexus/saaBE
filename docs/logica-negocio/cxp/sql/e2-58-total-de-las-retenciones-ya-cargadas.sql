-- =====================================================================
-- e2-58 — Las retenciones recibidas ya cargadas se ven en 0,00 en la
--          Consulta de Documentos: ponerles su total retenido
-- Modulo: cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 2 hace UPDATE sobre PGS.DCXP.
--    Correr los BLOQUES 0 y 1, LEERLOS, y recien ahi el 2.
--
-- ⛔⛔ EL COMMIT DEL FINAL NO ES OPCIONAL ⛔⛔
--    Correr el script no guarda nada. El e2-52 se perdio exactamente asi el
--    2026-09-21: el UPDATE corrio, informo las filas, y desaparecio al
--    cerrar la sesion. Ver §46.1 del estado del equipo.
--
-- POR QUE EXISTE — reporte del usuario con captura, 2026-09-21: las filas
-- de "Comprobante de Retencion" salen con Subtotal 0,00 · IVA 0,00 ·
-- Total 0,00.
--
-- CAUSA, medida en el codigo:
--   El TXT de "recibidos" del SRI trae valorSinImpuestos, iva e importeTotal
--   en 0.00 para un comprobante de retencion, y la carga los graba asi en
--   PGS.DCXP (ProcesoCargaDocumentosServiceImpl:293-295). Esta bien que el
--   TXT los traiga en cero: una retencion no tiene subtotal ni IVA. Lo que
--   falta es el unico numero que si significa algo, el TOTAL RETENIDO, que
--   la carga YA graba en la tabla de la retencion (`rc.setTotal(...)`).
--   O sea: el dato existe, la pantalla lee otra columna.
--
--   Desde el commit `35cf5ecc` la carga tambien lo escribe en
--   DCXP.DCXPIMTT. Este script hace lo mismo con lo YA CARGADO.
--
-- LAS DOS FAMILIAS (medidas por `omen-saa-2-be`):
--   tipoTablaDestino = 'RETENCION_COMPRA_V2' -> PGS.RCV2.TOTAL  (lo actual)
--   tipoTablaDestino = 'RETENCION_COMPRA'    -> PGS.RTCM.TOTAL  (historico)
--   El vinculo es DCXP.DCXPIDBD (id del documento en su tabla destino).
--
-- POR QUE ES SEGURO — ningun lector depende de ese cero, verificado sobre
--   todo el backend: la validacion del XML y `detectarDiferencias` saltean
--   las retenciones a proposito (el TXT no reporta totales confiables para
--   ellas), el asiento y el pago usan la suma de retenidos, y
--   DetalleCargaTxt guarda su propia copia de lo que dijo el TXT en
--   columnas aparte. Una recarga del TXT no pisa este valor.
--
-- Columnas copiadas de las entidades: DocumentoCxp (PGS.DCXP: DCXPCDGO,
--   DCXPTPCM, DCXPSRCM, DCXPRSEM, DCXPFEMS, DCXPIMTT, DCXPVSIM, DCXPIVAA,
--   DCXPIDBD, DCXPTBTD), RetencionCompraV2 (PGS.RCV2: ID, TOTAL, ESTADO),
--   RetencionCompra (PGS.RTCM: ID, TOTAL, ESTADO).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Cuantas hay y cuanto suman. ANOTAR: es el respaldo del reverso
--            (el valor original es 0 en todas, pero conviene el conteo).
-- ESPERADO: TOTAL_HOY = 0 en todas, y TOTAL_NUEVO con el retenido real.
--   Si aparece alguna con TOTAL_HOY <> 0, esa ya esta bien y el BLOQUE 2
--   no la toca.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - resumen' AS bloque,
       d.DCXPTBTD AS tabla_destino,
       COUNT(*) AS documentos,
       SUM(NVL(d.DCXPIMTT,0)) AS total_hoy,
       SUM(CASE WHEN d.DCXPTBTD = 'RETENCION_COMPRA_V2'
                THEN (SELECT NVL(r.TOTAL,0) FROM PGS.RCV2 r WHERE r.ID = d.DCXPIDBD)
                ELSE (SELECT NVL(r.TOTAL,0) FROM PGS.RTCM r WHERE r.ID = d.DCXPIDBD)
           END) AS total_nuevo
  FROM PGS.DCXP d
 WHERE d.DCXPTBTD IN ('RETENCION_COMPRA', 'RETENCION_COMPRA_V2')
   AND d.DCXPIDBD IS NOT NULL
 GROUP BY d.DCXPTBTD
 ORDER BY d.DCXPTBTD;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Una por una, para contrastar contra la pantalla.
-- ESPERADO: las 14 de la captura entre ellas, con TOTAL_NUEVO > 0.
--   Si alguna da TOTAL_NUEVO = 0, esa retencion tiene el total vacio en su
--   propia tabla: es otro problema y el BLOQUE 2 no la toca. Avisar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - detalle' AS bloque,
       d.DCXPCDGO AS id_documento, d.DCXPTBTD AS tabla_destino, d.DCXPIDBD AS id_en_su_tabla,
       d.DCXPSRCM AS serie, d.DCXPRSEM AS proveedor, d.DCXPFEMS AS fecha_emision,
       NVL(d.DCXPIMTT,0) AS total_hoy,
       CASE WHEN d.DCXPTBTD = 'RETENCION_COMPRA_V2'
            THEN (SELECT NVL(r.TOTAL,0) FROM PGS.RCV2 r WHERE r.ID = d.DCXPIDBD)
            ELSE (SELECT NVL(r.TOTAL,0) FROM PGS.RTCM r WHERE r.ID = d.DCXPIDBD)
       END AS total_nuevo
  FROM PGS.DCXP d
 WHERE d.DCXPTBTD IN ('RETENCION_COMPRA', 'RETENCION_COMPRA_V2')
   AND d.DCXPIDBD IS NOT NULL
 ORDER BY d.DCXPFEMS DESC, d.DCXPCDGO;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — UPDATE. El total del documento pasa a ser el total retenido.
--            El subtotal y el IVA NO se tocan: quedan en 0, que es lo
--            correcto para una retencion.
-- ESPERADO: tantas filas como conto el BLOQUE 0, menos las que ya tenian
--           valor y las que tienen el total vacio en su propia tabla.
-- ---------------------------------------------------------------------
UPDATE PGS.DCXP d
   SET d.DCXPIMTT = CASE WHEN d.DCXPTBTD = 'RETENCION_COMPRA_V2'
                         THEN (SELECT r.TOTAL FROM PGS.RCV2 r WHERE r.ID = d.DCXPIDBD)
                         ELSE (SELECT r.TOTAL FROM PGS.RTCM r WHERE r.ID = d.DCXPIDBD)
                    END
 WHERE d.DCXPTBTD IN ('RETENCION_COMPRA', 'RETENCION_COMPRA_V2')
   AND d.DCXPIDBD IS NOT NULL
   AND NVL(d.DCXPIMTT,0) = 0
   AND CASE WHEN d.DCXPTBTD = 'RETENCION_COMPRA_V2'
            THEN (SELECT NVL(r.TOTAL,0) FROM PGS.RCV2 r WHERE r.ID = d.DCXPIDBD)
            ELSE (SELECT NVL(r.TOTAL,0) FROM PGS.RTCM r WHERE r.ID = d.DCXPIDBD)
       END > 0;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Control posterior, ANTES del COMMIT.
-- ESPERADO: cero filas. Lo que salga aca es una retencion que quedo en 0 y
--           hay que mirar a mano (su propia tabla tiene el total vacio).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - control posterior' AS bloque,
       d.DCXPCDGO AS id_documento, d.DCXPTBTD AS tabla_destino, d.DCXPSRCM AS serie,
       d.DCXPRSEM AS proveedor, NVL(d.DCXPIMTT,0) AS total_ahora
  FROM PGS.DCXP d
 WHERE d.DCXPTBTD IN ('RETENCION_COMPRA', 'RETENCION_COMPRA_V2')
   AND d.DCXPIDBD IS NOT NULL
   AND NVL(d.DCXPIMTT,0) = 0
 ORDER BY d.DCXPCDGO;


-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA DEL BLOQUE 2.
-- COMMIT;


-- =====================================================================
-- REVERSO — comentado. El valor original era 0 en todas las que toca el
-- BLOQUE 2 (esa es su condicion), asi que el reverso es exacto:
--
--   UPDATE PGS.DCXP SET DCXPIMTT = 0
--    WHERE DCXPTBTD IN ('RETENCION_COMPRA','RETENCION_COMPRA_V2');
--   COMMIT;
--
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
