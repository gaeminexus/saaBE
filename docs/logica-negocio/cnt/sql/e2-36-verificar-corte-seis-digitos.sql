-- =====================================================================
-- e2-36 — El unico control que falta del corte a seis digitos del balance
-- Modulo: CNT  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT.
--
-- 🔴 POR QUE EXISTE
--   Los seis reportes de balance (bc9e0a1a) ahora cortan las cuentas por
--   longitud de codigo: la Resolucion SBS-2013-0507 obliga el catalogo "hasta un
--   nivel de seis (6) digitos" y deja las subcuentas auxiliares de 7+ digitos
--   libres para uso interno, asi que un estado formal no debe mezclarlas.
--
--   El corte es: LENGTH(REPLACE(DTMTCTCN,'.','')) <= P_NIVEL_MAXIMO (default 6).
--
--   Ya esta verificado, leyendo el codigo, que el corte NO puede alterar un
--   total:
--     - Las seis consultas alimentan sus totales con decode(plnnnvll,1, ...):
--       el TOTAL solo suma filas de NIVEL 1, nunca las que el corte esconde.
--     - TempReportesServiceImpl.actualizaSaldosAcumulacion ya suma las cuentas
--       hijas hacia arriba, nivel por nivel, dentro de DTMT, ANTES de que el
--       Jasper corra. Una auxiliar de 7+ digitos ya esta dentro de su padre.
--
--   Queda UN supuesto que no se puede comprobar leyendo codigo, porque depende
--   de como este armado el plan de cuentas de ESTE fondo:
--
--     ⚠️ QUE NINGUNA CUENTA DE NIVEL 1 TENGA CODIGO DE MAS DE SEIS DIGITOS.
--
--   Si la hubiera, el corte la escondería y el TOTAL perderia esa rama. Es
--   rarisimo -el nivel 1 suele ser "1", "2", "3"- pero si pasa, el balance
--   saldria mal y sin avisar. Esto lo mide.
-- =====================================================================


-- =====================================================================
-- 0 — Que ejecucion mirar. Generar un balance en la pantalla y usar el
--     idEjecucion mas reciente, o elegir uno de esta lista.
-- =====================================================================
SELECT '0 - ejecuciones disponibles' AS control,
       d.DTMTSCRP AS id_ejecucion, COUNT(*) AS filas,
       MIN(d.PLNNNVLL) AS nivel_min, MAX(d.PLNNNVLL) AS nivel_max
  FROM CNT.DTMT d
 GROUP BY d.DTMTSCRP
 ORDER BY d.DTMTSCRP DESC;


-- =====================================================================
-- 1 — 🔴 EL CONTROL QUE DECIDE. ESPERADO: 0 filas.
--     Cuentas de NIVEL 1 cuyo codigo, sin los puntos, pasa de seis digitos.
--     Si devuelve algo, PARAR Y AVISAR: hay que subir el default de
--     P_NIVEL_MAXIMO o cambiar el criterio del corte. NO usar los reportes
--     para una entrega formal hasta resolverlo.
-- =====================================================================
SELECT '1 - nivel 1 con mas de 6 digitos' AS control,
       d.DTMTSCRP AS id_ejecucion, d.DTMTCTCN AS cuenta, d.PLNNNMBR AS nombre,
       d.PLNNNVLL AS nivel,
       LENGTH(REPLACE(d.DTMTCTCN, '.', '')) AS digitos,
       d.DTMTSLAC AS saldo_actual
  FROM CNT.DTMT d
 WHERE d.PLNNNVLL = 1
   AND LENGTH(REPLACE(d.DTMTCTCN, '.', '')) > 6
 ORDER BY d.DTMTSCRP DESC, d.DTMTCTCN;


-- =====================================================================
-- 2 — Como queda repartido el plan por cantidad de digitos y nivel.
--     Sirve para ver de un vistazo cuanto se oculta con el corte por defecto y
--     confirmar que el nivel 1 vive en 1 o 2 digitos, como se espera.
-- =====================================================================
SELECT '2 - reparto por digitos' AS control,
       d.PLNNNVLL AS nivel,
       LENGTH(REPLACE(d.DTMTCTCN, '.', '')) AS digitos,
       COUNT(*) AS cuentas,
       CASE WHEN LENGTH(REPLACE(d.DTMTCTCN, '.', '')) <= 6
            THEN 'SE IMPRIME (catalogo)' ELSE 'se oculta (auxiliar interna)' END AS con_default_6
  FROM CNT.DTMT d
 WHERE d.DTMTSCRP = (SELECT MAX(x.DTMTSCRP) FROM CNT.DTMT x)
 GROUP BY d.PLNNNVLL, LENGTH(REPLACE(d.DTMTCTCN, '.', ''))
 ORDER BY d.PLNNNVLL, digitos;


-- =====================================================================
-- 3 — Contraste del total: el que suma el reporte (solo nivel 1) contra el
--     que sumarian todas las cuentas que el corte deja pasar.
--     Es la comprobacion empirica de lo que el codigo ya garantiza.
-- =====================================================================
SELECT '3 - totales' AS control,
       SUM(CASE WHEN d.PLNNNVLL = 1 THEN NVL(d.DTMTSLAC, 0) ELSE 0 END) AS total_del_reporte_nivel1,
       SUM(CASE WHEN LENGTH(REPLACE(d.DTMTCTCN, '.', '')) <= 6 AND d.PLNNNVLL = 1
                THEN NVL(d.DTMTSLAC, 0) ELSE 0 END)                     AS total_nivel1_que_sobrevive_al_corte,
       SUM(CASE WHEN d.PLNNNVLL = 1 THEN NVL(d.DTMTSLAC, 0) ELSE 0 END)
       - SUM(CASE WHEN LENGTH(REPLACE(d.DTMTCTCN, '.', '')) <= 6 AND d.PLNNNVLL = 1
                  THEN NVL(d.DTMTSLAC, 0) ELSE 0 END)                   AS diferencia_debe_ser_cero
  FROM CNT.DTMT d
 WHERE d.DTMTSCRP = (SELECT MAX(x.DTMTSCRP) FROM CNT.DTMT x);


-- =====================================================================
-- QUE SIGUE
-- =====================================================================
-- - 1 devuelve 0 filas y 3 devuelve diferencia 0: el corte esta bien y los
--   balances se pueden usar para una entrega formal.
-- - 1 devuelve filas: AVISAR con el resultado. La cuenta de nivel 1 con codigo
--   largo se estaria escondiendo y el total perderia esa rama.
-- - El punto 2 ademas dice cuantas auxiliares internas se ocultan: si el numero
--   sorprende, vale la pena mirarlo antes de entregar.
-- =====================================================================
