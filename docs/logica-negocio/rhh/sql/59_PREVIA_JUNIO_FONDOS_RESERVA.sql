-- ============================================================================
-- 59 - COMPROBACION PREVIA DE JUNIO: LOS INSUMOS DEL FONDO DE RESERVA
-- ==
-- Se corre en PRODUCCION, DESPUES de subir el WAR y ANTES de crear el periodo
-- de junio. Solo lee. No modifica nada.
-- ==
-- POR QUE EXISTE. Junio es el primer mes en toda la carga historica en que el
-- fondo de reserva produce un renglon. De enero a mayo nadie tenia derecho, asi
-- que ESTE CAMINO DEL MOTOR NUNCA SE HA EJECUTADO EN PRODUCCION. Los cinco
-- contrastes en verde no dicen nada sobre el, porque no lo tocaron.
-- ==
-- Y el esperado de junio -44,60 se apoya entero en un dato que nadie ha mirado
-- en produccion: la FECHA DE INGRESO. El motor calcula el aniversario como
-- MPLDFCIN + 1 anio -fechaAniversarioFondosReserva-, y de ahi salen los dias
-- prorrateados con la convencion 30 - d. Un dia de diferencia en esa fecha
-- mueve el importe de esa persona y rompe la descomposicion del guion SIN
-- dar ningun error.
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - Las fechas de ingreso de los cinco que cumplen el ano en junio.
-- ==
-- ESPERADO, y la columna DIAS_MOTOR es la que hay que mirar:
--   Barcenas   1717991341   ingreso 26-06-2025   aniversario 26-06-2026   4 dias
--   Munoz      1717649873   ingreso 25-06-2025   aniversario 25-06-2026   5 dias
--   Nieto      1723962849   ingreso 25-06-2025   aniversario 25-06-2026   5 dias
--   Pardo      1726657164   ingreso 25-06-2025   aniversario 25-06-2026   5 dias
--   Viteri     1712232659   ingreso 25-06-2025   aniversario 25-06-2026   5 dias
-- ==
-- MODALIDAD: los cuatro primeros en 1 MENSUALIZADO -van al rol- y Viteri en
-- 2 ACUMULADO_EN_EL_IESS -va a provision, no al rol-. Si Viteri apareciera en
-- 1, junio le pagaria el fondo en el rol y el esperado -44,60 se rompe.
-- ============================================================================
SELECT e.MPLDIDNT                                        AS CEDULA,
       e.MPLDAPLL                                        AS APELLIDOS,
       e.MPLDFCIN                                        AS FECHA_INGRESO,
       ADD_MONTHS(e.MPLDFCIN, 12)                        AS ANIVERSARIO,
       30 - TO_NUMBER(TO_CHAR(ADD_MONTHS(e.MPLDFCIN, 12), 'DD')) AS DIAS_MOTOR,
       c.CNTEFRMD                                        AS MODALIDAD_FR,
       c.CNTESLRB                                        AS SUELDO,
       c.CNTEESTD                                        AS ESTADO_CONTRATO
  FROM RHH.MPLD e
  JOIN RHH.CNTE c ON c.MPLDCDGO = e.MPLDCDGO
 WHERE e.MPLDIDNT IN ('1717991341','1717649873','1723962849','1726657164','1712232659')
 ORDER BY e.MPLDAPLL;

-- ============================================================================
-- BLOQUE 2 - Y QUE NO HAYA UN SEXTO. El guion afirma que son exactamente cinco.
-- ==
-- Lista a todo el que cumpla el ano dentro de junio de 2026, es decir todo el
-- que ingreso en junio de 2025. ESPERADO: las mismas cinco personas del
-- bloque 1 y ninguna mas. Una sexta fila es un renglon de fondo de reserva que
-- el esperado no contempla, y aparece como diferencia sin atribuir.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS, e.MPLDFCIN AS FECHA_INGRESO,
       c.CNTEFRMD AS MODALIDAD_FR, c.CNTEESTD AS ESTADO_CONTRATO
  FROM RHH.MPLD e
  JOIN RHH.CNTE c ON c.MPLDCDGO = e.MPLDCDGO
 WHERE e.MPLDFCIN >= DATE '2025-06-01'
   AND e.MPLDFCIN <= DATE '2025-06-30'
 ORDER BY e.MPLDFCIN, e.MPLDAPLL;

-- ============================================================================
-- BLOQUE 3 - El concepto de fondo de reserva y el de su provision.
-- ==
-- porcentajeVigente lee primero CPNMPRCN del concepto y solo si viene nulo cae
-- a la parametria del anio. Y conceptoPorRol localiza el concepto por CPNMROLM,
-- no por el alterno: con CPNMROLM nulo el motor NO ENCUENTRA el concepto y el
-- renglon no se crea, en silencio.
-- ==
-- ESPERADO: dos filas.
--   alterno  7, CPNMROLM  5, CPNMPRCN 8.33, CPNMESTD 1   Fondos de reserva
--   alterno 53, CPNMROLM 20, CPNMPRCN 8.33, CPNMESTD 1   Provision fondos de reserva
-- ==
-- El rol 20 lo repuso el sql/54. El rol 5 deberia venir de la carga original.
-- ============================================================================
SELECT CPNMCDGO, CPNMALTR, CPNMNMBR, CPNMROLM, CPNMPRCN, CPNMESTD
  FROM RHH.CPNM
 WHERE CPNMROLM IN (5, 20)
 ORDER BY CPNMROLM;

-- ============================================================================
-- BLOQUE 4 - La parametria del anio, que es el respaldo del porcentaje.
-- ESPERADO: fondos de reserva 8.33 y dias del mes 30. Los 30 son el divisor
-- del prorrateo: con otro valor los dias salen distintos.
-- ============================================================================
SELECT PRNMANOO AS ANIO, PRNMFNRS AS FONDOS_RESERVA, PRNMDIAS AS DIAS_MES
  FROM RHH.PRNM
 WHERE PRNMANOO = 2026;
