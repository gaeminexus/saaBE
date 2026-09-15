-- =====================================================================
-- e2-47 — Completar con ceros el numero de las notas de venta manuales
--          (el SRI exige 15 digitos en el documento sustento)
-- Modulo: cxp (nota de venta manual)  ·  Equipo: omen-saa-2  ·  2026-09-15
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 2 hace UPDATE sobre PGS.FCTC.
--    Correr el BLOQUE 0 y el 1, leerlos, y recien ahi el 2.
--
-- POR QUE EXISTE — incidente en produccion, 2026-09-15 11:11
--   La retencion 278 sobre la nota de venta 002-001-0000610 volvio DEVUELTA:
--     cvc-pattern-valid: Value '0020010000610' is not facet-valid with
--     respect to pattern '[0-9]{15}' for type 'numDocSustento'
--   La nota de venta se registro a mano con secuencial de 7 digitos
--   ('0000610'); una factura electronica siempre trae 9. El registro manual
--   (FacturaCompraServiceImpl, POST /fctc/manual) guarda lo que se tipea,
--   sin completar ceros. El sistema borro la retencion 278 (nunca llego al
--   SRI), asi que no queda nada que limpiar del lado de la retencion.
--
--   '0000610' y '000000610' son el mismo numero: completar con ceros a la
--   izquierda NO cambia el documento, solo su escritura.
--   Los pagos y cruces apuntan a la nota de venta por ID, no por numero.
--
-- Columnas copiadas de FacturaCompra (PGS.FCTC: ID, TIPOCOMPROBANTE,
--   TITULAR, EMPRESA, NUMERO, NUMESTABLECIMIENTO, NUMPTOEMISION, SECUENCIAL,
--   ESTADO).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Las notas de venta y como quedarian. ANOTAR ESTA SALIDA:
--            es la unica copia del valor original (sirve para el reverso).
-- ESPERADO: la nota 002-001-0000610 con NECESITA = 'SI' y
--           NUMERO_NUEVO = '002-001-000000610'.
--   SOLO_DIGITOS = 'NO' o DEMASIADO_LARGO = 'SI' en alguna fila -> esa fila
--   NO se toca en el bloque 2; avisar al arbitro.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - notas de venta' AS bloque,
       f.ID, f.TITULAR, f.ESTADO,
       f.NUMERO, f.NUMESTABLECIMIENTO, f.NUMPTOEMISION, f.SECUENCIAL,
       CASE WHEN REGEXP_LIKE(f.NUMESTABLECIMIENTO || f.NUMPTOEMISION || f.SECUENCIAL, '^[0-9]+$')
            THEN 'SI' ELSE 'NO' END AS solo_digitos,
       CASE WHEN LENGTH(f.NUMESTABLECIMIENTO) > 3 OR LENGTH(f.NUMPTOEMISION) > 3
                 OR LENGTH(f.SECUENCIAL) > 9 THEN 'SI' ELSE 'NO' END AS demasiado_largo,
       CASE WHEN LENGTH(f.NUMESTABLECIMIENTO) < 3 OR LENGTH(f.NUMPTOEMISION) < 3
                 OR LENGTH(f.SECUENCIAL) < 9 THEN 'SI' ELSE 'NO' END AS necesita,
       LPAD(f.NUMESTABLECIMIENTO, 3, '0') || '-' || LPAD(f.NUMPTOEMISION, 3, '0')
         || '-' || LPAD(f.SECUENCIAL, 9, '0') AS numero_nuevo
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Control de choque: ¿el numero nuevo ya existe en otro
--            documento del mismo proveedor?
-- ESPERADO: CERO filas. Si hay filas, NO correr el bloque 2.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - choque con numero nuevo' AS bloque,
       f.ID AS id_nota_venta, o.ID AS id_otro_documento, o.TIPOCOMPROBANTE, o.NUMERO
  FROM PGS.FCTC f
  JOIN PGS.FCTC o
    ON o.TITULAR = f.TITULAR
   AND o.ID <> f.ID
   AND REPLACE(o.NUMERO, '-', '') =
       LPAD(f.NUMESTABLECIMIENTO, 3, '0') || LPAD(f.NUMPTOEMISION, 3, '0') || LPAD(f.SECUENCIAL, 9, '0')
 WHERE f.TIPOCOMPROBANTE = '02';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — ⚠️ UPDATE: completar con ceros
-- ESPERADO: tantas filas como NECESITA = 'SI' (y SOLO_DIGITOS = 'SI',
--           DEMASIADO_LARGO = 'NO') en el bloque 0. Si no coincide -> ROLLBACK.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.NUMESTABLECIMIENTO = LPAD(f.NUMESTABLECIMIENTO, 3, '0'),
       f.NUMPTOEMISION      = LPAD(f.NUMPTOEMISION, 3, '0'),
       f.SECUENCIAL         = LPAD(f.SECUENCIAL, 9, '0'),
       f.NUMERO             = LPAD(f.NUMESTABLECIMIENTO, 3, '0') || '-'
                              || LPAD(f.NUMPTOEMISION, 3, '0') || '-'
                              || LPAD(f.SECUENCIAL, 9, '0')
 WHERE f.TIPOCOMPROBANTE = '02'
   AND REGEXP_LIKE(f.NUMESTABLECIMIENTO || f.NUMPTOEMISION || f.SECUENCIAL, '^[0-9]+$')
   AND LENGTH(f.NUMESTABLECIMIENTO) <= 3 AND LENGTH(f.NUMPTOEMISION) <= 3
   AND LENGTH(f.SECUENCIAL) <= 9
   AND (LENGTH(f.NUMESTABLECIMIENTO) < 3 OR LENGTH(f.NUMPTOEMISION) < 3
        OR LENGTH(f.SECUENCIAL) < 9);

-- COMMIT;   <- quitar el comentario SOLO si el numero de filas coincide


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Control DESPUES
-- ESPERADO: todas con LARGO_SIN_GUIONES = 15.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - notas de venta despues' AS bloque,
       f.ID, f.NUMERO, LENGTH(REPLACE(f.NUMERO, '-', '')) AS largo_sin_guiones
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- REVERSO — comentado. No se puede derivar solo: usar los valores
-- NUMERO/NUMESTABLECIMIENTO/NUMPTOEMISION/SECUENCIAL anotados del BLOQUE 0.
-- ---------------------------------------------------------------------
-- UPDATE PGS.FCTC
--    SET NUMERO = '<NUMERO del bloque 0>',
--        NUMESTABLECIMIENTO = '<...>', NUMPTOEMISION = '<...>', SECUENCIAL = '<...>'
--  WHERE ID = <ID>;
-- COMMIT;
