-- =====================================================================================
-- 362 — INSERT de UN producto de pago. omen-saa-1, 2026-11-01. Decisión del usuario:
-- «Creemos un producto propio [para las devoluciones del B8], pero asignémosle la misma cuenta contable
--  hasta confirmar».
-- Producto «DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE», copiado del 976 «Desembolso de prestamo a socio»
-- y en su MISMO GRUPO (cuenta 2.3.90.90.10 SOCIOS POR PAGAR). Así en la bandeja de Tesorería la devolución se
-- distingue del desembolso, y la contabilidad no cambia. Si contabilidad confirma otra cuenta, se cambia el
-- grupo del producto (o la cuenta del grupo), sin tocar código.
-- El código lo toma de un PARÁMETRO (rubro 237, se siembra después con el ID que salga aquí), no por cuenta:
-- con dos productos en la misma cuenta, la resolución por cuenta sería ambigua.
-- ⛔ SE CREA INACTIVO (ESTADO = 0). El código de PRODUCCIÓN de hoy resuelve el producto del DESEMBOLSO por la
--    cuenta 2.3.90.90.10: un segundo producto ACTIVO con esa cuenta detendría los desembolsos («más de un
--    producto») hasta desplegar el EAR con los parámetros. Se ACTIVA en el despliegue del EAR único (bloque 3,
--    comentado), junto con la semilla de los parámetros.
-- ID: IDENTITY (lo asigna la base). Pasar al árbitro el ID del bloque 2.
-- =====================================================================================

-- BLOQUE 0 — CONTROL ANTES
-- 0.1 El 976 y su grupo/cuenta. ESPERADO: 976 / 001 / grupo con 2.3.90.90.10.
SELECT p.ID, p.CODIGO, p.NOMBRE, p.GRUPOPRODUCTO, g.GRPPNMBR, n.PLNNCNTA, p.ESTADO
  FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.ID = 976;
-- 0.2 No existe ya. ESPERADO: 0 filas.
SELECT p.ID, p.NOMBRE FROM PGS.PRDP p WHERE UPPER(p.NOMBRE) LIKE '%DEVOLUCION DE DESCUENTO PETRO%';


-- BLOQUE 1 — (ESCRIBE) copia del 976 con nombre, código y descripción propios.
INSERT INTO PGS.PRDP (EMPRESA, GRUPOPRODUCTO, NOMBRE, CODIGO, CODIGOAUX, PRECIOUNITARIO, DESCUENTO, TIPODESCUENTO,
                      INCLUYEIVA, TIPOIVA, TIPOICE, ICE, DESCRIPCION, SUBSIDIO, PRECIOSINSUB, IRBPNR, MULTIPRECIO,
                      STOCK, MANEJAUNIDAD, UNIDAD, ESTADO)
SELECT p.EMPRESA, p.GRUPOPRODUCTO, 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE', 'DEVDESC', p.CODIGOAUX,
       p.PRECIOUNITARIO, p.DESCUENTO, p.TIPODESCUENTO, p.INCLUYEIVA, p.TIPOIVA, p.TIPOICE, p.ICE,
       'Devolucion al participe del sobrante de una carga Petro/ARCH (destino 3, B8). Misma cuenta que el desembolso (socios por pagar) hasta que contabilidad confirme.',
       p.SUBSIDIO, p.PRECIOSINSUB, p.IRBPNR, p.MULTIPRECIO, p.STOCK, p.MANEJAUNIDAD, p.UNIDAD, 0
  FROM PGS.PRDP p
 WHERE p.ID = 976
   AND NOT EXISTS (SELECT 1 FROM PGS.PRDP x WHERE UPPER(x.NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE');
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: el nuevo en el MISMO grupo del 976, con 2.3.90.90.10 y ESTADO 0. PASAR EL ID AL ÁRBITRO.
SELECT p.ID, p.CODIGO, p.NOMBRE, p.GRUPOPRODUCTO, n.PLNNCNTA, p.ESTADO
  FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.ID = 976 OR UPPER(p.NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE'
 ORDER BY p.ID;


-- BLOQUE 3 — ⛔ NO CORRER AHORA. Se corre en el DESPLIEGUE del EAR único (lo indica el árbitro en el runbook):
-- UPDATE PGS.PRDP SET ESTADO = 1 WHERE UPPER(NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE' AND ESTADO = 0;
-- COMMIT;


-- REVERSO (comentado; solo si ninguna orden de pago lo usa todavía):
-- DELETE FROM PGS.PRDP WHERE UPPER(NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE';
-- COMMIT;

-- RESULTADO (2026-11-01, producción): se corrió la PRIMERA versión (activo) → producto 1222 «DEVDESC» quedó con ESTADO 1
-- en el grupo 79 (2.3.90.90.10), junto al 976. Eso detiene los desembolsos del código de producción (dos productos
-- activos en la cuenta). Corrección inmediata indicada al usuario: UPDATE PGS.PRDP SET ESTADO = 0 WHERE ID = 1222.
-- El 1222 se activa en el despliegue del EAR único, con la semilla de los parámetros (rubro 237, devolución = 1222,
-- desembolso = 976).
-- 2026-11-01: corregido en producción. 976 ESTADO 1, 1222 ESTADO 0 (inactivo hasta el despliegue del EAR único).
