-- =====================================================================
-- e2-66 — Las bases NO OBJETO y EXENTO que el e2-55 NO cubrio
--          Agosto 2026 declara 400,39 como GRAVADA sin un centavo de IVA
-- Modulo: sri / cxp  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ESTE SCRIPT ESCRIBE. EL `COMMIT` DEL FINAL ESTA COMENTADO A PROPOSITO,
-- ASI QUE **SIN DESCOMENTARLO NO PASA NADA**: los UPDATE corren, informan
-- las filas afectadas, y se pierden enteros al cerrar la sesion. SIN UN
-- SOLO ERROR. Paso el 2026-09-21 con el e2-52 y costo el dia (§46.1 del
-- documento de estado). El BLOQUE 3 es el unico que prueba que quedo
-- guardado.
--
-- DE DONDE SALE — de la salida del `e2-56`, corrida por el usuario el
-- 2026-09-21. Agosto 2026 tiene 17 lineas con codigo SRI 6 (no objeto) o
-- 7 (exento). De esas, 12 son CONTRIBUCION BOMBEROS y ya las cubrio el
-- e2-55 (columna ES_TERCERO = 'SI'). Las otras 5 no las cubre nadie:
--
--   id 423  001-100-000000037  RAMIREZ MOLINA LEONARDO DAVID
--           "REEMBOLSOS"        codigo 6   400,00   <-- la factura ENTERA
--   id 495  092-999-011117120  CNEL EP     "Intereses por Mora"  cod 7  0,02
--   id 427  001-999-134308072  E.E. QUITO  "Intereses por Mora"  cod 7  0,16
--   id 425  001-999-134470375  E.E. QUITO  "Intereses por Mora"  cod 7  0,18
--   id 496  001-999-134482033  E.E. QUITO  "Intereses por Mora"  cod 7  0,03
--                                                        TOTAL:  400,39
--
--   El e2-55 identifica las lineas de terceros POR SU DESCRIPCION
--   (BOMBERO/BASURA). Un reembolso y un interes por mora no matchean, y
--   por eso quedaron afuera. No es un fallo del e2-55: es su alcance.
--
-- POR QUE IMPORTA HOY — el ATS calcula
--     baseImpGrav = SUBTOTAL - SUBCERO - SUBNOOBJ - SUBEXENT
--   (GeneradorAtsServiceImpl:288, `baseGravadaCompra`). Con SUBNOOBJ y
--   SUBEXENT en cero, esos 400,39 se declaran como base GRAVADA de un
--   documento sin IVA. Es EXACTAMENTE el sintoma que el DIMM mostro a la
--   manana y que motivo todo el frente del dia: base gravada con
--   "Monto IVA = 0,00".
--
-- NO HACE FALTA WAR. Las dos premisas se midieron contra el codigo el
-- 2026-09-21, con el WAR de `3e32aa35` YA DESPLEGADO:
--   1. El ATS resta SUBNOOBJ y SUBEXENT y los emite como `baseNoGraIva`
--      y `baseImpExe` (GeneradorAtsServiceImpl:288, :325, :830-835).
--   2. La carga nueva ya reparte cod 6 -> SUBNOOBJ y cod 7 -> SUBEXENT en
--      los cuatro documentos (ProcesoCargaDocumentosServiceImpl:1687-1688,
--      :2818, :2946, :3072).
--   O sea: lo que entre de ahora en adelante entra bien. Esto corrige
--   SOLO lo que ya estaba cargado.
--
-- CRITERIO — se asigna desde el DETALLE, no se suma:
--     SUBNOOBJ = suma de las lineas con CODIGOIVASRI = 6
--     SUBEXENT = suma de las lineas con CODIGOIVASRI = 7
--   Asignar (y no incrementar) lo hace IDEMPOTENTE: correrlo dos veces da
--   el mismo resultado, y las facturas de bomberos que el e2-55 ya dejo
--   bien quedan igual. Ademas solo toca las que DIFIEREN, asi que el
--   bloque de control posterior es limpio.
--   Estas facturas vienen del XML, que trae el codigo por linea: el
--   detalle ES la fuente. (Distinto del caso de la nota de venta, que se
--   tipea y donde manda el SUBTOTAL de cabecera — §46.5.)
--
-- GUARDA — no se toca ninguna factura donde
--     SUBCERO + SUBNOOBJ_nuevo + SUBEXENT_nuevo > SUBTOTAL
--   porque produciria una base gravada NEGATIVA. El generador ya avisa de
--   ese caso (:285); aca directamente no se crea.
--
-- OBSERVACION PARA EL CONTADOR, no la decide este equipo: la factura 423
--   es un REEMBOLSO por el total del documento. El XML la declara con
--   codigo 6 (no objeto) y este script respeta ese dato. Si el criterio
--   contable fuera declararla bajo el esquema de reembolso del ATS, el
--   tratamiento es otro y hay que decirlo ANTES de generar el anexo.
--
-- Columnas verificadas contra las entidades, no de memoria:
--   PGS.FCTC  -> FacturaCompra: ID, NUMERO, FECHA, SUBTOTAL, SUBCERO,
--                SUBNOOBJ, SUBEXENT, VIVA, ESTADO
--   PGS.DFCC  -> DetalleFacturaCompra: FACTURA, DESCRIPCION,
--                BASEIMPONIBLE, CODIGOIVASRI
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — CONTROL PREVIO. Solo lectura.
-- ESPERADO: 5 filas (423, 495, 427, 425, 496) con DIFERENCIA <> 0.
--           Las de bomberos NO deben aparecer: el e2-55 ya las dejo bien.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - antes' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0,
       NVL(f.SUBNOOBJ,0) AS noobj_hoy,
       NVL(d.base6,0)    AS noobj_correcto,
       NVL(f.SUBEXENT,0) AS exent_hoy,
       NVL(d.base7,0)    AS exent_correcto,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS gravada_hoy,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(d.base6,0)    - NVL(d.base7,0)    AS gravada_correcta,
       (NVL(d.base6,0) - NVL(f.SUBNOOBJ,0)) + (NVL(d.base7,0) - NVL(f.SUBEXENT,0))  AS diferencia
  FROM PGS.FCTC f
  JOIN ( SELECT FACTURA,
                SUM(CASE WHEN CODIGOIVASRI = 6 THEN NVL(BASEIMPONIBLE,0) ELSE 0 END) AS base6,
                SUM(CASE WHEN CODIGOIVASRI = 7 THEN NVL(BASEIMPONIBLE,0) ELSE 0 END) AS base7
           FROM PGS.DFCC
          GROUP BY FACTURA ) d ON d.FACTURA = f.ID
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ( NVL(f.SUBNOOBJ,0) <> NVL(d.base6,0) OR NVL(f.SUBEXENT,0) <> NVL(d.base7,0) )
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — EL UPDATE de AGOSTO 2026, que es el periodo que se declara.
-- ESPERADO: "5 filas actualizadas". Si dice 0, el BLOQUE 0 ya venia vacio
--           y no habia nada que corregir.
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBNOOBJ = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                        FROM PGS.DFCC d WHERE d.FACTURA = f.ID ),
       f.SUBEXENT = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                        FROM PGS.DFCC d WHERE d.FACTURA = f.ID )
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND EXISTS ( SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IN (6,7) )
   -- solo las que difieren: deja intactas las que el e2-55 ya dejo bien
   AND ( NVL(f.SUBNOOBJ,0) <> ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                                  FROM PGS.DFCC d WHERE d.FACTURA = f.ID )
      OR NVL(f.SUBEXENT,0) <> ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                                  FROM PGS.DFCC d WHERE d.FACTURA = f.ID ) )
   -- GUARDA: nunca producir una base gravada negativa
   AND NVL(f.SUBCERO,0)
       + ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI IN (6,7) THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
             FROM PGS.DFCC d WHERE d.FACTURA = f.ID ) <= NVL(f.SUBTOTAL,0);


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las que la GUARDA dejo afuera. Solo lectura.
-- ESPERADO: CERO filas. Cada fila es una factura cuyo detalle no cierra
--           contra su cabecera: hay que mirarla a mano, NO forzarla.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - descartadas por la guarda' AS bloque,
       f.ID, f.NUMERO, NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0,
       NVL(d.base67,0) AS no_gravado_del_detalle,
       NVL(f.SUBCERO,0) + NVL(d.base67,0) - NVL(f.SUBTOTAL,0) AS exceso
  FROM PGS.FCTC f
  JOIN ( SELECT FACTURA, SUM(CASE WHEN CODIGOIVASRI IN (6,7) THEN NVL(BASEIMPONIBLE,0) ELSE 0 END) AS base67
           FROM PGS.DFCC GROUP BY FACTURA ) d ON d.FACTURA = f.ID
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(f.SUBCERO,0) + NVL(d.base67,0) > NVL(f.SUBTOTAL,0)
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — CONTROL POSTERIOR. Solo lectura. ES EL QUE IMPORTA.
-- Es la MISMA consulta del BLOQUE 0.
-- ESPERADO: **CERO FILAS**.
--   Si sigue devolviendo las 5 de antes, el UPDATE corrio y NO se guardo:
--   falta el COMMIT. No es un problema de criterio ni de datos.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - despues (debe dar CERO filas)' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBNOOBJ,0) AS noobj_hoy, NVL(d.base6,0) AS noobj_correcto,
       NVL(f.SUBEXENT,0) AS exent_hoy, NVL(d.base7,0) AS exent_correcto
  FROM PGS.FCTC f
  JOIN ( SELECT FACTURA,
                SUM(CASE WHEN CODIGOIVASRI = 6 THEN NVL(BASEIMPONIBLE,0) ELSE 0 END) AS base6,
                SUM(CASE WHEN CODIGOIVASRI = 7 THEN NVL(BASEIMPONIBLE,0) ELSE 0 END) AS base7
           FROM PGS.DFCC GROUP BY FACTURA ) d ON d.FACTURA = f.ID
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ( NVL(f.SUBNOOBJ,0) <> NVL(d.base6,0) OR NVL(f.SUBEXENT,0) <> NVL(d.base7,0) )
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — El numero que tiene que dar, para contrastarlo contra el
--            anexo regenerado. Solo lectura. (§35: contrastar la salida
--            contra un valor conocido.)
-- ESPERADO despues del COMMIT, para agosto 2026:
--            no_objeto = 477,12   ·   exento = 0,39
--          y esos 477,51 DEJAN de estar dentro de baseImpGrav.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - lo que debe declarar agosto' AS bloque,
       SUM(NVL(f.SUBTOTAL,0)) AS subtotal,
       SUM(NVL(f.SUBCERO,0))  AS base_0,
       SUM(NVL(f.SUBNOOBJ,0)) AS no_objeto,
       SUM(NVL(f.SUBEXENT,0)) AS exento,
       SUM(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0)) AS base_gravada
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01';


-- ---------------------------------------------------------------------
-- BLOQUE 5 — SEPTIEMBRE 2026. Mismo arreglo, periodo TODAVIA NO
--            DECLARADO. El e2-56 midio 1 factura con 420,00 de codigo 6.
-- Se deja ACTIVO a proposito: corregirlo ahora es gratis y evita repetir
-- toda esta conversacion el mes que viene.
-- ESPERADO: "1 fila actualizada" (o 0 si ya entro bien por la carga nueva).
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBNOOBJ = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                        FROM PGS.DFCC d WHERE d.FACTURA = f.ID ),
       f.SUBEXENT = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                        FROM PGS.DFCC d WHERE d.FACTURA = f.ID )
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-09-01' AND f.FECHA < DATE '2026-10-01'
   AND EXISTS ( SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IN (6,7) )
   AND ( NVL(f.SUBNOOBJ,0) <> ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                                  FROM PGS.DFCC d WHERE d.FACTURA = f.ID )
      OR NVL(f.SUBEXENT,0) <> ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
                                  FROM PGS.DFCC d WHERE d.FACTURA = f.ID ) )
   AND NVL(f.SUBCERO,0)
       + ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI IN (6,7) THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
             FROM PGS.DFCC d WHERE d.FACTURA = f.ID ) <= NVL(f.SUBTOTAL,0);


-- ---------------------------------------------------------------------
-- BLOQUE 6 — JULIO 2026 y anteriores. COMENTADO A PROPOSITO.
-- El e2-56 midio julio: 448,20 de codigo 6 y 0,01 de codigo 7. Pero
-- **julio YA SE DECLARO**, y cambiar la base de un periodo presentado es
-- decision del usuario y del contador, no de este script — el mismo
-- criterio que el e2-62.
-- Casi todo julio es bomberos (ya movido por el e2-55): lo que quedaria
-- por corregir es chico. Medirlo primero con el BLOQUE 0 cambiandole las
-- fechas, y recien despues decidir.
--
-- UPDATE PGS.FCTC f
--    SET f.SUBNOOBJ = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 6 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
--                         FROM PGS.DFCC d WHERE d.FACTURA = f.ID ),
--        f.SUBEXENT = ( SELECT NVL(SUM(CASE WHEN d.CODIGOIVASRI = 7 THEN NVL(d.BASEIMPONIBLE,0) ELSE 0 END),0)
--                         FROM PGS.DFCC d WHERE d.FACTURA = f.ID )
--  WHERE f.ESTADO = 1
--    AND f.FECHA < DATE '2026-08-01'
--    AND EXISTS ( SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IN (6,7) );


-- ---------------------------------------------------------------------
-- REVERSO — comentado. Devuelve las dos columnas a CERO en agosto y
-- septiembre. Ojo: esto tambien revierte lo que el e2-55 dejo bien, asi
-- que si se usa hay que volver a correr el e2-55 despues.
--
-- UPDATE PGS.FCTC f SET f.SUBNOOBJ = 0, f.SUBEXENT = 0
--  WHERE f.ESTADO = 1
--    AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-10-01';
-- ---------------------------------------------------------------------


-- SIN ESTO, NADA DE LO DE ARRIBA QUEDA GUARDADO. Descomentar y correr.
-- COMMIT;
