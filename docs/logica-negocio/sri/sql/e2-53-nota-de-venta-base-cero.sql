-- =====================================================================
-- e2-53 — La nota de venta se declara con base GRAVADA y deberia ir en
--          base 0% (el talon del ATS la muestra en "BI tarifa diferente 0%")
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- NO ES SOLO LECTURA: los BLOQUES 2 y 3 hacen UPDATE sobre PGS.FCTC y
--    PGS.DFCC. Correr los BLOQUES 0 y 1, LEERLOS, y recien ahi el 2 y el 3.
--
-- POR QUE EXISTE — §4 de DIAGNOSTICO-ATS-BASES-Y-RETENCIONES.md, que quedo
-- sin medir el 2026-09-11. Medido el 2026-09-21 en el codigo, no supuesto:
--
--   1. La pantalla de nota de venta manual arranca con SUBCERO = 0 y con
--      codigoIVASRI = '' en cada linea
--      (nota-venta-compra-manual.component.ts:83,191,302).
--   2. El registro graba lo que llega sin recalcular
--      (FacturaCompraServiceImpl:453 setSubcero(solicitud.getSubcero());
--       :356 codigosIVA.add(codigoIVA), que queda NULL si el payload no lo
--       manda; contrato §1: "NO se recalculan los totales de cabecera").
--   3. El ATS escribe baseImponible = SUBCERO y
--      baseImpGrav = SUBTOTAL - SUBCERO (GeneradorAtsServiceImpl:320).
--
--   O sea: una nota de venta entra con SUBCERO = 0 y el ATS declara TODA su
--   base como gravada. Una nota de venta es de regimen simplificado y no
--   traslada IVA (API-NOTA-VENTA-COMPRA-MANUAL.md:93).
--
-- POR QUE EL e2-52 NO LAS AGARRO, aunque corre sobre la misma tabla:
--    su criterio de "base 0% del detalle" es NVL(CODIGOIVASRI,-1) = 0 y el
--    de "gravada" es NVL(CODIGOIVASRI,-1) NOT IN (0,6,7). Un detalle con
--    CODIGOIVASRI NULL cae en el segundo: la nota de venta se clasifico
--    como gravada o mixta y quedo sin tocar. El e2-52 esta bien para lo que
--    fue escrito (documentos que vienen del XML, que siempre traen el
--    codigo); este cubre el hueco de los que se tipean.
--
-- CRITERIO — aritmetico, no tributario: solo se corrige la nota de venta que
--   NO tiene IVA en la cabecera (VIVA = 0) y NO tiene ninguna linea con un
--   codigo de IVA gravado explicito. En esa, la base 0% es el SUBTOTAL
--   entero. Cualquier nota de venta con IVA o con una linea gravada queda
--   SIN TOCAR y se lista en el BLOQUE 1 para revisarla a mano.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID,
--   TIPOCOMPROBANTE, NUMERO, TITULAR, FECHA, SUBTOTAL, SUBCERO, VIVA, TOTAL,
--   ESTADO), DetalleFacturaCompra (PGS.DFCC: FACTURA, BASEIMPONIBLE,
--   VALORIVA, CODIGOIVASRI).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Las notas de venta, una por una, con lo que dice su detalle.
--            ANOTAR ESTA SALIDA: es la unica copia de los valores
--            originales, y es lo que hace posible el reverso del final.
-- ESPERADO: pocas filas (el e2-43 conto 2 notas de venta al 2026-09-15).
--           BASE_0_HOY = 0 y GRAVADA_HOY = el SUBTOTAL entero es el defecto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - notas de venta' AS bloque,
       f.ID, f.NUMERO, f.TITULAR, f.FECHA, f.ESTADO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_hoy,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) AS gravada_hoy,
       NVL(f.VIVA,0)     AS iva_cabecera,
       NVL(f.TOTAL,0)    AS total,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS lineas,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NULL) AS lineas_sin_codigo,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NOT NULL AND d.CODIGOIVASRI NOT IN (0,6,7)) AS base_gravada_detalle,
       (SELECT NVL(SUM(d.VALORIVA),0) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS iva_detalle
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Clasificacion: cuales toca el BLOQUE 2 y cuales quedan a mano.
-- ESPERADO: 'CORREGIBLE' las que el ATS declara mal; 'YA ESTA BIEN' las que
--           ya tienen SUBCERO = SUBTOTAL; 'REVISAR A MANO' cualquiera con
--           IVA o con una linea gravada — esas NO se tocan.
-- Si aparece una fila 'REVISAR A MANO', avisar al arbitro ANTES de seguir.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - clasificacion' AS bloque, caso, COUNT(*) AS notas_venta,
       SUM(subtotal) AS suma_subtotal, SUM(base_0_hoy) AS suma_base_0_hoy
  FROM (
    SELECT f.ID, NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0_hoy,
           CASE
             WHEN NVL(f.VIVA,0) >= 0.005
               OR (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
                    AND d.CODIGOIVASRI IS NOT NULL AND d.CODIGOIVASRI NOT IN (0,6,7)) > 0
               THEN 'REVISAR A MANO'
             WHEN ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) < 0.005 THEN 'YA ESTA BIEN'
             ELSE 'CORREGIBLE'
           END AS caso
      FROM PGS.FCTC f
     WHERE f.TIPOCOMPROBANTE = '02'
       AND f.ESTADO = 1
  )
 GROUP BY caso
 ORDER BY caso;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — UPDATE. La base 0% de la nota de venta es su SUBTOTAL.
--            El COMMIT esta comentado a proposito: mirar el BLOQUE 4 antes.
-- ESPERADO: tantas filas como dijo 'CORREGIBLE' en el BLOQUE 1.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBCERO = NVL(f.SUBTOTAL,0)
 WHERE f.TIPOCOMPROBANTE = '02'
   AND f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID
                      AND d.CODIGOIVASRI IS NOT NULL
                      AND d.CODIGOIVASRI NOT IN (0,6,7));


-- ---------------------------------------------------------------------
-- BLOQUE 3 — UPDATE. El detalle sin codigo de IVA pasa a 0 (0%), que es
--            lo que la cabecera acaba de declarar. Sin esto la nota de venta
--            vuelve a clasificarse como "gravada" la proxima vez que alguien
--            mida por el detalle — es el hueco por el que el e2-52 la dejo
--            pasar.
-- ESPERADO: las lineas que el BLOQUE 0 conto en LINEAS_SIN_CODIGO, de las
--           notas de venta que corrigio el BLOQUE 2.
-- ---------------------------------------------------------------------
UPDATE PGS.DFCC d
   SET d.CODIGOIVASRI = 0
 WHERE d.CODIGOIVASRI IS NULL
   AND EXISTS (SELECT 1 FROM PGS.FCTC f
                WHERE f.ID = d.FACTURA
                  AND f.TIPOCOMPROBANTE = '02'
                  AND f.ESTADO = 1
                  AND NVL(f.VIVA,0) < 0.005
                  AND NOT EXISTS (SELECT 1 FROM PGS.DFCC g
                                   WHERE g.FACTURA = f.ID
                                     AND g.CODIGOIVASRI IS NOT NULL
                                     AND g.CODIGOIVASRI NOT IN (0,6,7)));


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Control posterior. Correr ANTES del COMMIT.
-- ESPERADO: GRAVADA_AHORA = 0 en todas las que el BLOQUE 1 dio 'CORREGIBLE',
--           y LINEAS_SIN_CODIGO = 0 en esas mismas.
--           Si alguna quedo con GRAVADA_AHORA <> 0, hacer ROLLBACK y avisar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - control posterior' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_ahora,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) AS gravada_ahora,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NULL) AS lineas_sin_codigo
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
 ORDER BY f.ID;


-- COMMIT;


-- =====================================================================
-- REVERSO — comentado. Los valores originales son los del BLOQUE 0: no hay
-- forma de reconstruirlos desde la base despues del COMMIT, por eso el
-- BLOQUE 0 pide anotarlos.
--
--   UPDATE PGS.FCTC SET SUBCERO = <base_0_hoy del BLOQUE 0> WHERE ID = <id>;
--   UPDATE PGS.DFCC SET CODIGOIVASRI = NULL WHERE FACTURA = <id>;
--   COMMIT;
--
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
