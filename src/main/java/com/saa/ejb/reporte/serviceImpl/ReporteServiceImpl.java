package com.saa.ejb.reporte.serviceImpl;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.saa.ejb.reporte.service.ReporteService;

import jakarta.ejb.Stateless;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * Servicio para la generación de reportes JasperReports
 */
@Stateless
public class ReporteServiceImpl implements ReporteService {
    
    private static final Logger LOGGER = Logger.getLogger(ReporteServiceImpl.class.getName());
    
    // Candidatos típicos de DataSource en WildFly
    private static final List<String> DS_CANDIDATES = List.of(
            "java:jboss/datasources/SaaDS",
            "java:/SaaDS",
            "java:/jdbc/SaaDS",
            "java:jboss/datasources/OracleDS",
            "java:/OracleDS",
            "java:/jdbc/OracleDS"
    );
    
    /**
     * Busca el DataSource mediante JNDI
     */
    private DataSource lookupDataSource() throws NamingException {
        InitialContext ctx = new InitialContext();
        NamingException last = null;

        for (String jndi : DS_CANDIDATES) {
            try {
                Object obj = ctx.lookup(jndi);
                if (obj instanceof DataSource ds) {
                    LOGGER.log(Level.INFO, "DataSource encontrado en: {0}", jndi);
                    return ds;
                }
            } catch (NamingException e) {
                last = e;
            }
        }
        throw (last != null) ? last : new NamingException("No DataSource encontrado en: " + DS_CANDIDATES);
    }
    
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
                                  Map<String, Object> parametros, String formato) throws Exception {
        
        LOGGER.log(Level.INFO, "Generando reporte: modulo={0}, nombre={1}, formato={2}", 
                   new Object[]{modulo, nombreReporte, formato});
        
        Connection conn = null;
        try {
            // Construir la ruta del reporte .jasper (precompilado)
            String rutaJasper = String.format("/rep/%s/%s.jasper", modulo, nombreReporte);
            
            LOGGER.log(Level.INFO, "Cargando reporte precompilado: {0}", rutaJasper);
            
            // Cargar el archivo jasper precompilado
            InputStream reportStream = getClass().getResourceAsStream(rutaJasper);
            
            if (reportStream == null) {
                throw new IllegalArgumentException("No se encontró el reporte precompilado: " + rutaJasper + 
                    ". Por favor, compile el archivo .jrxml con JasperSoft Studio 7.0.3 primero.");
            }
            
            // Cargar el reporte precompilado (mucho más rápido que compilar en runtime)
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(reportStream);
            
            // Agregar ruta de imágenes a los parámetros (si el frontend no la envió o es null)
            if (!parametros.containsKey("P_IMAGEN") || parametros.get("P_IMAGEN") == null) {
                // Cargar la imagen como objeto java.awt.Image para compatibilidad con evaluationTime="Report"
                try {
                    InputStream imageStream = getClass().getResourceAsStream("/rep/img/logo_aso.jpeg");
                    if (imageStream != null) {
                        Image image = ImageIO.read(imageStream);
                        imageStream.close();
                        
                        if (image != null) {
                            parametros.put("P_IMAGEN", image);
                            LOGGER.log(Level.INFO, "Imagen cargada como java.awt.Image: {0}x{1}", 
                                      new Object[]{image.getWidth(null), image.getHeight(null)});
                        } else {
                            LOGGER.log(Level.WARNING, "No se pudo decodificar la imagen logo_aso.jpeg");
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "No se pudo encontrar la imagen en /rep/img/logo_aso.jpeg");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al cargar imagen: {0}", e.getMessage());
                }
            } else {
                LOGGER.log(Level.INFO, "P_IMAGEN ya proporcionado por el frontend: {0}", parametros.get("P_IMAGEN"));
            }
            
            // Agregar ruta de imágenes genérica (por si acaso se usa en el reporte)
            String rutaImagenes = getClass().getResource("/rep/img/").toString();
            parametros.put("RUTA_IMAGENES", rutaImagenes);
            parametros.put("SUBREPORT_DIR", getClass().getResource("/rep/" + modulo + "/").toString());
            
            // Obtener conexión a la base de datos
            DataSource dataSource = lookupDataSource();
            conn = dataSource.getConnection();
            
            // Llenar el reporte con datos
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parametros, conn);
            
            // Exportar según el formato solicitado
            return exportarReporte(jasperPrint, formato);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al generar reporte", e);
            throw new Exception("Error al generar el reporte: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error al cerrar conexión", e);
                }
            }
        }
    }
    
    /**
     * Exporta el reporte al formato especificado
     */
    private byte[] exportarReporte(JasperPrint jasperPrint, String formato) throws Exception {
        
        String formatoUpper = formato != null ? formato.toUpperCase() : "PDF";
        
        switch (formatoUpper) {
            case "PDF":
                return JasperExportManager.exportReportToPdf(jasperPrint);
                
            case "EXCEL":
            case "XLS":
            case "XLSX":
                return exportarExcel(jasperPrint);
                
            case "HTML":
                return exportarHtml(jasperPrint);
                
            default:
                LOGGER.log(Level.WARNING, "Formato no soportado: {0}, usando PDF", formato);
                return JasperExportManager.exportReportToPdf(jasperPrint);
        }
    }
    
    /**
     * Exporta el reporte a formato Excel
     */
    private byte[] exportarExcel(JasperPrint jasperPrint) throws Exception {
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        JRXlsxExporter exporter = new JRXlsxExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        
        SimpleXlsxReportConfiguration configuration = new SimpleXlsxReportConfiguration();
        configuration.setOnePagePerSheet(false);
        configuration.setDetectCellType(true);
        configuration.setCollapseRowSpan(false);
        
        exporter.setConfiguration(configuration);
        exporter.exportReport();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Exporta el reporte a formato HTML
     */
    private byte[] exportarHtml(JasperPrint jasperPrint) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        HtmlExporter exporter = new HtmlExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleHtmlExporterOutput(outputStream));
        exporter.exportReport();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Valida que el módulo sea válido
     */
    public boolean esModuloValido(String modulo) {
        return modulo != null && 
               (modulo.equals("cnt") || modulo.equals("tsr") || modulo.equals("crd") || 
                modulo.equals("cxc") || modulo.equals("cxp") || modulo.equals("rhh"));
    }
}
