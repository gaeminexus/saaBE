-- =====================================================================================
-- 389 — MEDICIÓN (SOLO LECTURA): ¿hay dos plantillas con el alterno 1 en la misma empresa? (6.4 del cuadre)
-- Equipo omen-saa-1 · 2026-11-24 · No modifica nada. Se puede correr en producción.
--
-- Contexto: PlantillasSistema.CUENTA_CIERRE = 1 (la plantilla del asiento de cierre del ejercicio, que lee
-- plantillaService.codigoByAlterno(1, empresa)) y PlantillasCredito.APERTURA_PLANILLA_MENSUAL = 1 (la apertura del
-- cierre de cartera) usan el MISMO código alterno. Si en CNT.PLNS conviven dos filas con PLNSCDAL = 1 para la misma empresa,
-- codigoByAlterno devuelve una de las dos sin criterio y el cierre del ejercicio podría tomar la plantilla de la apertura
-- (o al revés). Antes de sembrar la línea de redondeo del cierre del ejercicio hay que saber cuál es cuál.
-- =====================================================================================


-- BLOQUE 1 — Plantillas con alterno 1, por empresa. Esperado: UNA fila por empresa (CUANTAS = 1). Más de una = duplicado.
SELECT p.PJRQCDGO AS empresa, p.PLNSCDAL AS alterno, COUNT(*) AS cuantas,
       LISTAGG(p.PLNSCDGO || ' «' || p.PLNSNMBR || '»', ' | ') WITHIN GROUP (ORDER BY p.PLNSCDGO) AS plantillas
  FROM CNT.PLNS p
 WHERE p.PLNSCDAL = 1
 GROUP BY p.PJRQCDGO, p.PLNSCDAL
 ORDER BY p.PJRQCDGO;

-- BLOQUE 2 — Detalle de cada una: sistema, estado y sus líneas (para ver cuál es la de apertura de cartera y cuál la de cierre).
SELECT p.PJRQCDGO AS empresa, p.PLNSCDGO AS plantilla, p.PLNSNMBR AS nombre, p.PLNSSSTM AS sistema, p.PLNSESTD AS estado,
       d.DTPLCDGO AS linea, d.DTPLAXL1 AS aux1, d.DTPLAXL2 AS aux2, d.DTPLMVMN AS movimiento, n.PLNNCNTA AS cuenta, n.PLNNNMBR AS nombre_cuenta
  FROM CNT.PLNS p
  LEFT JOIN CNT.DTPL d ON d.PLNSCDGO = p.PLNSCDGO
  LEFT JOIN CNT.PLNN n ON n.PLNNCDGO = d.PLNNCDGO
 WHERE p.PLNSCDAL = 1
 ORDER BY p.PJRQCDGO, p.PLNSCDGO, d.DTPLAXL1, d.DTPLAXL2;

-- BLOQUE 3 — Lo que devolvería hoy codigoByAlterno(1, empresa): la primera fila por código (si hay duplicado, depende del orden
-- del motor). Esperado: la plantilla de «CIERRE» y no la de «APERTURA».
SELECT p.PJRQCDGO AS empresa, MIN(p.PLNSCDGO) AS plantilla_minima, MAX(p.PLNSCDGO) AS plantilla_maxima,
       CASE WHEN MIN(p.PLNSCDGO) = MAX(p.PLNSCDGO) THEN 'UNA' ELSE 'DUPLICADO' END AS veredicto
  FROM CNT.PLNS p
 WHERE p.PLNSCDAL = 1
 GROUP BY p.PJRQCDGO
 ORDER BY p.PJRQCDGO;
