import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JacksonReportLoader;

/**
 * Valida un .jrxml contra el MISMO cargador Jackson que usa Jaspersoft Studio 7.0.3 y el motor
 * de JasperReports 7.0.3 en tiempo de ejecucion (JacksonReportLoader) -- NO compila el reporte
 * (no genera .jasper, no necesita jasperreports-jdt), solo verifica que el documento se pueda
 * deserializar a JasperDesign sin errores.
 *
 * Por que hace falta esto y no alcanza con "XML bien formado": el formato compacto que usa
 * Jaspersoft Studio 7 (atributo kind="..." en cada <element>) no tiene un XSD -- se deserializa
 * directo a las clases Java de diseno (JRDesignStaticText, JRDesignTextField, JRDesignBand, ...)
 * via Jackson. Un .jrxml puede ser XML perfectamente valido y aun asi JasperReports lo rechaza
 * con UnrecognizedPropertyException (ej.: un <element kind="staticText"> con <expression> en vez
 * de <text>, o un <summary> con un <band> anidado en vez de los atributos height/splitType
 * directo en <summary>). Este validador corre ese mismo chequeo sin necesitar Jaspersoft Studio
 * instalado -- ver docs/scripts/USAR-ValidateJrxml.md para el procedimiento completo.
 */
public class ValidateJrxml {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Uso: java ValidateJrxml <archivo.jrxml>");
            System.exit(2);
        }
        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        try {
            Optional<JasperDesign> design = JacksonReportLoader.instance()
                .loadReport(DefaultJasperReportsContext.getInstance(), bytes);
            if (design.isPresent()) {
                JasperDesign d = design.get();
                System.out.println("VALIDO - JasperDesign cargado OK: " + d.getName()
                    + " - parametros=" + d.getParameters().length
                    + " - campos=" + d.getFields().length
                    + " - variables=" + d.getVariables().length
                    + " - grupos=" + d.getGroups().length);
            } else {
                System.out.println("NO_RECONOCIDO - JacksonReportLoader no reconoció el formato"
                    + " (¿es un .jrxml clásico verboso, no el formato compacto de Studio 7?)");
                System.exit(1);
            }
        } catch (Throwable e) {
            System.out.println("INVALIDO - " + e.getClass().getName() + ": " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("  causado por: " + cause.getClass().getName() + ": " + cause.getMessage());
                cause = cause.getCause();
            }
            System.exit(1);
        }
    }
}
