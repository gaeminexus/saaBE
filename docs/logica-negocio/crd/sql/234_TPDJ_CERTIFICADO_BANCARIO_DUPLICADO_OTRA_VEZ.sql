-- =====================================================================================
-- 234 - URGENTE: los certificados bancarios YA CARGADOS salen como "no cargados"
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- ⛔ ESTE SCRIPT SOLO ESCRIBE EN EL PASO 4, QUE ESTA COMENTADO. Los pasos 1 a 3 son
-- SELECT: correrlos y LEERLOS antes de descomentar nada. El PASO 2 decide todo.
--
-- =====================================================================================
-- EL SINTOMA
-- =====================================================================================
-- Reportado por el usuario el 2026-09-22: en la ficha del participe, los certificados
-- de las cuentas bancarias QUE YA SE HABIAN CARGADO aparecen como si no existieran.
--
-- =====================================================================================
-- LA CAUSA MAS PROBABLE, Y COMO SE LLEGO ACA (hipotesis a confirmar con el PASO 1)
-- =====================================================================================
-- CRD.TPDJ tiene MAS DE UNA fila activa llamada 'CERTIFICADO BANCARIO'.
--
-- El backend resuelve ese tipo POR NOMBRE y, desde la correccion del 2026-09-04, exige
-- EXACTAMENTE UNA fila activa (CuentaBancariaParticipeServiceImpl.resolverTipoCertificado
-- Bancario:264-285). Con dos o mas lanza ERR_TIPO_ADJUNTO_NO_CONFIGURADO. Y como
-- obtenerCertificado:239 llama a ese resolver ANTES de buscar el adjunto, la consulta
-- falla para TODAS las cuentas -> la pantalla las muestra a todas sin certificado.
--
-- ⚠️ ESTO YA PASO EL 2026-09-04 (ver sql/193), por la MISMA causa:
-- CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql trae un control previo que dice textual
-- "esperado: 0 filas. Si devuelve algo, NO correr el INSERT - ya existe", pero el INSERT
-- que sigue NO TIENE GUARDA: es un INSERT plano. Si alguien corre el script de corrido,
-- inserta un duplicado. En septiembre creo el id 37 junto al 4; ahora volvio a pasar.
-- El control esta bien escrito: lo que falla es que depende de que un humano lo lea.
--   ⇒ Por eso este cambio corrige TAMBIEN el script de carga (INSERT con NOT EXISTS),
--     para que no pueda volver a pasar una cuarta vez.
--
-- ⛔⛔ NO ES SOLO COSMETICO: el mismo resolver gobierna
--   - crearConCertificado -> no se puede cargar NINGUN certificado nuevo, y
--   - el certificado como condicion para que salga dinero a los jubilados
--     (§6/D2 del contrato de pago de pension complementaria).
-- Mientras esto este roto, revisar antes de correr cualquier pago de jubilados.
-- =====================================================================================


-- =====================================================================================
-- PASO 1 - CONFIRMAR LA CAUSA. Esperado si la hipotesis es correcta: 2 o mas filas.
-- =====================================================================================
-- Esta es exactamente la consulta que hace el backend (TipoAdjuntoDaoServiceImpl:25-31):
-- igualdad exacta sobre el nombre, y solo activas.

SELECT t.TPDJCDGO, t.TPDJNMBR, t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(t.TPDJNMBR) = UPPER('CERTIFICADO BANCARIO')
   AND t.TPDJIDST = 1
 ORDER BY t.TPDJCDGO;

--
-- COMO SE LEE:
--   2 o mas filas -> CAUSA CONFIRMADA. Seguir al PASO 2.
--   exactamente 1 -> NO es esto. PARAR y avisar: el sintoma tiene otra causa y hay que
--                    volver a diagnosticar (mirar el log de WildFly buscando
--                    ERR_TIPO_ADJUNTO_NO_CONFIGURADO o el 404 de /cnbp/{id}/certificado).
--   0 filas       -> tampoco es un duplicado: FALTA la fila, o quedo INACTIVA. Ver el
--                    PASO 1b y, en ese caso, la correccion es reactivar la que
--                    corresponda (no insertar otra: mirar primero el PASO 2).
--

-- PASO 1b - Diagnostico ampliado: TODO lo que se parezca, con su estado y su grafia.
--           Sirve para el caso de 0 filas y para detectar nombres con espacios o sufijos.

SELECT t.TPDJCDGO,
       '[' || t.TPDJNMBR || ']'                              AS NOMBRE_ENTRE_CORCHETES,
       LENGTH(t.TPDJNMBR)                                    AS LARGO,
       t.TPDJIDST
  FROM CRD.TPDJ t
 WHERE UPPER(t.TPDJNMBR) LIKE '%CERTIFICAD%'
 ORDER BY t.TPDJIDST DESC, t.TPDJCDGO;

--
-- ⚠️ El nombre va entre corchetes a proposito: un espacio al final ('CERTIFICADO
--    BANCARIO ') se ve identico en una grilla y NO matchea la igualdad exacta del
--    backend. Si el LARGO no es 20, hay un espacio o un caracter de mas.
--


-- =====================================================================================
-- PASO 2 - (!) DECIDE TODO: cual de los tipos usan los certificados REALES
-- =====================================================================================
-- Sin esto se puede desactivar la fila equivocada y dejar el sintoma igual: el error
-- desaparece, el backend resuelve un tipo... y sigue sin encontrar los adjuntos, porque
-- los adjuntos viejos apuntan al OTRO.

SELECT a.TPDJCDGO                                            AS TIPO_USADO,
       COUNT(*)                                              AS ADJUNTOS,
       SUM(CASE WHEN a.ADJNIDST = 1 THEN 1 ELSE 0 END)       AS ACTIVOS,
       COUNT(DISTINCT a.ADJNIDRF)                            AS CUENTAS_DISTINTAS,
       MIN(a.ADJNFCRG)                                       AS DESDE,
       MAX(a.ADJNFCRG)                                       AS HASTA
  FROM CRD.ADJN a
 WHERE a.TPDJCDGO IN (SELECT t.TPDJCDGO
                        FROM CRD.TPDJ t
                       WHERE UPPER(t.TPDJNMBR) LIKE '%CERTIFICAD%BANCARIO%')
 GROUP BY a.TPDJCDGO
 ORDER BY ADJUNTOS DESC;

--
-- COMO SE LEE:
--   UNA sola fila  -> ese TPDJCDGO es EL BUENO. Los demas son duplicados a desactivar.
--                     Es el caso esperado y el que resuelve el PASO 4.
--   DOS o mas      -> hay certificados repartidos entre varios tipos. ⛔ NO desactivar
--                     nada todavia: primero hay que reapuntar los del duplicado al bueno,
--                     y eso es otra decision y otro script. AVISAR al arbitro con esta
--                     salida pegada.
--   NINGUNA fila   -> no hay un solo certificado cargado en la base. Entonces el
--                     duplicado no explica el sintoma "los que ya se habian cargado":
--                     AVISAR, porque significa que los adjuntos se perdieron por otro
--                     motivo y eso es mas grave que un catalogo duplicado.
--


-- =====================================================================================
-- PASO 3 - Contexto: cuantas cuentas y cuantos adjuntos hay en total
-- =====================================================================================

SELECT (SELECT COUNT(*) FROM CRD.CNBP)                                   AS CUENTAS_CNBP,
       (SELECT COUNT(*) FROM CRD.CNBP c WHERE c.CNBPIDST = 1)            AS CUENTAS_ACTIVAS,
       (SELECT COUNT(*) FROM CRD.ADJN)                                   AS ADJUNTOS_TOTALES
  FROM DUAL;


-- =====================================================================================
-- PASO 4 - (!) LA CORRECCION. COMENTADA. No descomentar sin haber leido el PASO 2.
-- =====================================================================================
-- Desactiva los tipos DUPLICADOS y deja activo SOLO el que usan los adjuntos reales.
-- No borra nada: TPDJIDST = 0 es reversible, y no se toca ni un solo adjunto.
--
-- ⛔ REEMPLAZAR <<TIPO_BUENO>> por el TPDJCDGO que el PASO 2 mostro CON adjuntos.
--    Si el PASO 2 no devolvio ninguna fila (nunca se cargo un certificado), conservar
--    el de MENOR TPDJCDGO, que es el original del catalogo, y desactivar los demas.
--
-- UPDATE CRD.TPDJ
--    SET TPDJIDST = 0
--  WHERE UPPER(TPDJNMBR) = UPPER('CERTIFICADO BANCARIO')
--    AND TPDJIDST = 1
--    AND TPDJCDGO <> <<TIPO_BUENO>>;
--
-- -- Esperado: tantas filas actualizadas como duplicados haya (normalmente 1).
-- COMMIT;


-- =====================================================================================
-- PASO 5 - CONTROL POSTERIOR. Correr DESPUES del paso 4.
-- =====================================================================================

SELECT COUNT(*)                                              AS ACTIVOS_CON_ESE_NOMBRE
  FROM CRD.TPDJ t
 WHERE UPPER(t.TPDJNMBR) = UPPER('CERTIFICADO BANCARIO')
   AND t.TPDJIDST = 1;

--
-- (!) TIENE QUE DAR EXACTAMENTE 1.
--     0 -> el backend lanza ERR_TIPO_ADJUNTO_NO_CONFIGURADO y nadie puede ver ni cargar
--          un certificado: se desactivaron todos. Reactivar con el PASO 6.
--     2+ -> sigue roto, el UPDATE no alcanzo.
--
-- Y DESPUES: abrir la ficha de un participe que TENGA certificado cargado y confirmar
-- que aparece. El control de arriba dice que el catalogo quedo bien; solo la pantalla
-- dice que el usuario recupero lo suyo.
--

-- =====================================================================================
-- PASO 6 - REVERSO, comentado
-- =====================================================================================
-- Si algo salio mal, reactivar el que se desactivo:
--
-- UPDATE CRD.TPDJ SET TPDJIDST = 1 WHERE TPDJCDGO = <<EL_QUE_SE_DESACTIVO>>;
-- COMMIT;
--
-- Este script NO modifica un solo adjunto en ningun caso.


-- =====================================================================================
-- ✅ MEDIDO EN PRODUCCION — 2026-09-22. CAUSA CONFIRMADA Y RESUELTA.
-- =====================================================================================
-- Salida real de los pasos 1, 1b y 2, pegada acá para que no haya que volver a medirla:
--
-- PASO 1 (activas con igualdad exacta)      -> DOS filas:
--     4    CERTIFICADO BANCARIO   1
--    38    CERTIFICADO BANCARIO   1     <- el que creo el INSERT del 2026-09-22
--
-- PASO 1b (todo lo que se parece)           -> ademas:
--    37    [CERTIFICADO BANCARIO]  largo 20, estado 0   <- el duplicado de septiembre,
--                                                          ya desactivado por el sql/193
--    20/24/31  otros certificados de otra cosa, no matchean la igualdad exacta: no molestan
--    ⇒ Los tres 'CERTIFICADO BANCARIO' tienen LARGO 20: ninguno tiene espacios de mas.
--
-- PASO 2 (cual usan los adjuntos REALES)    -> UNA sola fila:
--     TIPO_USADO 4 | ADJUNTOS 410 | ACTIVOS 394 | CUENTAS 273
--     DESDE 2025-02-05 17:54 | HASTA 2026-09-21 16:09
--
-- ⇒ DECISION, sin ambiguedad: EL BUENO ES EL 4. El 38 no tiene UN SOLO adjunto.
--   Y el HASTA del 4 (ayer 16:09) confirma que el sistema venia funcionando con el 4
--   hasta que aparecio el 38: nada que reapuntar, nada que migrar.
-- =====================================================================================

-- PASO 4 RESUELTO — este es el UPDATE que corresponde, ya sin placeholders.
-- Esperado: 1 fila actualizada.

-- UPDATE CRD.TPDJ
--    SET TPDJIDST = 0
--  WHERE TPDJCDGO = 38
--    AND UPPER(TRIM(TPDJNMBR)) = 'CERTIFICADO BANCARIO';
--
-- COMMIT;

-- Y el control posterior (PASO 5) tiene que devolver EXACTAMENTE 1:
--
-- SELECT COUNT(*) AS ACTIVOS_CON_ESE_NOMBRE
--   FROM CRD.TPDJ t
--  WHERE UPPER(t.TPDJNMBR) = UPPER('CERTIFICADO BANCARIO')
--    AND t.TPDJIDST = 1;
--
-- NO se toca el 37 (ya esta en 0) ni ninguno de los 410 adjuntos.
