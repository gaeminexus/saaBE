-- =====================================================================================
-- 193 - CRD.TPDJ tiene DOS 'CERTIFICADO BANCARIO' y el backend elige uno en silencio
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- ⛔ ESTE SCRIPT SI ESCRIBE, pero solo en el paso 3, que esta COMENTADO. Los pasos 1 y 2
-- son SELECT y hay que correrlos y LEERLOS antes de descomentar nada.
--
-- =====================================================================================
-- EL HALLAZGO (medido el 2026-09-04, bloque 1 del sql/192)
-- =====================================================================================
-- CRD.TPDJ devolvio DOS filas activas con el mismo nombre:
--
--     4   CERTIFICADO BANCARIO   1
--    37   CERTIFICADO BANCARIO   1
--
-- Y el backend las resuelve asi (CuentaBancariaParticipeServiceImpl:249-257):
--
--     List<TipoAdjunto> tipos = tipoAdjuntoDaoService.selectByNombre('CERTIFICADO BANCARIO');
--     if (tipos.isEmpty()) { throw ... }
--     return tipos.get(0);          // <-- toma el PRIMERO, sin mirar cuantos hay
--
-- Y la consulta del DAO (TipoAdjuntoDaoServiceImpl:25-31) NO TIENE ORDER BY:
--
--     select t from TipoAdjunto t where UPPER(t.nombre) = UPPER(:nombre) and t.estado = 1
--
-- =====================================================================================
-- POR QUE ESTO EXPLICA EL SINTOMA, Y POR QUE ES PEOR QUE UN CATALOGO FALTANTE
-- =====================================================================================
-- Los certificados en CRD.ADJN se guardaron con UNO de los dos TPDJCDGO. Cuando la
-- pantalla pregunta, el backend resuelve el tipo con get(0) y busca los adjuntos de ESE.
-- Si le toca el otro, no encuentra nada: "Falta" para TODOS, sin ningun error.
--
-- (!!) Y SIN ORDER BY, CUAL SALE PRIMERO NO ESTA GARANTIZADO. Oracle puede devolverlos
-- en otro orden si cambia el plan, las estadisticas o el almacenamiento. O sea que esto
-- puede "arreglarse solo" un dia y volver a romperse otro, sin que nadie toque nada.
-- Un defecto intermitente es mas caro de diagnosticar que uno permanente.
--
-- COMO SE LLEGO ACA: CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql tiene un control previo
-- que dice, textual, "esperado: 0 filas. Si devuelve algo, NO correr el INSERT - ya
-- existe". El 4 ya estaba; el INSERT se corrio igual y creo el 37. El control estaba
-- bien escrito: lo que fallo fue leerlo.
-- =====================================================================================

-- ==========================================================================
-- PASO 1 - (!) DECIDE TODO: cual de los dos usan los adjuntos REALES
-- ==========================================================================

SELECT a.TPDJCDGO                                            AS TIPO_USADO,
       COUNT(*)                                              AS ADJUNTOS,
       SUM(CASE WHEN a.ADJNIDST = 1 THEN 1 ELSE 0 END)       AS ACTIVOS,
       COUNT(DISTINCT a.ADJNIDRF)                            AS CUENTAS_DISTINTAS,
       MIN(a.ADJNFCRG)                                       AS DESDE,
       MAX(a.ADJNFCRG)                                       AS HASTA
  FROM CRD.ADJN a
 WHERE a.TPDJCDGO IN (4, 37)
 GROUP BY a.TPDJCDGO
 ORDER BY a.TPDJCDGO;

--
-- COMO SE LEE:
-- (!) UNA sola fila -> ese TPDJCDGO es el bueno. El otro es el duplicado a desactivar.
-- (!) DOS filas -> hay certificados repartidos entre los dos tipos. NO se desactiva
--     ninguno todavia: primero hay que reapuntar los del duplicado al bueno, y eso es
--     otro script y otra decision. AVISAR.
-- (!) NINGUNA fila -> no hay ni un solo certificado cargado en la base. Entonces el
--     duplicado NO es la causa del sintoma: simplemente nunca se subio ninguno, y lo
--     que falta son los documentos. Igual conviene desactivar el duplicado (paso 3)
--     para que no muerda mas adelante, pero eso no va a hacer aparecer certificados.
--

-- ==========================================================================
-- PASO 2 - Cuantas cuentas hay, para saber si el paso 1 tiene sentido
-- ==========================================================================

SELECT COUNT(*)                                              AS CUENTAS_CNBP,
       SUM(CASE WHEN c.CNBPIDST = 1 THEN 1 ELSE 0 END)       AS ACTIVAS
  FROM CRD.CNBP c;

--
-- (!) Si CUENTAS_CNBP = 0, no puede haber certificados (un certificado cuelga de una
--     cuenta), y el paso 1 va a dar vacio por eso, no por el duplicado.
--

-- ==========================================================================
-- PASO 3 - (!) LA CORRECCION. COMENTADA. No descomentar sin leer el paso 1.
-- ==========================================================================
-- Desactiva el tipo DUPLICADO para que selectByNombre devuelva UNA sola fila y get(0)
-- deje de ser una loteria. No borra nada: TPDJIDST = 0 es reversible con un UPDATE
-- inverso, y los adjuntos que apunten a ese tipo NO se tocan ni se pierden.
--
-- ⛔ REEMPLAZAR <<TIPO_A_DESACTIVAR>> por el que el PASO 1 mostro SIN adjuntos.
--    Si el paso 1 no devolvio ninguna fila, desactivar el 37 (el que creo el INSERT de
--    mas) y conservar el 4, que es el original del catalogo.
--
-- UPDATE CRD.TPDJ
--    SET TPDJIDST = 0
--  WHERE TPDJCDGO = <<TIPO_A_DESACTIVAR>>
--    AND UPPER(TRIM(TPDJNMBR)) = 'CERTIFICADO BANCARIO';
--
-- COMMIT;

-- ==========================================================================
-- PASO 4 - CONTROL POSTERIOR. Correr DESPUES del paso 3.
-- ==========================================================================

SELECT COUNT(*)                                              AS ACTIVOS_CON_ESE_NOMBRE
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) = 'CERTIFICADO BANCARIO'
   AND t.TPDJIDST = 1;

--
-- (!) TIENE QUE DAR 1. Ni 0 (el backend lanza ERR_TIPO_ADJUNTO_NO_CONFIGURADO y nadie
--     puede cargar una cuenta bancaria nueva) ni 2 (seguimos en la loteria).
--

-- ==========================================================================
-- PASO 5 - REVERSO, comentado
-- ==========================================================================
-- Si algo salio mal, volver a activar el que se desactivo:
--
-- UPDATE CRD.TPDJ SET TPDJIDST = 1 WHERE TPDJCDGO = <<TIPO_A_DESACTIVAR>>;
-- COMMIT;
--
-- No hace falta tocar CRD.ADJN en ningun caso: este script no modifica un solo adjunto.

-- =====================================================================================
-- PENDIENTE APARTE, DE CODIGO Y NO DE DATOS:
-- resolverTipoCertificadoBancario() hace get(0) sobre una lista que puede traer N. Que
-- el dato quede limpio hoy no impide que alguien vuelva a insertar un duplicado manana.
-- El backend deberia FALLAR RUIDOSO con 2 o mas, igual que ya hace unicaCuentaActiva y
-- unicaActiva con las cuentas y las VPPC - el patron correcto ya existe en el modulo.
-- =====================================================================================
