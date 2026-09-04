-- =====================================================================================
-- 192 - Por que el padron marca "Falta" cuenta y certificado en TODOS los jubilados
-- FECHA: 2026-09-04 · EQUIPO: CRD / Equipo B (eqB, omen-saa-1)
--
-- SOLO SELECT. No modifica una sola fila. Se puede correr en horario laboral.
--
-- Sin comandos de SQL*Plus: no usa PROMPT, DEFINE, SET ni &variables.
--
-- SINTOMA REPORTADO (2026-09-04): en la pestana Padron, las columnas "Cuenta" y
-- "Certificado" salen "Falta" para TODOS los jubilados, sin excepcion.
--
-- QUE DETERMINA CADA COSA, verificado contra el codigo el 2026-09-04:
--
--   CUENTA BANCARIA -> tabla CRD.CNBP, una fila por cuenta del participe.
--     "Tiene cuenta" = existe EXACTAMENTE UNA fila con ENTDCDGO = participe y
--     CNBPIDST = 1 (Estado.ACTIVO). Cero o mas de una: el backend falla a proposito
--     con SIN_CUENTA_BANCARIA (PagoPensionComplementariaServiceImpl.unicaCuentaActiva).
--
--   CERTIFICADO -> (!) NO ES UNA COLUMNA DE CNBP. Es un adjunto aparte:
--     CRD.ADJN con ADJNIDRF = el CODIGO DE LA CUENTA (CNBPCDGO), NO el del participe,
--     TPDJCDGO = el tipo llamado 'CERTIFICADO BANCARIO' y ADJNIDST = 1.
--
--   (!!) Y EL TIPO SE BUSCA POR NOMBRE, NO POR UN ID FIJO:
--     CuentaBancariaParticipeServiceImpl.resolverTipoCertificadoBancario() hace
--     selectByNombre('CERTIFICADO BANCARIO') sobre CRD.TPDJ, y si no lo encuentra
--     LANZA EXCEPCION. Esa fila la crea CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql,
--     que hay que correr EN CADA AMBIENTE. Si no se corrio en produccion, TODOS los
--     certificados fallan por igual - que es exactamente el sintoma.
--
--   (!!!) Y ESO EXPLICARIA TAMBIEN LA COLUMNA "CUENTA": crear una cuenta bancaria de
--     participe SOLO se puede por POST /rest/cnbp/conCertificado (el POST simple esta
--     bloqueado a proposito desde el 2026-08-25). Ese camino exige subir el PDF, y para
--     eso necesita el tipo de adjunto. Sin la fila de TPDJ, nunca se pudo crear ninguna
--     cuenta -> CNBP vacia -> las DOS columnas en "Falta". Una sola causa raiz.
--
-- EL BLOQUE 1 ES EL QUE DECIDE. Correrlo primero.
-- =====================================================================================

-- ==========================================================================
-- BLOQUE 1 - (!!) LA CAUSA MAS PROBABLE: existe el tipo de adjunto?
-- ==========================================================================

SELECT t.TPDJCDGO                                  AS ID_TIPO,
       t.TPDJNMBR                                  AS NOMBRE,
       t.TPDJIDST                                  AS ESTADO
  FROM CRD.TPDJ t
 WHERE UPPER(TRIM(t.TPDJNMBR)) LIKE '%CERTIFICADO%';

--
-- (!) Si NO devuelve una fila con el nombre EXACTO 'CERTIFICADO BANCARIO', esa es la
--     causa. El backend busca por nombre exacto: 'Certificado Bancario' o
--     'CERTIFICADO BANCARIO ' con espacio al final NO sirven.
-- (!) Remedio: correr docs/logica-negocio/crd/sql/CARGA-TIPO-ADJUNTO-CERTIFICADO-BANCARIO.sql
--     en produccion. Es el script que crea esta fila y esta escrito desde el 2026-08-31.
-- (!) Si la fila existe pero TPDJIDST no es 1, tambien conviene revisarlo.
--

-- ==========================================================================
-- BLOQUE 2 - Hay cuentas bancarias de participes, en general?
-- ==========================================================================

SELECT COUNT(*)                                              AS FILAS_EN_CNBP,
       COUNT(DISTINCT c.ENTDCDGO)                            AS PARTICIPES_CON_CUENTA,
       SUM(CASE WHEN c.CNBPIDST = 1 THEN 1 ELSE 0 END)       AS CUENTAS_ACTIVAS
  FROM CRD.CNBP c;

--
-- (!) FILAS_EN_CNBP = 0 confirma la cadena completa: nunca se pudo crear una cuenta
--     porque el unico camino exige el certificado, y el certificado exige el tipo del
--     bloque 1. No es un defecto de la pantalla: es parametria que falta.
-- (!) Si hay filas pero CUENTAS_ACTIVAS = 0, el problema es otro: las cuentas existen
--     pero estan inactivas.
--

-- ==========================================================================
-- BLOQUE 3 - Los 191 jubilados: cuantos tienen cuenta activa
-- ==========================================================================

SELECT COUNT(*)                                              AS JUBILADOS,
       SUM(CASE WHEN x.CUENTAS_ACTIVAS = 1 THEN 1 ELSE 0 END) AS CON_UNA_CUENTA_OK,
       SUM(CASE WHEN x.CUENTAS_ACTIVAS = 0 THEN 1 ELSE 0 END) AS SIN_CUENTA,
       SUM(CASE WHEN x.CUENTAS_ACTIVAS > 1 THEN 1 ELSE 0 END) AS CON_VARIAS_TAMBIEN_FALLA
  FROM (SELECT e.ENTDCDGO,
               (SELECT COUNT(*) FROM CRD.CNBP c
                 WHERE c.ENTDCDGO = e.ENTDCDGO AND c.CNBPIDST = 1) AS CUENTAS_ACTIVAS
          FROM CRD.ENTD e
         WHERE e.ENTDIDST = 3) x;          -- JUBILADO_COMPLEMENTARIO

--
-- (!) CON_VARIAS_TAMBIEN_FALLA no es un detalle: el backend exige EXACTAMENTE UNA cuenta
--     activa. Dos cuentas activas fallan igual que ninguna, con SIN_CUENTA_BANCARIA, y
--     eso en la pantalla se ve identico a "no tiene". Son dos problemas distintos.
--

-- ==========================================================================
-- BLOQUE 4 - Certificados realmente cargados
-- ==========================================================================

SELECT COUNT(*)                                              AS ADJUNTOS_CERTIFICADO,
       SUM(CASE WHEN a.ADJNIDST = 1 THEN 1 ELSE 0 END)       AS ACTIVOS,
       COUNT(DISTINCT a.ADJNIDRF)                            AS CUENTAS_CON_CERTIFICADO
  FROM CRD.ADJN a
  JOIN CRD.TPDJ t ON t.TPDJCDGO = a.TPDJCDGO
 WHERE UPPER(TRIM(t.TPDJNMBR)) LIKE '%CERTIFICADO%';

--
-- (!) ADJNIDRF es el CODIGO DE LA CUENTA (CRD.CNBP.CNBPCDGO), no el del participe. Si
--     alguien cargo adjuntos apuntando al participe, no los va a encontrar nadie.
--

-- ==========================================================================
-- BLOQUE 5 - El cruce completo, jubilado por jubilado (los primeros 30)
-- ==========================================================================

SELECT e.ENTDNMID                                            AS CEDULA,
       SUBSTR(e.ENTDRZNS,1,40)                               AS NOMBRE,
       (SELECT COUNT(*) FROM CRD.CNBP c
         WHERE c.ENTDCDGO = e.ENTDCDGO AND c.CNBPIDST = 1)   AS CUENTAS_ACTIVAS,
       (SELECT COUNT(*)
          FROM CRD.CNBP c
          JOIN CRD.ADJN a ON a.ADJNIDRF = c.CNBPCDGO AND a.ADJNIDST = 1
          JOIN CRD.TPDJ t ON t.TPDJCDGO = a.TPDJCDGO
         WHERE c.ENTDCDGO = e.ENTDCDGO
           AND UPPER(TRIM(t.TPDJNMBR)) LIKE '%CERTIFICADO%') AS CERTIFICADOS
  FROM CRD.ENTD e
 WHERE e.ENTDIDST = 3
   AND ROWNUM <= 30
 ORDER BY e.ENTDRZNS;

--
-- (!) Si CUENTAS_ACTIVAS y CERTIFICADOS dan 0 en toda la lista, la pantalla NO esta
--     equivocada: esta diciendo la verdad, y lo que falta son los datos.
-- (!) Si dan valores > 0 y la pantalla igual muestra "Falta", entonces si es un defecto
--     del frontend y hay que mirar que id le esta pasando al endpoint
--     GET /rest/cnbp/{id}/certificado - que espera el codigo de la CUENTA, no el del
--     participe. Confundir esos dos ids da 404 para todos por igual.
--

-- =====================================================================================
-- No hay bloque de reverso: este script no escribe nada.
-- =====================================================================================
