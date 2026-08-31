-- =====================================================================
-- ANULADO (7) en el ciclo de vida de PGS.DCXP
-- Modulo: CXP  ·  Equipo: omen-saa-3  ·  Fecha: 2026-08-31
--
-- QUE HACE
--   Agrega UN detalle al rubro 175 (CXP_ESTADO_DOCUMENTO_CXP), que ya
--   existe con LEIDO(1), XML_CARGADO(2), REGISTRADO_BD(3), ERROR(4),
--   NOVEDAD(5) y REVERTIDO(6). No crea ningun rubro nuevo.
--
--   PDTR 1400 -- reservado en REGISTRO-RESERVAS-EQUIPOS.md, primer codigo
--   usado del bloque 1400-1499 de este equipo.
--
-- POR QUE HACE FALTA UN ESTADO NUEVO Y NO SE REUSA REVERTIDO
--   REVERTIDO(6) significa "los registros destino SE BORRARON": la ingesta
--   se deshizo y el documento puede volver a procesarse.
--   ANULADO(7) significa "los registros SIGUEN AHI, anulados": la factura
--   existe, su asiento esta anulado y el pago esta anulado. Es un hecho
--   contable que ocurrio. Es TERMINAL: no se reprocesa nunca.
--   Mezclarlos haria imposible distinguir los dos casos al consultar, y
--   permitiria reprocesar un documento anulado.
--
--   Diseño completo:
--   docs/logica-negocio/cxp/DISENO-ANULAR-VS-RECONTABILIZAR-FACTURA-COMPRA.md
--
-- ORDEN RESPECTO DEL WAR
--   Este script va ANTES de desplegar el WAR. El codigo Java ya declara
--   EstadoDocumentoCxp.ANULADO = 7 y lo escribe en DCXP.DCXPESTD; si la
--   fila del catalogo no esta, el estado se graba igual (DCXPESTD no tiene
--   FK ni CHECK -- verificado) pero las pantallas que resuelven la etiqueta
--   contra SCP.PDTR mostrarian el numero pelado en vez del texto.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0: CONTROL PREVIO  (leer la salida antes de seguir)
-- ---------------------------------------------------------------------

-- 0.1 El rubro 175 tiene que existir. Se busca por codigo alterno, que es
--     lo que usa la aplicacion; el PK puede ser otro numero.
SELECT PRBRCDGO AS PK_RUBRO, PRBRALTR AS ALTERNO, PRBRDSCR
  FROM SCP.PRBR
 WHERE PRBRALTR = 175;
-- Debe devolver EXACTAMENTE 1 fila. Si devuelve 0, PARAR: el rubro no
-- existe y este script no es el que hay que correr.

-- 0.2 Los seis detalles actuales del rubro. Sirve de foto previa.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 175
 ORDER BY d.PDTRALTR;
-- Se esperan 6 filas, alternos 1..6.

-- 0.3 El PDTRCDGO 1400 tiene que estar libre.
SELECT COUNT(*) AS DEBE_SER_CERO FROM SCP.PDTR WHERE PDTRCDGO = 1400;
-- Si devuelve 1, PARAR y avisar al arbitro: el bloque 1400-1499 estaba
-- reservado para este equipo y alguien mas lo uso.

-- 0.4 MAX actual, control obligatorio del registro de reservas (regla 2).
SELECT MAX(PDTRCDGO) AS MAX_PDTR FROM SCP.PDTR;

-- 0.5 Nadie debe tener ya el estado 7. Si hay filas, este script ya se
--     corrio o el 7 significa otra cosa: PARAR.
SELECT COUNT(*) AS DEBE_SER_CERO FROM PGS.DCXP WHERE DCXPESTD = 7;


-- ---------------------------------------------------------------------
-- BLOQUE 1: el detalle nuevo
--   PDTRVLRN = 7   -> el valor numerico que se graba en DCXP.DCXPESTD
--   PDTRVLRV       -> la etiqueta simbolica, igual que los otros estados
--   PDTRALTR = 7   -> codigo alterno, coincide con el valor
-- ---------------------------------------------------------------------
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
SELECT 1400, r.PRBRCDGO,
       'ANULADO: documento anulado, los registros siguen existiendo. TERMINAL, no se reprocesa',
       7, 'ANULADO', 7, 1
  FROM SCP.PRBR r
 WHERE r.PRBRALTR = 175;

COMMIT;


-- ---------------------------------------------------------------------
-- BLOQUE 2: CONTROL POSTERIOR
-- ---------------------------------------------------------------------

-- 2.1 Ahora tienen que ser 7 detalles, alternos 1..7.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRVLRN, d.PDTRVLRV, d.PDTRDSCR, d.PDTRESTD
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 175
 ORDER BY d.PDTRALTR;

-- 2.2 La fila nueva, aislada.
SELECT * FROM SCP.PDTR WHERE PDTRCDGO = 1400;
-- Debe devolver 1 fila, con PDTRVLRN = 7 y PDTRESTD = 1.

-- NOTA sobre secuencias: NO hay que sincronizar nada. SCP.SQ_PDTRCDGO no
-- existe, y la aplicacion no crea detalles de rubro (DetalleRubroRest solo
-- expone dos @GET). Ver §1bis de REGISTRO-RESERVAS-EQUIPOS.md, donde esa
-- regla quedo derogada tras verificarse contra la base.


-- ---------------------------------------------------------------------
-- BLOQUE 3: REVERSO  (comentado a proposito -- descomentar solo si hace falta)
-- ---------------------------------------------------------------------
-- ⚠️ Antes de revertir: si algun DCXP ya quedo en estado 7, borrar este
--    detalle deja esas filas apuntando a un estado sin etiqueta. Revisar
--    primero con el SELECT del bloque 0.5.
--
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO = 1400;
-- COMMIT;
