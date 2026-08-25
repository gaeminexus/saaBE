package com.saa.ejb.reporte.service;

import java.util.List;
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
     * Genera un reporte llenado con una COLECCIÓN de JavaBeans en vez de una consulta JDBC:
     * no pide conexión a la base en ningún momento. Aditivo — {@link #generarReporte} no se
     * toca; los 40 reportes existentes siguen por esa rama. Pensado para reportes cuyo dato no
     * está en la base (PLAN-SIMULADORES-PRESTAMOS.md §3): el {@code .jrxml} no lleva
     * {@code <query>} y sus {@code <field>} se resuelven contra las propiedades del bean.
     *
     * <b>Requiere el {@code .jasper} precompilado</b>: no hay una rama de compilación en tiempo
     * de ejecución acá (a propósito — ver CLAUDE.md, "el .jasper no es opcional").
     *
     * @param modulo Módulo del reporte (p.ej. "crd")
     * @param nombreReporte Nombre del archivo jasper sin extensión
     * @param parametros Parámetros escalares de cabecera del reporte
     * @param datos Colección de beans que alimenta el detalle (puede ser vacía, no null)
     * @param formato Formato de salida (PDF, EXCEL, HTML)
     * @return Bytes del reporte generado
     * @throws Exception Si el .jasper no existe o falla el llenado/exportación
     */
    public byte[] generarReporteDesdeColeccion(String modulo, String nombreReporte,
                                                Map<String, Object> parametros, List<?> datos,
                                                String formato) throws Exception;

    /**
     * Valida que el módulo sea válido
     */
    public boolean esModuloValido(String modulo);
}
