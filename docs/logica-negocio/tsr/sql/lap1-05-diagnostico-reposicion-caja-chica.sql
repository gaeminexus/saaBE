-- =====================================================================
-- lap1-05  ·  Diagnostico: identificar una reposicion de caja chica
--             y el pago/asiento de los que cuelga
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- SOLO CONSULTAS. Este script no modifica nada, a proposito.
--
-- Las reposiciones NO se borran con SQL. Una reposicion arrastra un pago
-- programado (PGS.PGTR), un asiento contable (CNT.ASNT) y, si se pago con
-- cheque, un movimiento bancario. Borrar la fila de TSR.MVCH deja los tres
-- vivos y descuadra la contabilidad contra la caja.
--
-- La forma correcta esta al final del script.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 1. Movimientos de una caja: la reposicion que buscas esta aca.
--    MVCHTPOO (rubro 232): 1=Apertura 2=Gasto 3=Reposicion 4=Ajuste+ 5=Ajuste-
--    MVCHESTD: 1=Activo 2=Anulado
--    Reemplaza 1 por el codigo de tu caja (CJCHCDGO).
-- ---------------------------------------------------------------------
SELECT m.MVCHCDGO AS ID_MOVIMIENTO,
       m.MVCHTPOO AS TIPO,
       CASE m.MVCHTPOO
            WHEN 1 THEN 'APERTURA'  WHEN 2 THEN 'GASTO'    WHEN 3 THEN 'REPOSICION'
            WHEN 4 THEN 'AJUSTE +'  WHEN 5 THEN 'AJUSTE -' ELSE 'DESCONOCIDO' END AS TIPO_TEXTO,
       m.MVCHFCHA AS FECHA,
       m.MVCHVLOR AS VALOR,
       m.MVCHDSCR AS DESCRIPCION,
       m.MVCHESTD AS ESTADO,
       CASE m.MVCHESTD WHEN 1 THEN 'ACTIVO' WHEN 2 THEN 'ANULADO' ELSE 'SIN ESTADO' END AS ESTADO_TEXTO,
       m.PGTRCDGO AS ID_PAGO,
       m.ASNTCDGO AS ID_ASIENTO,
       m.CRCHCDGO AS ID_CIERRE,
       m.MVCHMTAN AS MOTIVO_ANULACION
  FROM TSR.MVCH m
 WHERE m.CJCHCDGO = 1
 ORDER BY m.MVCHFCHA DESC, m.MVCHCDGO DESC;


-- ---------------------------------------------------------------------
-- 2. La reposicion con su pago y su asiento, todo junto.
--    Reemplaza 0 por el ID_MOVIMIENTO que ubicaste arriba.
--    PGTRESTD: 0=Por aprobar 1=Registrado 2=En archivo 3=Confirmado
--              4=Rechazado 5=Anulado
--    ASNTESTD: ver rubro de estado de asiento (1 = activo).
-- ---------------------------------------------------------------------
SELECT m.MVCHCDGO AS ID_MOVIMIENTO,
       m.MVCHVLOR AS VALOR_MOVIMIENTO,
       m.MVCHESTD AS ESTADO_MOVIMIENTO,
       p.PGTRCDGO AS ID_PAGO,
       p.PGTRESTD AS ESTADO_PAGO,
       CASE p.PGTRESTD
            WHEN 0 THEN 'POR APROBAR' WHEN 1 THEN 'REGISTRADO' WHEN 2 THEN 'EN ARCHIVO'
            WHEN 3 THEN 'CONFIRMADO'  WHEN 4 THEN 'RECHAZADO'  WHEN 5 THEN 'ANULADO'
            ELSE 'DESCONOCIDO' END AS ESTADO_PAGO_TEXTO,
       p.PGTRORGN AS ORIGEN_PAGO,
       p.PGTRIDOR AS ID_ORIGEN,
       p.PGTRVLOR AS VALOR_PAGO,
       p.PGTRFPAG AS FORMA_PAGO,
       a.ASNTCDGO AS ID_ASIENTO,
       a.ASNTNMRO AS NUMERO_ASIENTO,
       a.ASNTESTD AS ESTADO_ASIENTO,
       a.ASNTOBSR AS OBSERVACION_ASIENTO
  FROM TSR.MVCH m
  LEFT JOIN PGS.PGTR p ON p.PGTRCDGO = m.PGTRCDGO
  LEFT JOIN CNT.ASNT a ON a.ASNTCDGO = m.ASNTCDGO
 WHERE m.MVCHCDGO = 0;


-- ---------------------------------------------------------------------
-- 3. Control cruzado: el pago tambien apunta al movimiento.
--    PGTRORGN = 'TSR_CAJA_CHICA' y PGTRIDOR = MVCHCDGO.
--    Sirve para detectar un vinculo roto en cualquiera de los dos lados.
-- ---------------------------------------------------------------------
SELECT p.PGTRCDGO AS ID_PAGO,
       p.PGTRESTD AS ESTADO_PAGO,
       p.PGTRIDOR AS ID_MOVIMIENTO_SEGUN_PAGO,
       m.MVCHCDGO AS ID_MOVIMIENTO_REAL,
       m.PGTRCDGO AS ID_PAGO_SEGUN_MOVIMIENTO,
       CASE WHEN m.MVCHCDGO IS NULL THEN 'PAGO SIN MOVIMIENTO'
            WHEN m.PGTRCDGO IS NULL THEN 'MOVIMIENTO NO APUNTA AL PAGO'
            WHEN m.PGTRCDGO <> p.PGTRCDGO THEN 'APUNTAN A PAGOS DISTINTOS'
            ELSE 'OK' END AS DIAGNOSTICO
  FROM PGS.PGTR p
  LEFT JOIN TSR.MVCH m ON m.MVCHCDGO = p.PGTRIDOR
 WHERE p.PGTRORGN = 'TSR_CAJA_CHICA'
 ORDER BY p.PGTRCDGO DESC;


-- ---------------------------------------------------------------------
-- 4. Saldo de la caja, calculado como lo hace el backend
--    (CajaChicaServiceImpl.calcularSaldo): suma apertura + reposicion +
--    ajuste+, resta gasto + ajuste-, SOLO movimientos activos.
--    Reemplaza 1 por el codigo de tu caja.
-- ---------------------------------------------------------------------
SELECT c.CJCHCDGO AS ID_CAJA,
       c.CJCHNMBR AS NOMBRE,
       c.CJCHMNTO AS FONDO,
       NVL(SUM(CASE WHEN m.MVCHTPOO IN (1,3,4) THEN m.MVCHVLOR
                    WHEN m.MVCHTPOO IN (2,5)   THEN -m.MVCHVLOR
                    ELSE 0 END), 0) AS SALDO
  FROM TSR.CJCH c
  LEFT JOIN TSR.MVCH m ON m.CJCHCDGO = c.CJCHCDGO AND m.MVCHESTD = 1
 WHERE c.CJCHCDGO = 1
 GROUP BY c.CJCHCDGO, c.CJCHNMBR, c.CJCHMNTO;


-- =====================================================================
-- COMO SE ANULA UNA REPOSICION (no es con SQL)
-- =====================================================================
--
-- El backend RECHAZA a proposito anular una reposicion por la via del
-- gasto. MovimientoCajaChicaServiceImpl.anularGasto:349-355 lanza:
--   «El movimiento N no es un gasto. Reverse el pago programado N° X
--    (pgtr/revertirConfirmado).»
--
-- Segun el ESTADO_PAGO que devolvio la consulta 2:
--
--   PGTRESTD = 3 (CONFIRMADO) → POST /SaaBE/rest/pgtr/revertirConfirmado/{ID_PAGO}
--        cuerpo: {"motivo": "...", "idUsuario": N}
--        Anula el asiento, anula el movimiento bancario, desvincula el
--        asiento del pago y marca el MVCH como ANULADO (2)
--        — PagoProgramadoServiceImpl.revertirContabilidadOrigenExterno:2864.
--
--   PGTRESTD = 0, 1 o 2 (aun no confirmado) → anular el pago
--        (PagoProgramadoServiceImpl:1889 tambien anula el MVCH).
--        No hay asiento todavia: la contabilidad de la reposicion se
--        genera al confirmar, no al registrar.
--
--   PGTRESTD = 4 o 5, o ID_PAGO nulo → el pago ya esta deshecho o la
--        reposicion nunca lo tuvo. Verifica el ESTADO_MOVIMIENTO: si
--        quedo en 1 (ACTIVO) hay un vinculo roto — avisa antes de tocar
--        nada.
--
-- ⛔ NO borres la fila de TSR.MVCH. El saldo de la caja se calcula
--    sumando movimientos activos, asi que borrarla cuadra la caja y deja
--    huerfanos el pago, el asiento y el movimiento bancario: la
--    contabilidad queda descuadrada contra la caja sin ningun aviso.
--    Anular (MVCHESTD = 2) es lo correcto, y el endpoint lo hace junto
--    con todo lo demas.
