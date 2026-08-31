-- =====================================================================================
-- DIAGNOSTICO — que hace falta para cargar TPAPPRDP (OPCION C)
-- FECHA: 2026-08-31
--
-- SOLO LECTURA. No modifica nada. NO cargues TPAPPRDP hasta ver estas salidas: hay que
-- saber que productos y grupos existen antes de decidir si se reusan o se crean.
--
-- QUE ESTAMOS ARMANDO (opcion C, decision del usuario 2026-08-31):
--   CRD reclasifica:  D 2.1.01.05.01 / 2.1.02.05.01  ->  H 2.3.01.05.01 / 2.3.01.10.01
--   CXP paga:         D 2.3.01.05.01 / 2.3.01.10.01  ->  H Banco
--
-- ⚠️ LA CUENTA DEL GRUPO DE PRODUCTO TIENE QUE SER LA 2.3.01.xx, NO LA 2.1.xx.
-- CXP debita `producto.grupoProducto.planCuenta` (verificado en
-- PagoProgramadoServiceImpl:2741-2746). En la opcion C, CXP cancela la obligacion que CRD
-- ya reconocio, asi que debita la cuenta de LIQUIDACION POR PAGAR. Si se apuntara a la
-- 2.1.xx, el aporte del socio bajaria DOS VECES —una por la reclasificacion de CRD y otra
-- por el pago de CXP— y los dos asientos cuadrarian igual.
--
-- La cadena es:  CRD.TPAP.TPAPPRDP -> PGS.PRDP.ID -> PGS.PRDP.GRUPOPRODUCTO
--                -> PGS.GRPP.GRPPCDGO -> PGS.GRPP.PLNNCDGO -> CNT.PLNN
-- =====================================================================================


-- =====================================================================================
-- 1. ESTADO ACTUAL DEL MAPEO
-- =====================================================================================

-- 1.1 Los tipos de aporte y su producto de pago. Esperado HOY: TPAPPRDP en NULL en todos
--     (por eso `contabiliza` da false y la devolucion no genera asiento).
--     Los que interesan son cesantia (11) y jubilacion (9); mira si hay otros que devuelvan
--     dinero y tambien necesiten mapeo.
SELECT t.TPAPCDGO, t.TPAPNMBR, t.TPAPPRDP
FROM   CRD.TPAP t
ORDER  BY t.TPAPCDGO;


-- =====================================================================================
-- 2. QUE PRODUCTOS DE PAGO EXISTEN YA
-- =====================================================================================

-- 2.1 Productos de pago con su grupo y la cuenta contable a la que resuelven.
--     Buscamos si YA existe alguno que apunte a 2.3.01.05.01 o 2.3.01.10.01 — si existe,
--     se reusa; si no, hay que crearlo (producto + grupo + cuenta) desde
--     CXP -> Productos y Contabilidad -> Grupos de Producto, o por script.
SELECT p.ID          AS PRODUCTO_ID,
       p.NOMBRE      AS PRODUCTO,
       p.CODIGO      AS PRODUCTO_CODIGO,
       g.GRPPCDGO    AS GRUPO_ID,
       g.GRPPNMBR    AS GRUPO,
       n.PLNNCDGO    AS CUENTA_ID,
       n.PLNNCNTA    AS CUENTA,
       n.PLNNNMBR    AS CUENTA_NOMBRE
FROM   PGS.PRDP p
LEFT   JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
ORDER  BY p.NOMBRE;

-- 2.2 Grupos de producto que YA apuntan a las cuentas que necesitamos.
--     Si aparece algo, ese grupo se reusa y solo hay que crear (o reapuntar) el producto.
SELECT g.GRPPCDGO, g.GRPPNMBR, n.PLNNCNTA, n.PLNNNMBR, g.GRPPESTD
FROM   PGS.GRPP g
JOIN   CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
WHERE  n.PLNNCNTA IN ('2.3.01.05.01','2.3.01.10.01')
ORDER  BY n.PLNNCNTA;


-- =====================================================================================
-- 3. LAS CUATRO CUENTAS TIENEN QUE EXISTIR
-- =====================================================================================

-- 3.1 Esperado: 4 filas. Las dos primeras las usa CRD en la reclasificacion (el DEBE),
--     las dos ultimas las usan CRD (el HABER) y CXP (el DEBE del pago).
--     Anota los PLNNCDGO: son los que necesita el arbitro para escribir el script de carga.
--     Si alguna falta, PARAR: no se puede armar el asiento y hay que crearla primero.
SELECT n.PLNNCDGO, n.PLNNCNTA, n.PLNNNMBR, n.PJRQCDGO AS EMPRESA
FROM   CNT.PLNN n
WHERE  n.PLNNCNTA IN ('2.1.01.05.01','2.1.02.05.01','2.3.01.05.01','2.3.01.10.01')
ORDER  BY n.PLNNCNTA;


-- =====================================================================================
-- 4. LA PLANTILLA DEL ASIENTO DE RECLASIFICACION
-- =====================================================================================

-- 4.1 Las plantillas de CRD que existen hoy, con su alterno. La 27 se llama
--     "RECLASIFICACION APORTE O COBRO EN EXCESO" y hace exactamente el asiento que
--     necesitamos —D 2.1.xx -> H 2.3.01.xx—, pero fue levantada para COBRO EN EXCESO.
--     Hay que decidir si se reusa o se crea una propia: mira que lineas tiene la 27 abajo.
SELECT p.PLNTCDGO, p.PLNTALTR, p.PLNTNMBR, p.PJRQCDGO AS EMPRESA
FROM   CNT.PLNT p
WHERE  p.PLNTALTR IN (21, 27, 28)
ORDER  BY p.PLNTALTR;

-- 4.2 Las lineas de la 27 y la 28, con su auxiliar y su cuenta.
--     ⚠️ Mira si los PLNTALTR/DTPLAXL1 son SEMANTICOS o POSICIONALES: la unica plantilla
--     de CRD confirmada como renumerada al catalogo semantico es la 21. Si la 27 es
--     posicional, NO se puede resolver por auxiliar y hay que renumerarla o crear una nueva.
SELECT p.PLNTALTR, d.DTPLAXL1, d.DTPLAXL2, d.DTPLMVTO AS MOVIMIENTO,
       n.PLNNCNTA, n.PLNNNMBR
FROM   CNT.PLNT p
JOIN   CNT.DTPL d ON d.PLNTCDGO = p.PLNTCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNTALTR IN (27, 28)
ORDER  BY p.PLNTALTR, d.DTPLAXL1;


-- =====================================================================================
-- 5. QUE HACER CON EL RESULTADO
-- =====================================================================================
-- Pasale al arbitro las salidas de 2.1, 2.2, 3.1 y 4.2. Con eso escribe:
--   - el script que crea (o reapunta) los productos de pago de cesantia y jubilacion, y
--   - el script que carga CRD.TPAP.TPAPPRDP para los tipos 9 y 11.
--
-- NO cargues TPAPPRDP a mano antes de eso. Encenderlo apunta el asiento de CXP a la cuenta
-- del grupo del producto, y si esa cuenta esta mal elegida el asiento igual va a cuadrar:
-- es el error que no se nota.
--
-- Y NO lo cargues antes de que el WAR con la reclasificacion de CRD este desplegado. En la
-- opcion C los dos asientos son mitades de uno solo: encender CXP primero deja el pago
-- registrado contra una obligacion que nadie reconocio.
-- =====================================================================================
