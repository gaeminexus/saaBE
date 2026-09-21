-- =====================================================================================
-- TIPO DE APORTE "VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS" + SU CUENTA EN CRD.CTAP
-- FECHA: 2026-09-21 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- QUE ES: dos filas de configuracion. NO ES DDL: CRD.TPAP y CRD.CTAP ya existen y no se
-- tocan. Va DESPUES de 230_DDL_RECEPCION_VALORES_SEGURO.sql.
--
-- PARA QUE: el valor de sepelio que la aseguradora entrega al fondo entra a la cuenta del
-- participe como un tipo de aporte propio, y su cuenta contable (2.3.90.90.11) sale de
-- CRD.CTAP. Sin estas dos filas, POST /rest/rvsg/{id}/aprobar responde 409 nombrando el
-- tipo y la empresa: no adivina ninguna cuenta.
--
-- ⚠️ ESTO NO ES UN APORTE, CONTABLEMENTE: es un pasivo del fondo con terceros. Se modela
-- como tipo de aporte para reusar saldo, invariantes y la pantalla de devolucion. La
-- consecuencia a vigilar esta anotada en el diseno §4: ningun certificado ni reporte debe
-- sumar "todos los tipos" sin filtrar, o le mostraria al participe plata que no es suya.
--
-- USA LAS SECUENCIAS, no PK explicitas, justamente para no tener que sincronizarlas
-- despues (regla 8 de este equipo).
--
-- NO EJECUTAR SIN REVISAR. Correr por bloques. SQL PURO.
-- SOLO EN EL BACKEND: no se espeja a saaFE.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno no da lo esperado, PARAR
-- =====================================================================================

-- 0.1 La cuenta contable 2.3.90.90.11 tiene que existir. Esperado: 1 fila.
--     ⛔ SI DEVUELVE 0 FILAS, PARAR Y AVISAR A CONTABILIDAD: la cuenta del correo del
--     2026-09-21 no esta creada en el plan y no hay nada que mapear.
--     ⛔ SI DEVUELVE MAS DE UNA, PARAR: hay que elegir la de la empresa correcta y fijar
--     el PLNNCDGO a mano en los INSERT de abajo en vez de la subconsulta.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PJRQCDGO
FROM   CNT.PLNN n
WHERE  REPLACE(n.PLNNCNTA, '.', '') = '23909011'
ORDER  BY n.PJRQCDGO;

-- 0.2 La empresa sobre la que se configura. Esperado: 1 fila (1236, la de los aportes).
--     Si la instalacion usa otra, cambiar el 1236 en TODO este script.
SELECT j.PJRQCDGO, j.PJRQNMBR FROM SCP.PJRQ j WHERE j.PJRQCDGO = 1236;

-- 0.3 El tipo de aporte no puede existir ya. Esperado: 0 filas.
--     Si devuelve algo, este script ya se corrio: PARAR y no repetirlo.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPIDST
FROM   CRD.TPAP t
WHERE  UPPER(t.TPAPNMBR) LIKE '%SEGURO%BENEFICIARIO%';

-- 0.4 Foto de "antes": cuantos tipos y cuantas configuraciones hay hoy.
SELECT (SELECT COUNT(*) FROM CRD.TPAP) AS TIPOS_ANTES,
       (SELECT COUNT(*) FROM CRD.CTAP) AS CUENTAS_ANTES
FROM   DUAL;

-- 0.5 Las secuencias existen. Esperado: 2 filas.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME FROM ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD'
AND    s.SEQUENCE_NAME IN ('SQ_TPAPCDGO','SQ_CTAPCDGO');
-- ⚠️ Si SQ_TPAPCDGO no existe, CRD.TPAP se venia cargando con PK explicitas. En ese caso
--    reemplazar CRD.SQ_TPAPCDGO.NEXTVAL por (SELECT MAX(TPAPCDGO)+1 FROM CRD.TPAP) en el
--    bloque 1 y NO olvidar que entonces no hay secuencia que sincronizar.


-- =====================================================================================
-- 1. EL TIPO DE APORTE
-- =====================================================================================
--
-- TPAPIDST = 1 (vigente): el codigo exige que lo este, tanto para registrar el aporte
-- como para aprobar la recepcion.
-- TPAPCSBC: se deja NULL a proposito. Ese campo es la familia SBS (CE cesantia, JU
-- jubilacion, RE rendimiento...) y esto no pertenece a ninguna: no es un aporte del socio.
-- TPAPPRDP: NULL por ahora. Es el producto de pago de CXP, y todavia no existe uno contra
-- 2.3.90.90.11 — hace falta para la FASE 2 (el pago a los beneficiarios), no para recibir.

INSERT INTO CRD.TPAP (TPAPCDGO, TPAPNMBR, TPAPCSBC, TPAPIDST, TPAPPRDP)
VALUES (CRD.SQ_TPAPCDGO.NEXTVAL,
        'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS',
        NULL, 1, NULL);


-- =====================================================================================
-- 2. SU CUENTA CONTABLE EN CRD.CTAP
-- =====================================================================================
--
-- CTAPPLNP (pasivo) = 2.3.90.90.11. Es el HABER del asiento de recepcion.
--
-- CTAPPLNL (liquidacion) = LA MISMA CUENTA, y es deliberado:
--   1. La columna es NOT NULL (verificado en el CREATE TABLE de
--      94_CUENTAS_POR_TIPO_APORTE.sql:65), asi que hay que poner algo.
--   2. El camino de la recepcion NO la usa: el asiento de aprobar toma solo CTAPPLNP.
--   3. Si alguien devolviera este tipo por el camino normal de devolucion de aportes, la
--      reclasificacion saldria D 23909011 / H 23909011: neutra, cuadra, no descuadra nada.
--      La fase 2 va a omitir esa reclasificacion con una marca explicita, porque
--      contabilidad pidio DOS asientos en todo el ciclo y no tres.

INSERT INTO CRD.CTAP (CTAPCDGO, TPAPCDGO, PJRQCDGO, CTAPPLNP, CTAPPLNL, CTAPESTD)
VALUES (CRD.SQ_CTAPCDGO.NEXTVAL,
        (SELECT MAX(t.TPAPCDGO) FROM CRD.TPAP t
          WHERE t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS'),
        1236,
        (SELECT n.PLNNCDGO FROM CNT.PLNN n
          WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011' AND n.PJRQCDGO = 1236),
        (SELECT n.PLNNCDGO FROM CNT.PLNN n
          WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011' AND n.PJRQCDGO = 1236),
        1);

COMMIT;


-- =====================================================================================
-- 3. CONTROL POSTERIOR
-- =====================================================================================

-- 3.1 ⭐ EL DATO QUE HAY QUE ANOTAR: el codigo del tipo de aporte que quedo creado.
--     Es el que la pantalla de recepcion manda como idTipoAporte. Esperado: 1 fila.
SELECT t.TPAPCDGO AS ID_TIPO_APORTE_NUEVO, t.TPAPNMBR, t.TPAPIDST
FROM   CRD.TPAP t
WHERE  t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS';

-- 3.2 La configuracion contable quedo apuntando a la cuenta correcta.
--     Esperado: 1 fila, con las dos cuentas mostrando 2.3.90.90.11.
SELECT c.CTAPCDGO, c.TPAPCDGO, t.TPAPNMBR, c.PJRQCDGO,
       np.PLNNCNTA AS CUENTA_PASIVO,      np.PLNNNMBR AS NOMBRE_PASIVO,
       nl.PLNNCNTA AS CUENTA_LIQUIDACION, c.CTAPESTD
FROM   CRD.CTAP c
JOIN   CRD.TPAP t  ON t.TPAPCDGO  = c.TPAPCDGO
JOIN   CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
JOIN   CNT.PLNN nl ON nl.PLNNCDGO = c.CTAPPLNL
WHERE  t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS';

-- 3.3 Foto de "despues": tiene que haber exactamente un tipo y una cuenta mas que en 0.4.
SELECT (SELECT COUNT(*) FROM CRD.TPAP) AS TIPOS_DESPUES,
       (SELECT COUNT(*) FROM CRD.CTAP) AS CUENTAS_DESPUES
FROM   DUAL;

-- 3.4 Ningun otro tipo de aporte quedo tocado. Esperado: 0 filas.
SELECT c.CTAPCDGO, c.TPAPCDGO FROM CRD.CTAP c
WHERE  c.CTAPPLNP IN (SELECT n.PLNNCDGO FROM CNT.PLNN n
                       WHERE REPLACE(n.PLNNCNTA, '.', '') = '23909011')
AND    c.TPAPCDGO <> (SELECT MAX(t.TPAPCDGO) FROM CRD.TPAP t
                       WHERE t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS');


-- =====================================================================================
-- 4. REVERSO — COMENTADO A PROPOSITO
-- =====================================================================================
--
-- ⛔ Solo sirve si NO se registro todavia ninguna recepcion con este tipo. Si ya hay
--    filas en CRD.RVSG o movimientos en CRD.APRT de este tipo, NO borrar: dejaria
--    aportes apuntando a un tipo inexistente. En ese caso, para sacarlo de circulacion
--    alcanza con TPAPIDST = 0 (no vigente), que el codigo ya rechaza.
--
-- DELETE FROM CRD.CTAP
--  WHERE TPAPCDGO = (SELECT MAX(t.TPAPCDGO) FROM CRD.TPAP t
--                     WHERE t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS');
-- DELETE FROM CRD.TPAP
--  WHERE TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS';
-- COMMIT;
