-- =====================================================================
-- e2-44 — Por que el ATS de agosto (AT082026, generado 14/9 17:27) sale
--          sin <tipoCliente> para el cliente con pasaporte C05580508
-- Modulo: sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-15
--
-- ✅ SOLO LECTURA. Correr entero y pegar la salida con la columna BLOQUE.
--
-- Error del validador del SRI:
--   "EL DETALLE DE VENTA CON TIPO ID CLIENTE [06], IDENTIFICACION
--    [C05580508] Y TIPO COMPROBANTE [18] ... debe indicar el tipo de
--    cliente cuando el tipo de identificacion es 06"
--
-- El codigo que escribe tipoCliente existe desde el 2026-09-11 11:29
-- (b170911e): GeneradorAtsServiceImpl.resolverTipoClienteVenta. Solo NO lo
-- escribe si el titular no tiene TTLRTPAT ni TTLRRZZA, o si TTLRRZZA no es
-- 1 ni 2. Este script dice cual de los casos es — o si el dato esta bien y
-- el problema es el WAR que genero el archivo.
--
-- Columnas copiadas de las entidades:
--   Titular (TSR.TTLR: TTLRCDGO, TTLRIDNT, TTLRNMBR, TTLRRYYA, TTLRRZZA,
--            TTLRRYYB, TTLRRZZB, TTLRTPAT, TTLRPREL, TTLRESTD)
--   Factura (CBR.FCTR: ID, COMPRADOR, NUMERO, FECHA, TOTAL, ESTADO, ESTADOEMISION)
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 1 — El titular (o los titulares) con esa identificacion
-- ESPERADO: UNA fila, con TTLRRZZA = 1 (NATURAL) o 2 (JURIDICO), o con
--           TTLRTPAT = '01'/'02'.
--   Si TTLRRZZA y TTLRTPAT vienen NULOS -> falta el Tipo de Persona.
--   Si TTLRRZZA trae otro numero (p. ej. un codigo de 4 cifras) -> se
--      grabo la PK del detalle en vez del alterno.
--   Si salen DOS filas -> titular duplicado: las facturas pueden colgar
--      del que no tiene el dato.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - titular' AS bloque,
       t.TTLRCDGO, t.TTLRIDNT, t.TTLRNMBR,
       t.TTLRRYYB AS tipo_ident_p, t.TTLRRZZB AS tipo_ident_h,
       t.TTLRRYYA AS tipo_persona_p, t.TTLRRZZA AS tipo_persona_h,
       t.TTLRTPAT AS tipo_proveedor_ats,
       t.TTLRPREL AS parte_relacionada,
       t.TTLRESTD AS estado
  FROM TSR.TTLR t
 WHERE UPPER(REPLACE(t.TTLRIDNT, ' ', '')) LIKE '%05580508%';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Facturas de agosto 2026 de ese cliente que entran al ATS
-- ESPERADO: filas con ESTADO = 5 (autorizada). El COMPRADOR debe ser el
--           TTLRCDGO del bloque 1 que tiene el Tipo de Persona.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - facturas agosto' AS bloque,
       f.ID, f.COMPRADOR, f.NUMERO, f.FECHA, f.TOTAL, f.ESTADO, f.ESTADOEMISION
  FROM CBR.FCTR f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.COMPRADOR
 WHERE UPPER(REPLACE(t.TTLRIDNT, ' ', '')) LIKE '%05580508%'
   AND f.FECHA >= DATE '2026-08-01'
   AND f.FECHA <  DATE '2026-09-01'
 ORDER BY f.FECHA;
