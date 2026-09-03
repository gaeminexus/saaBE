-- =====================================================================
-- URGENTE — PGS.APLP.APLPFCTC debe aceptar NULL
-- Modulo: CXP  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-03
--
-- EL SINTOMA, reportado por el usuario en produccion
--   Cruzar un anticipo de proveedor con una LIQUIDACION de compra falla:
--
--     ORA-01400: no se puede realizar una insercion NULL en
--                ("PGS"."APLP"."APLPFCTC")
--     en AplicacionPagoCxpServiceImpl.aplicarAnticipos
--
-- LA CAUSA
--   El frente de cruce anticipo-liquidacion (cxp/sql/lap1-10, equipo lap-saa-1)
--   agrego APLPLQCC como NULLABLE y su propio comentario declara que es
--   «excluyente con APLPFCTC — una aplicacion afecta una factura O una
--   liquidacion». Correcto. Pero APLPFCTC sigue siendo NOT NULL de cuando
--   TODA aplicacion era contra una factura, y no se ajusto esa mitad.
--
--   O sea: se reconocio la exclusion mutua en el comentario y se implemento
--   solo un lado. Una aplicacion contra liquidacion manda APLPFCTC en null y
--   Oracle la rechaza.
--
-- POR QUE VA POR BASE Y NO POR CODIGO
--   El codigo esta bien: la entidad mapea las dos FK como @ManyToOne sin
--   nullable=false, y el service llena una u otra segun el documento. Lo que
--   esta desalineado es la restriccion de la tabla.
--
-- ES SEGURO CORRERLO CON EL SISTEMA ARRIBA
--   Relajar un NOT NULL no reescribe filas ni invalida nada: las existentes ya
--   cumplen la restriccion mas estricta. No hace falta ventana ni redespliegue.
--
-- ⛔ NO se agrega un CHECK de exclusion mutua en este script, a proposito.
--   Seria lo correcto conceptualmente, pero hay SEIS columnas de documento en
--   esta tabla (APLPFCTC, APLPLQCC, APLPNTCC, APLPNTDC, APLPRTNC, APLPRTV2) y
--   un CHECK mal escrito rechazaria cruces que hoy funcionan. Se propone
--   aparte, con el inventario de combinaciones reales medido primero.
-- =====================================================================


-- =====================================================================
-- BLOQUE 0 -- CONTROLES ANTES. Correr y LEER.
-- =====================================================================

-- 0.1 Confirmar el estado actual de las SEIS columnas de documento.
--     ESPERADO: APLPFCTC con NULLABLE = 'N' (la causa del error) y las otras
--     cinco con 'Y'.
--     ⚠️ Si aparece OTRA en 'N', avisar: el mismo error va a repetirse con el
--     documento que la use, y este script solo arregla APLPFCTC.
SELECT column_name, data_type, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'APLP'
   AND column_name IN ('APLPFCTC','APLPLQCC','APLPNTCC','APLPNTDC',
                       'APLPRTNC','APLPRTV2')
 ORDER BY nullable, column_name;

-- 0.2 Cuantas aplicaciones existen hoy, y contra que documento.
--     Sirve de linea base para el control posterior: ninguna debe cambiar.
SELECT COUNT(*)                                                   AS total,
       SUM(CASE WHEN APLPFCTC IS NOT NULL THEN 1 ELSE 0 END)      AS con_factura,
       SUM(CASE WHEN APLPLQCC IS NOT NULL THEN 1 ELSE 0 END)      AS con_liquidacion,
       SUM(CASE WHEN APLPNTCC IS NOT NULL THEN 1 ELSE 0 END)      AS con_nota_credito,
       SUM(CASE WHEN APLPNTDC IS NOT NULL THEN 1 ELSE 0 END)      AS con_nota_debito
  FROM PGS.APLP;


-- =====================================================================
-- BLOQUE 1 -- EL CAMBIO. Una sola linea.
-- =====================================================================

ALTER TABLE PGS.APLP MODIFY (APLPFCTC NULL);

COMMENT ON COLUMN PGS.APLP.APLPFCTC IS
    'Factura de compra afectada. NULLABLE desde 2026-09-03: excluyente con APLPLQCC y las demas FK de documento — una aplicacion afecta UN documento, y desde el cruce anticipo-liquidacion ese documento puede no ser una factura';


-- =====================================================================
-- BLOQUE 2 -- CONTROLES DESPUES. Correr y LEER.
-- =====================================================================

-- 2.1 Las seis columnas de documento, todas nullable. ESPERADO: 6 filas 'Y'.
SELECT column_name, nullable
  FROM all_tab_columns
 WHERE owner = 'PGS' AND table_name = 'APLP'
   AND column_name IN ('APLPFCTC','APLPLQCC','APLPNTCC','APLPNTDC',
                       'APLPRTNC','APLPRTV2')
 ORDER BY column_name;

-- 2.2 Ninguna fila cambio. ESPERADO: los mismos numeros que 0.2.
SELECT COUNT(*)                                                   AS total,
       SUM(CASE WHEN APLPFCTC IS NOT NULL THEN 1 ELSE 0 END)      AS con_factura,
       SUM(CASE WHEN APLPLQCC IS NOT NULL THEN 1 ELSE 0 END)      AS con_liquidacion
  FROM PGS.APLP;

-- 2.3 La tabla sigue valida (ningun indice ni constraint invalidado).
--     ESPERADO: 0 filas.
SELECT constraint_name, status
  FROM all_constraints
 WHERE owner = 'PGS' AND table_name = 'APLP' AND status <> 'ENABLED';


-- =====================================================================
-- BLOQUE 3 -- REVERSO. COMENTADO A PROPOSITO.
-- ⛔ Solo se puede volver atras si NO se creo ninguna aplicacion contra
--    liquidacion. Con una sola fila en APLPLQCC, el ALTER falla — y esta bien
--    que falle: volver al NOT NULL rompe el cruce que este script habilita.
-- =====================================================================
--
-- -- Control previo obligatorio. Debe dar 0 para poder revertir:
-- SELECT COUNT(*) FROM PGS.APLP WHERE APLPFCTC IS NULL;
--
-- ALTER TABLE PGS.APLP MODIFY (APLPFCTC NOT NULL);
-- =====================================================================


-- =====================================================================
-- LO QUE ESTE ERROR ENSEÑA, y es el tercer caso de la misma forma esta semana
--
--   Los otros dos: AVPC.PRSTCDGO/DTPRCDGO (equipo B de crd, mismo ORA-01400) y
--   la advertencia del propio lap1-10 sobre no usar DEFAULT n NOT NULL.
--
--   La forma: cuando una columna pasa de ser OBLIGATORIA a ser UNA DE VARIAS
--   ALTERNATIVAS, el trabajo no es agregar la nueva — es relajar la vieja. El
--   comentario del lap1-10 ya decia «excluyente con APLPFCTC»; faltaba actuar
--   sobre esa frase.
--
--   Control barato para la proxima: al agregar una FK de documento a una tabla
--   que ya tiene otras, listar el NULLABLE de TODAS antes de terminar.
-- =====================================================================
