-- =====================================================================================
-- 237 - Por que la previsualizacion dice $450,40 de seguro medico y el proceso genera $0
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. ⭐ NO ESCRIBE NADA: todo SELECT, se puede correr entero.
-- Los comentarios van ARRIBA de cada sentencia, nunca intercalados (leccion del 236).
--
-- =====================================================================================
-- LOS DOS NUMEROS Y POR QUE NO SON EL MISMO
-- =====================================================================================
-- Pantalla (previsualizacion 9/2026): SEGURO MEDICO (A PROVEEDOR) = $450,40, 139 aptos
--                                     de 182 evaluados.
-- Log del proceso:                    "Sin seguro medico que pagar al proveedor en
--                                      9/2026 ($0)".
--
-- NO es que uno de los dos este mal calculado: calculan COSAS DISTINTAS.
--
--   PREVISUALIZACION (previsualizarCorrida): suma el seguro NOMINAL de cada jubilado
--   APTO — el valor configurado en CRD.VPPC (VPPCVLSR). Es "cuanto habria que cobrar".
--
--   PROCESO (generarSeguroIndividual, §11.1 / H60): fija lo que REALMENTE se puede
--   cobrar, y vale 0 en tres casos:
--     1. El jubilado no tiene ANCLA (resolverAnclaRetroactivo devuelve null) -> 0.
--     2. El ancla dice que ya esta AL DIA para el periodo -> 0.
--     3. Hay ancla y no esta al dia -> min(nominal, saldo libre de aportes tipo 23).
--        Sin saldo, 0.
--
-- ⇒ Un total de $0 con 139 aptos significa que TODOS cayeron en 1, 2 o 3. Este script
--   dice en cual, que es lo unico que decide como se sigue.
--
-- ⛔ LA HIPOTESIS PRINCIPAL ES H46 — EL ANCLA ENVENENADA, ya registrada en el tablero:
-- "Si una corrida falla a mitad de camino, NO se puede reintentar. El movimiento
-- negativo de CRD.APRT sobrevive a cualquier reverso, el ancla queda envenenada y el
-- reintento informa 'al dia' y no le paga a nadie, sin lanzar ningun error."
-- Hoy hubo intentos fallidos: generarSeguroIndividual corre en REQUIRES_NEW, o sea que
-- CADA seguro se confirma en su propia transaccion aunque despues la corrida entera
-- falle (fallo por ORA-01400 en la cabecera CRJB). Esos movimientos ya movieron el ancla.
-- =====================================================================================


-- =====================================================================================
-- 1. LO PRIMERO: que quedo grabado de 9/2026 en CRD.PGPC
-- =====================================================================================
-- Si los intentos de hoy fijaron seguros, estan aca. Esperado, segun que haya pasado:
--   0 filas            -> no se fijo nada; el $0 NO viene de un intento previo.
--   N filas con valor  -> se fijaron con valor: U1 deberia sumarlos y dar $450,40.
--                         Si el proceso igual da $0, el defecto esta en la lectura de U1.
--   N filas en 0       -> ⛔ se fijaron EN CERO: es el caso 1 o 2 de arriba (sin ancla o
--                         al dia). El reintento va a seguir dando $0 para siempre.

SELECT COUNT(*)                                                       AS FILAS_9_2026,
       SUM(CASE WHEN p.PGPCVLSG IS NULL THEN 1 ELSE 0 END)            AS SEGURO_NULO,
       SUM(CASE WHEN p.PGPCVLSG = 0 THEN 1 ELSE 0 END)                AS SEGURO_EN_CERO,
       SUM(CASE WHEN p.PGPCVLSG > 0 THEN 1 ELSE 0 END)                AS SEGURO_CON_VALOR,
       SUM(NVL(p.PGPCVLSG, 0))                                        AS TOTAL_SEGURO_FIJADO
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 9;


-- =====================================================================================
-- 2. El detalle, por si el bloque 1 muestra una mezcla
-- =====================================================================================

SELECT p.ENTDCDGO, p.PGPCVLSG AS SEGURO_FIJADO, p.PGPCVLPN AS PENSION, p.PGPCFCRG
  FROM CRD.PGPC p
 WHERE p.PGPCANNO = 2026 AND p.PGPCMESS = 9
 ORDER BY NVL(p.PGPCVLSG, -1), p.ENTDCDGO;


-- =====================================================================================
-- 3. ⭐ EL ANCLA: el ultimo movimiento de aporte tipo 23 de cada jubilado
-- =====================================================================================
-- resolverAnclaRetroactivo mira los movimientos del tipo de aporte de pension
-- complementaria. Si el ultimo movimiento ya cubre septiembre, el jubilado figura
-- "al dia" y su seguro se fija en 0.
--
-- COMO SE LEE: si la mayoria de los 139 aptos tiene ULTIMO_MOVIMIENTO en septiembre de
-- 2026 o despues, el ancla esta movida y estamos en H46. Si es de agosto o antes, el
-- ancla NO es la causa y hay que mirar el saldo (bloque 4).

SELECT TRUNC(MAX(a.APRTFCMV), 'MM')                                   AS MES_DEL_ULTIMO_MOVIMIENTO,
       COUNT(DISTINCT a.ENTDCDGO)                                     AS JUBILADOS
  FROM CRD.APRT a
 WHERE a.TPAPCDGO = (SELECT MIN(t.TPAPCDGO) FROM CRD.TPAP t
                      WHERE UPPER(t.TPAPNMBR) LIKE '%PENSION%COMPLEMENTARIA%')
 GROUP BY a.ENTDCDGO
 ORDER BY 1 DESC;


-- =====================================================================================
-- 4. El saldo disponible: sin saldo, el seguro se topa en 0 aunque haya ancla
-- =====================================================================================
-- Reparte a los jubilados por tramo de saldo del aporte de pension complementaria.
-- Si casi todos estan en SIN_SALDO, la causa es el saldo y no el ancla.

SELECT CASE WHEN SUM(a.APRTVLMV) IS NULL     THEN 'SIN MOVIMIENTOS'
            WHEN SUM(a.APRTVLMV) <= 0        THEN 'SIN SALDO (<= 0)'
            WHEN SUM(a.APRTVLMV) < 10        THEN 'SALDO < 10'
            ELSE                                  'CON SALDO'
       END                                                            AS TRAMO,
       COUNT(*)                                                       AS JUBILADOS
  FROM CRD.ENTD e
  LEFT JOIN CRD.APRT a
         ON a.ENTDCDGO = e.ENTDCDGO
        AND a.TPAPCDGO = (SELECT MIN(t.TPAPCDGO) FROM CRD.TPAP t
                           WHERE UPPER(t.TPAPNMBR) LIKE '%PENSION%COMPLEMENTARIA%')
 WHERE e.ENTDIDST = (SELECT MIN(d.PDTRALTR) FROM SCP.PDTR d
                      JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
                     WHERE UPPER(d.PDTRNMBR) LIKE '%JUBILADO%COMPLEMENTARIO%')
 GROUP BY e.ENTDCDGO
 ORDER BY 1;


-- =====================================================================================
-- 5. El nominal configurado: de donde sale el $450,40 de la pantalla
-- =====================================================================================
-- Esperado: la suma de los VPPCVLSR activos de los jubilados aptos deberia acercarse a
-- $450,40. Confirma que la previsualizacion suma el NOMINAL y no lo cobrable.

SELECT COUNT(*)                                                       AS CONFIGURACIONES_ACTIVAS,
       SUM(NVL(v.VPPCVLSR, 0))                                        AS SUMA_SEGURO_NOMINAL,
       SUM(CASE WHEN NVL(v.VPPCVLSR, 0) > 0 THEN 1 ELSE 0 END)        AS CON_SEGURO_CONFIGURADO
  FROM CRD.VPPC v
 WHERE v.VPPCIDST = 1;


-- =====================================================================================
-- 6. QUE HACER CON CADA RESULTADO — la decision es del usuario, no del script
-- =====================================================================================
-- A) Bloque 1 con SEGURO_CON_VALOR > 0 y el proceso igual da $0
--    -> el defecto esta en la lectura de U1 (selectByEntidadYPeriodo). Es codigo nuestro
--       y se corrige. AVISAR con la salida del bloque 1.
--
-- B) Bloque 1 con SEGURO_EN_CERO > 0 y bloque 3 mostrando movimientos de 9/2026
--    -> ⛔ H46 CONFIRMADO: los intentos fallidos de hoy movieron el ancla y el seguro
--       quedo fijado en 0. El reintento NO lo va a arreglar nunca. La salida documentada
--       hasta hoy es restaurar la base al punto previo; cualquier otra cosa PIDE DDL y
--       es una decision de negocio. AVISAR ANTES DE VOLVER A INTENTAR.
--
-- C) Bloque 1 con 0 filas y bloque 4 mostrando casi todos SIN SALDO
--    -> no hay defecto: el seguro no se cobra porque los jubilados no tienen saldo de
--       aportes tipo 23 del que descontarlo. Entonces la pantalla es la que enga�a, y lo
--       que hay que corregir es la PREVISUALIZACION, que promete un nominal que el
--       proceso no puede cobrar. Mismo patron que H70.
--
-- D) Bloque 3 con el ultimo movimiento en agosto o antes y bloque 4 CON SALDO
--    -> no encaja con ninguna hipotesis: el proceso deberia haber cobrado. AVISAR con
--       las cinco salidas y se vuelve a diagnosticar desde el codigo.
-- =====================================================================================
