-- =====================================================================================
-- 319 — MEDICION (solo lectura): acuerdos de condonacion YA APLICADOS que condonaron interes de
--       cuotas que todavia no vencian
-- FECHA: 2026-10-06 · EQUIPO: omen-saa-1 (CRD · EQUIPO B)
--
-- Defecto (corregido en el WAR siguiente): el desglose del acuerdo sumaba el interes, el desgravamen y el
-- seguro de TODAS las cuotas pendientes, tambien las futuras, y el asiento de condonacion llevaba a gasto
-- ese interes futuro como «condonado». Regla correcta, la de la precancelacion: de las cuotas futuras solo
-- se cobra el capital. Su interes nunca fue deuda.
-- Un prestamo condonado queda CANCELADO: toda cuota suya en CANCELADA_ANTICIPADA (7) la cerro el acuerdo.
-- =====================================================================================

-- 1. Por acuerdo: cuantas cuotas futuras cerro y cuanto interes tenian. Esperado: ojala 0 filas.
SELECT a.ACCNCDGO AS ACUERDO, NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO, a.ACCNFCHA AS FECHA_ACUERDO,
       a.ACCNFCAP AS FECHA_APLICACION, a.ACCNVLCN AS VALOR_CONDONADO_TOTAL,
       COUNT(d.DTPRCDGO) AS CUOTAS_FUTURAS, ROUND(SUM(NVL(d.DTPRINTR, 0)), 2) AS INTERES_FUTURO
  FROM CRD.ACCN a
  JOIN CRD.PRST p ON p.PRSTCDGO = a.PRSTCDGO
  JOIN CRD.DTPR d ON d.PRSTCDGO = a.PRSTCDGO
 WHERE a.ACCNESTD = 2
   AND d.DTPRESTD = 7
   AND d.DTPRFCVN > a.ACCNFCHA
   AND NVL(d.DTPRINTR, 0) > 0
 GROUP BY a.ACCNCDGO, NVL(p.PRSTIDAS, p.PRSTCDGO), a.ACCNFCHA, a.ACCNFCAP, a.ACCNVLCN
 ORDER BY a.ACCNCDGO;

-- 2. Acuerdos REGISTRADOS y todavia no aplicados (con la version nueva no van a cuadrar: hay que anularlos
--    y registrarlos de nuevo si su prestamo tiene cuotas futuras).
SELECT a.ACCNCDGO AS ACUERDO, NVL(p.PRSTIDAS, p.PRSTCDGO) AS PRESTAMO, a.ACCNESTD, a.ACCNFCHA, a.ACCNVLCN,
       (SELECT COUNT(*) FROM CRD.DTPR d WHERE d.PRSTCDGO = a.PRSTCDGO
          AND NVL(d.DTPRESTD, 0) NOT IN (4, 7) AND d.DTPRFCVN > SYSDATE) AS CUOTAS_FUTURAS
  FROM CRD.ACCN a
  JOIN CRD.PRST p ON p.PRSTCDGO = a.PRSTCDGO
 WHERE a.ACCNESTD = 1   -- VIGENTE
 ORDER BY a.ACCNCDGO;
