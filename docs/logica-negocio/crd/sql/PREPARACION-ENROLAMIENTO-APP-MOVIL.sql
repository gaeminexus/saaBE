-- =====================================================================================
-- PREPARACION DEL ENROLAMIENTO - app movil ASOPREP (CRD.USAP)
-- =====================================================================================
-- Equipo: app movil ASOPREP (omen-app-1). Escrito por el arbitro el 2026-09-07.
--
-- QUE ES ESTO. La prueba de punta a punta de la app necesita UN participe que pueda
-- loguearse, y hoy no hay ninguno enrolado: CRD.USAP esta vacia. Este script prepara ese
-- enrolamiento y lo verifica despues. Es de CONSULTA salvo el bloque 5, que es el reverso
-- y esta comentado.
--
-- ==========  EL ENROLAMIENTO NO SE HACE POR INSERT. LEER ESTO ANTES DE INTENTARLO.  ==========
--
-- USAPCLVE guarda un hash PBKDF2WithHmacSHA256 con SALT ALEATORIO por usuario, en el formato
-- iteraciones$saltBase64$hashBase64 (100.000 iteraciones, salt de 16 bytes, clave de 256 bits).
-- Ver UsuarioAppServiceImpl.generarHash(). Oracle no puede producir ese valor con SQL puro, y
-- un INSERT a mano deja una fila que NO valida nunca: el login falla con "credenciales
-- invalidas" y el sintoma apunta al borde o a la app, que es donde NO esta el problema.
--
--   -> El alta va SIEMPRE por POST /SaaBE/rest/usap/crear (bloque 3). Es intranet, nunca el borde.
--
-- =====================================================================================


-- -------------------------------------------------------------------------------------
-- 1. ELEGIR UN PARTICIPE DE PRUEBA QUE SIRVA
-- -------------------------------------------------------------------------------------
-- No sirve cualquiera. Para que la prueba recorra las pantallas de la app hace falta uno
-- que TENGA datos: prestamos y aportes. Un participe sin movimientos loguea bien y despues
-- muestra todo vacio, y ahi no se distingue "anda y no tiene datos" de "no anda".
--
-- Devuelve candidatos ordenados por cuanto tienen para mostrar.

SELECT e.ENTDCDGO,
       e.ENTDNMID                        AS IDENTIFICACION,
       e.ENTDRZNS                        AS NOMBRE,
       (SELECT COUNT(*) FROM CRD.PRST p
         WHERE p.ENTDCDGO = e.ENTDCDGO)  AS PRESTAMOS,
       (SELECT COUNT(*) FROM CRD.PRST p
         WHERE p.ENTDCDGO = e.ENTDCDGO
           AND (p.PRSTIDST IS NULL OR p.PRSTIDST IN (2, 8, 10, 11))) AS PRESTAMOS_VIGENTES,
       (SELECT COUNT(*) FROM CRD.APRT a
         WHERE a.ENTDCDGO = e.ENTDCDGO)  AS APORTES
FROM   CRD.ENTD e
WHERE  e.ENTDNMID IS NOT NULL
  AND  LENGTH(TRIM(e.ENTDNMID)) = 10                 -- cedula ecuatoriana
  AND  TRIM(e.ENTDNMID) = TRANSLATE(TRIM(e.ENTDNMID), 'X0123456789', 'X0123456789')
  AND  EXISTS (SELECT 1 FROM CRD.PRST p WHERE p.ENTDCDGO = e.ENTDCDGO)
  AND  EXISTS (SELECT 1 FROM CRD.APRT a WHERE a.ENTDCDGO = e.ENTDCDGO)
ORDER  BY PRESTAMOS_VIGENTES DESC, APORTES DESC
FETCH FIRST 20 ROWS ONLY;

-- Los estados 2, 8, 10 y 11 (VIGENTE, DE_PLAZO_VENCIDO, VIGENTE_POR_REVISAR, EN_MORA) son los
-- que la app agrupa como "vigentes"; el catalogo completo esta en CONTRATO-INTRANET-MOVIL.md.
-- PRSTIDST NULL entra a proposito: no es un estado terminal y el JPQL de
-- PrestamoDaoServiceImpl.selectVigentesByEntidad lo incluye. Si se lo excluye aca, el candidato
-- elegido puede mostrar en la app mas prestamos de los que este script conto.
--
-- ATENCION: PRSTIDST es el estado vigente. ESPSCDGO NO -- es la FK al catalogo CRD.ESPS.
-- Trampa documentada en CLAUDE.md; filtrar por la columna equivocada da resultados vacios.


-- -------------------------------------------------------------------------------------
-- 2. COMPROBAR QUE EL ELEGIDO NO ESTE YA ENROLADO
-- -------------------------------------------------------------------------------------
-- crear() rechaza con "ya tiene una credencial de app registrada" si la entidad ya tiene fila.
-- Reemplazar el literal por la cedula elegida en el bloque 1.

SELECT e.ENTDCDGO,
       e.ENTDNMID          AS IDENTIFICACION,
       e.ENTDRZNS          AS NOMBRE,
       u.USAPCDGO,
       u.USAPESTD          AS ESTADO,       -- 1 ACTIVO, 2 BLOQUEADO, 3 ELIMINADO
       u.USAPDCCL          AS DEBE_CAMBIAR_CLAVE,
       CASE WHEN u.USAPCDGO IS NULL THEN 'LIBRE - se puede enrolar'
            ELSE 'YA ENROLADO - crear() lo va a rechazar' END AS VEREDICTO
FROM   CRD.ENTD e
LEFT   JOIN CRD.USAP u ON u.ENTDCDGO = e.ENTDCDGO
WHERE  TRIM(e.ENTDNMID) = '&&CEDULA_PRUEBA';


-- -------------------------------------------------------------------------------------
-- 3. EL ALTA -- por REST, no por SQL
-- -------------------------------------------------------------------------------------
-- Contra el SaaBE de la INTRANET (el .4 o el local). NUNCA contra el borde: crear,
-- resetearClave y reactivar son de oficina y no se exponen a internet jamas.
--
--   POST http://localhost:8080/SaaBE/rest/usap/crear
--   Content-Type: application/json
--
--   { "identificacion": "<la cedula del bloque 1>",
--     "claveTemporal":  "<clave temporal>",
--     "usuario":        "enrolamiento-prueba" }
--
-- El campo "usuario" queda en USAPUSAR y NO es adorno: es quien responde por el alta. Poner
-- algo que identifique a una persona real cuando esto se haga en produccion.
--
-- La fila nace con USAPESTD = 1 (ACTIVO) y USAPDCCL = 1 (debe cambiar clave), asi que el
-- primer login de la app va a exigir el cambio. Eso es lo correcto y ademas prueba ese flujo.


-- -------------------------------------------------------------------------------------
-- 4. VERIFICAR QUE EL ALTA QUEDO BIEN
-- -------------------------------------------------------------------------------------
-- Correr DESPUES del POST. Un 200 no alcanza como prueba: mirar la fila.

SELECT u.USAPCDGO,
       u.ENTDCDGO,
       u.USAPIDNT           AS IDENTIFICACION,
       u.USAPESTD           AS ESTADO,
       u.USAPINTF           AS INTENTOS_FALLIDOS,
       u.USAPBLHS           AS BLOQUEADO_HASTA,
       u.USAPDCCL           AS DEBE_CAMBIAR_CLAVE,
       u.USAPFCRG           AS FECHA_CREACION,
       u.USAPUSAR           AS USUARIO_REGISTRO,
       CASE WHEN u.USAPCLVE LIKE '100000$%$%' THEN 'HASH PBKDF2 OK'
            ELSE '** REVISAR: el formato del hash no es el esperado **' END AS FORMATO_HASH,
       LENGTH(u.USAPCLVE)   AS LARGO_HASH,
       e.ENTDNMID           AS CEDULA_EN_ENTD,
       CASE WHEN TRIM(e.ENTDNMID) = TRIM(u.USAPIDNT) THEN 'COINCIDE'
            ELSE '** REVISAR: USAPIDNT no coincide con ENTDNMID **' END AS COHERENCIA
FROM   CRD.USAP u
JOIN   CRD.ENTD e ON e.ENTDCDGO = u.ENTDCDGO
WHERE  TRIM(u.USAPIDNT) = '&&CEDULA_PRUEBA';

-- NUNCA se muestra USAPCLVE completo, ni siquiera en una prueba: solo su formato y su largo.
-- Es un hash, no una clave, pero no hay ninguna razon para pasearlo por una pantalla o un log.


-- -------------------------------------------------------------------------------------
-- 5. REVERSO -- borrar el usuario de prueba. COMENTADO A PROPOSITO.
-- -------------------------------------------------------------------------------------
-- Descomentar solo para limpiar un enrolamiento de PRUEBA. En produccion no se borran
-- credenciales: se usa desactivar (el participe) o resetearClave / reactivar (la oficina).
--
-- DELETE FROM CRD.USAP WHERE TRIM(USAPIDNT) = '&&CEDULA_PRUEBA';
-- COMMIT;


-- -------------------------------------------------------------------------------------
-- 6. CENSO PARA EL RIESGO #1 DEL PLAN -- cuantos participes se podrian enrolar
-- -------------------------------------------------------------------------------------
-- El riesgo #1 es que los participes que hoy usan la app de Azure no tengan fila en CRD.ENTD,
-- o la tengan con la cedula desalineada: no podrian loguearse nunca y se descubriria el dia
-- del lanzamiento. Este bloque caracteriza el lado del SAA.
--
-- OJO CON LO QUE ESTE BLOQUE NO PUEDE RESPONDER: no existe aca la lista de usuarios de Azure.
-- Esto dice cuantos participes del SAA son enrolables, NO cuantos usuarios de la app actual
-- van a poder entrar. El cruce real necesita que el cliente entregue el padron de Azure.

SELECT COUNT(*)                                                          AS TOTAL_ENTIDADES,
       COUNT(CASE WHEN e.ENTDNMID IS NULL
                   OR TRIM(e.ENTDNMID) IS NULL THEN 1 END)               AS SIN_IDENTIFICACION,
       COUNT(CASE WHEN LENGTH(TRIM(e.ENTDNMID)) = 10 THEN 1 END)         AS CEDULA_10,
       COUNT(CASE WHEN LENGTH(TRIM(e.ENTDNMID)) NOT IN (10, 13) THEN 1 END) AS LARGO_RARO,
       COUNT(CASE WHEN LENGTH(TRIM(e.ENTDNMID)) > 20 THEN 1 END)         AS NO_ENTRA_EN_USAPIDNT
FROM   CRD.ENTD e;

-- USAPIDNT es VARCHAR2(20): si NO_ENTRA_EN_USAPIDNT devuelve algo distinto de 0, esos
-- participes no se pueden enrolar sin ampliar la columna.

-- Identificaciones DUPLICADAS. USAPIDNT es UNIQUE, asi que dos participes con la misma cedula
-- significan que el segundo no se va a poder enrolar y que el primero podria ver datos del
-- otro. Si devuelve filas, es una decision del usuario antes de enrolar a nadie.

SELECT TRIM(e.ENTDNMID)                     AS IDENTIFICACION,
       COUNT(*)                             AS VECES,
       LISTAGG(e.ENTDCDGO, ', ')
         WITHIN GROUP (ORDER BY e.ENTDCDGO) AS ENTIDADES
FROM   CRD.ENTD e
WHERE  e.ENTDNMID IS NOT NULL
  AND  TRIM(e.ENTDNMID) IS NOT NULL
GROUP  BY TRIM(e.ENTDNMID)
HAVING COUNT(*) > 1
ORDER  BY VECES DESC;
