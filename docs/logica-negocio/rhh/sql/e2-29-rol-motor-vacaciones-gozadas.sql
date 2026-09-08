-- =====================================================================
-- e2-29 — Rol de motor 36 para el concepto de vacaciones gozadas
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ⚠️ NO ES SOLO LECTURA. Inserta un detalle de rubro y actualiza conceptos.
--    Correr el BLOQUE 0 primero, LEERLO, y recien despues seguir.
--
-- 🔴 POR QUE EXISTE
--   Pedido del usuario: "en un mes que tomaron vacaciones varios empleados no me
--   esta dando de baja la provision de esas vacaciones... quiero que el sistema
--   debite la provision en ese mes, con asiento automatico".
--
--   Medido en produccion el 2026-09-08: RHH.PVNM tipo 3 (VACACIONES) acumula
--   164 filas por $6.840,03 y NADA las descarga. La provision se acumula todos
--   los meses y la unica baja que existe es el finiquito, asi que cuando un
--   empleado goza sus vacaciones el gasto se registra DOS VECES: una al
--   provisionar mes a mes y otra como sueldo del mes en que las tomo.
--
--   Al aprobar una solicitud, SolicitudVacacionesServiceImpl ya crea una
--   NovedadNomina con el concepto de codigo alterno 12 ("Vacaciones pagadas").
--   Ese renglon hoy va integro a gasto de sueldos. Con el rol de motor, la
--   contabilizacion del rol lo reconoce y debita la provision.
--
-- POR QUE UN ROL DE MOTOR Y NO EL CODIGO ALTERNO
--   ContabilizacionNominaServiceImpl clasifica los renglones SIEMPRE por
--   CPNMROLM, nunca por codigo alterno -- esta escrito en el javadoc del propio
--   metodo que habria que tocar. Decimos, fondos de reserva y valores no
--   pagados se resuelven todos asi. Clasificar vacaciones por alterno dejaria
--   DOS formas de resolver el mismo tipo de renglon en la misma clase, que es
--   como se construye el proximo defecto.
--
--   ⚠️ NO se crea un concepto nuevo. Es EL MISMO concepto con alterno 12 que ya
--   existe y que ya usa la aprobacion de vacaciones: solo gana su rol de motor.
--   Crear otro dejaria dos conceptos compitiendo y la novedad seguiria
--   apuntando al viejo.
--
-- ORDEN RESPECTO DEL WAR
--   Da igual: el codigo lleva una guarda: si una empresa no tiene concepto con
--   rol 36, el renglon sigue yendo a gasto como hoy, sin reventar. Asi que el
--   script puede correr antes o despues del WAR. Lo que NO pasa hasta que
--   corran los dos es la baja de la provision.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROLES ANTES. Correr esto solo, LEER, y pegar el resultado.
-- =====================================================================

-- 0.1 El rubro 221 (rol del concepto en el motor) y que el 36 este libre.
--     ESPERADO: una fila del rubro, y CERO filas del detalle 36.
SELECT '0.1a - rubro 221' AS control, PRBRCDGO AS rubro_pk, PRBRALTR AS alterno, PRBRDSCR
  FROM SCP.PRBR WHERE PRBRALTR = 221;
SELECT '0.1b - conflicto detalle 36' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR = 36;

-- 0.2 El codigo PDTR 1511 debe estar libre (sigue a 1509-1510 de los roles
--     34/35 del e2-26). ESPERADO: 0 filas.
SELECT '0.2 - PDTR 1511 libre' AS control, PDTRCDGO, PDTRDSCR
  FROM SCP.PDTR WHERE PDTRCDGO = 1511;

-- 0.3 🔴 EL CONTROL QUE DECIDE: el concepto de vacaciones pagadas por empresa.
--     ESPERADO: una fila por empresa que corre nomina (medido: UNA, la 1236),
--     con rol_actual VACIO.
--     - Si rol_actual ya trae un valor distinto de 36, PARAR Y AVISAR: ese
--       concepto ya esta clasificado como otra cosa y pisarlo cambiaria como lo
--       trata el motor hoy.
--     - Si NO aparece ninguna fila, PARAR: la empresa no tiene el concepto que
--       la aprobacion de vacaciones usa, y el problema es anterior a esto.
SELECT '0.3 - concepto de vacaciones pagadas' AS control,
       c.CPNMCDGO, c.PJRQCDGO AS empresa, c.CPNMNMBR AS nombre, c.CPNMABRV,
       c.CPNMALTR AS alterno, c.CPNMTPCN AS tipo, c.CPNMROLM AS rol_actual,
       c.CPNMESTD AS estado
  FROM RHH.CPNM c
 WHERE c.CPNMALTR = 12
 ORDER BY c.PJRQCDGO;

-- 0.4 Que ningun otro concepto tenga ya el rol 36. ESPERADO: 0 filas.
SELECT '0.4 - conceptos con rol 36' AS control, CPNMCDGO, PJRQCDGO, CPNMNMBR
  FROM RHH.CPNM WHERE CPNMROLM = 36;

-- 0.5 El saldo de provision de vacaciones, para contrastar despues.
--     Medido el 2026-09-08: 164 filas, 6.840,03.
SELECT '0.5 - provision de vacaciones acumulada' AS control,
       COUNT(*) AS filas, SUM(PVNMVLOR) AS saldo
  FROM RHH.PVNM WHERE PVNMTPPR = 3;


-- =====================================================================
-- BLOQUE 1 — EL DETALLE 36 DEL RUBRO 221
-- =====================================================================
-- Codigo fijo del bloque de este equipo (1500-1599), no una secuencia: es
-- determinista y el 0.2 ya verifico que esta libre. El rubro se ubica por su
-- ALTERNO, que no es su PK.
INSERT INTO SCP.PDTR (PDTRCDGO, PRBRCDGO, PDTRDSCR, PDTRALTR, PDTRESTD)
SELECT 1511, r.PRBRCDGO, 'VACACIONES GOZADAS', 36, 1
  FROM SCP.PRBR r WHERE r.PRBRALTR = 221;


-- =====================================================================
-- BLOQUE 2 — EL CONCEPTO GANA SU ROL DE MOTOR
-- =====================================================================
-- El mismo concepto que ya existe con alterno 12. NO se crea otro.
-- El AND CPNMROLM IS NULL es deliberado: si el 0.3 mostro un rol distinto en
-- alguna empresa, esta sentencia NO la toca y queda a la vista en el bloque 3
-- en vez de pisarse en silencio.
UPDATE RHH.CPNM
   SET CPNMROLM = 36
 WHERE CPNMALTR = 12
   AND CPNMROLM IS NULL;

COMMIT;


-- =====================================================================
-- BLOQUE 3 — CONTROLES DESPUES. Correrlos SIEMPRE.
-- =====================================================================

-- 3.1 El detalle del rubro. ESPERADO: una fila, alterno 36.
SELECT '3.1 - detalle 36' AS control, d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR = 36;

-- 3.2 🔴 El concepto por empresa. ESPERADO: rol_debe_ser_36 = 36 en TODAS las
--     filas que el 0.3 mostro con rol vacio.
--     Si alguna quedo sin el 36, es una de las que ya tenia otro rol: avisar,
--     NO forzarla.
SELECT '3.2 - concepto actualizado' AS control,
       c.CPNMCDGO, c.PJRQCDGO AS empresa, c.CPNMNMBR,
       c.CPNMALTR AS alterno, c.CPNMROLM AS rol_debe_ser_36, c.CPNMESTD AS estado
  FROM RHH.CPNM c
 WHERE c.CPNMALTR = 12
 ORDER BY c.PJRQCDGO;


-- =====================================================================
-- BLOQUE 4 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- UPDATE RHH.CPNM SET CPNMROLM = NULL WHERE CPNMALTR = 12 AND CPNMROLM = 36;
-- DELETE FROM SCP.PDTR WHERE PDTRCDGO = 1511;
-- COMMIT;
--
-- Revertir devuelve el comportamiento de hoy: el renglon de vacaciones vuelve a
-- ir integro a gasto de sueldos y la provision deja de descargarse.


-- =====================================================================
-- ⚠️ LO QUE ESTE SCRIPT NO ARREGLA
-- =====================================================================
-- 1. Los $6.840,03 ya acumulados. Esto corrige de aca en adelante; lo anterior
--    es un ajuste por unica vez que decide el usuario.
--
-- 2. 🔴 Que el saldo de RHH.PVNM NO LO CONSUME NADIE. Verificado el 2026-09-08:
--    no existe ningun metodo que descuente una fila de provision al pagarse, y
--    ni decimos, ni fondos de reserva, ni el finiquito lo hacen. El javadoc de
--    sumaValorByEmpleadoYTipo lo dice: "hoy nada la consume, asi que la suma
--    completa es el saldo".
--
--    O sea que el asiento va a quedar bien y el saldo de PVNM va a seguir
--    creciendo igual, para los TRES beneficios. Es un frente aparte, mas ancho
--    que esta tarea, y esta a decision del usuario.
-- =====================================================================
