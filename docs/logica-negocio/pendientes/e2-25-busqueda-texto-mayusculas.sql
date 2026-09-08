-- =====================================================================
-- e2-25 — ¿Las búsquedas de texto nuevas van a encontrar algo?
-- Modulo: transversal  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-08
--
-- ✅ SOLO LECTURA. No inserta, no borra, no hace COMMIT. Correr entero.
--
-- 🔴 POR QUE EXISTE
--   El lote 2 movio seis pantallas de consulta a filtrar en el servidor. Todas
--   las busquedas de texto pasan por el LIKE del buscador generico
--   (EntityDaoImpl.selectByCriteria), y ahi hay una asimetria medida en el
--   codigo:
--
--     query.setParameter(campo, "%" + valor.toUpperCase() + "%");
--
--   Mayusculiza el PARAMETRO, pero NO envuelve la columna en upper(). O sea que
--   la comparacion real es:  COLUMNA LIKE '%TEXTO EN MAYUSCULAS%'
--
--   Si el dato guardado NO esta en mayusculas, eso no encuentra nada. Y no da
--   error: devuelve cero filas, que se ve igual que "no hay resultados".
--
--   El mecanismo para arreglarlo YA EXISTE: la marca `truncado` de DatosBusqueda
--   antepone el comando TRUNCADO del catalogo justo antes de (b.campo) --por eso
--   la columna va entre parentesis en el JPQL generado--. Pero se verifico por
--   grep que NINGUNA pantalla del sistema la usa hoy.
--
--   Este script responde las dos preguntas que hacen falta antes de decidir:
--   que dice el catalogo, y como estan guardados los datos de verdad.
-- =====================================================================


-- =====================================================================
-- 1 — Que contiene el comando TRUNCADO del catalogo
-- =====================================================================
-- Rubro TIPO_COMANDOS_BUSQUEDA, detalle con codigo alterno 8
-- (TipoComandosBusqueda.TRUNCADO = 8).
--
-- 🔴 ESTO DECIDE EL ARREGLO:
--   - Si el valor es 'upper' (o 'UPPER'), el mecanismo ya sirve: alcanza con
--     marcar `truncado` en los criterios de texto y la columna se compara en
--     mayusculas. Arreglo barato y sin tocar el buscador generico.
--   - Si es otra cosa, o si no devuelve ninguna fila, ese camino NO sirve y hay
--     que resolverlo de otra forma. Avisar antes de tocar nada.
SELECT '1 - comando TRUNCADO' AS control,
       r.PRBRALTR AS rubro_alterno, d.PDTRALTR AS detalle_alterno,
       d.PDTRDSCR AS descripcion, d.PDTRVLRV AS valor_que_se_inyecta
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE r.PRBRALTR = (SELECT PRBRALTR FROM SCP.PRBR WHERE PRBRCDGO = d.PRBRCDGO)
   AND d.PDTRALTR = 8
   AND r.PRBRDSCR LIKE '%COMANDO%';

-- 1b. Si el 1 no devuelve nada, este muestra TODOS los comandos de busqueda para
--     ubicar el rubro correcto y ver como estan escritos los demas operadores.
SELECT '1b - todos los comandos de busqueda' AS control,
       r.PRBRALTR AS rubro_alterno, r.PRBRDSCR AS rubro,
       d.PDTRALTR AS alterno, d.PDTRDSCR AS descripcion, d.PDTRVLRV AS valor
  FROM SCP.PDTR d JOIN SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
 WHERE UPPER(r.PRBRDSCR) LIKE '%COMANDO%'
 ORDER BY d.PDTRALTR;


-- =====================================================================
-- 2 — Como estan guardados los datos de verdad
-- =====================================================================
-- Por cada columna de texto que las pantallas nuevas van a buscar, cuenta
-- cuantas filas estan en mayusculas y cuantas no.
--
-- LECTURA:
--   - "no_mayusculas" en 0  -> el dato esta todo en mayusculas y las busquedas
--                              funcionan tal como quedaron. No hay nada que hacer.
--   - "no_mayusculas" > 0   -> 🔴 esas filas NO se van a encontrar al buscarlas
--                              por texto. Hay que arreglar el buscador.
--
-- Nota: las tablas CBR.FCTR y CBR.ANTC no siguen la convencion de 8 caracteres
-- del resto del sistema; sus columnas van sin prefijo, verificado en las
-- entidades el 2026-09-08.

SELECT '2 - CBR.ANTC titular' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN t.PJRQNMBR = UPPER(t.PJRQNMBR) THEN 0 ELSE 1 END) AS no_mayusculas
  FROM CBR.ANTC a JOIN SCP.PJRQ t ON t.PJRQCDGO = a.TITULAR;

SELECT '2 - CBR.FCTR comprador' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN t.PJRQNMBR = UPPER(t.PJRQNMBR) THEN 0 ELSE 1 END) AS no_mayusculas
  FROM CBR.FCTR f JOIN SCP.PJRQ t ON t.PJRQCDGO = f.COMPRADOR;

SELECT '2 - TSR.EGRS descripcion' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN EGRSDSCR = UPPER(EGRSDSCR) THEN 0 ELSE 1 END) AS no_mayusculas
  FROM TSR.EGRS
 WHERE EGRSDSCR IS NOT NULL;

SELECT '2 - SCP.PJRQ nombre (titulares en general)' AS control,
       COUNT(*) AS total,
       SUM(CASE WHEN PJRQNMBR = UPPER(PJRQNMBR) THEN 0 ELSE 1 END) AS no_mayusculas
  FROM SCP.PJRQ
 WHERE PJRQNMBR IS NOT NULL;


-- =====================================================================
-- 3 — Una muestra, para verlo con los ojos
-- =====================================================================
-- Diez nombres de titular tal como estan guardados. Si salen en "Mayúscula
-- Inicial" o en minusculas, el problema del punto 2 es real y se ve solo.
SELECT '3 - muestra de nombres' AS control, PJRQCDGO, PJRQNMBR
  FROM (SELECT PJRQCDGO, PJRQNMBR FROM SCP.PJRQ
         WHERE PJRQNMBR IS NOT NULL ORDER BY PJRQCDGO DESC)
 WHERE ROWNUM <= 10;


-- =====================================================================
-- QUE SIGUE, segun lo que devuelva
-- =====================================================================
-- CASO A — Todo en mayusculas (punto 2 con no_mayusculas = 0 en todas).
--   No hay nada que hacer: las seis pantallas del lote 2 funcionan como estan.
--
-- CASO B — Hay datos en minusculas Y el punto 1 devuelve 'upper'.
--   Arreglo barato: marcar `truncado` en los criterios de texto del frontend.
--   No se toca el buscador generico, que lo usa todo el sistema.
--
-- CASO C — Hay datos en minusculas y el punto 1 NO devuelve 'upper'.
--   Hay que decidir entre agregar el upper() a EntityDaoImpl.selectByCriteria
--   --que afecta a TODO el sistema y haria que busquedas existentes empiecen a
--   encontrar mas cosas, lo cual suele ser correcto pero es un cambio de
--   comportamiento global-- o corregir el catalogo. Se decide con el resultado
--   a la vista, no antes.
-- =====================================================================
