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
 * Utilidad para crear conexiones HTTP/HTTPS al SRI tolerando el certificado SSL.
 *
 * Problema: el servidor de producción del SRI (cel.sri.gob.ec) responde desde la
 * IP 181.113.227.222. Java rechaza la conexión HTTPS porque el certificado del
 * servidor tiene como CN/SAN el nombre de dominio, no la IP.
 *
 * Solución: X509ExtendedTrustManager + HostnameVerifier permisivo.
 *
 * IMPORTANTE — por qué X509ExtendedTrustManager y NO SSLSocketFactory/SSLParameters:
 *   HttpsURLConnection.connect() llama internamente a
 *   sun.security.ssl.SSLSocketImpl.startHandshake() que invoca
 *   SSLParameters.setEndpointIdentificationAlgorithm("HTTPS") DESPUÉS de que
 *   creamos el socket, sobreescribiendo cualquier valor que hubiéramos puesto.
 *   Eso causa "Connection reset" porque el peer aborta el handshake al detectar
 *   el alert TLS "unrecognized_name".
 *
 *   X509ExtendedTrustManager es llamado DENTRO del handshake TLS, en el momento
 *   exacto en que se valida el certificado del servidor. Al implementar
 *   checkServerTrusted(chain, authType, engine) como no-op, la validación
 *   de IP/hostname nunca ocurre, independientemente de lo que haga HttpsURLConnection
 *   con los SSLParameters.
 */
public class SriHttpUtil {

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

        if (conn instanceof HttpsURLConnection) {
            HttpsURLConnection httpsConn = (HttpsURLConnection) conn;
            httpsConn.setSSLSocketFactory(SSL_CONTEXT_SRI.getSocketFactory());
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