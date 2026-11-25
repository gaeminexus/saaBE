-- TRASLADO-CONDONACIONES-PRODUCCION.sql — D-176 en PRODUCCIÓN (24-nov-2026).
-- Documento: 03-trabajo/cambios-saa/c45-produccion/TRASLADO-CONDONACIONES-PRODUCCION.md (leerlo antes).
--
-- Qué hace: SOLO SELECT. No cambia nada. Sirve para:
--   00  comprobar que ya se corrió def9-07 (cuenta 4.8.90.90.45 y plantilla 25 / línea 70 apuntando a ella);
--   01  ver el estado de los períodos de 2026 (el cargador C5 rechaza un período MAYORIZADO o CERRADO);
--   02  listar, por asiento, todo lo que tocó 4.1.01.30 o 4.8.02.1 en 2026 y qué hacer con cada línea;
--   03  totales por mes, cuenta y acción;
--   04  GENERAR las líneas del CSV del traslado (se copian tal cual al archivo que carga el cargador C5);
--   05  detalle del asiento huérfano (acuerdo no aplicado con su asiento de condonación vivo), que se ANULA, no se traslada;
--   06  controles después de cargar el traslado y anular el huérfano.
-- Los cambios no se hacen con este script: el traslado lo registra el cargador C5 (POST /SaaBE/rest/asnt/cargarManuales) y el
-- huérfano se anula con POST /SaaBE/rest/asnt/anular/{idAsiento}. Sin datos personales: el préstamo se muestra por su código
-- interno y por su número de operación (PRSTIDAS); la observación del asiento (que trae cédula y nombre) no se imprime.
-- SQL puro: correr bloque por bloque en el cliente JDBC de VS Code, sobre la base de PRODUCCIÓN (o sobre la base de trabajo para
-- ensayar). Empresa 1236.

-- =====================================================================================================================
-- 00 Pre-requisito D-176. ESPERADO: la primera consulta devuelve 1 fila (4.8.90.90.45, activa, de movimiento) y la segunda
--    1 fila con PLNNCNTA = 4.8.90.90.45. Si la primera da 0 filas o la segunda muestra 4.1.01.30 (o 4.8.02.1): PARAR, correr
--    completo 03-trabajo/meses/2025-09/sql/def9-07-cuenta-4.8.90.90.45-perdidas-condonaciones.sql (PASE-A-PRODUCCION §4.18),
--    confirmar con COMMIT y volver aquí. Desde ese momento las condonaciones nuevas ya caen en 4.8.90.90.45.
-- =====================================================================================================================
SELECT PLNNCDGO, PLNNCNTA, PLNNNMBR, PLNNTPOO, PLNNNVLL, PLNNESTD
  FROM CNT.PLNN WHERE PJRQCDGO = 1236 AND PLNNCNTA = '4.8.90.90.45';
SELECT D.DTPLCDGO, S.PLNSCDAL, D.DTPLAXL1, N.PLNNCNTA, N.PLNNNMBR, D.DTPLESTD
  FROM CNT.DTPL D JOIN CNT.PLNS S ON S.PLNSCDGO = D.PLNSCDGO JOIN CNT.PLNN N ON N.PLNNCDGO = D.PLNNCDGO
 WHERE S.PJRQCDGO = 1236 AND S.PLNSCDAL = 25 AND D.DTPLAXL1 = 70;

-- =====================================================================================================================
-- 01 Períodos de 2026. PRDOESTD: 1 abierto, 2 mayorizado, 3 desmayorizado (admite asientos). PRDOCNCR = 1: cierre contable C17.
--    El cargador rechaza todo el archivo si un asiento cae en un período 2 o cerrado: en ese caso ver §5 del documento.
-- =====================================================================================================================
SELECT PRDOCDGO, PRDOANNN, PRDOMSSS, PRDOESTD, PRDOCNCR
  FROM CNT.PRDO WHERE PJRQCDGO = 1236 AND PRDOANNN = 2026 ORDER BY PRDOMSSS;

-- =====================================================================================================================
-- 02 Lista por asiento: toda línea de 4.1.01.30 o 4.8.02.1 en 2026, en cualquier estado del asiento (1 activo, 2 anulado,
--    3 reversado). ORIGEN:
--      MOTOR 25/70       línea «Condonación acuerdo N - gasto por condonación» del acuerdo de condonación (/accn);
--      TRASLADO D-176    línea de un asiento de este traslado ya cargado (archivo asientos-manuales-2026-produccion-d176-…);
--      OTRO              cualquier otro movimiento (p. ej. uno real de inversiones en 4.1.01.30): NO se toca.
--    ACCION (solo para MOTOR 25/70 en asientos activos o reversados):
--      TRASLADAR         el acuerdo está APLICADO (ACCNESTD 2): la condonación es real y pasa a 4.8.90.90.45;
--      ANULAR HUERFANO   el acuerdo NO está aplicado (1 vigente o 3 anulado) y su asiento de condonación sigue vivo: el reverso del
--                        cobro del acuerdo no anula ese asiento (CobroCreditoServiceImpl.reversarProceso). Se anula, no se traslada.
--    Guarda de la base de trabajo: allí el re-proceso pasó los acuerdos 14, 18 y 19 a estado 3 con motivo «def8-03 D-138…»
--    (no es un dato de producción); se tratan como APLICADOS. En producción ningún motivo empieza por «def8-».
--    ESPERADO en la base de trabajo (24-nov-2026): 19 líneas MOTOR 25/70, ninguna OTRO en 2026; 1 huérfano (asiento 8527, acuerdo 1).
-- =====================================================================================================================
WITH L AS (
  SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTESTD, a.ASNTUSRO, a.ASNTOBSR, d.DTASCDGO, d.DTASCNTA, d.DTASDBEE, d.DTASHBRR,
         d.DTASDSCR,
         TO_NUMBER(REGEXP_SUBSTR(d.DTASDSCR, '^Condonación acuerdo ([0-9]+) - gasto por condonación$', 1, 1, NULL, 1)) ACUERDO
    FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
   WHERE a.PJRQCDGO = 1236 AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1')
     AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01')
SELECT L.ASNTCDGO, L.ASNTNMAL, TO_CHAR(L.ASNTFCHA, 'YYYY-MM-DD') FECHA, L.ASNTESTD, L.ASNTUSRO, L.DTASCNTA CUENTA,
       L.DTASDBEE DEBE, L.DTASHBRR HABER, L.ACUERDO, c.PRSTCDGO PRESTAMO, p.PRSTIDAS OPERACION, c.ACCNESTD ESTADO_ACUERDO,
       CASE WHEN c.ACCNMTRC LIKE 'def8-%' THEN 'RE-PROCESO (solo base de trabajo)' WHEN c.ACCNESTD = 3 THEN 'ANULADO EN PRODUCCION' END MARCA_ESTADO,
       (SELECT LISTAGG(x.ACCNCDGO, ',') WITHIN GROUP (ORDER BY x.ACCNCDGO) FROM CRD.ACCN x
         WHERE x.PRSTCDGO = c.PRSTCDGO AND x.ACCNCDGO <> c.ACCNCDGO AND x.ACCNESTD = 2) OTRO_ACUERDO_APLICADO,
       CASE WHEN L.ACUERDO IS NOT NULL THEN 'MOTOR 25/70'
            WHEN L.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%' THEN 'TRASLADO D-176'
            ELSE 'OTRO' END ORIGEN,
       CASE WHEN L.ACUERDO IS NULL OR L.ASNTESTD NOT IN (1, 3) THEN 'NO TOCAR'
            WHEN c.ACCNESTD = 2 OR c.ACCNMTRC LIKE 'def8-%' THEN 'TRASLADAR'
            ELSE 'ANULAR HUERFANO' END ACCION,
       CASE WHEN L.ACUERDO IS NULL THEN SUBSTR(L.DTASDSCR, 1, 80) END GLOSA_SI_OTRO
  FROM L LEFT JOIN CRD.ACCN c ON c.ACCNCDGO = L.ACUERDO
         LEFT JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
 ORDER BY L.ASNTFCHA, L.ASNTCDGO, L.DTASCDGO;

-- =====================================================================================================================
-- 03 Totales por mes, cuenta y acción (asientos activos o reversados). ESPERADO en la base de trabajo (24-nov-2026):
--      2026-08 4.1.01.30 TRASLADAR        2 líneas  11.977,85
--      2026-09 4.8.02.1  ANULAR HUERFANO  1 línea   11.841,62  (asiento 8527)
--      2026-09 4.8.02.1  TRASLADAR        3 líneas     910,37
--      2026-10 4.1.01.30 TRASLADAR       11 líneas  28.495,65
--      2026-11 4.1.01.30 TRASLADAR        2 líneas     969,44
--    Total a trasladar 42.353,31 (41.442,94 de 4.1.01.30 + 910,37 de 4.8.02.1).
--    En producción pueden aparecer líneas posteriores al 19-nov-2026 (último asiento de usuarios de ASOPREP en la base de trabajo)
--    y anteriores a def9-07: el CSV del bloque 04 ya las incluye.
-- =====================================================================================================================
WITH L AS (
  SELECT a.ASNTFCHA, a.ASNTESTD, a.ASNTOBSR, d.DTASCNTA, d.DTASDBEE, d.DTASHBRR,
         TO_NUMBER(REGEXP_SUBSTR(d.DTASDSCR, '^Condonación acuerdo ([0-9]+) - gasto por condonación$', 1, 1, NULL, 1)) ACUERDO
    FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
   WHERE a.PJRQCDGO = 1236 AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1') AND a.ASNTESTD IN (1, 3)
     AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01')
SELECT TO_CHAR(L.ASNTFCHA, 'YYYY-MM') MES, L.DTASCNTA CUENTA,
       CASE WHEN L.ACUERDO IS NOT NULL AND (c.ACCNESTD = 2 OR c.ACCNMTRC LIKE 'def8-%') THEN 'TRASLADAR'
            WHEN L.ACUERDO IS NOT NULL THEN 'ANULAR HUERFANO'
            WHEN L.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%' THEN 'TRASLADO D-176 YA CARGADO'
            ELSE 'OTRO (NO TOCAR)' END ACCION,
       COUNT(*) LINEAS, SUM(L.DTASDBEE) DEBE, SUM(L.DTASHBRR) HABER
  FROM L LEFT JOIN CRD.ACCN c ON c.ACCNCDGO = L.ACUERDO
 GROUP BY TO_CHAR(L.ASNTFCHA, 'YYYY-MM'), L.DTASCNTA,
       CASE WHEN L.ACUERDO IS NOT NULL AND (c.ACCNESTD = 2 OR c.ACCNMTRC LIKE 'def8-%') THEN 'TRASLADAR'
            WHEN L.ACUERDO IS NOT NULL THEN 'ANULAR HUERFANO'
            WHEN L.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%' THEN 'TRASLADO D-176 YA CARGADO'
            ELSE 'OTRO (NO TOCAR)' END
 ORDER BY 1, 2, 3;

-- =====================================================================================================================
-- 04 Generador del CSV del traslado (formato de 03-trabajo/plantillas/PLANTILLA-ASIENTOS-MANUALES.csv, variante cuenta_saa;
--    cuenta_b17). Copiar la columna LINEA, en el orden que sale, a
--    asientos-manuales-2026-produccion-d176-traslado-condonaciones.csv (UTF-8, separador «;»). Un asiento por mes:
--    D 4.8.90.90.45 por el total del mes / H 4.1.01.30 y H 4.8.02.1, una línea por asiento de condonación de origen.
--    Fecha: último día del mes si ya terminó; en el mes en curso, la fecha de la última condonación trasladada (nunca una fecha
--    futura; si después hiciera falta un segundo traslado del mismo mes, cambiar -001 por -002 y el nombre del archivo).
--    Solo entra lo marcado TRASLADAR (el huérfano
--    no) y lo que todavía no tiene traslado cargado. Si se corre después de cargar, debe devolver solo la cabecera.
-- =====================================================================================================================
WITH L AS (
  SELECT a.ASNTCDGO, a.ASNTFCHA, d.DTASCDGO, d.DTASCNTA, d.DTASDBEE - d.DTASHBRR VALOR,
         TO_NUMBER(REGEXP_SUBSTR(d.DTASDSCR, '^Condonación acuerdo ([0-9]+) - gasto por condonación$', 1, 1, NULL, 1)) ACUERDO
    FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
   WHERE a.PJRQCDGO = 1236 AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1') AND a.ASNTESTD IN (1, 3)
     AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01'),
T AS (
  SELECT L.*, p.PRSTIDAS, TRUNC(L.ASNTFCHA, 'MM') MES
    FROM L JOIN CRD.ACCN c ON c.ACCNCDGO = L.ACUERDO
           LEFT JOIN CRD.PRST p ON p.PRSTCDGO = c.PRSTCDGO
   WHERE (c.ACCNESTD = 2 OR c.ACCNMTRC LIKE 'def8-%')
     AND NOT EXISTS (SELECT 1 FROM CNT.DTAS t JOIN CNT.ASNT ta ON ta.ASNTCDGO = t.ASNTCDGO
                      WHERE ta.ASNTESTD IN (1, 3)
                        AND ta.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%'
                        AND t.DTASDSCR LIKE 'D-176 asiento ' || L.ASNTCDGO || ' %')),
G AS (
  SELECT 'D176-' || TO_CHAR(MES, 'YYYY-MM') || '-001' NUMERO,
         TO_CHAR(CASE WHEN LAST_DAY(MES) < TRUNC(SYSDATE) THEN LAST_DAY(MES) ELSE MAX(TRUNC(ASNTFCHA)) END, 'YYYY-MM-DD') FECHA,
         'D-176 en produccion: traslado de las condonaciones del motor de ' || TO_CHAR(MES, 'YYYY-MM')
           || ' (plantilla 25, linea 70) de 4.1.01.30 y 4.8.02.1 a la cuenta 4.8.90.90.45 Perdidas y condonaciones' GLOSA,
         MES
    FROM T GROUP BY MES)
SELECT LINEA FROM (
  SELECT 0 ORD_MES, 0 ORD_LINEA, 0 ORD_ASNT,
         'fecha;numero_asiento;glosa;cuenta_saa;cuenta_b17;nombre_cuenta;debe;haber;documento_respaldo;centro_costo' LINEA
    FROM DUAL
  UNION ALL
  SELECT TO_NUMBER(TO_CHAR(G.MES, 'YYYYMM')), 1, 0,
         G.FECHA || ';' || G.NUMERO || ';' || G.GLOSA || ';4.8.90.90.45;489090;PERDIDAS Y CONDONACIONES;'
           || TO_CHAR(SUM(T.VALOR), 'FM9999999990.00') || ';0.00;D-176 asientos '
           || LISTAGG(T.ASNTCDGO, ', ') WITHIN GROUP (ORDER BY T.ASNTCDGO) || ';'
    FROM G JOIN T ON T.MES = G.MES
   GROUP BY G.MES, G.FECHA, G.NUMERO, G.GLOSA
  UNION ALL
  SELECT TO_NUMBER(TO_CHAR(G.MES, 'YYYYMM')), 2, T.ASNTCDGO,
         G.FECHA || ';' || G.NUMERO || ';' || G.GLOSA || ';' || T.DTASCNTA || ';'
           || CASE T.DTASCNTA WHEN '4.1.01.30' THEN '410130;INVERSIONES RENTA VARIABLE SECTOR NO FINANCIERO PRIVADO'
                              ELSE '4802;PERDIDA Y CONDONACION DE PRESTAMOS' END
           || ';0.00;' || TO_CHAR(T.VALOR, 'FM9999999990.00') || ';D-176 asiento ' || T.ASNTCDGO || ' acuerdo ' || T.ACUERDO
           || ' operacion ' || T.PRSTIDAS || ';'
    FROM G JOIN T ON T.MES = G.MES)
 ORDER BY ORD_MES, ORD_LINEA, ORD_ASNT;

-- =====================================================================================================================
-- 05 Huérfanos: el asiento de condonación completo (todas sus líneas) de un acuerdo que no está aplicado, y el asiento del
--    acuerdo aplicado del mismo préstamo que lo reemplazó. ESPERADO en la base de trabajo: asiento 8527 (acuerdo 1, préstamo 6477,
--    operación 70097; D 4.8.02.1 11.841,62 / H 1.3.12.20 569,42, 1.3.12.15 776,55, 1.4.02.15 10.495,65), reemplazado por el
--    asiento 9245 (acuerdo 7, mismas cuentas de haber; D 4.1.01.30 11.832,56). Este es el que se anula (§4 del documento).
-- =====================================================================================================================
WITH H AS (
  SELECT DISTINCT a.ASNTCDGO, TO_NUMBER(REGEXP_SUBSTR(d.DTASDSCR, '^Condonación acuerdo ([0-9]+) - gasto por condonación$', 1, 1, NULL, 1)) ACUERDO
    FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
   WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1')
     AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01'
     AND REGEXP_LIKE(d.DTASDSCR, '^Condonación acuerdo [0-9]+ - gasto por condonación$')),
HU AS (
  SELECT H.ASNTCDGO, H.ACUERDO, c.PRSTCDGO, c.ACCNESTD FROM H JOIN CRD.ACCN c ON c.ACCNCDGO = H.ACUERDO
   WHERE c.ACCNESTD <> 2 AND NVL(c.ACCNMTRC, '-') NOT LIKE 'def8-%'),
R AS (
  SELECT HU.ASNTCDGO HUERFANO, x.ACCNCDGO ACUERDO_REEMPLAZO, d.ASNTCDGO ASIENTO_REEMPLAZO
    FROM HU JOIN CRD.ACCN x ON x.PRSTCDGO = HU.PRSTCDGO AND x.ACCNESTD = 2 AND x.ACCNCDGO <> HU.ACUERDO
            JOIN CNT.DTAS d ON d.DTASDSCR = 'Condonación acuerdo ' || x.ACCNCDGO || ' - gasto por condonación'
            JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO AND a.ASNTESTD IN (1, 3))
SELECT 'HUERFANO' PAPEL, HU.ASNTCDGO, a.ASNTNMAL, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') FECHA, a.ASNTESTD, HU.ACUERDO,
       HU.PRSTCDGO PRESTAMO, p.PRSTIDAS OPERACION, d.DTASCNTA, d.DTASDBEE, d.DTASHBRR
  FROM HU JOIN CNT.ASNT a ON a.ASNTCDGO = HU.ASNTCDGO JOIN CNT.DTAS d ON d.ASNTCDGO = HU.ASNTCDGO
          LEFT JOIN CRD.PRST p ON p.PRSTCDGO = HU.PRSTCDGO
UNION ALL
SELECT 'REEMPLAZO de ' || R.HUERFANO, R.ASIENTO_REEMPLAZO, a.ASNTNMAL, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD'), a.ASNTESTD,
       R.ACUERDO_REEMPLAZO, NULL, NULL, d.DTASCNTA, d.DTASDBEE, d.DTASHBRR
  FROM R JOIN CNT.ASNT a ON a.ASNTCDGO = R.ASIENTO_REEMPLAZO JOIN CNT.DTAS d ON d.ASNTCDGO = R.ASIENTO_REEMPLAZO
 ORDER BY 1, 2, 9;

-- =====================================================================================================================
-- 06 Controles DESPUÉS de cargar el CSV y de anular el huérfano.
-- 06.1 Saldo 2026 por cuenta y origen (asientos activos o reversados). ESPERADO: 4.1.01.30 MOTOR + TRASLADO = 0 (queda solo lo
--      OTRO, que en la base de trabajo no existe); 4.8.02.1 = 0 en total; 4.8.90.90.45 = lo trasladado + las condonaciones que el
--      motor registró ahí después de def9-07. Si 4.8.02.1 no da 0, falta anular el huérfano (bloque 05).
-- =====================================================================================================================
SELECT d.DTASCNTA CUENTA,
       CASE WHEN REGEXP_LIKE(d.DTASDSCR, '^Condonación acuerdo [0-9]+ - gasto por condonación$') THEN 'MOTOR 25/70'
            WHEN a.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%' THEN 'TRASLADO D-176'
            ELSE 'OTRO' END ORIGEN,
       COUNT(*) LINEAS, SUM(d.DTASDBEE) DEBE, SUM(d.DTASHBRR) HABER, SUM(d.DTASDBEE - d.DTASHBRR) SALDO_DEUDOR
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1', '4.8.90.90.45')
   AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01'
 GROUP BY ROLLUP (d.DTASCNTA,
       CASE WHEN REGEXP_LIKE(d.DTASDSCR, '^Condonación acuerdo [0-9]+ - gasto por condonación$') THEN 'MOTOR 25/70'
            WHEN a.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%' THEN 'TRASLADO D-176'
            ELSE 'OTRO' END)
 ORDER BY 1, 2;

-- 06.2 Asientos del traslado: uno por mes, activos y cuadrados. ESPERADO: DEBE = HABER en cada uno; en la base de trabajo serían
--      4 (ago 11.977,85; sep 910,37; oct 28.495,65; nov 969,44).
SELECT a.ASNTCDGO, a.ASNTNMAL, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') FECHA, a.ASNTESTD,
       REGEXP_SUBSTR(a.ASNTOBSR, '#(D176-[0-9-]+)', 1, 1, NULL, 1) NUMERO, SUM(d.DTASDBEE) DEBE, SUM(d.DTASHBRR) HABER
  FROM CNT.ASNT a JOIN CNT.DTAS d ON d.ASNTCDGO = a.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%'
 GROUP BY a.ASNTCDGO, a.ASNTNMAL, a.ASNTFCHA, a.ASNTESTD, a.ASNTOBSR ORDER BY a.ASNTFCHA;

-- 06.3 Condonaciones del motor por trasladar que quedaron sin traslado. ESPERADO: 0 filas.
SELECT a.ASNTCDGO, TO_CHAR(a.ASNTFCHA, 'YYYY-MM-DD') FECHA, d.DTASCNTA, d.DTASDBEE
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
  JOIN CRD.ACCN c ON c.ACCNCDGO = TO_NUMBER(REGEXP_SUBSTR(d.DTASDSCR, '^Condonación acuerdo ([0-9]+) - gasto por condonación$', 1, 1, NULL, 1))
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3) AND d.DTASCNTA IN ('4.1.01.30', '4.8.02.1')
   AND a.ASNTFCHA >= DATE '2026-01-01' AND a.ASNTFCHA < DATE '2027-01-01'
   AND (c.ACCNESTD = 2 OR c.ACCNMTRC LIKE 'def8-%')
   AND NOT EXISTS (SELECT 1 FROM CNT.DTAS t JOIN CNT.ASNT ta ON ta.ASNTCDGO = t.ASNTCDGO
                    WHERE ta.ASNTESTD IN (1, 3) AND ta.ASNTOBSR LIKE 'CARGA MANUAL [asientos-manuales-2026-produccion-d176-%'
                      AND t.DTASDSCR LIKE 'D-176 asiento ' || a.ASNTCDGO || ' %');

-- 06.4 El huérfano quedó anulado (ASNTESTD 2) o reversado (3, con su reverso fechado en el primer período abierto). ESPERADO:
--      el asiento 8527 en 2 o 3, y si es 3, un asiento de reverso activo con las mismas cuentas al revés.
SELECT ASNTCDGO, ASNTNMAL, TO_CHAR(ASNTFCHA, 'YYYY-MM-DD') FECHA, ASNTESTD, ASNTUSAN, TO_CHAR(ASNTFCAN, 'YYYY-MM-DD HH24:MI') FCAN
  FROM CNT.ASNT WHERE PJRQCDGO = 1236 AND ASNTCDGO = 8527;

-- 06.5 La línea 70 de la plantilla 25 sigue en 4.8.90.90.45 y desde def9-07 no entró ninguna condonación nueva a 4.1.01.30 ni a
--      4.8.02.1 (ESPERADO: la última fecha de 06.1 en esas cuentas es anterior a la corrida de def9-07).
SELECT d.DTASCNTA, TO_CHAR(MAX(a.ASNTFCPR), 'YYYY-MM-DD HH24:MI') ULTIMO_REGISTRO
  FROM CNT.DTAS d JOIN CNT.ASNT a ON a.ASNTCDGO = d.ASNTCDGO
 WHERE a.PJRQCDGO = 1236 AND a.ASNTESTD IN (1, 3)
   AND REGEXP_LIKE(d.DTASDSCR, '^Condonación acuerdo [0-9]+ - gasto por condonación$')
 GROUP BY d.DTASCNTA ORDER BY 1;
