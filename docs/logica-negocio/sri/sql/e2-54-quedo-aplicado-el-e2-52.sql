-- =====================================================================
-- e2-54 — El ATS de agosto SIGUE declarando la base 0% como gravada:
--          ¿el e2-52 quedo aplicado o no?
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- Los BLOQUES 0 y 1 son SOLO LECTURA. El BLOQUE 2 hace UPDATE y solo se
-- corre si el BLOQUE 0 dice que hace falta.
--
-- POR QUE EXISTE — captura del DIMM del 2026-09-21, 08/2026, proveedor
-- 1790053881001 (EMPRESA ELECTRICA QUITO), 8 compras:
--     Base IVA 0%            = 7.23 en las ocho
--     Base IVA diferente 0%  = 10.48 / 221.33 / 2.06 / 17.55 / 2.76 /
--                              17.40 / 204.61 / 451.03
--     Monto IVA              = 0.00 en las ocho
--
--   O sea: el ATS sigue declarando como GRAVADA una base que no tiene un
--   centavo de IVA. Es exactamente lo que el e2-52 tenia que corregir
--   (poner SUBCERO = SUBTOTAL en las compras 100% al 0%), y el usuario lo
--   corrio el 2026-09-21.
--
-- LAS DOS EXPLICACIONES POSIBLES, y este script las distingue:
--
--   (A) El UPDATE del e2-52 NO SE GUARDO. Su `COMMIT;` esta COMENTADO --
--       es la convencion de la casa para que un script sea seguro de correr
--       de corrido. Si el cliente SQL no hace autocommit, el UPDATE vivio
--       solo en esa sesion y se perdio al cerrarla, sin ningun error.
--       ⛔ ESTA ES LA HIPOTESIS PRINCIPAL. Y si es esta, el e2-53 (notas de
--       venta) tampoco quedo aplicado, por la misma razon.
--
--   (B) El UPDATE se guardo y el archivo del DIMM es ANTERIOR: el .xml se
--       genero antes de correr el SQL y hay que regenerarlo.
--
--   El BLOQUE 0 contesta cual de las dos, sin ambiguedad:
--     - si sigue habiendo filas 'FALTA CORREGIR'  -> es (A)
--     - si da todo en 'YA CORREGIDA'              -> es (B): regenerar el ATS
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, VIVA, ESTADO, TITULAR, TIPOCOMPROBANTE),
--   DetalleFacturaCompra (PGS.DFCC: FACTURA, BASEIMPONIBLE, CODIGOIVASRI),
--   Titular (TSR.TTLR: TTLRCDGO, TTLRIDNT).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LA PREGUNTA: ¿quedo aplicado el e2-52?
-- Mira TODAS las compras de agosto 2026 que son 100% al 0% segun su
-- detalle y sin IVA en la cabecera, o sea las que el e2-52 tenia que tocar.
-- ESPERADO SI EL e2-52 QUEDO APLICADO: 0 filas en 'FALTA CORREGIR'.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - quedo aplicado el e2-52' AS bloque, caso,
       COUNT(*) AS compras, SUM(subtotal) AS suma_subtotal,
       SUM(base_0_cabecera) AS suma_base_0_hoy,
       SUM(subtotal - base_0_cabecera) AS suma_que_el_ats_declara_gravada
  FROM (
    SELECT f.ID, NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0_cabecera,
           CASE WHEN ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) < 0.005
                THEN 'YA CORREGIDA' ELSE 'FALTA CORREGIR' END AS caso
      FROM PGS.FCTC f
     WHERE f.ESTADO = 1
       AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
       AND NVL(f.VIVA,0) < 0.005
       AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
       AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                        WHERE d.FACTURA = f.ID
                          AND d.CODIGOIVASRI IS NOT NULL
                          AND d.CODIGOIVASRI NOT IN (0,6,7))
  )
 GROUP BY caso
 ORDER BY caso;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las 8 de la captura, una por una, para contrastar contra la
--            pantalla del DIMM.
-- ESPERADO SI EL e2-52 QUEDO APLICADO: BASE_0_CABECERA = SUBTOTAL y
--           GRAVADA_QUE_DECLARA_ATS = 0 en las ocho.
--   Si BASE_0_CABECERA sigue en 7.23 -> el UPDATE no se guardo (caso A).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - proveedor de la captura' AS bloque,
       f.ID, f.NUMERO, f.FECHA,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_cabecera,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) AS gravada_que_declara_ats,
       NVL(f.VIVA,0)     AS iva_cabecera,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NULL) AS lineas_sin_codigo
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE REPLACE(t.TTLRIDNT, ' ', '') = '1790053881001'
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — REAPLICACION. Correr SOLO si el BLOQUE 0 mostro filas en
--            'FALTA CORREGIR'. Es el mismo UPDATE del e2-52.
--
-- ⛔⛔ Y ESTA VEZ, EL COMMIT. ⛔⛔
--     El UPDATE de abajo NO QUEDA GUARDADO hasta que se ejecute el COMMIT
--     del final. Esa es la hipotesis (A) de la cabecera: la primera vez se
--     corrio el UPDATE, se vio "N filas actualizadas", y se perdio al
--     cerrar la sesion porque el COMMIT estaba comentado.
--     Despues del UPDATE: correr el BLOQUE 3, y si esta bien, el COMMIT.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBCERO = NVL(f.SUBTOTAL,0)
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID
                      AND d.CODIGOIVASRI IS NOT NULL
                      AND d.CODIGOIVASRI NOT IN (0,6,7));


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Control posterior, ANTES del COMMIT.
-- ESPERADO: cero filas. Lo que salga acá es lo que el ATS seguiria
--           declarando mal; si sale algo, ROLLBACK y avisar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - control posterior' AS bloque, f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0_ahora
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID
                      AND d.CODIGOIVASRI IS NOT NULL
                      AND d.CODIGOIVASRI NOT IN (0,6,7))
 ORDER BY f.ID;


-- ⛔ ESTE COMMIT NO ES OPCIONAL. Sin el, nada de lo anterior se guarda.
-- COMMIT;


-- =====================================================================
-- DESPUES DEL COMMIT: regenerar el ATS de 08/2026. El .xml que esta en el
-- DIMM se armo con los valores viejos y no se actualiza solo.
--
-- REVERSO — comentado. El valor original de SUBCERO de estas compras es el
-- que el e2-51 dejo registrado (7.23 para las de la electrica):
--   UPDATE PGS.FCTC SET SUBCERO = <valor original> WHERE ID = <id>;
--   COMMIT;
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
