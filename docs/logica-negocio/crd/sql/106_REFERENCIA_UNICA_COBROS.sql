-- =====================================================================================
-- Referencia UNICA en los cobros (CRD.CBCR.CBCRRFRN)
-- FECHA: 2026-09-01 · Equipo A de crd   ·   REESCRITO con los codigos de estado REALES
--
-- ⛔ NO CORRER DE CORRIDO. El bloque 0 es solo lectura y decide si el resto se puede correr.
--
-- DECISIONES DEL USUARIO (2026-09-01):
--   · La referencia no se puede repetir.
--   · '9' y '09' son los valores de "no tengo referencia" y SI pueden repetirse.
--   · Un cobro ANULADO **libera** su referencia: se puede volver a usar.
--
-- ⚠️ CORRECCION IMPORTANTE. La primera version de este script asumia que ANULADO era el
-- estado 3. **Es el 5.** Los codigos reales, de com.saa.rubros.CrdEstadoCobro:
--
--     1 REGISTRADO · 2 APROBADO · 3 PROCESADO · 4 RECHAZADO · 5 ANULADO
--
-- Con el codigo equivocado, la consulta que separa "conflictos vivos" excluia justamente
-- los PROCESADOS — los cobros mas reales que hay — y habria dado un falso "todo limpio".
--
-- COMO SE IMPLEMENTA. Indice unico basado en funcion: la expresion devuelve NULL para el
-- '9', el '09' y los anulados. Oracle **no compara NULLs entre si** en un indice unico, asi
-- que todos esos conviven; cualquier otro valor solo puede aparecer una vez.
-- Se compara con TRIM: '9 ' cuenta como '9', y 'ABC' / 'ABC ' son la MISMA referencia.
-- =====================================================================================


-- =====================================================================================
-- BLOQUE 0 — SOLO LECTURA
-- =====================================================================================

-- 0.1 Panorama.
SELECT COUNT(*)                                                          AS TOTAL_COBROS,
       COUNT(c.CBCRRFRN)                                                 AS CON_REFERENCIA,
       SUM(CASE WHEN TRIM(c.CBCRRFRN) IN ('9','09') THEN 1 ELSE 0 END)   AS CON_NUEVE,
       SUM(CASE WHEN c.CBCRRFRN IS NULL THEN 1 ELSE 0 END)               AS SIN_REFERENCIA
FROM   CRD.CBCR c;


-- 0.2 ⚠️ LA CONSULTA QUE DECIDE — conflictos REALES, con la regla ya acordada:
--     sin '9'/'09', y sin contar los ANULADOS (estado 5), que liberan su referencia.
--     **Si devuelve filas, el indice NO se puede crear hasta resolverlas a mano.**
SELECT TRIM(c.CBCRRFRN)                        AS REFERENCIA,
       COUNT(*)                                AS CUANTOS_VIVOS,
       LISTAGG(c.CBCRCDGO, ', ')
           WITHIN GROUP (ORDER BY c.CBCRCDGO)  AS CODIGOS_DE_COBRO,
       LISTAGG(c.CBCRESTD, ', ')
           WITHIN GROUP (ORDER BY c.CBCRCDGO)  AS ESTADOS
FROM   CRD.CBCR c
WHERE  c.CBCRRFRN IS NOT NULL
AND    TRIM(c.CBCRRFRN) NOT IN ('9','09')
AND    NVL(c.CBCRESTD, 0) <> 5
GROUP  BY TRIM(c.CBCRRFRN)
HAVING COUNT(*) > 1
ORDER  BY 2 DESC, 1;


-- 0.3 El detalle de cada cobro en conflicto, para poder decidir cual se corrige.
--     Muestra fecha, valor, entidad y los tres asientos: un cobro CON asientos generados
--     no se toca a la ligera.
SELECT c.CBCRCDGO, c.CBCRTPOO, c.CBCRESTD, c.CBCRRFRN, c.CBCRFCHA, c.CBCRVLRR,
       c.CBCRASN1, c.CBCRASRP, c.CBCRASN2, c.CBCRUSRG
FROM   CRD.CBCR c
WHERE  TRIM(c.CBCRRFRN) IN (
           SELECT TRIM(x.CBCRRFRN)
           FROM   CRD.CBCR x
           WHERE  x.CBCRRFRN IS NOT NULL
           AND    TRIM(x.CBCRRFRN) NOT IN ('9','09')
           AND    NVL(x.CBCRESTD, 0) <> 5
           GROUP  BY TRIM(x.CBCRRFRN)
           HAVING COUNT(*) > 1)
ORDER  BY TRIM(c.CBCRRFRN), c.CBCRCDGO;


-- 0.4 ¿Ya hay algun indice sobre esa columna?
SELECT i.INDEX_NAME, i.UNIQUENESS, i.STATUS
FROM   ALL_IND_COLUMNS ic
JOIN   ALL_INDEXES i ON i.OWNER = ic.INDEX_OWNER AND i.INDEX_NAME = ic.INDEX_NAME
WHERE  ic.TABLE_OWNER = 'CRD' AND ic.TABLE_NAME = 'CBCR'
AND    ic.COLUMN_NAME = 'CBCRRFRN';


-- =====================================================================================
-- BLOQUE 1 — EL INDICE. Correr SOLO cuando 0.2 devuelva CERO filas.
-- =====================================================================================
-- Refleja las dos decisiones: '9'/'09' se repiten, y los ANULADOS (5) liberan su
-- referencia. Si el indice se crea con conflictos vivos, Oracle lo rechaza con ORA-01452
-- y no rompe nada — pero conviene mirar el 0.2 primero para saber que hay que arreglar.

CREATE UNIQUE INDEX CRD.UX_CBCR_REFERENCIA
    ON CRD.CBCR (CASE WHEN TRIM(CBCRRFRN) IN ('9','09') OR NVL(CBCRESTD, 0) = 5
                      THEN NULL
                      ELSE TRIM(CBCRRFRN) END);


-- =====================================================================================
-- BLOQUE 2 — VERIFICACION
-- =====================================================================================
-- 2.1 Quedo UNIQUE y en el schema CRD (no en el de la sesion). Esperado: 1 fila, UNIQUE.
SELECT i.OWNER, i.INDEX_NAME, i.UNIQUENESS, i.STATUS
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME = 'UX_CBCR_REFERENCIA';


-- =====================================================================================
-- BLOQUE 3 — REVERSO. Comentado a proposito.
-- =====================================================================================
-- DROP INDEX CRD.UX_CBCR_REFERENCIA;
-- =====================================================================================
