-- =====================================================================================
-- ⛔ URGENTE — POR QUE NO CUADRAN LOS ASIENTOS DEL CIERRE DE CARTERA
-- FECHA: 2026-09-07   EQUIPO: omen-saa-1 (omen1)   SCRIPT: 214 (rango 200-249)
--
-- ⚠️ NO ESCRIBE NADA. Los tres bloques son SELECT. Se puede correr en horario laboral.
--
-- =====================================================================================
-- EL DIAGNOSTICO, medido contra el codigo (no es un defecto de calculo)
--
--   CierreCarteraServiceImpl.distribuye:534
--
--       List<BandaProductoDetalle> bandasProducto =
--               bandas.get(claveConfiguracion(idProducto, tipoCartera));
--       if (bandasProducto == null || bandasProducto.isEmpty()) {
--           acumulaSinClasificar(...);
--           continue;                 <-- el capital se DESCARTA de esta distribucion
--       }
--
--   La clave de configuracion es (PRODUCTO, TIPO DE CARTERA), y el tipo de cartera
--   DEPENDE DE LA FECHA: `tipoCarteraYDias(vencimiento, fecha)`. Una cuota que vence entre
--   los dos cortes es POR VENCER en el corte viejo y VENCIDA en el nuevo.
--
--   ⇒ Si un producto tiene bandas configuradas para UN tipo de cartera y no para el otro,
--     ese capital ENTRA por un lado del asiento y SE DESCARTA por el otro. El asiento queda
--     descuadrado exactamente por ese monto.
--
--   Es literal lo que dice la guarda que rechaza la ejecucion: "revise que la configuracion
--   de bandas cubra todos los productos con cartera".
--
--   ⭐ LAS BANDAS NO ESTAN MAL GENERADAS Y LA VALIDACION NO ESTA EQUIVOCADA. Falta
--     PARAMETRIZACION. Se arregla desde la pantalla de bandas, sin tocar codigo ni datos.
--
-- TIPO DE CARTERA: 1 = POR VENCER, 2 = VENCIDA (com.saa.rubros, mismo criterio que usa
-- ContabilizacionIndividualCreditoService#tipoCarteraYDias).
--
-- COMO DEVOLVER EL RESULTADO: pegar la salida de los tres bloques. El BLOQUE 1 es EL que
-- contesta.
-- =====================================================================================

SET PAGESIZE 300
SET LINESIZE 260
SET FEEDBACK ON


-- =====================================================================================
-- BLOQUE 1 — ⭐ LA RESPUESTA: productos CON CARTERA que NO tienen bandas configuradas
-- para alguno de los dos tipos.
--
-- Cada fila con FALTA = 'SI' es capital que se descarta en un lado del asiento.
-- Si este bloque devuelve 0 filas, el diagnostico es otro y hay que avisar.
-- =====================================================================================

SELECT p.PRDCCDGO                AS PRODUCTO,
       p.PRDCNMBR                AS NOMBRE_PRODUCTO,
       t.TIPO_CARTERA,
       CASE t.TIPO_CARTERA WHEN 1 THEN 'POR VENCER' ELSE 'VENCIDA' END AS NOMBRE_TIPO,
       NVL(cfg.BANDAS, 0)        AS BANDAS_CONFIGURADAS,
       CASE WHEN NVL(cfg.BANDAS, 0) = 0 THEN 'SI  <<<<' ELSE 'no' END AS FALTA,
       car.CAPITAL_EN_CARTERA,
       car.CUOTAS
  FROM CRD.PRDC p
 CROSS JOIN (SELECT 1 AS TIPO_CARTERA FROM DUAL
             UNION ALL SELECT 2 FROM DUAL) t
  JOIN (SELECT pr.PRDCCDGO,
               ROUND(SUM(NVL(d.DTPRCPTL,0) - NVL(d.DTPRCPPG,0)), 2) AS CAPITAL_EN_CARTERA,
               COUNT(*) AS CUOTAS
          FROM CRD.DTPR d
          JOIN CRD.PRST pr2 ON pr2.PRSTCDGO = d.PRSTCDGO
          JOIN CRD.PRDC pr  ON pr.PRDCCDGO  = pr2.PRDCCDGO
         WHERE d.DTPRESTD NOT IN (4, 7)
           AND NVL(d.DTPRCPTL,0) - NVL(d.DTPRCPPG,0) > 0
         GROUP BY pr.PRDCCDGO) car ON car.PRDCCDGO = p.PRDCCDGO
  LEFT JOIN (SELECT c.PRDCCDGO, c.CBPRTPCR, COUNT(b.BNDPCDGO) AS BANDAS
               FROM CRD.CBPR c
               LEFT JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
              WHERE c.CBPRFCFN IS NULL
              GROUP BY c.PRDCCDGO, c.CBPRTPCR) cfg
    ON cfg.PRDCCDGO = p.PRDCCDGO AND cfg.CBPRTPCR = t.TIPO_CARTERA
 ORDER BY FALTA DESC, car.CAPITAL_EN_CARTERA DESC;


-- =====================================================================================
-- BLOQUE 2 — La parametrizacion vigente, para ver que hay y que falta
-- =====================================================================================

SELECT c.CBPRCDGO       AS CONFIGURACION,
       c.PRDCCDGO       AS PRODUCTO,
       p.PRDCNMBR       AS NOMBRE_PRODUCTO,
       c.CBPRTPCR       AS TIPO_CARTERA,
       CASE c.CBPRTPCR WHEN 1 THEN 'POR VENCER' ELSE 'VENCIDA' END AS NOMBRE_TIPO,
       c.PJRQCDGO       AS EMPRESA,
       c.CBPRFCIN       AS VIGENTE_DESDE,
       c.CBPRFCFN       AS VIGENTE_HASTA,
       COUNT(b.BNDPCDGO) AS BANDAS,
       SUM(CASE WHEN b.PLNNCDGO IS NULL THEN 1 ELSE 0 END) AS BANDAS_SIN_CUENTA
  FROM CRD.CBPR c
  JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
  LEFT JOIN CRD.BNDP b ON b.CBPRCDGO = c.CBPRCDGO
 GROUP BY c.CBPRCDGO, c.PRDCCDGO, p.PRDCNMBR, c.CBPRTPCR, c.PJRQCDGO,
          c.CBPRFCIN, c.CBPRFCFN
 ORDER BY p.PRDCNMBR, c.CBPRTPCR;


-- =====================================================================================
-- BLOQUE 3 — ⚠️ SEGUNDA CAUSA POSIBLE: bandas configuradas SIN cuenta contable
-- Una banda sin PLNNCDGO tampoco puede entrar al asiento. Es el mismo defecto que se
-- corrigio hoy en el asiento de condonacion, en otro lugar.
-- Esperado: 0 filas. Si devuelve algo, hay que configurarles la cuenta.
-- =====================================================================================

SELECT c.PRDCCDGO AS PRODUCTO, p.PRDCNMBR AS NOMBRE_PRODUCTO,
       c.CBPRTPCR AS TIPO_CARTERA,
       b.BNDPCDGO AS BANDA, b.BNDPNMRO AS NUMERO
  FROM CRD.BNDP b
  JOIN CRD.CBPR c ON c.CBPRCDGO = b.CBPRCDGO
  JOIN CRD.PRDC p ON p.PRDCCDGO = c.PRDCCDGO
 WHERE b.PLNNCDGO IS NULL
   AND c.CBPRFCFN IS NULL
 ORDER BY p.PRDCNMBR, c.CBPRTPCR, b.BNDPNMRO;
