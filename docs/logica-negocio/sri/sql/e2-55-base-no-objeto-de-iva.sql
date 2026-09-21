-- =====================================================================
-- e2-55 — Base NO OBJETO DE IVA: la contribucion a bomberos/basura de las
--          planillas electricas deja de declararse como tarifa 0%
-- Modulo: cxp / sri  ·  Equipo: omen-saa-2  ·  Fecha: 2026-09-21
--
-- DDL + correccion de datos. VA ANTES DEL WAR: la entidad FacturaCompra
--    mapea la columna nueva, y sin ella toda lectura de PGS.FCTC muere con
--    ORA-00904 (gestion de documentos, bandeja, carga, pagos, ATS).
--
-- ⛔⛔ EL COMMIT DEL FINAL NO ES OPCIONAL ⛔⛔
--    Correr el script no guarda nada. El e2-52 se perdio exactamente asi el
--    2026-09-21: el UPDATE corrio, informo las filas, y desaparecio al
--    cerrar la sesion. Ver §46.1 del estado del equipo.
--    (El ALTER TABLE del BLOQUE 1 si es autocommit en Oracle, como todo DDL.
--     Los UPDATE del BLOQUE 3 NO.)
--
-- POR QUE EXISTE — criterio del auditor interno Roberto Guachamin, por
-- correo del 2026-09-19, trasladado por el usuario el 2026-09-21:
--
--   "La contribucion a bomberos pagada en las facturas de consumo electrico,
--    si bien no forman parte de la factura, al momento de cancelar su valor
--    incrementa debido a dicha contribucion, esta es considerada como NO
--    OBJETO DE IVA."
--   Y los valores detallados en la factura son tarifa 0%.
--
-- QUE HACE HOY EL SISTEMA (medido en el codigo, 2026-09-21):
--   ProcesoCargaDocumentosServiceImpl:1752-1754 suma los valores de terceros
--   al SUBTOTAL, al SUBCERO y al TOTAL. O sea que el bomberos entra en la
--   base 0%. Y GeneradorAtsServiceImpl:819 escribe baseNoGraIva = "0.00"
--   FIJO: hoy el ATS no tiene de donde sacar la base no objeto.
--
--   En la captura del DIMM del 2026-09-21 se ve al derecho: la columna
--   "Base No Objeto IVA" da 0.00 en las 8 compras de la electrica, y el
--   7.23 de bomberos viajaba en "Base IVA 0%".
--
-- COMO SE IDENTIFICA EL VALOR DE TERCEROS EN LO YA CARGADO:
--   La carga les crea una LINEA DE DETALLE propia (`:1726-1741`) con la
--   descripcion del campoAdicional del XML, y solo toma los conceptos cuyo
--   nombre contiene BOMBERO o BASURA (`leerValoresTerceros:4421`). Esa
--   descripcion es el identificador, y es lo que usa el BLOQUE 0.
--
-- INVARIANTE QUE ESTABLECE ESTE CAMBIO:
--   SUBTOTAL = total sin impuestos, TODO incluido (no cambia)
--   SUBNOOBJ = base no objeto de IVA  (columna nueva, default 0)
--   SUBCERO  = base tarifa 0%, SIN incluir la no objeto
--   gravada  = SUBTOTAL - SUBCERO - SUBNOOBJ   (lo calcula el ATS)
--   Con SUBNOOBJ = 0 el comportamiento es identico al de hoy: es aditivo.
--
-- Columnas copiadas de las entidades: FacturaCompra (PGS.FCTC: ID, NUMERO,
--   FECHA, SUBTOTAL, SUBCERO, VIVA, TOTAL, ESTADO, TITULAR, OBSERVACION),
--   DetalleFacturaCompra (PGS.DFCC: FACTURA, DESCRIPCION, BASEIMPONIBLE,
--   CODIGOIVASRI).
-- =====================================================================


-- ---------------------------------------------------------------------
-- BLOQUE 0 — SOLO LECTURA. Que facturas tienen valores de terceros y
--            cuanto suman. ANOTAR ESTA SALIDA: es el respaldo del reverso.
-- ESPERADO: las planillas electricas del periodo, con TERCEROS > 0.
--   Para agosto 2026 y el proveedor 1790053881001 tienen que aparecer las
--   ocho/diez de la captura, con TERCEROS = 7.23 cada una.
--   Si alguna da TERCEROS >= SUBTOTAL, avisar: no se toca (BLOQUE 3).
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 0 - facturas con valores de terceros' AS bloque,
       f.ID, f.NUMERO, f.FECHA, t.TTLRIDNT AS ruc_proveedor,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBCERO,0)  AS base_0_hoy,
       (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID
           AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
             OR UPPER(d.DESCRIPCION) LIKE '%BASURA%')) AS terceros,
       NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
         WHERE d.FACTURA = f.ID
           AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
             OR UPPER(d.DESCRIPCION) LIKE '%BASURA%')) AS base_0_nueva,
       NVL(f.VIVA,0) AS iva_cabecera
  FROM PGS.FCTC f
  JOIN TSR.TTLR t ON t.TTLRCDGO = f.TITULAR
 WHERE f.ESTADO = 1
   AND EXISTS (SELECT 1 FROM PGS.DFCC d
                WHERE d.FACTURA = f.ID
                  AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                    OR UPPER(d.DESCRIPCION) LIKE '%BASURA%'))
 ORDER BY f.FECHA, f.ID;


-- ---------------------------------------------------------------------
-- BLOQUE 1 — DDL. La columna nueva. Aditiva y con default 0: un WAR viejo
--            la ignora y sigue andando igual.
-- ESPERADO: "Table altered". Si dice que la columna ya existe, saltear.
-- ---------------------------------------------------------------------
ALTER TABLE PGS.FCTC ADD (SUBNOOBJ NUMBER DEFAULT 0);

COMMENT ON COLUMN PGS.FCTC.SUBNOOBJ IS
  'Base NO OBJETO DE IVA (ATS baseNoGraIva). Contribucion a bomberos/basura de las planillas electricas. Criterio del auditor interno, correo 2026-09-19. No esta incluida en SUBCERO; si en SUBTOTAL y en TOTAL.';


-- ---------------------------------------------------------------------
-- BLOQUE 2 — Control: la columna quedo creada y en 0 en todas las filas.
-- ESPERADO: una sola fila, CON_VALOR = 0.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 2 - columna creada' AS bloque,
       COUNT(*) AS filas_total,
       COUNT(CASE WHEN NVL(SUBNOOBJ,0) <> 0 THEN 1 END) AS con_valor
  FROM PGS.FCTC;


-- ---------------------------------------------------------------------
-- BLOQUE 3 — UPDATE. Lo ya cargado: el valor de terceros sale de la base
--            0% y pasa a la no objeto. El SUBTOTAL y el TOTAL no se tocan:
--            el dinero que se le debe al proveedor es el mismo.
-- ESPERADO: tantas filas como listo el BLOQUE 0 (salvo las que tengan
--           TERCEROS >= SUBCERO, que quedan afuera por la guarda de abajo).
-- ---------------------------------------------------------------------
UPDATE PGS.FCTC f
   SET f.SUBNOOBJ = (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                      WHERE d.FACTURA = f.ID
                        AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                          OR UPPER(d.DESCRIPCION) LIKE '%BASURA%')),
       f.SUBCERO  = NVL(f.SUBCERO,0) - (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                      WHERE d.FACTURA = f.ID
                        AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                          OR UPPER(d.DESCRIPCION) LIKE '%BASURA%'))
 WHERE f.ESTADO = 1
   AND EXISTS (SELECT 1 FROM PGS.DFCC d
                WHERE d.FACTURA = f.ID
                  AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                    OR UPPER(d.DESCRIPCION) LIKE '%BASURA%'))
   -- Guarda: nunca dejar la base 0% negativa. Si el terceros es mayor que
   -- el SUBCERO actual, esa factura tiene otro problema y se revisa a mano.
   AND NVL(f.SUBCERO,0) >= (SELECT NVL(SUM(d.BASEIMPONIBLE),0) FROM PGS.DFCC d
                      WHERE d.FACTURA = f.ID
                        AND (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
                          OR UPPER(d.DESCRIPCION) LIKE '%BASURA%'));


-- ---------------------------------------------------------------------
-- BLOQUE 4 — El detalle tambien dice la verdad: la linea de terceros pasa
--            del codigo 0 (tarifa 0%) al 6 (no objeto de impuesto), que es
--            el codigo del SRI para esto. Asi cualquier medicion futura por
--            el detalle clasifica igual que la cabecera.
-- ESPERADO: las lineas de bomberos/basura de las facturas del BLOQUE 0.
-- ---------------------------------------------------------------------
UPDATE PGS.DFCC d
   SET d.CODIGOIVASRI = 6
 WHERE (UPPER(d.DESCRIPCION) LIKE '%BOMBERO%'
     OR UPPER(d.DESCRIPCION) LIKE '%BASURA%')
   AND NVL(d.CODIGOIVASRI,-1) = 0
   AND EXISTS (SELECT 1 FROM PGS.FCTC f WHERE f.ID = d.FACTURA AND f.ESTADO = 1);


-- ---------------------------------------------------------------------
-- BLOQUE 5 — Control posterior, ANTES del COMMIT.
-- ESPERADO: SUBNOOBJ = TERCEROS y BASE_0_AHORA = SUBTOTAL - TERCEROS en
--           todas. Ninguna con BASE_0_AHORA negativa.
--           Si algo no cuadra: ROLLBACK y avisar.
-- ---------------------------------------------------------------------
SELECT 'BLOQUE 5 - control posterior' AS bloque,
       f.ID, f.NUMERO,
       NVL(f.SUBTOTAL,0) AS subtotal,
       NVL(f.SUBNOOBJ,0) AS base_no_objeto,
       NVL(f.SUBCERO,0)  AS base_0_ahora,
       NVL(f.SUBTOTAL,0) - NVL(f.SUBCERO,0) - NVL(f.SUBNOOBJ,0) AS gravada_que_declarara_ats
  FROM PGS.FCTC f
 WHERE f.ESTADO = 1
   AND NVL(f.SUBNOOBJ,0) <> 0
 ORDER BY f.FECHA, f.ID;


-- ⛔ SIN ESTE COMMIT NO SE GUARDA NADA DE LOS BLOQUES 3 Y 4.
-- COMMIT;


-- =====================================================================
-- DESPUES DEL COMMIT: desplegar el WAR y regenerar el ATS.
--   ORDEN ESTRICTO: este script -> WAR -> regenerar.
--   Un WAR con la columna mapeada y sin la columna en la base rompe TODA
--   lectura de PGS.FCTC con ORA-00904.
--
-- REVERSO — comentado. Los valores originales son los del BLOQUE 0.
--   UPDATE PGS.FCTC SET SUBCERO = NVL(SUBCERO,0) + NVL(SUBNOOBJ,0), SUBNOOBJ = 0
--    WHERE ESTADO = 1 AND NVL(SUBNOOBJ,0) <> 0;
--   UPDATE PGS.DFCC SET CODIGOIVASRI = 0
--    WHERE (UPPER(DESCRIPCION) LIKE '%BOMBERO%' OR UPPER(DESCRIPCION) LIKE '%BASURA%')
--      AND NVL(CODIGOIVASRI,-1) = 6;
--   COMMIT;
--   -- Y la columna, solo si hace falta volver a un WAR viejo:
--   -- ALTER TABLE PGS.FCTC DROP COLUMN SUBNOOBJ;
--
-- Antes del COMMIT alcanza con:
--   ROLLBACK;
-- =====================================================================
