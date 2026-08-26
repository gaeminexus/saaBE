-- ============================================================================
-- 66 - PREVIA DE AGOSTO: QUE LA CONTABILIZACION NO LE FALLE A STEVEN
-- ==
-- Se corre en PRODUCCION antes de dejarle agosto creado. Solo lee.
-- ==
-- POR QUE EXISTE, Y ES LA MISMA HISTORIA DEL FONDO DE RESERVA EN JUNIO.
-- ==
-- Los siete meses de la carga historica corrieron en modo 1, HISTORICO SIN
-- CONTABILIZAR, y los tres asientos quedaron en NULO cada vez. Es decir:
-- EL CAMINO CONTABLE NO SE HA EJECUTADO NUNCA EN PRODUCCION. Siete contrastes
-- en verde no dicen nada sobre el, porque no lo tocaron.
-- ==
-- Agosto es el primero en modo 2, PRODUCTIVO_CONTABILIZA, y ademas lo va a
-- operar el cliente. Un fallo de configuracion aqui no lo vemos nosotros: le
-- sale a el, en mitad de su primer cierre real.
-- ==
-- QUE PUEDE FALLAR, leido de ContabilizacionNominaServiceImpl:
--   CFNMCTMR nula      -> IncomeException "no esta informada para la empresa"
--   CFNMPLRL/PLPR/PLPG nulas -> "no tiene la plantilla del ... asignada"
--   la plantilla no existe   -> "No existe la plantilla contable con codigo
--                               alterno N para la empresa"
--   la plantilla sin lineas  -> "La plantilla del ... no define la linea ..."
--   el asiento no cuadra     -> "El asiento no cuadra: DEBE ... y HABER ..."
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - LA CONFIGURACION DE NOMINA DE LA EMPRESA.
-- ==
-- ESPERADO: una fila, y NINGUNA de estas columnas nula:
--   CFNMCTMR  cuenta marcadora   sin ella no se distingue una linea
--                                configurada de una que no lo esta, y el
--                                sistema emitiria asientos con todas las
--                                cuentas iguales SIN AVISAR
--   CFNMPLRL  plantilla del rol
--   CFNMPLPR  plantilla de provisiones
--   CFNMPLPG  plantilla del pago
--   CFNMTARL / CFNMTAPR / CFNMTAPG   tipos de asiento
-- ==
-- CFNMSCIE y CFNMSGSC pueden estar nulas: son del exportador del IESS, no de
-- la contabilidad. CFNMTPEM tambien: se queda nula a proposito hasta que
-- Steven de el codigo real.
-- ============================================================================
SELECT CFNMCDGO, PJRQCDGO AS EMPRESA,
       CFNMCTMR AS CUENTA_MARCADORA,
       CFNMPLRL AS PLANT_ROL, CFNMPLPR AS PLANT_PROVISION,
       CFNMPLPG AS PLANT_PAGO, CFNMPLLQ AS PLANT_LIQUIDACION,
       CFNMTARL AS TIPO_ASNT_ROL, CFNMTAPR AS TIPO_ASNT_PROV,
       CFNMTAPG AS TIPO_ASNT_PAGO,
       CFNMDCCS AS DESGLOSA_CENTRO_COSTO, CFNMTLCD AS TOLERANCIA_CUADRE,
       CFNMESTD AS ESTADO
  FROM RHH.CFNM;

-- ============================================================================
-- BLOQUE 2 - QUE LAS TRES PLANTILLAS EXISTAN DE VERDAD.
-- ==
-- resuelvePlantilla busca por CODIGO ALTERNO Y EMPRESA. Que CFNM lleve un
-- numero no significa que exista la plantilla: son dos comprobaciones, no una.
-- ==
-- ESPERADO: TRES filas, una por cada alterno que el bloque 1 devolvio en
-- CFNMPLRL, CFNMPLPR y CFNMPLPG, todas con PLNSESTD = 1.
-- Si falta alguna, la contabilizacion de ese asiento revienta.
-- ============================================================================
SELECT p.PLNSCDGO AS PLANTILLA, p.PLNSCDAL AS ALTERNO, p.PLNSNMBR AS NOMBRE,
       p.PJRQCDGO AS EMPRESA, p.PLNSESTD AS ESTADO,
       (SELECT COUNT(*) FROM CNT.DTPL d
         WHERE d.PLNSCDGO = p.PLNSCDGO AND d.DTPLESTD = 1) AS LINEAS_ACTIVAS
  FROM CNT.PLNS p
 WHERE p.PLNSCDAL IN (SELECT CFNMPLRL FROM RHH.CFNM
                       UNION SELECT CFNMPLPR FROM RHH.CFNM
                       UNION SELECT CFNMPLPG FROM RHH.CFNM)
 ORDER BY p.PLNSCDAL;

-- ============================================================================
-- BLOQUE 3 - LAS LINEAS DE ESAS PLANTILLAS, Y SU CUENTA CONTABLE.
-- ==
-- ESPERADO: cada linea con su PLNNCDGO apuntando a una cuenta que EXISTE.
-- La columna CUENTA_EXISTE tiene que decir SI en todas.
-- Una linea sin cuenta, o con una cuenta que no esta en el plan, es un asiento
-- que no se puede armar.
-- ==
-- Y mirar la columna ES_MARCADORA: las lineas que llevan la cuenta marcadora
-- son las que el motor considera PENDIENTES DE CONFIGURAR. Si hay muchas, la
-- plantilla esta a medio hacer.
-- ============================================================================
SELECT p.PLNSCDAL AS ALTERNO_PLANTILLA, p.PLNSNMBR AS PLANTILLA,
       d.DTPLCDGO AS LINEA, d.DTPLDSCR AS DESCRIPCION,
       d.PLNNCDGO AS CUENTA, d.DTPLMVMN AS MOVIMIENTO,
       d.DTPLAXL1 AS AUXILIAR1, d.DTPLESTD AS ESTADO,
       CASE WHEN d.PLNNCDGO IS NULL THEN 'NO - SIN CUENTA'
            WHEN EXISTS (SELECT 1 FROM CNT.PLNN c WHERE c.PLNNCDGO = d.PLNNCDGO)
                 THEN 'SI' ELSE 'NO - CUENTA INEXISTENTE' END AS CUENTA_EXISTE,
       CASE WHEN d.PLNNCDGO = (SELECT CFNMCTMR FROM RHH.CFNM WHERE ROWNUM = 1)
            THEN 'MARCADORA - LINEA SIN CONFIGURAR' ELSE '' END AS ES_MARCADORA
  FROM CNT.DTPL d
  JOIN CNT.PLNS p ON p.PLNSCDGO = d.PLNSCDGO
 WHERE p.PLNSCDAL IN (SELECT CFNMPLRL FROM RHH.CFNM
                       UNION SELECT CFNMPLPR FROM RHH.CFNM
                       UNION SELECT CFNMPLPG FROM RHH.CFNM)
   AND d.DTPLESTD = 1
 ORDER BY p.PLNSCDAL, d.DTPLCDGO;

-- ============================================================================
-- BLOQUE 4 - LA CUENTA MARCADORA, que exista y se sepa cual es.
-- ESPERADO: una fila. Si el bloque 1 la trajo nula, esto sale vacio y hay que
-- resolverlo ANTES de dejarle agosto a Steven.
-- ============================================================================
SELECT c.PLNNCDGO AS CUENTA, c.PLNNCNTA AS CUENTA_CONTABLE, c.PLNNNMBR AS NOMBRE
  FROM CNT.PLNN c
 WHERE c.PLNNCDGO = (SELECT CFNMCTMR FROM RHH.CFNM WHERE ROWNUM = 1);

-- ============================================================================
-- BLOQUE 5 - QUE AGOSTO NO EXISTA YA, Y EL ESTADO DE LOS SIETE ANTERIORES.
-- ==
-- ESPERADO: siete periodos, enero a julio, TODOS en estado 7, todos con
-- PRDNMODO = 1. Agosto no debe aparecer todavia.
-- ==
-- ⚠ Y LA TRAMPA QUE HAY QUE NEUTRALIZAR AL CREAR AGOSTO: esHistorico devuelve
-- true cuando PRDNMODO es NULO. Un periodo creado sin modo NO CONTABILIZA, y
-- no avisa: Steven calcularia, pulsaria contabilizar, y los asientos se
-- quedarian en nulo sin ningun error. AGOSTO TIENE QUE QUEDAR CON PRDNMODO = 2
-- Y HAY QUE COMPROBARLO LEYENDOLO DE VUELTA.
-- ============================================================================
SELECT PRDNCDGO AS PRDN, PRDNANOO AS ANIO, PRDNMSEE AS MES,
       PRDNESTD AS ESTADO, PRDNMODO AS MODO,
       PRDNFCHI AS DESDE, PRDNFCHF AS HASTA, PRDNFCCN AS FECHA_CONTABLE,
       PRDNASNT AS ASIENTO_ROL, PRDNASPR AS ASIENTO_PROV, PRDNASPG AS ASIENTO_PAGO
  FROM RHH.PRDN
 WHERE PRDNANOO = 2026
 ORDER BY PRDNMSEE;
