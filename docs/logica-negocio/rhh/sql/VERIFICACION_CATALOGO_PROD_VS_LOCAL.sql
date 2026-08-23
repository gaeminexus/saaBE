-- =====================================================
-- MODULO: RHH - EL CATALOGO DE PRODUCCION CONTRA EL DE LOCAL
-- DESCRIPCION: Solo LEE. Cuenta las tablas de configuracion para descartar
--              que algun script de carga se haya ejecutado dos veces.
-- FECHA: 2026-08-21
-- =====================================================
-- POR QUE EXISTE
--   El 2026-08-21 se descubrio que el 05 corrio dos veces en produccion: la
--   segunda pasada tiro PRDNESTD, NMNAESTD y LQDCESTD (ver sql/53). Si el 05
--   corrio dos veces, cualquier otro script pudo hacerlo. Los INSERT no avisan
--   al repetirse: dejan el catalogo duplicado, y un concepto de nomina por
--   duplicado NO da error -- cambia los numeros y parece normal.
-- .
--   Los datos ya estan verificados (SLAP 57, SLDV 22, ACMN 34, MPLD, CNTE,
--   CTRL de los siete meses, DSRC 2 / CTDS 4). Falta el catalogo.
-- .
-- COMO SE USA
--   Correrlo en LOCAL y en PRODUCCION y comparar las dos salidas linea a linea.
--   Local es la base buena: es donde se calibraron los cinco meses.
--   Cualquier tabla con MAS filas en produccion = script repetido. PARAR.
--   Cualquier tabla con MENOS = script que no llego a correr. PARAR.
-- =====================================================

SET PAGESIZE 100
SET LINESIZE 120
COLUMN TABLA FORMAT A10
COLUMN QUE   FORMAT A38

SELECT 'CPNM' TABLA, COUNT(*) FILAS, 'Conceptos de nomina' QUE FROM RHH.CPNM
UNION ALL SELECT 'PRNM', COUNT(*), 'Parametros de nomina'        FROM RHH.PRNM
UNION ALL SELECT 'TBIR', COUNT(*), 'Tabla del impuesto a la renta' FROM RHH.TBIR
UNION ALL SELECT 'CFNM', COUNT(*), 'Configuracion de nomina'     FROM RHH.CFNM
UNION ALL SELECT 'CRGO', COUNT(*), 'Cargos'                      FROM RHH.CRGO
UNION ALL SELECT 'CRGF', COUNT(*), 'Cargos familiares'           FROM RHH.CRGF
UNION ALL SELECT 'DPRT', COUNT(*), 'Departamentos'               FROM RHH.DPRT
UNION ALL SELECT 'DPTC', COUNT(*), 'Departamento-cargo'          FROM RHH.DPTC
UNION ALL SELECT 'DTLL', COUNT(*), 'Detalles'                    FROM RHH.DTLL
UNION ALL SELECT 'GSPR', COUNT(*), 'Grupos de personal'          FROM RHH.GSPR
UNION ALL SELECT 'NXOO', COUNT(*), 'Anexos'                      FROM RHH.NXOO
UNION ALL SELECT 'TPCE', COUNT(*), 'Tipos de contrato'           FROM RHH.TPCE
UNION ALL SELECT 'TPGP', COUNT(*), 'Tipos de grupo'              FROM RHH.TPGP
UNION ALL SELECT 'TRNO', COUNT(*), 'Turnos'                      FROM RHH.TRNO
UNION ALL SELECT 'CSTR', COUNT(*), 'Centros de trabajo'          FROM RHH.CSTR
ORDER BY 1;


-- ====================================================
-- Y el que mas dolerria: conceptos con el mismo codigo alterno.
-- Tiene que devolver CERO filas. Si devuelve alguna, el sql/03 corrio
-- dos veces y hay que borrar los duplicados ANTES de calcular nada:
-- el motor busca por CPNMALTR y con dos filas o coge una al azar o revienta.
-- ====================================================
SELECT CPNMALTR, COUNT(*) AS VECES
  FROM RHH.CPNM
 GROUP BY CPNMALTR
HAVING COUNT(*) > 1
 ORDER BY CPNMALTR;


-- =====================================================
-- CPNMROLM: LA COLUMNA QUE ESTE COTEJO NO MIRABA, Y QUE GOBIERNA ONCE RAMAS
-- Anadido el 2026-08-22, despues de que se colara en produccion.
-- =====================================================
-- QUE PASO. Los roles 17..22 --los seis de provision-- estaban en NULO en
-- produccion: el bloque de UPDATE del sql/11 no surtio efecto y UN UPDATE QUE
-- NO ENCUENTRA FILAS NO DA ERROR. Las provisiones se escribian con
-- PVNM.CPNMCDGO en nulo, es decir SIN CUENTA CONTABLE, porque generaProvision
-- acepta el concepto nulo sin protestar. Se reparo con el sql/54.
-- .
-- POR QUE NO LO CAZO ESTE SCRIPT: contaba filas por tabla y buscaba alternos
-- duplicados, pero no miraba esta columna. Una columna sin CHECK, sin NOT NULL
-- y que el motor consulta once veces tiene que estar en el cotejo.
-- .
-- CONTROL 1: el censo. 31 filas, del 1 al 31, sin huecos ni repetidos.
-- El rol 32 (patronal del finiquito) NO sale: no existe todavia, y es el
-- punto 3 de la lista de correcciones del motor.
-- =====================================================
SELECT CPNMROLM, CPNMALTR, CPNMNMBR
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL
 ORDER BY CPNMROLM;


-- =====================================================
-- CONTROL 2: los huecos, dicho de una vez y sin tener que contar a mano.
-- Tiene que devolver CERO filas.
-- =====================================================
SELECT r.N AS ROL_QUE_FALTA
  FROM (SELECT LEVEL AS N FROM DUAL CONNECT BY LEVEL <= 31) r
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CPNM c
                    WHERE c.PJRQCDGO = :EMPRESA AND c.CPNMROLM = r.N)
 ORDER BY 1;


-- =====================================================
-- CONTROL 3: dos conceptos con el mismo rol. Tiene que devolver CERO filas.
-- Lo impide el indice unico UQ_CPNM_ROLM del sql/11, pero si ese indice no
-- llego a crearse en esta instalacion, aqui se ve.
-- =====================================================
SELECT CPNMROLM, COUNT(*) AS VECES
  FROM RHH.CPNM
 WHERE PJRQCDGO = :EMPRESA AND CPNMROLM IS NOT NULL
 GROUP BY CPNMROLM
HAVING COUNT(*) > 1
 ORDER BY CPNMROLM;


-- =====================================================
-- CONTROL 4: el sintoma aguas abajo. Ninguna provision puede estar sin
-- concepto. Tiene que devolver CERO filas.
-- =====================================================
SELECT p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES, v.PVNMTPPR AS TIPO,
       COUNT(*) AS SIN_CONCEPTO
  FROM RHH.PVNM v JOIN RHH.PRDN p ON p.PRDNCDGO = v.PRDNCDGO
 WHERE v.CPNMCDGO IS NULL
 GROUP BY p.PRDNANOO, p.PRDNMSEE, v.PVNMTPPR
 ORDER BY 1, 2, 3;
