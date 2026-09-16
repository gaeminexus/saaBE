-- =====================================================================
-- e2-52 — Corrige la base 0% de las compras ya cargadas (el ATS la declara
--          en la columna "BASE IVA DIFERENTE 0%")
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-16
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 3 hace UPDATE sobre PGS.FCTC.
--    Correr los bloques 0, 1 y 2, LEERLOS, y recien ahi el 3.
--
-- QUE SE MIDIO (e2-51, corrido por el usuario el 2026-09-16)
--   Las 10 facturas del proveedor 1790053881001 de agosto tienen, TODAS:
--     SUBCERO (cabecera) = 7.23  ·  base 0% del DETALLE = el subtotal entero
--     base gravada del detalle = 0  ·  IVA = 0
--   O sea: son facturas 100% al 0%, y la cabecera guarda 7.23 como base 0%.
--   El ATS escribe baseImponible = SUBCERO y baseImpGrav = SUBTOTAL - SUBCERO
--   (GeneradorAtsServiceImpl:828-829), asi que declara 7.23 en "base 0%" y el
--   resto como gravado. El XML del ATS esta bien; el dato de la carga no.
--   Es la consecuencia de la decision del §40.7 del estado: "SI se arregla el
--   reparto de bases en la carga, NO se recalculan las compras ya cargadas".
--   Este script recalcula las ya cargadas.
--
-- CRITERIO — deliberadamente conservador, sale de lo medido:
--   Solo se corrigen las facturas donde el DETALLE dice que NO hay nada
--   gravado (suma de bases con CODIGOIVASRI distinto de 0/6/7 = 0) y la
--   cabecera no tiene IVA (VIVA = 0). En esas, la base 0% es el SUBTOTAL
--   entero. Las facturas MIXTAS (parte 0%, parte gravada) NO se tocan: el
--   bloque 2 las lista para revisarlas a mano. Ahi la cabecera y el detalle
--   pueden diferir por descuentos o redondeos, y repartir a ciegas seria
--   inventar un numero.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, SUBTOTAL,
--   SUBCERO, VIVA, TOTAL, FECHA, ESTADO, TITULAR), DetalleFacturaCompra
--   (PGS.DFCC: FACTURA, BASEIMPONIBLE, VALORIVA, CODIGOIVASRI).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Inventario: cuantas facturas estan mal y de que tipo
-- (reemplaza al BLOQUE 2 del e2-51, que fallo con ORA-22818 porque Oracle
--  no admite subconsultas dentro del GROUP BY; aca van en una vista inline)
-- ESPERADO: la fila 'OK' con casi todo; 'CORREGIBLE 100% CERO' es lo que
--           arregla el bloque 3; 'MIXTA - REVISAR' es lo que queda a mano.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - inventario' AS bloque, caso, COUNT(*) AS facturas,
       SUM(subtotal) AS suma_subtotal, SUM(subcero_cabecera) AS suma_base_0_hoy,
       SUM(base_0_detalle) AS suma_base_0_detalle
  FROM (
    SELECT f.ID, NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS subcero_cabecera,
           NVL(f.VIVA,0) AS iva,
           (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle,
           (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) AS base_grav_detalle,
           CASE
             WHEN ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) < 0.005 THEN 'OK'
             WHEN (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) < 0.005
                  AND NVL(f.VIVA,0) < 0.005 THEN 'CORREGIBLE 100% CERO'
             ELSE 'MIXTA - REVISAR'
           END AS caso
      FROM PGS.FCTC f
     WHERE f.ESTADO = 1
       AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
  )
 GROUP BY caso
 ORDER BY caso;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las corregibles, una por una (las que toca el bloque 3)
-- ESPERADO: BASE_0_NUEVA = SUBTOTAL en todas.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - corregibles' AS bloque,
       f.ID, f.NUMERO, f.FECHA, t.TTLRIDNT AS ruc_proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0_hoy,
       NVL(f.SUBTOTAL,0) AS base_0_nueva, NVL(f.VIVA,0) AS iva, NVL(f.TOTAL,0) AS total
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBCERO,0) - NVL(f.SUBTOTAL,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) < 0.005
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las MIXTAS que quedan mal: NO se tocan, se revisan a mano
-- ESPERADO: idealmente cero filas. Cada fila hay que mirarla con el contador:
--           la cabecera y el detalle difieren y hay parte gravada.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - mixtas a revisar' AS bloque,
       f.ID, f.NUMERO, f.FECHA, NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0) AS base_0_hoy,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) AS base_grav_detalle,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
          WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) >= 0.005
   AND NOT ((SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) < 0.005
            AND NVL(f.VIVA,0) < 0.005)
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — ⚠️ UPDATE: la base 0% pasa a ser el subtotal entero
-- Solo en las facturas del BLOQUE 1 (sin IVA y sin nada gravado en el detalle).
-- ESPERADO: el mismo numero de filas que devolvio el BLOQUE 1. Si no coincide,
--           ROLLBACK y avisar al arbitro.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBCERO = NVL(f.SUBTOTAL, 0)
 WHERE f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBCERO,0) - NVL(f.SUBTOTAL,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) < 0.005;

-- COMMIT;   <- quitar el comentario SOLO si el numero de filas coincide con el BLOQUE 1


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Control DESPUES
-- ESPERADO: 'CORREGIBLE 100% CERO' desaparece (0 filas) y 'OK' subio en esa
--           misma cantidad.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - despues' AS bloque, caso, COUNT(*) AS facturas
  FROM (
    SELECT CASE
             WHEN ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) < 0.005 THEN 'OK'
             WHEN (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) NOT IN (0,6,7)) < 0.005
                  AND NVL(f.VIVA,0) < 0.005 THEN 'CORREGIBLE 100% CERO'
             ELSE 'MIXTA - REVISAR'
           END AS caso
      FROM PGS.FCTC f
     WHERE f.ESTADO = 1
       AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
  )
 GROUP BY caso
 ORDER BY caso;


-- ---------------------------------------------------------------------
-- REVERSO — comentado. Deja las facturas corregidas con su base 0% anterior.
-- ⚠️ No se puede derivar: el valor viejo (7.23 en las medidas) no sale de
-- ningun otro campo. Guardar la salida del BLOQUE 1 ANTES de correr el 3;
-- el reverso se escribe con esos valores, factura por factura.
-- ---------------------------------------------------------------------
-- UPDATE PGS.FCTC SET SUBCERO = <valor del BLOQUE 1> WHERE ID = <id>;
-- COMMIT;
