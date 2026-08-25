-- ============================================================================
-- 62 - COMPROBACION PREVIA DE JULIO
-- ==
-- Se corre en PRODUCCION antes de crear el periodo de julio. Solo lee.
-- ==
-- POR QUE EXISTE. El esperado de julio, +31,43, se apoya en dos datos que NADIE
-- HA MIRADO EN PRODUCCION:
--   1. La fecha de ingreso de RODRIGUEZ VALENCIA. El motor calcula su
--      aniversario como MPLDFCIN + 1 anio y de ahi los dias prorrateados con la
--      convencion 30 - d. Si no es el 16-07-2025, sus 27,21 cambian.
--   2. Que no haya un SEPTIMO que cumpla el ano en julio. El sql/59 barrio
--      junio de 2025 y encontro una sexta que el guion no nombraba -Torres
--      Chavez-. Julio de 2025 NO SE HA BARRIDO NUNCA.
-- ==
-- Y un tercer dato que en julio pesa mas que en junio: los DIAS TRABAJADOS.
-- Nuestro motor NO reduce dias por vacaciones -no lee SLCT- y NO genera el
-- concepto 12, y las dos omisiones se cancelan. Pero si alguien tiene una
-- ausencia NO REMUNERADA en julio, los dias SI bajan y el sueldo baja con
-- ellos SIN NADA QUE LO COMPENSE. En junio eso movia el esperado; en julio lo
-- rompe por partida doble, porque el sueldo es tambien la base del fondo de
-- reserva.
-- ============================================================================

-- ============================================================================
-- BLOQUE 1 - Rodriguez Valencia, el que se estrena en julio.
-- ESPERADO: ingreso 16-07-2025, aniversario 16-07-2026, DIAS_MOTOR 14,
-- modalidad 1 MENSUALIZADO, sueldo 700, contrato ACTIVO.
-- Con 14 dias su base es 700 x 14/30 = 326,67 y su fondo 27,21, que es lo que
-- el cliente le paga. Cualquier otro numero de dias mueve el esperado.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS, e.MPLDFCIN AS FECHA_INGRESO,
       ADD_MONTHS(e.MPLDFCIN, 12) AS ANIVERSARIO,
       30 - TO_NUMBER(TO_CHAR(ADD_MONTHS(e.MPLDFCIN, 12), 'DD')) AS DIAS_MOTOR,
       c.CNTEFRMD AS MODALIDAD_FR, c.CNTESLRB AS SUELDO, c.CNTEESTD AS ESTADO
  FROM RHH.MPLD e
  JOIN RHH.CNTE c ON c.MPLDCDGO = e.MPLDCDGO
 WHERE e.MPLDIDNT = '0801999855';

-- ============================================================================
-- BLOQUE 2 - QUE NO HAYA UN SEPTIMO. Todo el que ingreso en julio de 2025.
-- ==
-- ESPERADO: SOLO Rodriguez Valencia en ENTRA. Cualquier otra fila en ENTRA es
-- un fondo de reserva que el esperado no contempla, y hay que rehacerlo antes
-- de calcular. Una fila en NO ENTRA es informativa, como Torres Chavez en junio.
-- ==
-- La columna EN_JULIO replica el filtro exacto de
-- ContratoEmpleadoDaoService.selectActivosEnPeriodo para el 01-07 al 31-07.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS, e.MPLDFCIN AS FECHA_INGRESO,
       e.MPLDESTD AS ESTADO_EMPLEADO, c.CNTEESTD AS ESTADO_CONTRATO,
       c.CNTEFCTR AS FECHA_TERMINACION, c.CNTEFRMD AS MODALIDAD_FR,
       CASE WHEN c.CNTEFCHI <= DATE '2026-07-31'
             AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-07-01')
             AND ( (c.CNTEFCTR IS NULL AND (e.MPLDESTD IS NULL OR e.MPLDESTD <> 4))
                OR (c.CNTEFCTR IS NOT NULL AND c.CNTEFCTR > DATE '2026-07-31') )
            THEN 'ENTRA' ELSE 'NO ENTRA' END AS EN_JULIO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE e.MPLDFCIN >= DATE '2025-07-01'
   AND e.MPLDFCIN <= DATE '2025-07-31'
 ORDER BY e.MPLDFCIN, e.MPLDAPLL;

-- ============================================================================
-- BLOQUE 3 - A quien va a recorrer el motor en julio.
-- ESPERADO: 20, los mismos de junio. Nadie entra y nadie sale.
-- ============================================================================
SELECT COUNT(*) AS COLABORADORES_JULIO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE c.CNTEFCHI <= DATE '2026-07-31'
   AND (c.CNTEFCHF IS NULL OR c.CNTEFCHF >= DATE '2026-07-01')
   AND ( (c.CNTEFCTR IS NULL AND (e.MPLDESTD IS NULL OR e.MPLDESTD <> 4))
      OR (c.CNTEFCTR IS NOT NULL AND c.CNTEFCTR > DATE '2026-07-31') );

-- ============================================================================
-- BLOQUE 4 - AUSENCIAS NO REMUNERADAS DE JULIO. En julio pesan doble.
-- ==
-- Nuestro motor da 30 dias a todo el mundo -no reduce por vacaciones- y por eso
-- el sueldo completo de Caiza, Nieto y Pardo cuadra con el sueldo por dias mas
-- las vacaciones del cliente. Una ausencia no remunerada rompe esa igualdad, y
-- ademas baja la base del fondo de reserva.
-- ==
-- ESPERADO: CERO FILAS. Si sale alguna, PARAR y rehacer el esperado.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       r.RSMNFCHA AS FECHA, r.RSMNTPAS AS TIPO_AUSENCIA
  FROM RHH.RSMN r
  JOIN RHH.MPLD e ON e.MPLDCDGO = r.MPLDCDGO
 WHERE r.RSMNFCHA >= DATE '2026-07-01'
   AND r.RSMNFCHA <= DATE '2026-07-31'
   AND r.RSMNTPAS IN (1, 3)
 ORDER BY e.MPLDAPLL, r.RSMNFCHA;

-- ============================================================================
-- BLOQUE 5 - Los contratos de los SEIS del fondo de reserva de julio.
-- ESPERADO: CNTEFCHF y CNTEFCTR nulas en los seis, CNTETPRL = 1 en los seis.
-- Una fecha de corte dentro de julio recorta la ventana del prorrateo.
-- ============================================================================
SELECT e.MPLDIDNT AS CEDULA, e.MPLDAPLL AS APELLIDOS,
       c.CNTEFCHI AS INICIO, c.CNTEFCHF AS FIN, c.CNTEFCTR AS TERMINACION,
       c.CNTETPRL AS TIPO_RELACION, c.CNTEFRMD AS MODALIDAD_FR, c.CNTESLRB AS SUELDO
  FROM RHH.CNTE c
  JOIN RHH.MPLD e ON e.MPLDCDGO = c.MPLDCDGO
 WHERE e.MPLDIDNT IN ('1717991341','1717649873','1723962849','1726657164',
                      '0801999855','1712232659')
 ORDER BY e.MPLDAPLL;
