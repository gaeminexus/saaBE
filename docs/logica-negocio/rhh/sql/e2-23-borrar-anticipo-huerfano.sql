-- =====================================================================
-- e2-23 — Borrar un anticipo de empleado que quedo huerfano, para regenerarlo
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- 🔴 ESTE SCRIPT BORRA. Correr el BLOQUE 0 ENTERO, LEERLO, y recien despues
--    decidir. Los bloques de borrado tienen condiciones explicitas: si el
--    BLOQUE 0 no da lo esperado, NO seguir y avisar.
--
-- POR QUE EXISTE
--   El anticipo 1 no se puede anular: la aplicacion dice que tiene el pago
--   CONFIRMADO y pide revertirlo primero, pero ese pago NO aparece en
--   Tesoreria > Pagos por transferencia > Consulta y gestion. El usuario
--   informa que ese anticipo se genero mientras se estaba borrando la base,
--   asi que lo mas probable es que la fila del pago haya quedado colgada,
--   incompleta o apuntando a una empresa que ya no existe.
--
--   La decision es borrar el anticipo y volver a generarlo desde la pantalla.
--
-- ⚠️ CAMBIAR EL ID EN UN SOLO LUGAR
--   Todo el script usa el anticipo 1. Si fuera otro, reemplazar TODAS las
--   apariciones de "= 1" en las clausulas ANTECDGO.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — DIAGNOSTICO. Solo lectura. Correrlo entero y pegar el resultado.
-- =====================================================================

-- 0.1 El anticipo. ESPERADO: una fila, ANTEESTD = 2 (APROBADO).
--     Mira tambien PGTRCDGO (el pago) y DSRCCDGO (el descuento recurrente):
--     si DSRCCDGO NO es nulo, hay cuotas de descuento colgando -> ver 0.5.
SELECT '0.1 - el anticipo' AS control,
       a.ANTECDGO, a.MPLDCDGO AS empleado, a.ANTEFCHA AS fecha, a.ANTEVLOR AS valor,
       a.ANTEESTD AS estado, a.PGTRCDGO AS pago, a.DSRCCDGO AS descuento_recurrente
  FROM RHH.ANTE a
 WHERE a.ANTECDGO = 1;

-- 0.2 El pago al que apunta el anticipo. 🔴 ESTE ES EL DATO CLAVE.
--     Si devuelve 0 filas: la FK esta colgada, el pago ya no existe. Es lo que
--     explica que no aparezca en la pantalla y el caso es simple.
--     Si devuelve una fila, mirar PGTRASNT (asiento) y PGTRPJRQ (empresa).
SELECT '0.2 - el pago por la FK del anticipo' AS control,
       p.PGTRCDGO, p.PGTRESTD AS estado, p.PGTRVLOR AS valor, p.PGTRPJRQ AS empresa,
       p.PGTRORGN AS origen, p.PGTRIDOR AS id_origen,
       p.PGTRASNT AS asiento, p.PGTRDTCH AS detalle_cheque, p.PGTRLTPG AS lote,
       p.PGTROBSR AS observacion
  FROM CXP.PGTR p
 WHERE p.PGTRCDGO = (SELECT a.PGTRCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1);

-- 0.3 El pago por el otro lado del vinculo (PGTRORGN + PGTRIDOR).
--     Puede haber pagos que apunten al anticipo aunque el anticipo no apunte a
--     ellos. ESPERADO: la misma fila del 0.2, o ninguna.
SELECT '0.3 - pagos que dicen venir de este anticipo' AS control,
       p.PGTRCDGO, p.PGTRESTD AS estado, p.PGTRVLOR AS valor, p.PGTRPJRQ AS empresa,
       p.PGTRASNT AS asiento
  FROM CXP.PGTR p
 WHERE p.PGTRORGN = 'RHH_ANTICIPO_EMPLEADO' AND p.PGTRIDOR = 1;

-- 0.4 🔴 ¿El pago genero contabilidad? ESPERADO: 0 filas.
--     Si devuelve un asiento, PARAR Y AVISAR. Borrar el pago dejaria un asiento
--     sin origen, y eso descuadra el mayor contra el balance sin dar ningun
--     error -- exactamente el defecto que se corrigio hoy. La contabilidad se
--     reversa, no se borra.
SELECT '0.4 - asiento del pago' AS control,
       s.ASNTCDGO, s.ASNTNMAL AS numero_alterno, s.ASNTESTD AS estado_asiento
  FROM CNT.ASNT s
 WHERE s.ASNTCDGO IN (
        SELECT p.PGTRASNT FROM CXP.PGTR p
         WHERE p.PGTRASNT IS NOT NULL
           AND (p.PGTRCDGO = (SELECT a.PGTRCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1)
                OR (p.PGTRORGN = 'RHH_ANTICIPO_EMPLEADO' AND p.PGTRIDOR = 1)));

-- 0.5 ¿Hay descuento recurrente y cuotas colgando? ESPERADO: 0 filas.
--     Si las hay, el anticipo ya empezo a descontarse del rol y borrarlo es
--     otra conversacion: PARAR Y AVISAR.
SELECT '0.5 - cuotas de descuento' AS control,
       c.CTDSCDGO, c.DSRCCDGO
  FROM RHH.CTDS c
 WHERE c.DSRCCDGO = (SELECT a.DSRCCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1);

-- 0.6 Por que no se ve en la pantalla: compara la empresa del pago con las
--     empresas que existen. Si PGTRPJRQ es nulo o no esta en SCP.PJRQ, la
--     pantalla nunca lo iba a listar porque filtra por la empresa de la sesion.
SELECT '0.6 - empresa del pago' AS control,
       p.PGTRPJRQ AS empresa_del_pago,
       (SELECT COUNT(*) FROM SCP.PJRQ e WHERE e.PJRQCDGO = p.PGTRPJRQ) AS existe_esa_empresa
  FROM CXP.PGTR p
 WHERE p.PGTRCDGO = (SELECT a.PGTRCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1);


-- =====================================================================
-- BLOQUE 1 — BORRADO. ⛔ CONDICIONES OBLIGATORIAS ANTES DE CORRERLO
-- =====================================================================
--   ✅ El 0.4 devolvio 0 filas  (no hay asiento).
--   ✅ El 0.5 devolvio 0 filas  (no hay cuotas de descuento).
--   ❌ Si alguna de las dos devolvio algo, NO CORRER ESTE BLOQUE. Avisar.
--
-- Orden: primero se suelta la FK del anticipo al pago, despues se borra el
-- pago, y al final el anticipo. Al reves, Oracle rechaza por integridad
-- referencial (que en este caso es una ayuda, no un estorbo).

-- 1.1 Soltar el vinculo anticipo -> pago.
UPDATE RHH.ANTE SET PGTRCDGO = NULL WHERE ANTECDGO = 1;

-- 1.2 Borrar el pago, por los dos caminos por los que puede estar vinculado.
--     Si el 0.2 devolvio 0 filas esto no borra nada, y esta bien.
DELETE FROM CXP.PGTR
 WHERE PGTRORGN = 'RHH_ANTICIPO_EMPLEADO' AND PGTRIDOR = 1;

-- 1.3 Borrar el descuento recurrente si lo hubiera (el 0.5 ya confirmo que no
--     tiene cuotas). Si DSRCCDGO era nulo, esto no borra nada.
DELETE FROM RHH.DSRC
 WHERE DSRCCDGO = (SELECT a.DSRCCDGO FROM RHH.ANTE a WHERE a.ANTECDGO = 1);

-- 1.4 Borrar el anticipo.
DELETE FROM RHH.ANTE WHERE ANTECDGO = 1;

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL POSTERIOR. Correrlo SIEMPRE.
-- =====================================================================
-- ESPERADO: las tres cuentas en 0.
SELECT '2 - quedo limpio' AS control,
       (SELECT COUNT(*) FROM RHH.ANTE WHERE ANTECDGO = 1)                                AS anticipo,
       (SELECT COUNT(*) FROM CXP.PGTR
         WHERE PGTRORGN = 'RHH_ANTICIPO_EMPLEADO' AND PGTRIDOR = 1)                      AS pago,
       (SELECT COUNT(*) FROM RHH.ANTE WHERE ANTECDGO = 1 AND PGTRCDGO IS NOT NULL)       AS vinculo
  FROM DUAL;


-- =====================================================================
-- BLOQUE 3 — DESPUES DE BORRAR
-- =====================================================================
-- Volver a generar el anticipo desde la pantalla de RHH, normalmente.
--
-- ⚠️ Y ojo con esto, que es independiente de la basura del borrado de base:
--    aprobar() deja el anticipo en APROBADO mientras el pago nace CONFIRMADO,
--    asi que el anticipo nuevo va a quedar en la MISMA combinacion que hoy
--    impide anular. Si hace falta anularlo otra vez, hay que revertir el pago
--    primero desde Tesoreria > Pagos por transferencia > Consulta y gestion.
--    Eso es un defecto de la logica, no de estos datos, y esta pendiente de
--    decision del usuario.
-- =====================================================================
