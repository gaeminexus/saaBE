-- =====================================================================
-- lap1-04  ·  TSR.CJCH — recuperar el estado borrado al editar la caja
-- Equipo lap-saa-1 · 2026-09-07
-- =====================================================================
--
-- QUE PASO
-- La pantalla de parametrizacion de caja chica (PUT /rest/cjch) manda un
-- payload que NO incluye CJCHESTD, CJCHFCRG, CJCHUSAR ni CJCHUSCS.
-- EntityDaoImpl.save() hace em.merge() con el objeto tal cual llego del
-- JSON, sin releer la fila: cada campo ausente se graba como NULL.
--
-- CajaChicaServiceImpl.saveSingle solo repone estado y fecha de registro
-- cuando codigo == null (alta). En edicion no repone nada.
--
-- CONSECUENCIA
-- La caja queda con CJCHESTD = NULL. Las pantallas operativas de gastos,
-- reposicion y cierre piden GET /cjch/activas/{idEmpresa}, que filtra
-- "c.estado = 1": una caja con estado NULL desaparece de las tres, y del
-- semaforo de saldos. Sigue apareciendo en parametrizacion porque esa
-- pantalla usa getAll y filtra solo por empresa.
--
-- Este script NO arregla el defecto — repone el dato perdido. La
-- correccion de codigo va aparte.
-- =====================================================================


-- ---------------------------------------------------------------------
-- CONTROL 1 — cajas con estado perdido.  MIRA ESTA LISTA ANTES DE SEGUIR.
-- ---------------------------------------------------------------------
-- Si alguna de estas cajas estaba deliberadamente INACTIVA (estado 2)
-- antes de editarla, NO corras el UPDATE de golpe: el bloque de abajo
-- las repone TODAS como activas. En ese caso usa el UPDATE puntual del
-- final, caja por caja.
SELECT c.CJCHCDGO   AS ID_CAJA,
       c.CJCHNMBR   AS NOMBRE,
       c.PJRQCDGO   AS ID_EMPRESA,
       c.CJCHESTD   AS ESTADO,
       c.CJCHMNTO   AS FONDO,
       c.CJCHMXGS   AS MAX_GASTO,
       c.CJCHPRAL   AS PCT_ALERTA,
       c.CJCHFCRG   AS FECHA_REGISTRO,
       c.CJCHUSAR   AS USUARIO_REGISTRO,
       c.CJCHUSCS   AS CUSTODIO
  FROM TSR.CJCH c
 WHERE c.CJCHESTD IS NULL
 ORDER BY c.PJRQCDGO, c.CJCHCDGO;


-- ---------------------------------------------------------------------
-- CONTROL 2 — foto completa de todas las cajas, para comparar despues.
-- ---------------------------------------------------------------------
SELECT c.CJCHCDGO AS ID_CAJA,
       c.CJCHNMBR AS NOMBRE,
       c.PJRQCDGO AS ID_EMPRESA,
       c.CJCHESTD AS ESTADO,
       c.CJCHMNTO AS FONDO,
       (SELECT COUNT(*) FROM TSR.MVCH m WHERE m.CJCHCDGO = c.CJCHCDGO) AS MOVIMIENTOS
  FROM TSR.CJCH c
 ORDER BY c.PJRQCDGO, c.CJCHCDGO;


-- ---------------------------------------------------------------------
-- CORRECCION — repone estado ACTIVA (1) en las cajas que quedaron NULL.
-- ---------------------------------------------------------------------
-- Solo toca filas con CJCHESTD IS NULL: una caja inactivada a proposito
-- tiene estado 2, no NULL, y este UPDATE no la alcanza.
UPDATE TSR.CJCH
   SET CJCHESTD = 1
 WHERE CJCHESTD IS NULL;

COMMIT;


-- ---------------------------------------------------------------------
-- CONTROL 3 — despues: no debe quedar ninguna fila.
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS CAJAS_SIN_ESTADO
  FROM TSR.CJCH
 WHERE CJCHESTD IS NULL;


-- ---------------------------------------------------------------------
-- CONTROL 4 — lo que va a ver la pantalla de gastos.
--             Reemplaza 1 por el codigo de tu empresa (PJRQCDGO).
--             Es la misma consulta que CajaChicaDaoServiceImpl.selectByEmpresaEstado.
-- ---------------------------------------------------------------------
SELECT c.CJCHCDGO AS ID_CAJA,
       c.CJCHNMBR AS NOMBRE,
       c.CJCHMNTO AS FONDO,
       c.CJCHESTD AS ESTADO
  FROM TSR.CJCH c
 WHERE c.PJRQCDGO = 1
   AND c.CJCHESTD = 1
 ORDER BY c.CJCHCDGO;


-- =====================================================================
-- LO QUE ESTE SCRIPT NO RECUPERA
-- =====================================================================
-- CJCHFCRG (fecha de registro), CJCHUSAR (usuario que registro) y
-- CJCHUSCS (custodio) tambien se borraron en la misma edicion y NO se
-- pueden reconstruir: no hay historial de la tabla. Hoy ninguna consulta
-- filtra por ellos, asi que no rompen nada — quedan como hueco de
-- auditoria. Si quieres reponer la fecha de registro con la de hoy,
-- descomenta:
--
-- UPDATE TSR.CJCH SET CJCHFCRG = SYSDATE WHERE CJCHFCRG IS NULL;
-- COMMIT;


-- =====================================================================
-- UPDATE PUNTUAL (si el CONTROL 1 mostro alguna caja que debia quedar
-- inactiva). Descomenta y pon los codigos a mano.
-- =====================================================================
-- UPDATE TSR.CJCH SET CJCHESTD = 1 WHERE CJCHCDGO IN (/* ids activas */);
-- UPDATE TSR.CJCH SET CJCHESTD = 2 WHERE CJCHCDGO IN (/* ids inactivas */);
-- COMMIT;


-- =====================================================================
-- REVERSO (comentado a proposito — no correr salvo que haga falta)
-- =====================================================================
-- Devuelve a NULL el estado de las cajas que este script activo. Solo
-- tiene sentido inmediatamente despues y con la lista del CONTROL 1 a
-- mano; sin esa lista no hay forma de distinguirlas de las que ya
-- estaban activas.
--
-- UPDATE TSR.CJCH SET CJCHESTD = NULL WHERE CJCHCDGO IN (/* ids del CONTROL 1 */);
-- COMMIT;
