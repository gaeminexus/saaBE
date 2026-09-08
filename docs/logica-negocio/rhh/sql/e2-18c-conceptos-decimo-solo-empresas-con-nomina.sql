-- =====================================================================
-- e2-18c — Los conceptos «decimo acumulado pagado», SOLO donde corresponde
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ⚠️ NO ES SOLO LECTURA. Inserta filas. Correr el BLOQUE 0 primero y LEERLO.
--
-- 🔴 POR QUE EXISTE ESTE SCRIPT, Y VALE LA PENA LEERLO
--    El BLOQUE 2 del e2-18 hacia FROM SCP.PJRQ sin filtro, dando por sentado que
--    esa tabla eran "las empresas". No lo es: SCP.PJRQ es la tabla de PERSONAS
--    JURIDICAS -- proveedores, clientes, todo. Medido el 2026-09-07: 786 filas.
--    Ese bloque habria creado 1572 conceptos de nomina, dos por cada proveedor
--    y cada cliente, y NO habria dado ningun error: los INSERT eran validos, el
--    NOT EXISTS se cumplia para las 786 y el COMMIT pasaba limpio.
--
--    Lo delato el e2-18b: sus controles 4 y 5 mostraron que TODOS los conceptos
--    de nomina que existen hoy pertenecen a UNA sola empresa, la 1236.
--
--    Es el mismo patron de los tres defectos de dinero de este dia: lo que rompe
--    fuerte se arregla solo; lo que contesta mal en silencio sobrevive.
--
-- QUE HACE
--    Crea los dos conceptos INFORMATIVOS para cada empresa que REALMENTE corre
--    nomina, definida como "tiene conceptos en RHH.CPNM". Ese conjunto se
--    ajusta solo si mañana se da de alta otra empresa.
--
-- ESTADO PREVIO, ya medido con e2-18b (no hace falta volver a mirarlo):
--    - Detalles 32 y 33 del rubro 221: YA EXISTEN (BLOQUE 1 del e2-18 corrio).
--    - Conceptos rol 32 y 33: NO existe ninguno. Es lo que falta.
--    - Novedades colgando de esos conceptos: 0.
--
-- ORDEN: va ANTES del WAR.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 — CONTROL PREVIO. Correr esto solo, LEER, y recien despues seguir.
-- =====================================================================

-- 0.1 Las empresas que de verdad corren nomina.
--     ESPERADO: un puñado de filas (medido el 2026-09-07: UNA, la 1236).
--     🔴 Si esto devolviera decenas o cientos, PARAR: seria la misma confusion
--        que se acaba de corregir, y la cuenta de abajo dice cuanto se va a crear.
SELECT '0.1 - empresas con nomina' AS control,
       c.PJRQCDGO, e.PJRQNMBR, COUNT(*) AS conceptos_existentes
  FROM RHH.CPNM c JOIN SCP.PJRQ e ON e.PJRQCDGO = c.PJRQCDGO
 GROUP BY c.PJRQCDGO, e.PJRQNMBR
 ORDER BY c.PJRQCDGO;

-- 0.2 Cuantas filas va a insertar EN TOTAL este script.
--     ESPERADO: 2 por empresa de 0.1 (medido: 2). Si dice 1572, NO SEGUIR.
SELECT '0.2 - filas que se van a insertar' AS control,
       (SELECT COUNT(DISTINCT PJRQCDGO) FROM RHH.CPNM) * 2 AS total_a_insertar,
       (SELECT COUNT(*) FROM SCP.PJRQ)                     AS personas_juridicas_NO_usar
  FROM DUAL;

-- 0.3 Que los detalles del rubro 221 esten. ESPERADO: las dos filas, 32 y 33.
--     Si faltaran, correr antes el BLOQUE 1 del e2-18 (es idempotente).
SELECT '0.3 - detalles del rubro' AS control, d.PDTRALTR, d.PDTRDSCR
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = 221 AND d.PDTRALTR IN (32, 33)
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- BLOQUE 1 — Los dos conceptos, solo para empresas con nomina
-- =====================================================================
-- CPNMTPCN = 5 es INFORMATIVO (RhhTipoConceptoNomina:26). Es LO QUE IMPIDE que
-- el valor entre al neto: el motor suma solo INGRESO y EGRESO
-- (ProcesoNominaServiceImpl:1078-1080 y :1493-1494). Si estos conceptos se
-- crearan con otro tipo, el decimo se pagaria DOS VECES.
--
-- CPNMESTD = 1 tambien es obligatorio: ConceptoNominaDaoServiceImpl.selectByRolMotor
-- filtra por rolMotor + empresa + estado = 1. Con estado distinto, el motor no
-- lo encuentra y la novedad nunca se crea.
--
-- CPNMCDGO NO se pasa: es IDENTITY (verificado, e2-18 bloque 0.5).
-- Los NOT EXISTS hacen que re-correr esto no duplique nada.

-- ⚠️ CORREGIDO el 2026-09-08: la primera version insertaba solo 7 columnas y
--    RHH.CPNM.CPNMTPCL (tipo de calculo) es NOT NULL — ORA-01400, descubierto
--    al correr el e2-26, que tenia el mismo hueco. Este script nunca se habia
--    corrido, asi que aca no llego a fallar. Ahora el concepto se COPIA desde
--    el decimo mensualizado de la misma empresa (rol 6 para el tercero, rol 7
--    para el cuarto) y se sobrescribe solo lo que cambia: tipo INFORMATIVO,
--    rol de motor, todas las banderas de base en 'N', sin RDEP ni codigo IESS.
--    Ver el comentario largo del BLOQUE 5 del e2-26 para el porque de cada
--    columna. Si una empresa no tuviera rol 6/7, no se le crea nada y el
--    BLOQUE 2 lo muestra con menos filas: avisar.
INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMTPCL, CPNMBSCL,
                      CPNMTPRL, CPNMVLRR, CPNMPRCN, CPNMFRML, CPNMIMIE, CPNMIMIR, CPNMAPFR,
                      CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT, CPNMPTRN, CPNMPRVS, CPNMOBLG,
                      CPNMRCRT, CPNMRDEP, CPNMIESS, CPNMORDN, CPNMESTD, CPNMFCHR, CPNMUSRR, CPNMROLM)
SELECT p.PJRQCDGO,
       'Decimo tercero acumulado pagado',
       'D3ACPG',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       5,                                   -- INFORMATIVO
       p.CPNMTPCL, p.CPNMBSCL, p.CPNMTPRL,  -- copiados del rol 6
       0, 0, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N',   -- ninguna base
       'N', 'N', 'N', 'N',                  -- patronal, provisiona, obligatorio, recortable
       NULL, NULL,                          -- RDEP del SRI y codigo IESS: NO
       NVL((SELECT MAX(c.CPNMORDN) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, SYSTIMESTAMP, 'e2-18c',
       32                                   -- RhhRolConceptoMotor.DECIMO_TERCERO_ACUMULADO_PAGADO
  FROM RHH.CPNM p
 WHERE p.CPNMROLM = 6
   AND NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.PJRQCDGO = p.PJRQCDGO AND c2.CPNMROLM = 32);

INSERT INTO RHH.CPNM (PJRQCDGO, CPNMNMBR, CPNMABRV, CPNMALTR, CPNMTPCN, CPNMTPCL, CPNMBSCL,
                      CPNMTPRL, CPNMVLRR, CPNMPRCN, CPNMFRML, CPNMIMIE, CPNMIMIR, CPNMAPFR,
                      CPNMBSDT, CPNMBSDC, CPNMBSVC, CPNMBSUT, CPNMPTRN, CPNMPRVS, CPNMOBLG,
                      CPNMRCRT, CPNMRDEP, CPNMIESS, CPNMORDN, CPNMESTD, CPNMFCHR, CPNMUSRR, CPNMROLM)
SELECT p.PJRQCDGO,
       'Decimo cuarto acumulado pagado',
       'D4ACPG',
       NVL((SELECT MAX(c.CPNMALTR) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       5,
       p.CPNMTPCL, p.CPNMBSCL, p.CPNMTPRL,  -- copiados del rol 7
       0, 0, NULL,
       'N', 'N', 'N', 'N', 'N', 'N', 'N',
       'N', 'N', 'N', 'N',
       NULL, NULL,
       NVL((SELECT MAX(c.CPNMORDN) FROM RHH.CPNM c WHERE c.PJRQCDGO = p.PJRQCDGO), 0) + 1,
       1, SYSTIMESTAMP, 'e2-18c',
       33                                   -- RhhRolConceptoMotor.DECIMO_CUARTO_ACUMULADO_PAGADO
  FROM RHH.CPNM p
 WHERE p.CPNMROLM = 7
   AND NOT EXISTS (SELECT 1 FROM RHH.CPNM c2
                    WHERE c2.PJRQCDGO = p.PJRQCDGO AND c2.CPNMROLM = 33);

COMMIT;


-- =====================================================================
-- BLOQUE 2 — CONTROL POSTERIOR. Correrlo SIEMPRE.
-- =====================================================================

-- 2.1 Los conceptos creados.
--     ESPERADO: DOS filas por empresa de 0.1 (medido: dos, ambas de la 1236).
--     🔴 tipo_debe_ser_5 tiene que decir 5 en TODAS. Si alguna trae otra cosa,
--        el decimo entraria al neto y se pagaria DOS VECES: corregir ANTES del WAR.
--     🔴 estado_debe_ser_1 tiene que decir 1 en todas, o el motor no las encuentra.
SELECT '2.1 - conceptos creados' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMABRV, c.CPNMALTR,
       c.CPNMTPCN AS tipo_debe_ser_5,
       c.CPNMROLM AS rol,
       c.CPNMESTD AS estado_debe_ser_1
  FROM RHH.CPNM c
 WHERE c.CPNMROLM IN (32, 33)
 ORDER BY c.PJRQCDGO, c.CPNMROLM;

-- 2.2 Que no se haya desbordado a personas juridicas que no son empresas.
--     ESPERADO: creados = empresas_con_nomina, para los dos roles.
--     🔴 Si creados fuera 786, se corrio el bloque viejo: ir al BLOQUE 3.
SELECT '2.2 - cobertura' AS control,
       (SELECT COUNT(DISTINCT PJRQCDGO) FROM RHH.CPNM)          AS empresas_con_nomina,
       (SELECT COUNT(*) FROM RHH.CPNM WHERE CPNMROLM = 32)      AS creados_rol_32,
       (SELECT COUNT(*) FROM RHH.CPNM WHERE CPNMROLM = 33)      AS creados_rol_33
  FROM DUAL;

-- 2.3 Que el alterno no haya chocado con uno existente dentro de la misma empresa.
--     ESPERADO: 0 filas.
SELECT '2.3 - alternos duplicados por empresa' AS control,
       c.PJRQCDGO, c.CPNMALTR, COUNT(*) AS veces
  FROM RHH.CPNM c
 GROUP BY c.PJRQCDGO, c.CPNMALTR
HAVING COUNT(*) > 1
 ORDER BY c.PJRQCDGO, c.CPNMALTR;


-- =====================================================================
-- BLOQUE 3 — REVERSO. COMENTADO A PROPOSITO.
-- =====================================================================
-- Control antes de borrar (esto SI es lectura). Tiene que dar 0, o hay
-- novedades colgando y borrar el concepto las deja huerfanas:
--   SELECT COUNT(*) FROM RHH.NVNM n JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
--    WHERE c.CPNMROLM IN (32, 33);
--
-- DELETE FROM RHH.CPNM WHERE CPNMROLM IN (32, 33);
-- COMMIT;
--
-- Los detalles 32/33 del rubro 221 NO se borran aca: son catalogo y no molestan.
-- Si hiciera falta, esta el BLOQUE 5 del e2-18.


-- =====================================================================
-- FIN
-- =====================================================================
