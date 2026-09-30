-- =====================================================================================
-- 248 — CONTROL INDEPENDIENTE DEL CUADRO DE PLAZO VENCIDO (solo lectura)
-- FECHA: 2026-09-30 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- PARA QUE: el usuario pidio que «el sistema realice bien los calculos». Este script
-- recalcula el cuadro de cada declaracion DIRECTO DESDE LA BASE (CRD.DTPR + CRD.PGPR), sin
-- pasar por el codigo Java, y lo compara fila por fila contra la foto que grabo el sistema
-- en CRD.PLVN. Si el codigo y este SQL coinciden, dos caminos independientes llegaron al
-- mismo numero. Si no coinciden, uno de los dos esta mal, y hay que ver cual antes de
-- mandar ese prestamo a legal.
--
-- LAS REGLAS QUE REPRODUCE (diseno §4.4bis, contrato §2, correcciones del arbitro):
--   * Universo: todas las cuotas del prestamo, EXCEPTO las CANCELADA_ANTICIPADA (7).
--   * Cobrado = pagos de CRD.PGPR no anulados (PGPRANUL nulo o 0), por componente.
--     ⛔ El capital cobrado SUMA el PAGO EXTRA (PGPRSLOT): el abono a capital se graba ahi, con
--     PGPRCPPG en 0 (AbonoCapitalPrestamoServiceImpl:232-240; migracion PrestamoServiceImpl:947).
--     Sin eso, todo prestamo con un abono falla la invariante de capital (caso 62439, 2026-09-30).
--   * Por cuota, con piso en cero: saldo_i = max(0, regla_i - pagado_i).
--     Devengado de la fila = cobrado + saldo (menos el capital: su devengado es el monto).
--   * Capital e interes: todas las cuotas (aceleracion, D11).
--   * Desgravamen e incendio: solo las cuotas con vencimiento <= fecha de corte (D22).
--
-- ⚠️ LO QUE NO COMPARA: la MORA. Su formula vive en Java
--    (ProcesoMoraPrestamoService.calcularMoraCuota) y reescribirla aca seria verificar una
--    copia, no el original. Solo se muestra el valor grabado, para leerlo a ojo.
--
-- ⚠️ CUANDO CORRERLO: justo DESPUES de declarar. Si entre la declaracion y este control
--    entra un pago al prestamo, el cobrado de hoy ya no es el del corte y va a aparecer
--    una diferencia que no es un error del calculo.
--
-- SOLO LECTURA: no escribe nada. SQL PURO, sin comandos SQL*Plus.
-- SOLO EN EL BACKEND: este archivo NO se espeja a saaFE.
-- =====================================================================================


-- =====================================================================================
-- 1. EL CONTRASTE — una fila por declaracion. Esperado: todas las columnas DIF_* en 0,00.
--    Para ver solo las que no cuadran, descomentar el WHERE del final.
-- =====================================================================================
WITH pagos AS (
    SELECT g.DTPRCDGO,
           SUM(NVL(g.PGPRCPPG, 0) + NVL(g.PGPRSLOT, 0)) AS CAP_PAG,   -- capital + PAGO EXTRA (abono): va al COBRADO
           SUM(NVL(g.PGPRCPPG, 0)) AS CAP_PAG_CUOTA,                -- solo capital de la cuota: va al SALDO
           SUM(NVL(g.PGPRINPG, 0) + NVL(g.PGPRINVP, 0)) AS INT_PAG,   -- interes + interes vencido pagado
           SUM(NVL(g.PGPRDSGR, 0)) AS DSG_PAG,
           SUM(NVL(g.PGPRVLSI, 0)) AS SEG_PAG,
           SUM(NVL(g.PGPRMRPG, 0)) AS MOR_PAG
      FROM CRD.PGPR g
     WHERE NVL(g.PGPRANUL, 0) = 0
     GROUP BY g.DTPRCDGO
),
por_cuota AS (
    SELECT v.PLVNCDGO,
           d.DTPRESTD,
           TRUNC(d.DTPRFCVN) AS VENCE,
           v.PLVNFCCR        AS CORTE,
           NVL(p.CAP_PAG, 0) AS CAP_PAG,
           NVL(p.INT_PAG, 0) AS INT_PAG,
           NVL(p.DSG_PAG, 0) AS DSG_PAG,
           NVL(p.SEG_PAG, 0) AS SEG_PAG,
           NVL(p.MOR_PAG, 0) AS MOR_PAG,
           GREATEST(0, ROUND(NVL(d.DTPRCPTL, 0) - NVL(p.CAP_PAG_CUOTA, 0), 2)) AS SAL_CAP,
           GREATEST(0, ROUND(NVL(d.DTPRINTR, 0) + NVL(d.DTPRINVN, 0) - NVL(p.INT_PAG, 0), 2)) AS SAL_INT,
           GREATEST(0, ROUND(CASE WHEN TRUNC(d.DTPRFCVN) <= v.PLVNFCCR THEN NVL(d.DTPRDSGR, 0) ELSE 0 END
                             - NVL(p.DSG_PAG, 0), 2)) AS SAL_DSG,
           GREATEST(0, ROUND(CASE WHEN TRUNC(d.DTPRFCVN) <= v.PLVNFCCR THEN NVL(d.DTPRVLSI, 0) ELSE 0 END
                             - NVL(p.SEG_PAG, 0), 2)) AS SAL_SEG
      FROM CRD.PLVN v
      JOIN CRD.DTPR d ON d.PRSTCDGO = v.PRSTCDGO
      LEFT JOIN pagos p ON p.DTPRCDGO = d.DTPRCDGO
     WHERE NVL(d.DTPRESTD, 0) <> 7          -- CANCELADA_ANTICIPADA fuera del universo
),
recalculo AS (
    SELECT c.PLVNCDGO,
           ROUND(SUM(c.CAP_PAG), 2) AS CAP_CB,  ROUND(SUM(c.SAL_CAP), 2) AS CAP_SL,
           ROUND(SUM(c.INT_PAG), 2) AS INT_CB,  ROUND(SUM(c.SAL_INT), 2) AS INT_SL,
           ROUND(SUM(c.DSG_PAG), 2) AS DSG_CB,  ROUND(SUM(c.SAL_DSG), 2) AS DSG_SL,
           ROUND(SUM(c.SEG_PAG), 2) AS SEG_CB,  ROUND(SUM(c.SAL_SEG), 2) AS SEG_SL,
           ROUND(SUM(c.MOR_PAG), 2) AS MOR_CB,
           COUNT(*)                                                        AS CUOTAS,
           SUM(CASE WHEN c.DTPRESTD = 4 THEN 1 ELSE 0 END)                 AS COBRADAS,
           SUM(CASE WHEN NVL(c.DTPRESTD, 0) <> 4 AND c.VENCE > c.CORTE THEN 1 ELSE 0 END) AS POR_VENCER
      FROM por_cuota c
     GROUP BY c.PLVNCDGO
)
SELECT v.PLVNCDGO                AS DECLARACION,
       v.PLVNNMPS                AS PRESTAMO,
       v.PLVNNMMM                AS MEMORANDO,
       v.PLVNESTD                AS ESTADO,
       v.PLVNFCCR                AS CORTE,
       -- Capital
       v.PLVNCPCB - r.CAP_CB     AS DIF_CAPITAL_COBRADO,
       v.PLVNSLCP - r.CAP_SL     AS DIF_CAPITAL_SALDO,
       -- Invariante 3: la tabla suma el monto (saldo por cuota = monto - cobrado)
       ROUND(v.PLVNMNTO - r.CAP_CB - r.CAP_SL, 2) AS DIF_TABLA_VS_MONTO,
       -- Interes
       v.PLVNINCB - r.INT_CB     AS DIF_INTERES_COBRADO,
       v.PLVNSLIN - r.INT_SL     AS DIF_INTERES_SALDO,
       -- Desgravamen
       v.PLVNDSCB - r.DSG_CB     AS DIF_DESGRAV_COBRADO,
       v.PLVNSLDS - r.DSG_SL     AS DIF_DESGRAV_SALDO,
       -- Seguro de incendio
       v.PLVNSGCB - r.SEG_CB     AS DIF_SEGURO_COBRADO,
       v.PLVNSLSG - r.SEG_SL     AS DIF_SEGURO_SALDO,
       -- Mora: SOLO el cobrado se compara; el devengado lo calcula Java
       v.PLVNMRCB - r.MOR_CB     AS DIF_MORA_COBRADA,
       v.PLVNSLMR                AS MORA_SALDO_GRABADO,
       -- Totales: el total grabado contra la suma de los saldos (con la mora grabada)
       ROUND(v.PLVNTTPC - (v.PLVNSLCP + v.PLVNSLIN + v.PLVNSLDS + v.PLVNSLSG + v.PLVNSLMR), 2) AS DIF_TOTAL_INTERNO,
       -- Cuotas
       v.PLVNCTCB - r.COBRADAS   AS DIF_CUOTAS_COBRADAS,
       v.PLVNCTPN - (r.CUOTAS - r.COBRADAS) AS DIF_CUOTAS_PENDIENTES,
       v.PLVNCTXV - r.POR_VENCER AS DIF_CUOTAS_POR_VENCER,
       v.PLVNCTPL                AS PLAZO_GRABADO,
       r.CUOTAS                  AS CUOTAS_EN_TABLA
  FROM CRD.PLVN v
  JOIN recalculo r ON r.PLVNCDGO = v.PLVNCDGO
-- WHERE ABS(v.PLVNSLCP - r.CAP_SL) > 0.01 OR ABS(v.PLVNSLIN - r.INT_SL) > 0.01
--    OR ABS(v.PLVNSLDS - r.DSG_SL) > 0.01 OR ABS(v.PLVNSLSG - r.SEG_SL) > 0.01
--    OR ABS(v.PLVNCPCB - r.CAP_CB) > 0.01
 ORDER BY v.PLVNCDGO;

-- LECTURA:
--   * DIF_* distintas de 0 (con tolerancia de un centavo por redondeo) → PARAR y pasarle
--     al arbitro la fila completa.
--   * DIF_TABLA_VS_MONTO distinta de 0 → la tabla de amortizacion no suma lo prestado. El
--     sistema no deberia haber dejado declarar ese prestamo (invariante 3). Si aparece, es
--     un defecto del codigo.
--   * PLAZO_GRABADO distinto de CUOTAS_EN_TABLA → no es error: el codigo lo informa sin
--     bloquear. Sirve para saber cuales tablas estan incompletas.


-- =====================================================================================
-- 2. EL SEGURO ANULADO — que lo que dice DPLV sea lo que quedo en las cuotas.
--    Esperado: 0 filas mientras la declaracion este viva (estado 1 o 2).
--    Cada fila es una cuota futura que deberia tener el seguro en cero (o en lo pagado) y no lo tiene.
-- =====================================================================================
SELECT v.PLVNCDGO, v.PLVNNMPS, d.DTPRCDGO, d.DTPRNMCT, d.DTPRFCVN,
       x.DPLVDSGR AS DESGRAV_ORIGINAL, d.DTPRDSGR AS DESGRAV_HOY,
       x.DPLVVLSI AS SEGURO_ORIGINAL,  d.DTPRVLSI AS SEGURO_HOY
  FROM CRD.DPLV x
  JOIN CRD.PLVN v ON v.PLVNCDGO = x.PLVNCDGO
  JOIN CRD.DTPR d ON d.DTPRCDGO = x.DTPRCDGO
  LEFT JOIN (SELECT g.DTPRCDGO, SUM(NVL(g.PGPRDSGR,0)) AS DSG_PAG, SUM(NVL(g.PGPRVLSI,0)) AS SEG_PAG
               FROM CRD.PGPR g WHERE NVL(g.PGPRANUL,0) = 0 GROUP BY g.DTPRCDGO) p
         ON p.DTPRCDGO = d.DTPRCDGO
 WHERE v.PLVNESTD IN (1, 2)
   AND x.DPLVFCRS IS NULL
   AND (   ROUND(NVL(d.DTPRDSGR,0), 2) <> ROUND(LEAST(x.DPLVDSGR, NVL(p.DSG_PAG,0)), 2)
        OR ROUND(NVL(d.DTPRVLSI,0), 2) <> ROUND(LEAST(x.DPLVVLSI, NVL(p.SEG_PAG,0)), 2))
 ORDER BY v.PLVNCDGO, d.DTPRNMCT;


-- =====================================================================================
-- 3. EL ESTADO — cada declaracion viva tiene su prestamo en 8, y cada revertida en otro.
--    Esperado: 0 filas.
-- =====================================================================================
SELECT v.PLVNCDGO, v.PLVNNMPS, v.PLVNESTD, p.PRSTIDST
  FROM CRD.PLVN v
  JOIN CRD.PRST p ON p.PRSTCDGO = v.PRSTCDGO
 WHERE (v.PLVNESTD IN (1, 2) AND p.PRSTIDST <> 8)
    OR (v.PLVNESTD = 3 AND p.PRSTIDST = 8
        AND NOT EXISTS (SELECT 1 FROM CRD.PLVN w
                         WHERE w.PRSTCDGO = v.PRSTCDGO AND w.PLVNESTD IN (1, 2)));
