-- =====================================================================================
-- 241 - URGENTE: reapuntar los certificados de los 10 beneficiarios YA CARGADOS
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. Comentarios ARRIBA, nunca intercalados.
-- ⛔ ESTE SCRIPT ESCRIBE, pero SOLO en el bloque 3, que esta COMENTADO.
--
-- =====================================================================================
-- QUE PASO
-- =====================================================================================
-- El control 0.2 del sql/240 decia: "No debe haber ningun beneficiario cargado todavia.
-- Esperado: 0. Si devuelve algo, PARAR". Devolvio 10 y el script siguio.
--
-- ⇒ Hay DIEZ beneficiarios cargados (CRD.CBBP) cuyos certificados quedaron en CRD.ADJN
--   con el tipo 4 ('CERTIFICADO BANCARIO'), que es el de las cuentas bancarias del
--   participe, en vez del tipo nuevo 'CERTIFICADO BANCARIO BENEFICIARIO' (id 58).
--
-- ESO TIENE DOS CONSECUENCIAS, Y LA SEGUNDA ES LA GRAVE:
--
--   1. Con el WAR nuevo, esos diez beneficiarios van a aparecer SIN certificado: el
--      codigo va a buscar el tipo 58 y sus adjuntos tienen el 4. Molesto, no destructivo.
--
--   2. ⛔ AHORA MISMO, las cuentas bancarias de participe cuyo CNBPCDGO coincida con el
--      CBBPCDGO de alguno de esos diez estan devolviendo EL CERTIFICADO DEL BENEFICIARIO.
--      selectByReferenciaYTipo ordena por fechaRegistro DESC y toma el primero, y los de
--      beneficiarios son de hoy: GANAN. Es un certificado de otra persona mostrandose en
--      la cuenta bancaria de un participe. Esto ya esta vivo, sin el WAR nuevo.
--
-- ⭐ LO QUE SALVA LA MIGRACION: las dos rutinas guardan en CARPETAS DISTINTAS.
--      CuentaBancariaParticipeServiceImpl:57   -> 'crd/certificados-bancarios'
--      CuentaBancariaBeneficiarioServiceImpl:57 -> 'crd/certificados-beneficiarios'
--   La URL del archivo (ADJNURLA) los distingue de forma determinista. No hay que
--   adivinar por fecha ni por codigo.
-- =====================================================================================


-- =====================================================================================
-- 1. EL DAÑO, MEDIDO
-- =====================================================================================

-- 1.1 Los adjuntos de BENEFICIARIOS que hoy tienen el tipo equivocado.
--     Esperado: hasta 10 filas (uno por beneficiario cargado con certificado).
SELECT a.ADJNCDGO, a.ADJNIDRF AS ID_BENEFICIARIO, a.TPDJCDGO AS TIPO_ACTUAL,
       a.ADJNNMAR AS ARCHIVO, a.ADJNFCRG, a.ADJNIDST
  FROM CRD.ADJN a
 WHERE a.ADJNURLA LIKE '%certificados-beneficiarios%'
 ORDER BY a.ADJNIDRF;

-- 1.2 ⛔ LA COLISION VIVA: cuentas bancarias de participe que comparten codigo con uno de
--     esos beneficiarios. Para CADA fila de aca, la pantalla de cuentas bancarias puede
--     estar mostrando el certificado del beneficiario en vez del suyo.
--     Esperado: idealmente 0 filas. Cada fila es un participe viendo un PDF ajeno.
SELECT c.CNBPCDGO AS CUENTA_BANCARIA, c.ENTDCDGO AS PARTICIPE_DE_LA_CUENTA,
       b.CBBPCDGO AS BENEFICIARIO_MISMO_CODIGO, b.ENTDCDGO AS PARTICIPE_DEL_BENEFICIARIO,
       b.CBBPNMBR AS NOMBRE_BENEFICIARIO
  FROM CRD.CNBP c
  JOIN CRD.CBBP b ON b.CBBPCDGO = c.CNBPCDGO
 ORDER BY c.CNBPCDGO;

-- 1.3 Y el contraste: para esos mismos codigos, que adjuntos hay con el tipo 4.
--     Si una cuenta bancaria tiene DOS adjuntos con el mismo ADJNIDRF y tipo 4 (el suyo
--     y el del beneficiario), el mas nuevo es el que se muestra.
SELECT a.ADJNIDRF, COUNT(*) AS ADJUNTOS_TIPO_4,
       SUM(CASE WHEN a.ADJNURLA LIKE '%certificados-beneficiarios%' THEN 1 ELSE 0 END) AS DE_BENEFICIARIO,
       SUM(CASE WHEN a.ADJNURLA LIKE '%certificados-bancarios%'     THEN 1 ELSE 0 END) AS DE_CUENTA
  FROM CRD.ADJN a
 WHERE a.TPDJCDGO = 4
   AND a.ADJNIDRF IN (SELECT b.CBBPCDGO FROM CRD.CBBP b)
 GROUP BY a.ADJNIDRF
 ORDER BY a.ADJNIDRF;


-- =====================================================================================
-- 2. CONTROL PREVIO A LA MIGRACION
-- =====================================================================================

-- 2.1 El tipo destino tiene que existir, activo y unico. Esperado: 1 fila (el id 58).
SELECT t.TPDJCDGO, t.TPDJNMBR, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO'
   AND t.TPDJIDST = 1;

-- 2.2 Ningun adjunto de la carpeta de beneficiarios debe tener ya el tipo nuevo.
--     Esperado: 0. Si no, parte de la migracion ya corrio.
SELECT COUNT(*) AS YA_MIGRADOS
  FROM CRD.ADJN a
 WHERE a.ADJNURLA LIKE '%certificados-beneficiarios%'
   AND a.TPDJCDGO = (SELECT MIN(t.TPDJCDGO) FROM CRD.TPDJ t
                      WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO'
                        AND t.TPDJIDST = 1);


-- =====================================================================================
-- 3. LA MIGRACION — COMENTADA. Descomentar solo despues de leer el bloque 1.
-- =====================================================================================
-- Reapunta al tipo propio SOLO los adjuntos que estan en la carpeta de beneficiarios.
-- No toca ni un adjunto de cuentas bancarias: el filtro por carpeta es exacto, no
-- heuristico. No borra nada, no mueve ningun archivo del disco.
--
-- Esperado: tantas filas actualizadas como devolvio el bloque 1.1.
--
-- UPDATE CRD.ADJN a
--    SET a.TPDJCDGO = (SELECT MIN(t.TPDJCDGO) FROM CRD.TPDJ t
--                       WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO BENEFICIARIO'
--                         AND t.TPDJIDST = 1)
--  WHERE a.ADJNURLA LIKE '%certificados-beneficiarios%'
--    AND a.TPDJCDGO = 4;
--
-- COMMIT;


-- =====================================================================================
-- 4. CONTROLES POSTERIORES
-- =====================================================================================

-- 4.1 Todos los de la carpeta de beneficiarios con el tipo nuevo. Esperado: 0 en TIPO_4.
SELECT SUM(CASE WHEN a.TPDJCDGO = 4 THEN 1 ELSE 0 END)                 AS TIPO_4,
       SUM(CASE WHEN a.TPDJCDGO <> 4 THEN 1 ELSE 0 END)                AS TIPO_NUEVO,
       COUNT(*)                                                        AS TOTAL
  FROM CRD.ADJN a
 WHERE a.ADJNURLA LIKE '%certificados-beneficiarios%';

-- 4.2 Y que NINGUN adjunto de cuentas bancarias se haya movido. Esperado: todos en 4.
SELECT SUM(CASE WHEN a.TPDJCDGO = 4 THEN 1 ELSE 0 END)                 AS TIPO_4,
       SUM(CASE WHEN a.TPDJCDGO <> 4 THEN 1 ELSE 0 END)                AS MOVIDOS_POR_ERROR,
       COUNT(*)                                                        AS TOTAL
  FROM CRD.ADJN a
 WHERE a.ADJNURLA LIKE '%certificados-bancarios%';

-- 4.3 La colision del 1.3 ya no existe: para los codigos de beneficiario, cada ADJNIDRF
--     con tipo 4 debe tener SOLO el adjunto de la cuenta bancaria (o ninguno).
SELECT a.ADJNIDRF, COUNT(*) AS ADJUNTOS_TIPO_4,
       SUM(CASE WHEN a.ADJNURLA LIKE '%certificados-beneficiarios%' THEN 1 ELSE 0 END) AS DE_BENEFICIARIO
  FROM CRD.ADJN a
 WHERE a.TPDJCDGO = 4
   AND a.ADJNIDRF IN (SELECT b.CBBPCDGO FROM CRD.CBBP b)
 GROUP BY a.ADJNIDRF
 ORDER BY a.ADJNIDRF;


-- =====================================================================================
-- 5. DESPUES DE ESTO, EN LA PANTALLA
-- =====================================================================================
-- 1. Abrir una cuenta bancaria de participe cuyo codigo aparecia en el bloque 1.2 y
--    confirmar que su certificado es EL SUYO. Es la unica verificacion que importa: el
--    SQL dice que los tipos quedaron bien, la pantalla dice que cada uno ve lo suyo.
-- 2. Con el WAR nuevo, abrir los diez beneficiarios y confirmar que su certificado se
--    descarga. Antes de la migracion iban a aparecer sin certificado.


-- =====================================================================================
-- 6. REVERSO — comentado
-- =====================================================================================
-- Devuelve los adjuntos de beneficiarios al tipo 4. Solo si algo salio mal:
--
-- UPDATE CRD.ADJN a SET a.TPDJCDGO = 4
--  WHERE a.ADJNURLA LIKE '%certificados-beneficiarios%';
-- COMMIT;
