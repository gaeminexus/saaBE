-- =====================================================================================
-- CRD.USAP - columnas para el "olvide mi clave" de la app movil (codigo al correo)
-- FECHA: 2026-10-21
-- EQUIPO: app movil ASOPREP (app1). Escrito por el arbitro omen-app-1-arb.
-- DISENO: docs/logica-negocio/crd/DISENO-RECUPERACION-CLAVE-CORREO-APP-MOVIL.md
--
-- QUE HACE: agrega 6 columnas NULLABLE a CRD.USAP. No toca ninguna columna existente, no
-- borra nada y no actualiza filas. Las filas que ya existen quedan con todo en NULL, que
-- significa "sin codigo vigente y sin envios": exactamente el estado inicial correcto.
--
-- EL CODIGO NUNCA SE GUARDA EN CLARO. USAPCRHS guarda un hash PBKDF2 con el MISMO formato
-- que USAPCLVE (iteraciones$saltBase64$hashBase64). Por eso tambien 256 caracteres.
--
-- POR QUE COLUMNAS EN USAP Y NO UNA TABLA APARTE: hay como maximo UN codigo vigente por
-- cuenta (pedir otro reemplaza al anterior), y la relacion es 1:1 con la credencial. Una
-- tabla aparte agregaria un nombre de 4 letras al registro de reservas sin ganar nada.
--
-- No consume ningun PRBR/PDTR. CRD es el esquema de omen-saa-1: se le avisa.
--
-- Orden de ejecucion: 0 (controles previos) -> 1 (ALTER) -> 2 (controles posteriores).
-- Reverso al final, comentado.
-- =====================================================================================


-- =====================================================================================
-- 0. CONTROLES PREVIOS - si alguno no da lo esperado, NO seguir
-- =====================================================================================

-- 0.1 La tabla existe en CRD. Esperado: 1 fila.
SELECT t.OWNER, t.TABLE_NAME
FROM   ALL_TABLES t
WHERE  t.OWNER = 'CRD' AND t.TABLE_NAME = 'USAP';

-- 0.2 Ninguna de las 6 columnas existe todavia. Esperado: 0 filas.
--     Si devuelve filas, el script ya se corrio (total o parcialmente): revisar antes de seguir.
SELECT c.COLUMN_NAME
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'USAP'
AND    c.COLUMN_NAME IN ('USAPCRHS','USAPCRVN','USAPCRIN','USAPCRUE','USAPCRVE','USAPCRNE');


-- =====================================================================================
-- 1. LAS COLUMNAS
-- =====================================================================================

ALTER TABLE CRD.USAP ADD (
    USAPCRHS VARCHAR2(256),   -- hash PBKDF2 del codigo vigente; NULL = sin codigo
    USAPCRVN TIMESTAMP,       -- vencimiento del codigo vigente
    USAPCRIN NUMBER,          -- intentos fallidos sobre el codigo vigente
    USAPCRUE TIMESTAMP,       -- hora del ultimo envio (freno de 60 segundos)
    USAPCRVE TIMESTAMP,       -- inicio de la ventana de 24 h de envios
    USAPCRNE NUMBER           -- envios dentro de esa ventana (maximo 5)
);

COMMENT ON COLUMN CRD.USAP.USAPCRHS IS 'Hash PBKDF2 del codigo de recuperacion de clave vigente (mismo formato que USAPCLVE). NULL = sin codigo. NUNCA se expone por REST.';
COMMENT ON COLUMN CRD.USAP.USAPCRVN IS 'Vencimiento del codigo de recuperacion vigente (envio + 15 minutos)';
COMMENT ON COLUMN CRD.USAP.USAPCRIN IS 'Intentos fallidos sobre el codigo de recuperacion vigente (5 = se anula el codigo)';
COMMENT ON COLUMN CRD.USAP.USAPCRUE IS 'Fecha/hora del ultimo envio de codigo de recuperacion (no se reenvia antes de 60 s)';
COMMENT ON COLUMN CRD.USAP.USAPCRVE IS 'Inicio de la ventana de 24 h para contar envios de codigo de recuperacion';
COMMENT ON COLUMN CRD.USAP.USAPCRNE IS 'Envios de codigo de recuperacion dentro de la ventana de 24 h (maximo 5)';


-- =====================================================================================
-- 2. CONTROLES POSTERIORES
-- =====================================================================================

-- 2.1 Las 6 columnas existen con su tipo. Esperado: 6 filas, todas NULLABLE = 'Y'.
SELECT c.COLUMN_NAME, c.DATA_TYPE, c.DATA_LENGTH, c.NULLABLE
FROM   ALL_TAB_COLUMNS c
WHERE  c.OWNER = 'CRD' AND c.TABLE_NAME = 'USAP'
AND    c.COLUMN_NAME IN ('USAPCRHS','USAPCRVN','USAPCRIN','USAPCRUE','USAPCRVE','USAPCRNE')
ORDER  BY c.COLUMN_NAME;

-- 2.2 Ninguna fila existente quedo con datos en las columnas nuevas. Esperado: 0.
SELECT COUNT(*) AS FILAS_CON_DATOS
FROM   CRD.USAP u
WHERE  u.USAPCRHS IS NOT NULL OR u.USAPCRVN IS NOT NULL OR u.USAPCRIN IS NOT NULL
OR     u.USAPCRUE IS NOT NULL OR u.USAPCRVE IS NOT NULL OR u.USAPCRNE IS NOT NULL;


-- =====================================================================================
-- REVERSO - SOLO si hay que deshacer. Antes de correrlo, retirar del WAR el codigo que
-- usa estas columnas: la entidad UsuarioApp las mapea, y sin ellas TODO acceso a USAP
-- falla (login, pantalla de oficina), no solo la recuperacion.
-- =====================================================================================
-- ALTER TABLE CRD.USAP DROP (USAPCRHS, USAPCRVN, USAPCRIN, USAPCRUE, USAPCRVE, USAPCRNE);
