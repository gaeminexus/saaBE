-- =====================================================================================
-- lap1-17 — NIVELACION DE LAS CUENTAS DE BANDA DE CARTERA — asiento manual de cuadre
-- Equipo lap-saa-1 · 2026-10-13 · Documento: NIVELACION-BANDAS-CARTERA-OCT-2026.md
-- =====================================================================================
-- QUE HACE: genera UN asiento de DIARIO (plantilla 1097, como los asientos manuales de
-- contabilidad) que lleva cada cuenta de banda de cartera (CRD.BNDP, 51 cuentas) de su
-- saldo actual al saldo que corresponde segun la clasificacion por dias del cierre de
-- cartera, con la cartera viva del dia en que se corre. El neto entre familias va contra
-- la cuenta de cuadre (ver PARAMETROS).
--
-- CUANDO SE CORRE: un dia de OCTUBRE 2026, al FINAL del dia, despues de registrar todo lo
-- del dia y antes de registrar nada del dia siguiente; y ANTES de ejecutar el cierre de
-- cartera de octubre. CONTROL 0 lo verifica y no deja seguir si no se cumple.
--
-- COMO SE CORRE: cliente JDBC (VS Code), sentencia por sentencia, en este orden:
--   CONTROL 0 → CONTROL 1 → CONTROL 2 → PASO 1 → PASO 2 → CONTROL 3.
-- Si un CONTROL no da lo que dice su comentario, PARAR. El bloque de REVERSO va comentado.
--
-- PARAMETROS (editar antes de correr; son literales, no hay variables):
--   USUARIO        : 'JCEVALLOS'  (ASNTUSRO del asiento)
--   CUENTA_CUADRE  : '1.9.90.10'  CARTERA PENDIENTE DE REGURALIZAR (saldo 0 hoy; la que
--                    contabilidad uso para las regularizaciones de cartera de mayo y junio).
--                    Si contabilidad decide otra, reemplazar las DOS apariciones en PASO 2.
--   MARCA          : 'NIVELACION BANDAS CARTERA 2026-10' (inicio de ASNTOBSR; identifica el
--                    asiento en CONTROL 0, PASO 2, CONTROL 3 y REVERSO — no cambiar).
--
-- REGLA DE LA FOTO (la misma que dejaron los asientos del cierre de septiembre, corrida
-- CRD.CRCT 4 — ver el documento §2):
--   · cuota con vencimiento ANTERIOR al 30-09-2026 → VENCIDO, dias = 30-09 − vencimiento
--   · cuota con vencimiento 30-09-2026 o posterior → POR VENCER, dias = vencimiento − 31-10,
--     minimo 1 (las cuotas del 30-09 al 31-10 quedan en banda 1, igual que las dejo el ②)
--   · capital pendiente por cuota = GREATEST(DTPRCPTL − pagos vigentes en CRD.PGPR, 0)
--   · universo: PRSTIDST IN (2,8,11), DTPRESTD NOT IN (4,7), vencimiento y producto no nulos
--   · banda: configuracion vigente CRD.CBPR/BNDP (periodos de 30 dias, ultima abierta)
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- CONTROL 0 — guardas. Debe devolver EXACTAMENTE UNA fila con PUEDE_CORRER = 'SI'.
-- Si devuelve 'NO' o ninguna fila, no seguir: alguna condicion no se cumple (la columna
-- MOTIVO dice cual).
-- -------------------------------------------------------------------------------------
SELECT CASE WHEN cierre_sep = 1 AND cierre_oct = 0 AND ya_existe = 0 AND en_octubre = 1 THEN 'SI' ELSE 'NO' END puede_correr,
       cierre_sep, cierre_oct, ya_existe, en_octubre,
       CASE WHEN cierre_sep <> 1 THEN 'no hay corrida EJECUTADA del cierre 2026-09; '
            ELSE '' END ||
       CASE WHEN cierre_oct <> 0 THEN 'ya existe corrida del cierre 2026-10: esta nivelacion es para ANTES de ese cierre; '
            ELSE '' END ||
       CASE WHEN ya_existe <> 0 THEN 'ya existe un asiento con la MARCA (ver REVERSO); '
            ELSE '' END ||
       CASE WHEN en_octubre <> 1 THEN 'hoy no es un dia de octubre 2026; ' ELSE '' END motivo
FROM (
  SELECT (SELECT COUNT(*) FROM CRD.CRCT WHERE PJRQCDGO = 1236 AND CRCTANOO = 2026 AND CRCTMESS = 9  AND CRCTIDST = 2) cierre_sep,
         (SELECT COUNT(*) FROM CRD.CRCT WHERE PJRQCDGO = 1236 AND CRCTANOO = 2026 AND CRCTMESS = 10) cierre_oct,
         (SELECT COUNT(*) FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%') ya_existe,
         CASE WHEN TRUNC(SYSDATE) BETWEEN DATE '2026-10-01' AND DATE '2026-10-31' THEN 1 ELSE 0 END en_octubre
  FROM dual
);


-- -------------------------------------------------------------------------------------
-- CONTROL 1 — las lineas que va a tener el asiento (foto vs mayor, por cuenta de banda).
-- Revisar con contabilidad ANTES del PASO 1. La ultima fila (CUENTA = 'NETO') es lo que
-- va a ir contra la CUENTA_CUADRE: positivo = DEBE a la cuenta de cuadre.
-- Es la MISMA consulta que usa el PASO 2; lo que se ve aca es lo que se inserta.
-- -------------------------------------------------------------------------------------
WITH cuotas AS (
  SELECT p.PRDCCDGO prd, TRUNC(d.DTPRFCVN) fv, GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) cap
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN (SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) cap FROM CRD.PGPR
             WHERE PGPRANUL IS NULL OR PGPRANUL = 0 GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTIDST IN (2, 8, 11)
  AND    (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
), clas AS (
  SELECT prd, cap,
         CASE WHEN fv < DATE '2026-09-30' THEN 2 ELSE 1 END tpcr,
         CASE WHEN fv < DATE '2026-09-30' THEN GREATEST(1, DATE '2026-09-30' - fv)
              ELSE GREATEST(1, fv - DATE '2026-10-31') END dias
  FROM   cuotas WHERE cap > 0
), bandas AS (
  SELECT c.PRDCCDGO prd, c.CBPRTPCR tpcr, b.BNDPNMRO nro, b.PLNNCDGO,
         NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) * 30 + 1 d_ini,
         CASE WHEN b.BNDPCNTD IS NULL THEN NULL
              ELSE SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) * 30 END d_fin
  FROM   CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
  WHERE  c.CBPRESTD = 1 AND b.BNDPESTD = 1 AND c.PJRQCDGO = 1236
), foto AS (
  SELECT bd.PLNNCDGO, SUM(cl.cap) capital
  FROM   clas cl JOIN bandas bd ON bd.prd = cl.prd AND bd.tpcr = cl.tpcr
                 AND cl.dias >= bd.d_ini AND (bd.d_fin IS NULL OR cl.dias <= bd.d_fin)
  GROUP BY bd.PLNNCDGO
), cuentas AS (
  SELECT DISTINCT PLNNCDGO, tpcr FROM bandas
), mayor AS (
  SELECT d.PLNNCDGO, SUM(NVL(d.DTASDBEE,0) - NVL(d.DTASHBRR,0)) saldo
  FROM   CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  WHERE  a.ASNTESTD = 1 AND a.PJRQCDGO = 1236 AND a.ASNTFCHA <= TRUNC(SYSDATE)
  AND    d.PLNNCDGO IN (SELECT PLNNCDGO FROM cuentas)
  GROUP BY d.PLNNCDGO
), lineas AS (
  SELECT c.tpcr, p.PLNNCNTA cuenta, p.PLNNNMBR nombre,
         NVL(f.capital,0) foto, NVL(m.saldo,0) mayor,
         ROUND(NVL(f.capital,0) - NVL(m.saldo,0), 2) ajuste
  FROM   cuentas c JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN foto  f ON f.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN mayor m ON m.PLNNCDGO = c.PLNNCDGO
)
SELECT tpcr, cuenta, nombre, foto, mayor, ajuste,
       CASE WHEN ajuste > 0 THEN ajuste ELSE 0 END debe,
       CASE WHEN ajuste < 0 THEN -ajuste ELSE 0 END haber
FROM   lineas WHERE ABS(ajuste) >= 0.005
UNION ALL
SELECT 9, 'NETO', 'contra CUENTA_CUADRE', SUM(foto), SUM(mayor), ROUND(SUM(mayor) - SUM(foto), 2),
       CASE WHEN SUM(mayor) - SUM(foto) > 0 THEN ROUND(SUM(mayor) - SUM(foto), 2) ELSE 0 END,
       CASE WHEN SUM(mayor) - SUM(foto) < 0 THEN ROUND(SUM(foto) - SUM(mayor), 2) ELSE 0 END
FROM   lineas
ORDER BY 1, 2;


-- -------------------------------------------------------------------------------------
-- CONTROL 2 — numeracion que va a recibir el asiento (misma regla que los DIARIO de
-- contabilidad: ASNTNMRO correlativo de la plantilla 1097 en el anio, ASNTNMMS correlativo
-- del mes, ASNTNMAL = DIA-2026-10-NNNN). Solo informativo.
-- -------------------------------------------------------------------------------------
SELECT (SELECT NVL(MAX(ASNTNMRO),0) + 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = 1097 AND ASNTANOO = 2026) asntnmro,
       (SELECT NVL(MAX(ASNTNMMS),0) + 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = 1097 AND ASNTANOO = 2026 AND ASNTPRDO = 10) asntnmms,
       (SELECT PRDOCDGO FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2026 AND PRDOMSSS = 10) prdocdgo,
       TRUNC(SYSDATE) fecha_asiento
FROM dual;


-- -------------------------------------------------------------------------------------
-- PASO 1 — cabecera del asiento. 1 fila. Campos copiados de un DIARIO manual real (11126).
-- -------------------------------------------------------------------------------------
INSERT INTO CNT.ASNT (ASNTCDGO, PJRQCDGO, PLNTCDGO, ASNTFCHA, ASNTNMRO, ASNTESTD, ASNTOBSR, ASNTUSRO,
                      ASNTASRV, ASNTPRDO, ASNTANOO, ASNTMNDA, MYRZCDGO, ASNTRYYA, ASNTRZZA, ASNTFCPR,
                      PRDOCDGO, ASNTRYYB, ASNTRZZB, ASNTNMAL, ASNTNMMS)
SELECT CNT.SQ_ASNTCDGO.NEXTVAL, 1236, 1097, TRUNC(SYSDATE),
       (SELECT NVL(MAX(ASNTNMRO),0) + 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = 1097 AND ASNTANOO = 2026),
       1,
       'NIVELACION BANDAS CARTERA 2026-10 - ASIENTO MANUAL DE CUADRE DE BANDAS: lleva cada cuenta de banda al saldo de la clasificacion por dias del cierre de cartera (vencido al 30-09-2026, por vencer al 31-10-2026) con la cartera viva al ' || TO_CHAR(TRUNC(SYSDATE),'DD-MM-YYYY') || '. Ver docs/logica-negocio/reportes/NIVELACION-BANDAS-CARTERA-OCT-2026.md',
       'JCEVALLOS',
       NULL, 10, 2026, 1, NULL, 15, 1, SYSDATE,
       (SELECT PRDOCDGO FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2026 AND PRDOMSSS = 10),
       15, 1,
       'DIA-2026-10-' || LPAD((SELECT NVL(MAX(ASNTNMMS),0) + 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = 1097 AND ASNTANOO = 2026 AND ASNTPRDO = 10), 4, '0'),
       (SELECT NVL(MAX(ASNTNMMS),0) + 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND PLNTCDGO = 1097 AND ASNTANOO = 2026 AND ASNTPRDO = 10)
FROM   dual
WHERE  NOT EXISTS (SELECT 1 FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%')
AND    TRUNC(SYSDATE) BETWEEN DATE '2026-10-01' AND DATE '2026-10-31'
AND    NOT EXISTS (SELECT 1 FROM CRD.CRCT WHERE PJRQCDGO = 1236 AND CRCTANOO = 2026 AND CRCTMESS = 10);


-- -------------------------------------------------------------------------------------
-- PASO 2 — lineas del asiento: una por cuenta de banda con ajuste, mas la linea de cuadre.
-- Misma consulta que CONTROL 1. La cabecera se ubica por la MARCA.
-- Si la CUENTA_CUADRE no existe en CNT.PLNN, la sentencia REVIENTA a proposito
-- (division por cero) y no inserta NADA — no puede quedar un asiento descuadrado.
-- El mayor se mide con fecha <= hoy y la cabecera recien creada todavia no tiene lineas,
-- asi que no se cuenta a si misma.
-- -------------------------------------------------------------------------------------
INSERT INTO CNT.DTAS (DTASCDGO, ASNTCDGO, PLNNCDGO, DTASDSCR, DTASDBEE, DTASHBRR, DTASNMCT, CNCSCDGO, DTASCNTA)
WITH cab AS (
  SELECT MAX(ASNTCDGO) asntcdgo FROM CNT.ASNT
  WHERE  PJRQCDGO = 1236 AND ASNTESTD = 1 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%'
), cuotas AS (
  SELECT p.PRDCCDGO prd, TRUNC(d.DTPRFCVN) fv, GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) cap
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN (SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) cap FROM CRD.PGPR
             WHERE PGPRANUL IS NULL OR PGPRANUL = 0 GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTIDST IN (2, 8, 11)
  AND    (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
), clas AS (
  SELECT prd, cap,
         CASE WHEN fv < DATE '2026-09-30' THEN 2 ELSE 1 END tpcr,
         CASE WHEN fv < DATE '2026-09-30' THEN GREATEST(1, DATE '2026-09-30' - fv)
              ELSE GREATEST(1, fv - DATE '2026-10-31') END dias
  FROM   cuotas WHERE cap > 0
), bandas AS (
  SELECT c.PRDCCDGO prd, c.CBPRTPCR tpcr, b.BNDPNMRO nro, b.PLNNCDGO,
         NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) * 30 + 1 d_ini,
         CASE WHEN b.BNDPCNTD IS NULL THEN NULL
              ELSE SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) * 30 END d_fin
  FROM   CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
  WHERE  c.CBPRESTD = 1 AND b.BNDPESTD = 1 AND c.PJRQCDGO = 1236
), foto AS (
  SELECT bd.PLNNCDGO, SUM(cl.cap) capital
  FROM   clas cl JOIN bandas bd ON bd.prd = cl.prd AND bd.tpcr = cl.tpcr
                 AND cl.dias >= bd.d_ini AND (bd.d_fin IS NULL OR cl.dias <= bd.d_fin)
  GROUP BY bd.PLNNCDGO
), cuentas AS (
  SELECT DISTINCT PLNNCDGO, tpcr FROM bandas
), mayor AS (
  SELECT d.PLNNCDGO, SUM(NVL(d.DTASDBEE,0) - NVL(d.DTASHBRR,0)) saldo
  FROM   CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  WHERE  a.ASNTESTD = 1 AND a.PJRQCDGO = 1236 AND a.ASNTFCHA <= TRUNC(SYSDATE)
  AND    d.PLNNCDGO IN (SELECT PLNNCDGO FROM cuentas)
  GROUP BY d.PLNNCDGO
), lineas AS (
  SELECT c.PLNNCDGO, p.PLNNCNTA cuenta, p.PLNNNMBR nombre,
         ROUND(NVL(f.capital,0) - NVL(m.saldo,0), 2) ajuste
  FROM   cuentas c JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN foto  f ON f.PLNNCDGO = c.PLNNCDGO
  LEFT JOIN mayor m ON m.PLNNCDGO = c.PLNNCDGO
), cuadre AS (
  SELECT p.PLNNCDGO, p.PLNNCNTA cuenta, p.PLNNNMBR nombre,
         ROUND(-(SELECT SUM(ajuste) FROM lineas), 2) ajuste
  FROM   CNT.PLNN p WHERE p.PJRQCDGO = 1236 AND p.PLNNCNTA = '1.9.90.10'   -- CUENTA_CUADRE
), todas AS (
  SELECT PLNNCDGO, cuenta, nombre, ajuste, 'Nivelacion bandas 2026-10 - ' || nombre dscr FROM lineas WHERE ABS(ajuste) >= 0.005
  UNION ALL
  SELECT PLNNCDGO, cuenta, nombre, ajuste, 'Nivelacion bandas 2026-10 - neto entre familias de cartera' FROM cuadre WHERE ABS(ajuste) >= 0.005
)
SELECT CNT.SQ_DTASCDGO.NEXTVAL, cab.asntcdgo, t.PLNNCDGO, t.dscr,
       CASE WHEN t.ajuste > 0 THEN t.ajuste ELSE 0 END,
       CASE WHEN t.ajuste < 0 THEN -t.ajuste ELSE 0 END,
       t.nombre, NULL, t.cuenta
FROM   todas t, cab
WHERE  cab.asntcdgo IS NOT NULL
-- guarda: si la CUENTA_CUADRE no existe en el plan de cuentas, 1/0 revienta y no inserta nada
AND    1 / (SELECT COUNT(*) FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND PLNNCNTA = '1.9.90.10') = 1;   -- CUENTA_CUADRE


-- -------------------------------------------------------------------------------------
-- CONTROL 3 — despues. (a) el asiento cuadra: DEBE = HABER y DESCUADRE = 0;
-- (b) el mayor de cada cuenta de banda queda igual a la foto: la consulta debe devolver
-- CERO filas (toda diferencia >= 0,01 aparece). Si (a) o (b) fallan: REVERSO.
-- -------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMRO, a.ASNTNMAL, TO_CHAR(a.ASNTFCHA,'YYYY-MM-DD') fecha, COUNT(d.DTASCDGO) lineas,
       SUM(d.DTASDBEE) debe, SUM(d.DTASHBRR) haber, ROUND(SUM(d.DTASDBEE) - SUM(d.DTASHBRR), 2) descuadre
FROM   CNT.ASNT a LEFT JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
WHERE  a.PJRQCDGO = 1236 AND a.ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%'
GROUP BY a.ASNTCDGO, a.ASNTNMRO, a.ASNTNMAL, a.ASNTFCHA;

WITH cuotas AS (
  SELECT p.PRDCCDGO prd, TRUNC(d.DTPRFCVN) fv, GREATEST(NVL(d.DTPRCPTL,0) - NVL(g.cap,0), 0) cap
  FROM   CRD.DTPR d
  JOIN   CRD.PRST p ON p.PRSTCDGO = d.PRSTCDGO
  LEFT JOIN (SELECT DTPRCDGO, SUM(NVL(PGPRCPPG,0)) cap FROM CRD.PGPR
             WHERE PGPRANUL IS NULL OR PGPRANUL = 0 GROUP BY DTPRCDGO) g ON g.DTPRCDGO = d.DTPRCDGO
  WHERE  p.PRSTIDST IN (2, 8, 11)
  AND    (d.DTPRESTD IS NULL OR d.DTPRESTD NOT IN (4, 7))
  AND    d.DTPRFCVN IS NOT NULL
  AND    p.PRDCCDGO IS NOT NULL
), clas AS (
  SELECT prd, cap,
         CASE WHEN fv < DATE '2026-09-30' THEN 2 ELSE 1 END tpcr,
         CASE WHEN fv < DATE '2026-09-30' THEN GREATEST(1, DATE '2026-09-30' - fv)
              ELSE GREATEST(1, fv - DATE '2026-10-31') END dias
  FROM   cuotas WHERE cap > 0
), bandas AS (
  SELECT c.PRDCCDGO prd, c.CBPRTPCR tpcr, b.BNDPNMRO nro, b.PLNNCDGO,
         NVL(SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
             ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING), 0) * 30 + 1 d_ini,
         CASE WHEN b.BNDPCNTD IS NULL THEN NULL
              ELSE SUM(b.BNDPCNTD) OVER (PARTITION BY c.CBPRCDGO ORDER BY b.BNDPNMRO
                   ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) * 30 END d_fin
  FROM   CRD.CBPR c JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
  WHERE  c.CBPRESTD = 1 AND b.BNDPESTD = 1 AND c.PJRQCDGO = 1236
), foto AS (
  SELECT bd.PLNNCDGO, SUM(cl.cap) capital
  FROM   clas cl JOIN bandas bd ON bd.prd = cl.prd AND bd.tpcr = cl.tpcr
                 AND cl.dias >= bd.d_ini AND (bd.d_fin IS NULL OR cl.dias <= bd.d_fin)
  GROUP BY bd.PLNNCDGO
), cuentas AS (
  SELECT DISTINCT PLNNCDGO FROM bandas
), mayor AS (
  SELECT d.PLNNCDGO, SUM(NVL(d.DTASDBEE,0) - NVL(d.DTASHBRR,0)) saldo
  FROM   CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  WHERE  a.ASNTESTD = 1 AND a.PJRQCDGO = 1236 AND a.ASNTFCHA <= TRUNC(SYSDATE)
  AND    d.PLNNCDGO IN (SELECT PLNNCDGO FROM cuentas)
  GROUP BY d.PLNNCDGO
)
SELECT p.PLNNCNTA cuenta, NVL(f.capital,0) foto, NVL(m.saldo,0) mayor, ROUND(NVL(f.capital,0) - NVL(m.saldo,0), 2) diferencia
FROM   cuentas c JOIN CNT.PLNN p ON p.PLNNCDGO = c.PLNNCDGO
LEFT JOIN foto  f ON f.PLNNCDGO = c.PLNNCDGO
LEFT JOIN mayor m ON m.PLNNCDGO = c.PLNNCDGO
WHERE  ABS(NVL(f.capital,0) - NVL(m.saldo,0)) >= 0.01
ORDER BY 1;


-- =====================================================================================
-- REVERSO (comentado; correr SOLO si CONTROL 3 falla o si contabilidad lo pide).
-- Opcion A — anular como anula contabilidad (queda el rastro, el mayor lo excluye por
--            ASNTESTD = 2). Es la preferida.
-- UPDATE CNT.ASNT
--    SET ASNTESTD = 2, ASNTFCAN = SYSDATE, ASNTUSAN = 'JCEVALLOS',
--        ASNTMTAN = 'Anulacion de la nivelacion de bandas 2026-10'
--  WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%';
--
-- Opcion B — borrar (solo si se corrio por error y no se mayorizo ni se reporto nada):
-- DELETE FROM CNT.DTAS WHERE ASNTCDGO IN (SELECT ASNTCDGO FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%');
-- DELETE FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTOBSR LIKE 'NIVELACION BANDAS CARTERA 2026-10%';
-- =====================================================================================
