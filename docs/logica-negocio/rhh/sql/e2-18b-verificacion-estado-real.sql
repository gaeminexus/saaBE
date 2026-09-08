-- =====================================================================
-- e2-18b — En que estado quedo realmente el e2-18
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-07
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Se puede correr entero.
--
-- POR QUE EXISTE
--   El BLOQUE 0.3 del e2-18 devolvio filas: los detalles 32 y 33 del rubro 221
--   YA EXISTEN, y con exactamente el texto que el BLOQUE 1 iba a escribir. Eso
--   no es una colision con otro catalogo: es una corrida previa del propio
--   e2-18. Lo que falta saber es si esa corrida llego hasta el BLOQUE 2, que es
--   el que crea los conceptos en RHH.CPNM.
--
--   El e2-18 tiene los dos INSERT guardados con NOT EXISTS, asi que re-correrlo
--   NO duplica. La decision no es "seguir o no": es saber que falta.
-- =====================================================================


-- =====================================================================
-- 1 — Los conceptos. ESTA ES LA PREGUNTA QUE IMPORTA.
-- =====================================================================
-- ESPERADO si el BLOQUE 2 corrio: DOS filas por empresa (rol 32 y rol 33),
-- todas con tipo_debe_ser_5 = 5.
--
-- 🔴 Si tipo_debe_ser_5 trae algo distinto de 5, PARAR y avisar: el conceptono
--    seria informativo, entraria al neto y el decimo se pagaria DOS VECES.
-- Si no devuelve NINGUNA fila: falta correr el BLOQUE 2 del e2-18.
SELECT '1 - conceptos creados' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMALTR,
       c.CPNMTPCN AS tipo_debe_ser_5, c.CPNMROLM AS rol, c.CPNMESTD AS estado
  FROM RHH.CPNM c
 WHERE c.CPNMROLM IN (32, 33)
 ORDER BY c.PJRQCDGO, c.CPNMROLM;


-- =====================================================================
-- 2 — Cuantas empresas faltan
-- =====================================================================
-- Cuenta empresas y conceptos por rol. ESPERADO: creados = empresas, para los
-- dos roles. Si creados < empresas, el BLOQUE 2 corrio a medias.
SELECT '2 - cobertura' AS control,
       (SELECT COUNT(*) FROM SCP.PJRQ)                                  AS empresas,
       (SELECT COUNT(*) FROM RHH.CPNM WHERE CPNMROLM = 32)              AS creados_rol_32,
       (SELECT COUNT(*) FROM RHH.CPNM WHERE CPNMROLM = 33)              AS creados_rol_33
  FROM DUAL;


-- =====================================================================
-- 3 — Que NO haya novedades colgando de estos conceptos
-- =====================================================================
-- ESPERADO: 0. Si fuera > 0, alguien ya uso el concepto y el e2-18 NO se puede
-- revertir con su BLOQUE 5 sin analizar antes que pasa con esas novedades.
SELECT '3 - novedades usando estos conceptos' AS control, COUNT(*) AS deberia_ser_cero
  FROM RHH.NVNM n JOIN RHH.CPNM c ON c.CPNMCDGO = n.CPNMCDGO
 WHERE c.CPNMROLM IN (32, 33);


-- =====================================================================
-- 4 — Que ningun OTRO concepto este pisando los roles 32 y 33
-- =====================================================================
-- Los conceptos del decimo MENSUALIZADO son rol 6 y 7. Esto verifica que no se
-- haya reusado uno de esos por error. ESPERADO: los de rol 32/33 son distintos
-- registros que los de rol 6/7.
SELECT '4 - decimos mensualizados (rol 6 y 7)' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMTPCN AS tipo, c.CPNMROLM AS rol
  FROM RHH.CPNM c
 WHERE c.CPNMROLM IN (6, 7)
 ORDER BY c.PJRQCDGO, c.CPNMROLM;


-- =====================================================================
-- 5 — Hallazgo lateral del BLOQUE 0.2, para dejarlo medido
-- =====================================================================
-- El 0.2 mostro que el rubro 221 tiene alternos 1..16 y 23..33: NO tiene el
-- 17..22. En Java esos seis existen (PROVISION_DECIMO_TERCERO = 17 hasta
-- PROVISION_DESAHUCIO = 22, RhhRolConceptoMotor:43-48).
--
-- Esto es PREVIO a los decimos y NO bloquea el e2-18. Se mide para saber si el
-- desfase importa: si ningun concepto usa esos roles, el catalogo simplemente
-- nunca se lleno y no molesta a nadie.
-- ESPERADO (lo mas probable): 0 filas.
SELECT '5 - conceptos con rol de provision sin catalogo' AS control,
       c.CPNMCDGO, c.PJRQCDGO, c.CPNMNMBR, c.CPNMROLM AS rol
  FROM RHH.CPNM c
 WHERE c.CPNMROLM BETWEEN 17 AND 22
 ORDER BY c.CPNMROLM, c.PJRQCDGO;


-- =====================================================================
-- FIN — nada que commitear, todo fue lectura
-- =====================================================================
