-- =====================================================================================
-- 308 — MEDICION: por que la «ultima fecha de cobro» sale vacia en plazo vencido (solo lectura)
-- FECHA: 2026-10-02 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- COMO SE CALCULA HOY (DeclaracionPlazoVencidoServiceImpl:741-772):
--   1. MAX(PGPR.PGPRFCHA) de los pagos NO anulados del prestamo, buscados a traves de la CUOTA
--      (p.detallePrestamo.prestamo.codigo, PagoPrestamoDaoServiceImpl:345-349);
--   2. si no hay, el mayor DTPR.DTPRFCPG (fechaPagado) de las cuotas del prestamo;
--   3. si tampoco, queda VACIA.
-- Ademas, la foto de una declaracion hecha ANTES del WAR c005649d (2026-09-30) quedo vacia aunque hoy
-- se calcularia: la foto no se recalcula.
--
-- Este script dice, para cada declaracion con la fecha vacia, cual de las causas es. Pasar la salida.
-- =====================================================================================
WITH vacias AS (
    SELECT v.PLVNCDGO, v.PRSTCDGO, v.PLVNNMPS, v.PLVNESTD, v.PLVNFCDC
      FROM CRD.PLVN v
     WHERE v.PLVNFCUC IS NULL
)
SELECT x.PLVNCDGO, x.PLVNNMPS AS PRESTAMO, x.PLVNESTD AS ESTADO, x.PLVNFCDC AS DECLARADA,
       -- Causa A: pagos que existen pero NO tienen cuota (la consulta actual entra por la cuota)
       (SELECT COUNT(*) FROM CRD.PGPR g WHERE g.PRSTCDGO = x.PRSTCDGO AND NVL(g.PGPRANUL,0) = 0
           AND g.DTPRCDGO IS NULL)                                                         AS PAGOS_SIN_CUOTA,
       (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g WHERE g.PRSTCDGO = x.PRSTCDGO AND NVL(g.PGPRANUL,0) = 0)
                                                                                           AS MAX_FECHA_PGPR_POR_PRESTAMO,
       -- Causa B: pagos con cuota (lo que la consulta actual SI ve)
       (SELECT MAX(g.PGPRFCHA) FROM CRD.PGPR g JOIN CRD.DTPR d ON d.DTPRCDGO = g.DTPRCDGO
         WHERE d.PRSTCDGO = x.PRSTCDGO AND NVL(g.PGPRANUL,0) = 0)                          AS MAX_FECHA_PGPR_POR_CUOTA,
       -- Causa C: cuotas PAGADAS sin fecha de pago y sin PGPR (migracion)
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = x.PRSTCDGO AND d.DTPRESTD = 4)  AS CUOTAS_PAGADAS,
       (SELECT MAX(d.DTPRFCPG) FROM CRD.DTPR d WHERE d.PRSTCDGO = x.PRSTCDGO)              AS MAX_FECHA_PAGADO_CUOTA,
       (SELECT MAX(d.DTPRFCVN) FROM CRD.DTPR d WHERE d.PRSTCDGO = x.PRSTCDGO AND d.DTPRESTD = 4)
                                                                                           AS ULTIMA_CUOTA_PAGADA_VENCE
  FROM vacias x
 ORDER BY x.PLVNCDGO;

-- LECTURA, por fila:
--   * MAX_FECHA_PGPR_POR_PRESTAMO con valor y MAX_FECHA_PGPR_POR_CUOTA vacia → causa A: hay pagos
--     colgados del prestamo sin cuota. Se corrige buscando por PGPR.PRSTCDGO, no por la cuota.
--   * Las dos con valor → la foto es VIEJA (declarada antes del WAR del 30-09): hoy saldria bien.
--     Se corrige revirtiendo y declarando de nuevo, o con un UPDATE puntual de PLVNFCUC.
--   * PGPR vacio, CUOTAS_PAGADAS > 0, MAX_FECHA_PAGADO_CUOTA vacia → causa C: cuotas pagadas en la
--     migracion sin ninguna fecha. El unico dato que queda es el vencimiento de la ultima pagada.
--   * Todo vacio y CUOTAS_PAGADAS = 0 → el prestamo NUNCA se pago: vacia es lo correcto.
