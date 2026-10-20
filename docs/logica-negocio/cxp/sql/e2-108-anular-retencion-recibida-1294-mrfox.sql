-- =====================================================================================
-- e2-108 — Anular la retención recibida 1294 (MRFOX CIA LTDA, 001-002-000000710, 05/10/2026, 218,84).
-- Equipo omen-saa-2 · 2026-10-20. BLOQUE 1 SOLO LECTURA; BLOQUE 2 CORRIGE.
--
-- Situación: a la retención ya se le anuló la contabilidad («CONTABILIDAD ANULADA: EL CLIENTE VA A PR…») y en la
-- última carga del SRI ya no apareció (el cliente la anuló allá) → quedó en NOVEDAD «DESAPARECIDO_EN_CARGA».
-- La bandeja solo ofrece Mantener (la devolvería a XML CARGADO, lista para volver a registrarse) o Reemplazar (pide
-- un XML nuevo). El backend SÍ tiene «marcar documento anulado» (POST /anularDocumento, estado 7, terminal), pero la
-- pantalla no tiene el botón. Mientras se agrega, este script hace lo mismo a mano, y además inactiva el registro de
-- la retención (PGS.RCV2/DRC2 o PGS.RTCM/DRCM), que el endpoint no toca: si queda activo, el ATS de OCTUBRE la cuenta
-- como retención que el cliente nos hizo (GeneradorAtsServiceImpl lee RCV2/DRC2 con estado = 1).
-- Columnas copiadas de DocumentoCxp (PGS.DCXP), RetencionCompraV2 (PGS.RCV2), DetalleRetencionCompraV2 (PGS.DRC2:
-- RETENCIONV2), RetencionCompra (PGS.RTCM), DetalleRetencionCompra (PGS.DRCM: RETENCION). Estado: 1 activo, 0 inactivo.
-- ASNTESTD: 2 = ANULADO (EstadoAsiento).
-- =====================================================================================

-- BLOQUE 1 — Control ANTES. ESPERADO: DCXPESTD = 5 (NOVEDAD), DCXPTBTD = 'RETENCION_COMPRA_V2' o 'RETENCION_COMPRA',
-- DCXPIDBD con valor; el asiento de la retención con estado anulado (o sin asiento). Si el asiento sale ACTIVO,
-- NO correr el bloque 2: pásame el resultado (falta anular la contabilidad primero).
SELECT 'BLOQUE 1a - bandeja' AS bloque, d.DCXPCDGO, d.DCXPESTD, d.DCXPTBTD, d.DCXPIDBD, d.DCXPSRCM, d.DCXPIMTT,
       d.DCXPNVDD, d.DCXPOBSR
  FROM PGS.DCXP d
 WHERE d.DCXPCDGO = 1294;

SELECT 'BLOQUE 1b - retencion v2' AS bloque, r.ID, r.NUMERO, r.FECHA, r.ESTADO, r.ASIENTO, a.ASNTESTD AS estado_asiento,
       (SELECT COUNT(*) FROM PGS.DRC2 x WHERE x.RETENCIONV2 = r.ID AND x.ESTADO = 1) AS detalles_activos
  FROM PGS.RCV2 r
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = r.ASIENTO
 WHERE r.ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA_V2');

SELECT 'BLOQUE 1c - retencion v1' AS bloque, r.ID, r.NUMERO, r.FECHA, r.ESTADO, r.ASIENTO, a.ASNTESTD AS estado_asiento,
       (SELECT COUNT(*) FROM PGS.DRCM x WHERE x.RETENCION = r.ID AND x.ESTADO = 1) AS detalles_activos
  FROM PGS.RTCM r
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = r.ASIENTO
 WHERE r.ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA');

-- =====================================================================================
-- BLOQUE 2 — CORRECCIÓN. Correr SOLO si el bloque 1 salió como se espera.
-- =====================================================================================

-- 2a. Retención (cabecera y detalle) a inactivo. Solo una de las dos parejas afecta filas, según DCXPTBTD.
UPDATE PGS.DRC2 SET ESTADO = 0
 WHERE ESTADO = 1
   AND RETENCIONV2 = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA_V2');
UPDATE PGS.RCV2 SET ESTADO = 0
 WHERE ESTADO = 1
   AND ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA_V2');
UPDATE PGS.DRCM SET ESTADO = 0
 WHERE ESTADO = 1
   AND RETENCION = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA');
UPDATE PGS.RTCM SET ESTADO = 0
 WHERE ESTADO = 1
   AND ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA');

-- 2b. Documento de la bandeja a ANULADO (7), igual que el endpoint /anularDocumento. ESPERADO: 1 fila.
UPDATE PGS.DCXP
   SET DCXPESTD = 7,
       DCXPOBSR = SUBSTR('ANULADO: retencion anulada por el cliente ante el SRI (no aparecio en la carga). '
                         || 'e2-108 2026-10-20 | antes: ' || NVL(DCXPOBSR, ''), 1, 2000)
 WHERE DCXPCDGO = 1294
   AND DCXPESTD = 5;

-- Control DESPUÉS. ESPERADO: DCXPESTD = 7; la retención (1b o 1c) con ESTADO = 0 y detalles_activos = 0.
-- Volver a correr el BLOQUE 1.
-- Si todo cuadra: COMMIT;   Si no: ROLLBACK;

-- REVERSO (comentado): devuelve la bandeja a NOVEDAD y reactiva la retención.
-- UPDATE PGS.DCXP SET DCXPESTD = 5 WHERE DCXPCDGO = 1294 AND DCXPOBSR LIKE 'ANULADO: retencion anulada por el cliente%e2-108%';
-- UPDATE PGS.RCV2 SET ESTADO = 1 WHERE ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA_V2');
-- UPDATE PGS.DRC2 SET ESTADO = 1 WHERE RETENCIONV2 = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA_V2');
-- UPDATE PGS.RTCM SET ESTADO = 1 WHERE ID = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA');
-- UPDATE PGS.DRCM SET ESTADO = 1 WHERE RETENCION = (SELECT DCXPIDBD FROM PGS.DCXP WHERE DCXPCDGO = 1294 AND DCXPTBTD = 'RETENCION_COMPRA');
-- COMMIT;
-- (Ojo: el reverso de DRC2/DRCM reactiva todos los detalles; si alguno estaba inactivo antes, anotarlo del bloque 1.)
