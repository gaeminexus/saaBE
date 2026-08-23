-- =====================================================
-- MODULO: RHH - VALORES ESPERADOS DE MARZO 2026, DEL ROL Y DE LA PLANILLA
-- DESCRIPCION: Carga RHH.CTRL con lo que ASOPREP pago en marzo y lo que el
--              IESS cobro, para CONTRASTE_MES_CONTRA_ROL_REAL.sql
--              (ANIO=2026, MES=3).
-- ORDEN DE EJECUCION: 36
-- FECHA: 2026-08-20
-- =====================================================
-- REGLA 6: ningun numero de aqui sale de un calculo nuestro. Vienen de la
-- hoja ROL MARZO (layout B) y ROL PROVISIONES del libro ROL MARZO 2026.xlsb
-- (REF-02 §7 y §8) y de la planilla del IESS del periodo 2026-03 (REF-03
-- §1.5). Se cargan tal cual. Munoz Santos con 51,98 / 498,03, que es lo que
-- el libro muestra (REF-06 §17); el markdown trae 51,97 / 498,02 y esta mal.
--
-- LO QUE CAMBIA RESPECTO A FEBRERO
--   1. 20 personas en el rol: Castro Arce y Cevallos Aleman salieron el
--      06-03-2026 (notificacion dentro del periodo de prueba) y no estan en
--      la hoja. Se les paga por liquidacion (fase 8), como a Torres y Benitez
--      en enero. NO hay acta ni comprobante en la carpeta: el motor calcula y
--      lo que de se documenta, sin valor esperado contra el que cuadrar.
--   2. La planilla del IESS SI las declara, con 482,00 y 30 dias cada una
--      (REF-06 §1). Es el mes donde el sistema DEBE discrepar: 22 filas de
--      PLANILLA contra 20 nominas, y 964,00 de masa de mas del lado del
--      cliente. Una coincidencia ahi seria el error (plan §3.4).
--   3. El IESS siguio cobrando el quirografario de Castro Arce (NUT 19854526,
--      14,79). En el rol no esta, asi que AQUI NO SE CARGA: el control 3
--      (prestamos contra el IESS) se hace a mano y debe dar 266,92 contra
--      281,71. ASOPREP asumio los 14,79 (REF-06 §2).
--   4. Desaparece el par de VACACIONES: INGRESOS y DESCUENTOS del rol se
--      comparan directos, sin restar nada. Primer mes en que el control 4
--      es limpio.
--   5. Prestamos del periodo 2026-03: hipotecarios 1.015,15 (Pazmino J. sube
--      a 145,30), quirografarios del rol 266,92 (Calderon 14,23 · Manosalvas
--      157,21 · Robayo 95,48, NUT 20048689, nuevo).
--   6. Sin anticipos. Mendez Torres sigue con 241,00 sobre 15 dias (ultimo
--      mes: desde el 01-04 pasa a tiempo completo por adenda).
--   7. ROL PROVISIONES trae para Mendez un SEGURO SALUD TIEMPO PARCIAL de
--      10,63 que la planilla del IESS no cobra (tiempo parcial 0,00). No se
--      contrasta: su TOTAL IESS sigue siendo 49,65. Pregunta para Steven.
--
-- LAS DIFERENCIAS QUE VAN A SALIR Y NO SON DEFECTO (plan §4)
--   - Robayo: 20,17 de IR que el cliente no retiene hasta agosto.
--   - Manosalvas: INGRESOS y LIQUIDO +0,01 (el libro resta sin redondear:
--     2.206,8333 - 726,06 = 1.480,77; nosotros 2.206,84 - 726,06 = 1.480,78).
--   - Munoz Santos: LIQUIDO -0,01 y TOTAL_IESS +0,01 (REF-06 §17).
--   - Mendez Torres: TOTAL_IESS 49,64 contra 49,65; dias 30 contra 15.
--   - Castro Arce y Cevallos Aleman: EN LA PLANILLA Y SIN NOMINA, 99,29 cada
--     una. Esperado y correcto.
--   Liquido esperado del motor: 17.591,12 - 20,17 + 0,01 - 0,01 = 17.570,95.
-- =====================================================

-- Repetible: borra lo de este mes antes de recargar.
DELETE FROM RHH.CTRL WHERE CTRLANOO = 2026 AND CTRLMESS = 3;


-- =====================================================
-- EL ROL DE MARZO -- hoja ROL MARZO, 20 trabajadores
-- =====================================================
-- Columnas del rol -> CPNMALTR: igual que en el script 31.
INSERT INTO RHH.CTRL (CTRLANOO, CTRLMESS, CTRLIDNT, CTRLALTR, CTRLVLOR, CTRLFNTE, CTRLUSRR)
SELECT 2026, 3, d.CED, d.ALTR, d.VLOR, 'ROL', 'CARGA' FROM (
    -- Sueldo (concepto 1)
    SELECT '1717991341' CED, 1 ALTR,  700.00 VLOR FROM DUAL UNION ALL
    SELECT '2150051205', 1,  700.00 FROM DUAL UNION ALL   -- Bravo Caiza, cedula correcta
    SELECT '1753528379', 1,  482.00 FROM DUAL UNION ALL
    SELECT '1719624809', 1,  700.00 FROM DUAL UNION ALL
    SELECT '1311981953', 1, 2000.00 FROM DUAL UNION ALL
    SELECT '1715156574', 1,  700.00 FROM DUAL UNION ALL
    SELECT '1750302984', 1,  700.00 FROM DUAL UNION ALL
    SELECT '1716120769', 1, 2000.00 FROM DUAL UNION ALL
    SELECT '1004350904', 1,  241.00 FROM DUAL UNION ALL
    SELECT '0103179537', 1, 1546.00 FROM DUAL UNION ALL
    SELECT '1717649873', 1,  550.00 FROM DUAL UNION ALL
    SELECT '1723962849', 1,  900.00 FROM DUAL UNION ALL
    SELECT '1726657164', 1,  700.00 FROM DUAL UNION ALL
    SELECT '0909917759', 1, 1500.00 FROM DUAL UNION ALL
    SELECT '2100192463', 1,  500.00 FROM DUAL UNION ALL
    SELECT '1725996498', 1, 1500.00 FROM DUAL UNION ALL
    SELECT '0801999855', 1,  700.00 FROM DUAL UNION ALL
    SELECT '1712362720', 1, 1500.00 FROM DUAL UNION ALL
    SELECT '1712232659', 1, 2200.00 FROM DUAL UNION ALL
    SELECT '1307779064', 1,  500.00 FROM DUAL UNION ALL
    -- Decimo tercero mensualizado (concepto 5) - los tres MENSUAL
    SELECT '1715156574', 5,   58.33 FROM DUAL UNION ALL
    SELECT '1716120769', 5,  166.67 FROM DUAL UNION ALL
    SELECT '0103179537', 5,  128.83 FROM DUAL UNION ALL
    -- Decimo cuarto mensualizado (concepto 6) - los mismos tres
    SELECT '1715156574', 6,   40.17 FROM DUAL UNION ALL
    SELECT '1716120769', 6,   40.17 FROM DUAL UNION ALL
    SELECT '0103179537', 6,   40.17 FROM DUAL UNION ALL
    -- Aporte personal IESS 9,45% (concepto 20) - los 20
    SELECT '1717991341', 20,  66.15 FROM DUAL UNION ALL
    SELECT '2150051205', 20,  66.15 FROM DUAL UNION ALL
    SELECT '1753528379', 20,  45.55 FROM DUAL UNION ALL
    SELECT '1719624809', 20,  66.15 FROM DUAL UNION ALL
    SELECT '1311981953', 20, 189.00 FROM DUAL UNION ALL
    SELECT '1715156574', 20,  66.15 FROM DUAL UNION ALL
    SELECT '1750302984', 20,  66.15 FROM DUAL UNION ALL
    SELECT '1716120769', 20, 189.00 FROM DUAL UNION ALL
    SELECT '1004350904', 20,  22.77 FROM DUAL UNION ALL
    SELECT '0103179537', 20, 146.10 FROM DUAL UNION ALL
    SELECT '1717649873', 20,  51.98 FROM DUAL UNION ALL
    SELECT '1723962849', 20,  85.05 FROM DUAL UNION ALL
    SELECT '1726657164', 20,  66.15 FROM DUAL UNION ALL
    SELECT '0909917759', 20, 141.75 FROM DUAL UNION ALL
    SELECT '2100192463', 20,  47.25 FROM DUAL UNION ALL
    SELECT '1725996498', 20, 141.75 FROM DUAL UNION ALL
    SELECT '0801999855', 20,  66.15 FROM DUAL UNION ALL
    SELECT '1712362720', 20, 141.75 FROM DUAL UNION ALL
    SELECT '1712232659', 20, 207.90 FROM DUAL UNION ALL
    SELECT '1307779064', 20,  47.25 FROM DUAL UNION ALL
    -- Prestamo quirografario IESS (concepto 23) - lo que el ROL descuenta
    SELECT '1719624809', 23,  14.23 FROM DUAL UNION ALL   -- NUT 19368191
    SELECT '1716120769', 23, 157.21 FROM DUAL UNION ALL   -- NUT 13795529
    SELECT '1725996498', 23,  95.48 FROM DUAL UNION ALL   -- NUT 20048689, Robayo, nuevo
    -- Castro Arce 14,79 (NUT 19854526): en el IESS, NO en el rol. No se carga.
    -- Prestamo hipotecario IESS (concepto 24) - detalle 2026-03
    SELECT '1715156574', 24, 490.00 FROM DUAL UNION ALL   -- NUT 311404
    SELECT '1716120769', 24, 379.85 FROM DUAL UNION ALL   -- NUT 7946837
    SELECT '0909917759', 24, 145.30 FROM DUAL             -- NUT 591589, 145,30 este mes
    -- Sin anticipos en marzo.
) d;


-- =====================================================
-- LOS TOTALES DE CABECERA, DEL ROL
-- =====================================================
-- Ya sin el par de vacaciones: INGRESOS = I:TOTAL, DESCUENTOS = D:TOTAL.
INSERT INTO RHH.CTRL (CTRLANOO, CTRLMESS, CTRLIDNT, CTRLTOTL, CTRLVLOR, CTRLFNTE, CTRLUSRR)
SELECT 2026, 3, d.CED, d.TOTL, d.VLOR, 'ROL', 'CARGA' FROM (
    SELECT '1717991341' CED, 'INGRESOS' TOTL,  700.00 VLOR FROM DUAL UNION ALL
    SELECT '1717991341', 'DESCUENTOS',   66.15 FROM DUAL UNION ALL
    SELECT '1717991341', 'LIQUIDO',     633.85 FROM DUAL UNION ALL
    SELECT '2150051205', 'INGRESOS',    700.00 FROM DUAL UNION ALL
    SELECT '2150051205', 'DESCUENTOS',   66.15 FROM DUAL UNION ALL
    SELECT '2150051205', 'LIQUIDO',     633.85 FROM DUAL UNION ALL
    SELECT '1753528379', 'INGRESOS',    482.00 FROM DUAL UNION ALL
    SELECT '1753528379', 'DESCUENTOS',   45.55 FROM DUAL UNION ALL
    SELECT '1753528379', 'LIQUIDO',     436.45 FROM DUAL UNION ALL
    SELECT '1719624809', 'INGRESOS',    700.00 FROM DUAL UNION ALL
    SELECT '1719624809', 'DESCUENTOS',   80.38 FROM DUAL UNION ALL
    SELECT '1719624809', 'LIQUIDO',     619.62 FROM DUAL UNION ALL
    SELECT '1311981953', 'INGRESOS',   2000.00 FROM DUAL UNION ALL
    SELECT '1311981953', 'DESCUENTOS',  189.00 FROM DUAL UNION ALL
    SELECT '1311981953', 'LIQUIDO',    1811.00 FROM DUAL UNION ALL
    SELECT '1715156574', 'INGRESOS',    798.50 FROM DUAL UNION ALL
    SELECT '1715156574', 'DESCUENTOS',  556.15 FROM DUAL UNION ALL
    SELECT '1715156574', 'LIQUIDO',     242.35 FROM DUAL UNION ALL
    SELECT '1750302984', 'INGRESOS',    700.00 FROM DUAL UNION ALL
    SELECT '1750302984', 'DESCUENTOS',   66.15 FROM DUAL UNION ALL
    SELECT '1750302984', 'LIQUIDO',     633.85 FROM DUAL UNION ALL
    SELECT '1716120769', 'INGRESOS',   2206.83 FROM DUAL UNION ALL
    SELECT '1716120769', 'DESCUENTOS',  726.06 FROM DUAL UNION ALL
    SELECT '1716120769', 'LIQUIDO',    1480.77 FROM DUAL UNION ALL
    SELECT '1004350904', 'INGRESOS',    241.00 FROM DUAL UNION ALL
    SELECT '1004350904', 'DESCUENTOS',   22.77 FROM DUAL UNION ALL
    SELECT '1004350904', 'LIQUIDO',     218.23 FROM DUAL UNION ALL
    SELECT '0103179537', 'INGRESOS',   1715.00 FROM DUAL UNION ALL
    SELECT '0103179537', 'DESCUENTOS',  146.10 FROM DUAL UNION ALL
    SELECT '0103179537', 'LIQUIDO',    1568.90 FROM DUAL UNION ALL
    SELECT '1717649873', 'INGRESOS',    550.00 FROM DUAL UNION ALL
    SELECT '1717649873', 'DESCUENTOS',   51.98 FROM DUAL UNION ALL
    SELECT '1717649873', 'LIQUIDO',     498.03 FROM DUAL UNION ALL
    SELECT '1723962849', 'INGRESOS',    900.00 FROM DUAL UNION ALL
    SELECT '1723962849', 'DESCUENTOS',   85.05 FROM DUAL UNION ALL
    SELECT '1723962849', 'LIQUIDO',     814.95 FROM DUAL UNION ALL
    SELECT '1726657164', 'INGRESOS',    700.00 FROM DUAL UNION ALL
    SELECT '1726657164', 'DESCUENTOS',   66.15 FROM DUAL UNION ALL
    SELECT '1726657164', 'LIQUIDO',     633.85 FROM DUAL UNION ALL
    SELECT '0909917759', 'INGRESOS',   1500.00 FROM DUAL UNION ALL
    SELECT '0909917759', 'DESCUENTOS',  287.05 FROM DUAL UNION ALL
    SELECT '0909917759', 'LIQUIDO',    1212.95 FROM DUAL UNION ALL
    SELECT '2100192463', 'INGRESOS',    500.00 FROM DUAL UNION ALL
    SELECT '2100192463', 'DESCUENTOS',   47.25 FROM DUAL UNION ALL
    SELECT '2100192463', 'LIQUIDO',     452.75 FROM DUAL UNION ALL
    SELECT '1725996498', 'INGRESOS',   1500.00 FROM DUAL UNION ALL
    SELECT '1725996498', 'DESCUENTOS',  237.23 FROM DUAL UNION ALL
    SELECT '1725996498', 'LIQUIDO',    1262.77 FROM DUAL UNION ALL
    SELECT '0801999855', 'INGRESOS',    700.00 FROM DUAL UNION ALL
    SELECT '0801999855', 'DESCUENTOS',   66.15 FROM DUAL UNION ALL
    SELECT '0801999855', 'LIQUIDO',     633.85 FROM DUAL UNION ALL
    SELECT '1712362720', 'INGRESOS',   1500.00 FROM DUAL UNION ALL
    SELECT '1712362720', 'DESCUENTOS',  141.75 FROM DUAL UNION ALL
    SELECT '1712362720', 'LIQUIDO',    1358.25 FROM DUAL UNION ALL
    SELECT '1712232659', 'INGRESOS',   2200.00 FROM DUAL UNION ALL
    SELECT '1712232659', 'DESCUENTOS',  207.90 FROM DUAL UNION ALL
    SELECT '1712232659', 'LIQUIDO',    1992.10 FROM DUAL UNION ALL
    SELECT '1307779064', 'INGRESOS',    500.00 FROM DUAL UNION ALL
    SELECT '1307779064', 'DESCUENTOS',   47.25 FROM DUAL UNION ALL
    SELECT '1307779064', 'LIQUIDO',     452.75 FROM DUAL
) d;


-- =====================================================
-- EL CONTROL 2: LA PLANILLA DEL IESS DEL PERIODO 2026-03
-- =====================================================
-- 22 afiliados y no 20: Castro Arce y Cevallos Aleman declaradas con 482,00 y
-- 30 dias aunque salieron el 06-03 (REF-06 §1). Se cargan TAL CUAL: la
-- discrepancia contra nuestras 20 nominas es la esperada del plan §3.4.
-- TOTAL_IESS = SUELDO x 20,60 %. Coincide con ROL PROVISIONES col. T.
INSERT INTO RHH.CTRL (CTRLANOO, CTRLMESS, CTRLIDNT, CTRLTOTL, CTRLVLOR, CTRLFNTE, CTRLUSRR)
SELECT 2026, 3, d.CED, 'TOTAL_IESS', ROUND(d.SLDO * 0.206, 2), 'PLANILLA', 'CARGA' FROM (
    SELECT '1717991341' CED,  700.00 SLDO FROM DUAL UNION ALL
    SELECT '2150051205',      700.00 FROM DUAL UNION ALL   -- Bravo Caiza
    SELECT '1753528379',      482.00 FROM DUAL UNION ALL
    SELECT '1719624809',      700.00 FROM DUAL UNION ALL
    SELECT '1720245735',      482.00 FROM DUAL UNION ALL   -- Castro Arce: salio el 06-03, declarada entera
    SELECT '1716501778',      482.00 FROM DUAL UNION ALL   -- Cevallos Aleman: idem
    SELECT '1311981953',     2000.00 FROM DUAL UNION ALL
    SELECT '1715156574',      700.00 FROM DUAL UNION ALL
    SELECT '1750302984',      700.00 FROM DUAL UNION ALL
    SELECT '1716120769',     2000.00 FROM DUAL UNION ALL
    SELECT '1004350904',      241.00 FROM DUAL UNION ALL   -- Mendez Torres, 15 dias
    SELECT '0103179537',     1546.00 FROM DUAL UNION ALL
    SELECT '1717649873',      550.00 FROM DUAL UNION ALL
    SELECT '1723962849',      900.00 FROM DUAL UNION ALL
    SELECT '1726657164',      700.00 FROM DUAL UNION ALL
    SELECT '0909917759',     1500.00 FROM DUAL UNION ALL
    SELECT '2100192463',      500.00 FROM DUAL UNION ALL
    SELECT '1725996498',     1500.00 FROM DUAL UNION ALL
    SELECT '0801999855',      700.00 FROM DUAL UNION ALL
    SELECT '1712362720',     1500.00 FROM DUAL UNION ALL
    SELECT '1712232659',     2200.00 FROM DUAL UNION ALL
    SELECT '1307779064',      500.00 FROM DUAL
) d;

COMMIT;


-- =====================================================
-- COMPROBACION DE LA CARGA -- contra los totales del cliente
-- =====================================================
SELECT CTRLFNTE, COUNT(*) AS FILAS, COUNT(DISTINCT CTRLIDNT) AS PERSONAS
  FROM RHH.CTRL WHERE CTRLANOO = 2026 AND CTRLMESS = 3
 GROUP BY CTRLFNTE ORDER BY CTRLFNTE;
-- Esperado: PLANILLA 22 / 22 · ROL 112 / 20
--   (112 = 52 conceptos + 60 totales; conceptos: 20 sueldos, 3 y 3 decimos,
--    20 aportes, 3 quirografarios, 3 hipotecarios)

SELECT CTRLTOTL, ROUND(SUM(CTRLVLOR), 2) AS TOTAL
  FROM RHH.CTRL WHERE CTRLANOO = 2026 AND CTRLMESS = 3 AND CTRLFNTE = 'ROL'
   AND CTRLTOTL IS NOT NULL
 GROUP BY CTRLTOTL ORDER BY CTRLTOTL;
-- Esperado: DESCUENTOS 3.202,22 · INGRESOS 20.793,33 · LIQUIDO 17.591,12
--   (20.793,33 - 3.202,22 = 17.591,11: el centavo es el de Munoz, plan §3.3)

SELECT CTRLALTR, ROUND(SUM(CTRLVLOR), 2) AS TOTAL, COUNT(*) AS PERSONAS
  FROM RHH.CTRL WHERE CTRLANOO = 2026 AND CTRLMESS = 3 AND CTRLALTR IS NOT NULL
 GROUP BY CTRLALTR ORDER BY CTRLALTR;
-- Esperado:  1 -> 20.319,00 / 20
--            5 ->    353,83 /  3
--            6 ->    120,51 /  3
--           20 ->  1.920,15 / 20
--           23 ->    266,92 /  3   (el IESS cobro 281,71: +14,79 de Castro Arce)
--           24 ->  1.015,15 /  3

SELECT ROUND(SUM(CTRLVLOR), 2) AS TOTAL_IESS_PLANILLA
  FROM RHH.CTRL WHERE CTRLANOO = 2026 AND CTRLMESS = 3 AND CTRLTOTL = 'TOTAL_IESS';
-- Esperado: 4.384,30 -- incluye 198,58 de las dos que ya no estaban.
--   Lo nuestro debe dar 4.185,72 (20 personas; 4.185,71 de ROL PROVISIONES
--   + el centavo de Munoz). La diferencia de 198,58 ES la esperada.


-- =====================================================
-- LO QUE FALTA PARA CONTRASTAR (por pantalla, frontend)
-- =====================================================
-- 1. Crear el periodo 2026-03 con PRDNMODO = 1.
-- 2. Las dos salidas, por la pantalla de liquidacion (fase 8):
--      Castro Arce     1720245735  salida 06-03-2026  notificacion en periodo de prueba
--      Cevallos Aleman 1716501778  salida 06-03-2026  idem
--    Ingreso de ambas 08-12-2025, RMU 482,00. Sin indemnizacion (periodo de
--    prueba). Sin acta: se reporta lo que el motor de, sin ajustar.
-- 3. Novedades del mes, las seis cuotas IESS del detalle 2026-03 COMO LAS
--    DESCUENTA EL ROL (no como las cobra el IESS):
--      Hipotecario   Cossio             490,00   NUT 311404
--      Hipotecario   Manosalvas         379,85   NUT 7946837
--      Hipotecario   Pazmino Jaramillo  145,30   NUT 591589   (no 145,29)
--      Quirografario Calderon            14,23   NUT 19368191
--      Quirografario Manosalvas         157,21   NUT 13795529
--      Quirografario Robayo              95,48   NUT 20048689 (nuevo)
--    El de Castro Arce (14,79) NO se registra: ya no esta en nomina.
--    Anticipos: ninguno. Ojo: Calderon ya no tiene cuota del de diciembre
--    (eran dos, enero y febrero) y Pardo tampoco; si el motor genera alguno,
--    es defecto.
-- 4. Calcular. Esperado: 20 nominas, liquido 17.570,95.
-- 5. CONTRASTE_MES_CONTRA_ROL_REAL.sql con ANIO=2026, MES=3. Bloque 4 primero:
--    22 personas en CTRL contra 20 nominas es lo correcto este mes.
