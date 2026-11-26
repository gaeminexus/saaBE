package com.saa.ejb.asoprep.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.asoprep.serviceImpl.ResultadoLecturaRetornoPetro.SeccionCuadre;
import com.saa.model.crd.DetalleCargaArchivo;
import com.saa.model.crd.ParticipeXCargaArchivo;

/**
 * C46: lector del reporte posicional de retorno de Petrocomercial (antes {@code CargaArchivoPetroServiceImpl.procesarContenido}). Clase pura, sin EJB ni BD:
 * recibe el contenido y devuelve las filas y el cuadre contra los TOTAL del propio reporte.
 *
 * <p>Formato (columnas, base 0): rol [0,7), nombre [7,44), plazo [44,50), saldo [50,61), meses [61,65), interés [65,70), seguro [70,80), monto a descontar
 * [80,95), capital [95,110), interés [110,125), seguro [125,140), total descontado [140,155), no descontado [155,170), [170,184), [184,198).
 *
 * <p>Lo que se agregó respecto del lector anterior (que saltaba toda línea sin rol como «resumen»):
 * <ul>
 * <li><b>Línea de continuación</b>: cuando un partícipe tiene dos descuentos en el mismo producto, Petro imprime la segunda línea con rol y nombre en blanco
 * pero con todas las columnas numéricas, y debajo una línea «TOTAL EMPLEADO». Esa segunda línea es un registro más, con el rol del último partícipe leído de la
 * MISMA sección; «TOTAL EMPLEADO» se valida contra la suma de las líneas del partícipe.</li>
 * <li><b>Cuadre</b>: por sección, Σ total descontado cargado contra su «TOTAL &lt;producto&gt;»; y la Σ de todo contra «TOTAL ==&gt;». Tolerancia 0,005.
 * Un descuadre NO falla la lectura (hay descuadres del propio Petro): queda en {@link ResultadoLecturaRetornoPetro#getAlertaCuadre()}.</li>
 * </ul>
 */
public class LectorRetornoPetro {

    private static final String NUMERO = "-?[0-9.]*[0-9](,[0-9]+)?";

    private final ResultadoLecturaRetornoPetro resultado = new ResultadoLecturaRetornoPetro();
    private final List<ParticipeXCargaArchivo> registros = new ArrayList<ParticipeXCargaArchivo>();

    // Estado del último partícipe leído en la sección actual: sobrevive al salto de página (EP) y se reinicia al cambiar de producto.
    private String codigoSeccionActual;
    private Long ultimoRol;
    private String ultimoNombre;
    private double sumaParticipe;

    private LectorRetornoPetro() {
    }

    /** Lee el contenido completo del retorno (líneas separadas por salto de línea). */
    public static ResultadoLecturaRetornoPetro leer(String contenido) {
        return new LectorRetornoPetro().procesa(contenido);
    }

    private ResultadoLecturaRetornoPetro procesa(String contenido) {
        String[] lineas = contenido.split("\n");
        for (int k = 0; k < lineas.length; k++) {
            if (lineas[k].endsWith("\r")) {
                lineas[k] = lineas[k].substring(0, lineas[k].length() - 1);
            }
        }
        int i = 0;
        boolean encontroEP = false;

        while (i < lineas.length) {
            String lineaActual = lineas[i];

            if (lineaActual != null && lineaActual.trim().startsWith("EP")) {
                encontroEP = true;
                i += 8;
                if (i >= lineas.length) break;

                String lineaAporte = lineas[i];
                if (lineaAporte == null || lineaAporte.trim().isEmpty()) {
                    throw new IllegalArgumentException("Error en línea " + (i + 1) + ": Se esperaba línea de producto/aporte después del encabezado EP");
                }
                String codigoAporte = lineaAporte.substring(0, Math.min(4, lineaAporte.length())).trim();
                String descripcionAporte = lineaAporte.length() > 4 ? lineaAporte.substring(4).trim() : "";

                if (!codigoAporte.equals(codigoSeccionActual)) {
                    // Cambió el producto: el «último rol» de la sección anterior no vale aquí.
                    codigoSeccionActual = codigoAporte;
                    ultimoRol = null;
                    ultimoNombre = null;
                    sumaParticipe = 0.0;
                }
                // La última página del reporte es un resumen por producto (CODDSC/CODRAZ/VALOR) con un encabezado más corto: su «línea de producto» no trae
                // código y no es una sección. Se lee igual que siempre (sus líneas no tienen rol y se cuentan como resumen) pero no abre una sección de cuadre.
                if (!codigoAporte.isEmpty()) {
                    resultado.seccion(codigoAporte, descripcionAporte);
                }

                i++;
                i++;
                if (i >= lineas.length) break;

                while (i < lineas.length) {
                    String lineaRegistro = lineas[i];
                    if (lineaRegistro != null && lineaRegistro.trim().startsWith("EP")) {
                        break;
                    }
                    if (lineaRegistro != null && lineaRegistro.trim().length() > 0) {
                        procesaLinea(lineaRegistro, i + 1, codigoAporte, descripcionAporte);
                    }
                    i++;
                }
                continue;
            }
            i++;
        }

        if (!encontroEP) {
            throw new IllegalArgumentException("El archivo no contiene ningún encabezado 'EP' válido. Formato de archivo incorrecto.");
        }
        resultado.setRegistros(registros);
        return resultado;
    }

    private void procesaLinea(String lineaRegistro, int numeroLinea, String codigoAporte, String descripcionAporte) {
        String codigo = extraerCampo(lineaRegistro, 0, 7).trim();
        boolean continuacion = false;

        if (codigo.isEmpty()) {
            if (lineaRegistro.contains("TOTAL")) {
                procesaTotal(lineaRegistro, numeroLinea, codigoAporte);
                resultado.cuentaResumen();
                return;
            }
            if (!esContinuacion(lineaRegistro)) {
                resultado.cuentaResumen();
                return;
            }
            if (ultimoRol == null) {
                throw new IllegalArgumentException("Error al procesar línea " + numeroLinea + " del producto '" + descripcionAporte
                    + "': línea sin rol ni nombre (continuación de un partícipe) pero la sección no tiene un partícipe previo; no se adivina el rol"
                    + " | Línea cruda: \"" + recorta(lineaRegistro) + "\"");
            }
            continuacion = true;
        }

        try {
            ParticipeXCargaArchivo registro = new ParticipeXCargaArchivo();
            DetalleCargaArchivo detalleTemp = new DetalleCargaArchivo();
            detalleTemp.setCodigoPetroProducto(codigoAporte);
            detalleTemp.setNombreProductoPetro(descripcionAporte);
            registro.setDetalleCargaArchivo(detalleTemp);

            if (lineaRegistro.length() < 50) {
                throw new IllegalArgumentException("Línea muy corta, longitud mínima esperada: 50 caracteres");
            }

            registro.setNombre(continuacion ? ultimoNombre : extraerCampo(lineaRegistro, 7, 44).trim());
            registro.setPlazoInicial(parseDouble(extraerCampo(lineaRegistro, 44, 50).trim()).longValue());
            registro.setSaldoActual(parseDouble(extraerCampo(lineaRegistro, 50, 61).trim()));
            registro.setMesesPlazo(parseDouble(extraerCampo(lineaRegistro, 61, 65).trim()).longValue());
            registro.setInteresAnual(parseDouble(extraerCampo(lineaRegistro, 65, 70).trim()));
            registro.setValorSeguro(parseDouble(extraerCampo(lineaRegistro, 70, 80).trim()));
            registro.setMontoDescontar(parseDouble(extraerCampo(lineaRegistro, 80, 95).trim()));
            registro.setCapitalDescontado(parseDouble(extraerCampo(lineaRegistro, 95, 110).trim()));
            registro.setInteresDescontado(parseDouble(extraerCampo(lineaRegistro, 110, 125).trim()));
            registro.setSeguroDescontado(parseDouble(extraerCampo(lineaRegistro, 125, 140).trim()));
            registro.setTotalDescontado(parseDouble(extraerCampo(lineaRegistro, 140, 155).trim()));
            registro.setCapitalNoDescontado(parseDouble(extraerCampo(lineaRegistro, 155, 170).trim()));
            registro.setInteresNoDescontado(parseDouble(extraerCampo(lineaRegistro, 170, 184).trim()));
            registro.setDesgravamenNoDescontado(parseDouble(extraerCampo(lineaRegistro, 184, 198).trim()));

            registro.setCodigoPetro(continuacion ? ultimoRol : parseLongSimple(codigo));
            registros.add(registro);

            double total = registro.getTotalDescontado() != null ? registro.getTotalDescontado().doubleValue() : 0.0;
            resultado.seccion(codigoAporte, descripcionAporte).suma(total);
            resultado.sumaTotal(total);
            if (continuacion) {
                sumaParticipe += total;
                resultado.cuentaContinuacion();
            } else {
                ultimoRol = registro.getCodigoPetro();
                ultimoNombre = registro.getNombre();
                sumaParticipe = total;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Error al procesar línea " + numeroLinea + " del producto '" + descripcionAporte
                + "' (columnas de dato: 0-198): " + e.getMessage() + " | Línea cruda: \"" + recorta(lineaRegistro) + "\"", e);
        }
    }

    /** Línea sin rol ni nombre, sin la palabra TOTAL, con plazo, monto a descontar y total descontado numéricos. */
    private static boolean esContinuacion(String linea) {
        if (!extraerCampo(linea, 7, 44).trim().isEmpty() || linea.length() < 50) {
            return false;
        }
        return extraerCampo(linea, 44, 50).trim().matches(NUMERO)
            && extraerCampo(linea, 80, 95).trim().matches(NUMERO)
            && extraerCampo(linea, 140, 155).trim().matches(NUMERO);
    }

    /** Línea TOTAL: empleado (se valida), de zona (se ignora), de sección (subtotal) o final «TOTAL ==&gt;». */
    private void procesaTotal(String linea, int numeroLinea, String codigoAporte) {
        int flecha = linea.indexOf("==>");
        if (flecha >= 0) {
            String valor = linea.substring(flecha + 3).trim();
            try {
                resultado.setTotalGeneralPetro(Double.valueOf(parseDouble(valor)));
            } catch (Exception e) {
                resultado.getOtrasAlertas().add("línea " + numeroLinea + ": no se pudo leer el valor de TOTAL ==> ('" + valor + "')");
            }
            return;
        }
        int posTotal = linea.indexOf("TOTAL");
        int posDosPuntos = linea.indexOf(':', posTotal);
        String etiqueta = normaliza(linea.substring(posTotal + 5, posDosPuntos >= 0 ? posDosPuntos : linea.length()));
        String campo = extraerCampo(linea, 140, 155).trim();

        if (etiqueta.equals("EMPLEADO")) {
            if (!campo.matches(NUMERO)) {
                return;
            }
            double petro = parseDouble(campo);
            if (Math.abs(petro - sumaParticipe) > ResultadoLecturaRetornoPetro.TOLERANCIA) {
                resultado.getOtrasAlertas().add("línea " + numeroLinea + " (" + codigoAporte + "): TOTAL EMPLEADO "
                    + ResultadoLecturaRetornoPetro.formato(petro) + " no coincide con la suma de las líneas del partícipe "
                    + ResultadoLecturaRetornoPetro.formato(sumaParticipe));
            }
            return;
        }
        if (etiqueta.startsWith("ZONA")) {
            return;
        }
        SeccionCuadre seccion = buscaSeccion(etiqueta);
        if (seccion == null || !campo.matches(NUMERO)) {
            resultado.getOtrasAlertas().add("línea " + numeroLinea + ": la línea TOTAL '" + etiqueta
                + (seccion == null ? "' no corresponde a ninguna sección del archivo" : "' no trae el total descontado"));
            return;
        }
        seccion.setSubtotalPetro(Double.valueOf(parseDouble(campo)));
    }

    private SeccionCuadre buscaSeccion(String etiqueta) {
        SeccionCuadre aproximada = null;
        for (SeccionCuadre s : resultado.getSecciones()) {
            String descripcion = normaliza(s.getDescripcion());
            if (descripcion.equals(etiqueta)) {
                return s;
            }
            if (aproximada == null && !descripcion.isEmpty() && (descripcion.startsWith(etiqueta) || etiqueta.startsWith(descripcion))) {
                aproximada = s;
            }
        }
        return aproximada;
    }

    private static String normaliza(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private static String recorta(String linea) {
        return linea.length() > 120 ? linea.substring(0, 120) + "..." : linea;
    }

    // Mismos criterios que tenía CargaArchivoPetroServiceImpl: vacío es 0 legítimo, mal formado propaga.
    private static String extraerCampo(String linea, int inicio, int fin) {
        if (linea == null) return "";
        StringBuilder completa = new StringBuilder(linea);
        while (completa.length() < fin) {
            completa.append(" ");
        }
        return completa.substring(inicio, Math.min(fin, completa.length()));
    }

    private static Double parseDouble(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0.0;
        String limpio = valor.trim().replaceAll("\\s", "");
        boolean tieneComa = limpio.contains(",");
        boolean tienePunto = limpio.contains(".");
        if (tieneComa && tienePunto) {
            limpio = limpio.replace(".", "").replace(",", ".");
        } else if (tieneComa) {
            limpio = limpio.replace(",", ".");
        }
        try {
            return Double.parseDouble(limpio);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico mal formado: '" + valor + "'", e);
        }
    }

    private static Long parseLongSimple(String valor) {
        if (valor == null || valor.trim().isEmpty()) return 0L;
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Valor numérico mal formado: '" + valor + "'", e);
        }
    }
}
