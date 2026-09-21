-- =====================================================================
-- e2-57 — ¿CUALES DE NUESTROS ARREGLOS DE DATOS ESTAN REALMENTE EN LA BASE?
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- POR QUE EXISTE — pregunta del usuario, 2026-09-21: "muchos de los errores
-- que me reportan supuestamente ya te los habia reportado y habian sido
-- solucionados, ¿que paso?".
--
-- Hoy se midio que el e2-52 se corrio el 21-09 y NO quedo guardado: el
-- COMMIT de nuestros scripts va comentado por convencion, el UPDATE corre,
-- informa las filas afectadas, y se pierde al cerrar la sesion SIN ningun
-- error (§46.1 del estado del equipo). Se descubrio porque el ATS seguia
-- mal; ningun otro script deja esa pista.
--
-- Este script contesta de una sola vez, para CADA arreglo de datos que este
-- equipo entrego, si esta aplicado o no. No corrige nada: solo mide.
--
-- COMO LEERLO: cada bloque devuelve una fila con VEREDICTO.
--   'APLICADO'      -> el arreglo esta en la base
--   'NO APLICADO'   -> se perdio, o nunca se corrio. Hay que rehacerlo
--   'NADA QUE HACER'-> no habia filas para corregir
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — e2-45: el rubro 35 (tipo de persona) guardaba el TEXTO 'null'
-- El ATS no escribia <tipoCliente> por eso. Corregido a '01'/'02'.
-- ESPERADO: 'APLICADO', con los dos detalles en 01 y 02.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - e2-45 rubro 35' AS bloque,
       CASE WHEN COUNT(CASE WHEN LOWER(NVL(d.PDTRVLRV,'x')) = 'null' THEN 1 END) > 0
              THEN 'NO APLICADO - sigue el texto null'
            WHEN COUNT(CASE WHEN d.PDTRVLRV IN ('01','02') THEN 1 END) >= 2
              THEN 'APLICADO'
            ELSE 'REVISAR A MANO' END AS veredicto,
       COUNT(*) AS detalles_del_rubro,
       LISTAGG(d.PDTRALTR || '=' || NVL(d.PDTRVLRV,'(nulo)'), ' | ')
         WITHIN GROUP (ORDER BY d.PDTRALTR) AS valores
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 35;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — e2-47: el numero de las notas de venta manuales a 15 digitos
-- La retencion 278 volvio DEVUELTA por un numDocSustento de 13 digitos.
-- ESPERADO: 'APLICADO' (establecimiento 3, punto 3, secuencial 9).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - e2-47 nota de venta 15 digitos' AS bloque,
       CASE WHEN COUNT(*) = 0 THEN 'NADA QUE HACER - no hay notas de venta'
            WHEN COUNT(CASE WHEN LENGTH(f.NUMESTABLECIMIENTO) <> 3
                              OR LENGTH(f.NUMPTOEMISION) <> 3
                              OR LENGTH(f.SECUENCIAL) <> 9 THEN 1 END) > 0
              THEN 'NO APLICADO - siguen con menos digitos'
            ELSE 'APLICADO' END AS veredicto,
       COUNT(*) AS notas_de_venta,
       LISTAGG(f.NUMESTABLECIMIENTO || '-' || f.NUMPTOEMISION || '-' || f.SECUENCIAL, ' | ')
         WITHIN GROUP (ORDER BY f.ID) AS numeros
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02';


-- ---------------------------------------------------------------------
-- BLOQUE 3 — e2-53: la nota de venta declara su base como 0%, no gravada
-- ESPERADO: 'APLICADO' (SUBCERO = SUBTOTAL en las que no tienen IVA).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - e2-53 nota de venta base 0%' AS bloque,
       CASE WHEN COUNT(*) = 0 THEN 'NADA QUE HACER - no hay notas de venta activas sin IVA'
            WHEN COUNT(CASE WHEN ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005 THEN 1 END) > 0
              THEN 'NO APLICADO - siguen declarando base gravada'
            ELSE 'APLICADO' END AS veredicto,
       COUNT(*) AS notas_de_venta,
       SUM(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) AS base_gravada_que_declararia
  FROM PGS.FCTC f
 WHERE f.TIPOCOMPROBANTE = '02'
   AND f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — e2-52 / e2-54: las compras 100% al 0% no se declaran gravadas
-- ESPERADO: 'APLICADO' (se verifico el 21-09, pero se vuelve a medir acá
--           para tener las cinco respuestas juntas).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - e2-52/e2-54 compras 100% al 0%' AS bloque,
       CASE WHEN COUNT(*) = 0 THEN 'NADA QUE HACER'
            ELSE 'NO APLICADO - ' || COUNT(*) || ' compra(s) sin corregir' END AS veredicto,
       NVL(SUM(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)),0) AS base_gravada_de_mas
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND NVL(f.VIVA,0) < 0.005
   AND ABS(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0)) >= 0.005
   AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
   AND NOT EXISTS (SELECT 1 FROM PGS.DFCC d
                    WHERE d.FACTURA = f.ID
                      AND d.CODIGOIVASRI IS NOT NULL
                      AND d.CODIGOIVASRI NOT IN (0,6,7));


-- ---------------------------------------------------------------------
-- BLOQUE 5 — e2-55: la columna SUBNOOBJ y el bomberos fuera de la base 0%
-- ESPERADO antes de correr el e2-55: 'NO APLICADO'. Despues: 'APLICADO'.
--   Si la columna todavia no existe, este bloque falla con ORA-00904: eso
--   tambien es una respuesta ('el e2-55 no se corrio').
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 5 - e2-55 base no objeto' AS bloque,
       CASE WHEN COUNT(*) = 0 THEN 'NADA QUE HACER - ninguna factura con terceros'
            WHEN COUNT(CASE WHEN NVL(f.SUBNOOBJ,0) < 0.005 THEN 1 END) > 0
              THEN 'NO APLICADO - el bomberos sigue en la base 0%'
            ELSE 'APLICADO' END AS veredicto,
       COUNT(*) AS facturas_con_terceros,
       SUM(NVL(f.SUBNOOBJ,0)) AS total_no_objeto
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND EXISTS (SELECT 1 FROM PGS.DFCC d
                WHERE d.FACTURA = f.ID
                  AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                    OR UPPER(d.DESCRIPCION) LIKE '%BASURA%'));


-- ---------------------------------------------------------------------
-- BLOQUE 6 — LO QUE NUNCA SE ARREGLO (no es un arreglo perdido, es un
--            frente abierto): las facturas MIXTAS, con parte al 0% y parte
--            gravada, donde la cabecera no coincide con el detalle.
-- Estas son las que el usuario reporta como "manda toda la base con IVA".
-- ESPERADO: informativo. Es lo que mide el frente del reparto por tarifa.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 6 - facturas mixtas (frente abierto)' AS bloque,
       COUNT(*) AS facturas,
       SUM(base_0_detalle)   AS base_0_segun_el_detalle,
       SUM(base_0_cabecera)  AS base_0_segun_la_cabecera,
       SUM(base_0_detalle - base_0_cabecera) AS base_que_el_ats_declara_gravada_de_mas
  FROM (
    SELECT f.ID,
           NVL(f.SUBCERO,0) AS base_0_cabecera,
           (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
             WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle
      FROM PGS.FCTC f
     WHERE f.ESTADO = 1
       AND EXISTS (SELECT 1 FROM PGS.DFCC d WHERE d.FACTURA = f.ID)
  )
 WHERE ABS(base_0_detalle - base_0_cabecera) >= 0.005;


-- ---------------------------------------------------------------------
-- BLOQUE 7 — El mismo corte, factura por factura y solo de agosto 2026,
--            que es lo que afecta al anexo que se esta declarando.
-- ESPERADO: la lista de las que hay que corregir para el ATS de agosto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 7 - mixtas de agosto 2026' AS bloque,
       f.ID, f.NUMERO, f.FECHA, t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_cabecera,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0) AS base_0_detalle,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND d.CODIGOIVASRI IS NOT NULL
           AND d.CODIGOIVASRI NOT IN (0,6,7)) AS base_gravada_detalle,
       NVL(f.VIVA,0) AS iva_cabecera
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ABS(NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID AND NVL(d.CODIGOIVASRI,-1) = 0)) >= 0.005
 ORDER BY f.FECHA, f.ID;
