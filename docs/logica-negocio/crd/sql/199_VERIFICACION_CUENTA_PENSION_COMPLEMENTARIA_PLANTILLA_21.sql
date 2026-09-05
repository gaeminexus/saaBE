-- =====================================================================================
-- 199 - Verificacion de la cuenta del aporte 23 (pension complementaria) en la plantilla 21
-- FECHA: 2026-09-05 - EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT + un INSERT COMENTADO al final (no se corre solo, hay que descomentarlo
-- a mano despues de revisar los resultados de los bloques 1-4).
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- Contexto: la corrida de jubilados fallaba con "El tipo de aporte 23 no tiene cuenta
-- contable parametrizada". Decision del usuario 2026-09-05: se cierra contra la MISMA
-- cuenta individual que se abre al jubilar al participe -- cuenta contable 2.1.02.25.01
-- (dato textual del usuario). El cambio Java ya esta escrito
-- (CrdLineaAsiento.APORTES_PENSION_COMPLEMENTARIA = 53, candidato) pero necesita:
--   a) confirmar que 2.1.02.25.01 existe en el plan de cuentas (CNT.PLNN) y conseguir
--      su PLNNCDGO -- el INSERT de mas abajo lo necesita por codigo, no por texto.
--   b) confirmar que la plantilla 21 (alterno = APLICACION_PETRO) NO tiene ya una linea
--      apuntando a esa cuenta con otro aux1 (si la tiene, USAR ESE aux1 en el Java en vez
--      de 53, y no hace falta ningun INSERT).
--   c) confirmar que aux1=53 no esta ya ocupado en esa plantilla por otra cosa.
--
-- NOTA sobre el nombre de tabla: com.saa.model.cnt.PlanCuenta mapea a CNT.PLNN (columna PK
-- PLNNCDGO, columna de codigo contable PLNNCNTA) -- verificado contra la entidad Java, no
-- adivinado. Si en algun mensaje aparecio "CNT.PLCN"/"PLCNCDGO", es un nombre distinto al
-- que usa el codigo actual; correr igual este script contra PLNN primero.
--
-- ✅ EJECUTADO EN PRODUCCION el 2026-09-05 (omen-saa-1-arb). La linea YA EXISTE: plantilla 21,
-- aux1=53, cuenta 2.1.02.25.01 (PLNNCDGO=10358, "CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS").
-- El BLOQUE 5 de mas abajo NO hay que volver a correrlo -- se deja tal cual, con la leccion
-- aprendida, como referencia para el PROXIMO script de este equipo que necesite un INSERT
-- manual a una tabla con PK por secuencia.
--
-- ⛔ LECCION (nos costo un ORA-01400 en produccion, la version anterior de este script la
-- tenia mal): DetallePlantilla usa @SequenceGenerator(name="SQ_DTPLCDGO",
-- sequenceName="CNT.SQ_DTPLCDGO") -- CNT.DTPL.DTPLCDGO es GenerationType.SEQUENCE. JPA la
-- resuelve sola; un INSERT a mano NO. Cualquier INSERT manual a una tabla cuya entidad use
-- @SequenceGenerator/@GeneratedValue(SEQUENCE) tiene que llevar la PK explicita:
-- <ESQUEMA>.SQ_XXXXCDGO.NEXTVAL. Verificar el nombre de la secuencia en la entidad Java antes
-- de escribir el INSERT, no asumirla por el patron del nombre de tabla.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 1 - ¿Existe la cuenta 2.1.02.25.01 en el plan de cuentas? Traer su PLNNCDGO.
-- =====================================================================================
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE, n.PJRQCDGO AS EMPRESA,
       n.PLNNESTD AS ESTADO
FROM   CNT.PLNN n
WHERE  n.PLNNCNTA = '2.1.02.25.01';

-- =====================================================================================
-- BLOQUE 2 - La OTRA cuenta (referencia, no es un error): 2.3.01.10.03 es la que
--            AporteServiceImpl#generarAsientoJubilacion (plantilla 29, aux1=5) acredita AL
--            JUBILAR -- nace el PASIVO, la asociacion le debe la pension al participe.
--            2.1.02.25.01 (bloque 1) es la cuenta INDIVIDUAL: al cruzar contra un prestamo
--            no se paga nada, se da de baja el saldo de esa cuenta -- es una compensacion,
--            no un desembolso. Son dos cuentas para dos hechos economicos distintos, ya
--            confirmado por el usuario -- este bloque es solo para tener las dos a la vista,
--            no para "elegir" una.
-- =====================================================================================
SELECT n.PLNNCDGO, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE
FROM   CNT.PLNN n
WHERE  n.PLNNCNTA = '2.3.01.10.03';

-- =====================================================================================
-- BLOQUE 3 - TODAS las lineas de la plantilla 21, para ver si 2.1.02.25.01 ya esta
--            apuntada con otro aux1, y que aux1 estan ocupados
-- =====================================================================================
SELECT p.PLNSCDGO, p.PLNSCDAL AS ALTERNO, p.PJRQCDGO AS EMPRESA, p.PLNSESTD AS ESTADO_PLANTILLA,
       d.DTPLAXL1 AS AUX1, d.DTPLAXL2 AS AUX2, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCDGO AS CUENTA_ID, n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE,
       d.DTPLDSCR AS DESCRIPCION, d.DTPLESTD AS ESTADO_LINEA
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL = 21
ORDER  BY d.DTPLAXL1;

-- QUE MIRAR:
--   - Si alguna fila ya tiene CUENTA = '2.1.02.25.01': usar ESE DTPLAXL1 en
--     CrdLineaAsiento.APORTES_PENSION_COMPLEMENTARIA en vez de 53, y NO correr el INSERT
--     de mas abajo (la linea ya existe).
--   - Si DTPLAXL1 = 53 YA aparece ocupado por otra cosa: el candidato 53 no sirve, hay que
--     elegir otro numero libre (mirar que valores de DTPLAXL1 salen en este bloque y elegir
--     uno que no este ni aca ni en com.saa.rubros.CrdLineaAsiento).
--   - Fijarse tambien el DTPLMVMN que usan las lineas 50/51/52 (aportes cesantia/jubilacion/
--     adicional) -- el INSERT de abajo lo copia de la linea 50 en vez de adivinarlo, pero
--     conviene mirarlo con los propios ojos antes de correr el INSERT.

-- =====================================================================================
-- BLOQUE 4 - Aux1 ya ocupados en la 21 (lista corta, para decidir un numero libre si 53
--            resultara ocupado)
-- =====================================================================================
SELECT DISTINCT d.DTPLAXL1 AS AUX1_OCUPADO
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
WHERE  p.PLNSCDAL = 21
ORDER  BY d.DTPLAXL1;

-- =====================================================================================
-- BLOQUE 5 - INSERT COMENTADO. YA CORRIDO EN PRODUCCION (2026-09-05) -- no volver a
--            correrlo, se deja como referencia corregida para el proximo script similar.
--            Version original de este bloque (antes de esta correccion) NO llevaba
--            DTPLCDGO ni la secuencia -- fallo con ORA-01400 (no se puede insertar NULL en
--            CNT.DTPL.DTPLCDGO). Corregido con lo que de verdad se corrio.
--
--            DTPLAXL2 = 0, no NULL: selectByPlantillaYAuxiliar no filtra por auxiliar2 (asi
--            que NULL habria funcionado igual), pero las tres hermanas (50/51/52) usan 0 --
--            no hay razon para que esta linea sea distinta.
--            PLNNCDGO = 10358 literal: es el id real que devolvio el BLOQUE 1 contra la
--            base de produccion, ya no una subconsulta.
-- =====================================================================================

-- INSERT INTO CNT.DTPL (
--     DTPLCDGO,
--     PLNSCDGO,
--     PLNNCDGO,
--     DTPLAXL1,
--     DTPLAXL2,
--     DTPLMVMN,
--     DTPLDSCR,
--     DTPLESTD
-- )
-- SELECT
--     CNT.SQ_DTPLCDGO.NEXTVAL,
--     (SELECT p.PLNSCDGO FROM CNT.PLNS p WHERE p.PLNSCDAL = 21 AND p.PJRQCDGO = 1236),
--     10358,
--     53,
--     0,
--     2,
--     'Aportes personales PENSION COMPLEMENTARIA',
--     1
-- FROM DUAL;

-- =====================================================================================
-- BLOQUE 6 - Verificacion DESPUES del INSERT -- CONFIRMADO en produccion (2026-09-05):
--
--   ALTERNO | AUX1 | MOVIMIENTO | CUENTA        | CUENTA_NOMBRE
--   21      | 53   | 2          | 2.1.02.25.01  | CTA INDIVIDUAL DE PENSIONES COMPLEMENTARIAS
-- =====================================================================================
SELECT p.PLNSCDAL AS ALTERNO, d.DTPLAXL1 AS AUX1, d.DTPLMVMN AS MOVIMIENTO,
       n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS CUENTA_NOMBRE, d.DTPLDSCR AS DESCRIPCION
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL = 21
AND    d.DTPLAXL1 = 53;
