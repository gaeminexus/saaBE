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
-- ⛔ Este producto NO se usa hasta que esté el código del parámetro (después de U-4) y su semilla.
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
       p.SUBSIDIO, p.PRECIOSINSUB, p.IRBPNR, p.MULTIPRECIO, p.STOCK, p.MANEJAUNIDAD, p.UNIDAD, p.ESTADO
  FROM PGS.PRDP p
 WHERE p.ID = 976
   AND NOT EXISTS (SELECT 1 FROM PGS.PRDP x WHERE UPPER(x.NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE');
-- (!) "1 fila insertada".
COMMIT;


-- BLOQUE 2 — CONTROL DESPUÉS. ESPERADO: 1 fila en el MISMO grupo del 976 y con 2.3.90.90.10. PASAR EL ID AL ÁRBITRO.
SELECT p.ID, p.CODIGO, p.NOMBRE, p.GRUPOPRODUCTO, n.PLNNCNTA, p.ESTADO
  FROM PGS.PRDP p JOIN PGS.GRPP g ON g.GRPPCDGO = p.GRUPOPRODUCTO LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = g.PLNNCDGO
 WHERE p.ID = 976 OR UPPER(p.NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE'
 ORDER BY p.ID;


-- REVERSO (comentado; solo si ninguna orden de pago lo usa todavía):
-- DELETE FROM PGS.PRDP WHERE UPPER(NOMBRE) = 'DEVOLUCION DE DESCUENTO PETRO/ARCH A PARTICIPE';
-- COMMIT;
