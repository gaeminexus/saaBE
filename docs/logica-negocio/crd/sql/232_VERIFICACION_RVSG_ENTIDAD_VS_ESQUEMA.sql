-- =====================================================================================
-- VERIFICACION ENTIDAD-VS-ESQUEMA de CRD.RVSG + la configuracion del 231
-- FECHA: 2026-09-21 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- ⛔ SOLO LECTURA. No crea, no borra, no modifica nada. Es seguro correrlo de corrido.
--
-- PARA QUE: Hibernate incluye TODA columna @Column en el SELECT que genera. Una sola
-- columna mapeada que no exista en la tabla -- o con otro nombre -- rompe CUALQUIER
-- lectura de la entidad con ORA-00904, y eso NO se ve al compilar: aparece la primera vez
-- que un operador abre la pantalla. La tabla se acaba de crear con 22 columnas a mano.
--
-- CUANDO: despues de correr 230 y 231, ANTES de que alguien registre el primer caso.
--
-- QUE HACER CON LA SALIDA: los bloques 1, 2 y 3 tienen que dar TODO 'OK'. Cualquier
-- 'FALTA' o 'SOBRA' hay que resolverlo antes de usar la pantalla.
-- =====================================================================================


-- =====================================================================================
-- 1. LAS 22 COLUMNAS DE LA ENTIDAD EXISTEN EN LA TABLA
-- =====================================================================================
-- Esperado: 22 filas, TODAS con RESULTADO = 'OK'.
-- Una fila 'FALTA EN LA BASE' = ORA-00904 garantizado al abrir la pantalla.

WITH esperadas AS (
    SELECT 'RVSGCDGO' AS COLUMNA FROM DUAL UNION ALL
    SELECT 'ENTDCDGO' FROM DUAL UNION ALL
    SELECT 'TPAPCDGO' FROM DUAL UNION ALL
    SELECT 'RVSGESTD' FROM DUAL UNION ALL
    SELECT 'CNBCCDGO' FROM DUAL UNION ALL
    SELECT 'RVSGVLRR' FROM DUAL UNION ALL
    SELECT 'RVSGFCHA' FROM DUAL UNION ALL
    SELECT 'RVSGRFRN' FROM DUAL UNION ALL
    SELECT 'RVSGRTRS' FROM DUAL UNION ALL
    SELECT 'RVSGOBSR' FROM DUAL UNION ALL
    SELECT 'ASNTCDGO' FROM DUAL UNION ALL
    SELECT 'APRTCDGO' FROM DUAL UNION ALL
    SELECT 'RVSGUSRG' FROM DUAL UNION ALL
    SELECT 'RVSGFCRG' FROM DUAL UNION ALL
    SELECT 'RVSGUSAP' FROM DUAL UNION ALL
    SELECT 'RVSGFCAP' FROM DUAL UNION ALL
    SELECT 'RVSGUSRC' FROM DUAL UNION ALL
    SELECT 'RVSGFCRC' FROM DUAL UNION ALL
    SELECT 'RVSGMTRC' FROM DUAL UNION ALL
    SELECT 'RVSGUSAN' FROM DUAL UNION ALL
    SELECT 'RVSGFCAN' FROM DUAL UNION ALL
    SELECT 'RVSGMTAN' FROM DUAL
)
SELECT e.COLUMNA,
       CASE WHEN c.COLUMN_NAME IS NULL THEN '*** FALTA EN LA BASE ***' ELSE 'OK' END AS RESULTADO,
       c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE
FROM   esperadas e
LEFT   JOIN ALL_TAB_COLUMNS c
       ON c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG' AND c.COLUMN_NAME = e.COLUMNA
ORDER  BY RESULTADO DESC, e.COLUMNA;


-- 1.2 El camino inverso: columnas en la tabla que la entidad NO mapea.
--     Esperado: 0 filas. Una columna de mas no rompe las lecturas, pero si es NOT NULL
--     rompe TODOS los INSERT -- que es exactamente lo que paso con VPPCVLSR en H68.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE,
       CASE WHEN c.NULLABLE = 'N' THEN '*** NOT NULL Y SIN MAPEAR: ROMPE TODO INSERT ***'
            ELSE 'sobra, pero no rompe' END AS RIESGO
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG'
AND    c.COLUMN_NAME NOT IN ('RVSGCDGO','ENTDCDGO','TPAPCDGO','RVSGESTD','CNBCCDGO',
                             'RVSGVLRR','RVSGFCHA','RVSGRFRN','RVSGRTRS','RVSGOBSR',
                             'ASNTCDGO','APRTCDGO','RVSGUSRG','RVSGFCRG','RVSGUSAP',
                             'RVSGFCAP','RVSGUSRC','RVSGFCRC','RVSGMTRC','RVSGUSAN',
                             'RVSGFCAN','RVSGMTAN')
ORDER  BY c.COLUMN_ID;


-- 1.3 ⚠️ La obligatoriedad real, que el mapeo JPA NO dice (leccion de H68: VPPCVLSR no
--     declaraba nullable=false y lo era). Esperado: NULLABLE = 'N' SOLO en estas nueve:
--     RVSGCDGO, ENTDCDGO, TPAPCDGO, RVSGESTD, CNBCCDGO, RVSGVLRR, RVSGFCHA, RVSGUSRG,
--     RVSGFCRG. Si alguna otra salio NOT NULL, el flujo va a reventar con ORA-01400 en
--     cuanto se registre sin ese dato -- tipicamente los campos de aprobacion o anulacion,
--     que nacen vacios a proposito.
SELECT c.COLUMN_NAME, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG' AND c.NULLABLE = 'N'
ORDER  BY c.COLUMN_NAME;


-- =====================================================================================
-- 2. LO QUE EL CODIGO NECESITA ADEMAS DE LAS COLUMNAS
-- =====================================================================================

-- 2.1 La secuencia. Esperado: 1 fila. Sin ella, todo INSERT falla.
SELECT 'SQ_RVSGCDGO' AS OBJETO,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE '*** FALTA ***' END AS RESULTADO
FROM   ALL_SEQUENCES s
WHERE  s.SEQUENCE_OWNER = 'CRD' AND s.SEQUENCE_NAME = 'SQ_RVSGCDGO';

-- 2.2 El indice unico de referencia. Esperado: 1 fila con 'OK'.
--     Si falta, registrar dos veces el mismo deposito mete el valor DOS VECES en el saldo
--     del participe y nada lo impide.
SELECT 'UX_RVSG_REFERENCIA' AS OBJETO,
       CASE WHEN COUNT(*) = 1 THEN 'OK' ELSE '*** FALTA: se puede duplicar un deposito ***' END AS RESULTADO
FROM   ALL_INDEXES i
WHERE  i.TABLE_OWNER = 'CRD' AND i.INDEX_NAME = 'UX_RVSG_REFERENCIA' AND i.UNIQUENESS = 'UNIQUE';

-- 2.3 Constraints. Esperado: PK_RVSG, CK_RVSG_ESTADO, CK_RVSG_VALOR, y las FK que se
--     hayan podido crear. Las dos que cruzan de esquema (FK_RVSG_CNBC, FK_RVSG_ASNT)
--     pueden faltar legitimamente si no habia GRANT: eso NO es un problema.
SELECT c.CONSTRAINT_NAME, c.CONSTRAINT_TYPE, c.STATUS
FROM   ALL_CONSTRAINTS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'RVSG'
ORDER  BY c.CONSTRAINT_TYPE, c.CONSTRAINT_NAME;


-- =====================================================================================
-- 3. LA CONFIGURACION DEL 231 — sin esto la pantalla registra pero NO se puede aprobar
-- =====================================================================================

-- 3.1 ⭐ EL DATO QUE HAY QUE ANOTAR: el tipo de aporte, con el NOMBRE EXACTO que la
--     pantalla busca. Esperado: 1 fila, ESTADO = 1.
--     ⚠️ Si el nombre no es exactamente este, el frontend no lo va a preseleccionar:
--     lo resuelve por nombre, no por codigo.
SELECT t.TPAPCDGO AS ID_TIPO_APORTE, t.TPAPNMBR AS NOMBRE, t.TPAPIDST AS ESTADO,
       CASE WHEN t.TPAPIDST = 1 THEN 'OK' ELSE '*** NO VIGENTE: el codigo lo rechaza ***' END AS RESULTADO
FROM   CRD.TPAP t
WHERE  t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS';

-- 3.2 Su cuenta contable. Esperado: 1 fila con CUENTA_PASIVO = 2.3.90.90.11.
--     Si no sale nada, aprobar responde 409 nombrando el tipo y la empresa.
SELECT c.CTAPCDGO, c.PJRQCDGO AS EMPRESA,
       np.PLNNCNTA AS CUENTA_PASIVO, np.PLNNNMBR AS NOMBRE_CUENTA, c.CTAPESTD AS ESTADO
FROM   CRD.CTAP c
JOIN   CRD.TPAP t  ON t.TPAPCDGO  = c.TPAPCDGO
JOIN   CNT.PLNN np ON np.PLNNCDGO = c.CTAPPLNP
WHERE  t.TPAPNMBR = 'VALOR DE SEGURO POR ENTREGAR A BENEFICIARIOS';

-- 3.3 ⚠️ La contabilidad de CRD tiene que estar ACTIVA o `aprobar` responde 409 a
--     proposito (no se deja entrar dinero al saldo sin asiento).
--
--     ⛔ CORREGIDO 2026-09-21: la primera version de este bloque consultaba "SCP.RUBR" y
--     daba ORA-00942. Las tablas son SCP.PRBR (rubro) y SCP.PDTR (detalle) — el codigo de
--     4 letras es PRBR, no RUBR — y la busqueda NO es por nombre sino por CODIGO ALTERNO,
--     que es como la lee el sistema: DetalleRubroDaoServiceImpl:268 filtra por
--     t.rubro.codigoAlterno (PRBRALTR = 237, Rubros.CRD_PARAMETROS_CONTABILIDAD) y
--     t.codigoAlterno (PDTRALTR = 1, CrdParametroContabilidad.CONTABILIDAD_ACTIVA), y
--     devuelve valorNumerico (PDTRVLRN).
--
--     Esperado: 1 fila con VALOR_NUMERICO = 1.
--
--     ⚠️ CERO FILAS TAMBIEN SIGNIFICA APAGADA, y es facil de leer mal: el DAO usa
--     getSingleResult(), asi que si la fila no existe lanza NoResultException y
--     ConfiguracionContabilidadServiceImpl:61-67 lo atrapa y devuelve FALSE a proposito
--     ("apagado es el lado seguro"). Lo mismo si hubiera mas de una fila. Es decir: si
--     este bloque no devuelve exactamente una fila con 1, `aprobar` va a responder 409
--     aunque nadie haya apagado nada.
SELECT r.PRBRCDGO, r.PRBRDSCR AS RUBRO, r.PRBRALTR AS ALTERNO_RUBRO,
       d.PDTRCDGO, d.PDTRDSCR AS DETALLE, d.PDTRALTR AS ALTERNO_DETALLE,
       d.PDTRVLRN AS VALOR_NUMERICO, d.PDTRESTD AS ESTADO,
       CASE WHEN d.PDTRVLRN = 1 THEN 'OK - contabilidad ACTIVA'
            ELSE '*** APAGADA: aprobar va a responder 409 ***' END AS RESULTADO
FROM   SCP.PDTR d
JOIN   SCP.PRBR r ON r.PRBRCDGO = d.PRBRCDGO
WHERE  r.PRBRALTR = 237
AND    d.PDTRALTR = 1;


-- =====================================================================================
-- 4. LAS CUENTAS ASOPREP QUE VAN A APARECER EN EL COMBO DE LA PANTALLA
-- =====================================================================================
-- La pantalla reusa el bloque de respaldo de los cobros, que SOLO lista cuentas con la
-- marca de cobro de credito. Si el deposito del sepelio entro a una cuenta que NO esta
-- en esta lista, el operador no la va a poder elegir.
--
-- Revisar que la(s) cuenta(s) por donde entro el dinero de los 4 casos esten aca. Si
-- falta alguna, es un dato a corregir (marcarla), no un cambio de codigo.
--
-- Ademas, cada una TIENE que tener cuenta contable y empresa: de ahi sale la empresa del
-- asiento. Una cuenta sin PLNNCDGO hace fallar el registro con un mensaje claro.
SELECT b.CNBCCDGO, b.CNBCNMRO AS NUMERO, b.PLNNCDGO AS CUENTA_CONTABLE,
       CASE WHEN b.PLNNCDGO IS NULL
            THEN '*** SIN CUENTA CONTABLE: no se puede registrar por esta cuenta ***'
            ELSE 'OK' END AS RESULTADO
FROM   TSR.CNBC b
WHERE  NVL(b.CNBCCBCR, 0) = 1
ORDER  BY b.CNBCCDGO;
