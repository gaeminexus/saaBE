package com.saa.ejb.crd.firma;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Proveedor Security Data (DatAgree). FASE 1: la API del proveedor se desconoce (su documentación no está disponible), así
 * que NO se inventa ningún endpoint ni formato: cada operación verifica la configuración y después lanza
 * {@code PROVEEDOR_NO_DISPONIBLE}. La fase 2 completa los métodos con su documentación.
 *
 * <p>Configuración: propiedades de sistema de WildFly, sin valores por defecto (nunca en el código ni en el repo):
 * {@code saa.firma.securitydata.url}, {@code .usuario}, {@code .clave} y {@code .ruc}. Si falta alguna:
 * {@code PROVEEDOR_NO_CONFIGURADO} con el nombre de la que falta (nunca su valor). La clave y el token no se escriben en
 * ningún log ni mensaje.</p>
 */
public class SecurityDataProveedor implements ProveedorFirmaElectronica {

    public static final String NOMBRE = "SECURITY_DATA";

    static final String PROP_URL = "saa.firma.securitydata.url";
    static final String PROP_USUARIO = "saa.firma.securitydata.usuario";
    static final String PROP_CLAVE = "saa.firma.securitydata.clave";
    static final String PROP_RUC = "saa.firma.securitydata.ruc";

    private static final String PENDIENTE = "integración con Security Data pendiente de su documentación";

    @Override
    public String nombre() {
        return NOMBRE;
    }

    @Override
    public ResultadoEnvio enviar(byte[] documentoPdf, String nombreDocumento, DatosParticipe participe) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.enviar - documento: " + nombreDocumento);
        configuracion();
        throw noDisponible();
    }

    @Override
    public EstadoTramite consultar(String idExterno) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.consultar - idExterno: " + idExterno);
        configuracion();
        throw noDisponible();
    }

    @Override
    public byte[] descargarFirmado(String idExterno) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.descargarFirmado - idExterno: " + idExterno);
        configuracion();
        throw noDisponible();
    }

    @Override
    public void anular(String idExterno, String motivo) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.anular - idExterno: " + idExterno);
        configuracion();
        throw noDisponible();
    }

    private ProveedorFirmaException noDisponible() {
        return new ProveedorFirmaException(ProveedorFirmaException.PROVEEDOR_NO_DISPONIBLE, PENDIENTE);
    }

    /**
     * Lee la configuración de las propiedades de sistema. Si falta alguna lanza {@code PROVEEDOR_NO_CONFIGURADO} nombrando
     * TODAS las que faltan (solo los nombres).
     */
    private Configuracion configuracion() throws ProveedorFirmaException {
        String url = propiedad(PROP_URL);
        String usuario = propiedad(PROP_USUARIO);
        String clave = propiedad(PROP_CLAVE);
        String ruc = propiedad(PROP_RUC);

        StringBuilder faltan = new StringBuilder();
        agregarSiFalta(faltan, PROP_URL, url);
        agregarSiFalta(faltan, PROP_USUARIO, usuario);
        agregarSiFalta(faltan, PROP_CLAVE, clave);
        agregarSiFalta(faltan, PROP_RUC, ruc);
        if (faltan.length() > 0) {
            throw new ProveedorFirmaException(ProveedorFirmaException.PROVEEDOR_NO_CONFIGURADO,
                "faltan propiedades de sistema de WildFly: " + faltan);
        }
        return new Configuracion(url, usuario, clave, ruc);
    }

    private static String propiedad(String nombre) {
        String valor = System.getProperty(nombre);
        return valor == null || valor.trim().isEmpty() ? null : valor.trim();
    }

    private static void agregarSiFalta(StringBuilder faltan, String nombre, String valor) {
        if (valor == null) {
            if (faltan.length() > 0) {
                faltan.append(", ");
            }
            faltan.append(nombre);
        }
    }

    /** Cliente HTTP del JDK (sin dependencias nuevas) para la fase 2; hoy ningún método lo usa todavía. */
    @SuppressWarnings("unused")
    private static HttpClient clienteHttp() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    /** Configuración leída. La clave existe solo en memoria: el toString no la incluye. */
    @SuppressWarnings("unused")
    private static final class Configuracion {
        private final String url;
        private final String usuario;
        private final String clave;
        private final String ruc;

        Configuracion(String url, String usuario, String clave, String ruc) {
            this.url = url;
            this.usuario = usuario;
            this.clave = clave;
            this.ruc = ruc;
        }

        @Override
        public String toString() {
            return "Configuracion[url=" + url + ", usuario=" + usuario + ", ruc=" + ruc + "]";
        }
    }
}
