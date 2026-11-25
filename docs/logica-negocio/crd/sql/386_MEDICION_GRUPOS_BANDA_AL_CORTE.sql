-- =====================================================================================
-- 386 — MEDICIÓN (SOLO LECTURA): ¿los grupos de bandas Q/P/H cambian si se filtra la configuración al corte? (6.6)
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada.
--
-- Contexto: `ParametroContableService.gruposBandaCartera` arma el grupo (Q, P, H o SIN GRUPO) de cada cuenta de banda con
-- TODA configuración de bandas activa (CBPRESTD = 1, BNDPESTD = 1), sin mirar su vigencia. `cuentaBandaMayorPorMadre`, en
-- cambio, ya se filtra al corte (CBPRFCIN <= corte y CBPRFCFN nula o >= corte). Esta consulta compara, para cada fin de
-- mes de jun-2025 a oct-2026, el grupo de cada cuenta SIN filtro contra el grupo FILTRADO al corte.
--
-- Regla del grupo (igual que el Java): la cuenta tiene grupo si los productos que la usan tienen UN solo tipo de préstamo
-- (PRDC.TPPRCDGO) y ese tipo es 1 (Q), 3 (P) o 2 (H); con varios tipos, un tipo nulo o un tipo sin letra queda SIN GRUPO.
--
-- Cómo leerlo: si la consulta devuelve 0 filas, filtrar al corte NO cambia ningún grupo y no hace falta tocar
-- gruposBandaCartera. Si devuelve filas, esas son las cuentas/cortes donde difieren. Cambiar la raíz de cartera y la empresa
-- en PARAMS (raíz 1.3 = parámetro CUENTA_CARTERA_CREDITOS del rubro 250).
-- =====================================================================================

WITH params AS (
    SELECT 1236 AS empresa, '1.3.%' AS raiz FROM DUAL
),
cortes AS (
    SELECT LAST_DAY(ADD_MONTHS(DATE '2025-06-01', LEVEL - 1)) AS corte
      FROM DUAL CONNECT BY LEVEL <= 17      -- jun-2025 a oct-2026
),
filas AS (
    SELECT n.PLNNCNTA AS cuenta, c.CBPRCDGO, p.TPPRCDGO AS tipo, c.CBPRFCIN AS desde, c.CBPRFCFN AS hasta
      FROM params pa
      JOIN CNT.PLNN n ON n.PJRQCDGO = pa.empresa AND n.PLNNCNTA LIKE pa.raiz
      JOIN CRD.BNDP b ON b.PLNNCDGO = n.PLNNCDGO AND b.BNDPESTD = 1
      JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO AND c.CBPRESTD = 1
      JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
),
sin_filtro AS (
    SELECT cuenta, COUNT(DISTINCT NVL(tipo, -1)) AS tipos, MIN(NVL(tipo, -1)) AS tipo
      FROM filas GROUP BY cuenta
),
al_corte AS (
    SELECT k.corte, f.cuenta, COUNT(DISTINCT NVL(f.tipo, -1)) AS tipos, MIN(NVL(f.tipo, -1)) AS tipo
      FROM cortes k
      JOIN filas f ON f.desde <= k.corte AND (f.hasta IS NULL OR f.hasta >= k.corte)
     GROUP BY k.corte, f.cuenta
),
grupos AS (
    SELECT k.corte, s.cuenta,
           CASE WHEN s.tipos = 1 THEN DECODE(s.tipo, 1, 'Q', 3, 'P', 2, 'H', 'SIN GRUPO') ELSE 'SIN GRUPO' END AS grupo_sin_filtro,
           CASE WHEN a.cuenta IS NULL THEN 'AUSENTE AL CORTE'
                WHEN a.tipos = 1 THEN DECODE(a.tipo, 1, 'Q', 3, 'P', 2, 'H', 'SIN GRUPO')
                ELSE 'SIN GRUPO' END AS grupo_al_corte
      FROM cortes k
      CROSS JOIN sin_filtro s
      LEFT JOIN al_corte a ON a.corte = k.corte AND a.cuenta = s.cuenta
)
SELECT corte, cuenta, grupo_sin_filtro, grupo_al_corte
  FROM grupos
 WHERE grupo_sin_filtro <> grupo_al_corte
 ORDER BY corte, cuenta;

-- Complemento (informativo): configuraciones con vigencia cerrada o futura, que son las únicas que pueden hacer
-- diferir el resultado. Si esta lista está vacía, nunca difieren.
SELECT c.CBPRCDGO, c.PRDCCDGO, c.CBPRTPCR, c.CBPRFCIN, c.CBPRFCFN, c.CBPRESTD
  FROM CRD.CBPR c
 WHERE c.CBPRESTD = 1 AND (c.CBPRFCFN IS NOT NULL OR c.CBPRFCIN > DATE '2025-06-30')
 ORDER BY c.PRDCCDGO, c.CBPRFCIN;
