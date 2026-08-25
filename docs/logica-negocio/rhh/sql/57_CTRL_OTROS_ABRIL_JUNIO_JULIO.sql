-- =====================================================
-- MODULO: RHH - LOS OTROS DEL ROL, COMO FILA DE CONCEPTO EN CTRL
-- DESCRIPCION: Completa la transcripcion del rol del cliente en los tres
--              meses que llevan importes en su columna OTROS.
-- ORDEN DE EJECUCION: 57   (despues del 56, antes de reabrir abril)
-- FECHA: 2026-08-23
-- PARAMETRO: ninguno
-- =====================================================
-- ESTO NO AJUSTA EL VALOR DEL CLIENTE. LEER ANTES DE EJECUTAR.
-- .
-- La regla 3 dice que un valor esperado del cliente NO se ajusta nunca para
-- que cuadre con el nuestro, y esto no lo hace. El rol del cliente SI trae
-- estos importes: estan dentro de sus totales de DESCUENTOS, que CTRL ya
-- carga. Lo que faltaba era la fila POR CONCEPTO, y faltaba por un motivo
-- mecanico: cuando se transcribio el rol no existia concepto al que mapear
-- la columna OTROS. El concepto 31 lo creo el sql/56.
-- .
-- La comprobacion de que se completa y no se inventa, en abril:
--   66,15 (aporte) + 14,13 (quirografario) + 350,00 (anticipo) = 430,28
--   DESCUENTOS que CTRL ya declara para Calderon               = 605,28
--   605,28 - 430,28                                            = 175,00
-- El importe ya estaba en CTRL. Solo no estaba desglosado.
-- .
-- POR QUE HAY QUE HACERLO. Sin la fila de concepto, el bloque 2 cuadraria
-- --los totales coinciden-- pero el bloque 1 sacaria una fila nueva: el
-- concepto 31 nuestro, 175,00, NO ESTA EN EL ROL. Seria un descuadre
-- fabricado por una transcripcion incompleta, no por un calculo.
-- .
-- LOS TRES MESES, y los importes salen de las cabeceras del 40, 50 y 51:
--   Abril  Calderon Parraga                                    175,00
--   Junio  Calderon Parraga                                      0,10
--   Julio  Barcenas 1,95 · Munoz 1,53 · Nieto 2,50 · Pardo 1,95
--          · Viteri 36,67 (fondo de reserva de junio, recuperado) 44,60
-- .
-- Junio y julio NO se han corrido todavia: aqui solo se deja el dato listo
-- para cuando se corran.
-- =====================================================


-- ---------------------------------------------------.
-- CONTROL 1: el concepto 31 tiene que existir (sql/56 corrido).
-- ---------------------------------------------------.
SELECT CPNMALTR, CPNMNMBR, CPNMTPCN AS TIPO, CPNMRCRT AS RECORTABLE,
       CASE WHEN CPNMALTR = 31 THEN 'OK' ELSE '*** FALTA EL SQL/56 ***' END AS VEREDICTO
  FROM RHH.CPNM WHERE PJRQCDGO = 1236 AND CPNMALTR = 31;


-- ---------------------------------------------------.
-- CONTROL 2: no debe existir ya ninguna fila 31 en CTRL. CERO filas.
-- Repetible: si sale algo, este script ya corrio y no se reejecuta.
-- ---------------------------------------------------.
SELECT CTRLANOO, CTRLMESS, CTRLIDNT, CTRLVLOR
  FROM RHH.CTRL WHERE CTRLALTR = 31 ORDER BY CTRLMESS, CTRLIDNT;


INSERT INTO RHH.CTRL (CTRLANOO, CTRLMESS, CTRLIDNT, CTRLALTR, CTRLVLOR, CTRLFNTE, CTRLUSRR)
SELECT 2026, d.MES, d.CED, 31, d.VLOR, 'ROL', 'CARGA'
  FROM (
    SELECT 4 MES, '1719624809' CED, 175.00 VLOR FROM DUAL UNION ALL  -- abril  Calderon
    SELECT 6,     '1719624809',       0.10 FROM DUAL UNION ALL       -- junio  Calderon
    SELECT 7,     '1717991341',       1.95 FROM DUAL UNION ALL       -- julio  Barcenas
    SELECT 7,     '1717649873',       1.53 FROM DUAL UNION ALL       -- julio  Munoz
    SELECT 7,     '1723962849',       2.50 FROM DUAL UNION ALL       -- julio  Nieto
    SELECT 7,     '1726657164',       1.95 FROM DUAL UNION ALL       -- julio  Pardo
    SELECT 7,     '1712232659',      36.67 FROM DUAL                 -- julio  Viteri, FR de junio
  ) d
 WHERE NOT EXISTS (SELECT 1 FROM RHH.CTRL WHERE CTRLALTR = 31);

COMMIT;


-- ---------------------------------------------------.
-- CONTROL DESPUES: siete filas. Abril 175,00 · junio 0,10 · julio 44,60.
-- ---------------------------------------------------.
SELECT CTRLMESS AS MES, COUNT(*) AS FILAS, SUM(CTRLVLOR) AS TOTAL_OTROS
  FROM RHH.CTRL WHERE CTRLALTR = 31
 GROUP BY CTRLMESS ORDER BY CTRLMESS;


-- ---------------------------------------------------.
-- CONTROL DESPUES 2: EL QUE DE VERDAD IMPORTA.
-- Los conceptos de cada persona tienen que sumar su total de DESCUENTOS.
-- Antes de este script, Calderon en abril fallaba por 175,00 exactos.
-- Tiene que devolver CERO filas para abril, junio y julio.
-- ---------------------------------------------------.
SELECT c.CTRLANOO AS ANIO, c.CTRLMESS AS MES, c.CTRLIDNT,
       SUM(c.CTRLVLOR) AS SUMA_CONCEPTOS, t.CTRLVLOR AS TOTAL_DECLARADO,
       t.CTRLVLOR - SUM(c.CTRLVLOR) AS DIFERENCIA
  FROM RHH.CTRL c
  JOIN RHH.CTRL t ON t.CTRLANOO = c.CTRLANOO AND t.CTRLMESS = c.CTRLMESS
                 AND t.CTRLIDNT = c.CTRLIDNT AND t.CTRLTOTL = 'DESCUENTOS'
 WHERE c.CTRLALTR IN (20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31)
   AND c.CTRLMESS IN (4, 6, 7)
 GROUP BY c.CTRLANOO, c.CTRLMESS, c.CTRLIDNT, t.CTRLVLOR
HAVING ABS(t.CTRLVLOR - SUM(c.CTRLVLOR)) > 0.005
 ORDER BY 2, 3;
