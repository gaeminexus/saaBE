-- ============================================================================
-- 61 - MOVER EL INSTRUMENTO A JUNIO Y COMPROBARLO
-- ==
-- Se corre en PRODUCCION, con junio ya CALCULADO y en estado 3, ANTES de
-- aprobarlo. Es el paso 1 del paragrafo 5 del GUION-MES-2026-06.md.
-- ==
-- Este script SI escribe: una fila, dos columnas, en la tabla del instrumento
-- de contraste. No toca ningun dato de nomina.
-- ==
-- POR QUE TIENE SU PROPIO SCRIPT EN VEZ DE SER UN UPDATE SUELTO. CTRL_PARAM
-- falla en las dos direcciones y una de ellas no se nota:
--   ADELANTADO -el mes aun sin calcular- los bloques salen VACIOS, y un vacio
--   al menos extraña.
--   ATRASADO -un mes anterior ya cerrado- NO VACIA NADA: el instrumento
--   contrasta ESE OTRO MES, con su CTRL y su NMNA completos, y sale VERDE AL
--   CENTIMO. Es el caso peor, porque nada lo delata.
-- ==
-- Hoy esta en 2026 . 4, que es ABRIL: un mes cerrado, contrastado y en cero.
-- Correr el contraste sin mover esto daria un abril impecable con aspecto de
-- junio perfecto. Por eso el UPDATE va con su SELECT de antes y su SELECT de
-- despues, y por eso el bloque 4 del instrumento se mira ANTES que ninguna cifra.
-- ============================================================================

-- ============================================================================
-- PASO 1 - Donde esta ahora. ESPERADO: 2026 . 4
-- ============================================================================
SELECT ANIO, MES FROM RHH.CTRL_PARAM;

-- ============================================================================
-- PASO 2 - Moverlo a junio.
-- ============================================================================
UPDATE RHH.CTRL_PARAM SET ANIO = 2026, MES = 6;
COMMIT;

-- ============================================================================
-- PASO 3 - Comprobarlo. ESPERADO: 2026 . 6, y UNA SOLA FILA.
-- Si devuelve mas de una fila, PARAR: el instrumento lee la primera que le
-- venga y deja de ser determinista.
-- ============================================================================
SELECT ANIO, MES, COUNT(*) OVER () AS FILAS_EN_LA_TABLA FROM RHH.CTRL_PARAM;

-- ============================================================================
-- PASO 4 - Que el periodo que se va a contrastar es el que se calculo.
-- ESPERADO: PRDN 61, ano 2026, mes 6, estado 3 CALCULADO, 20 nominas.
-- El estado 3 es deliberado: el contraste va ANTES de aprobar. El instrumento
-- lee NMNA, RNGL, PVNM y CTRL, y NO lee ACMN, que es lo unico que escribe
-- cerrarPeriodo: da el mismo resultado en 3 que en 7. En estado 3 un fallo se
-- arregla recalculando; en 7 obliga a reabrir.
-- ============================================================================
SELECT p.PRDNCDGO AS PRDN, p.PRDNANOO AS ANIO, p.PRDNMSEE AS MES,
       p.PRDNESTD AS ESTADO, COUNT(n.NMNACDGO) AS NOMINAS
  FROM RHH.PRDN p
  LEFT JOIN RHH.NMNA n ON n.PRDNCDGO = p.PRDNCDGO
 WHERE p.PRDNANOO = 2026 AND p.PRDNMSEE = 6
 GROUP BY p.PRDNCDGO, p.PRDNANOO, p.PRDNMSEE, p.PRDNESTD;

-- ============================================================================
-- PASO 5 - Ahora si, el instrumento: CONTRASTE_MES_CONTRA_ROL_REAL.sql
-- ==
-- ORDEN DE LECTURA, y no es el orden en que estan escritos en el archivo:
--   BLOQUE 4 primero  - que la comparacion sea completa
--   BLOQUE 3          - total IESS afiliado por afiliado
--   BLOQUES 1 y 2     - diferencias por concepto y en los totales
--   BLOQUE 1B         - lo que el rol no imprime, con sus DOS consultas
-- ==
-- Y en CADA bloque, lo primero que se mira es PERIODO_LEIDO = 2026-06, antes
-- que ninguna cifra. Un control que no dice sobre que corrio no es un control.
-- ============================================================================
