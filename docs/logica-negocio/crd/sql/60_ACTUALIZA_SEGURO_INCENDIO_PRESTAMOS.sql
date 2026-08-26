-- =============================================================================
-- 60 - ACTUALIZACION DEL SEGURO DE INCENDIO POR PRESTAMO (131 prestamos)
--      Alcance: cuotas NO PAGADAS con vencimiento en SEP / OCT / NOV 2026
-- =============================================================================
--
-- QUE HACE
--   Sobre las cuotas de los 131 prestamos identificados por PRSTIDAS
--   (idAsoprep) que cumplan LAS DOS condiciones de alcance:
--       DTPRFCVN entre 2026-09-01 y 2026-11-30  (vencimiento en sep/oct/nov 2026)
--       DTPRESTD NOT IN (4, 7)                  (ni PAGADA ni CANCELADA ANTICIPADA)
--   hace:
--       1. DTPRVLSI (valor seguro incendio) = el valor de la lista
--       2. DTPRTTLL (total) = capital + interes + mora + desgravamen + seguro
--       3. DTPRTTCS (totalConSeguro) = el mismo valor que DTPRTTLL
--
-- ESTE SCRIPT MODIFICA DATOS. Los pasos 0 y 1 son de lectura y hay que correrlos
-- y leerlos ANTES de llegar al paso 3. El paso 2 crea el respaldo y no es
-- opcional: es la unica via de reverso.
--
-- -----------------------------------------------------------------------------
-- SOBRE EL ALCANCE
-- -----------------------------------------------------------------------------
--   COLUMNA DE ESTADO: se usa DTPRESTD, no DTPRIDST. En CRD.DTPR el estado
--     vigente es DTPRESTD; DTPRIDST se graba como copia y puede quedar
--     desfasado (ver la tabla de trampas en CLAUDE.md). Filtrar por la columna
--     equivocada dejaria fuera cuotas que si habia que tocar.
--
--   "NO PAGADAS" = DTPRESTD NOT IN (4, 7). Se excluyen dos estados:
--       4 = PAGADA
--       7 = CANCELADA_ANTICIPADA  <- tambien esta liquidada; ponerle seguro
--                                    nuevo la dejaria debiendo.
--     Quedan DENTRO, entre otros, PENDIENTE (1), ACTIVA (2), EN_MORA (5) y
--     PARCIAL (6). PARCIAL entra a proposito: recibio un abono pero no esta
--     saldada. Si se prefiere ser literal y excluir SOLO el 4, ver VARIANTE B.
--     El CONTROL 1.5 muestra que estados hay realmente en la ventana.
--
--   VENTANA DE FECHAS: DTPRFCVN >= DATE '2026-09-01' AND < DATE '2026-12-01'.
--     El limite superior es el primer dia de diciembre, no el 30 de noviembre:
--     DTPRFCVN es TIMESTAMP y un vencimiento del 30-nov con hora distinta de
--     00:00 se perderia con un <= DATE '2026-11-30'.
--
--   Con esta ventana, un prestamo mensual deberia aportar 3 cuotas. El
--   CONTROL 1.4 marca los que aportan menos de 3 o ninguna.
--
-- -----------------------------------------------------------------------------
-- POR QUE HAY QUE TOCAR DTPRTTCS Y NO SOLO DTPRTTLL
-- -----------------------------------------------------------------------------
--   En todo el codigo que escribe una cuota, DTPRTTCS se graba SIEMPRE con el
--   mismo valor que DTPRTTLL:
--     PrestamoServiceImpl:244-245, AbonoCapitalPrestamoServiceImpl:651-652,
--     ProcesoMoraPrestamoServiceImpl:257-258.
--   Si se actualiza solo el total, totalConSeguro queda desfasado y cualquier
--   pantalla o reporte que lea esa columna muestra el valor viejo.
--
-- -----------------------------------------------------------------------------
-- ⚠ EL MOTOR DE PAGOS NO LEE DTPRTTLL - Y SUMA UN COMPONENTE MAS
-- -----------------------------------------------------------------------------
--   MotorPagoPrestamoServiceImpl:678-685 calcula lo que la cuota debe asi:
--
--       capital + interes + desgravamen + valorSeguroIncendio + mora + interesVencido
--                                                                      ^^^^^^^^^^^^^^
--   NO lee DTPRTTLL: lo recompone desde las columnas. Y suma DTPRINVN
--   (interesVencido), que NO esta en la formula pedida.
--
--   En cuotas de sep/oct/nov 2026 -meses futuros respecto de hoy- lo esperable
--   es que DTPRMRAA y DTPRINVN esten en 0 y las dos formulas coincidan. El
--   CONTROL 1.6 lo verifica en la ventana concreta:
--     - 0 filas -> no hay nada que decidir, seguir con el MERGE tal cual.
--     - con filas -> en esas cuotas DTPRTTLL quedaria por debajo de lo que
--       cobra el motor; aplicar la VARIANTE A.
--
--   Nota sobre "interes en mora": se interpreto como DTPRMRAA (mora), que es la
--   columna que ProcesoMoraPrestamoServiceImpl:252-257 suma al total. DTPRINVN
--   (interes vencido) es otra cosa y por eso queda fuera por defecto.
--
-- -----------------------------------------------------------------------------
-- LO QUE ESTE SCRIPT NO TOCA (a proposito)
-- -----------------------------------------------------------------------------
--   Cuotas fuera de la ventana sep-nov 2026: quedan con su seguro actual. Si el
--     seguro debia aplicarse a toda la vida del prestamo, este script cubre
--     solo tres meses y hay que decirlo.
--   PRST.PRSTPRIN (primaSeguroIncendio) y PRST.PRSTTSIN (tasa): no se pidieron.
--     Si el valor de la lista es la prima del prestamo y no el valor por cuota,
--     hay que decirlo ANTES: cambia todo el planteamiento.
--   PRST.PRSTTTSG (totalSeguros): ninguna linea del codigo la escribe
--     (setTotalSeguros solo existe como setter), asi que se deja como esta.
--   PRST.PRSTTTPR (totalPrestamo): PrestamoServiceImpl:296 lo calcula como
--     totalCapital + totalInteres, sin seguros. No le afecta este cambio.
--   DTPRSLDO (saldo): ver PASO 5, opcional.
--
-- =============================================================================


-- =============================================================================
-- PASO 0 - TABLA DE TRABAJO CON LOS 131 PARES (idAsoprep, valor seguro)
-- =============================================================================
-- Se crea una tabla real (no temporal) para que sobreviva entre statements
-- aunque el cliente reconecte. Se elimina en el PASO 6.
-- Si el usuario de BD no tiene permiso de CREATE TABLE, ver la VARIANTE C.
-- =============================================================================

CREATE TABLE CRD.TMP_SEG_INCENDIO (
    IDAS  NUMBER        NOT NULL,
    VLSI  NUMBER(18,2)  NOT NULL
);

INSERT ALL
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66200, 8.95)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65734, 12.61)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65647, 55.76)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67460, 13.04)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68548, 24.50)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66299, 12.66)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67673, 36.61)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66209, 25.64)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65891, 18.28)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67135, 4.94)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65485, 38.95)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66714, 42.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66905, 99.73)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67163, 27.68)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67076, 14.82)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (69185, 69.23)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68607, 12.17)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68101, 8.28)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65706, 53.99)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67194, 5.19)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65864, 52.79)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (69891, 7.80)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66224, 47.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66970, 67.19)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65841, 178.83)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67549, 4.99)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66212, 16.21)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65682, 45.73)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67155, 15.85)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66872, 32.41)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66183, 133.16)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65565, 12.70)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67038, 25.73)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66376, 40.27)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (70178, 23.47)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65462, 68.33)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67515, 7.13)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68229, 12.30)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67086, 70.72)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67793, 7.35)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65721, 13.26)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65908, 98.98)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67402, 37.93)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66605, 30.93)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68020, 7.82)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (69987, 18.09)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68598, 38.89)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67299, 7.42)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67023, 52.23)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66855, 6.86)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68057, 26.38)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65525, 5.54)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68033, 22.55)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66469, 36.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65684, 49.64)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65689, 24.05)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67070, 16.58)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67606, 9.78)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67095, 12.03)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67231, 63.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67858, 1.72)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (70018, 2.50)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67187, 1.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66569, 7.08)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67344, 1.19)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67326, 18.40)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68698, 3.75)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66632, 6.81)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65526, 6.53)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68095, 2.42)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67470, 2.83)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67989, 30.14)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68446, 3.04)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65854, 4.54)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66442, 4.54)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65934, 13.75)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68072, 1.92)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68595, 7.08)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65858, 2.44)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68263, 1.60)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66810, 2.72)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65593, 1.00)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67670, 12.41)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67221, 2.41)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (69836, 64.15)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68191, 6.51)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66173, 2.67)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67700, 3.06)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67797, 1.73)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67490, 1.96)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67335, 2.45)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67034, 2.82)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67996, 4.78)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68025, 1.67)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68593, 1.25)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66993, 2.46)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65831, 1.39)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67291, 6.83)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66234, 9.02)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67699, 7.34)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67022, 6.34)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (68044, 1.32)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67913, 1.11)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67877, 5.03)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67381, 11.24)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66839, 4.80)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67091, 2.71)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66870, 8.01)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67191, 2.27)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65524, 1.96)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67809, 2.63)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66433, 17.12)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67014, 17.82)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66120, 2.62)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65746, 3.84)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67228, 2.74)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65386, 4.58)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66201, 5.27)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66819, 4.80)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66038, 1.37)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67382, 1.81)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66818, 1.98)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (70187, 3.98)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (65687, 2.16)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66898, 2.24)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67524, 5.93)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67051, 3.93)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66155, 22.43)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (67601, 1.32)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (63469, 4.15)
  INTO CRD.TMP_SEG_INCENDIO (IDAS, VLSI) VALUES (66297, 2.51)
SELECT * FROM DUAL;

COMMIT;


-- =============================================================================
-- PASO 1 - CONTROLES ANTES DE TOCAR NADA
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CONTROL 1.1 - La lista se cargo completa y sin errores de transcripcion
-- -----------------------------------------------------------------------------
-- Esperado exactamente:  FILAS = 131   IDAS_DISTINTOS = 131   SUMA_VALORES = 2457.08
-- Si SUMA_VALORES no cuadra contra la suma de la columna original, PARAR:
-- hay un valor mal transcrito y no se puede seguir.
-- -----------------------------------------------------------------------------
SELECT  COUNT(*)              AS FILAS,
        COUNT(DISTINCT IDAS)  AS IDAS_DISTINTOS,
        SUM(VLSI)             AS SUMA_VALORES,
        MIN(VLSI)             AS VALOR_MINIMO,
        MAX(VLSI)             AS VALOR_MAXIMO
FROM    CRD.TMP_SEG_INCENDIO;


-- -----------------------------------------------------------------------------
-- CONTROL 1.2 - idAsoprep de la lista que NO existen en CRD.PRST
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas. Cada fila que salga es un prestamo que NO se va a
-- actualizar y que hay que revisar a mano.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS, v.VLSI
FROM    CRD.TMP_SEG_INCENDIO v
WHERE   NOT EXISTS (SELECT 1 FROM CRD.PRST p WHERE p.PRSTIDAS = v.IDAS)
ORDER BY v.IDAS;


-- -----------------------------------------------------------------------------
-- CONTROL 1.3 - idAsoprep que apuntan a MAS DE UN prestamo
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas. Si sale alguno, el MERGE del PASO 3 aborta con ORA-30926
-- ("unable to get a stable set of rows") y ademas significa que ese valor se
-- aplicaria a dos prestamos distintos. Resolver antes de continuar.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS,
        COUNT(*)                          AS PRESTAMOS,
        LISTAGG(p.PRSTCDGO, ', ')
            WITHIN GROUP (ORDER BY p.PRSTCDGO) AS IDS_PRESTAMO
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS
GROUP BY v.IDAS
HAVING  COUNT(*) > 1
ORDER BY 1;


-- -----------------------------------------------------------------------------
-- CONTROL 1.4 - ⚠ COBERTURA: cuantas cuotas cae dentro del alcance por prestamo
-- -----------------------------------------------------------------------------
-- Lo importante es la columna CUOTAS_EN_ALCANCE.
--   = 3  -> lo esperado en un prestamo mensual (sep, oct y nov).
--   < 3  -> falta algun mes: o ya esta pagado, o el prestamo termina antes, o
--           no es mensual.
--   = 0  -> ese prestamo NO se va a actualizar en absoluto. Revisar uno por uno.
-- MESES_EN_ALCANCE lista los meses que si entran, para ver cual falta.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS                                              AS ID_ASOPREP,
        p.PRSTCDGO                                          AS ID_PRESTAMO,
        p.PRSTIDST                                          AS ESTADO_PRESTAMO,
        v.VLSI                                              AS SEGURO_NUEVO,
        COUNT(d.DTPRCDGO)                                   AS CUOTAS_TOTALES,
        SUM(CASE WHEN d.DTPRFCVN >= DATE '2026-09-01'
                  AND d.DTPRFCVN <  DATE '2026-12-01'
                 THEN 1 ELSE 0 END)                         AS CUOTAS_EN_VENTANA,
        SUM(CASE WHEN d.DTPRFCVN >= DATE '2026-09-01'
                  AND d.DTPRFCVN <  DATE '2026-12-01'
                  AND NVL(d.DTPRESTD, 0) NOT IN (4, 7)
                 THEN 1 ELSE 0 END)                         AS CUOTAS_EN_ALCANCE,
        LISTAGG(CASE WHEN d.DTPRFCVN >= DATE '2026-09-01'
                      AND d.DTPRFCVN <  DATE '2026-12-01'
                      AND NVL(d.DTPRESTD, 0) NOT IN (4, 7)
                     THEN TO_CHAR(d.DTPRFCVN, 'YYYY-MM') END, ', ')
            WITHIN GROUP (ORDER BY d.DTPRFCVN)              AS MESES_EN_ALCANCE
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS
LEFT    JOIN CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
GROUP BY v.IDAS, p.PRSTCDGO, p.PRSTIDST, v.VLSI
ORDER BY CUOTAS_EN_ALCANCE, v.IDAS;


-- -----------------------------------------------------------------------------
-- CONTROL 1.5 - Que estados hay en la ventana sep-nov 2026
-- -----------------------------------------------------------------------------
-- Muestra la distribucion completa, incluidos los estados que el alcance
-- EXCLUYE. Sirve para confirmar el criterio de "no pagadas": si aparece mucho
-- volumen en el 7 (CANCELADA ANTICIPADA) o si se quiere incluir/excluir el 6
-- (PARCIAL), este es el momento de decidirlo.
-- Estados: 1 PENDIENTE  2 ACTIVA  3 EMITIDA  4 PAGADA  5 EN_MORA
--          6 PARCIAL    7 CANCELADA_ANTICIPADA         8 VENCIDA
-- -----------------------------------------------------------------------------
SELECT  d.DTPRESTD                                    AS ESTADO_CUOTA,
        CASE d.DTPRESTD WHEN 1 THEN 'PENDIENTE'
                        WHEN 2 THEN 'ACTIVA'
                        WHEN 3 THEN 'EMITIDA'
                        WHEN 4 THEN 'PAGADA'
                        WHEN 5 THEN 'EN MORA'
                        WHEN 6 THEN 'PARCIAL'
                        WHEN 7 THEN 'CANCELADA ANTICIPADA'
                        WHEN 8 THEN 'VENCIDA'
                        ELSE TO_CHAR(d.DTPRESTD) END  AS ESTADO_NOMBRE,
        CASE WHEN NVL(d.DTPRESTD, 0) IN (4, 7) THEN 'EXCLUIDA'
             ELSE 'SE ACTUALIZA' END                  AS EN_ALCANCE,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM')                AS MES_VENCIMIENTO,
        COUNT(*)                                      AS CUOTAS,
        COUNT(DISTINCT d.PRSTCDGO)                    AS PRESTAMOS
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS  = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO  = p.PRSTCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
GROUP BY d.DTPRESTD, TO_CHAR(d.DTPRFCVN, 'YYYY-MM')
ORDER BY MES_VENCIMIENTO, ESTADO_CUOTA;


-- -----------------------------------------------------------------------------
-- CONTROL 1.6 - ⚠ DECISION: mora o interes vencido dentro del alcance
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas, porque son cuotas futuras.
-- Si sale algo con INTERES_VENCIDO > 0, DTPRTTLL va a quedar por debajo de lo
-- que cobra el motor de pagos (MotorPagoPrestamoServiceImpl:684) -> VARIANTE A.
-- Si sale algo con MORA > 0, la formula ya la contempla, pero conviene mirar
-- por que una cuota de sep-nov 2026 ya tiene mora.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS                          AS ID_ASOPREP,
        p.PRSTCDGO                      AS ID_PRESTAMO,
        d.DTPRCDGO                      AS ID_CUOTA,
        d.DTPRNMCT                      AS NUMERO_CUOTA,
        d.DTPRFCVN                      AS FECHA_VENCIMIENTO,
        d.DTPRESTD                      AS ESTADO_CUOTA,
        d.DTPRMRAA                      AS MORA,
        d.DTPRINVN                      AS INTERES_VENCIDO,
        d.DTPRTTLL                      AS TOTAL_ACTUAL
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS  = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO  = p.PRSTCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
AND     (NVL(d.DTPRMRAA, 0) > 0 OR NVL(d.DTPRINVN, 0) > 0)
ORDER BY v.IDAS, d.DTPRNMCT;


-- -----------------------------------------------------------------------------
-- CONTROL 1.7 - Detalle fila a fila de lo que se va a modificar
-- -----------------------------------------------------------------------------
-- Esta es la lista exacta de cuotas que el MERGE va a tocar, con el valor
-- actual y el que quedaria. Guardarla: es el "antes" contra el que se compara
-- el PASO 4. Deberia traer aproximadamente 131 x 3 = 393 filas.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS                                        AS ID_ASOPREP,
        p.PRSTCDGO                                    AS ID_PRESTAMO,
        d.DTPRCDGO                                    AS ID_CUOTA,
        d.DTPRNMCT                                    AS NUMERO_CUOTA,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD')             AS FECHA_VENCIMIENTO,
        d.DTPRESTD                                    AS ESTADO_CUOTA,
        NVL(d.DTPRCPTL, 0)                            AS CAPITAL,
        NVL(d.DTPRINTR, 0)                            AS INTERES,
        NVL(d.DTPRMRAA, 0)                            AS MORA,
        NVL(d.DTPRDSGR, 0)                            AS DESGRAVAMEN,
        NVL(d.DTPRVLSI, 0)                            AS SEGURO_ACTUAL,
        v.VLSI                                        AS SEGURO_NUEVO,
        NVL(d.DTPRTTLL, 0)                            AS TOTAL_ACTUAL,
        ROUND( NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) + NVL(d.DTPRMRAA,0)
             + NVL(d.DTPRDSGR,0) + v.VLSI, 2)         AS TOTAL_NUEVO,
        ROUND( NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) + NVL(d.DTPRMRAA,0)
             + NVL(d.DTPRDSGR,0) + v.VLSI
             - NVL(d.DTPRTTLL,0), 2)                  AS DELTA
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS  = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO  = p.PRSTCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
ORDER BY v.IDAS, d.DTPRNMCT;


-- =============================================================================
-- PASO 2 - RESPALDO (OBLIGATORIO)
-- =============================================================================
-- Copia completa de TODAS las cuotas de los 131 prestamos, no solo las del
-- alcance: si despues hay que revisar algo fuera de la ventana, la foto ya
-- esta. Es la unica forma de revertir. NO se borra al final: dejarla hasta que
-- el cambio este validado en produccion.
-- =============================================================================
CREATE TABLE CRD.BKP_DTPR_SEG_INC_20260826 AS
SELECT  d.*
FROM    CRD.DTPR d
WHERE   d.PRSTCDGO IN (SELECT p.PRSTCDGO
                       FROM   CRD.PRST p
                       JOIN   CRD.TMP_SEG_INCENDIO v ON v.IDAS = p.PRSTIDAS);

-- Verificar que el respaldo tiene filas antes de continuar.
SELECT COUNT(*) AS FILAS_RESPALDADAS FROM CRD.BKP_DTPR_SEG_INC_20260826;


-- =============================================================================
-- PASO 3 - EL CAMBIO
-- =============================================================================
-- Alcance:  vencimiento en sep/oct/nov 2026  Y  estado NOT IN (4, 7)
-- Formula:  DTPRVLSI = valor de la lista
--           DTPRTTLL = ROUND(capital + interes + mora + desgravamen + seguro, 2)
--           DTPRTTCS = mismo valor que DTPRTTLL
--
-- El numero de filas actualizadas debe coincidir con las filas del CONTROL 1.7.
-- Antes de ejecutar, revisar los controles 1.4, 1.5 y 1.6.
-- =============================================================================
MERGE INTO CRD.DTPR d
USING ( SELECT  p.PRSTCDGO, v.VLSI
        FROM    CRD.TMP_SEG_INCENDIO v
        JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS ) src
ON      (d.PRSTCDGO = src.PRSTCDGO)
WHEN MATCHED THEN UPDATE SET
        d.DTPRVLSI = src.VLSI,
        d.DTPRTTLL = ROUND( NVL(d.DTPRCPTL, 0)
                          + NVL(d.DTPRINTR, 0)
                          + NVL(d.DTPRMRAA, 0)
                          + NVL(d.DTPRDSGR, 0)
                          + src.VLSI, 2),
        d.DTPRTTCS = ROUND( NVL(d.DTPRCPTL, 0)
                          + NVL(d.DTPRINTR, 0)
                          + NVL(d.DTPRMRAA, 0)
                          + NVL(d.DTPRDSGR, 0)
                          + src.VLSI, 2)
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7);

COMMIT;


-- =============================================================================
-- PASO 4 - CONTROLES DESPUES
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CONTROL 4.1 - Toda cuota del alcance quedo con el seguro que le toca
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS, p.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT,
        d.DTPRVLSI AS SEGURO_GRABADO, v.VLSI AS SEGURO_ESPERADO
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
AND     NVL(d.DTPRVLSI, -1) <> v.VLSI
ORDER BY v.IDAS, d.DTPRNMCT;


-- -----------------------------------------------------------------------------
-- CONTROL 4.2 - El total cuadra con sus componentes, y TTCS con TTLL
-- -----------------------------------------------------------------------------
-- Esperado: 0 filas.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS, p.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT,
        d.DTPRCPTL AS CAPITAL, d.DTPRINTR AS INTERES, d.DTPRMRAA AS MORA,
        d.DTPRDSGR AS DESGRAVAMEN, d.DTPRVLSI AS SEGURO,
        d.DTPRTTLL AS TOTAL_GRABADO,
        ROUND(NVL(d.DTPRCPTL,0)+NVL(d.DTPRINTR,0)+NVL(d.DTPRMRAA,0)
             +NVL(d.DTPRDSGR,0)+NVL(d.DTPRVLSI,0), 2) AS TOTAL_ESPERADO,
        d.DTPRTTCS AS TOTAL_CON_SEGURO
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
AND     ( NVL(d.DTPRTTLL, -1) <> ROUND(NVL(d.DTPRCPTL,0)+NVL(d.DTPRINTR,0)
                                      +NVL(d.DTPRMRAA,0)+NVL(d.DTPRDSGR,0)
                                      +NVL(d.DTPRVLSI,0), 2)
       OR NVL(d.DTPRTTCS, -1) <> NVL(d.DTPRTTLL, -2) )
ORDER BY v.IDAS, d.DTPRNMCT;


-- -----------------------------------------------------------------------------
-- CONTROL 4.3 - NADA fuera del alcance se movio
-- -----------------------------------------------------------------------------
-- Compara contra el respaldo TODA cuota que NO estaba en el alcance. Esperado:
-- 0 filas. Si sale algo, el MERGE toco de mas y hay que revertir.
-- -----------------------------------------------------------------------------
SELECT  d.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT,
        TO_CHAR(d.DTPRFCVN, 'YYYY-MM-DD') AS FECHA_VENCIMIENTO,
        d.DTPRESTD                        AS ESTADO_CUOTA,
        b.DTPRVLSI AS SEGURO_ANTES, d.DTPRVLSI AS SEGURO_DESPUES,
        b.DTPRTTLL AS TOTAL_ANTES,  d.DTPRTTLL AS TOTAL_DESPUES
FROM    CRD.DTPR d
JOIN    CRD.BKP_DTPR_SEG_INC_20260826 b ON b.DTPRCDGO = d.DTPRCDGO
WHERE   NOT ( d.DTPRFCVN >= DATE '2026-09-01'
              AND d.DTPRFCVN < DATE '2026-12-01'
              AND NVL(d.DTPRESTD, 0) NOT IN (4, 7) )
AND     ( NVL(d.DTPRVLSI, -1) <> NVL(b.DTPRVLSI, -1)
       OR NVL(d.DTPRTTLL, -1) <> NVL(b.DTPRTTLL, -1)
       OR NVL(d.DTPRTTCS, -1) <> NVL(b.DTPRTTCS, -1) )
ORDER BY d.PRSTCDGO, d.DTPRNMCT;


-- -----------------------------------------------------------------------------
-- CONTROL 4.4 - Antes / despues por prestamo
-- -----------------------------------------------------------------------------
-- DELTA_TOTAL es cuanto sube la deuda registrada de cada prestamo en los tres
-- meses. Deberia ser aproximadamente  seguro_nuevo x cuotas_actualizadas,
-- menos el seguro que ya tuvieran.
-- -----------------------------------------------------------------------------
SELECT  v.IDAS                                        AS ID_ASOPREP,
        p.PRSTCDGO                                    AS ID_PRESTAMO,
        v.VLSI                                        AS SEGURO_NUEVO,
        COUNT(*)                                      AS CUOTAS_ACTUALIZADAS,
        SUM(NVL(b.DTPRVLSI, 0))                       AS SEGURO_ANTES,
        SUM(NVL(d.DTPRVLSI, 0))                       AS SEGURO_DESPUES,
        SUM(NVL(b.DTPRTTLL, 0))                       AS TOTAL_ANTES,
        SUM(NVL(d.DTPRTTLL, 0))                       AS TOTAL_DESPUES,
        ROUND(SUM(NVL(d.DTPRTTLL,0)) - SUM(NVL(b.DTPRTTLL,0)), 2) AS DELTA_TOTAL
FROM    CRD.TMP_SEG_INCENDIO v
JOIN    CRD.PRST p ON p.PRSTIDAS = v.IDAS
JOIN    CRD.DTPR d ON d.PRSTCDGO = p.PRSTCDGO
JOIN    CRD.BKP_DTPR_SEG_INC_20260826 b ON b.DTPRCDGO = d.DTPRCDGO
WHERE   d.DTPRFCVN >= DATE '2026-09-01'
AND     d.DTPRFCVN <  DATE '2026-12-01'
AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7)
GROUP BY v.IDAS, p.PRSTCDGO, v.VLSI
ORDER BY ABS(SUM(NVL(d.DTPRTTLL,0)) - SUM(NVL(b.DTPRTTLL,0))) DESC;


-- =============================================================================
-- PASO 5 - OPCIONAL: recomponer DTPRSLDO de las cuotas actualizadas
-- =============================================================================
-- El saldo grabado queda desfasado hasta que el motor de pagos vuelva a tocar
-- la cuota; el motor lo recalcula solo (MotorPagoPrestamoServiceImpl:180) con
--     saldo = capital+interes+desgravamen+seguro+mora+interesVencido - pagado
-- Este UPDATE hace exactamente lo mismo, sobre el mismo alcance, para que la
-- columna no muestre un valor viejo mientras tanto. Ejecutar SOLO si se decide
-- que el saldo debe reflejar el cambio de inmediato.
--
-- OJO: aqui SI entra DTPRINVN, porque el objetivo es igualar al motor.
-- =============================================================================
-- UPDATE  CRD.DTPR d
-- SET     d.DTPRSLDO = GREATEST(0, ROUND(
--             NVL(d.DTPRCPTL,0) + NVL(d.DTPRINTR,0) + NVL(d.DTPRDSGR,0)
--           + NVL(d.DTPRVLSI,0) + NVL(d.DTPRMRAA,0) + NVL(d.DTPRINVN,0)
--           - ( NVL(d.DTPRCPPG,0) + NVL(d.DTPRINPG,0) + NVL(d.DTPRDSPG,0)
--             + NVL(d.DTPRMRPG,0) + NVL(d.DTPRINVP,0) ), 2))
-- WHERE   d.PRSTCDGO IN (SELECT p.PRSTCDGO
--                        FROM   CRD.PRST p
--                        JOIN   CRD.TMP_SEG_INCENDIO v ON v.IDAS = p.PRSTIDAS)
-- AND     d.DTPRFCVN >= DATE '2026-09-01'
-- AND     d.DTPRFCVN <  DATE '2026-12-01'
-- AND     NVL(d.DTPRESTD, 0) NOT IN (4, 7);
--
-- COMMIT;
--
-- NOTA: el pagado de seguro incendio no tiene columna propia en DTPR (el motor
-- lo acumula aparte), por eso no aparece restandose arriba.


-- =============================================================================
-- PASO 6 - LIMPIEZA
-- =============================================================================
-- Solo la tabla de trabajo. El RESPALDO se conserva hasta validar el cambio.
-- =============================================================================
-- DROP TABLE CRD.TMP_SEG_INCENDIO PURGE;


-- =============================================================================
-- REVERSO (si hay que deshacer el cambio)
-- =============================================================================
-- Restituye las tres columnas desde el respaldo en TODAS las cuotas de los 131
-- prestamos, esten o no en el alcance: las de fuera se reescriben con su propio
-- valor original, asi que es inocuo.
--
-- UPDATE  CRD.DTPR d
-- SET     (d.DTPRVLSI, d.DTPRTTLL, d.DTPRTTCS) =
--         (SELECT b.DTPRVLSI, b.DTPRTTLL, b.DTPRTTCS
--          FROM   CRD.BKP_DTPR_SEG_INC_20260826 b
--          WHERE  b.DTPRCDGO = d.DTPRCDGO)
-- WHERE   EXISTS (SELECT 1
--                 FROM   CRD.BKP_DTPR_SEG_INC_20260826 b
--                 WHERE  b.DTPRCDGO = d.DTPRCDGO);
--
-- COMMIT;
--
-- (Si se ejecuto el PASO 5, agregar d.DTPRSLDO = b.DTPRSLDO a la lista.)


-- =============================================================================
-- VARIANTES
-- =============================================================================
--
-- (A) INCLUIR EL INTERES VENCIDO EN EL TOTAL
--     Aplicar si el CONTROL 1.6 devolvio filas con INTERES_VENCIDO > 0 y se
--     quiere que DTPRTTLL quede alineado con lo que cobra el motor de pagos.
--     En el MERGE del PASO 3, agregar  + NVL(d.DTPRINVN, 0)  a las DOS
--     expresiones (DTPRTTLL y DTPRTTCS), y lo mismo en el CONTROL 4.2.
--
-- (B) EXCLUIR SOLO LAS PAGADAS, DEJANDO ENTRAR LAS CANCELADAS ANTICIPADAS
--     Lectura literal de "que no esten pagadas". Cambiar en TODAS las consultas
--     y en el MERGE:
--         NVL(d.DTPRESTD, 0) NOT IN (4, 7)
--     por:
--         NVL(d.DTPRESTD, 0) <> 4
--     Mirar antes el CONTROL 1.5: si no hay cuotas en estado 7 dentro de la
--     ventana, las dos versiones dan exactamente lo mismo.
--
-- (C) SIN PERMISO DE CREATE TABLE
--     Reemplazar CRD.TMP_SEG_INCENDIO por una subconsulta inline en cada
--     statement:
--         ( SELECT 66200 AS IDAS, 8.95 AS VLSI FROM DUAL
--           UNION ALL SELECT 65734, 12.61 FROM DUAL
--           ... ) v
--     Es la misma logica pero hay que repetir las 131 filas en cada consulta,
--     con el riesgo de transcripcion que eso implica. Preferir la tabla.
--
-- (D) OTRA VENTANA DE MESES
--     Cambiar las dos fechas en TODAS las consultas y en el MERGE:
--         >= DATE '<primer dia del primer mes>'
--         <  DATE '<primer dia del mes SIGUIENTE al ultimo>'
--     Hoy: sep-nov 2026 -> DATE '2026-09-01' y DATE '2026-12-01'.
--
-- (E) SI EL VALOR ES LA PRIMA TOTAL Y NO EL VALOR POR CUOTA
--     Este script NO sirve tal cual: pone el valor completo en CADA una de las
--     tres cuotas. Habria que repartir la prima y ademas escribir
--     PRST.PRSTPRIN. Avisar antes de ejecutar nada.
-- =============================================================================
