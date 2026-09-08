package tools.jasper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JRException;

/**
 * Compila todos los .jrxml de src/main/resources/rep/** a .jasper, fuera de WildFly.
 *
 * <p>Medido el 2026-09-08 (ver docs/logica-negocio/tsr/DISENO-REPORTES-CONCILIACION-BANCARIA.md
 * §5): no hay Jaspersoft Studio en esta máquina, pero {@code jasperreports-jdt} (el compilador
 * de expresiones basado en el Eclipse Compiler for Java) sí se resuelve por Maven, y con el
 * classpath del proyecto + ese jar, {@link JasperCompileManager#compileReportToFile} hace
 * exactamente lo que hace Studio al guardar un reporte. Esto NO reemplaza el hecho de que
 * WildFly, en runtime, no puede compilar (ver el CLAUDE.md) — es la herramienta para producir
 * el .jasper de antemano y commitearlo junto al .jrxml, que es lo que WildFly sí puede cargar.</p>
 *
 * <p>Uso: {@code compilar-jasper.bat} en la raíz del repo (arma el classpath solo). A mano:</p>
 * <pre>
 * java -cp "&lt;classpath del pom de tools/jasper&gt;" tools.jasper.CompilarJasper [raiz-rep] [--forzar]
 * </pre>
 *
 * <p>Sin argumentos, recorre {@code src/main/resources/rep} desde el directorio de trabajo
 * actual y compila cualquier {@code .jrxml} cuyo {@code .jasper} falte o esté más viejo que el
 * fuente. {@code --forzar} (en cualquier posición de los argumentos) recompila todo, aunque el
 * {@code .jasper} esté al día — para el caso de haber cambiado la versión de JasperReports o el
 * propio harness. Termina con código de salida distinto de cero si algún reporte falló, para
 * que el commit no siga adelante con un {@code .jrxml} que no compila.</p>
 */
public final class CompilarJasper {

    private CompilarJasper() {
    }

    public static void main(String[] args) throws IOException {
        boolean forzar = false;
        String raizArg = null;
        for (String arg : args) {
            if ("--forzar".equals(arg)) {
                forzar = true;
            } else {
                raizArg = arg;
            }
        }
        Path raiz = Path.of(raizArg != null ? raizArg : "src/main/resources/rep");
        if (!Files.isDirectory(raiz)) {
            System.out.println("No existe el directorio " + raiz.toAbsolutePath()
                    + " -- corré este harness desde la raíz del repo (o pasale la ruta a rep/ como argumento).");
            System.exit(2);
            return;
        }

        // net.sf.jasperreports.compiler.class no tiene extensión auto-registrada en el jar de
        // jasperreports-jdt (verificado: no trae META-INF/services ni jasperreports_extension.
        // properties) -- hay que fijar la propiedad a mano para que el motor use JRJdtCompiler
        // en vez de intentar JRJavacCompiler (que en el WAR, dentro de WildFly, es justamente
        // el que no funciona: sin javac externo ni acceso al classloader del deployment).
        System.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.jdt.JRJdtCompiler");

        List<Path> jrxmls = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(raiz)) {
            paths.filter(p -> p.toString().endsWith(".jrxml")).sorted().forEach(jrxmls::add);
        }

        if (jrxmls.isEmpty()) {
            System.out.println("No se encontró ningún .jrxml bajo " + raiz.toAbsolutePath() + ".");
            return;
        }

        int compilados = 0;
        int alDia = 0;
        int fallidos = 0;
        for (Path jrxml : jrxmls) {
            Path jasper = Path.of(jrxml.toString().substring(0, jrxml.toString().length() - ".jrxml".length())
                    + ".jasper");
            boolean falta = !Files.exists(jasper);
            boolean vencido = !falta && jrxml.toFile().lastModified() > jasper.toFile().lastModified();
            if (!forzar && !falta && !vencido) {
                alDia++;
                continue;
            }
            System.out.println("Compilando " + raiz.relativize(jrxml) + " ...");
            try {
                JasperCompileManager.compileReportToFile(jrxml.toString(), jasper.toString());
                System.out.println("  OK -> " + jasper.getFileName());
                compilados++;
            } catch (JRException e) {
                System.out.println("  ERROR: " + e.getMessage());
                fallidos++;
            }
        }

        System.out.println();
        System.out.println("Compilados: " + compilados + " | Al día: " + alDia + " | Fallidos: " + fallidos
                + " | Total: " + jrxmls.size());
        if (fallidos > 0) {
            System.exit(1);
        }
    }
}
