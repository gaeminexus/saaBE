-- =====================================================================================
-- LINEAS DE BANDA SIN CUENTA CONTABLE — asientos de condonacion ya grabados
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 209 (rango 200-249)
--
-- ⛔ EL BLOQUE 2 DE ESTE SCRIPT FALLA CON ORA-30926 — USAR sql/210 EN SU LUGAR.
--    Su UPDATE lleva un JOIN dentro de la subconsulta correlacionada y Oracle no lo acepta.
--    Los bloques 0 y 1 (solo SELECT) SI sirven y son los que produjeron los datos del 210.
--
-- EL DEFECTO, medido contra el codigo y confirmado con un asiento real
-- (CRE-2026-08-0459, condonacion del acuerdo 7):
--   `AcuerdoCondonacionServiceImpl` armaba la linea de banda seteando solo los textos
--   (DTASCNTA / DTASNMCT) y NUNCA `setPlanCuenta(...)`, asi que CNT.DTAS.PLNNCDGO quedaba
--   en NULL. El bloque calcado de Petro (CobroPetroContableServiceImpl:915) si lo hace:
--   fue una linea que se cayo en la copia.
--   Corregido en el codigo por el commit de este mismo dia; este script repara lo ya grabado.
--
-- ⭐ POR QUE LA REPARACION ES SEGURA:
--   En las filas rotas DTASCNTA y DTASNMCT SI tienen el texto correcto —
--   `ClasificadorBandaServiceImpl:117-121` setea idPlanCuenta, cuentaContable y nombreCuenta
--   dentro del MISMO `if (banda.getPlanCuenta() != null)`: van los tres o ninguno. Y la guarda
--   de `AcuerdoCondonacionServiceImpl:801` lanza si idPlanCuenta es null, asi que un asiento
--   que existe tenia los tres. Solo falto copiar la FK.
--   => La cuenta se resuelve buscando en CNT.PLNN por el numero ya guardado. No hay que
--      reconstruir nada desde CRD.BNDP.
--
-- ⛔ EL CRUCE VA POR CUENTA **Y EMPRESA**. CNT.PLNN esta scopeado por PJRQCDGO igual que
--    CNT.ASNT: el mismo numero de cuenta existe en varias empresas. Cruzar solo por numero
--    engancharia la cuenta de OTRA empresa, en silencio y sin error.
--
-- ⚠️ SECUENCIAS: no se inserta ninguna fila. Solo UPDATE de una FK. Nada que sincronizar.
--
-- ⚠️ SI YA SE MAYORIZO el periodo de estos asientos, avisar ANTES de correr esto: puede
--    hacer falta remayorizar para que los valores lleguen al mayor.
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los bloques 0, 1 y 3.
-- =====================================================================================

SET PAGESIZE 200
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 0 — CONTROL ANTES: cuantas lineas quedaron sin cuenta, y de que asientos
-- Esperado: al menos las dos lineas de banda del asiento CRE-2026-08-0459.
-- Si devuelve 0 filas, PARAR: el defecto no dejo el rastro que se asume.
-- =====================================================================================

SELECT l.DTASCDGO   AS LINEA,
       a.ASNTCDGO   AS ASIENTO,
       a.ASNTNMAL   AS NUMERO_ALTERNO,
       a.ASNTFCHA   AS FECHA_ASIENTO,
       a.PJRQCDGO   AS EMPRESA,
       l.DTASCNTA   AS CUENTA_TEXTO,
       l.DTASNMCT   AS NOMBRE_TEXTO,
       l.PLNNCDGO   AS FK_PLAN_CUENTA,
       l.DTASDBEE   AS DEBE,
       l.DTASHBRR   AS HABER,
       l.DTASDSCR   AS DESCRIPCION
  FROM CNT.DTAS l
  JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO
 WHERE l.PLNNCDGO IS NULL
 ORDER BY a.ASNTCDGO, l.DTASCDGO;

-- 0.2 Totales, para dimensionar
SELECT COUNT(*)                        AS LINEAS_SIN_CUENTA,
       COUNT(DISTINCT l.ASNTCDGO)      AS ASIENTOS_AFECTADOS,
       ROUND(SUM(NVL(l.DTASDBEE,0)), 2) AS TOTAL_DEBE,
       ROUND(SUM(NVL(l.DTASHBRR,0)), 2) AS TOTAL_HABER
  FROM CNT.DTAS l
 WHERE l.PLNNCDGO IS NULL;


-- =====================================================================================
-- BLOQUE 1 — ¿Cada linea rota resuelve a UNA sola cuenta de SU empresa?
-- Esta es la verificacion que decide si el bloque 2 se puede correr.
--
-- COINCIDENCIAS debe ser 1 en TODAS las filas.
--   0 -> el texto guardado no existe como cuenta en esa empresa: PARAR y avisar.
--   >1 -> la cuenta esta duplicada en la misma empresa: PARAR, el UPDATE elegiria al azar.
-- =====================================================================================

SELECT l.DTASCDGO AS LINEA,
       a.PJRQCDGO AS EMPRESA,
       l.DTASCNTA AS CUENTA_TEXTO,
       (SELECT COUNT(*) FROM CNT.PLNN pc
         WHERE pc.PLNNCNTA = l.DTASCNTA
           AND pc.PJRQCDGO = a.PJRQCDGO) AS COINCIDENCIAS,
       (SELECT MIN(pc.PLNNCDGO) FROM CNT.PLNN pc
         WHERE pc.PLNNCNTA = l.DTASCNTA
           AND pc.PJRQCDGO = a.PJRQCDGO) AS PLAN_CUENTA_RESUELTO,
       (SELECT MIN(pc.PLNNNMBR) FROM CNT.PLNN pc
         WHERE pc.PLNNCNTA = l.DTASCNTA
           AND pc.PJRQCDGO = a.PJRQCDGO) AS NOMBRE_RESUELTO
  FROM CNT.DTAS l
  JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO
 WHERE l.PLNNCDGO IS NULL
 ORDER BY l.DTASCDGO;


-- =====================================================================================
-- BLOQUE 2 — LA REPARACION
-- Solo toca las filas donde el bloque 1 dio COINCIDENCIAS = 1. Las de 0 o >1 quedan
-- intactas a proposito: el `= 1` del EXISTS es la guarda.
-- =====================================================================================

UPDATE CNT.DTAS l
   SET l.PLNNCDGO = (SELECT pc.PLNNCDGO
                       FROM CNT.PLNN pc
                       JOIN CNT.ASNT a2 ON a2.ASNTCDGO = l.ASNTCDGO
                      WHERE pc.PLNNCNTA = l.DTASCNTA
                        AND pc.PJRQCDGO = a2.PJRQCDGO)
 WHERE l.PLNNCDGO IS NULL
   AND l.DTASCNTA IS NOT NULL
   AND (SELECT COUNT(*)
          FROM CNT.PLNN pc
          JOIN CNT.ASNT a3 ON a3.ASNTCDGO = l.ASNTCDGO
         WHERE pc.PLNNCNTA = l.DTASCNTA
           AND pc.PJRQCDGO = a3.PJRQCDGO) = 1;


-- =====================================================================================
-- BLOQUE 3 — CONTROL DESPUES (mirar ANTES de confirmar)
-- Esperado 3.1: las lineas de CRE-2026-08-0459 con su PLNNCDGO puesto y el nombre
--               resuelto coincidiendo con el texto que ya tenian.
-- Esperado 3.2: 0 filas, o solo las que el bloque 1 marco con COINCIDENCIAS distinto de 1.
-- Si no da eso: ROLLBACK y avisar.
-- =====================================================================================

-- 3.1 Como quedaron las que se repararon
SELECT l.DTASCDGO AS LINEA, a.ASNTNMAL AS ASIENTO, l.DTASCNTA AS CUENTA_TEXTO,
       l.PLNNCDGO AS FK_PUESTA, pc.PLNNCNTA AS CUENTA_FK, pc.PLNNNMBR AS NOMBRE_FK,
       l.DTASNMCT AS NOMBRE_TEXTO_ORIGINAL,
       CASE WHEN pc.PLNNCNTA = l.DTASCNTA THEN 'OK' ELSE '⛔ NO COINCIDE' END AS CONTROL
  FROM CNT.DTAS l
  JOIN CNT.ASNT a  ON a.ASNTCDGO = l.ASNTCDGO
  JOIN CNT.PLNN pc ON pc.PLNNCDGO = l.PLNNCDGO
 WHERE l.DTASDSCR LIKE 'Condonación acuerdo%'
 ORDER BY l.DTASCDGO;

-- 3.2 Lo que quedo sin reparar, si algo quedo
SELECT l.DTASCDGO, a.ASNTNMAL, a.PJRQCDGO AS EMPRESA, l.DTASCNTA, l.DTASDSCR
  FROM CNT.DTAS l
  JOIN CNT.ASNT a ON a.ASNTCDGO = l.ASNTCDGO
 WHERE l.PLNNCDGO IS NULL
 ORDER BY l.DTASCDGO;


-- =====================================================================================
-- BLOQUE 4 — CONFIRMAR
-- Descomentar recien despues de leer el BLOQUE 3.
-- =====================================================================================

-- COMMIT;


-- =====================================================================================
-- BLOQUE 5 — REVERSO (comentado)
-- Devuelve a NULL solo las lineas de condonacion, que son las que este script toca.
-- Sirve ANTES del COMMIT (ahi alcanza ROLLBACK) o para deshacerlo despues.
-- =====================================================================================

-- UPDATE CNT.DTAS SET PLNNCDGO = NULL
--  WHERE DTASDSCR LIKE 'Condonación acuerdo%' AND DTASDSCR LIKE '%banda%';
-- COMMIT;
