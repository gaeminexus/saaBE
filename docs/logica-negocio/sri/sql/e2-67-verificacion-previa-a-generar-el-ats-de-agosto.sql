-- =====================================================================
-- e2-67 — Verificacion PREVIA a generar el ATS de agosto 2026
--          Los cuatro numeros que hay que mirar antes de apretar Generar
-- Modulo: sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- ✅ SOLO LECTURA. No inserta, no borra, no actualiza, no hace COMMIT.
--    Correr entero. No necesita WAR ni despliegue.
--
-- POR QUE EXISTE — el arbitro verifico contra el codigo, uno por uno, los
-- defectos reportados del ATS, y quedan corregidos TODOS menos uno, que
-- no depende de nosotros: el proveedor con PASAPORTE. Ese caso hace
-- RECHAZAR EL ANEXO ENTERO si aparece, y hoy el generador solo AVISA.
--
--   GeneradorAtsServiceImpl:817-818 — cuando tpIdProv = "03" agrega un
--   aviso y NO emite <tipoProv>/<denoProv>, porque el nombre exacto del
--   elemento no esta confirmado (CATALOGO-ATS.md dice "DenoProv",
--   LEVANTAMIENTO-ATS-103-104.md dice "denopr", y el Catalogo_ATS.xls no
--   desempata: §46.7 del estado). Emitirlo con el nombre equivocado es
--   ESTRICTAMENTE PEOR que no emitirlo.
--
--   El BLOQUE 0 contesta en una linea si agosto tiene ese caso. Si da
--   CERO, el item no bloquea nada y el anexo puede generarse tranquilo.
--
-- Los otros tres bloques son los numeros conocidos contra los que hay que
-- contrastar la salida del DIMM — §35: *contrastar la salida contra un
-- valor conocido*, que es la unica verificacion que este sistema respeta.
--
-- ⚠️ CORRERLO DESPUES del e2-66 (y de su COMMIT). Antes, el BLOQUE 1 va a
--    dar los numeros viejos.
--
-- Columnas verificadas contra las entidades, no de memoria:
--   TSR.TTLR -> Titular: TTLRCDGO, TTLRIDNT, TTLRNMBR, TTLRRZZB
--               (TTLRRZZB = rubroTipoIdentificacionH, HIJO del rubro 36;
--                1 = cedula, 2 = RUC, 3 = PASAPORTE, 4 = del exterior,
--                segun com.saa.rubros.TipoIdentificacion. Es el ID del
--                hijo, NO el codigo alterno — leer el padre ya rompio el
--                archivo del banco una vez, §40.4/§42 del estado)
--   PGS.FCTC -> FacturaCompra: ID, NUMERO, FECHA, SUBTOTAL, SUBCERO,
--               SUBNOOBJ, SUBEXENT, VIVA, ESTADO, TITULAR, FCTCESIN
--   PGS.RCV2 -> RetencionCompraV2: ID, NUMERO, FECHA, ESTADO, PROVEEDOR
--   PGS.DRC2 -> DetalleRetencionCompraV2: RETENCIONV2, CODIMPUESTO,
--               VALORRETEN, BASEIMPONIBLE, TIPODOCRETEN, NUMDOCRETEN,
--               ESTADO
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — ⛔ EL UNICO QUE PUEDE HACER RECHAZAR EL ANEXO.
--            Compras de agosto cuyo proveedor tiene PASAPORTE.
-- ESPERADO: **CERO FILAS**.
--   Si da cero -> el item del §46.7 no afecta este periodo. Generar.
--   Si da filas -> el anexo va a salir SIN <tipoProv>/<denoProv> para
--     esos proveedores y el SRI puede rechazarlo. En ese caso NO generar
--     todavia: avisar al arbitro, que necesita el XSD oficial del ATS (lo
--     instala el validador del DIMM) o un ATS ya AUTORIZADO que tenga un
--     proveedor con pasaporte, para copiar el nombre del elemento tal cual.
--   El generador ademas emite su propio aviso con nombre e identificacion.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - proveedor con PASAPORTE (debe dar CERO)' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRIDNT AS identificacion, t.TTLRNMBR AS proveedor,
       t.TTLRRZZB AS tipo_identificacion_hijo,
       NVL(f.SUBTOTAL,0) AS subtotal
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND NVL(t.TTLRRZZB,-1) = 3
   AND ( f.FCTCESIN IS NULL OR f.FCTCESIN <> 1 )   -- las de intermediario no van al ATS
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — Las bases de compra que tiene que declarar agosto.
-- ESPERADO despues del e2-66 CON su COMMIT:
--     no_objeto = 477,12   ·   exento = 0,39
--   y NINGUNA factura con base gravada > 0 y IVA = 0 en el BLOQUE 1b.
--   Ese par —gravada sin IVA— es el sintoma que se vio en el DIMM y el
--   que este frente entero vino a corregir.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 1 - totales de compra de agosto' AS bloque,
       COUNT(*)               AS facturas,
       SUM(NVL(f.SUBTOTAL,0)) AS subtotal,
       SUM(NVL(f.SUBCERO,0))  AS base_0,
       SUM(NVL(f.SUBNOOBJ,0)) AS no_objeto,
       SUM(NVL(f.SUBEXENT,0)) AS exento,
       SUM(NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0)) AS base_gravada,
       SUM(NVL(f.VIVA,0))     AS iva
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ( f.FCTCESIN IS NULL OR f.FCTCESIN <> 1 );


-- BLOQUE 1b — El sintoma, factura por factura. ESPERADO: CERO FILAS.
-- Cada fila es una compra que declara base GRAVADA sin un centavo de IVA.
SELECT 'BLOQUE 1b - gravada sin IVA (debe dar CERO)' AS bloque,
       f.ID, f.NUMERO, t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal, NVL(f.SUBCERO,0) AS base_0,
       NVL(f.SUBNOOBJ,0) AS no_objeto, NVL(f.SUBEXENT,0) AS exento,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) AS base_gravada,
       NVL(f.VIVA,0) AS iva
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND ( f.FCTCESIN IS NULL OR f.FCTCESIN <> 1 )
   AND NVL(f.VIVA,0) = 0
   AND ( NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) - NVL(f.SUBEXENT,0) ) > 0.005
 ORDER BY f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Las retenciones que NOS hicieron, que es el talon que salia
--            en 0,00 hasta el 2026-09-21 (§46.2, corregido en 1aeed9ed y
--            ya desplegado).
-- ESPERADO: dos filas con valores > 0 (renta y/o IVA). El total de cada
--   una tiene que coincidir con "RESUMEN DE RETENCIONES QUE LE EFECTUARON
--   EN EL PERIODO" del talon del DIMM.
--   Si da CERO filas y el contador dice que si nos retuvieron, el problema
--   NO es el generador: es que las retenciones no estan cargadas en CXP.
-- ⚠️ Se filtra por la fecha de la RETENCION (RCV2.FECHA), que es lo que
--   hace el generador (:1344). Si el contador las declara en el mes de la
--   VENTA, este numero y el del anexo cambian — decision abierta, §46.8.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - retenciones que nos hicieron' AS bloque,
       d.CODIMPUESTO AS cod_impuesto,
       CASE d.CODIMPUESTO WHEN '1' THEN 'RENTA' WHEN '2' THEN 'IVA' ELSE 'OTRO' END AS que_es,
       COUNT(DISTINCT r.ID)          AS retenciones,
       COUNT(*)                      AS lineas,
       SUM(NVL(d.BASEIMPONIBLE,0))   AS base,
       SUM(NVL(d.VALORRETEN,0))      AS valor_retenido
  FROM PGS.DRC2 d
  JOIN PGS.RCV2 r ON r.ID = d.RETENCIONV2
 WHERE NVL(r.ESTADO,1) = 1
   AND NVL(d.ESTADO,1) = 1
   AND r.FECHA >= DATE '2026-08-01' AND r.FECHA < DATE '2026-09-01'
 GROUP BY d.CODIMPUESTO
 ORDER BY d.CODIMPUESTO;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — Las facturas marcadas como intermediario, que NO deben
--            aparecer en el anexo ni en el cuadre 104.
-- ESPERADO: la 343 (001-001-000002868, R&M WORLDTRAVEL), marcada por el
--   e2-64. Si aparece alguna mas que no reconozcas, mirala antes de
--   generar: cada una es plata que sale del anexo.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 3 - intermediario, fuera del ATS' AS bloque,
       f.ID, f.NUMERO, TO_CHAR(f.FECHA,'YYYY-MM-DD') AS fecha,
       t.TTLRNMBR AS proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal, f.FCTCESIN AS es_intermediario
  FROM PGS.FCTC f
  LEFT JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND f.FECHA >= DATE '2026-08-01' AND f.FECHA < DATE '2026-09-01'
   AND f.FCTCESIN = 1
 ORDER BY f.ID;
