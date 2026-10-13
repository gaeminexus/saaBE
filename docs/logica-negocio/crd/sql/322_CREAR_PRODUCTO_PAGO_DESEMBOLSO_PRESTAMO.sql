-- =====================================================================================
-- 322 - CREAR el grupo y el producto de pago del DESEMBOLSO DE PRESTAMOS
--       (cuenta 2.3.90.90.10 SOCIOS POR PAGAR)
-- FECHA: 2026-10-13 · EQUIPO: omen-saa-1 (crd)
--
-- ⛔ ESTE SCRIPT SI ESCRIBE. Correr los bloques 0 primero y leer su resultado.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- POR QUE: el sql/157 (bloque 2) salio VACIO: no hay ningun producto de pago cuyo grupo
-- apunte a 2.3.90.90.10. Sin ese producto, "Aprobar" en Creditos > Prestamos > Ingreso
-- falla siempre (PrestamoServiceImpl, constante ID_PRODUCTO_PAGO_SOCIOS_POR_PAGAR = null):
-- la orden de desembolso que CRD le deja a tesoreria necesita un producto cuyo grupo
-- descargue la misma cuenta que el asiento de entrega acredita.
--
-- MISMO PATRON QUE EL 202 (pension jubilados), que ya funciono en produccion:
--   - El GRUPO se copia del grupo del producto 517 (Pension Complementaria Jubilados) —
--     rubros, sustento tributario, estado, empresa— y solo cambian el PK, el nombre y
--     la CUENTA, que se busca por numero (2.3.90.90.10) en la empresa de ese grupo.
--   - El PRODUCTO se copia del 517 —precio, IVA, ICE en 0, empresa, estado— y solo
--     cambian el nombre, la descripcion y el grupo.
--   Ningun valor esta escrito a mano salvo los nombres.
--
-- ⛔⛔ DOS TRAMPAS DE PK, OPUESTAS (las mismas del 202):
--   PGS.GRPP -> secuencia PGS.SQ_GRPPCDGO: HAY que dar el PK con NEXTVAL.
--   PGS.PRDP -> IDENTITY: NO hay que dar el ID; Oracle lo asigna. Se lee en el bloque 3.
-- =====================================================================================


-- ==========================================================================
-- BLOQUE 0.A - (!) Como se generan los PK, y que la secuencia no este atrasada
-- ==========================================================================

SELECT 'PGS.PRDP.ID' AS OBJETO, i.GENERATION_TYPE AS TIPO_IDENTITY
  FROM ALL_TAB_IDENTITY_COLS i
 WHERE i.OWNER = 'PGS' AND i.TABLE_NAME = 'PRDP' AND i.COLUMN_NAME = 'ID';

SELECT s.LAST_NUMBER                               AS PROXIMO_SECUENCIA,
       (SELECT MAX(g.GRPPCDGO) FROM PGS.GRPP g)    AS MAX_GRPPCDGO,
       CASE WHEN s.LAST_NUMBER > (SELECT MAX(g.GRPPCDGO) FROM PGS.GRPP g)
            THEN 'OK' ELSE 'ATRASADA - correr el bloque 0.D' END AS ESTADO
  FROM ALL_SEQUENCES s
 WHERE s.SEQUENCE_OWNER = 'PGS' AND s.SEQUENCE_NAME = 'SQ_GRPPCDGO';

-- (!) La 1ra consulta TIENE que devolver una fila (PRDP.ID es IDENTITY). Si no: PARAR y avisar.
-- (!) La 2da TIENE que decir OK. Si dice ATRASADA: correr el 0.D antes del bloque 1.
--     Si no devuelve fila, la secuencia no existe con ese nombre: PARAR y avisar.


-- ==========================================================================
-- BLOQUE 0.B - (!) Los moldes y la cuenta existen, y la cuenta es de MOVIMIENTO
-- ==========================================================================

SELECT p.ID              AS PRODUCTO_MOLDE,
       p.NOMBRE          AS NOMBRE_MOLDE,
       p.EMPRESA         AS EMPRESA,
       p.GRUPOPRODUCTO   AS GRUPO_MOLDE,
       n.PLNNCDGO        AS ID_CUENTA_DESTINO,
       n.PLNNCNTA        AS CUENTA_DESTINO,
       n.PLNNNMBR        AS NOMBRE_CUENTA,
       n.PLNNTPOO        AS TIPO_CUENTA,      -- 1 = acumulacion, 2 = movimiento
       n.PLNNESTD        AS ESTADO_CUENTA
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCNTA = '2.3.90.90.10' AND n.PJRQCDGO = g.PJRQCDGO
 WHERE p.ID = 517;

-- (!) Tiene que devolver EXACTAMENTE UNA fila:
--     - NOMBRE_MOLDE = "Pension Complementaria Jubilados".
--     - CUENTA_DESTINO = 2.3.90.90.10, SOCIOS POR PAGAR, TIPO_CUENTA = 2 (movimiento).
-- (!) Si CUENTA_DESTINO sale vacia: la cuenta no existe en esa empresa. PARAR y avisar
--     (habria que crearla en el plan de cuentas antes; no se crea aca).
-- (!) Si sale MAS de una fila: la cuenta esta duplicada en el plan. PARAR y avisar.
-- (!) Si TIPO_CUENTA = 1: es de acumulacion y no admite movimientos. PARAR y avisar.


-- ==========================================================================
-- BLOQUE 0.C - (!) No exista ya lo que vamos a crear
-- ==========================================================================

SELECT g.GRPPCDGO AS ID, g.GRPPNMBR AS NOMBRE, 'GRUPO' AS QUE_ES
  FROM PGS.GRPP g
 WHERE UPPER(g.GRPPNMBR) LIKE '%DESEMBOLSO%PRESTAMO%'
UNION ALL
SELECT p.ID, p.NOMBRE, 'PRODUCTO'
  FROM PGS.PRDP p
 WHERE UPPER(p.NOMBRE) LIKE '%DESEMBOLSO%PRESTAMO%';

-- (!) Se esperan CERO filas. Si devuelve algo, ya se creo: NO volver a correr; ir al
--     bloque 3 y tomar ese ID.


-- ==========================================================================
-- BLOQUE 0.D - SOLO SI el 0.A dijo ATRASADA: adelantar la secuencia
-- ==========================================================================
-- Descomentar y correr el bloque PL/SQL completo. Deja la secuencia por encima del MAX.
--
-- DECLARE
--   v_max NUMBER;
--   v_sig NUMBER;
-- BEGIN
--   SELECT NVL(MAX(GRPPCDGO), 0) INTO v_max FROM PGS.GRPP;
--   SELECT PGS.SQ_GRPPCDGO.NEXTVAL INTO v_sig FROM DUAL;
--   IF v_sig <= v_max THEN
--     EXECUTE IMMEDIATE 'ALTER SEQUENCE PGS.SQ_GRPPCDGO INCREMENT BY ' || (v_max - v_sig + 1);
--     SELECT PGS.SQ_GRPPCDGO.NEXTVAL INTO v_sig FROM DUAL;
--     EXECUTE IMMEDIATE 'ALTER SEQUENCE PGS.SQ_GRPPCDGO INCREMENT BY 1';
--   END IF;
-- END;
-- /


-- ==========================================================================
-- BLOQUE 1 - Crear el GRUPO (copia del grupo del 517, con la cuenta 2.3.90.90.10)
-- ==========================================================================

INSERT INTO PGS.GRPP (GRPPCDGO, GRPPNMBR, GRPPRYYA, GRPPRZZA, PLNNCDGO,
                      GRPPCSUS, GRPPESTD, PJRQCDGO)
SELECT PGS.SQ_GRPPCDGO.NEXTVAL,
       'DESEMBOLSO DE PRESTAMOS A SOCIOS',
       g.GRPPRYYA,
       g.GRPPRZZA,
       n.PLNNCDGO,
       g.GRPPCSUS,
       g.GRPPESTD,
       g.PJRQCDGO
  FROM PGS.GRPP g
  JOIN CNT.PLNN n ON n.PLNNCNTA = '2.3.90.90.10' AND n.PJRQCDGO = g.PJRQCDGO
 WHERE g.GRPPCDGO = (SELECT p.GRUPOPRODUCTO FROM PGS.PRDP p WHERE p.ID = 517);

-- (!) Tiene que decir "1 fila insertada". Si dice 0: la cuenta no se encontro (ver 0.B);
--     hacer ROLLBACK y avisar. NO seguir al bloque 2.


-- ==========================================================================
-- BLOQUE 2 - Crear el PRODUCTO (copia del 517). ⛔ Sin ID: lo asigna Oracle.
-- ==========================================================================

INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, CODIGOAUX,
                      PRECIOUNITARIO, DESCUENTO, TIPODESCUENTO, INCLUYEIVA, TIPOIVA,
                      TIPOICE, ICE, DESCRIPCION, SUBSIDIO, PRECIOSINSUB, IRBPNR,
                      MULTIPRECIO, STOCK, MANEJAUNIDAD, UNIDAD, ESTADO)
SELECT p.EMPRESA,
       (SELECT g.GRPPCDGO FROM PGS.GRPP g
         WHERE g.GRPPNMBR = 'DESEMBOLSO DE PRESTAMOS A SOCIOS'),
       'Desembolso de prestamo a socio',
       p.CODIGO,
       p.CODIGOAUX,
       p.PRECIOUNITARIO,
       p.DESCUENTO,
       p.TIPODESCUENTO,
       p.INCLUYEIVA,
       p.TIPOIVA,
       p.TIPOICE,
       p.ICE,
       'Desembolso del prestamo aprobado en CRD - descarga 2.3.90.90.10 SOCIOS POR PAGAR',
       p.SUBSIDIO,
       p.PRECIOSINSUB,
       p.IRBPNR,
       p.MULTIPRECIO,
       p.STOCK,
       p.MANEJAUNIDAD,
       p.UNIDAD,
       p.ESTADO
  FROM PGS.PRDP p
 WHERE p.ID = 517;

-- (!) Tiene que decir "1 fila insertada".

COMMIT;


-- ==========================================================================
-- BLOQUE 3 - (!) CONTROL POSTERIOR: el ID que asigno Oracle
-- ==========================================================================

SELECT p.ID                AS ID_PRODUCTO_NUEVO,
       p.NOMBRE            AS NOMBRE,
       p.EMPRESA           AS ID_EMPRESA,
       p.ESTADO            AS ESTADO,
       p.GRUPOPRODUCTO     AS ID_GRUPO,
       g.GRPPNMBR          AS GRUPO,
       n.PLNNCNTA          AS CUENTA_CONTABLE,
       n.PLNNNMBR          AS CUENTA_NOMBRE,
       p.PRECIOUNITARIO    AS PRECIO,
       p.INCLUYEIVA        AS INCLUYE_IVA,
       p.TIPOIVA           AS TIPO_IVA
  FROM PGS.PRDP p
  JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.NOMBRE = 'Desembolso de prestamo a socio';

-- (!) UNA fila, con CUENTA_CONTABLE = 2.3.90.90.10 y CUENTA_NOMBRE = SOCIOS POR PAGAR.
-- (!) ID_EMPRESA y ESTADO iguales a los del 517; precio e IVA en 0.
-- (!) ⭐ ANOTAR ID_PRODUCTO_NUEVO y pasarlo al arbitro: va en la constante
--     ID_PRODUCTO_PAGO_SOCIOS_POR_PAGAR del backend.


-- ==========================================================================
-- BLOQUE 4 - REVERSO (comentado). Solo si el bloque 3 salio mal.
-- ==========================================================================
-- ⛔ NO correr si ya se aprobo algun prestamo con este producto: habria ordenes de pago
--    apuntando a un producto borrado. En ese caso avisar en vez de borrar.
-- Primero el producto, despues el grupo (FK).
--
-- DELETE FROM PGS.PRDP WHERE NOMBRE = 'Desembolso de prestamo a socio';
-- DELETE FROM PGS.GRPP WHERE GRPPNMBR = 'DESEMBOLSO DE PRESTAMOS A SOCIOS';
-- COMMIT;
