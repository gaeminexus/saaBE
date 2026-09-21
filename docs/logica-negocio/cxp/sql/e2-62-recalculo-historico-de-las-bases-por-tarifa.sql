-- =====================================================================
-- e2-62 — Recalculo HISTORICO de las bases por tarifa desde el detalle
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ⚠️ NO ES SOLO LECTURA: los BLOQUES 3 a 6 hacen UPDATE.
--    Correr los BLOQUES 0, 1 y 2, LEERLOS ENTEROS, y recien ahi seguir.
--
-- ⛔⛔ ESTE SCRIPT CAMBIA NUMEROS DE PERIODOS YA DECLARADOS ⛔⛔
--    El usuario eligio el alcance COMPLETO el 2026-09-21 sabiendo esto
--    (ver PLAN-CLASIFICACION-POR-TARIFA-COMPRAS.md §4). El BLOQUE 0 muestra
--    el impacto MES POR MES antes de tocar nada: leerlo y decidir si se
--    corre para todo el historico o solo para los meses sin declarar.
--    Para acotarlo, descomentar el filtro de fecha marcado en cada bloque.
--
-- ⛔⛔ EL COMMIT DEL FINAL NO ES OPCIONAL ⛔⛔
--    Correr el script no guarda nada. El e2-52 se perdio exactamente asi.
--
-- ⚠️ VA DESPUES DEL e2-55 Y DEL e2-61: usa columnas que crean esos dos.
--
-- POR QUE EXISTE — decision del usuario: la clasificacion por tarifa se
-- hace completa. El codigo ya reparte lo que entra de ahora en adelante
-- (commit b6dd7bcb); esto es para lo YA CARGADO.
--
-- EL CRITERIO, y es lo unico que hace seguro correr esto sobre el historico:
--   Solo se recalcula el documento cuyo DETALLE ESTA COMPLETO Y CIERRA:
--     (a) ninguna linea con CODIGOIVASRI nulo  -- un codigo nulo es
--         "no se sabe", no "gravado"; y
--     (b) | suma de TODAS las bases del detalle - SUBTOTAL | < 0.01
--   Si el detalle no cierra, el documento NO SE TOCA y se lista en el
--   BLOQUE 2 para mirarlo a mano.
--
--   Esto ya evito un desastre una vez: la factura 423 tiene 400.00 con el
--   detalle entero sin codigos, y recalcular a ciegas la habria dejado
--   declarando 400 como GRAVADOS (e2-60).
--
-- QUE NO TOCA: SUBTOTAL, TOTAL, VIVA. Solo mueve plata ENTRE los buckets.
--   La suma de los buckets nunca puede superar al SUBTOTAL.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC),
--   NotaCreditoCompra (PGS.NTCC), NotaDebitoCompra (PGS.NTDC),
--   LiquidacionCompraCompra (PGS.LQCC), DetalleFacturaCompra (PGS.DFCC:
--   FACTURA, BASEIMPONIBLE, CODIGOIVASRI).
--
-- ⚠️ SOLO CUBRE LA FACTURA DE COMPRA (PGS.FCTC / PGS.DFCC). Las notas de
--    credito, de debito y las liquidaciones tienen su detalle en OTRAS
--    tablas; sus buckets nuevos quedan en 0, que es exactamente como se
--    comportaban antes. Cuando el BLOQUE 7 diga que hay volumen ahi, se
--    escribe el script que corresponda con los nombres de esas tablas
--    copiados de sus entidades -- no se adivinan aca.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — EL IMPACTO, MES POR MES. Leer esto antes que nada.
-- ESPERADO: una fila por periodo. CUANTAS = documentos que cambiarian;
--           MUEVE = cuanta base se mueve de una columna a otra.
--   Los meses ya declarados aparecen aca: esa es la decision a tomar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - impacto por periodo' AS bloque,
       TO_CHAR(f.FECHA, 'YYYY-MM') AS periodo,
       COUNT(*) AS cuantas,
       SUM(ABS(NVL(f.SUBCERO,0)   - det_0))    AS mueve_base_0,
       SUM(ABS(NVL(f.SUBNOOBJ,0)  - det_6))    AS mueve_no_objeto,
       SUM(ABS(NVL(f.SUBEXENT,0)  - det_7))    AS mueve_exento,
       SUM(ABS(NVL(f.SUBTOTAL5,0) - det_5))    AS mueve_5,
       SUM(ABS(NVL(f.SUBTOTAL8,0) - det_8))    AS mueve_8
  FROM PGS.FCTC f
  JOIN (SELECT d.FACTURA AS id,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 0 THEN d.BASEIMPONIBLE END),0) AS det_0,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 5 THEN d.BASEIMPONIBLE END),0) AS det_5,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN d.BASEIMPONIBLE END),0) AS det_6,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN d.BASEIMPONIBLE END),0) AS det_7,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 8 THEN d.BASEIMPONIBLE END),0) AS det_8,
               NVL(SUM(d.BASEIMPONIBLE),0) AS det_total,
               COUNT(CASE WHEN d.CODIGOIVASRI IS NULL THEN 1 END) AS sin_codigo
          FROM PGS.DFCC d GROUP BY d.FACTURA) x ON x.id = f.ID
 WHERE f.ESTADO = 1
   AND x.sin_codigo = 0
   AND ABS(x.det_total - NVL(f.SUBTOTAL,0)) < 0.01
   AND ( ABS(NVL(f.SUBCERO,0)   - x.det_0) >= 0.005
      OR ABS(NVL(f.SUBNOOBJ,0)  - x.det_6) >= 0.005
      OR ABS(NVL(f.SUBEXENT,0)  - x.det_7) >= 0.005
      OR ABS(NVL(f.SUBTOTAL5,0) - x.det_5) >= 0.005
      OR ABS(NVL(f.SUBTOTAL8,0) - x.det_8) >= 0.005 )
 GROUP BY TO_CHAR(f.FECHA, 'YYYY-MM')
 ORDER BY periodo;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las corregibles, una por una. ANOTAR: es el respaldo del
--            reverso, no hay otra copia de los valores originales.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - corregibles' AS bloque,
       f.ID, f.NUMERO, f.FECHA,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)   AS base_0_hoy,    x.det_0 AS base_0_nueva,
       NVL(f.SUBNOOBJ,0)  AS no_objeto_hoy, x.det_6 AS no_objeto_nueva,
       NVL(f.SUBEXENT,0)  AS exento_hoy,    x.det_7 AS exento_nueva,
       NVL(f.SUBTOTAL5,0) AS base5_hoy,     x.det_5 AS base5_nueva,
       NVL(f.SUBTOTAL8,0) AS base8_hoy,     x.det_8 AS base8_nueva
  FROM PGS.FCTC f
  JOIN (SELECT d.FACTURA AS id,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 0 THEN d.BASEIMPONIBLE END),0) AS det_0,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 5 THEN d.BASEIMPONIBLE END),0) AS det_5,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN d.BASEIMPONIBLE END),0) AS det_6,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN d.BASEIMPONIBLE END),0) AS det_7,
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 8 THEN d.BASEIMPONIBLE END),0) AS det_8,
               NVL(SUM(d.BASEIMPONIBLE),0) AS det_total,
               COUNT(CASE WHEN d.CODIGOIVASRI IS NULL THEN 1 END) AS sin_codigo
          FROM PGS.DFCC d GROUP BY d.FACTURA) x ON x.id = f.ID
 WHERE f.ESTADO = 1
   AND x.sin_codigo = 0
   AND ABS(x.det_total - NVL(f.SUBTOTAL,0)) < 0.01
   AND ( ABS(NVL(f.SUBCERO,0)   - x.det_0) >= 0.005
      OR ABS(NVL(f.SUBNOOBJ,0)  - x.det_6) >= 0.005
      OR ABS(NVL(f.SUBEXENT,0)  - x.det_7) >= 0.005
      OR ABS(NVL(f.SUBTOTAL5,0) - x.det_5) >= 0.005
      OR ABS(NVL(f.SUBTOTAL8,0) - x.det_8) >= 0.005 )
 -- Para acotar a un periodo, descomentar:
 -- AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las que NO se tocan, y por que. Control del criterio.
-- ESPERADO: la 423 entre ellas ("detalle sin codigos"), y las de la
--           electrica como "el detalle no cierra" por centavos.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - no se tocan' AS bloque,
       f.ID, f.NUMERO, f.FECHA,
       NVL(f.SUBTOTAL,0) AS subtotal, x.det_total AS suma_del_detalle,
       x.sin_codigo AS lineas_sin_codigo,
       CASE WHEN x.sin_codigo > 0 THEN 'DETALLE SIN CODIGOS - no se puede decidir'
            ELSE 'EL DETALLE NO CIERRA CONTRA EL SUBTOTAL' END AS motivo
  FROM PGS.FCTC f
  JOIN (SELECT d.FACTURA AS id, NVL(SUM(d.BASEIMPONIBLE),0) AS det_total,
               COUNT(CASE WHEN d.CODIGOIVASRI IS NULL THEN 1 END) AS sin_codigo
          FROM PGS.DFCC d GROUP BY d.FACTURA) x ON x.id = f.ID
 WHERE f.ESTADO = 1
   AND (x.sin_codigo > 0 OR ABS(x.det_total - NVL(f.SUBTOTAL,0)) >= 0.01)
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — UPDATE. Los cinco buckets salen del detalle, de una vez.
-- ESPERADO: tantas filas como conto el BLOQUE 0 en total.
--   Para acotar a un periodo, descomentar el filtro de fecha del final.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET (f.SUBCERO, f.SUBNOOBJ, f.SUBEXENT, f.SUBTOTAL5, f.SUBTOTAL8) =
       (SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 0 THEN d.BASEIMPONIBLE END),0),
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN d.BASEIMPONIBLE END),0),
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN d.BASEIMPONIBLE END),0),
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 5 THEN d.BASEIMPONIBLE END),0),
               NVL(SUM(CASE WHEN d.CODIGOIVASRI = 8 THEN d.BASEIMPONIBLE END),0)
          FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
 WHERE f.ESTADO = 1
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NULL)
   AND ABS( (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
            - NVL(f.SUBTOTAL,0) ) < 0.01
 -- Para acotar a un periodo, descomentar:
 -- AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Control posterior, ANTES del COMMIT.
-- ESPERADO: CERO filas. Cualquier fila aca es un documento donde la suma
--           de los buckets supera al SUBTOTAL: ROLLBACK y avisar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - control: buckets mayores que el subtotal' AS bloque,
       f.ID, f.NUMERO, NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0) + NVL(f.SUBNOOBJ,0) + NVL(f.SUBEXENT,0)
         + NVL(f.SUBTOTAL5,0) + NVL(f.SUBTOTAL8,0) AS suma_de_buckets
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND NVL(f.SUBCERO,0) + NVL(f.SUBNOOBJ,0) + NVL(f.SUBEXENT,0)
       + NVL(f.SUBTOTAL5,0) + NVL(f.SUBTOTAL8,0) - NVL(f.SUBTOTAL,0) > 0.01
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 5 — Control posterior 2: como queda agosto 2026, que es lo que
--            se esta declarando.
-- ESPERADO: la gravada de cada factura igual a lo que dice su detalle.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 5 - agosto 2026 despues' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0)  AS subtotal,
       NVL(f.SUBCERO,0)   AS base_0,
       NVL(f.SUBNOOBJ,0)  AS no_objeto,
       NVL(f.SUBEXENT,0)  AS exento,
       NVL(f.SUBTOTAL5,0) AS base_5,
       NVL(f.SUBTOTAL8,0) AS base_8,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS gravada_del_ats
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 6 — Consulta suelta que quedo pendiente del BE-8: ¿hay notas de
--            venta manuales con lineas de codigo 6 o 7?
-- ESPERADO: CERO filas. La pantalla no permite elegir esos codigos hoy
--           (la linea nace en '0' y no hay control), asi que si sale algo
--           entro por el endpoint a mano y hay que mirarlo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 6 - notas de venta con codigo 6 o 7' AS bloque,
       f.ID, f.NUMERO, d.DESCRIPCION, d.CODIGOIVASRI, d.BASEIMPONIBLE
  FROM PGS.FCTC f
  JOIN PGS.DFCC d ON d.FACTURA = f.ID
 WHERE f.TIPOCOMPROBANTE = '02'
   AND d.CODIGOIVASRI IN (6,7)
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 7 — Cuanto volumen hay en los OTROS documentos de compra, que
--            este script no toca. Dice si hace falta escribir el suyo.
-- ESPERADO: informativo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 7 - otros documentos de compra' AS bloque, 'NTCC' AS tabla,
       COUNT(*) AS documentos, SUM(NVL(SUBTOTAL,0)) AS subtotal, SUM(NVL(SUBCERO,0)) AS base_0
  FROM PGS.NTCC WHERE ESTADO = 1
UNION ALL
SELECT 'BLOQUE 7 - otros documentos de compra', 'NTDC',
       COUNT(*), SUM(NVL(SUBTOTAL,0)), SUM(NVL(SUBCERO,0)) FROM PGS.NTDC WHERE ESTADO = 1
UNION ALL
SELECT 'BLOQUE 7 - otros documentos de compra', 'LQCC',
       COUNT(*), SUM(NVL(SUBTOTAL,0)), SUM(NVL(SUBCERO,0)) FROM PGS.LQCC WHERE ESTADO = 1;


-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA DEL BLOQUE 3.
-- COMMIT;


-- =====================================================================
-- DESPUES DEL COMMIT: regenerar el ATS del periodo y revisar el cuadre 104.
--
-- REVERSO — comentado. Los valores originales son los del BLOQUE 1, que
-- por eso pide anotarse. No hay forma de reconstruirlos despues del COMMIT:
--   UPDATE PGS.FCTC SET SUBCERO = <base_0_hoy>, SUBNOOBJ = <no_objeto_hoy>,
--          SUBEXENT = <exento_hoy>, SUBTOTAL5 = <base5_hoy>, SUBTOTAL8 = <base8_hoy>
--    WHERE ID = <id>;
--   COMMIT;
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
