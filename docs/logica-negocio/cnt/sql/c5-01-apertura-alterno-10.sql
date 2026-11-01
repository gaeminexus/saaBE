-- C5 (cuadre Superintendencia): tipo de asiento APERTURA de la empresa 1236 en el codigo alterno 10.
-- Decision del coordinador (2026-10-28): los alternos 7, 8 y 9 quedan para IN-A / EG-A / DI-A (PLNTSSTM = 1); la base
-- del proyecto habia creado APERTURA con el 8 (script 05 de cuadreSuper) y hay que moverlo. TipoAsientos.APERTURA = 10.
-- Idempotente para los dos casos: APERTURA no existe -> no hace nada (en la base nueva se crea directamente con 10);
-- APERTURA existe con 8 -> lo mueve al 10 (solo si el 10 esta libre); ya esta en 10 -> no hace nada.
-- SQL puro, sin COMMIT: lo da quien lo corre despues de revisar el control posterior.

-- 1 Control previo: APERTURA y quien tiene el 8 y el 10 en 1236
SELECT PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PLNTSSTM
FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND (PLNTNMBR = 'APERTURA' OR PLNTCDAL IN (8, 10)) ORDER BY PLNTCDAL;

-- 2 Mover APERTURA del 8 al 10 (0 filas si no existe, si ya esta en 10 o si el 10 esta ocupado por otro tipo)
UPDATE CNT.PLNT SET PLNTCDAL = 10
WHERE PJRQCDGO = 1236 AND PLNTNMBR = 'APERTURA' AND PLNTCDAL = 8
  AND NOT EXISTS (SELECT 1 FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTCDAL = 10);

-- 3 Control posterior: se espera APERTURA con 10 (o ninguna fila APERTURA, si no existe) y nada de APERTURA en el 8
SELECT PLNTCDGO, PLNTNMBR, PLNTCDAL, PLNTESTD, PLNTSSTM
FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND (PLNTNMBR = 'APERTURA' OR PLNTCDAL IN (8, 10)) ORDER BY PLNTCDAL;
-- Si el 10 estaba ocupado por otro tipo, el UPDATE no hizo nada: aparece aqui y hay que decidir otro alterno.
SELECT COUNT(*) apertura_todavia_en_8 FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTNMBR = 'APERTURA' AND PLNTCDAL = 8;
-- Los asientos ya registrados no cambian: CNT.ASNT referencia PLNTCDGO, no el alterno.
SELECT COUNT(*) asientos_apertura FROM CNT.ASNT WHERE PJRQCDGO = 1236
  AND PLNTCDGO IN (SELECT PLNTCDGO FROM CNT.PLNT WHERE PJRQCDGO = 1236 AND PLNTNMBR = 'APERTURA');
