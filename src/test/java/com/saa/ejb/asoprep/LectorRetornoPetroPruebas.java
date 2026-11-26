package com.saa.ejb.asoprep;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.asoprep.serviceImpl.LectorRetornoPetro;
import com.saa.ejb.asoprep.serviceImpl.ResultadoLecturaRetornoPetro;
import com.saa.model.crd.ParticipeXCargaArchivo;

/**
 * C46: pruebas de {@link LectorRetornoPetro} sin JUnit ni red (el repo no tiene framework de pruebas). Fragmentos sintéticos del formato posicional de
 * Petro: ancho fijo, roles 1001/1002, nombres «PARTICIPE UNO», sin datos reales. Sale con código 1 si algo falla.
 * Correr con: {@code sh tools/pruebas/correr-pruebas-c46.sh}
 */
public class LectorRetornoPetroPruebas {

    private static int fallos = 0;
    private static int pruebas = 0;

    // ------------------------------------------------------------------ constructores de líneas

    private static void pone(StringBuilder sb, int finExclusivo, String texto) {
        int inicio = finExclusivo - texto.length();
        for (int k = 0; k < texto.length(); k++) {
            sb.setCharAt(inicio + k, texto.charAt(k));
        }
    }

    private static StringBuilder vacia(int largo) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < largo; k++) {
            sb.append(' ');
        }
        return sb;
    }

    /** Línea de partícipe (rol y nombre vacíos si es continuación) con los montos en las columnas del reporte. */
    private static String fila(String rol, String nombre, String monto) {
        StringBuilder sb = vacia(198);
        pone(sb, 7, rol);
        for (int k = 0; k < nombre.length(); k++) {
            sb.setCharAt(8 + k, nombre.charAt(k));
        }
        pone(sb, 50, "1");
        pone(sb, 61, "500,00");
        pone(sb, 65, "1");
        pone(sb, 70, "0,00");
        pone(sb, 80, "0,000000");
        pone(sb, 95, monto);
        pone(sb, 110, monto);
        pone(sb, 125, "0,00");
        pone(sb, 140, "0,00");
        pone(sb, 155, monto);
        pone(sb, 170, "0,00");
        pone(sb, 184, "0,00");
        pone(sb, 198, "0,00");
        return sb.toString();
    }

    /** Línea «TOTAL xxx» con ':' y los montos en las mismas columnas (sección, zona o empleado). */
    private static String total(String etiqueta, String monto) {
        StringBuilder sb = vacia(198);
        String texto = "TOTAL " + etiqueta;
        for (int k = 0; k < texto.length(); k++) {
            sb.setCharAt(33 + k, texto.charAt(k));
        }
        sb.setCharAt(78, ':');
        pone(sb, 95, monto);
        pone(sb, 110, monto);
        pone(sb, 125, "0,00");
        pone(sb, 140, "0,00");
        pone(sb, 155, monto);
        pone(sb, 170, "0,00");
        pone(sb, 184, "0,00");
        pone(sb, 198, "0,00");
        return sb.toString();
    }

    private static String totalFinal(String monto) {
        StringBuilder sb = vacia(105);
        String texto = "TOTAL ==>";
        for (int k = 0; k < texto.length(); k++) {
            sb.setCharAt(48 + k, texto.charAt(k));
        }
        pone(sb, 105, monto);
        return sb.toString();
    }

    /** Encabezado de página: 8 líneas (la primera empieza con EP), la línea de producto y la de «====». */
    private static List<String> pagina(String codigo, String descripcion) {
        List<String> l = new ArrayList<String>();
        l.add("  EP PETROECUADOR");
        l.add("  GESTION TALENTO HUMANO");
        l.add("");
        l.add("  SUBSISTEMA DE ROL DE PAGOS   DESCUENTOS  JUBILACION ASOPREP");
        l.add(" ***********************************************************************");
        l.add("  ROL   APELLIDOS Y NOMBRES              PLAZO   SALDO ACTUAL   MESES %INTER");
        l.add("                                         INICIAL                PLAZO  ANUAL");
        l.add(" ***********************************************************************");
        l.add("  " + codigo + "  " + descripcion);
        l.add("  ============================================");
        return l;
    }

    private static String une(List<String> lineas) {
        StringBuilder sb = new StringBuilder();
        for (String s : lineas) {
            sb.append(s).append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ aserciones

    private static void verifica(String nombre, boolean condicion) {
        pruebas++;
        if (!condicion) {
            fallos++;
            System.out.println("  FALLA: " + nombre);
        } else {
            System.out.println("  ok: " + nombre);
        }
    }

    private static boolean igual(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

    private static long cuentaRol(List<ParticipeXCargaArchivo> registros, long rol, String producto) {
        long n = 0;
        for (ParticipeXCargaArchivo r : registros) {
            if (r.getCodigoPetro() != null && r.getCodigoPetro().longValue() == rol
                && producto.equals(r.getDetalleCargaArchivo().getCodigoPetroProducto())) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ casos

    /** AH (1001: 100,00; 1002: 50,00) y PE (1001: 30,00); todo con su TOTAL. Opcionalmente la PE trae la segunda línea sin rol. */
    private static List<String> archivoBase(boolean conContinuacion, String totalEmpleado, String subtotalPe, String totalGeneral) {
        List<String> l = new ArrayList<String>(pagina("AH", "APORTE VOLUNTARIO"));
        l.add(fila("1001", "PARTICIPE UNO", "100,00"));
        l.add(fila("1002", "PARTICIPE DOS", "50,00"));
        l.add(total("APORTE VOLUNTARIO", "150,00"));
        l.addAll(pagina("PE", "PRESTAMO ESPECIAL"));
        l.add(fila("1001", "PARTICIPE UNO", "30,00"));
        if (conContinuacion) {
            l.add(fila("", "", "177,00"));
            l.add(total("EMPLEADO", totalEmpleado));
        }
        l.add(total("PRESTAMO ESPECIAL", subtotalPe));
        l.add(total("ZONA CENTRO NORTE", totalGeneral));
        l.add(totalFinal(totalGeneral));
        return l;
    }

    private static void casoA() {
        System.out.println("(a) archivo normal que cuadra");
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(archivoBase(false, null, "30,00", "180,00")));
        verifica("3 registros", r.getRegistros().size() == 3);
        verifica("sin continuaciones", r.getContinuaciones() == 0);
        verifica("sin alerta", r.getAlertaCuadre() == null);
        verifica("suma = TOTAL ==>", r.getTotalGeneralPetro() != null && igual(r.getSumaTotalCargada(), r.getTotalGeneralPetro().doubleValue()));
        verifica("dos secciones", r.getSecciones().size() == 2);
    }

    private static void casoB() {
        System.out.println("(b) segunda linea sin rol + TOTAL EMPLEADO");
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(archivoBase(true, "207,00", "207,00", "357,00")));
        verifica("4 registros", r.getRegistros().size() == 4);
        verifica("dos registros con el mismo rol y producto en PE", cuentaRol(r.getRegistros(), 1001L, "PE") == 2);
        verifica("una continuacion", r.getContinuaciones() == 1);
        boolean montosPropios = false;
        boolean nombreHeredado = false;
        for (ParticipeXCargaArchivo p : r.getRegistros()) {
            if ("PE".equals(p.getDetalleCargaArchivo().getCodigoPetroProducto()) && p.getTotalDescontado() != null && igual(p.getTotalDescontado(), 177.0)) {
                montosPropios = true;
                nombreHeredado = "PARTICIPE UNO".equals(p.getNombre()) && p.getCodigoPetro().longValue() == 1001L;
            }
        }
        verifica("la continuacion trae sus montos propios (177,00)", montosPropios);
        verifica("la continuacion hereda nombre y rol del participe anterior", nombreHeredado);
        verifica("cuadra, sin alerta", r.getAlertaCuadre() == null);
        verifica("suma = TOTAL ==> (357,00)", r.getTotalGeneralPetro() != null && igual(r.getSumaTotalCargada(), 357.0));
    }

    private static void casoC() {
        System.out.println("(c) subtotal de seccion que no cuadra (+20,00)");
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(archivoBase(false, null, "50,00", "200,00")));
        String alerta = r.getAlertaCuadre();
        verifica("hay alerta", alerta != null);
        verifica("nombra la seccion PE (PRESTAMO ESPECIAL)", alerta != null && alerta.contains("PE (PRESTAMO ESPECIAL)"));
        verifica("subtotal 50,00, cargado 30,00, diferencia 20,00", alerta != null && alerta.contains("subtotal 50,00, cargado 30,00, diferencia 20,00"));
        verifica("TOTAL ==> con diferencia 20,00", alerta != null && alerta.contains("TOTAL ==> 200,00, cargado 180,00, diferencia 20,00"));
        verifica("la carga no falla: 3 registros", r.getRegistros().size() == 3);
        verifica("AH cuadra y no aparece", alerta != null && !alerta.contains("APORTE VOLUNTARIO"));
    }

    private static void casoD() {
        System.out.println("(d) continuacion despues de un salto de pagina EP en la misma seccion");
        List<String> l = new ArrayList<String>(pagina("PE", "PRESTAMO ESPECIAL"));
        l.add(fila("1001", "PARTICIPE UNO", "113,00"));
        l.addAll(pagina("PE", "PRESTAMO ESPECIAL"));
        l.add(fila("", "", "177,00"));
        l.add(total("EMPLEADO", "290,00"));
        l.add(fila("1002", "PARTICIPE DOS", "10,00"));
        l.add(total("PRESTAMO ESPECIAL", "300,00"));
        l.add(total("ZONA CENTRO NORTE", "300,00"));
        l.add(totalFinal("300,00"));
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(l));
        verifica("3 registros", r.getRegistros().size() == 3);
        verifica("la continuacion toma el rol 1001 tras el salto de pagina", cuentaRol(r.getRegistros(), 1001L, "PE") == 2);
        verifica("cuadra, sin alerta", r.getAlertaCuadre() == null);

        // Cambia el producto entre la pagina del participe y la continuacion: no se adivina el rol.
        List<String> m = new ArrayList<String>(pagina("PE", "PRESTAMO ESPECIAL"));
        m.add(fila("1001", "PARTICIPE UNO", "113,00"));
        m.addAll(pagina("PH", "PRESTAMO HIPOTECARIO"));
        m.add(fila("", "", "177,00"));
        boolean error = false;
        String mensaje = "";
        try {
            LectorRetornoPetro.leer(une(m));
        } catch (IllegalArgumentException e) {
            error = true;
            mensaje = e.getMessage();
        }
        verifica("continuacion sin participe previo en la seccion -> error", error);
        verifica("el error trae el numero de linea y la linea cruda", mensaje.contains("línea 22") && mensaje.contains("Línea cruda"));
    }

    private static void casoE() {
        System.out.println("(e) TOTAL EMPLEADO que no cuadra");
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(archivoBase(true, "999,00", "207,00", "357,00")));
        String alerta = r.getAlertaCuadre();
        verifica("hay alerta", alerta != null);
        verifica("menciona TOTAL EMPLEADO con 999,00 contra 207,00", alerta != null && alerta.contains("TOTAL EMPLEADO 999,00") && alerta.contains("207,00"));
        verifica("las secciones y el total si cuadran: no hay diferencia de seccion", alerta != null && !alerta.contains("subtotal") && !alerta.contains("TOTAL ==> "));
        verifica("la carga no falla: 4 registros", r.getRegistros().size() == 4);
    }

    private static void casoF() {
        System.out.println("(f) sin TOTAL de seccion ni TOTAL ==>");
        List<String> l = new ArrayList<String>(pagina("AH", "APORTE VOLUNTARIO"));
        l.add(fila("1001", "PARTICIPE UNO", "100,00"));
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(l));
        String alerta = r.getAlertaCuadre();
        verifica("alerta por seccion sin TOTAL", alerta != null && alerta.contains("sin línea TOTAL"));
        verifica("alerta por falta de TOTAL ==>", alerta != null && alerta.contains("no trae la línea TOTAL ==>"));
        verifica("montos con punto de miles se leen (165.569,33)", montoConMiles());
    }

    private static boolean montoConMiles() {
        List<String> l = new ArrayList<String>(pagina("AH", "APORTE VOLUNTARIO"));
        l.add(fila("1001", "PARTICIPE UNO", "165.569,33"));
        l.add(total("APORTE VOLUNTARIO", "165.569,33"));
        l.add(totalFinal("165.569,33"));
        ResultadoLecturaRetornoPetro r = LectorRetornoPetro.leer(une(l));
        return r.getAlertaCuadre() == null && igual(r.getSumaTotalCargada(), 165569.33) && r.getTotalGeneralPetro() != null
            && igual(r.getTotalGeneralPetro().doubleValue(), 165569.33);
    }

    public static void main(String[] args) {
        casoA();
        casoB();
        casoC();
        casoD();
        casoE();
        casoF();
        System.out.println();
        System.out.println(pruebas + " verificaciones, " + fallos + " fallos");
        if (fallos > 0) {
            System.exit(1);
        }
    }
}
