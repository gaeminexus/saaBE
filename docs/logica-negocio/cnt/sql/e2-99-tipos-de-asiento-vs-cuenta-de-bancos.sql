-- =====================================================================================
-- e2-99 — P5: tipo de asiento contra la cuenta de BANCOS. SOLO LECTURA.
-- Equipo omen-saa-2 · 2026-10-12.
--
-- Regla del usuario (2026-10-12), que reemplaza la lectura literal del correo de Ana del 06-10:
--   · Desde octubre, todo asiento GENERADO POR EL SISTEMA que afecte una cuenta de bancos sale como
--     INGRESO de tesorería (banco en el DEBE) o EGRESO de tesorería (banco en el HABER).
--   · El resto sale con el tipo de asiento de su módulo.
--   · Ningún asiento generado por el sistema sale como DIARIO.
--
-- Este script mide cuánto se aparta hoy la realidad de esa regla, para dimensionar el cambio. No corrige nada.
-- Columnas copiadas de TipoAsiento (CNT.PLNT), Asiento (CNT.ASNT), DetalleAsiento (CNT.DTAS),
-- PlanCuenta (CNT.PLNN) y CuentaBancaria (TSR.CNBC). ASNTRZZB = rubroModuloSistemaH (módulo que lo generó).
-- «Cuenta de bancos» se mide de dos formas, para ver si coinciden: la cuenta contable de una CuentaBancaria
-- (TSR.CNBC.PLNNCDGO) y el prefijo 1.1.02 del plan.
-- =====================================================================================

-- BLOQUE 1 — Catálogo de tipos de asiento. ESPERADO: ver qué fila hay detrás de cada código alterno
-- (PLNTCDAL), cuál es la de diario (nombre que empieza con DIA) y cuáles son de Ingreso/Egreso de tesorería.
SELECT 'BLOQUE 1 - tipos de asiento' AS bloque, t.PJRQCDGO, t.PLNTCDGO, t.PLNTCDAL, t.PLNTNMBR,
       t.PLNTESTD, t.PLNTSSTM
  FROM CNT.PLNT t
 ORDER BY t.PJRQCDGO, t.PLNTCDAL, t.PLNTCDGO;

-- BLOQUE 2 — ¿Las dos definiciones de «bancos» coinciden? ESPERADO: 0 filas. Cada fila es una cuenta
-- que una definición llama banco y la otra no.
SELECT 'BLOQUE 2 - definiciones de bancos' AS bloque, p.PLNNCDGO, p.PLNNCNTA, p.PLNNNMBR,
       CASE WHEN EXISTS (SELECT 1 FROM TSR.CNBC c WHERE c.PLNNCDGO = p.PLNNCDGO) THEN 'SI' ELSE 'NO' END AS es_cuenta_bancaria,
       CASE WHEN p.PLNNCNTA LIKE '1.1.02%' THEN 'SI' ELSE 'NO' END AS prefijo_1_1_02
  FROM CNT.PLNN p
 WHERE (EXISTS (SELECT 1 FROM TSR.CNBC c WHERE c.PLNNCDGO = p.PLNNCDGO)
        OR p.PLNNCNTA LIKE '1.1.02%')
   AND (CASE WHEN EXISTS (SELECT 1 FROM TSR.CNBC c WHERE c.PLNNCDGO = p.PLNNCDGO) THEN 1 ELSE 0 END)
       <> (CASE WHEN p.PLNNCNTA LIKE '1.1.02%' THEN 1 ELSE 0 END)
 ORDER BY p.PLNNCNTA;

-- BLOQUE 3 — Asientos desde el 01-10-2026, por tipo, módulo generador y lado del banco.
-- ESPERADO según la regla: lado_banco DEBE → tipo Ingreso; HABER → Egreso; SIN BANCO → tipo del módulo,
-- nunca Ingreso/Egreso ni Diario. Las filas con ASNTRZZB nulo son, en principio, asientos manuales.
-- AMBOS = el asiento tiene banco en el debe y en el haber (p. ej. transferencia entre cuentas propias):
-- la regla no dice qué tipo lleva; se decide con el usuario.
SELECT 'BLOQUE 3 - octubre' AS bloque, t.PLNTCDAL, t.PLNTNMBR, a.ASNTRZZB AS modulo_sistema, x.lado_banco,
       COUNT(*) AS asientos
  FROM CNT.ASNT a
  JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO
  JOIN (SELECT d.ASNTCDGO,
               CASE
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASDBEE, 0) <> 0 THEN 1 ELSE 0 END) > 0
                  AND SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASHBRR, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'AMBOS'
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASDBEE, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'DEBE'
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASHBRR, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'HABER'
                 ELSE 'SIN BANCO'
               END AS lado_banco
          FROM CNT.DTAS d
          LEFT JOIN (SELECT DISTINCT PLNNCDGO FROM TSR.CNBC) c ON c.PLNNCDGO = d.PLNNCDGO
         GROUP BY d.ASNTCDGO) x ON x.ASNTCDGO = a.ASNTCDGO
 WHERE a.ASNTFCHA >= DATE '2026-10-01'
 GROUP BY t.PLNTCDAL, t.PLNTNMBR, a.ASNTRZZB, x.lado_banco
 ORDER BY a.ASNTRZZB, x.lado_banco, t.PLNTCDAL;

-- BLOQUE 4 — Mismo corte que el 3, pero solo los asientos que hoy INCUMPLEN la regla, con su número,
-- para ver ejemplos concretos. Ajustar los nombres de tipo en el WHERE con lo que dé el BLOQUE 1 si no
-- empiezan con ING / EGR / DIA.
SELECT 'BLOQUE 4 - incumplen' AS bloque, a.ASNTNMAL, a.ASNTFCHA, t.PLNTNMBR, a.ASNTRZZB AS modulo_sistema,
       x.lado_banco, a.ASNTUSRO
  FROM CNT.ASNT a
  JOIN CNT.PLNT t ON t.PLNTCDGO = a.PLNTCDGO
  JOIN (SELECT d.ASNTCDGO,
               CASE
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASDBEE, 0) <> 0 THEN 1 ELSE 0 END) > 0
                  AND SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASHBRR, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'AMBOS'
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASDBEE, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'DEBE'
                 WHEN SUM(CASE WHEN c.PLNNCDGO IS NOT NULL AND NVL(d.DTASHBRR, 0) <> 0 THEN 1 ELSE 0 END) > 0 THEN 'HABER'
                 ELSE 'SIN BANCO'
               END AS lado_banco
          FROM CNT.DTAS d
          LEFT JOIN (SELECT DISTINCT PLNNCDGO FROM TSR.CNBC) c ON c.PLNNCDGO = d.PLNNCDGO
         GROUP BY d.ASNTCDGO) x ON x.ASNTCDGO = a.ASNTCDGO
 WHERE a.ASNTFCHA >= DATE '2026-10-01'
   AND a.ASNTRZZB IS NOT NULL
   AND (   (x.lado_banco = 'DEBE'      AND UPPER(t.PLNTNMBR) NOT LIKE 'ING%')
        OR (x.lado_banco = 'HABER'     AND UPPER(t.PLNTNMBR) NOT LIKE 'EGR%')
        OR (x.lado_banco = 'SIN BANCO' AND (UPPER(t.PLNTNMBR) LIKE 'ING%' OR UPPER(t.PLNTNMBR) LIKE 'EGR%'))
        OR UPPER(t.PLNTNMBR) LIKE 'DIA%')
 ORDER BY a.ASNTFCHA, a.ASNTNMAL;
