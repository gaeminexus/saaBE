-- =====================================================================================
-- 244 - CRD.CRJB: columna para el asiento del seguro medico
-- FECHA: 2026-09-23 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ VA ANTES DEL WAR que trae el cambio de codigo.
--
-- =====================================================================================
-- PARA QUE
-- =====================================================================================
-- Hasta hoy el proceso de seguro medico (inicio de mes) NO generaba ningun asiento: solo
-- fijaba el valor de cada jubilado y armaba la orden de pago al proveedor. El asiento que
-- da de baja las cuentas individuales contra "seguros medicos por pagar" se generaba
-- recien al correr las PENSIONES de fin de mes, dentro del devengo
-- (generarAsientoDevengoPension, lineas aux1=3 y aux1=4 de la plantilla 35).
--
-- ⛔ EL PROBLEMA QUE ESO CAUSA: el dinero le sale al proveedor al INICIO del mes (la orden
-- agregada) y el pasivo se reconoce al FINAL. Si tesoreria paga esa orden antes de que
-- corran las pensiones, el asiento del pago debita "seguros medicos por pagar" sin que
-- nada la haya acreditado: la cuenta queda en negativo. Y si el periodo contable se
-- cierra en el medio, deja de ser temporal.
--
-- ⇒ DECISION DEL USUARIO (2026-09-23): el proceso de seguro genera su propio asiento, en
--   el momento en que fija los valores y manda a pagar. Y es UN SOLO ASIENTO por corrida,
--   por el total del periodo — no uno por jubilado.
--
-- POR QUE UNO SOLO, y queda escrito para que no se re-litigue: el hecho economico es uno
-- ("este mes se descontó X a los jubilados y se le debe X al proveedor") y el pago con el
-- que hay que cuadrarlo tambien es uno solo, la orden agregada. Conciliar lo devengado
-- contra lo pagado pasa a ser comparar un asiento contra una orden.
-- ⚠️ Contrapartida aceptada: desde el asiento NO se ve a quien se le bajo. Ese detalle
--    vive en CRD.PGPC (PGPCVLSG por jubilado) y en el reporte de la corrida.
--
-- =====================================================================================
-- LA COLUMNA
-- =====================================================================================
-- CRJBASSG — "ASiento del SeGuro". Sigue el patron de las otras columnas del bloque de
-- seguro de esta misma tabla: CRJBESSG (estado), CRJBFCSG (fecha), CRJBUSSG (usuario),
-- CRJBVLSG (valor), CRJBIDSG (id de la orden de pago), CRJBCTSG (cantidad).
--
-- Es NUMBER y NULLABLE a proposito: nulo significa "todavia no se genero el asiento", que
-- es el estado normal de una corrida que no corrio el seguro. ⛔ NO lleva DEFAULT: la
-- leccion de H77 es que un DEFAULT no defiende de nada cuando Hibernate manda la columna
-- con NULL explicito, y aca el nulo es justamente un valor valido.
--
-- Es un NUMERO SIN FK, igual que PGPCNMAS y PGPCNMASDV: mismo criterio que ya usa
-- PagoPensionComplementaria para los numeros de asiento (ver su javadoc, lineas 38-39).
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS
-- =====================================================================================

-- 0.1 La columna no debe existir. Esperado: 0 filas.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRJB' AND c.COLUMN_NAME = 'CRJBASSG';

-- 0.2 Foto de las columnas de hoy. Esperado: 15 filas, sin CRJBASSG.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRJB'
 ORDER BY c.COLUMN_ID;

-- 0.3 Las corridas que ya existen, para saber cuales quedan con la columna en nulo.
--     Esperado hoy: la de 2026/9, con el seguro ya generado (CRJBESSG = 1).
SELECT c.CRJBANNO, c.CRJBMESS, c.CRJBESSG, c.CRJBVLSG, c.CRJBIDSG
  FROM CRD.CRJB c
 ORDER BY c.CRJBANNO, c.CRJBMESS;


-- =====================================================================================
-- 1. LA COLUMNA
-- =====================================================================================

ALTER TABLE CRD.CRJB ADD (CRJBASSG NUMBER);

COMMENT ON COLUMN CRD.CRJB.CRJBASSG IS
  'Codigo del asiento contable del seguro medico del periodo (D cuentas individuales / H seguros medicos por pagar). UNO SOLO por corrida, por el total. Numero sin FK, mismo criterio que PGPC.PGPCNMAS. Nulo = todavia no se genero.';


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 La columna quedo. Esperado: 1 fila, NUMBER, NULLABLE = Y.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRJB' AND c.COLUMN_NAME = 'CRJBASSG';

-- 2.2 Las 16 columnas. Esperado: 16 filas, con CRJBASSG al final.
SELECT COUNT(*) AS COLUMNAS FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD' AND c.TABLE_NAME = 'CRJB';

-- 2.3 Las corridas existentes quedan con la columna en nulo, que es correcto: su seguro
--     se devengo con el esquema viejo, dentro del asiento de pensiones.
SELECT c.CRJBANNO, c.CRJBMESS, c.CRJBESSG, c.CRJBASSG
  FROM CRD.CRJB c
 ORDER BY c.CRJBANNO, c.CRJBMESS;


-- =====================================================================================
-- 3. ⛔ LA CORRIDA DE 9/2026 YA GENERADA — QUE PASA CON ELLA
-- =====================================================================================
-- El seguro de 9/2026 ya se genero (orden 478, $450,40) ANTES de este cambio, asi que
-- CRJBASSG va a quedar NULO para ese periodo y su asiento NO existe.
--
-- ⇒ Eso es CORRECTO y no hay que backfillear nada: con la columna en nulo, el devengo de
--   las pensiones de fin de mes va a seguir incluyendo las lineas del seguro, como hasta
--   hoy. O sea que el seguro de septiembre se devenga por el camino viejo, completo y una
--   sola vez.
--
-- ⚠️ El cambio de codigo DEBE respetar eso: el devengo de pensiones deja de incluir el
--    seguro SOLO cuando el periodo tiene CRJBASSG generado. Sin esa condicion, septiembre
--    quedaria sin devengar el seguro por ningun lado.
--
-- ⇒ El esquema nuevo empieza a regir con la corrida de OCTUBRE.


-- =====================================================================================
-- 4. REVERSO — comentado
-- =====================================================================================
-- ⛔ Solo si ninguna corrida tiene todavia un asiento de seguro generado. Si alguna lo
--    tiene, borrar la columna pierde el vinculo con ese asiento.
--
-- ALTER TABLE CRD.CRJB DROP COLUMN CRJBASSG;
