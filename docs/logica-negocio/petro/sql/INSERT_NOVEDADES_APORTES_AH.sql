-- ============================================
-- SCRIPT: Inserción de Novedades para Producto AH (Aportes)
-- Fecha: 2026-03-30
-- Descripción: Nuevos códigos de novedad para validación de aportes en archivo Petrocomercial
-- ============================================

-- Tabla: DTLL (Detalle Rubro)
-- Rubro: 99 (ASPNovedadesCargaArchivo)

-- Novedad 18: No se encontró HistorialSueldo para la entidad
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (218, 99, 18, 'HISTORIAL_SUELDO_NO_ENCONTRADO', 18, 1);

-- Novedad 19: Existen múltiples registros activos en HistorialSueldo para la misma entidad
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (219, 99, 19, 'MULTIPLES_REGISTROS_HISTORIAL_SUELDO', 19, 1);

-- Novedad 20: Los valores de montoJubilacion o montoCesantia están en NULL
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (220, 99, 20, 'VALORES_HISTORIAL_NULOS', 20, 1);

-- Novedad 21: El archivo indica $0 en producto AH (no se realizó el descuento del aporte)
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (221, 99, 21, 'APORTE_VALORES_CERO', 21, 1);

-- Novedad 22: El monto del archivo no coincide con el esperado (diferencia mayor a $1)
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (222, 99, 22, 'APORTE_MONTO_INCONSISTENTE', 22, 1);

-- Novedad 23: Diferencia menor o igual a $1 entre monto archivo y monto esperado (dentro de tolerancia)
INSERT INTO DTLL (CDGO, TBLA, NMRO, DSCRPCN, VLOR, ESTD) 
VALUES (223, 99, 23, 'APORTE_DIFERENCIA_MENOR_UN_DOLAR', 23, 1);

COMMIT;

-- ============================================
-- VERIFICACIÓN
-- ============================================
-- SELECT * FROM DTLL WHERE TBLA = 99 AND NMRO >= 18 ORDER BY NMRO;
