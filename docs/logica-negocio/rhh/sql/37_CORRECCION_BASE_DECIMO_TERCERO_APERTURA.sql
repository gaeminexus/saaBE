-- =====================================================
-- MODULO: RHH - CORRECCION DE DATOS: BASE DEL DECIMO TERCERO EN LA APERTURA
-- DESCRIPCION: Las filas de RHH.ACMN tipo 3 (BASE_DECIMO_TERCERO) que nacieron
--              de la apertura (ACMNAPRT = 'S') guardan el IMPORTE del decimo
--              (V13 de sql/26: 58,33 para Barcenas, 183,33 para Viteri) donde
--              el motor espera la BASE (la remuneracion del tramo: 700,00 y
--              2.200,00). Esta correccion multiplica por 12 esas filas.
-- ORDEN DE EJECUCION: 37
-- FECHA: 2026-08-20
-- =====================================================
-- POR QUE
--   calcularDecimoTercero (BeneficioSocialServiceImpl) hace
--     SUM(ACMNVLOR tipo 3, dic-anterior..nov) / 12.
--   Las filas mensuales que escribe cerrarPeriodo llevan la base (482,00 para
--   Castro Arce en enero); la de apertura lleva 30,03. Sumarlas subestima el
--   decimo en (base - importe)/12 por persona: Castro 27,53; Viteri 168,06.
--   Lo destapo el calculo a mano de la liquidacion de Castro Arce y Cevallos
--   Aleman (salida 06-03-2026), los primeros ACUMULA que se liquidan.
--
-- POR QUE x12 ES GENERAL
--   El importe acumulado es siempre SUM(bases del tramo) / 12, sea el tramo
--   de un mes (corte al 31-dic: solo diciembre) o de siete. La base agregada
--   que el motor necesita es importe x 12. No depende de la fecha de corte.
--
-- EL CODIGO TAMBIEN SE CORRIGE (backend): MigracionRhhServiceImpl.aplicaAcumulado
--   debe grabar valor x 12 cuando el tipo es DECIMO_TERCERO_ACUMULADO, para que
--   un revertir + aplicar de la apertura no vuelva a dejar el importe. SLAP
--   sigue guardando el importe, que es lo que el cliente entrega.
--
-- NO SE TOCA el tipo 4 (BASE_DECIMO_CUARTO): ahi lo que vale son los DIAS
--   (ACMNDIAS = 150 para Viteri, 23 para Castro) y el lector se corrige para
--   leerlos; el importe 195,83 (a SBU 470) queda como referencia y no se usa.
-- =====================================================
--
-- *** NO REEJECUTAR DESPUES DE PUBLICAR LA CORRECCION C (2026-08-20 noche) ***
--   aplicaAcumulado ya multiplica por doce al aplicar la migracion. Si se revierte y
--   reaplica la apertura, las filas nacen con la base correcta y este script las
--   multiplicaria por doce OTRA VEZ. Ejecutado una sola vez el 2026-08-20: 17 filas,
--   suma 14.075,52 (= 12 x 1.172,96), Castro Arce 360,36. Misma familia que el
--   aviso del script 30.

-- =====================================================
-- PASO 1: CONTROL ANTES -- mirar antes de ejecutar el UPDATE
-- =====================================================
-- Cada fila debe mostrar un importe "pequeno" (sueldo/12 o menos) y la base
-- propuesta igual al sueldo del tramo (o su prorrateo para los de diciembre).
SELECT a.ACMNCDGO, m.MPLDIDNT, m.MPLDAPLL, a.ACMNANOO, a.ACMNMSEE,
       a.ACMNVLOR AS IMPORTE_HOY, ROUND(a.ACMNVLOR * 12, 2) AS BASE_PROPUESTA,
       s.SLAPVLOR AS V13_EN_SLAP
  FROM RHH.ACMN a
  JOIN RHH.MPLD m ON m.MPLDCDGO = a.MPLDCDGO
  LEFT JOIN RHH.SLAP s ON s.SLAPIDNT = m.MPLDIDNT AND s.SLAPTPSL = 3
 WHERE a.ACMNAPRT = 'S' AND a.ACMNTPAC = 3
 ORDER BY m.MPLDAPLL;
-- Esperado: una fila por persona ACUMULA con V13 > 0 en sql/26 (los tres
-- MENSUAL -- Cossio, Manosalvas, Moscoso -- no aparecen). IMPORTE_HOY debe
-- coincidir con V13_EN_SLAP en todas. Anotar cuantas filas salen: N.

-- Que ninguna este ya corregida (si alguna BASE_PROPUESTA supera 12 x sueldo,
-- parar: ya se multiplico una vez).
SELECT COUNT(DISTINCT a.ACMNCDGO) AS YA_CORREGIDAS
  FROM RHH.ACMN a
  JOIN RHH.MPLD m ON m.MPLDCDGO = a.MPLDCDGO
  JOIN RHH.CNTE c ON c.MPLDCDGO = m.MPLDCDGO   -- cualquier contrato de la persona
 WHERE a.ACMNAPRT = 'S' AND a.ACMNTPAC = 3
   AND a.ACMNVLOR > c.CNTESLRB;
-- Esperado: 0.


-- =====================================================
-- PASO 2: LA CORRECCION
-- =====================================================
UPDATE RHH.ACMN
   SET ACMNVLOR = ROUND(ACMNVLOR * 12, 2)
 WHERE ACMNAPRT = 'S' AND ACMNTPAC = 3;
-- Debe tocar exactamente N filas (las del paso 1). Si toca 0, no hay error:
-- mirar el conteo.

COMMIT;


-- =====================================================
-- PASO 3: CONTROL DESPUES
-- =====================================================
-- La base de apertura entre 12 tiene que devolver el V13 de SLAP al centavo
-- (x12 y /12 son exactos a dos decimales salvo en el redondeo de ROUND: si
-- alguna difiere en 0,01 es aceptable y se anota).
SELECT m.MPLDIDNT, m.MPLDAPLL, a.ACMNVLOR AS BASE, ROUND(a.ACMNVLOR / 12, 2) AS V13_RECALC,
       s.SLAPVLOR AS V13_SLAP, ROUND(a.ACMNVLOR / 12, 2) - s.SLAPVLOR AS DIF
  FROM RHH.ACMN a
  JOIN RHH.MPLD m ON m.MPLDCDGO = a.MPLDCDGO
  JOIN RHH.SLAP s ON s.SLAPIDNT = m.MPLDIDNT AND s.SLAPTPSL = 3
 WHERE a.ACMNAPRT = 'S' AND a.ACMNTPAC = 3
 ORDER BY m.MPLDAPLL;
-- Esperado: DIF = 0 en todas. Viteri BASE 2.200,00 · Barcenas 700,00 ·
-- Castro Arce y Cevallos Aleman 360,36.

-- Y el decimo tercero de Castro Arce al 06-03-2026, sumado como lo suma el
-- motor (dic-2025 a mar-2026, tipo 3), mas el tramo del mes en curso que pone
-- el finiquito (96,40):
SELECT ROUND((SUM(a.ACMNVLOR) + 96.40) / 12, 2) AS DT_CASTRO_ESPERADO
  FROM RHH.ACMN a JOIN RHH.MPLD m ON m.MPLDCDGO = a.MPLDCDGO
 WHERE m.MPLDIDNT = '1720245735' AND a.ACMNTPAC = 3
   AND (a.ACMNANOO = 2025 AND a.ACMNMSEE = 12 OR a.ACMNANOO = 2026 AND a.ACMNMSEE <= 2);
-- Esperado: (360,36 + 482 + 482 + 96,40) / 12 = 118,40.


-- =====================================================
-- PENDIENTE DE ACLARAR, NO BLOQUEA
-- =====================================================
-- El V13 de Castro Arce y Cevallos Aleman en sql/26 es 30,03, que es
-- 470 x 23 / 360: la formula del decimo CUARTO con el SBU de 2025. El decimo
-- TERCERO de 23 dias a 482,00 seria 482 x 23 / 30 / 12 = 30,79 (base 369,53).
-- Son 0,76 por persona. Hay que ver de donde salio el 30,03 (si lo dio el
-- cliente se respeta; si lo calculo el guion, se corrige en sql/26 y aqui).
