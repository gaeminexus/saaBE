-- =====================================================================
-- CORRECCION: los dos indices de RHH.ODBS quedaron fuera del schema RHH
-- Modulo: RHH  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-03
--
-- QUE PASO
--   El script e2-03 creo los dos indices SIN prefijo de schema:
--
--       CREATE INDEX IX_ODBS_EMPR_ANIO ON RHH.ODBS (...);
--       CREATE UNIQUE INDEX UQ_ODBS_VIVA ON RHH.ODBS (...);
--
--   La TABLA lleva prefijo, el INDICE no. Oracle crea el indice en el schema
--   del usuario que ejecuta, no en el de la tabla. Asi que quedaron fuera de
--   RHH e invisibles a cualquier control que filtre por OWNER = 'RHH'.
--
--   Lo detecto el arbitro de lap-saa-1 revisando el script. Es la misma trampa
--   que ya estaba documentada en tsr/sql/README-ORDEN-PRODUCCION.md.
--
-- POR QUE NO ES COSMETICO
--   UQ_ODBS_VIVA no es un indice de rendimiento: es la REGLA DE NEGOCIO que
--   impide que existan dos ordenes vivas para la misma combinacion de empresa,
--   tipo de beneficio, anio y region. Un indice unico funciona igual este en el
--   schema que este --Oracle lo aplica de todos modos-- PERO:
--
--     1. No aparece en los controles por OWNER, asi que una verificacion de
--        integridad diria que la regla NO existe. Ya paso: el propio BLOQUE 5.6
--        del e2-03 filtra por owner = 'RHH' y devolveria 1 fila en vez de 3.
--     2. Si el schema del ejecutor se borra o se recrea, la regla desaparece
--        con el, sin que nadie lo note hasta que se generen dos ordenes.
--     3. Un DROP TABLE RHH.ODBS no se lleva un indice de otro schema de forma
--        obvia, y queda basura.
--
--   O sea: la regla se aplica hoy por accidente, no por diseno.
--
-- ES SEGURO CORRERLO DE CORRIDO. Bloques de control ANTES y DESPUES, y el
-- reverso comentado. NO borra datos: solo recrea indices.
--
-- CUANDO CORRERLO
--   Antes de que se genere la primera orden de beneficio social. Si ya se
--   generaron ordenes, correrlo IGUAL: el BLOQUE 2 falla ruidoso si existen
--   duplicados, que es exactamente lo que se quiere saber.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 Donde estan realmente los indices de ODBS.
--     ESPERADO: PK_ODBS con owner RHH, y los otros dos con OTRO owner
--     (el del usuario que corrio el e2-03).
--     Si los tres ya dicen RHH, este script NO hace falta: no lo corras.
SELECT owner, index_name, uniqueness, table_owner, table_name
  FROM all_indexes
 WHERE table_name = 'ODBS'
 ORDER BY owner, index_name;

-- 0.2 Cuantas ordenes hay. Si es 0, la recreacion no puede fallar.
SELECT COUNT(*) AS ORDENES_EXISTENTES FROM RHH.ODBS;

-- 0.3 Hay duplicados que impedirian recrear el indice unico?
--     ESPERADO: 0 filas. Si devuelve algo, PARAR: hay dos ordenes vivas para
--     la misma combinacion y hay que decidir cual anular antes de seguir.
SELECT PJRQCDGO, ODBSTPBN, ODBSANOO, NVL(ODBSRGON, -1) AS REGION,
       COUNT(*) AS CUANTAS
  FROM RHH.ODBS
 WHERE ODBSESTD IN (1, 2, 3)
 GROUP BY PJRQCDGO, ODBSTPBN, ODBSANOO, NVL(ODBSRGON, -1)
HAVING COUNT(*) > 1;


-- =====================================================================
-- BLOQUE 1 -- BORRAR LOS INDICES MAL UBICADOS
-- =====================================================================
-- OJO: van SIN prefijo, igual que se crearon. Si tu sesion es la misma que
-- corrio el e2-03, esto los encuentra. Si NO lo es, reemplaza el nombre por
-- el OWNER.INDICE que devolvio el control 0.1.

DROP INDEX IX_ODBS_EMPR_ANIO;
DROP INDEX UQ_ODBS_VIVA;


-- =====================================================================
-- BLOQUE 2 -- RECREARLOS DENTRO DE RHH
-- =====================================================================
-- La diferencia con el e2-03 es el prefijo RHH. en el nombre del indice.

CREATE INDEX RHH.IX_ODBS_EMPR_ANIO ON RHH.ODBS (PJRQCDGO, ODBSANOO, ODBSTPBN);

-- Indice FUNCIONAL a proposito: solo cubre los estados vivos (1,2,3). Una
-- orden ANULADA(4) no debe impedir que se genere otra para la misma
-- combinacion; las anuladas entran como NULL y Oracle no las considera.
CREATE UNIQUE INDEX RHH.UQ_ODBS_VIVA ON RHH.ODBS (
    CASE WHEN ODBSESTD IN (1,2,3) THEN PJRQCDGO END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSTPBN END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSANOO END,
    CASE WHEN ODBSESTD IN (1,2,3) THEN NVL(ODBSRGON, -1) END
);


-- =====================================================================
-- BLOQUE 3 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 3.1 Los tres indices, los tres en RHH. ESPERADO: 3 filas, todas owner RHH.
SELECT owner, index_name, uniqueness
  FROM all_indexes
 WHERE owner = 'RHH' AND table_name = 'ODBS'
 ORDER BY index_name;

-- 3.2 No quedo ninguno huerfano en otro schema. ESPERADO: 0 filas.
SELECT owner, index_name
  FROM all_indexes
 WHERE table_name = 'ODBS' AND owner <> 'RHH';

-- 3.3 El conteo de ordenes no cambio. ESPERADO: el mismo numero que 0.2.
SELECT COUNT(*) AS ORDENES_EXISTENTES FROM RHH.ODBS;


-- =====================================================================
-- BLOQUE 4 -- REVERSO. COMENTADO A PROPOSITO.
-- Solo tiene sentido si algo salio mal a mitad y hay que volver al estado
-- anterior -- que era el estado DEFECTUOSO, asi que rara vez se quiere.
-- =====================================================================
--
-- DROP INDEX RHH.IX_ODBS_EMPR_ANIO;
-- DROP INDEX RHH.UQ_ODBS_VIVA;
-- CREATE INDEX IX_ODBS_EMPR_ANIO ON RHH.ODBS (PJRQCDGO, ODBSANOO, ODBSTPBN);
-- CREATE UNIQUE INDEX UQ_ODBS_VIVA ON RHH.ODBS (
--     CASE WHEN ODBSESTD IN (1,2,3) THEN PJRQCDGO END,
--     CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSTPBN END,
--     CASE WHEN ODBSESTD IN (1,2,3) THEN ODBSANOO END,
--     CASE WHEN ODBSESTD IN (1,2,3) THEN NVL(ODBSRGON, -1) END
-- );
-- =====================================================================


-- =====================================================================
-- LECCION PARA LOS PROXIMOS SCRIPTS DE ESTE EQUIPO
--
--   El prefijo de schema se pone en la TABLA y tambien en el INDICE, la
--   SECUENCIA y el CONSTRAINT. Poner el de la tabla y olvidar el del objeto
--   no da error: crea el objeto en otro lado y todo parece funcionar.
--
--   Es de la misma familia que el resto de lo que costo caro esta semana:
--   un mecanismo que no falla deja de avisar cuando esta equivocado.
-- =====================================================================
