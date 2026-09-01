-- =====================================================================================
-- VERIFICACION — ¿las cuatro plantillas apuntan a la MISMA cuenta "por aplicar"?
-- FECHA: 2026-08-31 · Equipo A de crd
--
-- ⛔ SOLO LECTURA. Ni un UPDATE. Este script diagnostica, no corrige.
--
-- POR QUE IMPORTA, Y POR QUE AHORA. El asiento de reparto del cobro (el 2do de los tres)
-- se desplego hoy y **ya esta generando asientos en produccion** con el flag de
-- contabilidad encendido. Resuelve sus cuentas por la plantilla 20 de forma POSICIONAL
-- (aux1 1/2/3), mientras que apertura (1), aplicacion de Petro (21) y neteo (33) las
-- resuelven por el catalogo SEMANTICO CrdLineaAsiento:
--
--     APORTES_POR_COBRAR   = 1        APORTES_POR_APLICAR   = 3
--     PRESTAMOS_POR_COBRAR = 2        PRESTAMOS_POR_APLICAR = 4
--
-- Las cuatro deberian terminar en la MISMA cuenta contable (1.4.05.05 aportes /
-- 1.4.05.10 prestamos). **Si la 20 no esta configurada igual que las otras, el asiento 2
-- descarga la transitoria contra una cuenta distinta de la que el resto usa — y los dos
-- asientos CUADRAN igual.** No hay error, no hay log: solo dos saldos que no se cruzan.
--
-- Esto NO se puede verificar desde el codigo: la configuracion vive en la base.
-- =====================================================================================


-- 1. Las cuatro plantillas existen. Esperado: 4 filas (alternos 1, 20, 21, 33).
--    Si falta alguna, el proceso que la usa falla al resolverla.
SELECT p.PLNSCDGO, p.PLNSCDAL, p.PLNSNMBR, p.PLNSESTD
FROM   CNT.PLNS p
WHERE  p.PLNSCDAL IN (1, 20, 21, 33)
ORDER  BY p.PLNSCDAL;


-- 2. LA CONSULTA QUE IMPORTA — todas las lineas de esas cuatro, con su cuenta real.
--    Comparar: la linea de aux1=3 (APORTES_POR_APLICAR) de la plantilla 1, de la 21 y de
--    la 33 deberia dar la MISMA cuenta. Y la que el reparto (20) usa posicionalmente
--    deberia dar esa misma cuenta tambien.
SELECT p.PLNSCDAL              AS PLANTILLA_ALTERNO,
       p.PLNSNMBR              AS PLANTILLA,
       d.DTPLAXL1              AS AUX1,
       d.DTPLMVMN              AS MOVIMIENTO,
       n.PLNNCNTA              AS CUENTA,
       n.PLNNNMBR              AS NOMBRE_CUENTA
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
LEFT   JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL IN (1, 20, 21, 33)
ORDER  BY p.PLNSCDAL, d.DTPLAXL1;


-- 3. El resumen: por cada aux1, cuantas cuentas DISTINTAS hay entre las plantillas.
--    Esperado para aux1 = 3 y 4: **UNA sola cuenta** (CUANTAS_CUENTAS = 1).
--    Si da 2 o mas, ahi esta el problema y la columna CUENTAS dice cuales son.
SELECT d.DTPLAXL1                                   AS AUX1,
       COUNT(DISTINCT n.PLNNCNTA)                   AS CUANTAS_CUENTAS,
       LISTAGG(DISTINCT p.PLNSCDAL || '->' || n.PLNNCNTA, ' | ')
           WITHIN GROUP (ORDER BY p.PLNSCDAL)       AS CUENTAS
FROM   CNT.PLNS p
JOIN   CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
JOIN   CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  p.PLNSCDAL IN (1, 20, 21, 33)
GROUP  BY d.DTPLAXL1
ORDER  BY d.DTPLAXL1;


-- 4. CONTROL VIVO — la cuenta transitoria tiene que cerrar en cero.
--    Es la prueba de que el asiento 2 esta descargando lo que el asiento 1 cargo.
--    Si da distinto de cero y no hay cobros REGISTRADOS pendientes de procesar, hay un
--    cobro cuyo reparto se genero por otro monto o contra otra cuenta.
SELECT SUM(d.DTASVLDB) - SUM(d.DTASVLHB) AS SALDO_TRANSITORIA,
       COUNT(*)                          AS CUANTAS_LINEAS
FROM   CNT.DTAS d
JOIN   CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
WHERE  n.PLNNCNTA = '2.3.01.15.01';


-- 5. Los asientos de reparto generados hasta ahora, para revisarlos de a uno si el
--    control 4 no da cero.
SELECT c.CBCRCDGO, c.CBCRASN1, c.CBCRASRP, c.CBCRASN2, c.CBCRVLOR, c.CBCRESTD
FROM   CRD.CBCR c
WHERE  c.CBCRASRP IS NOT NULL
ORDER  BY c.CBCRCDGO DESC;
