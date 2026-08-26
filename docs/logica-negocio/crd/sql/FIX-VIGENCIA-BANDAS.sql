-- =====================================================================================
-- CORRECCION — VIGENCIA DE LA CARGA INICIAL DE BANDAS (CRD.CBPR)
-- FECHA: 2026-08-25
--
-- PROBLEMA detectado el 2026-08-25 llamando a los endpoints ya desplegados:
--   La carga inicial se grabo con CBPRFCIN = 2026-09-01 (futuro). Como el proceso
--   resuelve "configuracion vigente a la fecha", HOY no hay ninguna vigente:
--     GET /rest/cbpr/listado  -> los 15 productos con porVencer:null y vencido:null
--                                (la pantalla de parametrizacion se ve VACIA)
--     GET /rest/cbpr/clasificar -> "No hay configuracion de bandas vigente al ..."
--   Con ?fecha=2026-09-01 todo responde bien, o sea el motor esta correcto: lo que
--   esta mal es la fecha de inicio de vigencia de los datos.
--
-- IMPACTO ADICIONAL EN FASE 2: el cierre mensual clasifica a la fecha de corte. Un
--   cierre de agosto 2026 (corte 2026-08-31) tampoco encontraria configuracion, y
--   cualquier reproceso de meses anteriores fallaria igual.
--
-- SOLUCION: retrotraer el inicio de vigencia. Las bandas que se cargaron NO son un
--   cambio normativo nuevo: son las que el fondo ya venia usando (las cuentas del plan
--   existen desde antes). La vigencia historica del modelo esta para los cambios
--   FUTUROS de la entidad de control, no para estrenar la parametrizacion actual.
--   Por eso la fecha de inicio debe ser anterior a cualquier mes que se pueda procesar
--   o reprocesar.
--
-- SQL PURO (sin comandos SQL*Plus). Ejecutar por bloques y revisar los controles.
-- =====================================================================================


-- =====================================================================================
-- 1. CONTROL PREVIO — que se va a cambiar (esperado: 28 filas con 2026-09-01)
-- =====================================================================================

SELECT CBPRFCIN, COUNT(*) configuraciones
FROM CRD.CBPR
GROUP BY CBPRFCIN
ORDER BY CBPRFCIN;


-- =====================================================================================
-- 2. CORRECCION
--    Fecha propuesta: 2020-01-01 — suficientemente anterior a cualquier mes que se
--    pueda reprocesar. Ajustar si el negocio prefiere otra (ver el doc de carga).
--    Solo toca las configuraciones de la carga inicial (usuario CARGA-INICIAL-BANDAS)
--    y las que siguen vigentes: no altera ninguna vigencia ya cerrada.
-- =====================================================================================

UPDATE CRD.CBPR
SET CBPRFCIN = TO_DATE('2020-01-01','YYYY-MM-DD'),
    CBPRFCMD = CURRENT_TIMESTAMP,
    CBPRUSMD = 'FIX-VIGENCIA-BANDAS'
WHERE CBPRUSRG = 'CARGA-INICIAL-BANDAS'
  AND CBPRFCIN = TO_DATE('2026-09-01','YYYY-MM-DD')
  AND CBPRFCFN IS NULL;


-- =====================================================================================
-- 3. CONTROLES POSTERIORES
-- =====================================================================================

-- 3.1 Todas las configuraciones vigentes arrancan en 2020-01-01 (esperado: 28)
SELECT CBPRFCIN, COUNT(*) configuraciones
FROM CRD.CBPR
GROUP BY CBPRFCIN
ORDER BY CBPRFCIN;

-- 3.2 Hay configuracion vigente HOY para cada producto parametrizado (esperado: 28)
SELECT COUNT(*) vigentes_hoy
FROM CRD.CBPR
WHERE CBPRESTD = 1
  AND CBPRFCIN <= TRUNC(SYSDATE)
  AND (CBPRFCFN IS NULL OR CBPRFCFN >= TRUNC(SYSDATE));

COMMIT;

-- Verificacion funcional despues del COMMIT (fuera de SQL):
--   GET /SaaBE/rest/cbpr/listado?idEmpresa=1236
--     -> los productos parametrizados ya NO deben venir con porVencer/vencido en null.
--   GET /SaaBE/rest/cbpr/clasificar?idProducto=7&idEmpresa=1236&tipoCartera=2&dias=100
--     -> banda 3, rango 91-270, cuenta 1.3.12.10.
