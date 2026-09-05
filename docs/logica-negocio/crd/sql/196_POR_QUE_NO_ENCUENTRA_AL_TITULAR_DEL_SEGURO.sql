-- =====================================================================================
-- 196 - Por que la corrida dice que NO existe el titular del seguro, si en pantalla SI esta
-- FECHA: 2026-09-05 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila.
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- =====================================================================================
-- EL SINTOMA
-- =====================================================================================
-- El prevuelo muestra:
--   PROVEEDOR_SEGURO_NO_ENCONTRADO: no existe un titular activo con RUC '1768153530001'
--
-- Pero la pantalla de titulares SI lo muestra:
--   ID 156 - Empresa Publica de Hidrocarburos del Ecuador EP PETROECUADOR
--   Identificacion 1768153530001 - Activo
--
-- =====================================================================================
-- LA CONSULTA QUE CORRE EL CODIGO (TitularDaoServiceImpl.selectByIdentificacion)
-- =====================================================================================
--   SELECT t FROM Titular t
--    WHERE t.identificacion = :identificacion    -- columna TTLRIDNT
--      AND t.estado         = :estado            -- columna TTLRESTD, se le pasa 1
--
-- El parametro se manda con .trim(), pero la COLUMNA no se trimea. Entonces, con el
-- registro existiendo, solo quedan dos causas posibles:
--
--   A. TTLRESTD no vale 1. La pantalla puede estar mostrando "Activo" a partir de otro
--      campo o de otro catalogo, y no de esta columna. Es la trampa que el CLAUDE.md ya
--      documenta para otras tablas: dos columnas que parecen de estado y solo una vale.
--
--   B. TTLRIDNT tiene espacios (adelante o atras). En Oracle, con VARCHAR2, '1768...001 '
--      NO es igual a '1768...001', y el = falla sin ningun error.
--
-- El bloque 1 distingue las dos en una sola mirada.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - (!) EL QUE DECIDE: como esta guardado realmente el registro
-- ==========================================================================
-- Los delimitadores > < hacen visibles los espacios que de otro modo no se ven.

SELECT t.TTLRCDGO                                          AS ID,
       '>' || t.TTLRIDNT || '<'                            AS RUC_CON_DELIMITADORES,
       LENGTH(t.TTLRIDNT)                                  AS LARGO_RUC,
       t.TTLRESTD                                          AS ESTADO_CRUDO,
       CASE WHEN t.TTLRESTD = 1 THEN 'SI' ELSE 'NO' END    AS PASA_FILTRO_ESTADO,
       CASE WHEN t.TTLRIDNT = '1768153530001' THEN 'SI'
            ELSE 'NO' END                                  AS PASA_FILTRO_RUC,
       SUBSTR(t.TTLRRZSC, 1, 45)                           AS RAZON_SOCIAL,
       t.TTLRPRVD                                          AS ES_PROVEEDOR
  FROM TSR.TTLR t
 WHERE TRIM(t.TTLRIDNT) = '1768153530001';

--
-- COMO SE LEE, y contesta sola:
-- (!) LARGO_RUC distinto de 13, o espacios visibles entre > y <  -> es la causa B.
-- (!) ESTADO_CRUDO distinto de 1                                 -> es la causa A.
-- (!) Las dos columnas PASA_FILTRO en SI y aun asi falla         -> avisar: el problema
--     no esta en el dato y hay que mirar a que base apunta el despliegue.
-- (!) Ninguna fila                                               -> el titular NO esta en
--     ESTA base. La pantalla estaria leyendo de otra. Avisar.
--

-- ==========================================================================
-- BLOQUE 2 - Hay mas de un titular con ese RUC?
-- ==========================================================================
-- selectByIdentificacion usa setMaxResults(1) y no puede revelar un duplicado por si
-- mismo. Si hubiera dos, podria estar tomando el inactivo y descartando el bueno.

SELECT COUNT(*)                                            AS FILAS_CON_ESE_RUC,
       SUM(CASE WHEN t.TTLRESTD = 1 THEN 1 ELSE 0 END)     AS ACTIVAS
  FROM TSR.TTLR t
 WHERE TRIM(t.TTLRIDNT) = '1768153530001';

--
-- (!) Se espera 1 y 1. Si FILAS_CON_ESE_RUC es mayor que 1, hay duplicado y el
--     setMaxResults(1) esta eligiendo en silencio - avisar antes de correr nada.
--

-- ==========================================================================
-- BLOQUE 3 - La cuenta bancaria del proveedor (hace falta si o si)
-- ==========================================================================
-- Sin cuenta de destino la orden nace muerta: tesoreria NO puede asignarla al aprobar
-- (verificado por omen-saa-2: las seis llamadas a setCuentaDestino estan fuera de
-- aprobar, y el endpoint no recibe ese parametro). O nace con cuenta, o no sirve.

SELECT c.CTBNCDGO                                          AS ID_CUENTA,
       c.CTBNNMCT                                          AS NUMERO_CUENTA,
       c.CTBNESTD                                          AS ESTADO,
       c.BEXTCDGO                                          AS ID_BANCO,
       c.CTBNTPCT                                          AS TIPO_CUENTA
  FROM TSR.CTBN c
  JOIN TSR.TTLR t ON t.TTLRCDGO = c.TTLRCDGO
 WHERE TRIM(t.TTLRIDNT) = '1768153530001';

--
-- (!) Tiene que devolver AL MENOS UNA fila activa, con numero, banco y tipo de cuenta.
--     El formateador del archivo del banco usa los tres, no solo el numero.
-- (!) Si devuelve MAS DE UNA activa, avisar: elegir la primera es el mismo defecto del
--     get(0) sin ORDER BY que ya mordio hoy con los dos CERTIFICADO BANCARIO de CRD.TPDJ.
-- (!) Si no devuelve ninguna, hay que cargarle la cuenta al proveedor antes de correr.
--
-- Tabla TSR.CTBN (CuentaBancariaTitular), verificada contra el modelo JPA antes de
-- entregar este script. Si aun asi fallara, el bloque 1 y el 2 igual sirven: son los
-- que contestan la pregunta urgente.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================
