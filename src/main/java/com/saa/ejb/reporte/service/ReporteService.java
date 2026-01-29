package com.saa.ejb.reporte.service;

import java.util.Map;

import jakarta.ejb.Local;

/**
 * Servicio para la generación de reportes JasperReports
 */
@Local
public interface ReporteService {
    
    /**
     * Genera un reporte basado en el módulo, nombre y parámetros proporcionados
     * 
     * @param modulo Módulo del reporte (cnt, tsr, crd, cxc, cxp, rhh)
     * @param nombreReporte Nombre del archivo jrxml sin extensión
     * @param parametros Parámetros para el reporte
     * @param formato Formato de salida (PDF, EXCEL, HTML)
     * @return Bytes del reporte generado
     * @throws Exception Si ocurre un error al generar el reporte
     */
    public byte[] generarReporte(String modulo, String nombreReporte, 
                                  Map<String, Object> parametros, String formato) throws Exception;
    
    
    /**
     * Valida que el módulo sea válido
     */
    public boolean esModuloValido(String modulo);
}
