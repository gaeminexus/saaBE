/*
 * SCRIPT: Rubros del proceso de carga de documentos CXP
 * Módulo: CXP - Cuentas por Pagar
 * Schema: SCP
 * Fecha:  2026-07-25
 *
 * Rubros creados:
 *   174 - CXP_RESULTADO_CARGA_TXT   → Resultado de cada línea al cargar el TXT SRI
 *   175 - CXP_ESTADO_DOCUMENTO_CXP  → Ciclo de vida del DocumentoCxp (tabla DCXP)
 *   176 - CXP_ESTADO_NOVEDAD        → Resolución de la novedad detectada en DCXP
 *   177 - CXP_ACCION_NOVEDAD        → Acción que envía el frontend para resolver novedad
 *
 * Tablas afectadas:
 *   SCP.PRBR  — cabecera del rubro  (modelo Java: Rubro)
 *   SCP.PDTR  — detalle del rubro   (modelo Java: DetalleRubro)
 *
 * Columnas PRBR: PRBRCDGO (PK), PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO
 * Columnas PDTR: PDTRCDGO (PK), PRBRCDGO (FK), PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD
 *   PDTRVLRN = valor numérico → código que usa el Java y que se almacena en BD
 *   PDTRVLRV = valor alfanumérico → label legible para el frontend (ej: "NUEVO", "MANTENER")
 *
 * IDs usados:
 *   PRBR: 173, 174, 175, 176
 *   PDTR: 734–738 (rubro 174), 739–744 (rubro 175), 745–747 (rubro 176), 748–749 (rubro 177)
 *
 * IMPORTANTE sobre tipos de columna:
 *   DCTX.DCTXRSLT fue cambiada de VARCHAR2(20) a NUMBER — almacena el PDTRVLRN del rubro 174.
 *   Ver ALTER en: rename-columnas-crtx-dcxp-dctx.sql
 * =============================================================================
 */

-- =============================================================================
-- RUBRO 174: CXP_RESULTADO_CARGA_TXT
-- Resultado que se almacena en DCTX.DCTXRSLT (NUMBER) al procesar cada línea del TXT.
-- =============================================================================
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (173, 'CXP - Resultado de línea en carga de TXT SRI', SYSDATE, 174, 0);

-- Detalle 174.1 — NUEVO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (734, 173, 'Documento nuevo: primera vez que aparece en el sistema', 1, 'NUEVO', 1, 1);

-- Detalle 174.2 — DUPLICADO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (735, 173, 'Documento duplicado: ya existía sin diferencias de valores ni fechas', 2, 'DUPLICADO', 2, 1);

-- Detalle 174.3 — NOVEDAD
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (736, 173, 'Novedad: el documento ya existía pero con diferencias en montos o fechas', 3, 'NOVEDAD', 3, 1);

-- Detalle 174.4 — IGNORADO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (737, 173, 'Ignorado: el RUC receptor del documento no coincide con el RUC de la empresa', 4, 'IGNORADO', 4, 1);

-- Detalle 174.5 — DESAPARECIDO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (738, 173, 'Desaparecido: documento activo del período que no apareció en esta nueva carga', 5, 'DESAPARECIDO', 5, 1);


-- =============================================================================
-- RUBRO 175: CXP_ESTADO_DOCUMENTO_CXP
-- Estado del ciclo de vida almacenado en DCXP.DCXPESTD (NUMBER).
-- =============================================================================
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (174, 'CXP - Estado del ciclo de vida del documento (DCXP)', SYSDATE, 175, 0);

-- Detalle 175.1 — LEIDO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (739, 174, 'LEIDO: documento leído del TXT, pendiente de cargar XML', 1, 'LEIDO', 1, 1);

-- Detalle 175.2 — XML_CARGADO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (740, 174, 'XML_CARGADO: XML validado y guardado en disco, pendiente de registrar en BD (estado transitorio)', 2, 'XML_CARGADO', 2, 1);

-- Detalle 175.3 — REGISTRADO_BD
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (741, 174, 'REGISTRADO_BD: registros creados en las tablas CXP destino (FacturaCompra, etc.)', 3, 'REGISTRADO_BD', 3, 1);

-- Detalle 175.4 — ERROR
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (742, 174, 'ERROR: falló algún paso del proceso (ver campo observacion del documento)', 4, 'ERROR', 4, 1);

-- Detalle 175.5 — NOVEDAD
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (743, 174, 'NOVEDAD: documento ya existía con valores distintos o desapareció en nueva carga, pendiente resolución', 5, 'NOVEDAD', 5, 1);

-- Detalle 175.6 — REVERTIDO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (744, 174, 'REVERTIDO: registros de BD eliminados (reversión aplicada manualmente)', 6, 'REVERTIDO', 6, 1);


-- =============================================================================
-- RUBRO 176: CXP_ESTADO_NOVEDAD
-- Resolución de la novedad almacenada en DCXP.DCXPENVD (NUMBER).
-- Solo aplica cuando estadoDocumento = 5 (NOVEDAD).
-- =============================================================================
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (175, 'CXP - Estado de resolución de novedad del documento (DCXP)', SYSDATE, 176, 0);

-- Detalle 176.1 — PENDIENTE
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (745, 175, 'PENDIENTE: novedad detectada, aún no resuelta por el usuario', 1, 'PENDIENTE', 1, 1);

-- Detalle 176.2 — REEMPLAZADO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (746, 175, 'REEMPLAZADO: usuario eligió subir nuevo XML y re-registrar el documento', 2, 'REEMPLAZADO', 2, 1);

-- Detalle 176.3 — MANTENIDO
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (747, 175, 'MANTENIDO: usuario eligió conservar el documento previo sin cambios', 3, 'MANTENIDO', 3, 1);


-- =============================================================================
-- RUBRO 177: CXP_ACCION_NOVEDAD
-- Acción que el FRONTEND envía en el body de POST /resolverNovedad/{id}.
-- El backend compara el campo "accion" del JSON (número) contra PDTRVLRN.
-- =============================================================================
INSERT INTO SCP.PRBR (PRBRCDGO, PRBRDSCR, PRBRFCHA, PRBRALTR, PRBRTPOO)
VALUES (176, 'CXP - Acción para resolver novedad (enviada por el frontend)', SYSDATE, 177, 0);

-- Detalle 177.1 — MANTENER
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (748, 176, 'MANTENER: conservar el documento previo sin ningún cambio', 1, 'MANTENER', 1, 1);

-- Detalle 177.2 — REEMPLAZAR
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRVLRN, PDTRVLRV, PDTRALTR, PDTRESTD)
VALUES (749, 176, 'REEMPLAZAR: revertir el registro anterior y procesar el nuevo XML enviado', 2, 'REEMPLAZAR', 2, 1);


COMMIT;

-- =============================================================================
-- VERIFICACIÓN
-- =============================================================================
SELECT r.PRBRALTR AS rubro_alterno, r.PRBRDSCR AS rubro_descripcion,
       d.PDTRALTR AS detalle_alterno, d.PDTRVLRN AS valor_numerico,
       d.PDTRVLRV AS valor_string,    d.PDTRDSCR AS detalle_descripcion
FROM   SCP.PRBR r
JOIN   SCP.PDTR d ON d.PRBRCDGO = r.PRBRCDGO
WHERE  r.PRBRALTR IN (174, 175, 176, 177)
ORDER  BY r.PRBRALTR, d.PDTRALTR;