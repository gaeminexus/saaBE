-- =====================================================================================================
-- c20-07 — DIAGNÓSTICO (SOLO LECTURA): ingreso de tesorería numerado como egreso (EG-A-2026-10-0011)
-- omen-saa-1-arb · 2026-11-05 · No modifica nada.
--
-- Caso: «Ingreso tesorería | Capital de la inversión de póliza … INV-DPF-59616», 2.795.000,00, 23-10-2026.
--   DEBE  1.1.02.05.75     COOP ALIANZA DEL VALLE CTA. AHO.   (la cuenta de ahorros donde entra el dinero)
--   HABER 1.2.01.05.03.07  COOP ALIANZA DEL VALLE             (la inversión que se recupera)
--
-- Regla automática del tipo (AsientoContableServiceImpl.clasificarTipoAutomatico): cuenta como «banco» toda línea
-- cuya cuenta contable sea el PLNNCDGO de una TSR.CNBC de un banco con BNCOREAL = 1. Neto > 0 = IN-A, < 0 = EG-A,
-- 0 con banco a los dos lados = EG-A (transferencia entre cuentas propias), sin banco = DI-A.
-- Hipótesis: la cuenta de la INVERSIÓN (1.2.01.05.03.07) está configurada como cuenta bancaria de un banco
-- «real» → banco a los dos lados, neto 0 → EG-A. (Banco.bancoReal vale 1 por defecto al crear un banco.)
-- =====================================================================================================

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 1 — Qué cuentas bancarias (TSR.CNBC) apuntan a las dos cuentas del asiento, y si su banco es «real».
-- Esperado si la hipótesis es cierta: una fila con la 1.2.01.05.03.07 y BNCOREAL = 1.
-- ---------------------------------------------------------------------------------------------------
SELECT n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE_CUENTA,
       c.CNBCCDGO, c.CNBCNMRO AS NUMERO_CUENTA_BANCARIA, c.CNBCESTD AS ESTADO_CNBC,
       b.BNCOCDGO, b.BNCONMBR AS BANCO, b.BNCOREAL, b.BNCOESTD AS ESTADO_BANCO
FROM   TSR.CNBC c
JOIN   TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
JOIN   CNT.PLNN n ON n.PLNNCDGO = c.PLNNCDGO
WHERE  n.PLNNCNTA IN ('1.1.02.05.75', '1.2.01.05.03.07')
ORDER BY n.PLNNCNTA, c.CNBCCDGO;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 2 — Barrido: TODAS las cuentas bancarias de bancos «reales» cuya cuenta contable NO es de fondos
-- disponibles (no empieza por 1.1.). Cada una puede convertir un ingreso o un egreso en «transferencia».
-- ---------------------------------------------------------------------------------------------------
SELECT n.PLNNCNTA AS CUENTA, n.PLNNNMBR AS NOMBRE_CUENTA,
       c.CNBCCDGO, c.CNBCNMRO AS NUMERO_CUENTA_BANCARIA, c.CNBCESTD AS ESTADO_CNBC,
       b.BNCOCDGO, b.BNCONMBR AS BANCO, b.BNCOREAL
FROM   TSR.CNBC c
JOIN   TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO
JOIN   CNT.PLNN n ON n.PLNNCDGO = c.PLNNCDGO
WHERE  b.BNCOREAL = 1
AND    n.PLNNCNTA NOT LIKE '1.1.%'
ORDER BY n.PLNNCNTA;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 3 — Asientos EG-A activos con banco en DEBE y HABER donde una de las cuentas «banco» no es 1.1.
-- (los que la regla pudo numerar mal por la misma causa). Incluye el EG-A-2026-10-0011.
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, TRUNC(a.ASNTFCHA) AS FECHA, a.ASNTUSRO AS USUARIO,
       SUBSTR(a.ASNTOBSR, 1, 100) AS GLOSA
FROM   CNT.ASNT a
WHERE  a.ASNTESTD = 1
AND    a.ASNTNMAL LIKE 'EG-A-%'
AND    EXISTS (SELECT 1 FROM CNT.DTAS d
               JOIN TSR.CNBC c ON c.PLNNCDGO = d.PLNNCDGO
               JOIN TSR.BNCO b ON b.BNCOCDGO = c.BNCOCDGO AND b.BNCOREAL = 1
               JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
               WHERE d.ASNTCDGO = a.ASNTCDGO AND n.PLNNCNTA NOT LIKE '1.1.%')
ORDER BY a.ASNTFCHA, a.ASNTCDGO;

-- ---------------------------------------------------------------------------------------------------
-- BLOQUE 4 — El asiento del caso: su tipo, numeración y período (para la corrección).
-- ---------------------------------------------------------------------------------------------------
SELECT a.ASNTCDGO, a.ASNTNMAL, a.ASNTNMRO, a.ASNTNMMS, a.ASNTMDOR, a.ASNTNMMD,
       a.PLNTCDGO AS TIPO_ASIENTO, TRUNC(a.ASNTFCHA) AS FECHA, a.ASNTESTD, a.MYRZCDGO
FROM   CNT.ASNT a
WHERE  a.ASNTNMAL = 'EG-A-2026-10-0011';
