-- =====================================================================================
-- DIAGNOSTICO — las dos consultas que no dieron lo esperado en el script 88
-- FECHA: 2026-08-31
--
-- SOLO LECTURA. No modifica nada. Son cinco consultas cortas.
--
-- Contexto: el 88 dio bien todo el DDL (tablas, columnas, constraints, indices), pero
--   - la consulta 3 no trajo el alterno 7 JUBILACION (script 81), y
--   - la consulta 4.1 (secuencias) volvio VACIA, lo cual es inconcluso, no un OK.
-- =====================================================================================


-- =====================================================================================
-- A. LAS SECUENCIAS — ¿no existen, o no las ve el usuario conectado?
-- =====================================================================================

-- A.1 Con quien estoy conectado. Sirve para interpretar todo lo de abajo: ALL_SEQUENCES
--     muestra solo lo que este usuario tiene privilegio de ver.
SELECT USER AS USUARIO_CONECTADO FROM DUAL;

-- A.2 Sin filtrar por owner. Si aparecen con OWNER distinto de 'SCP', el problema es que
--     viven en otro schema y el mapeo JPA (@SequenceGenerator sequenceName="SCP.SQ_PDTRCDGO")
--     apunta al lugar equivocado. Si no aparece NADA, o no existen o no se ven.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER, s.INCREMENT_BY, s.CACHE_SIZE
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME IN ('SQ_PDTRCDGO','SQ_PRBRCDGO')
ORDER  BY s.SEQUENCE_OWNER, s.SEQUENCE_NAME;

-- A.3 Cuantas secuencias del schema SCP ve este usuario, en total. Si da 0, es un problema
--     de VISIBILIDAD (hay que conectarse con el usuario del datasource o con DBA), no de
--     que falten estas dos. Si da un numero alto y las nuestras no estan, entonces
--     realmente NO EXISTEN.
SELECT COUNT(*) AS SECUENCIAS_SCP_VISIBLES
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'SCP';

-- A.4 Como se llaman las secuencias de SCP que si se ven. Puede que existan con otro
--     nombre (p. ej. sin el prefijo SQ_) y el mapeo JPA nunca se haya ejercitado.
SELECT s.SEQUENCE_NAME, s.LAST_NUMBER
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'SCP'
ORDER  BY s.SEQUENCE_NAME;


-- =====================================================================================
-- B. EL SCRIPT 81 — por que no entro el alterno 7
-- =====================================================================================

-- B.1 ¿Existe el rubro 235? El INSERT del 81 es un SELECT ... FROM SCP.PRBR WHERE
--     PRBRALTR = 235. Si el rubro no estuviera, el INSERT afecta 0 filas EN SILENCIO
--     — no da error, simplemente no inserta nada.
--     Esperado: 1 fila. (Si da 1, el rubro existe y el 81 sencillamente no se corrio.)
SELECT r.PRBRCDGO, r.PRBRALTR, r.PRBRDSCR, r.PRBRESTD
FROM   SCP.PRBR r
WHERE  r.PRBRALTR = 235;

-- B.2 ¿El PDTRCDGO 1178 esta ocupado por otra cosa? Si otro rubro se lo llevo, el 81 no
--     se puede correr tal cual y hay que reasignarle un codigo del rango del equipo A
--     (1200-1299). Esperado: 0 filas.
SELECT d.PDTRCDGO, d.PDTRALTR, d.PDTRDSCR, r.PRBRALTR AS RUBRO_ALTERNO
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  d.PDTRCDGO = 1178;


-- =====================================================================================
-- C. QUE HACER CON EL RESULTADO — no ejecutar nada de esto sin leer
-- =====================================================================================
--
-- A.3 = 0            -> problema de visibilidad. Reconectarse con el usuario del
--                       datasource (el de java:/jdbc/SaaDS) o con DBA y repetir A.2.
--                       El control del 88 no midio nada; hay que repetirlo.
--
-- A.3 > 0 y A.2 vacia -> las secuencias NO EXISTEN. Es un defecto PREEXISTENTE, no lo
--                       causaron los scripts de hoy: crear un rubro o un detalle de rubro
--                       desde la pantalla de parametrizacion falla con ORA-02289.
--                       NO crear las secuencias a ojo: avisar al arbitro con la salida de
--                       A.4, porque el valor de arranque tiene que quedar por encima de
--                       MAX(PDTRCDGO) = 1180 y de MAX(PRBRCDGO), y elegirlo mal reproduce
--                       exactamente el problema que se queria evitar.
--
-- A.2 con OWNER <> 'SCP' -> viven en otro schema. El mapeo JPA apunta a SCP y hay que
--                       decidir si se mueve el mapeo o se crea un sinonimo. Avisar.
--
-- B.1 = 1 fila       -> el rubro 235 existe: el script 81 simplemente no se corrio.
--                       Se puede correr ahora, es independiente del resto (JUBILACION lo
--                       usa el frente de jubilados, que todavia no existe).
--
-- B.1 = 0 filas      -> el rubro 235 no existe y el 81 nunca podria haber insertado nada.
--                       Hay que crear el rubro primero. Avisar al arbitro.
--
-- B.2 con filas      -> 1178 esta ocupado. NO forzar: avisar, y el codigo se reasigna
--                       desde el rango 1200-1299 actualizando REGISTRO-RESERVAS-EQUIPOS.md.
-- =====================================================================================
