-- =====================================================================
-- e2-60 — Las facturas MIXTAS: el ATS declara como gravada la parte al 0%
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ⚠️ NO ES SOLO LECTURA: el BLOQUE 3 hace UPDATE sobre PGS.FCTC.
--    Correr los BLOQUES 0, 1 y 2, LEERLOS, y recien ahi el 3.
--
-- ⛔⛔ EL COMMIT DEL FINAL NO ES OPCIONAL ⛔⛔
--    Correr el script no guarda nada. El e2-52 se perdio exactamente asi.
--
-- POR QUE EXISTE — reporte del usuario, 2026-09-21: "tengo un caso de
-- facturas que tienen base 0 y base con IVA pero esta enviando la
-- totalidad base con IVA", y la salida del e2-57 que lo confirma con
-- numeros.
--
-- QUE MOSTRO EL e2-57 (BLOQUE 7, agosto 2026) — ocho facturas donde la
-- cabecera no coincide con el detalle, y NO son todas el mismo caso:
--
--   TRES SON EL DEFECTO REAL (cabecera con base 0% en CERO):
--     343  R&M WORLDTRAVEL  subtotal 1521.17  det: 1441.17 al 0% + 80.00 gravada + 12.00 IVA
--     493  CORP. FAVORITA   subtotal   20.15  det:    7.20 al 0% + 12.95 gravada +  1.94 IVA
--     494  CORP. FAVORITA   subtotal   21.52  det:    6.10 al 0% + 15.42 gravada +  2.31 IVA
--   En las tres el detalle CIERRA EXACTO contra el subtotal (0% + gravada =
--   subtotal) y el IVA es el 15% de la parte gravada. O sea: el detalle
--   esta bien y la cabecera esta mal. Son 1454.47 declarados como gravados
--   que no lo son.
--
--   CUATRO SON REDONDEO (diferencias de 0.02 a 0.18):
--     495 CNEL EP (0.02) · 427, 425, 496 Empresa Electrica (0.16/0.18/0.03)
--   La cabecera ya quedo bien con el e2-52/e2-54; la suma del detalle
--   difiere por centavos. NO SE TOCAN: mover la cabecera por 0.18 seria
--   romper un dato correcto para perseguir un decimal.
--
--   UNA NO SE PUEDE DECIDIR CON EL DETALLE:
--     423  RAMIREZ MOLINA  subtotal 400.00  cabecera 400.00 al 0%
--          detalle: 0.00 al 0% y 0.00 gravada -> SUS LINEAS NO TIENEN CODIGO
--   Aca la cabecera dice 400 al 0% y el detalle no dice nada (CODIGOIVASRI
--   nulo). ⛔ RECALCULAR DESDE EL DETALLE LA ROMPERIA: dejaria SUBCERO en 0
--   y el ATS declararia los 400 como GRAVADOS, que es peor que hoy.
--   Un codigo nulo significa "no se sabe", no "gravado". Se lista y no se
--   toca.
--
-- EL CRITERIO, entonces — y es el que hace seguro este script:
--   solo se corrige la factura cuyo DETALLE CIERRA CONTRA EL SUBTOTAL:
--     | base_0_detalle + base_gravada_detalle - SUBTOTAL | < 0.01
--   Si el detalle no cierra, no se sabe lo suficiente y no se toca.
--   Ese filtro deja afuera solo las de redondeo y la 423, que es
--   exactamente lo que se quiere.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, VIVA, ESTADO, TITULAR), DetalleFacturaCompra
--   (PGS.DFCC: FACTURA, BASEIMPONIBLE, CODIGOIVASRI), Titular (TSR.TTLR).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — La compra que el e2-57 conto como "NO APLICADO" del e2-52.
-- ESPERADO: una sola fila, con 420 de base gravada de mas. Mirarla antes
--           de nada: si su detalle no tiene codigos, es el caso de la 423
--           y NO hay que corregirla por esta via.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - la que quedo del e2-52' AS bloque,
       f.ID, f.NUMERO, f.FECHA, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_cabecera,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) AS gravada_que_declara_ats,
       NVL(f.VIVA,0) AS iva,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID) AS lineas,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NULL) AS lineas_sin_codigo
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID
                      AND d.CODIGOIVASRI IS NOT NULL
                      AND d.CODIGOIVASRI NOT IN (0,6,7))
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las MIXTAS CORREGIBLES de agosto: detalle que cierra contra
--            el subtotal. Son las que toca el BLOQUE 3.
-- ESPERADO: las tres del e2-57 (343, 493, 494), con
--           BASE_0_NUEVA = base_0_detalle y CIERRA = 'SI'.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - mixtas corregibles de agosto' AS bloque,
       f.ID, f.NUMERO, f.FECHA, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_hoy,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_nueva,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
           AND d.CODIGOIVASRI NOT IN (0,6,7)) AS base_gravada_detalle,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) >= 0.005
   AND ABS( (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)
          + (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
                AND d.CODIGOIVASRI NOT IN (0,6,7))
          - NVL(f.SUBTOTAL,0) ) < 0.01
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las que el BLOQUE 3 NO va a tocar, y por que. Es el control
--            que evita "corregir" una factura cuyo detalle no alcanza.
-- ESPERADO: las de redondeo (495, 427, 425, 496) y la 423 sin codigos.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - mixtas que NO se tocan' AS bloque,
       f.ID, f.NUMERO, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_hoy,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle,
       (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
         AND d.CODIGOIVASRI IS NULL) AS lineas_sin_codigo,
       CASE WHEN (SELECT COUNT(*) FROM PGS.DFCC d WHERE d.FACTURA = f.ID
                   AND d.CODIGOIVASRI IS NULL) > 0
            THEN 'DETALLE SIN CODIGOS - no se puede decidir'
            ELSE 'DIFERENCIA DE REDONDEO - la cabecera ya esta bien' END AS motivo
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) >= 0.005
   AND ABS( (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)
          + (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
                AND d.CODIGOIVASRI NOT IN (0,6,7))
          - NVL(f.SUBTOTAL,0) ) >= 0.01
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — UPDATE. La base 0% de la cabecera pasa a ser la del detalle,
--            solo en las facturas cuyo detalle CIERRA contra el subtotal.
-- ESPERADO: 3 filas (343, 493, 494).
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBCERO = (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                     WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) >= 0.005
   AND ABS( (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)
          + (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
              WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
                AND d.CODIGOIVASRI NOT IN (0,6,7))
          - NVL(f.SUBTOTAL,0) ) < 0.01;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Control posterior, ANTES del COMMIT.
-- ESPERADO: las tres con BASE_0_AHORA igual a la del detalle y
--           GRAVADA_AHORA igual a la gravada del detalle.
--           343 -> 1441.17 y 80.00 · 493 -> 7.20 y 12.95 · 494 -> 6.10 y 15.42
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - control posterior' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_ahora,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) AS gravada_ahora,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
 WHERE f.ID IN (343, 493, 494)
 ORDER BY f.ID;


-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA DEL BLOQUE 3.
-- COMMIT;


-- =====================================================================
-- DESPUES DEL COMMIT: regenerar el ATS de 08/2026.
--
-- REVERSO — comentado. Los valores originales de SUBCERO eran:
--   343 -> 0    ·   493 -> 0    ·   494 -> 0
--   UPDATE PGS.FCTC SET SUBCERO = 0 WHERE ID IN (343, 493, 494);
--   COMMIT;
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
