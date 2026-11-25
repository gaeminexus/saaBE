package com.saa.ejb.cxc.util;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

/**
 * Utilidad para crear conexiones HTTP/HTTPS al SRI.
 *
 * Lote 12 (VCN #9): por defecto la conexion usa TLS NORMAL: se valida la cadena del certificado contra el almacen de confianza de la JVM y el nombre
 * del servidor (cel.sri.gob.ec / celcer.sri.gob.ec). Medido el 2026-11-25: los dos presentan una hoja emitida por DigiCert Global G2 TLS RSA SHA256 2020
 * CA1 (raiz DigiCert Global Root G2, presente en el cacerts de JDK 21 y 8u261). Antes la clase aceptaba CUALQUIER certificado y CUALQUIER nombre (un
 * intermediario podia leer y alterar los comprobantes y la clave de la firma no viaja, pero si el XML firmado).
 *
 * VALVULA TRANSITORIA: con la propiedad de sistema {@code -Dsaa.sri.tlsPermisivo=true} se vuelve al comportamiento anterior (acepta todo) y se escribe un
 * AVISO FUERTE en el log en cada conexion. Solo para no quedarse sin emitir si la lista de control de la raiz falla en produccion; se retira en el ciclo
 * siguiente. Ver docs/logica-negocio/cxc/PROPUESTA-LOTE-12-VCN-CXC-SRI.md (lista de control).
 */
public class SriHttpUtil {

    /** Propiedad de sistema de la valvula transitoria (apagada por defecto). */
    public static final String PROP_TLS_PERMISIVO = "saa.sri.tlsPermisivo";

    private static boolean tlsPermisivo() {
        return Boolean.parseBoolean(System.getProperty(PROP_TLS_PERMISIVO, "false"));
    }


    /** Solo se construye si la valvula esta encendida: ver {@link #tlsPermisivo()}. */
    private static final class Permisivo {
    private static final SSLContext SSL_CONTEXT_SRI;

    static {
        try {
            // X509ExtendedTrustManager: la JVM lo llama con el SSLEngine activo,
            // lo que nos permite omitir la validación de hostname/IP sin interferir
            // con el resto del handshake TLS (cifrado, versión de protocolo, etc.)
            TrustManager[] trustSri = new TrustManager[] {
                new X509ExtendedTrustManager() {
                    // ── Métodos abstractos del contrato X509TrustManager ─────────
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        logCert(chain);
                    }
                    // ── Sobrecargas de X509ExtendedTrustManager ──────────────────
                    // Estas son las que invoca la JVM cuando hay un SSLEngine/SSLSocket activo.
                    // Al no lanzar excepción aceptamos cualquier certificado del servidor SRI.
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType,
                            java.net.Socket socket) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType,
                            java.net.Socket socket) {
                        logCert(chain);
                    }
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType,
                            SSLEngine engine) {}
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType,
                            SSLEngine engine) {
                        // Punto clave: esta sobrecarga es la que llama Java cuando
                        // HttpsURLConnection hace el handshake. No lanzar excepción = aceptar.
                        logCert(chain);
                    }
                    private void logCert(X509Certificate[] chain) {
                        if (chain != null && chain.length > 0) {
                            System.out.println("[SriHttpUtil] Certificado servidor SRI: "
                                    + chain[0].getSubjectX500Principal().getName());
                        }
                    }
                }
            };
            SSL_CONTEXT_SRI = SSLContext.getInstance("TLS");
            SSL_CONTEXT_SRI.init(null, trustSri, new java.security.SecureRandom());
        } catch (Exception e) {
            throw new RuntimeException(
                    "No se pudo inicializar SSLContext para SRI: " + e.getMessage(), e);
        }
    }
    }

    /** HostnameVerifier permisivo como segunda línea de defensa */
    private static final HostnameVerifier HOSTNAME_VERIFIER_SRI = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            System.out.println("[SriHttpUtil] HostnameVerifier: hostname=" + hostname
                    + " | peer=" + session.getPeerHost());
            return true;
        }
    };

    /**
     * Crea una HttpURLConnection configurada para tolerar el SSL del SRI.
     */
    public static HttpURLConnection crearConexion(String urlStr) throws Exception {
        String endpointUrl = urlStr.replace("?wsdl", "").replace("?WSDL", "");
        URL url = new URL(endpointUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (conn instanceof HttpsURLConnection && tlsPermisivo()) {
            System.err.println("[SriHttpUtil] *** AVISO: TLS PERMISIVO ENCENDIDO (-D" + PROP_TLS_PERMISIVO + "=true): se acepta CUALQUIER certificado y nombre "
                    + "para " + url.getHost() + ". Es una valvula transitoria: apague la propiedad cuando la raiz DigiCert Global Root G2 este en el almacen de la JVM. ***");
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            httpsConn.setSSLSocketFactory(Permisivo.SSL_CONTEXT_SRI.getSocketFactory());
            httpsConn.setHostnameVerifier(HOSTNAME_VERIFIER_SRI);
        }

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
        conn.setRequestProperty("SOAPAction", "");

        return conn;
    }

    /**
     * Envía un SOAP envelope y retorna la respuesta como String.
     */
    public static String enviarSoap(String urlStr, String soapEnvelope) throws Exception {
        byte[] soapBytes = soapEnvelope.getBytes("UTF-8");

        HttpURLConnection conn = crearConexion(urlStr);
        conn.setRequestProperty("Content-Length", String.valueOf(soapBytes.length));
        conn.getOutputStream().write(soapBytes);
        conn.getOutputStream().flush();

        int httpStatus = conn.getResponseCode();
        System.out.println("[SriHttpUtil] HTTP " + httpStatus
                + " | Content-Type: " + conn.getContentType()
                + " | URL: " + urlStr);

        java.io.InputStream is = (httpStatus < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();
        if (is == null) is = conn.getInputStream();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) baos.write(buf, 0, n);
        is.close();
        conn.disconnect();

        return baos.toString("UTF-8");
    }
}