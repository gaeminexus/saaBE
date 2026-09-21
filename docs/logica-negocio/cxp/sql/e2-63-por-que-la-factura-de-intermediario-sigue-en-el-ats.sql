-- =====================================================================
-- e2-63 — La factura marcada como intermediario sigue saliendo en el ATS
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- POR QUE EXISTE — captura del DIMM del 2026-09-21, ATS de 08/2026:
--   Proveedor 0992939680001 (R&M WORLDTRAVEL), factura 001-001-000002868:
--     Base IVA 0%            = 0.00
--     Base IVA diferente 0%  = 1521.17
--     Monto IVA              = 12.00
--   El usuario dice que esa factura esta marcada como intermediario y aun
--   asi aparece en el anexo.
--
-- ⚠️ SON DOS SINTOMAS EN LA MISMA FILA, Y CONVIENE NO MEZCLARLOS:
--
--   (A) Aparece, estando marcada como intermediario.
--       El ATS excluye las de intermediario desde el commit 5901c8ef
--       (2026-09-15): `and (f.esIntermediario is null or f.esIntermediario
--       <> 1)` en comprasFacturaCompra.
--
--   (B) Declara 1521.17 como BASE GRAVADA.
--       Pero el e2-60, que el usuario ya commiteo, dejo esta misma factura
--       (es la ID 343) con SUBCERO = 1441.17 y solo 80.00 gravados. O sea
--       que el numero de la captura es el de ANTES del e2-60.
--
--   Que (B) muestre datos viejos es la pista fuerte: si el .xml del DIMM
--   se genero antes de correr los scripts, (A) se explica igual sin que
--   haya ningun defecto -- el archivo es viejo, no el codigo.
--
--   LAS TRES CAUSAS POSIBLES, y este script las separa:
--     1. El XML del DIMM es anterior a los arreglos  -> BLOQUE 1 lo dice
--     2. FCTCESIN no esta en 1 para esa factura      -> BLOQUE 0 lo dice
--     3. El WAR desplegado es anterior al 2026-09-15 -> ni el script ni el
--        dato lo dicen: hay que mirar que WAR esta corriendo
--
-- ⚠️ Y UNA CUARTA, que conviene descartar: el ATS filtra las compras por
--    FECHAREGISTROCONTABLE (con respaldo a FECHA) y otros reportes por
--    FECHA. Si las dos fechas caen en meses distintos, un documento puede
--    aparecer donde no se lo espera. El BLOQUE 0 muestra las dos.
--
-- COMO SE MARCA UNA FACTURA DE INTERMEDIARIO (medido en el codigo):
--   ProcesoCargaDocumentosServiceImpl:1628 pone FCTCESIN = 1, y es el
--   UNICO lugar del backend que lo escribe -- o sea que se marca al
--   PROCESAR el documento, no despues. Una factura ya registrada solo se
--   marca por SQL (el equipo de la laptop tiene el lap1-09 para eso).
--   ⛔ Si el usuario creyo marcarla desde otra pantalla, puede no haberse
--      guardado nunca: el BLOQUE 0 lo contesta.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, NUMERO,
--   NUMESTABLECIMIENTO, NUMPTOEMISION, SECUENCIAL, FECHA,
--   FECHAREGISTROCONTABLE, SUBTOTAL, SUBCERO, SUBNOOBJ, SUBEXENT, VIVA,
--   ESTADO, TITULAR, FCTCESIN, FCTCPRIN), Titular (TSR.TTLR).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — LA FACTURA DE LA CAPTURA, con todo lo que decide su destino.
-- ESPERADO / COMO LEERLO:
--   ES_INTERMEDIARIO = 1  -> esta marcada; el ATS de hoy NO deberia
--                            declararla. Si igual aparece, el XML es viejo
--                            o el WAR es anterior al 2026-09-15.
--   ES_INTERMEDIARIO = 0 o nulo -> NUNCA SE MARCO. No es un defecto del
--                            ATS: hay que marcarla (y eso es un UPDATE).
--   BASE_0 = 1441.17      -> el e2-60 esta aplicado y la captura es vieja.
--   BASE_0 = 0            -> el e2-60 no quedo guardado en esta fila.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - la factura de la captura' AS bloque,
       f.ID, f.NUMERO, t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor,
       f.FECHA, f.FECHAREGISTROCONTABLE,
       f.FCTCESIN AS es_intermediario,
       f.FCTCPRIN AS producto_intermediario,
       f.ESTADO,
       NVL(f.SUBTOTAL,0)  AS subtotal,
       NVL(f.SUBCERO,0)   AS base_0,
       NVL(f.SUBNOOBJ,0)  AS no_objeto,
       NVL(f.SUBEXENT,0)  AS exento,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS gravada_que_declara_el_ats,
       NVL(f.VIVA,0)      AS iva
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE REPLACE(t.TTLRIDNT, ' ', '') = '0992939680001'
   AND f.NUMERO LIKE '%2868%'
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — ¿Cuantas facturas estan marcadas como intermediario, y de
--            que periodo? Dice si la marca se esta usando de verdad.
-- ESPERADO: si da CERO filas en todo el historico, entonces NINGUNA
--           factura se marco nunca, y el filtro del ATS no tiene nada que
--           excluir -- que es una respuesta, no un defecto.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - marcadas como intermediario' AS bloque,
       TO_CHAR(f.FECHA, 'YYYY-MM') AS periodo,
       COUNT(*) AS facturas,
       SUM(NVL(f.SUBTOTAL,0)) AS subtotal
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND NVL(f.FCTCESIN,0) = 1
 GROUP BY TO_CHAR(f.FECHA, 'YYYY-MM')
 ORDER BY periodo DESC;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — La distribucion de la columna, para ver si esta poblada.
-- ESPERADO: la mayoria en 0 (no son de intermediario). Muchas en NULO no
--           es un problema: el ATS trata nulo como "no es intermediario".
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - distribucion de FCTCESIN' AS bloque,
       CASE WHEN f.FCTCESIN IS NULL THEN '(nulo)' ELSE TO_CHAR(f.FCTCESIN) END AS valor,
       COUNT(*) AS facturas
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
 GROUP BY CASE WHEN f.FCTCESIN IS NULL THEN '(nulo)' ELSE TO_CHAR(f.FCTCESIN) END
 ORDER BY valor;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Las marcadas como intermediario de AGOSTO, una por una, con
--            las dos fechas que deciden en que periodo cae cada una.
-- ESPERADO: ninguna de estas deberia estar en el ATS de 08/2026.
--   Si FECHA y FECHAREGISTROCONTABLE caen en meses distintos, anotarlo:
--   el ATS usa la segunda y el cuadre 104 usa la primera.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - intermediarios de agosto' AS bloque,
       f.ID, f.NUMERO, t.TTLRIDNT AS ruc, t.TTLRNMBR AS proveedor,
       f.FECHA, f.FECHAREGISTROCONTABLE,
       NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND NVL(f.FCTCESIN,0) = 1
   AND ( (f.FECHAREGISTROCONTABLE >= DATE '2026-08-01' AND f.FECHAREGISTROCONTABLE < DATE '2026-09-01')
      OR (f.FECHAREGISTROCONTABLE IS NULL
          AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01') )
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 4 — Control cruzado: ¿el e2-60 sigue aplicado en las tres que
--            corrigio? Si estas dan los valores nuevos, la captura del
--            DIMM es de un XML viejo y no hay nada roto en los datos.
-- ESPERADO: 343 -> base_0 1441.17 / gravada 80.00
--           493 -> base_0 7.20    / gravada 12.95
--           494 -> base_0 6.10    / gravada 15.42
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 4 - las tres del e2-60' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS gravada,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
 WHERE f.ID IN (343, 493, 494)
 ORDER BY f.ID;
