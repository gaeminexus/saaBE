-- =====================================================================================
-- PRODUCTOS DE PAGO PARA LA DEVOLUCION DE APORTES + carga de CRD.TPAP.TPAPPRDP
-- FECHA: 2026-08-31
--
-- ⛔ NO CORRER ESTE SCRIPT ANTES DE DESPLEGAR EL WAR.
-- En la opcion C los dos asientos son mitades de uno solo. Encender el lado de CXP antes de
-- que CRD reconozca la obligacion deja el pago registrado contra una obligacion que nadie
-- reconocio. El orden es: WAR desplegado -> este script.
--
-- QUE HACE: crea los 5 grupos de producto (uno por cuenta de liquidacion) con sus 5
-- productos de pago, y carga CRD.TPAP.TPAPPRDP para los 11 tipos mapeados en CRD.CTAP.
--
-- CXP debita `producto.grupoProducto.planCuenta` al confirmarse el pago
-- (PagoProgramadoServiceImpl:2741-2746). Con esto, el pago de una devolucion de cesantia
-- debita 2.3.01.05.01 — la MISMA cuenta que CRD acredito al reclasificar. La obligacion
-- nace y se cancela en la misma cuenta, y Banco se acredita UNA sola vez.
--
-- ⚠️ POR QUE 5 Y NO 11: son 5 CUENTAS de liquidacion distintas. Varios tipos comparten
-- cuenta (las tres familias de cesantia liquidan todas en 2.3.01.05.01). Un producto por
-- CUENTA, no por tipo.
--
-- ⚠️ POR QUE SE CLONAN FILAS EN VEZ DE ESCRIBIR LOS INSERT A MANO:
-- PGS.GRPP y PGS.PRDP tienen columnas cuya obligatoriedad no se puede deducir de la entidad
-- JPA (GRPPRYYA, GRPPRZZA, GRPPCSUS, y las ~20 de PRDP). Copiar una fila que YA funciona y
-- cambiarle solo el nombre y la cuenta es inmune a eso. La plantilla es el producto
-- "PAGO DE NOMINA", que ya apunta a una cuenta de PASIVO (2.3.90.15 HONORARIOS POR PAGAR):
-- es el analogo exacto de lo que hace falta.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS — si alguno falla, PARAR
-- =====================================================================================

-- 0.1 La secuencia de grupos existe. Esperado: 1 fila.
--     ⚠️ No darlo por hecho: el 2026-08-31 se descubrio que SCP.SQ_PDTRCDGO y SQ_PRBRCDGO
--     NO existen pese a estar declaradas en las entidades JPA. Si esta consulta viene
--     vacia, PARAR y avisar: hay que resolver como se genera el PK antes de insertar.
SELECT s.SEQUENCE_OWNER, s.SEQUENCE_NAME, s.LAST_NUMBER
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_NAME = 'SQ_GRPPCDGO';

-- 0.2 PGS.PRDP.ID es IDENTITY (la entidad declara GenerationType.IDENTITY), asi que el
--     INSERT NO debe dar el ID. Confirmalo. Esperado: IDENTITY_COLUMN = 'YES'.
SELECT c.COLUMN_NAME, c.DATA_DEFAULT, c.IDENTITY_COLUMN, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'PGS' AND c.TABLE_NAME = 'PRDP' AND c.COLUMN_NAME = 'ID';

-- 0.3 La fila plantilla existe. Esperado: 1 fila — producto 345 "PAGO DE NOMINA",
--     grupo 38, cuenta 9678 (2.3.90.15 HONORARIOS POR PAGAR).
SELECT p.ID, p.NOMBRE, p.EMPRESA, p.GRUPOPRODUCTO, g.GRPPNMBR, n.PLNNCNTA, n.PLNNNMBR
FROM   PGS.PRDP p
JOIN   PGS.GRPP g ON g.GRPPCDGO  = p.GRUPOPRODUCTO
JOIN   CNT.PLNN n ON n.PLNNCDGO  = g.PLNNCDGO
WHERE  p.ID = 345;

-- 0.4 Las 5 cuentas de liquidacion existen y son de la empresa 1236. Esperado: 5 filas.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR
FROM   CNT.PLNN n
WHERE  n.PLNNCDGO IN (10360, 10361, 10362, 10363, 10364)
AND    n.PJRQCDGO = 1236
ORDER  BY n.PLNNCNTA;

-- 0.5 CRD.CTAP cargada. Esperado: 11 filas (el script 94 ya corrio).
SELECT COUNT(*) AS FILAS_CTAP FROM CRD.CTAP WHERE PJRQCDGO = 1236;

-- 0.6 TPAPPRDP todavia en NULL en todos. Esperado: 0 filas con producto.
--     Si alguno ya tiene, PARAR: alguien lo cargo por otro camino y hay que revisarlo.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPPRDP
FROM   CRD.TPAP t
WHERE  t.TPAPPRDP IS NOT NULL;


-- =====================================================================================
-- 1. LOS 5 GRUPOS DE PRODUCTO — clonados del grupo 38, cambiando nombre y cuenta
-- =====================================================================================

INSERT INTO PGS.GRPP (GRPPCDGO, GRPPNMBR, GRPPRYYA, GRPPRZZA, PLNNCDGO, GRPPCSUS, GRPPESTD, PJRQCDGO)
SELECT PGS.SQ_GRPPCDGO.NEXTVAL, m.NOMBRE, g.GRPPRYYA, g.GRPPRZZA, m.PLNN, g.GRPPCSUS, 1, g.PJRQCDGO
FROM   PGS.GRPP g
CROSS  JOIN (
    SELECT 'DEVOLUCION APORTES CESANTIA'      AS NOMBRE, 10360 AS PLNN FROM DUAL UNION ALL
    SELECT 'DEVOLUCION INTERESES CESANTIA',         10361 FROM DUAL UNION ALL
    SELECT 'DEVOLUCION APORTES JUBILACION',         10362 FROM DUAL UNION ALL
    SELECT 'DEVOLUCION INTERESES JUBILACION',       10363 FROM DUAL UNION ALL
    SELECT 'DEVOLUCION PENSIONES COMPLEMENTARIAS',  10364 FROM DUAL
) m
WHERE  g.GRPPCDGO = 38
AND    NOT EXISTS (SELECT 1 FROM PGS.GRPP x WHERE x.GRPPNMBR = m.NOMBRE);


-- =====================================================================================
-- 2. LOS 5 PRODUCTOS DE PAGO — clonados del producto 345
-- =====================================================================================
-- El ID no se da: es IDENTITY. Se copian TODAS las demas columnas del 345 y solo se
-- cambian NOMBRE, CODIGO y GRUPOPRODUCTO.

INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, CODIGOAUX, PRECIOUNITARIO,
                      DESCUENTO, TIPODESCUENTO, INCLUYEIVA, TIPOIVA, TIPOICE, ICE,
                      DESCRIPCION, SUBSIDIO, PRECIOSINSUB, IRBPNR, MULTIPRECIO, STOCK,
                      MANEJAUNIDAD, UNIDAD, ESTADO)
SELECT p.EMPRESA, g.GRPPCDGO, g.GRPPNMBR, m.CODIGO, p.CODIGOAUX, p.PRECIOUNITARIO,
       p.DESCUENTO, p.TIPODESCUENTO, p.INCLUYEIVA, p.TIPOIVA, p.TIPOICE, p.ICE,
       g.GRPPNMBR, p.SUBSIDIO, p.PRECIOSINSUB, p.IRBPNR, p.MULTIPRECIO, p.STOCK,
       p.MANEJAUNIDAD, p.UNIDAD, 1
FROM   PGS.PRDP p
CROSS  JOIN (
    SELECT 'DEVOLUCION APORTES CESANTIA'      AS NOMBRE, 'DEVAPCE' AS CODIGO FROM DUAL UNION ALL
    SELECT 'DEVOLUCION INTERESES CESANTIA',        'DEVINCE' FROM DUAL UNION ALL
    SELECT 'DEVOLUCION APORTES JUBILACION',        'DEVAPJU' FROM DUAL UNION ALL
    SELECT 'DEVOLUCION INTERESES JUBILACION',      'DEVINJU' FROM DUAL UNION ALL
    SELECT 'DEVOLUCION PENSIONES COMPLEMENTARIAS', 'DEVPECO' FROM DUAL
) m
JOIN   PGS.GRPP g ON g.GRPPNMBR = m.NOMBRE
WHERE  p.ID = 345
AND    NOT EXISTS (SELECT 1 FROM PGS.PRDP x WHERE x.CODIGO = m.CODIGO);


-- =====================================================================================
-- 3. CRD.TPAP.TPAPPRDP — el mapeo tipo de aporte -> producto
-- =====================================================================================
-- Se deriva de CRD.CTAP: el producto de un tipo es el que apunta al grupo cuya cuenta
-- contable es la CTAPPLNL de ese tipo. Asi las dos configuraciones no pueden divergir:
-- no se escribe una lista a mano, se lee de la que ya se verifico.

UPDATE CRD.TPAP t
SET    t.TPAPPRDP = (
    SELECT p.ID
    FROM   CRD.CTAP c
    JOIN   PGS.GRPP g ON g.PLNNCDGO = c.CTAPPLNL
    JOIN   PGS.PRDP p ON p.GRUPOPRODUCTO = g.GRPPCDGO
    WHERE  c.TPAPCDGO = t.TPAPCDGO
    AND    c.PJRQCDGO = 1236
    AND    g.GRPPNMBR LIKE 'DEVOLUCION %'
)
WHERE  EXISTS (SELECT 1 FROM CRD.CTAP c
               WHERE c.TPAPCDGO = t.TPAPCDGO AND c.PJRQCDGO = 1236);

COMMIT;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 Esperado: 11 filas. Cada tipo con su producto, y la cuenta del grupo del producto
--     IGUAL a la CTAPPLNL de CRD.CTAP. Si alguna fila muestra cuentas distintas en las dos
--     ultimas columnas, PARAR: el pago de CXP debitaria una cuenta distinta de la que CRD
--     acredito, y el asiento cuadraria igual.
SELECT t.TPAPCDGO, t.TPAPNMBR, p.NOMBRE AS PRODUCTO,
       nc.PLNNCNTA AS CUENTA_CTAP, ng.PLNNCNTA AS CUENTA_PRODUCTO,
       CASE WHEN nc.PLNNCDGO = ng.PLNNCDGO THEN 'OK' ELSE '*** DIFIERE ***' END AS CONTROL
FROM   CRD.TPAP t
JOIN   CRD.CTAP c  ON c.TPAPCDGO = t.TPAPCDGO AND c.PJRQCDGO = 1236
JOIN   CNT.PLNN nc ON nc.PLNNCDGO = c.CTAPPLNL
JOIN   PGS.PRDP p  ON p.ID = t.TPAPPRDP
JOIN   PGS.GRPP g  ON g.GRPPCDGO = p.GRUPOPRODUCTO
JOIN   CNT.PLNN ng ON ng.PLNNCDGO = g.PLNNCDGO
ORDER  BY t.TPAPCDGO;

-- 4.2 Tipos en CTAP que quedaron SIN producto. Esperado: 0 filas.
SELECT c.TPAPCDGO, t.TPAPNMBR
FROM   CRD.CTAP c
JOIN   CRD.TPAP t ON t.TPAPCDGO = c.TPAPCDGO
WHERE  c.PJRQCDGO = 1236 AND t.TPAPPRDP IS NULL;

-- 4.3 Tipos CON producto pero SIN fila en CTAP. Esperado: 0 filas.
--     Seria el peor caso: CXP contabilizaria el pago y CRD no reconoceria la obligacion.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPPRDP
FROM   CRD.TPAP t
WHERE  t.TPAPPRDP IS NOT NULL
AND    NOT EXISTS (SELECT 1 FROM CRD.CTAP c
                   WHERE c.TPAPCDGO = t.TPAPCDGO AND c.PJRQCDGO = 1236);

-- 4.4 Los 5 grupos y 5 productos creados. Esperado: 5 filas.
SELECT g.GRPPCDGO, g.GRPPNMBR, n.PLNNCNTA, p.ID AS PRODUCTO_ID, p.CODIGO
FROM   PGS.GRPP g
JOIN   CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
LEFT   JOIN PGS.PRDP p ON p.GRUPOPRODUCTO = g.GRPPCDGO
WHERE  g.GRPPNMBR LIKE 'DEVOLUCION %'
ORDER  BY g.GRPPNMBR;


-- =====================================================================================
-- 5. REVERSO — comentado a proposito. Leer antes de descomentar.
-- =====================================================================================
-- Apagar el lado de CXP es SOLO poner TPAPPRDP en NULL: `contabiliza` vuelve a dar false y
-- CXP deja de generar asiento, sin borrar nada. NO borrar los grupos ni los productos si ya
-- hubo un pago contabilizado contra ellos.
--
-- UPDATE CRD.TPAP SET TPAPPRDP = NULL WHERE TPAPCDGO IN (9,11,12,13,14,15,16,21,22,23,24);
-- COMMIT;
-- =====================================================================================
