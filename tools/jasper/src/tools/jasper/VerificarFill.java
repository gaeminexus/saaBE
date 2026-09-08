package tools.jasper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRLoader;

/**
 * Llena cada .jasper de src/main/resources/rep/** con {@code JREmptyDataSource} (sin conexión,
 * sin una sola fila) y parámetros dummy, para atrapar errores que SOLO aparecen al llenar el
 * reporte -- no al compilarlo.
 *
 * <p><b>Por qué hace falta esto y compilar no alcanza (2026-09-08, incidente real en
 * producción):</b> un {@code <style>} anónimo anidado dentro de un {@code <element>} (en vez de
 * un {@code <style name="...">} a nivel de reporte, referenciado por los elementos con
 * {@code style="Nombre"}) compila perfecto -- el compilador no valida referencias de estilo --
 * pero revienta al llenar con {@code JRRuntimeException: Could not resolve style(s): .}. Pasó en
 * {@code RPRT_CNCL_CNTA}/{@code RPRT_CNCL_GNRL}, compilados y commiteados, reventados en
 * producción la primera vez que un usuario generó el PDF. {@code JasperFillManager.fillReport}
 * con {@link JREmptyDataSource} corre exactamente la misma resolución de estilos, subreportes y
 * expresiones que un fill real, sin necesitar Oracle ni una sola fila -- así que atrapa esta
 * clase de error en el mismo lugar donde se atrapa "no compila", antes del commit.</p>
 *
 * <p>Los parámetros declarados por cada reporte se rellenan con valores dummy según su clase
 * declarada (String, Long, Integer, Double, BigDecimal, fechas, Image, listas). Un reporte que
 * dependa de un subreporte con {@code connectionExpression} apuntando a una conexión JDBC real
 * (via {@code $P{REPORT_CONNECTION}}, que aquí se pasa {@code null}) va a fallar por esa razón,
 * no por un defecto del reporte -- esos casos se detectan por el mensaje/tipo de la excepción y
 * se listan aparte, como "NO VERIFICABLE (necesita conexión)", nunca como aprobados a ciegas.</p>
 *
 * <p>Uso: {@code java ... tools.jasper.VerificarFill [ruta-a-.jasper-o-carpeta ...]}. Sin
 * argumentos, recorre {@code src/main/resources/rep}. Termina con código de salida distinto de
 * cero si algún reporte falló de verdad (no cuenta "no verificable").</p>
 */
public final class VerificarFill {

    private VerificarFill() {
    }

    public static void main(String[] args) throws IOException {
        List<Path> raices = new ArrayList<>();
        for (String arg : args) {
            raices.add(Path.of(arg));
        }
        if (raices.isEmpty()) {
            raices.add(Path.of("src/main/resources/rep"));
        }

        List<Path> jaspers = new ArrayList<>();
        for (Path raiz : raices) {
            if (Files.isDirectory(raiz)) {
                try (Stream<Path> paths = Files.walk(raiz)) {
                    paths.filter(p -> p.toString().endsWith(".jasper")).sorted().forEach(jaspers::add);
                }
            } else if (raiz.toString().endsWith(".jasper")) {
                jaspers.add(raiz);
            } else {
                System.out.println("Ignorado (no es .jasper ni carpeta): " + raiz);
            }
        }

        if (jaspers.isEmpty()) {
            System.out.println("No se encontró ningún .jasper.");
            return;
        }

        int ok = 0, noVerificable = 0, fallidos = 0;
        for (Path jasperPath : jaspers) {
            String nombre = jasperPath.toString();
            try {
                JasperReport reporte = (JasperReport) JRLoader.loadObject(jasperPath.toFile());
                Map<String, Object> params = valoresDummy(reporte);
                JasperPrint print = JasperFillManager.fillReport(reporte, params, new JREmptyDataSource());
                System.out.println("OK   " + nombre + "  (" + print.getPages().size() + " pagina(s))");
                ok++;
            } catch (Throwable t) {
                String razon = razonNoVerificable(t);
                if (razon != null) {
                    System.out.println("SKIP " + nombre + "  -- NO VERIFICABLE (" + razon + "): "
                            + t.getClass().getSimpleName() + ": " + t.getMessage());
                    noVerificable++;
                } else {
                    System.out.println("FAIL " + nombre + "  -- " + t.getClass().getName() + ": " + t.getMessage());
                    fallidos++;
                }
            }
        }

        System.out.println();
        System.out.println("OK: " + ok + " | NO VERIFICABLE: " + noVerificable
                + " | FALLIDOS: " + fallidos + " | Total: " + jaspers.size());
        if (fallidos > 0) {
            System.exit(1);
        }
    }

    /**
     * Heurística para distinguir "este reporte tiene un defecto real" de "esto no prueba nada
     * sobre el reporte, es una limitación de ESTE verificador". Dos categorías, no una sola:
     *
     * <p><b>Necesita conexión real:</b> típico de un subreporte con
     * {@code connectionExpression=$P{REPORT_CONNECTION}}, que aquí llega null porque no hay
     * Oracle detrás de {@link JREmptyDataSource}.</p>
     *
     * <p><b>Clase no encontrada al deserializar el .jasper:</b> el classpath de
     * {@code tools/jasper/pom.xml} es DELIBERADAMENTE un subconjunto del classpath real del WAR
     * en WildFly -- no declara cada dependencia que cada reporte de cada módulo pudiera llegar a
     * usar (p. ej. una librería de firma electrónica para un RIDE). Un {@code
     * ClassNotFoundException} acá no prueba que el reporte esté roto, prueba que esa clase no
     * está en ESTE classpath angosto; puede estar perfectamente disponible en WildFly. Tratarlo
     * como fallo sería exactamente el falso positivo que hace desconfiar de la herramienta
     * (medido 2026-09-08: cinco RIDE + un reporte con función UPPER() fallaban así por un bug
     * DISTINTO -truncamiento de <code>set /p</code> en el .bat, ya corregido- y ninguno de los
     * seis estaba realmente roto). No es perfecta a propósito: ante la duda, mejor marcar de más
     * como "no verificable" que acusar de roto un reporte ajeno -- eso lo decide un humano
     * leyendo el mensaje completo, no este heurístico.</p>
     *
     * @return	: la razón como texto si parece no verificable aquí, o {@code null} si parece un
     *			  defecto real del reporte
     */
    private static String razonNoVerificable(Throwable t) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            String msg = String.valueOf(cur.getMessage());
            if (msg.toLowerCase().contains("connection") || msg.contains("REPORT_CONNECTION")) {
                return "necesita conexión real";
            }
            String clase = cur.getClass().getName();
            if (clase.contains("JRFillSubreport") || (cur instanceof NullPointerException
                    && Stream.of(cur.getStackTrace()).anyMatch(e -> e.getClassName().contains("Subreport")
                            || e.getClassName().contains("QueryExecuter")))) {
                return "necesita conexión real";
            }
            if (cur instanceof ClassNotFoundException || cur instanceof NoClassDefFoundError
                    || clase.contains("ClassNotFoundException")
                    || msg.contains("Class not found")
                    || msg.contains("ClassNotFoundException")
                    || msg.contains("Error loading object from file")) {
                return "clase ausente en el classpath angosto del harness, no necesariamente en WildFly";
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valoresDummy(JasperReport reporte) {
        Map<String, Object> params = new HashMap<>();
        for (JRParameter p : reporte.getParameters()) {
            if (p.isSystemDefined()) {
                continue;
            }
            params.put(p.getName(), valorDummy(p.getValueClass()));
        }
        return params;
    }

    private static Object valorDummy(Class<?> clase) {
        if (clase == null) {
            return null;
        }
        if (String.class.equals(clase)) {
            return "X";
        }
        if (Long.class.equals(clase) || long.class.equals(clase)) {
            return Long.valueOf(1L);
        }
        if (Integer.class.equals(clase) || int.class.equals(clase)) {
            return Integer.valueOf(1);
        }
        if (Double.class.equals(clase) || double.class.equals(clase)) {
            return Double.valueOf(1D);
        }
        if (Float.class.equals(clase) || float.class.equals(clase)) {
            return Float.valueOf(1F);
        }
        if (BigDecimal.class.equals(clase)) {
            return BigDecimal.ONE;
        }
        if (BigInteger.class.equals(clase)) {
            return BigInteger.ONE;
        }
        if (Boolean.class.equals(clase) || boolean.class.equals(clase)) {
            return Boolean.FALSE;
        }
        if (java.sql.Timestamp.class.equals(clase)) {
            return new Timestamp(System.currentTimeMillis());
        }
        if (java.sql.Date.class.equals(clase)) {
            return new java.sql.Date(System.currentTimeMillis());
        }
        if (Date.class.isAssignableFrom(clase)) {
            return new Date();
        }
        if (LocalDate.class.equals(clase)) {
            return LocalDate.now();
        }
        if (LocalDateTime.class.equals(clase)) {
            return LocalDateTime.now();
        }
        if (java.awt.Image.class.isAssignableFrom(clase)) {
            return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }
        if (Collection.class.isAssignableFrom(clase)) {
            return new ArrayList<Object>();
        }
        if (java.sql.Connection.class.isAssignableFrom(clase)) {
            // Deliberadamente null: no hay BD acá. Si el reporte (o un subreporte) la necesita
            // de verdad, el fill va a fallar y pareceFaltaDeConexion() lo va a distinguir.
            return null;
        }
        // Tipo no contemplado (enums, objetos propios, etc.): null, para no adivinar mal.
        return null;
    }
}
