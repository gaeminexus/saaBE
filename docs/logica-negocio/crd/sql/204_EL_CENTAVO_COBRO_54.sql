-- =====================================================================================
-- EL CENTAVO DEL COBRO 54 — por que el asiento definitivo no cuadra
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 204 (rango 200-249)
--
-- NO ESCRIBE NADA. Los cuatro bloques son SELECT.
--
-- SINTOMA
--   POST /cbcr/54/procesar -> 500
--   "El asiento NNNN no esta cuadrado. TOTAL DEBE=171,86 | TOTAL HABER=171,85 | DIF=0,01"
--
-- LO QUE YA ESTA MEDIDO CONTRA EL CODIGO (no hace falta re-deducirlo)
--   1. El motor aplico 171,85 y dejo 0,01 como "excedente no aplicado", con 43 cuotas
--      pendientes por delante. NO es que no hubiera donde ponerlo:
--      MotorPagoPrestamoServiceImpl:286  ->  while (valorRestante > TOLERANCIA)
--      MotorPagoPrestamoServiceImpl:54   ->  TOLERANCIA = 0.01
--      Con valorRestante = 0.01 la condicion es 0.01 > 0.01 = FALSE. El bucle sale y el
--      centavo queda huerfano. Con >= habria entrado a la cuota siguiente.
--   2. El asiento definitivo arma sus dos lados de DOS FUENTES DISTINTAS:
--      DEBE   <- CobroCreditoServiceImpl:1799  totalesAportesPrestamos(detalles)  = DCBC (171,86)
--      HABER  <- las lineas armadas desde los PGPR realmente grabados               = 171,85
--      Cuando el motor deja cualquier excedente, el asiento descuadra por ese monto exacto.
--   3. AsientoContableServiceImpl:529 lo detecta y lanza IncomeException, que es
--      @ApplicationException(rollback = true) -> la transaccion entera se revierte.
--      El BLOQUE 2 confirma empiricamente que asi fue (medir, no confiar).
--
-- LO QUE ESTE SCRIPT RESUELVE
--   BLOQUE 1: si la cuota debe 171,85 y el cobro dice 171,86 -> el centavo sobra en el cobro.
--   BLOQUE 2: si los reintentos dejaron pagos o asientos huerfanos.
--   BLOQUE 3: cuantos cobros mas estan en la misma trampa (uno o cien cambia el plan).
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los tres bloques.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 240
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — DE DONDE SALE EL CENTAVO
-- Esperado: DCBCVLRR = 171,86 y DTPRTTLL de la cuota abierta = 171,85.
-- Si DTPRTTLL es 171,86 la hipotesis esta mal y hay que parar: el defecto es otro.
-- =====================================================================================

SELECT 'COBRO'          AS ORIGEN,
       c.CBCRCDGO       AS COBRO,
       c.CBCRVLRR       AS VALOR_CABECERA,
       c.CBCRESTD       AS ESTADO,
       d.DCBCCDGO       AS DETALLE,
       d.PRSTCDGO       AS PRESTAMO,
       d.DCBCVLRR       AS VALOR_DETALLE
  FROM CRD.CBCR c
  JOIN CRD.DCBC d ON d.CBCRCDGO = c.CBCRCDGO
 WHERE c.CBCRCDGO = 54;

-- Las cuotas abiertas del prestamo (sin ningun PGPR vigente), las 3 primeras.
-- SUMA_6 vs DTPRTTLL: si difieren, ademas hay un desfase interno de la cuota.
SELECT t.DTPRCDGO                                     AS CUOTA,
       t.DTPRNMCT                                     AS NUM,
       t.DTPRESTD                                     AS ESTADO,
       t.DTPRCPTL                                     AS CAPITAL,
       t.DTPRINTR                                     AS INTERES,
       t.DTPRMRAA                                     AS MORA,
       t.DTPRINVN                                     AS INT_VENCIDO,
       t.DTPRDSGR                                     AS DESGRAVAMEN,
       t.DTPRVLSI                                     AS SEG_INCENDIO,
       t.DTPRTTLL                                     AS TOTAL_DTPRTTLL,
       ROUND(NVL(t.DTPRCPTL,0) + NVL(t.DTPRINTR,0) + NVL(t.DTPRMRAA,0)
           + NVL(t.DTPRINVN,0) + NVL(t.DTPRDSGR,0) + NVL(t.DTPRVLSI,0), 2) AS SUMA_6,
       ROUND(NVL(t.DTPRTTLL,0) - (NVL(t.DTPRCPTL,0) + NVL(t.DTPRINTR,0) + NVL(t.DTPRMRAA,0)
           + NVL(t.DTPRINVN,0) + NVL(t.DTPRDSGR,0) + NVL(t.DTPRVLSI,0)), 2) AS DIF_TOTAL_VS_SUMA
  FROM (SELECT p.*
          FROM CRD.DTPR p
         WHERE p.PRSTCDGO = 4524
           AND NOT EXISTS (SELECT 1 FROM CRD.PGPR g
                            WHERE g.DTPRCDGO = p.DTPRCDGO
                              AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0))
         ORDER BY p.DTPRNMCT) t
 WHERE ROWNUM <= 3;


-- =====================================================================================
-- BLOQUE 2 — LOS REINTENTOS: ¿dejaron basura?
-- Esperado (si el rollback funciono, que es lo que dice el codigo):
--   2.1 -> 0 filas    2.2 -> las tres columnas de asiento en NULL    2.3 -> 0 filas
-- Cualquier fila aca cambia TODO el plan: seria contabilidad y pagos a medio grabar.
-- =====================================================================================

-- 2.1 Pagos grabados hoy sobre el prestamo 4524
SELECT g.PGPRCDGO AS PAGO, g.DTPRCDGO AS CUOTA, g.PGPRVLRR AS VALOR,
       g.PGPRFCRG AS FECHA_REGISTRO, g.PGPRANUL AS ANULADO, g.EVPRCDGO AS EVENTO
  FROM CRD.PGPR g
 WHERE g.PRSTCDGO = 4524
   AND g.PGPRFCRG >= TRUNC(SYSDATE)
 ORDER BY g.PGPRCDGO;

-- 2.2 El cobro 54 no debe tener ningun asiento enganchado todavia
SELECT c.CBCRCDGO AS COBRO, c.CBCRASN1 AS ASIENTO_1, c.CBCRASRP AS ASIENTO_REPARTO,
       c.CBCRASN2 AS ASIENTO_DEFINITIVO, c.CBCRESTD AS ESTADO
  FROM CRD.CBCR c
 WHERE c.CBCRCDGO = 54;

-- 2.3 Asientos huerfanos en el rango que consumieron los reintentos
SELECT a.ASNTCDGO AS ASIENTO, a.ASNTNMAL AS NUMERO_ALTERNO, a.ASNTFCHA AS FECHA,
       (SELECT COUNT(*) FROM CNT.DTAS x WHERE x.ASNTCDGO = a.ASNTCDGO) AS LINEAS
  FROM CNT.ASNT a
 WHERE a.ASNTCDGO BETWEEN 9215 AND 9230
 ORDER BY a.ASNTCDGO;


-- =====================================================================================
-- BLOQUE 3 — RADIO DE IMPACTO
-- Todo cobro SIN asiento definitivo (CBCRASN2 IS NULL) cuyo detalle no coincide con la
-- primera cuota abierta de su prestamo. DIF entre 0 y 0,01 = misma trampa que el 54.
-- Si esto devuelve una sola fila, es un caso puntual. Si devuelve decenas, no se arregla
-- cobro por cobro.
-- =====================================================================================

SELECT c.CBCRCDGO                    AS COBRO,
       c.CBCRESTD                    AS ESTADO,
       d.PRSTCDGO                    AS PRESTAMO,
       d.DCBCVLRR                    AS VALOR_COBRADO,
       q.DTPRCDGO                    AS PRIMERA_CUOTA_ABIERTA,
       q.DTPRTTLL                    AS DEBE_LA_CUOTA,
       ROUND(d.DCBCVLRR - q.DTPRTTLL, 2) AS DIF
  FROM CRD.CBCR c
  JOIN CRD.DCBC d ON d.CBCRCDGO = c.CBCRCDGO
  JOIN (SELECT p.PRSTCDGO, p.DTPRCDGO, p.DTPRTTLL,
               ROW_NUMBER() OVER (PARTITION BY p.PRSTCDGO ORDER BY p.DTPRNMCT) AS RN
          FROM CRD.DTPR p
         WHERE NOT EXISTS (SELECT 1 FROM CRD.PGPR g
                            WHERE g.DTPRCDGO = p.DTPRCDGO
                              AND (g.PGPRANUL IS NULL OR g.PGPRANUL = 0))) q
    ON q.PRSTCDGO = d.PRSTCDGO AND q.RN = 1
 WHERE c.CBCRASN2 IS NULL
   AND d.PRSTCDGO IS NOT NULL
   AND ABS(d.DCBCVLRR - q.DTPRTTLL) > 0.001
 ORDER BY ABS(d.DCBCVLRR - q.DTPRTTLL) DESC, c.CBCRCDGO;
