package tools.jasper;

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
 * java -cp "&lt;classpath del pom de tools/jasper&gt;" tools.jasper.CompilarJasper [ruta ...] [--forzar]
 * </pre>
 *
 * <p><b>Criterio de "falta compilar" — deliberadamente NO es por fecha de modificación.</b>
 * Un {@code mtime} más nuevo en el {@code .jrxml} que en el {@code .jasper} parece razonable,
 * pero en un clon fresco (o después de cualquier {@code git checkout}) TODOS los archivos
 * quedan con el mtime del momento del checkout, en un orden que no tiene nada que ver con
 * cuál se editó de verdad — comparar por fecha ahí recompilaría {@code .jasper} de otros
 * equipos que Studio generó bien, metiendo un diff binario ajeno en el commit de quien
 * simplemente corrió el harness. Por eso:</p>
 * <ul>
 * <li><b>Sin argumentos:</b> recorre {@code src/main/resources/rep} y compila SOLO los
 * {@code .jrxml} que no tienen {@code .jasper} todavía (un reporte nuevo).</li>
 * <li><b>Una ruta a un directorio:</b> igual que sin argumentos, pero usa ese directorio como
 * raíz en vez de {@code src/main/resources/rep}.</li>
 * <li><b>Una o más rutas a archivos {@code .jrxml}:</b> se recompilan SIEMPRE, sin importar si
 * el {@code .jasper} ya existe — es el caso real de "acabo de editar este reporte", donde el
 * pedido es justamente reemplazar el {@code .jasper} viejo. Ej.:
 * {@code compilar-jasper.bat src/main/resources/rep/tsr/RPRT_CNCL_CNTA.jrxml}.</li>
 * <li><b>{@code --forzar}</b> (en cualquier posición, combinable con una ruta a directorio):
 * recompila TODO bajo esa raíz, aunque el {@code .jasper} ya exista — para cuando cambia la
 * versión de JasperReports o el propio harness, no para el uso diario.</li>
 * </ul>
 *
 * <p>Termina con código de salida distinto de cero si algún reporte falló, para que el commit
 * no siga adelante con un {@code .jrxml} que no compila.</p>
 */
public final class CompilarJasper {

    private CompilarJasper() {
    }

    public static void main(String[] args) throws IOException {
        boolean forzar = false;
        Path raizOverride = null;
        List<Path> explicitos = new ArrayList<>();
        for (String arg : args) {
            if ("--forzar".equals(arg)) {
                forzar = true;
                continue;
            }
            Path p = Path.of(arg);
            if (Files.isDirectory(p)) {
                raizOverride = p;
            } else if (arg.endsWith(".jrxml")) {
                explicitos.add(p);
            } else {
                System.out.println("Argumento no reconocido (ni carpeta ni .jrxml): " + arg);
            }
        }

        // net.sf.jasperreports.compiler.class no tiene extensión auto-registrada en el jar de
        // jasperreports-jdt (verificado: no trae META-INF/services ni jasperreports_extension.
        // properties) -- hay que fijar la propiedad a mano para que el motor use JRJdtCompiler
        // en vez de intentar JRJavacCompiler (que en el WAR, dentro de WildFly, es justamente
        // el que no funciona: sin javac externo ni acceso al classloader del deployment).
        System.setProperty("net.sf.jasperreports.compiler.class", "net.sf.jasperreports.jdt.JRJdtCompiler");

        List<Path> jrxmls;
        boolean recompilarSiempre = forzar;
        if (!explicitos.isEmpty()) {
            // Una ruta de archivo pasada a mano siempre se recompila -- es el caso de
            // "acabo de editar este reporte", no un barrido general.
            jrxmls = explicitos;
            recompilarSiempre = true;
        } else {
            Path raiz = raizOverride != null ? raizOverride : Path.of("src/main/resources/rep");
            if (!Files.isDirectory(raiz)) {
                System.out.println("No existe el directorio " + raiz.toAbsolutePath()
                        + " -- corré este harness desde la raíz del repo (o pasale una ruta valida).");
                System.exit(2);
                return;
            }
            jrxmls = new ArrayList<>();
            try (Stream<Path> paths = Files.walk(raiz)) {
                paths.filter(p -> p.toString().endsWith(".jrxml")).sorted().forEach(jrxmls::add);
            }
            if (jrxmls.isEmpty()) {
                System.out.println("No se encontró ningún .jrxml bajo " + raiz.toAbsolutePath() + ".");
                return;
            }
        }

        int compilados = 0;
        int alDia = 0;
        int fallidos = 0;
        for (Path jrxml : jrxmls) {
            Path jasper = Path.of(jrxml.toString().substring(0, jrxml.toString().length() - ".jrxml".length())
                    + ".jasper");
            boolean falta = !Files.exists(jasper);
            if (!recompilarSiempre && !falta) {
                alDia++;
                continue;
            }
            System.out.println("Compilando " + jrxml + " ...");
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
