-- ============================================================================
-- 60 - COMPROBACION PREVIA DE JUNIO: A QUIEN VA A METER EL MOTOR
-- ==
-- Se corre en PRODUCCION antes de crear el periodo de junio. Solo lee.
-- ==
-- POR QUE EXISTE. El sql/59 destapo una SEXTA persona que cumple el ano en
-- junio y que el guion no contempla: TORRES CHAVEZ, ingreso 25-06-2025,
-- modalidad 1 MENSUALIZADO, contrato CERRADO. Si el motor la metiera en junio
-- le pagaria fondo de reserva y el esperado -44,60 dejaria de valer.
-- ==
-- El razonamiento dice que NO entra, porque su contrato tiene fecha de
-- terminacion anterior a junio. Pero el razonamiento no es la prueba: si esa
-- fecha estuviera NULA, el motor caeria a la otra rama del filtro y decidiria
-- por el estado del empleado, y si ademas el estado no fuera CESANTE, ENTRARIA.
-- ==
-- Este script no razona: EJECUTA EL MISMO FILTRO QUE EL MOTOR.
-- Es la traduccion literal de ContratoEmpleadoDaoService.selectActivosEnPeriodo
-- para el periodo 01-06-2026 a 30-06-2026, con la asimetria intacta:
--   contrato SIN fecha de terminacion  ->  decide el estado del empleado
--   contrato CON fecha de terminacion  ->  decide SOLO la fecha, y con > hasta
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - La lista exacta que el motor va a recorrer en junio.
-- ESPERADO: 20 colaboradores, los mismos de abril y mayo, y TORRES CHAVEZ
-- NO debe aparecer.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS, e.MPLDESTD AS ESTADO_EMPLEADO,
       c.CNTEESTD AS ESTADO_CONTRATO, c.CNTEFCTR AS FECHA_TERMINACION
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE c.CNTEFCHI <= DATE '2026-06-30'
   AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-06-01')
   AND ( (c.CNTEFCTR IS NULL AND (e.MPLDESTD IS NULL OR e.MPLDESTD <> 4))
      OR (c.CNTEFCTR IS NOT NULL AND c.CNTEFCTR > DATE '2026-06-30') )
 ORDER BY e.MPLDAPLL, e.MPLDNMBR;

-- ============================================================================
-- BLOQUE 2 - El recuento, para leerlo de un vistazo.
-- ESPERADO: 20.
-- ============================================================================
SELECT COUNT(*) AS COLABORADORES_JUNIO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE c.CNTEFCHI <= DATE '2026-06-30'
   AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-06-01')
   AND ( (c.CNTEFCTR IS NULL AND (e.MPLDESTD IS NULL OR e.MPLDESTD <> 4))
      OR (c.CNTEFCTR IS NOT NULL AND c.CNTEFCTR > DATE '2026-06-30') );

-- ============================================================================
-- BLOQUE 3 - Los seis que cumplen el ano en junio, cruzados con el filtro.
-- ==
-- ESPERADO: los cinco del guion en ENTRA, y TORRES CHAVEZ en NO ENTRA.
-- Si Torres saliera en ENTRA, PARAR: junio tendria un sexto fondo de reserva
-- y el esperado -44,60 hay que rehacerlo antes de calcular nada.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       e.MPLDESTD AS ESTADO_EMPLEADO, c.CNTEESTD AS ESTADO_CONTRATO,
       c.CNTEFCTR AS FECHA_TERMINACION, c.CNTEFRMD AS MODALIDAD_FR,
       CASE WHEN c.CNTEFCHI <= DATE '2026-06-30'
             AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-06-01')
             AND ( (c.CNTEFCTR IS NULL AND (e.MPLDESTD IS NULL OR e.MPLDESTD <> 4))
                OR (c.CNTEFCTR IS NOT NULL AND c.CNTEFCTR > DATE '2026-06-30') )
            THEN 'ENTRA' ELSE 'NO ENTRA' END AS EN_JUNIO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE e.MPLDFCIN >= DATE '2025-06-01'
   AND e.MPLDFCIN <= DATE '2025-06-30'
 ORDER BY e.MPLDFCIN, e.MPLDAPLL;

-- ============================================================================
-- BLOQUE 4 - LA BANDERA DE LA QUE CUELGA TODO, Y NO ESTABA EN EL sql/59.
-- ==
-- Lo levanto la auditoria del agente de backend el 2026-08-25 y lo he
-- verificado en el codigo: la base del fondo de reserva NO sale del contrato,
-- sale de los renglones ya generados que llevan la bandera APFR:
--     baseFr = sumaPorBandera(renglones, "APFR")     linea 898
--     "APFR" -> concepto.getAportaFondosReserva()    linea 1421
--     getAportaFondosReserva -> RHH.CPNM.CPNMAPFR    ConceptoNomina:139
-- ==
-- El concepto que aporta esa base es el de SUELDO, alterno 1, y ESE CONCEPTO
-- NO TIENE CPNMROLM 5 NI 20, asi que el bloque 3 del sql/59 no lo trae: su
-- filtro WHERE CPNMROLM IN (5,20) lo deja fuera. El 59 podia salir entero en
-- verde con esta bandera en 'N'.
-- ==
-- Y si esta en 'N', baseFr sale CERO, el fondo de reserva de los cinco sale
-- CERO, y NO HAY NINGUN ERROR: ni excepcion, ni aviso, ni renglon raro. Junio
-- saldria 82,23 por debajo del cliente en vez de 44,60 y habria que averiguar
-- por que desde el resultado.
-- ==
-- ESPERADO: el concepto 1 Sueldo con CPNMAPFR = 'S' y CPNMESTD = 1.
-- El seed del sql/08 lo crea asi; esto comprueba que sigue asi en produccion.
-- Las demas filas son informativas: son los otros conceptos que suman a la
-- base. Si aparece alguno inesperado en 'S', ese concepto tambien engorda el
-- fondo de reserva de quien lo cobre en junio.
-- ============================================================================
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR, CPNMROLM, CPNMAPFR, CPNMESTD
  FROM RHH.CPNM
 WHERE CPNMAPFR = 'S'
   AND CPNMESTD = 1
 ORDER BY CPNMALTR;

-- ============================================================================
-- BLOQUE 5 - AUSENCIAS NO REMUNERADAS DE JUNIO, el otro hueco del 59.
-- ==
-- calculaDiasTrabajados resta del mes los dias de RHH.RSMN con tipo de
-- ausencia FALTA_INJUSTIFICADA (1) o PERMISO_SIN_GOCE (3). Y diasTrabajados
-- entra DOS VECES en el fondo de reserva: en el sueldo del periodo, que es la
-- base, y en el factor min(dias, trabajados) / trabajados.
-- ==
-- Con 30 dias trabajados el factor de Viteri es 5/30 y su base 366,67, que es
-- lo que la planilla del IESS declara. Con 28 dias trabajados el factor pasa a
-- 5/28 sobre una base ya reducida, y el esperado -44,60 se mueve SIN QUE
-- NINGUN TOTAL LO DELATE.
-- ==
-- ESPERADO: CERO FILAS. Si sale alguna, y es de uno de los cinco del fondo de
-- reserva, PARAR: hay que rehacer el esperado de junio antes de calcular.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       r.RSMNFCHA AS FECHA, r.RSMNTPAS AS TIPO_AUSENCIA
  FROM RHH.RSMN r
  JOIN RHH.MPLD e ON e.MPLDCDGO = r.MPLDCDGO
 WHERE r.RSMNFCHA >= DATE '2026-06-01'
   AND r.RSMNFCHA <= DATE '2026-06-30'
   AND r.RSMNTPAS IN (1, 3)
 ORDER BY e.MPLDAPLL, r.RSMNFCHA;

-- ============================================================================
-- BLOQUE 6 - Los contratos de los cinco, mirados por donde el motor los recorta.
-- ==
-- baseFondosReservaProrrateada acota la ventana con CNTEFCTR y CNTEFCHF, y
-- calculaSueldoPeriodo cambia de formula segun CNTETPRL. El sql/59 no miraba
-- ninguna de las tres.
-- ==
-- ESPERADO: las tres fechas de corte NULAS en los cinco -ninguno termina
-- dentro de junio- y el mismo CNTETPRL en los cinco, el de jornada completa.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       c.CNTEFCHI AS INICIO, c.CNTEFCHF AS FIN, c.CNTEFCTR AS TERMINACION,
       c.CNTETPRL AS TIPO_RELACION, c.CNTEESTD AS ESTADO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE e.MPLDIDNT IN ('1717991341','1717649873','1723962849','1726657164','1712232659')
 ORDER BY e.MPLDAPLL;
