-- =====================================================================
-- e2-50 — TSR.CTBN.CTBNNMBR: nombre del titular de la CUENTA
-- Modulo: tsr  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-16
--
-- ⚠️ DDL. VA ANTES DEL WAR que mapee la columna: la entidad
--    CuentaBancariaTitular la va a nombrar en todo SELECT y, sin ella,
--    TODA lectura de cuentas de titulares muere con ORA-00904 (ficha del
--    titular, solicitud de pago, archivo del banco).
--
-- POR QUE EXISTE — pedido del usuario, 2026-09-16:
--   «se puede pagar dinero a los titulares a cuentas que no son de ellos
--    sino de otras personas, por lo que se requiere el nombre del titular
--    de la cuenta».
--   Es el mismo caso que resolvio el e2-42 con la identificacion
--   (CTBNTPID/CTBNIDNT): la cuenta puede estar a nombre de otra persona, y
--   el banco valida el NOMBRE contra el numero de cuenta.
--   Vacia = la cuenta esta a nombre del propio titular (comportamiento de
--   hoy); el archivo del banco sigue usando el nombre del titular.
--
-- Largo: 200, el mismo criterio que TSR.TTLR.TTLRNMBR (2000) recortado a lo
-- que un archivo bancario admite -- el Internacional trunca el nombre a 41
-- caracteres y el Pacifico no lo limita. 200 CHAR alcanza de sobra y no
-- invita a pegar un parrafo.
--
-- Columnas copiadas de la entidad CuentaBancariaTitular (TSR.CTBN):
--   CTBNCDGO, TTLRCDGO, BEXTCDGO, CTBNTPCT, CTBNNMCT, CTBNTPID, CTBNIDNT,
--   CTBNOBSR, CTBNESTD, CTBNFCRG, CTBNUSAR.
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — Control ANTES
-- ESPERADO: TABLA_EXISTE = 1 y COLUMNA_EXISTE = 0.
--   COLUMNA_EXISTE = 1 -> ya se corrio: NO correr el bloque 1.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - antes' AS bloque,
       (SELECT COUNT(*) FROM all_tables
         WHERE owner = 'TSR' AND table_name = 'CTBN')                              AS tabla_existe,
       (SELECT COUNT(*) FROM all_tab_columns
         WHERE owner = 'TSR' AND table_name = 'CTBN' AND column_name = 'CTBNNMBR') AS columna_existe
  FROM dual;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — DDL
-- ---------------------------------------------------------------------
ALTER TABLE TSR.CTBN ADD (CTBNNMBR VARCHAR2(200 CHAR));

COMMENT ON COLUMN TSR.CTBN.CTBNNMBR IS
  'Nombre del titular de la cuenta cuando la cuenta NO esta a nombre del titular del pago; vacio = es del propio titular';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control DESPUES
-- ESPERADO: una fila, VARCHAR2, CHAR_LENGTH 200, CHAR_USED 'C', NULLABLE 'Y'.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - despues' AS bloque,
       column_name, data_type, char_length, char_used, nullable
  FROM all_tab_columns
 WHERE owner = 'TSR' AND table_name = 'CTBN' AND column_name = 'CTBNNMBR';


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Lectura: cuentas que YA se sabe que son de otra persona
-- Son las que el e2-42 dejo con identificacion propia distinta a la del
-- titular. A esas conviene cargarles el nombre a mano despues del despliegue.
-- ESPERADO: la lista de cuentas a revisar (puede venir vacia).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - cuentas con identificacion propia' AS bloque,
       c.CTBNCDGO, c.TTLRCDGO, t.TTLRNMBR AS titular_del_pago,
       c.CTBNNMCT AS numero_cuenta, c.CTBNIDNT AS identificacion_de_la_cuenta,
       t.TTLRIDNT AS identificacion_del_titular
  FROM TSR.CTBN c
  JOIN TSR.TTLR t ON t.TTLRCDGO = c.TTLRCDGO
 WHERE c.CTBNIDNT IS NOT NULL
   AND REPLACE(c.CTBNIDNT, ' ', '') <> REPLACE(NVL(t.TTLRIDNT, ' '), ' ', '')
 ORDER BY c.CTBNCDGO;


-- ---------------------------------------------------------------------
-- REVERSO — comentado. Solo con el WAR anterior desplegado.
-- ---------------------------------------------------------------------
-- ALTER TABLE TSR.CTBN DROP COLUMN CTBNNMBR;
