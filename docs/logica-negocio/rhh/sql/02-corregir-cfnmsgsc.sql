-- =====================================================================
-- RRHH: RHH.CFNM.CFNMSGSC tiene una letra donde el codigo espera un numero
-- Modulo: RHH
-- Fecha:  2026-08-27
-- Autor:  orquestador (encontrado probando anticipos a trabajadores)
--
-- EL SINTOMA
--   Al aprobar un anticipo a trabajador, el servidor devuelve:
--     Could not extract column [15] from JDBC ResultSet
--     [Fallo al convertir a representacion interna]
--   El stack apunta a ConfiguracionNominaDaoServiceImpl.selectByEmpresa.
--
-- LA CAUSA
--   RHH.CFNM.CFNMSGSC es VARCHAR2(2) y contiene la letra 'R'.
--   La entidad ConfiguracionNomina la mapea como  private Long
--   seguroSocialIess. Hibernate intenta convertir 'R' a numero y revienta.
--
--   El rubro que gobierna ese campo (RhhCodigoSeguroSocialIess) solo admite
--   dos valores, y los dos son numericos:
--     1 = Ley de seguro social vigente
--     2 = Seguro mixto
--   Asi que 'R' no es un valor valido de ese catalogo bajo ninguna lectura.
--
-- POR QUE IMPORTA MUCHO MAS QUE LOS ANTICIPOS
--   El anticipo solo fue lo primero que toco ese camino. La MISMA carga de
--   ConfiguracionNomina la hacen:
--     ContabilizacionNominaServiceImpl   (contabilizacion de la nomina)
--     CalculoUtilidadesServiceImpl
--     ProvisionActuarialServiceImpl
--     ExportacionNovedadesIessServiceImpl
--     AsientoContableServiceImpl
--   Con 'R' en esa columna, TODOS fallan igual. Es decir: mientras ese
--   valor este ahi, la contabilizacion de la nomina no puede correr.
--
--   >>> COMPROBAR EN PRODUCCION ANTES QUE NADA (bloque 0). Si produccion
--   >>> tambien tiene una letra, la nomina esta rota alli y no se ha
--   >>> notado porque no se ha vuelto a contabilizar desde entonces.
--
-- QUE VALOR PONER
--   Es una decision de RRHH, no tecnica. El codigo mismo, cuando el campo
--   viene nulo, asume LEY_DE_SEGURO_SOCIAL_VIGENTE (1) --ver
--   ExportacionNovedadesIessServiceImpl:430-432-- asi que 1 es el valor
--   coherente con el comportamiento actual. Si RRHH declara seguro mixto,
--   poner 2.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: diagnostico. EJECUTAR PRIMERO EN PRODUCCION.
--   Si CFNMSGSC no es NULL y no es '1' ni '2', la nomina esta rota.
-- ---------------------------------------------------------------------
SELECT CFNMCDGO, PJRQCDGO, CFNMSGSC,
       CASE WHEN CFNMSGSC IS NULL THEN 'OK (nulo, el codigo asume 1)'
            WHEN CFNMSGSC IN ('1','2') THEN 'OK'
            ELSE 'ROTO: la nomina no puede contabilizar' END AS DIAGNOSTICO
  FROM RHH.CFNM;

-- ---------------------------------------------------------------------
-- BLOQUE 1: correccion del dato
--   Cambiar a '2' si RRHH declara seguro mixto.
--   El WHERE es defensivo: solo toca las filas que estan mal.
-- ---------------------------------------------------------------------
UPDATE RHH.CFNM
   SET CFNMSGSC = '1'
 WHERE CFNMSGSC IS NOT NULL
   AND CFNMSGSC NOT IN ('1','2');

-- Control: debe devolver 1 fila afectada si estaba en 'R', 0 si ya estaba bien
SELECT CFNMCDGO, CFNMSGSC FROM RHH.CFNM;

-- ---------------------------------------------------------------------
-- BLOQUE 2: que no vuelva a pasar
--   La columna es VARCHAR2(2) y la entidad la lee como numero. Mientras
--   siga siendo texto, cualquiera puede escribir una letra y volver a
--   romper la nomina, otra vez en silencio y otra vez lejos del origen.
--   El CHECK es la red minima; cambiar la columna a NUMBER seria mas
--   limpio pero obliga a tocar la entidad y no vale la pena hoy.
-- ---------------------------------------------------------------------
ALTER TABLE RHH.CFNM ADD CONSTRAINT CK_CFNM_SGSC
    CHECK (CFNMSGSC IS NULL OR CFNMSGSC IN ('1','2'));

COMMENT ON COLUMN RHH.CFNM.CFNMSGSC IS 'Codigo de seguro social IESS: 1=Ley de seguro social vigente, 2=Seguro mixto. Es VARCHAR2 por herencia pero la entidad lo lee como Long: solo admite digitos, por eso el CHECK';

-- ---------------------------------------------------------------------
-- BLOQUE 3: control final
-- ---------------------------------------------------------------------
SELECT CONSTRAINT_NAME, SEARCH_CONDITION, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'RHH' AND TABLE_NAME = 'CFNM' AND CONSTRAINT_NAME = 'CK_CFNM_SGSC';

COMMIT;

-- ---------------------------------------------------------------------
-- APARTE, PARA NO PERDERLO: la constante del rubro apunta al rubro
-- equivocado. Rubros.RHH_CODIGO_SEGURO_SOCIAL_IESS = 230, pero el rubro
-- 230 en la base es 'RHH CAUSA DE VARIACION DE SUELDO IESS', que es otra
-- cosa. Hoy no rompe nada porque RhhCodigoSeguroSocialIess declara sus dos
-- valores en Java y no los lee de la base, pero el dia que alguien los
-- lea del catalogo va a traer las causas de variacion de sueldo.
-- No se corrige aqui: es cambio de codigo, no de datos.
-- ---------------------------------------------------------------------
