-- =====================================================================
-- MEDICION previa al arreglo "la retencion recibida es de un CLIENTE"
-- Modulo: CXP/CNT  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-04
-- Diseno: docs/logica-negocio/cxp/PLAN-RETENCION-RECIBIDA-ES-DE-CLIENTE.md
--
-- QUE HACE
--   NADA. Es 100% SOLO LECTURA. Ningun INSERT, UPDATE, DELETE ni DDL.
--   Seguro de correr de corrido, en local y en produccion.
--
-- PARA QUE
--   Responde las dos preguntas que el diseno no puede cerrar leyendo codigo:
--     A) Cuantas retenciones YA se cargaron tomando la cuenta contable de
--        PROVEEDOR del titular, y cuales de esas ya generaron asiento. Ese es
--        el dano existente y decide si hace falta un ajuste contable.
--     B) Cuantos titulares emisores de retencion NO tienen cuenta bajo rol
--        Cliente. Al exigir el rol correcto, esos se van a empezar a bloquear.
--        AsientoContableServiceImpl:117 ya tenia medido que 61 de 87 titulares
--        con cuenta solo la tienen bajo rol Proveedor, asi que se esperan casos.
--
-- NOMBRES REALES (verificados contra las entidades, no supuestos)
--   PGS.RCV2  RetencionCompraV2   ·  PGS.RTCM  RetencionCompra
--     PROVEEDOR -> TSR.TTLR(TTLRCDGO)   EMPRESA -> SCP.PJRQ   ASIENTO -> CNT.ASNT
--   TSR.PRRL  PersonaRol   PRSNCDGO=titular   PRRLRZZA=rol (1=Cliente, 2=Proveedor)
--   TSR.PRCC  PersonaCuentaContable   PRRLCDGO=rol   PRCCTPOO=tipoCuenta (1=Facturas)
--                                     PJRQCDGO=empresa   PLNNCDGO=plan de cuenta
-- =====================================================================


-- =====================================================================
-- A1 -- Volumen: cuantas retenciones hay cargadas, por version y estado.
--       Si da 0 en las dos tablas, no hay dano existente y el arreglo entra
--       limpio: solo hay que ocuparse de la parte B.
-- =====================================================================
SELECT 'RCV2 (V2)' AS TABLA, r.ESTADO AS ESTADO,
       COUNT(*) AS FILAS,
       SUM(CASE WHEN r.ASIENTO IS NOT NULL THEN 1 ELSE 0 END) AS CON_ASIENTO
  FROM PGS.RCV2 r
 GROUP BY r.ESTADO
UNION ALL
SELECT 'RTCM (V1)', r.ESTADO,
       COUNT(*),
       SUM(CASE WHEN r.ASIENTO IS NOT NULL THEN 1 ELSE 0 END)
  FROM PGS.RTCM r
 GROUP BY r.ESTADO
 ORDER BY 1, 2;


-- =====================================================================
-- A2 -- EL DANO CONCRETO: retenciones CON asiento ya generado.
--       Ese asiento llevo al HABER la cuenta de PROVEEDOR del titular.
--       Segun D1 correspondia la cuenta CxC del mismo titular como CLIENTE.
--       ESPERADO: 0 filas. Cada fila es un asiento a revisar.
-- =====================================================================
SELECT 'RCV2' AS TABLA, r.ID AS ID, r.NUMERO AS NUMERO, r.FECHA AS FECHA,
       r.ASIENTO AS ASIENTO,
       t.TTLRCDGO AS TITULAR, t.TTLRNMBR AS NOMBRE, t.TTLRIDNT AS IDENTIFICACION
  FROM PGS.RCV2 r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
 WHERE r.ASIENTO IS NOT NULL
UNION ALL
SELECT 'RTCM', r.ID, r.NUMERO, r.FECHA,
       r.ASIENTO,
       t.TTLRCDGO, t.TTLRNMBR, t.TTLRIDNT
  FROM PGS.RTCM r
  JOIN TSR.TTLR t ON t.TTLRCDGO = r.PROVEEDOR
 WHERE r.ASIENTO IS NOT NULL
 ORDER BY 1, 4;


-- =====================================================================
-- B1 -- EL IMPACTO DEL ARREGLO: para cada titular que ya emitio una
--       retencion, que cuentas tiene por rol.
--       ESPERADO ideal: CUENTAS_CLIENTE >= 1 en todos.
--       Cada fila con CUENTAS_CLIENTE = 0 es un titular que, despues del
--       arreglo, va a bloquear la carga hasta que se le parametrice la cuenta.
--       NO es un defecto del arreglo: es el dato que faltaba y que hoy se
--       estaba tapando con la cuenta equivocada.
-- =====================================================================
WITH EMISORES AS (
    SELECT DISTINCT PROVEEDOR AS TITULAR, EMPRESA AS EMPRESA FROM PGS.RCV2
    UNION
    SELECT DISTINCT PROVEEDOR,            EMPRESA            FROM PGS.RTCM
)
SELECT  e.TITULAR AS TITULAR,
        t.TTLRIDNT AS IDENTIFICACION,
        t.TTLRNMBR AS NOMBRE,
        e.EMPRESA  AS EMPRESA,
        (SELECT COUNT(*) FROM TSR.PRCC c
           JOIN TSR.PRRL p ON p.PRRLCDGO = c.PRRLCDGO
          WHERE p.PRSNCDGO = e.TITULAR AND p.PRRLRZZA = 1
            AND c.PJRQCDGO = e.EMPRESA  AND c.PRCCTPOO = 1) AS CUENTAS_CLIENTE,
        (SELECT COUNT(*) FROM TSR.PRCC c
           JOIN TSR.PRRL p ON p.PRRLCDGO = c.PRRLCDGO
          WHERE p.PRSNCDGO = e.TITULAR AND p.PRRLRZZA = 2
            AND c.PJRQCDGO = e.EMPRESA  AND c.PRCCTPOO = 1) AS CUENTAS_PROVEEDOR
  FROM EMISORES e
  JOIN TSR.TTLR t ON t.TTLRCDGO = e.TITULAR
 ORDER BY 5, 3;


-- =====================================================================
-- B2 -- El caso puntual que reporto el usuario, para cruzarlo a mano.
--       COOPERATIVA DE AHORRO Y CREDITO CREDIMAS, RUC 1891745687001.
--       Muestra TODOS sus roles y cuentas, sin filtrar por empresa.
-- =====================================================================
SELECT  t.TTLRCDGO AS TITULAR, t.TTLRNMBR AS NOMBRE, t.TTLRIDNT AS RUC,
        p.PRRLCDGO AS ID_ROL,
        p.PRRLRZZA AS ROL,
        CASE p.PRRLRZZA WHEN 1 THEN 'CLIENTE' WHEN 2 THEN 'PROVEEDOR'
             ELSE 'OTRO/NULO' END          AS ROL_NOMBRE,
        p.PRRLESTD AS ESTADO_ROL,
        c.PRCCCDGO AS ID_CUENTA,
        c.PRCCTPOO AS TIPO_CUENTA,
        c.PJRQCDGO AS EMPRESA
  FROM TSR.TTLR t
  LEFT JOIN TSR.PRRL p ON p.PRSNCDGO = t.TTLRCDGO
  LEFT JOIN TSR.PRCC c ON c.PRRLCDGO = p.PRRLCDGO
 WHERE t.TTLRIDNT = '1891745687001'
 ORDER BY 5, 9;


-- =====================================================================
-- COMO LEER EL RESULTADO
--
--   A1/A2 en 0  -> no hay asientos malos. El arreglo entra sin ajuste contable.
--
--   A2 con filas -> cada una es un asiento que acredito la cuenta de proveedor
--                   de un cliente. El ajuste se disena APARTE y lo decide el
--                   usuario; NO se corrige con este script.
--
--   B1 con CUENTAS_CLIENTE = 0 -> esos titulares van a bloquear la carga
--                   despues del arreglo. Hay que parametrizarles la cuenta CxC
--                   en Contabilidad -> Cuentas por Titular, rol Cliente.
--                   Conviene hacerlo ANTES de desplegar para no cambiar un
--                   bloqueo por otro.
-- =====================================================================
