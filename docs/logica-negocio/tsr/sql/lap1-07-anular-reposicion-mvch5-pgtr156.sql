-- =====================================================================
-- lap1-07  ·  Anular la reposicion de $180.25 de "Caja Chica oficinas"
--             (TSR.MVCH 5) y su pago (PGS.PGTR 156)
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- POR QUE ESTO VA POR SQL Y NO POR PANTALLA
-- El pago 156 esta en estado 0 (POR APROBAR). La bandeja de TSR > Procesos >
-- Aprobacion de pagos solo aprueba, no anula; y CXP > Pagos por transferencia
-- no lista los POR APROBAR (su filtro arranca en "Registrado") y su boton de
-- anular exige estado 1 o 2. El endpoint POST /rest/pgtr/anular/156 SI lo
-- acepta, pero ninguna pantalla lo llama para este estado.
--
-- POR QUE ES SEGURO HACERLO POR SQL EN ESTE CASO CONCRETO
-- El pago nunca se aprobo, asi que NO tiene asiento (la contabilidad de una
-- reposicion se genera al confirmar el pago, no al registrarlo), NO tiene
-- cheque y NO tiene movimiento bancario. Solo hay dos filas que tocar. El
-- CONTROL 2 verifica las tres cosas antes de que toques nada: si alguna sale
-- distinta, NO sigas y avisa.
--
-- ⚠️ Esta excepcion NO generaliza. Una reposicion con el pago ya confirmado
--    tiene asiento y movimiento bancario, y ahi el SQL a mano descuadra la
--    contabilidad: eso va por POST /rest/pgtr/revertirConfirmado/{id}.
--
-- ⚠️ EL SALDO DE LA CAJA QUEDA EN NEGATIVO: -112.12
--    Es correcto y es el punto. Los gastos activos (361.70) superan a la
--    apertura (249.58) en 112.12, y hoy eso esta tapado por esta reposicion
--    cuyo dinero todavia no salio del banco. Al anularla el hueco queda a la
--    vista. Se corrige registrando la reposicion nueva por el monto real,
--    desde la pantalla de caja chica.
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — foto de la caja antes. Saldo esperado: 68.13
-- ---------------------------------------------------------------------
SELECT c.CJCHCDGO AS ID_CAJA,
       c.CJCHNMBR AS NOMBRE,
       c.CJCHMNTO AS FONDO,
       NVL(SUM(CASE WHEN m.MVCHTPOO IN (1,3,4) THEN m.MVCHVLOR
                    WHEN m.MVCHTPOO IN (2,5)   THEN -m.MVCHVLOR
                    ELSE 0 END), 0) AS SALDO_ANTES
  FROM TSR.CJCH c
  LEFT JOIN TSR.MVCH m ON m.CJCHCDGO = c.CJCHCDGO AND m.MVCHESTD = 1
 WHERE c.CJCHCDGO = 1
 GROUP BY c.CJCHCDGO, c.CJCHNMBR, c.CJCHMNTO;


-- ---------------------------------------------------------------------
-- CONTROL 2 — las tres condiciones que hacen seguro el SQL a mano.
--    Se espera EXACTAMENTE: SIN_ASIENTO / SIN_CHEQUE / SIN_CIERRE / POR_APROBAR / ACTIVO
--    Si CUALQUIERA sale distinto, DETENTE y avisa antes de seguir.
-- ---------------------------------------------------------------------
SELECT m.MVCHCDGO AS ID_MOVIMIENTO,
       m.MVCHVLOR AS VALOR,
       CASE WHEN m.ASNTCDGO IS NULL THEN 'SIN_ASIENTO' ELSE 'TIENE ASIENTO ' || m.ASNTCDGO END AS CHK_ASIENTO,
       CASE WHEN m.CRCHCDGO IS NULL THEN 'SIN_CIERRE'  ELSE 'EN CIERRE '   || m.CRCHCDGO END AS CHK_CIERRE,
       CASE WHEN m.MVCHESTD = 1     THEN 'ACTIVO'      ELSE 'ESTADO ' || NVL(TO_CHAR(m.MVCHESTD),'NULL') END AS CHK_MOVIMIENTO,
       CASE WHEN p.PGTRDTCH IS NULL THEN 'SIN_CHEQUE'  ELSE 'TIENE CHEQUE ' || p.PGTRDTCH END AS CHK_CHEQUE,
       CASE WHEN p.PGTRASNT IS NULL THEN 'PAGO_SIN_ASIENTO' ELSE 'PAGO CON ASIENTO ' || p.PGTRASNT END AS CHK_PAGO_ASIENTO,
       CASE WHEN p.PGTRESTD = 0     THEN 'POR_APROBAR' ELSE 'ESTADO ' || NVL(TO_CHAR(p.PGTRESTD),'NULL') END AS CHK_PAGO,
       p.PGTRORGN AS ORIGEN,
       p.PGTRIDOR AS APUNTA_A_MOVIMIENTO
  FROM TSR.MVCH m
  LEFT JOIN PGS.PGTR p ON p.PGTRCDGO = m.PGTRCDGO
 WHERE m.MVCHCDGO = 5;


-- ---------------------------------------------------------------------
-- CONTROL 3 — que no haya un movimiento bancario colgando del pago.
--             Se esperan 0 filas.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS MOVIMIENTOS_BANCO
  FROM TSR.MVCB mb
 WHERE mb.ASNTCDGO IN (SELECT p.PGTRASNT FROM PGS.PGTR p WHERE p.PGTRCDGO = 156 AND p.PGTRASNT IS NOT NULL);


-- =====================================================================
-- CORRECCION — dos filas, en este orden.
-- Reproduce exactamente lo que haria PagoProgramadoServiceImpl.anularPago:
-- estado ANULADO + motivo en el pago, y el movimiento anulado con el motivo
-- prefijado "PAGO ANULADO: " (anularMovimientoCajaChicaSiAplica:2968-2971).
-- =====================================================================

-- 1) El pago: estado 5 = ANULADO
UPDATE PGS.PGTR
   SET PGTRESTD = 5,
       PGTRMTVO = 'REPOSICION MAL REGISTRADA: se vuelve a registrar por el monto correcto'
 WHERE PGTRCDGO = 156
   AND PGTRESTD = 0;

-- 2) El movimiento de caja chica: estado 2 = ANULADO
UPDATE TSR.MVCH
   SET MVCHESTD = 2,
       MVCHMTAN = 'PAGO ANULADO: REPOSICION MAL REGISTRADA: se vuelve a registrar por el monto correcto',
       ASNTCDGO = NULL
 WHERE MVCHCDGO = 5
   AND MVCHESTD = 1;

COMMIT;


-- ---------------------------------------------------------------------
-- CONTROL 4 — despues: pago ANULADO (5) y movimiento ANULADO (2).
-- ---------------------------------------------------------------------
SELECT m.MVCHCDGO AS ID_MOVIMIENTO,
       m.MVCHESTD AS ESTADO_MOVIMIENTO,
       m.MVCHMTAN AS MOTIVO_MOVIMIENTO,
       p.PGTRCDGO AS ID_PAGO,
       p.PGTRESTD AS ESTADO_PAGO,
       p.PGTRMTVO AS MOTIVO_PAGO
  FROM TSR.MVCH m
  LEFT JOIN PGS.PGTR p ON p.PGTRCDGO = m.PGTRCDGO
 WHERE m.MVCHCDGO = 5;


-- ---------------------------------------------------------------------
-- CONTROL 5 — saldo despues. Esperado: -112.12
--             Si da otra cosa, avisa antes de registrar la reposicion nueva.
-- ---------------------------------------------------------------------
SELECT c.CJCHCDGO AS ID_CAJA,
       c.CJCHNMBR AS NOMBRE,
       c.CJCHMNTO AS FONDO,
       NVL(SUM(CASE WHEN m.MVCHTPOO IN (1,3,4) THEN m.MVCHVLOR
                    WHEN m.MVCHTPOO IN (2,5)   THEN -m.MVCHVLOR
                    ELSE 0 END), 0) AS SALDO_DESPUES
  FROM TSR.CJCH c
  LEFT JOIN TSR.MVCH m ON m.CJCHCDGO = c.CJCHCDGO AND m.MVCHESTD = 1
 WHERE c.CJCHCDGO = 1
 GROUP BY c.CJCHCDGO, c.CJCHNMBR, c.CJCHMNTO;


-- =====================================================================
-- DESPUES DE ESTO: registrar la reposicion nueva DESDE LA PANTALLA
-- =====================================================================
-- Caja chica > Reposicion. No por SQL: la pantalla crea el movimiento Y el
-- pago programado enlazados, y el pago es el que despues genera el asiento
-- al confirmarse. Un movimiento insertado a mano queda sin pago y sin
-- contabilidad, que es justo el descuadre que este script evita.
--
-- ⚠️ Con el saldo en -112.12 y fondo 600, la pantalla admite reponer hasta
--    712.12 (valida contra fondo - saldo). Si repones solo 180.25 la caja
--    queda en 68.13 otra vez: para volver al fondo completo hacen falta
--    712.12, y esa diferencia es la que hay que justificar con el custodio.
--
-- ⚠️ Al registrar, ELEGI LA CUENTA BANCARIA DE ORIGEN. Sin cuenta el pago
--    vuelve a nacer POR_APROBAR y queda otra vez sin pantalla que lo anule
--    — que es exactamente como se llego hasta aca.


-- =====================================================================
-- REVERSO (comentado a proposito — no correr salvo que haga falta)
-- =====================================================================
-- Devuelve el pago a POR_APROBAR y el movimiento a ACTIVO. Solo tiene
-- sentido si todavia NO registraste la reposicion nueva: con las dos
-- activas el saldo sumaria las dos reposiciones.
--
-- UPDATE PGS.PGTR SET PGTRESTD = 0, PGTRMTVO = NULL WHERE PGTRCDGO = 156;
-- UPDATE TSR.MVCH SET MVCHESTD = 1, MVCHMTAN = NULL WHERE MVCHCDGO = 5;
-- COMMIT;
