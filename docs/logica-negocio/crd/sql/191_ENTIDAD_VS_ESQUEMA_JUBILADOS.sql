-- =====================================================================================
-- 191 - Entidad vs esquema del circuito de jubilados - CORRER ANTES DE DESPLEGAR
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila. Se puede correr en horario laboral.
--
-- PARA QUE: Hibernate incluye TODA columna @Column en el SELECT que genera. Una columna
-- mapeada en la entidad Java que NO exista en la base rompe CUALQUIER lectura de esa
-- entidad con ORA-00904 - y no se ve al compilar: aparece cuando un usuario abre la
-- pantalla. Es la verificacion que corresponde antes de un despliegue.
--
-- QUE CUBRE: las dos tablas del circuito de pago a jubilados, CRD.PGPC y CRD.VPPC, que
-- son las que leen y escriben los endpoints recien entregados (generarPagosDelMes,
-- porPeriodo, porEntidad, sincronizarPagos).
--
-- COMO SE LEE: cada bloque devuelve las columnas mapeadas en Java que la base NO tiene.
-- LO CORRECTO ES QUE LOS DOS PRIMEROS BLOQUES VUELVAN VACIOS.
--
-- Columnas tomadas de PagoPensionComplementaria.java y ValorPagoPensionComplementaria.java
-- el 2026-09-04, incluyendo las FK declaradas con @JoinColumn.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - CRD.PGPC: columnas mapeadas que NO existen en la base
-- ==========================================================================

SELECT m.COLUMNA                                   AS COLUMNA_MAPEADA_QUE_FALTA
  FROM (
        SELECT 'PGPCCDGO' AS COLUMNA FROM DUAL UNION ALL
        SELECT 'ENTDCDGO' FROM DUAL UNION ALL
        SELECT 'FLLLCDGO' FROM DUAL UNION ALL
        SELECT 'PGPCANNO' FROM DUAL UNION ALL
        SELECT 'PGPCMESS' FROM DUAL UNION ALL
        SELECT 'PGPCVLPN' FROM DUAL UNION ALL
        SELECT 'PGPCVLSG' FROM DUAL UNION ALL
        SELECT 'PGPCVLRR' FROM DUAL UNION ALL
        SELECT 'PGPCFCHA' FROM DUAL UNION ALL
        SELECT 'PGPCESTD' FROM DUAL UNION ALL
        SELECT 'PGPCIDPG' FROM DUAL UNION ALL
        SELECT 'PGPCIDAP' FROM DUAL UNION ALL
        SELECT 'PGPCNMAS' FROM DUAL UNION ALL
        SELECT 'PGPCNMDV' FROM DUAL UNION ALL
        SELECT 'PGPCUSRG' FROM DUAL UNION ALL
        SELECT 'PGPCFCRG' FROM DUAL UNION ALL
        SELECT 'PGPCFCPG' FROM DUAL UNION ALL
        SELECT 'PGPCUSAN' FROM DUAL UNION ALL
        SELECT 'PGPCFCAN' FROM DUAL UNION ALL
        SELECT 'PGPCMTAN' FROM DUAL
       ) m
 WHERE NOT EXISTS (SELECT 1
                     FROM ALL_TAB_COLUMNS c
                    WHERE c.OWNER       = 'CRD'
                      AND c.TABLE_NAME  = 'PGPC'
                      AND c.COLUMN_NAME = m.COLUMNA);

--
-- (!) VACIO = correcto. La entidad y la tabla coinciden.
-- (!) Cualquier fila aca ROMPE toda lectura de PGPC con ORA-00904: se cae porPeriodo,
--     porEntidad y la corrida entera. PARAR el despliegue y avisar.
--

-- ==========================================================================
-- BLOQUE 2 - CRD.VPPC: columnas mapeadas que NO existen en la base
-- ==========================================================================

SELECT m.COLUMNA                                   AS COLUMNA_MAPEADA_QUE_FALTA
  FROM (
        SELECT 'VPPCCDGO' AS COLUMNA FROM DUAL UNION ALL
        SELECT 'ENTDCDGO' FROM DUAL UNION ALL
        SELECT 'VPPCVLRR' FROM DUAL UNION ALL
        SELECT 'VPPCNMCT' FROM DUAL UNION ALL
        SELECT 'VPPCTNPR' FROM DUAL UNION ALL
        SELECT 'VPPCVLSR' FROM DUAL UNION ALL
        SELECT 'VPPCIDST' FROM DUAL UNION ALL
        SELECT 'VPPCUSRG' FROM DUAL UNION ALL
        SELECT 'VPPCFCRG' FROM DUAL UNION ALL
        SELECT 'VPPCUSMD' FROM DUAL UNION ALL
        SELECT 'VPPCCFMD' FROM DUAL
       ) m
 WHERE NOT EXISTS (SELECT 1
                     FROM ALL_TAB_COLUMNS c
                    WHERE c.OWNER       = 'CRD'
                      AND c.TABLE_NAME  = 'VPPC'
                      AND c.COLUMN_NAME = m.COLUMNA);

--
-- (!) VACIO = correcto.
-- (!) Una fila aca rompe el padron de jubilados, que es de donde sale cuanto cobra cada
--     uno. Sin eso no hay corrida posible.
--

-- ==========================================================================
-- BLOQUE 3 - Al reves: columnas que la base tiene y Java NO mapea
-- ==========================================================================
-- Esto NO rompe nada: Hibernate ignora lo que no mapea. Es informativo, y sirve para
-- detectar una columna agregada por otro equipo que este frente todavia no conoce.

SELECT c.TABLE_NAME                                AS TABLA,
       c.COLUMN_NAME                               AS COLUMNA_SIN_MAPEAR,
       c.DATA_TYPE                                 AS TIPO,
       c.NULLABLE                                  AS ACEPTA_NULL
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER = 'CRD'
   AND c.TABLE_NAME IN ('PGPC','VPPC')
   AND c.COLUMN_NAME NOT IN (
        'PGPCCDGO','ENTDCDGO','FLLLCDGO','PGPCANNO','PGPCMESS','PGPCVLPN','PGPCVLSG',
        'PGPCVLRR','PGPCFCHA','PGPCESTD','PGPCIDPG','PGPCIDAP','PGPCNMAS','PGPCNMDV',
        'PGPCUSRG','PGPCFCRG','PGPCFCPG','PGPCUSAN','PGPCFCAN','PGPCMTAN',
        'VPPCCDGO','VPPCVLRR','VPPCNMCT','VPPCTNPR','VPPCVLSR','VPPCIDST','VPPCUSRG',
        'VPPCFCRG','VPPCUSMD','VPPCCFMD')
 ORDER BY c.TABLE_NAME, c.COLUMN_ID;

--
-- (!) Una columna aca con ACEPTA_NULL = 'N' Y SIN DEFAULT es un problema distinto y
--     serio: los INSERT de Hibernate no la incluyen, asi que la corrida fallaria con
--     ORA-01400 al crear el primer pago. Es lo que paso con CRD.AVPC (H18).
--

-- ==========================================================================
-- BLOQUE 4 - La PK de PGPC: la entidad usa IDENTITY, no secuencia
-- ==========================================================================
-- (!) VERIFICADO EN LA ENTIDAD, no supuesto: PagoPensionComplementaria.java:53-56 declara
-- @GeneratedValue(strategy = GenerationType.IDENTITY). NO hay CRD.SQ_PGPCCDGO y no debe
-- buscarse: con IDENTITY es la propia columna la que genera el valor, y Oracle mantiene su
-- secuencia interna. Ojo que esto NO es lo comun en el modulo - las tablas nuevas de crd
-- (CBCR, ACCN, CTAP, VGCN, USAP...) usan @SequenceGenerator con CRD.SQ_XXXXCDGO. PGPC es
-- de las que usan IDENTITY, como la mayoria de las 87 entidades viejas.

SELECT c.COLUMN_NAME                               AS COLUMNA,
       c.IDENTITY_COLUMN                           AS ES_IDENTITY,
       c.DATA_DEFAULT_ON_NULL                      AS DEFAULT_ON_NULL,
       (SELECT NVL(MAX(PGPCCDGO),0) FROM CRD.PGPC) AS MAX_EN_LA_TABLA
  FROM ALL_TAB_COLUMNS c
 WHERE c.OWNER       = 'CRD'
   AND c.TABLE_NAME  = 'PGPC'
   AND c.COLUMN_NAME = 'PGPCCDGO';

--
-- (!) ES_IDENTITY tiene que ser 'YES'. Si dice 'NO', la columna NO se autogenera y el
--     primer insert va a fallar con ORA-01400 sobre la PK, porque Hibernate con IDENTITY
--     no manda valor: espera que lo ponga la base.
-- (!) Con la tabla vacia (MAX_EN_LA_TABLA = 0) no hay riesgo de PK duplicada. La
--     sincronizacion de secuencias solo hace falta cuando se insertaron PKs explicitas,
--     y aca no se inserto nunca ninguna.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================
