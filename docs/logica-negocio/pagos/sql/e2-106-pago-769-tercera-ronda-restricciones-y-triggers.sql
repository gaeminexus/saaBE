-- =====================================================================================
-- e2-106 — Pago 769 (anticipo #181): tercera ronda. SOLO LECTURA. Equipo omen-saa-2 · 2026-10-15.
--
-- e2-104 (configuración) y e2-105 (secuencias, período, empleado, descuento previo) salieron LIMPIOS.
-- Queda lo que la base valida por su cuenta y el código no ve: índices únicos, CHECK, triggers, y que el
-- empleado 56 ya tenga otro descuento/anticipo que choque con el nuevo.
-- =====================================================================================

-- BLOQUE 1 — Lo que el empleado 56 ya tiene. Mirar si hay un descuento VIGENTE del mismo concepto (22) o un
-- anticipo anterior todavía en descuento: si existe un índice único por empleado+concepto (BLOQUE 2), choca.
SELECT 'BLOQUE 1a - descuentos del 56' AS bloque, d.DSRCCDGO, d.CPNMCDGO, d.DSRCTPDS, d.DSRCNMRO, d.DSRCVLOR,
       d.DSRCSLDD, d.DSRCESTD, d.DSRCFCHI, d.DSRCFCHR
  FROM RHH.DSRC d
 WHERE d.MPLDCDGO = 56
 ORDER BY d.DSRCCDGO;

SELECT 'BLOQUE 1b - anticipos del 56' AS bloque, a.ANTECDGO, a.ANTEVLOR, a.ANTEESTD, a.ANTEFCHI, a.DSRCCDGO
  FROM RHH.ANTE a
 WHERE a.MPLDCDGO = 56
 ORDER BY a.ANTECDGO;

-- BLOQUE 2 — Índices únicos de las tablas que se graban al confirmar. ESPERADO: solo las PK (…CDGO).
-- Cualquier otro índice único (p. ej. RHH.DSRC por MPLDCDGO+CPNMCDGO) es candidato a ser la causa.
SELECT 'BLOQUE 2 - indices unicos' AS bloque, i.TABLE_OWNER, i.TABLE_NAME, i.INDEX_NAME,
       LISTAGG(c.COLUMN_NAME, ', ') WITHIN GROUP (ORDER BY c.COLUMN_POSITION) AS columnas
  FROM ALL_INDEXES i
  JOIN ALL_IND_COLUMNS c ON c.INDEX_OWNER = i.OWNER AND c.INDEX_NAME = i.INDEX_NAME
 WHERE i.UNIQUENESS = 'UNIQUE'
   AND (i.TABLE_OWNER, i.TABLE_NAME) IN (('RHH','DSRC'), ('RHH','CTDS'), ('RHH','ANTE'), ('CNT','ASNT'),
                                         ('CNT','DTAS'), ('TSR','MVCB'), ('PGS','PGTR'))
 GROUP BY i.TABLE_OWNER, i.TABLE_NAME, i.INDEX_NAME
 ORDER BY i.TABLE_OWNER, i.TABLE_NAME, i.INDEX_NAME;

-- BLOQUE 3 — Restricciones CHECK (sin las NOT NULL de sistema). Una CHECK sobre un estado o un valor puede
-- rechazar la fila nueva. ESPERADO: pocas o ninguna; pegarme las que salgan.
SELECT 'BLOQUE 3 - check' AS bloque, k.OWNER, k.TABLE_NAME, k.CONSTRAINT_NAME, k.SEARCH_CONDITION_VC
  FROM ALL_CONSTRAINTS k
 WHERE k.CONSTRAINT_TYPE = 'C'
   AND k.SEARCH_CONDITION_VC NOT LIKE '%IS NOT NULL'
   AND (k.OWNER, k.TABLE_NAME) IN (('RHH','DSRC'), ('RHH','CTDS'), ('RHH','ANTE'), ('CNT','ASNT'),
                                   ('CNT','DTAS'), ('TSR','MVCB'), ('PGS','PGTR'))
 ORDER BY k.OWNER, k.TABLE_NAME;

-- BLOQUE 4 — Triggers en esas tablas. ESPERADO: ninguno, o ninguno que lance RAISE_APPLICATION_ERROR.
-- Si sale alguno, pegarme también su cuerpo: SELECT TRIGGER_BODY FROM ALL_TRIGGERS WHERE TRIGGER_NAME = '...';
SELECT 'BLOQUE 4 - triggers' AS bloque, t.OWNER, t.TABLE_NAME, t.TRIGGER_NAME, t.TRIGGER_TYPE,
       t.TRIGGERING_EVENT, t.STATUS
  FROM ALL_TRIGGERS t
 WHERE (t.TABLE_OWNER, t.TABLE_NAME) IN (('RHH','DSRC'), ('RHH','CTDS'), ('RHH','ANTE'), ('CNT','ASNT'),
                                         ('CNT','DTAS'), ('TSR','MVCB'), ('PGS','PGTR'))
 ORDER BY t.TABLE_OWNER, t.TABLE_NAME;

-- BLOQUE 5 — El pago 769 completo contra el último anticipo que sí se confirmó (730). Lo que difiera entre
-- los dos (cheque, lote, débito automático, longitud de observación/referencia) es pista.
SELECT 'BLOQUE 5 - 769 vs 730' AS bloque, p.PGTRCDGO, p.PGTRESTD, p.PGTRVLOR, p.PGTRFPRG, p.PGTRFRSP,
       p.PGTRRFBN, LENGTH(p.PGTROBSR) AS largo_observacion, p.PGTROBSR
  FROM PGS.PGTR p
 WHERE p.PGTRCDGO IN (730, 769);
