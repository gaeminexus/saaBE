-- =====================================================================================
-- MEDICION DE CUOTAS PARA LA APP MOVIL - desglose (desgravamen, seguro, total) y estado
-- =====================================================================================
-- Equipo: app movil ASOPREP (omen-app-1). Escrito por el arbitro el 2026-09-14.
-- Orden de trabajo: saaAPP/docs/ORDEN-CUOTAS-DESGLOSE-Y-ESTADO-2026-09-14.md
--
-- SOLO CONSULTA. No hay un solo INSERT/UPDATE/DELETE en este archivo.
--
-- POR QUE. La tabla de amortizacion de la app pasa a mostrar desgravamen, seguro de
-- incendio y "total" (CRD.DTPR.DTPRTTLL), y a traducir el estado (DTPRESTD) a texto.
-- Leyendo el codigo se sabe como se ESCRIBEN esas columnas; lo que no se sabe es que hay
-- en la base, sobre todo en cuotas de prestamos migrados. Tres preguntas:
--
--   A. Que valores de DTPRESTD existen de verdad? La app los traduce con el catalogo
--      com.saa.rubros.EstadoCuotaPrestamo (1..8). Un valor fuera de ese rango se veria
--      como "Estado N" en el telefono.
--   B. Cuantas cuotas tienen DTPRTTLL NULO? En esas la columna "Total" sale vacia ("-").
--      El motor de pago (MotorPagoPrestamoServiceImpl.calcularSaldosCuota) tiene un
--      fallback para ese caso; si el volumen es alto, hay que decidir si la app lo replica.
--   C. DTPRTTLL es de verdad cuota + desgravamen + seguro + mora? El comentario del motor
--      dice que el proceso diario de mora escribe DTPRTTLL junto con DTPRMRAA. Si no cuadra,
--      la tabla de la app mostraria columnas que no suman al total.
--
-- Columnas (CRD.DTPR): DTPRCTAA cuota (capital+interes), DTPRDSGR desgravamen,
-- DTPRVLSI seguro de incendio, DTPRMRAA mora, DTPRTTLL total, DTPRESTD estado vigente,
-- DTPRIDST copia de estado (puede estar desfasada, ver CLAUDE.md), DTPRFCPG fecha pagado.
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- A. Distribucion de estados de cuota, y cuantas de cada una traen fecha de pago
-- -------------------------------------------------------------------------------------
-- La app hoy decide "Pagada" por DTPRFCPG no nulo. La columna con_fecha_pago dice si ese
-- criterio y el estado estan de acuerdo (esperable: 4 y 7 con fecha, 1/2/3/5/8 sin fecha).
SELECT d.DTPRESTD                                          AS estado,
       CASE d.DTPRESTD
            WHEN 1 THEN 'PENDIENTE'
            WHEN 2 THEN 'ACTIVA'
            WHEN 3 THEN 'EMITIDA'
            WHEN 4 THEN 'PAGADA'
            WHEN 5 THEN 'EN_MORA'
            WHEN 6 THEN 'PARCIAL'
            WHEN 7 THEN 'CANCELADA_ANTICIPADA'
            WHEN 8 THEN 'VENCIDA'
            ELSE '*** FUERA DEL CATALOGO ***'
       END                                                 AS nombre,
       COUNT(*)                                            AS cuotas,
       SUM(CASE WHEN d.DTPRFCPG IS NOT NULL THEN 1 ELSE 0 END) AS con_fecha_pago,
       SUM(CASE WHEN NVL(d.DTPRIDST, -1) <> NVL(d.DTPRESTD, -1) THEN 1 ELSE 0 END) AS idst_desfasado
  FROM CRD.DTPR d
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD NULLS FIRST;


-- -------------------------------------------------------------------------------------
-- B. Cuotas sin DTPRTTLL, por estado
-- -------------------------------------------------------------------------------------
SELECT d.DTPRESTD                                            AS estado,
       COUNT(*)                                              AS cuotas,
       SUM(CASE WHEN d.DTPRTTLL IS NULL THEN 1 ELSE 0 END)   AS total_nulo,
       SUM(CASE WHEN d.DTPRDSGR IS NULL THEN 1 ELSE 0 END)   AS desgravamen_nulo,
       SUM(CASE WHEN d.DTPRVLSI IS NULL THEN 1 ELSE 0 END)   AS seguro_nulo
  FROM CRD.DTPR d
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD NULLS FIRST;


-- -------------------------------------------------------------------------------------
-- C. Cuadra DTPRTTLL con la suma de sus componentes?
-- -------------------------------------------------------------------------------------
-- Dos hipotesis a la vez: con mora y sin mora. Tolerancia de un centavo por redondeo.
-- Solo cuotas con DTPRTTLL no nulo (las nulas ya las conto el bloque B).
SELECT d.DTPRESTD AS estado,
       COUNT(*)   AS cuotas_con_total,
       SUM(CASE WHEN ABS(d.DTPRTTLL - (NVL(d.DTPRCTAA,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)
                                       + NVL(d.DTPRMRAA,0))) <= 0.01
                THEN 1 ELSE 0 END) AS cuadra_con_mora,
       SUM(CASE WHEN ABS(d.DTPRTTLL - (NVL(d.DTPRCTAA,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)))
                     <= 0.01
                THEN 1 ELSE 0 END) AS cuadra_sin_mora
  FROM CRD.DTPR d
 WHERE d.DTPRTTLL IS NOT NULL
 GROUP BY d.DTPRESTD
 ORDER BY d.DTPRESTD NULLS FIRST;


-- -------------------------------------------------------------------------------------
-- C2. Muestra de las que NO cuadran ni con mora ni sin mora (maximo 30)
-- -------------------------------------------------------------------------------------
-- Si el bloque C deja un resto grande, esta muestra dice que forma tiene la diferencia.
SELECT *
  FROM (SELECT d.PRSTCDGO, d.DTPRCDGO, d.DTPRNMCT, d.DTPRESTD,
               d.DTPRCTAA, d.DTPRDSGR, d.DTPRVLSI, d.DTPRMRAA, d.DTPRTTLL,
               ROUND(d.DTPRTTLL - (NVL(d.DTPRCTAA,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)
                                   + NVL(d.DTPRMRAA,0)), 2) AS diferencia_con_mora
          FROM CRD.DTPR d
         WHERE d.DTPRTTLL IS NOT NULL
           AND ABS(d.DTPRTTLL - (NVL(d.DTPRCTAA,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)
                                 + NVL(d.DTPRMRAA,0))) > 0.01
           AND ABS(d.DTPRTTLL - (NVL(d.DTPRCTAA,0) + NVL(d.DTPRDSGR,0) + NVL(d.DTPRVLSI,0)))
               > 0.01
         ORDER BY d.PRSTCDGO, d.DTPRNMCT)
 WHERE ROWNUM <= 30;


-- -------------------------------------------------------------------------------------
-- D. El prestamo de la prueba del telefono (4988, participe 1710000264)
-- -------------------------------------------------------------------------------------
-- Es el que el usuario ya vio en la app: sirve para contrastar la tabla nueva contra la base
-- fila por fila, sin buscar otro caso.
SELECT d.DTPRNMCT AS numero, d.DTPRFCVN AS vencimiento, d.DTPRCTAA AS cuota,
       d.DTPRDSGR AS desgravamen, d.DTPRVLSI AS seguro_incendio, d.DTPRMRAA AS mora,
       d.DTPRTTLL AS total, d.DTPRSLDO AS saldo, d.DTPRESTD AS estado, d.DTPRFCPG AS fecha_pagado
  FROM CRD.DTPR d
 WHERE d.PRSTCDGO = 4988
 ORDER BY d.DTPRNMCT;
