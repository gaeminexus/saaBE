-- =====================================================================================
-- 239 - ¿Los OCHO jubilados con seguro tienen DOS configuraciones VPPC activas?
-- FECHA: 2026-09-22 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Sin comandos de SQL*Plus. ⭐ NO ESCRIBE NADA. Comentarios ARRIBA, nunca intercalados.
--
-- =====================================================================================
-- DE DONDE SALE ESTA HIPOTESIS — UNA RESTA
-- =====================================================================================
-- Del 237: 190 configuraciones VPPC activas.
-- Del 238: 182 jubilados en el padron (ENTDIDST = 3), y los OCHO con seguro configurado
--          estan TODOS en el padron, TODOS con saldo (de 12.162 a 31.919) y con su
--          ultimo movimiento de aporte tipo 23 en 2026-08-31.
--
--   190 - 182 = 8.   ⭐ Sobran exactamente ocho configuraciones. Y son ocho los que
--                       tienen seguro. La coincidencia es demasiado buena.
--
-- POR QUE ESO EXPLICARIA EL $0, medido en el codigo:
--   generarSeguroIndividual:998 llama a unicaActiva(configuraciones, idEntidad).
--   unicaActiva:2964 LANZA IncomeException si la entidad tiene MAS DE UNA VPPC activa
--   ("no se puede saber cual vale"). Esa excepcion la atrapa el catch del bucle de
--   generarSeguroDelMes, que suma 1 a conError y SIGUE con el siguiente jubilado.
--
--   ⇒ Los ocho unicos que aportan al total lanzan excepcion, nunca se les fija el
--     seguro, no se graba su fila en CRD.PGPC, y el total queda en $0 — sin que el
--     proceso falle. Los otros 174 tienen el seguro en 0 y no cambian nada.
--
--   ⇒ Y explica lo que mas llamaba la atencion del 238: que NO hubiera ni una fila en
--     PGPC para 9/2026. Si los ocho lanzan antes de fijar, no hay nada que grabar.
--
-- ⚠️ ES UNA HIPOTESIS. La confirma o la tumba el bloque 1. Si la tumba, lo que decide
--    es el LOG del proceso, no la base: las lineas "Error al fijar el seguro medico de
--    la entidad X: ..." dicen textualmente por que fallo cada uno.
-- =====================================================================================


-- =====================================================================================
-- 1. ⭐ EL CONTROL QUE DECIDE: jubilados con MAS DE UNA VPPC activa
-- =====================================================================================
-- Esperado si la hipotesis es correcta: 8 filas, y sus ENTDCDGO tienen que ser los
-- mismos ocho del 238 (6510, 6526, 6532, 6540, 6645, 6654, 6687, 6695).

SELECT v.ENTDCDGO,
       COUNT(*)                                                       AS VPPC_ACTIVAS,
       SUM(NVL(v.VPPCVLSR, 0))                                        AS SUMA_SEGURO,
       LISTAGG(v.VPPCCDGO, ', ') WITHIN GROUP (ORDER BY v.VPPCCDGO)   AS CODIGOS_VPPC
  FROM CRD.VPPC v
 WHERE v.VPPCIDST = 1
 GROUP BY v.ENTDCDGO
HAVING COUNT(*) > 1
 ORDER BY v.ENTDCDGO;


-- =====================================================================================
-- 2. El detalle de esas configuraciones, para ver cual es cual
-- =====================================================================================
-- Muestra las dos filas de cada jubilado duplicado, con su valor de pension, su seguro,
-- quien las creo y cuando. La que hay que conservar es la que tiene el seguro real.

SELECT v.ENTDCDGO, v.VPPCCDGO, v.VPPCVLRR AS VALOR_PENSION, v.VPPCVLSR AS SEGURO,
       v.VPPCNMCT AS NUM_CUOTAS, v.VPPCIDST AS ESTADO,
       v.VPPCUSRG AS USUARIO_REGISTRO, v.VPPCFCRG AS FECHA_REGISTRO
  FROM CRD.VPPC v
 WHERE v.VPPCIDST = 1
   AND v.ENTDCDGO IN (SELECT d.ENTDCDGO FROM CRD.VPPC d
                       WHERE d.VPPCIDST = 1
                       GROUP BY d.ENTDCDGO HAVING COUNT(*) > 1)
 ORDER BY v.ENTDCDGO, v.VPPCCDGO;


-- =====================================================================================
-- 3. Control de contexto: el reparto completo
-- =====================================================================================
-- Esperado: 182 jubilados en el padron; si la hipotesis vale, 174 con una sola VPPC
-- activa y 8 con dos.

SELECT COUNT(*)                                                       AS JUBILADOS_PADRON,
       SUM(CASE WHEN c.ACTIVAS = 0 THEN 1 ELSE 0 END)                 AS SIN_VPPC_ACTIVA,
       SUM(CASE WHEN c.ACTIVAS = 1 THEN 1 ELSE 0 END)                 AS CON_UNA,
       SUM(CASE WHEN c.ACTIVAS > 1 THEN 1 ELSE 0 END)                 AS CON_MAS_DE_UNA
  FROM (SELECT e.ENTDCDGO,
               (SELECT COUNT(*) FROM CRD.VPPC v
                 WHERE v.ENTDCDGO = e.ENTDCDGO AND v.VPPCIDST = 1)    AS ACTIVAS
          FROM CRD.ENTD e
         WHERE e.ENTDIDST = 3) c;


-- =====================================================================================
-- 4. SI LA HIPOTESIS SE CONFIRMA — que sigue, y que NO hay que hacer todavia
-- =====================================================================================
-- La correccion de datos es desactivar la VPPC sobrante de cada uno de los ocho,
-- dejando ACTIVA la que tiene el seguro y el valor de pension correctos.
--
-- ⛔ PERO ESO NO SE DECIDE MIRANDO ESTE SCRIPT. El bloque 2 muestra las dos filas de
--    cada jubilado: cual de las dos es la buena es una decision de negocio (puede que la
--    segunda sea una correccion mas nueva y legitima, o un duplicado por error). Hay que
--    verlas con el usuario, jubilado por jubilado, antes de tocar una sola fila.
--
-- ⚠️ Y HAY UNA SEGUNDA PARTE, QUE ES DE CODIGO Y NO DE DATOS:
--    limpiar los datos hace que el seguro de ESTE mes se cobre, pero no impide que
--    vuelva a pasar. Hoy nada bloquea crear una segunda VPPC activa para el mismo
--    jubilado — es el MISMO patron que el CERTIFICADO BANCARIO duplicado de esta semana
--    (H74): un catalogo o una configuracion que el codigo exige unica, sin nada en la
--    base que lo impida. Ahi tambien la salida fue un indice unico parcial.
--    ⇒ Propuesta a decidir: UNIQUE sobre (ENTDCDGO) para las VPPC activas, con el mismo
--      truco del CASE que se uso en UX_TPDJ_NOMBRE_ACTIVO.
--
-- ⚠️ Y UNA TERCERA, LA PEOR DE LAS TRES:
--    el bucle atrapa la excepcion, suma a conError y SIGUE. Ocho jubilados que no
--    cobraron su seguro no detuvieron nada ni avisaron en la pantalla: el proceso
--    informo "$0" como si fuera un resultado normal. Un error por jubilado no puede
--    quedar solo en un contador que nadie mira.
-- =====================================================================================
