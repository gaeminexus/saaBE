-- =====================================================================
-- PAGOS FASE 1: la cuenta bancaria se elige al APROBAR, no al registrar
-- Modulo: PGS
-- Fecha:  2026-08-27
-- Autor:  orquestador (verificado contra la BD local, copia de produccion)
--
-- PARA QUE
--   Punto 14 del listado. Hoy PGS.PGTR.PGTRCNBC es NOT NULL: hay que
--   elegir la cuenta bancaria al REGISTRAR el pago. El negocio pide lo
--   contrario: que la solicitud nazca sin cuenta, y que tesoreria elija
--   la cuenta al aprobar, viendo todos los pagos juntos.
--   Ver docs/logica-negocio/pagos/PLAN-REDISENO-APROBACION-PAGOS.md
--
-- QUE HACE
--   Una sola cosa: relajar PGTRCNBC. El estado nuevo POR_APROBAR (0) vive
--   en la interfaz Java EstadoPagoProgramado, no en la base -- PGTRESTD no
--   tiene CHECK ni FK a catalogo, asi que no hay nada que agregar alli.
--   Verificado: no existe ningun CHECK sobre PGTRESTD.
--
-- POR QUE NO ROMPE NADA
--   Las 123 filas actuales tienen cuenta (0 con PGTRCNBC nulo), asi que
--   relajar la restriccion no toca ningun dato. La FK a TSR.CNBC se
--   mantiene: si viene cuenta, tiene que existir.
--
--   Los pagos en vuelo al desplegar siguen su curso normal: nacieron con
--   cuenta y en estado REGISTRADO(1). El estado 0 solo aplica a los que
--   nazcan despues. No hace falta migrar datos.
--
-- SQL puro. Ejecutar por bloques revisando los SELECT de control.
-- =====================================================================

-- ---------------------------------------------------------------------
-- BLOQUE 0: control previo
--   (a) PGTRCNBC debe figurar NOT NULL ('N')
--   (b) no debe haber ninguna fila con cuenta nula (deberia dar 0)
--   (c) la distribucion de estados, para comparar despues del despliegue
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRCNBC';

SELECT COUNT(*) AS PAGOS_SIN_CUENTA FROM PGS.PGTR WHERE PGTRCNBC IS NULL;

SELECT PGTRESTD AS ESTADO, COUNT(*) AS PAGOS FROM PGS.PGTR GROUP BY PGTRESTD ORDER BY 1;

-- ---------------------------------------------------------------------
-- BLOQUE 1: relajar la restriccion
-- ---------------------------------------------------------------------
ALTER TABLE PGS.PGTR MODIFY (PGTRCNBC NULL);

COMMENT ON COLUMN PGS.PGTR.PGTRCNBC IS 'Cuenta bancaria de la que sale el dinero. Opcional desde 2026-08-27: un pago en estado POR_APROBAR(0) todavia no la tiene, tesoreria la asigna al aprobar. Si viene, la FK exige que exista';

-- ---------------------------------------------------------------------
-- BLOQUE 2: control final
--   NULLABLE debe pasar a 'Y' y FK_PGTR_CTAORIGEN (PGTRCNBC -> TSR.CNBC)
--   debe seguir ENABLED. Ojo: se llama CTAORIGEN, no CNBC.
-- ---------------------------------------------------------------------
SELECT COLUMN_NAME, NULLABLE
  FROM ALL_TAB_COLUMNS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND COLUMN_NAME = 'PGTRCNBC';

SELECT CONSTRAINT_NAME, STATUS, VALIDATED
  FROM ALL_CONSTRAINTS
 WHERE OWNER = 'PGS' AND TABLE_NAME = 'PGTR' AND CONSTRAINT_TYPE = 'R'
   AND CONSTRAINT_NAME = 'FK_PGTR_CTAORIGEN';

COMMIT;
