-- =====================================================================
-- e2-42 — TSR.CTBN: la identificacion con la que se ABRIO la cuenta bancaria
-- Modulo: tsr / pagos  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-14
--
-- ⚠️ NO ES LECTURA. Agrega dos columnas y un CHECK. El bloque 4 es lectura.
--
-- QUE HACE
--   Agrega a TSR.CTBN (CuentaBancariaTitular):
--     CTBNTPID  NUMBER        tipo de identificacion = codigo ALTERNO del
--                             detalle del rubro 36 (1 cedula, 2 RUC,
--                             3 pasaporte). Un solo numero, NO el par P/H
--                             de TSR.TTLR (el P vale 36 siempre y ya costo
--                             un archivo del banco roto, §40.4 del estado).
--     CTBNIDNT  VARCHAR2(20)  la identificacion. Mismo largo que TTLRIDNT.
--
--   Diseño y contrato: docs/logica-negocio/tsr/API-IDENTIFICACION-CUENTA-BANCARIA.md
--
-- POR QUE
--   El archivo del banco manda la identificacion del TITULAR. Una persona
--   natural factura con RUC (1709616302001) y abre la cuenta con cedula
--   (1709616302): el banco valida contra la identificacion de la cuenta.
--
-- ES SEGURO CORRERLO CON EL SISTEMA ARRIBA
--   Columnas nullable: no reescribe filas. Mientras una cuenta no tenga
--   estos datos, el archivo sigue usando la del titular, como hoy.
--
-- ⛔ ORDEN RESPECTO DEL WAR
--   ESTE SCRIPT VA ANTES del WAR que mapee CTBNTPID/CTBNIDNT. Sin ellas,
--   TODA lectura de CuentaBancariaTitular da ORA-00904: la ficha de
--   titulares, la solicitud de pago, anticipos, egresos, el archivo del
--   banco — y tambien codigo de crd que lee cuentas de titulares.
--
-- REVERSO al final, COMENTADO.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 Las columnas NO deben existir. ESPERADO: 0 filas. Si hay filas, PARAR.
SELECT 'BLOQUE 0.1 - no existen' AS bloque, column_name
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'CTBN'
   AND column_name IN ('CTBNTPID', 'CTBNIDNT');

-- 0.2 El rubro 36 tiene los alternos 1, 2 y 3. ESPERADO: 3 filas o mas
--     (CEDULA, RUC, PASAPORTE, y quizas EXTERIOR). Si falta alguno, PARAR.
SELECT 'BLOQUE 0.2 - rubro 36' AS bloque, d.PDTRALTR, d.PDTRDSCR, d.PDTRVLRV
  FROM SCP.PDTR d
  JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 36
 ORDER BY d.PDTRALTR;

-- 0.3 Linea base. Anotar: el 3.3 debe dar lo mismo.
SELECT 'BLOQUE 0.3 - linea base' AS bloque, COUNT(*) AS total_cuentas
  FROM TSR.CTBN;


-- =====================================================================
-- BLOQUE 1 -- LAS COLUMNAS
-- =====================================================================

ALTER TABLE TSR.CTBN ADD (
    CTBNTPID NUMBER        NULL,
    CTBNIDNT VARCHAR2(20)  NULL
);

COMMENT ON COLUMN TSR.CTBN.CTBNTPID IS
    'Tipo de identificacion con la que se abrio la cuenta: codigo alterno del detalle del rubro 36 (1 cedula, 2 RUC, 3 pasaporte). NULL = usar la del titular';
COMMENT ON COLUMN TSR.CTBN.CTBNIDNT IS
    'Identificacion con la que se abrio la cuenta en el banco. Va al archivo de pagos en lugar de la del titular. NULL = usar la del titular';


-- =====================================================================
-- BLOQUE 2 -- LOS DOS JUNTOS O NINGUNO
-- =====================================================================
-- Un tipo sin numero, o un numero sin tipo, no sirve para el archivo del
-- banco. La aplicacion lo valida; esto es la red por si alguien carga por SQL.

ALTER TABLE TSR.CTBN ADD CONSTRAINT CK_CTBN_IDENTIFICACION CHECK (
    (CTBNTPID IS NULL AND CTBNIDNT IS NULL)
 OR (CTBNTPID IS NOT NULL AND CTBNIDNT IS NOT NULL)
);


-- =====================================================================
-- BLOQUE 3 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 3.1 ESPERADO: 2 filas, CTBNIDNT VARCHAR2 20 'Y' y CTBNTPID NUMBER 'Y'.
SELECT 'BLOQUE 3.1 - columnas' AS bloque, column_name, data_type, data_length, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'CTBN'
   AND column_name IN ('CTBNTPID', 'CTBNIDNT')
 ORDER BY column_name;

-- 3.2 ESPERADO: CK_CTBN_IDENTIFICACION / ENABLED.
SELECT 'BLOQUE 3.2 - check' AS bloque, constraint_name, status
  FROM all_constraints
 WHERE owner = 'TSR' AND table_name = 'CTBN'
   AND constraint_name = 'CK_CTBN_IDENTIFICACION';

-- 3.3 ESPERADO: el mismo total del 0.3, y CON_IDENTIFICACION = 0.
SELECT 'BLOQUE 3.3 - nada cambio' AS bloque,
       COUNT(*) AS total_cuentas,
       SUM(CASE WHEN CTBNIDNT IS NOT NULL THEN 1 ELSE 0 END) AS con_identificacion
  FROM TSR.CTBN;


-- =====================================================================
-- BLOQUE 4 -- LECTURA: cuentas que conviene revisar
-- =====================================================================
-- Cuentas ACTIVAS cuyo titular esta registrado con RUC (13 digitos): son las
-- candidatas a haberse abierto con cedula. NO se completan solas: la cedula
-- de una persona natural suele ser los 10 primeros digitos del RUC, pero no
-- siempre la cuenta se abrio con ella. Cargarla en la ficha del titular.
-- ESPERADO: la lista para revisar. Si da 0 filas, no hay nada que cargar.

SELECT 'BLOQUE 4 - revisar' AS bloque,
       t.TTLRCDGO, t.TTLRNMBR, t.TTLRIDNT AS identificacion_titular,
       b.BEXTNMBR AS banco, c.CTBNNMCT AS numero_cuenta, c.CTBNCDGO
  FROM TSR.CTBN c
  JOIN TSR.TTLR t ON t.TTLRCDGO = c.TTLRCDGO
  LEFT JOIN TSR.BEXT b ON b.BEXTCDGO = c.BEXTCDGO
 WHERE NVL(c.CTBNESTD, 1) = 1
   AND c.CTBNIDNT IS NULL
   AND LENGTH(REGEXP_REPLACE(t.TTLRIDNT, '[^0-9]', '')) = 13
 ORDER BY t.TTLRNMBR;


-- =====================================================================
-- REVERSO -- COMENTADO. Bajar ANTES el WAR que mapea estas columnas.
-- Se pierden las identificaciones cargadas.
-- =====================================================================
-- ALTER TABLE TSR.CTBN DROP CONSTRAINT CK_CTBN_IDENTIFICACION;
-- ALTER TABLE TSR.CTBN DROP (CTBNTPID, CTBNIDNT);
