-- =====================================================================
-- CONSULTAS PARA OBTENER NOVEDADES DEL PROCESAMIENTO PETROCOMERCIAL
-- =====================================================================

-- =====================================================================
-- 1. RESUMEN GENERAL DEL PROCESAMIENTO
-- =====================================================================
-- Esta consulta te da una vista general de toda la carga
SELECT 
    ca.CRARCDGO as idCarga,
    ca.CRARNMBR as nombreArchivo,
    ca.CRARMSAF || '/' || ca.CRARANAF as periodo,
    ca.CRARFCCR as fechaCarga,
    COUNT(pxca.PXCACDGO) as totalRegistrosArchivo,
    COUNT(CASE WHEN pxca.PXCANVCA = 0 THEN 1 END) as registrosOK_Fase1,
    COUNT(CASE WHEN pxca.PXCANVCA != 0 THEN 1 END) as registrosConError_Fase1,
    COUNT(prca.PRCACDGO) as registrosProcesados_Fase2,
    COUNT(CASE WHEN prca.PRCAPRCS = 1 THEN 1 END) as procesadosExitosos,
    COUNT(CASE WHEN prca.PRCAPRCS = 2 THEN 1 END) as procesadosConError,
    COUNT(prca.PRCAIDPG) as totalPagosGenerados,
    COUNT(prca.PRCAIDAP) as totalAportesGenerados,
    SUM(CASE WHEN prca.PRCAESDQ = 4 THEN 1 ELSE 0 END) as cuotasPagadas,
    SUM(CASE WHEN prca.PRCAESDQ = 5 THEN 1 ELSE 0 END) as cuotasEnMora,
    SUM(CASE WHEN prca.PRCAESDQ = 6 THEN 1 ELSE 0 END) as cuotasParciales
FROM CRD.CRAR ca
JOIN CRD.DTCA dca ON ca.CRARCDGO = dca.CRARCDGO
JOIN CRD.PXCA pxca ON dca.DTCACDGO = pxca.DTCACDGO
LEFT JOIN CRD.PRCA prca ON pxca.PXCACDGO = prca.PXCACDGO
WHERE ca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
GROUP BY ca.CRARCDGO, ca.CRARNMBR, ca.CRARMSAF, ca.CRARANAF, ca.CRARFCCR;


-- =====================================================================
-- 2. DETALLE DE TODOS LOS REGISTROS CON SUS NOVEDADES
-- =====================================================================
-- Esta consulta te muestra CADA registro del archivo con su estado
SELECT 
    pxca.PXCACDGO as idRegistro,
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombreParticipe,
    dca.DTCACDPP as codigoProducto,
    dca.DTCANMPP as nombreProducto,
    
    -- Novedades FASE 1 (validación)
    CASE pxca.PXCANVCA
        WHEN 0 THEN 'OK'
        WHEN 1 THEN 'PARTICIPE NO ENCONTRADO'
        WHEN 2 THEN 'CODIGO ROL DUPLICADO'
        WHEN 3 THEN 'NOMBRE ENTIDAD DUPLICADO'
        WHEN 4 THEN 'CODIGO PETRO NO COINCIDE CON NOMBRE'
        ELSE 'DESCONOCIDO'
    END as novedadValidacion,
    
    -- Novedades FASE 2 (procesamiento)
    CASE prca.PRCAPRCS
        WHEN 0 THEN 'NO PROCESADO'
        WHEN 1 THEN 'PROCESADO OK'
        WHEN 2 THEN 'ERROR'
        ELSE 'SIN PROCESAR'
    END as estadoProcesamiento,
    
    CASE prca.PRCANVPR
        WHEN 9 THEN 'PRODUCTO NO MAPEADO'
        WHEN 10 THEN 'PRESTAMO NO ENCONTRADO'
        WHEN 11 THEN 'MULTIPLES PRESTAMOS ACTIVOS'
        WHEN 12 THEN 'CUOTA NO ENCONTRADA'
        WHEN 13 THEN 'MONTO INCONSISTENTE'
        WHEN 14 THEN 'PRESTAMO PROCESADO OK'
        WHEN 15 THEN 'APORTE GENERADO OK'
        ELSE NULL
    END as novedadProcesamiento,
    
    -- Estado determinado de la cuota
    CASE prca.PRCAESDQ
        WHEN 1 THEN 'PENDIENTE'
        WHEN 2 THEN 'ACTIVA'
        WHEN 3 THEN 'EMITIDA'
        WHEN 4 THEN 'PAGADA'
        WHEN 5 THEN 'EN MORA'
        WHEN 6 THEN 'PARCIAL'
        WHEN 7 THEN 'CANCELADA ANTICIPADA'
        WHEN 8 THEN 'VENCIDA'
    END as estadoCuota,
    
    -- Valores del archivo
    pxca.PXCADSCT as montoDescontar,
    pxca.PXCADSDO as totalDescontado,
    pxca.PXCACPDS as capitalDescontado,
    pxca.PXCAINDS as interesDescontado,
    
    -- Resultados del procesamiento
    prca.PRCAIDPS as idPrestamo,
    prca.PRCAIDCT as idCuota,
    prca.PRCAIDPG as idPago,
    prca.PRCASLCP as saldoCapitalPendiente,
    prca.PRCASLIN as saldoInteresPendiente,
    prca.PRCAOBSR as observaciones,
    prca.PRCAERRO as error,
    prca.PRCAFCPR as fechaProcesamiento
    
FROM CRD.PXCA pxca
JOIN CRD.DTCA dca ON pxca.DTCACDGO = dca.DTCACDGO
JOIN CRD.CRAR ca ON dca.CRARCDGO = ca.CRARCDGO
LEFT JOIN CRD.PRCA prca ON pxca.PXCACDGO = prca.PXCACDGO
WHERE ca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
ORDER BY 
    prca.PRCAPRCS DESC,  -- Primero los procesados OK
    pxca.PXCANVCA,       -- Luego por novedad de validación
    prca.PRCANVPR;       -- Luego por novedad de procesamiento


-- =====================================================================
-- 3. SOLO REGISTROS CON ERRORES (Para revisión manual)
-- =====================================================================
SELECT 
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombre,
    dca.DTCANMPP as producto,
    
    -- Error FASE 1
    CASE WHEN pxca.PXCANVCA != 0 THEN
        CASE pxca.PXCANVCA
            WHEN 1 THEN 'PARTICIPE NO ENCONTRADO'
            WHEN 2 THEN 'CODIGO ROL DUPLICADO'
            WHEN 3 THEN 'NOMBRE ENTIDAD DUPLICADO'
            WHEN 4 THEN 'CODIGO PETRO NO COINCIDE CON NOMBRE'
        END
    ELSE NULL END as errorValidacion,
    
    -- Error FASE 2
    CASE prca.PRCANVPR
        WHEN 9 THEN 'PRODUCTO NO MAPEADO'
        WHEN 10 THEN 'PRESTAMO NO ENCONTRADO'
        WHEN 11 THEN 'MULTIPLES PRESTAMOS ACTIVOS'
        WHEN 12 THEN 'CUOTA NO ENCONTRADA'
        WHEN 13 THEN 'MONTO INCONSISTENTE'
    END as errorProcesamiento,
    
    prca.PRCAERRO as descripcionError,
    prca.PRCAOBSR as observaciones

FROM CRD.PXCA pxca
JOIN CRD.DTCA dca ON pxca.DTCACDGO = dca.DTCACDGO
JOIN CRD.CRAR ca ON dca.CRARCDGO = ca.CRARCDGO
LEFT JOIN CRD.PRCA prca ON pxca.PXCACDGO = prca.PXCACDGO
WHERE ca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
  AND (pxca.PXCANVCA != 0 OR prca.PRCAPRCS = 2)  -- Tiene errores
ORDER BY pxca.PXCANVCA, prca.PRCANVPR;


-- =====================================================================
-- 4. CUOTAS PAGADAS COMPLETAMENTE
-- =====================================================================
SELECT 
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombreParticipe,
    e.ENTDRZSC as razonSocial,
    p.PRSTCDGO as idPrestamo,
    pr.PRDCNMBR as tipoProducto,
    dp.DTPRNMCT as numeroCuota,
    dp.DTPRFCVN as fechaVencimiento,
    pp.PGPRVLOR as valorPagado,
    pp.PGPRCPPG as capitalPagado,
    pp.PGPRINPG as interesPagado,
    pp.PGPRFCHA as fechaPago
    
FROM CRD.PRCA prca
JOIN CRD.PXCA pxca ON prca.PXCACDGO = pxca.PXCACDGO
JOIN CRD.PRST p ON prca.PRCAIDPS = p.PRSTCDGO
JOIN CRD.ENTD e ON p.ENTDCDGO = e.ENTDCDGO
JOIN CRD.PRDC pr ON p.PRDCCDGO = pr.PRDCCDGO
JOIN CRD.DTPR dp ON prca.PRCAIDCT = dp.DTPRCDGO
JOIN CRD.PGPR pp ON prca.PRCAIDPG = pp.PGPRCDGO
WHERE prca.PRCAESDQ = 4  -- PAGADA
  AND pxca.DTCACDGO IN (
      SELECT DTCACDGO FROM CRD.DTCA WHERE CRARCDGO = :idCarga
  )
ORDER BY e.ENTDRZSC, dp.DTPRNMCT;


-- =====================================================================
-- 5. CUOTAS EN MORA
-- =====================================================================
SELECT 
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombreParticipe,
    e.ENTDRZSC as razonSocial,
    p.PRSTCDGO as idPrestamo,
    pr.PRDCNMBR as tipoProducto,
    dp.DTPRNMCT as numeroCuota,
    dp.DTPRFCVN as fechaVencimiento,
    dp.DTPRCPTL as capitalCuota,
    dp.DTPRINTR as interesCuota,
    dp.DTPRSLCP as saldoCapital,
    dp.DTPRSLIN as saldoInteres,
    prca.PRCAOBSR as motivo,
    TRUNC(SYSDATE - dp.DTPRFCVN) as diasMora
    
FROM CRD.PRCA prca
JOIN CRD.PXCA pxca ON prca.PXCACDGO = pxca.PXCACDGO
JOIN CRD.PRST p ON prca.PRCAIDPS = p.PRSTCDGO
JOIN CRD.ENTD e ON p.ENTDCDGO = e.ENTDCDGO
JOIN CRD.PRDC pr ON p.PRDCCDGO = pr.PRDCCDGO
JOIN CRD.DTPR dp ON prca.PRCAIDCT = dp.DTPRCDGO
WHERE prca.PRCAESDQ = 5  -- EN MORA
  AND pxca.DTCACDGO IN (
      SELECT DTCACDGO FROM CRD.DTCA WHERE CRARCDGO = :idCarga
  )
ORDER BY TRUNC(SYSDATE - dp.DTPRFCVN) DESC, e.ENTDRZSC;


-- =====================================================================
-- 6. CUOTAS CON PAGOS PARCIALES
-- =====================================================================
SELECT 
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombreParticipe,
    e.ENTDRZSC as razonSocial,
    p.PRSTCDGO as idPrestamo,
    dp.DTPRNMCT as numeroCuota,
    dp.DTPRCTAA as valorCuotaTotal,
    pxca.PXCADSDO as valorPagado,
    (dp.DTPRCTAA - pxca.PXCADSDO) as saldoPendiente,
    dp.DTPRSLCP as saldoCapital,
    dp.DTPRSLIN as saldoInteres,
    prca.PRCAOBSR as observaciones
    
FROM CRD.PRCA prca
JOIN CRD.PXCA pxca ON prca.PXCACDGO = pxca.PXCACDGO
JOIN CRD.PRST p ON prca.PRCAIDPS = p.PRSTCDGO
JOIN CRD.ENTD e ON p.ENTDCDGO = e.ENTDCDGO
JOIN CRD.DTPR dp ON prca.PRCAIDCT = dp.DTPRCDGO
WHERE prca.PRCAESDQ = 6  -- PARCIAL
  AND pxca.DTCACDGO IN (
      SELECT DTCACDGO FROM CRD.DTCA WHERE CRARCDGO = :idCarga
  )
ORDER BY (dp.DTPRCTAA - pxca.PXCADSDO) DESC;


-- =====================================================================
-- 7. PRÉSTAMOS CANCELADOS (Todas las cuotas pagadas)
-- =====================================================================
SELECT 
    e.ENTDCDGO as idEntidad,
    e.ENTDRZSC as razonSocial,
    e.ENTDRPCO as rolPetro,
    p.PRSTCDGO as idPrestamo,
    pr.PRDCNMBR as tipoProducto,
    p.PRSTMNSL as montoOriginal,
    p.PRSTPLZO as plazoOriginal,
    p.PRSTTPPR as totalPagado,
    p.PRSTSLTT as saldoActual,
    p.ESPSCDGO as estadoPrestamo,
    COUNT(dp.DTPRCDGO) as totalCuotas,
    COUNT(CASE WHEN dp.DTPRESTD = 4 THEN 1 END) as cuotasPagadas
    
FROM CRD.PRST p
JOIN CRD.ENTD e ON p.ENTDCDGO = e.ENTDCDGO
JOIN CRD.PRDC pr ON p.PRDCCDGO = pr.PRDCCDGO
JOIN CRD.DTPR dp ON p.PRSTCDGO = dp.PRSTCDGO
WHERE p.ESPSCDGO = 3  -- CANCELADO
  AND p.PRSTCDGO IN (
      SELECT DISTINCT prca.PRCAIDPS 
      FROM CRD.PRCA prca
      JOIN CRD.PXCA pxca ON prca.PXCACDGO = pxca.PXCACDGO
      WHERE pxca.DTCACDGO IN (
          SELECT DTCACDGO FROM CRD.DTCA WHERE CRARCDGO = :idCarga
      )
  )
GROUP BY e.ENTDCDGO, e.ENTDRZSC, e.ENTDRPCO, p.PRSTCDGO, pr.PRDCNMBR,
         p.PRSTMNSL, p.PRSTPLZO, p.PRSTTPPR, p.PRSTSLTT, p.ESPSCDGO
ORDER BY e.ENTDRZSC;


-- =====================================================================
-- 8. ESTADÍSTICAS POR PRODUCTO
-- =====================================================================
SELECT 
    dca.DTCACDPP as codigoProducto,
    dca.DTCANMPP as nombreProducto,
    COUNT(pxca.PXCACDGO) as totalRegistros,
    COUNT(prca.PRCACDGO) as registrosProcesados,
    COUNT(CASE WHEN prca.PRCAPRCS = 1 THEN 1 END) as procesadosOK,
    COUNT(CASE WHEN prca.PRCAPRCS = 2 THEN 1 END) as procesadosError,
    SUM(CASE WHEN prca.PRCAESDQ = 4 THEN 1 ELSE 0 END) as cuotasPagadas,
    SUM(CASE WHEN prca.PRCAESDQ = 5 THEN 1 ELSE 0 END) as cuotasEnMora,
    SUM(CASE WHEN prca.PRCAESDQ = 6 THEN 1 ELSE 0 END) as cuotasParciales,
    SUM(pxca.PXCADSDO) as totalDescontado,
    SUM(pxca.PXCACPDS) as totalCapitalPagado,
    SUM(pxca.PXCAINDS) as totalInteresPagado
    
FROM CRD.DTCA dca
JOIN CRD.PXCA pxca ON dca.DTCACDGO = pxca.DTCACDGO
LEFT JOIN CRD.PRCA prca ON pxca.PXCACDGO = prca.PXCACDGO
WHERE dca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
GROUP BY dca.DTCACDPP, dca.DTCANMPP
ORDER BY dca.DTCACDPP;


-- =====================================================================
-- 9. REGISTROS NO PROCESADOS (Pendientes)
-- =====================================================================
-- Estos son registros que pasaron FASE 1 pero no fueron procesados en FASE 2
SELECT 
    pxca.PXCACDGO as idRegistro,
    pxca.PXCACDPT as codigoPetro,
    pxca.PXCANMBR as nombreParticipe,
    dca.DTCANMPP as producto,
    pxca.PXCADSCT as montoDescontar,
    'No procesado en FASE 2' as motivo
    
FROM CRD.PXCA pxca
JOIN CRD.DTCA dca ON pxca.DTCACDGO = dca.DTCACDGO
WHERE dca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
  AND pxca.PXCANVCA = 0  -- Pasó validación FASE 1
  AND NOT EXISTS (
      SELECT 1 FROM CRD.PRCA prca 
      WHERE prca.PXCACDGO = pxca.PXCACDGO
  )
ORDER BY pxca.PXCACDPT;


-- =====================================================================
-- 10. TOTALES FINANCIEROS DEL PROCESAMIENTO
-- =====================================================================
SELECT 
    ca.CRARCDGO as idCarga,
    ca.CRARNMBR as nombreArchivo,
    ca.CRARMSAF || '/' || ca.CRARANAF as periodo,
    
    -- Totales del archivo original
    ca.CRARTTDS as totalDecontarArchivo,
    ca.CRARTTDO as totalDescontadoArchivo,
    ca.CRARTTCD as totalCapitalDescontadoArchivo,
    ca.CRARTTID as totalInteresDescontadoArchivo,
    
    -- Totales procesados (verificación)
    SUM(CASE WHEN prca.PRCAIDPG IS NOT NULL THEN pxca.PXCADSDO ELSE 0 END) as totalPagosGenerados,
    SUM(CASE WHEN prca.PRCAIDPG IS NOT NULL THEN pxca.PXCACPDS ELSE 0 END) as totalCapitalProcesado,
    SUM(CASE WHEN prca.PRCAIDPG IS NOT NULL THEN pxca.PXCAINDS ELSE 0 END) as totalInteresProcesado,
    
    -- Saldos pendientes
    SUM(prca.PRCASLCP) as totalSaldoCapitalPendiente,
    SUM(prca.PRCASLIN) as totalSaldoInteresPendiente
    
FROM CRD.CRAR ca
JOIN CRD.DTCA dca ON ca.CRARCDGO = dca.CRARCDGO
JOIN CRD.PXCA pxca ON dca.DTCACDGO = pxca.DTCACDGO
LEFT JOIN CRD.PRCA prca ON pxca.PXCACDGO = prca.PXCACDGO
WHERE ca.CRARCDGO = :idCarga  -- PARÁMETRO: ID del archivo
GROUP BY ca.CRARCDGO, ca.CRARNMBR, ca.CRARMSAF, ca.CRARANAF,
         ca.CRARTTDS, ca.CRARTTDO, ca.CRARTTCD, ca.CRARTTID;


-- =====================================================================
-- NOTA: En todas las consultas, reemplaza :idCarga con el ID real
-- Ejemplo: WHERE ca.CRARCDGO = 123
-- =====================================================================
