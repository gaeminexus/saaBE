package com.saa.ejb.cxp.serviceImpl;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxc.util.SriHttpUtil;
import com.saa.ejb.cxp.service.SriAutorizacionService;
import com.saa.ejb.cxp.service.dto.ResultadoAutorizacionSri;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación del cliente compartido del WS2 de autorización del SRI.
 *
 * <p>
 * {@code NOT_SUPPORTED}: aquí no se toca la base. Es una llamada de red que
 * puede tardar hasta minuto y medio entre el connect y el read timeout de
 * {@link SriHttpUtil}, y no tiene por qué mantener abierta una transacción ni
 * una conexión del pool mientras espera.
 * </p>
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class SriAutorizacionServiceImpl implements SriAutorizacionService {

    /** Posición (base 0) del dígito de ambiente dentro de la clave de acceso. */
    private static final int INDICE_AMBIENTE = 23;

    /**
     * Localiza la apertura del sobre, tolerando un prefijo de namespace.
     * El {@code >} final es lo que impide que enganche con {@code <autorizaciones>}.
     */
    private static final Pattern APERTURA_AUTORIZACION =
            Pattern.compile("<((?:[\\w.-]+:)?)autorizacion>");

    // =====================================================================
    // Ventana del SRI
    // =====================================================================

    @Override
    public LocalDate limiteVentanaConsulta() {
        return LocalDate.now().minusMonths(1);
    }

    @Override
    public boolean dentroDeVentana(LocalDate fechaEmision) {
        if (fechaEmision == null) return true;
        return !fechaEmision.isBefore(limiteVentanaConsulta());
    }

    // =====================================================================
    // Ambiente
    // =====================================================================

    @Override
    public String motivoClaveInvalida(String claveAcceso) {
        String clave = claveAcceso != null ? claveAcceso.trim() : "";

        if (clave.length() != LARGO_CLAVE_ACCESO)
            return "La clave de acceso debe tener " + LARGO_CLAVE_ACCESO + " dígitos y tiene "
                    + clave.length() + ": [" + clave + "].";

        for (int i = 0; i < clave.length(); i++)
            if (!Character.isDigit(clave.charAt(i)))
                return "La clave de acceso tiene caracteres que no son dígitos: [" + clave + "].";

        char digito = clave.charAt(INDICE_AMBIENTE);
        if (digito != '1' && digito != '2')
            return "El dígito 24 de la clave de acceso no es un ambiente válido "
                    + "(1=pruebas, 2=producción): [" + digito + "] en [" + clave + "].";

        return null;
    }

    @Override
    public Long ambienteDesdeClaveAcceso(String claveAcceso) throws Throwable {
        if (claveAcceso == null || claveAcceso.trim().length() < LARGO_CLAVE_ACCESO)
            throw new IncomeException("La clave de acceso no tiene los " + LARGO_CLAVE_ACCESO
                    + " dígitos requeridos: [" + claveAcceso + "]");

        char digito = claveAcceso.trim().charAt(INDICE_AMBIENTE);
        if (digito != '1' && digito != '2')
            throw new IncomeException("El dígito 24 de la clave de acceso no es un ambiente "
                    + "válido (1=pruebas, 2=producción): [" + digito + "] en [" + claveAcceso + "]");

        return digito == '2' ? 2L : 1L;
    }

    // =====================================================================
    // Consulta al WS2
    // =====================================================================

    @Override
    public ResultadoAutorizacionSri consultarAutorizacion(String claveAcceso) throws Throwable {

        System.out.println("=== consultarAutorizacion claveAcceso=" + claveAcceso);

        String clave = claveAcceso != null ? claveAcceso.trim() : null;
        Long ambiente = ambienteDesdeClaveAcceso(clave);
        String url = ambiente == 2L ? URL_AUTORIZACION_PRODUCCION : URL_AUTORIZACION_PRUEBAS;

        ResultadoAutorizacionSri resultado = new ResultadoAutorizacionSri();
        resultado.setClaveAcceso(clave);
        resultado.setAmbiente(ambiente);

        // Mismo sobre SOAP que usa FacturaServiceImpl desde que el WS2 está en producción.
        String soapEnvelope =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:aut=\"http://ec.gob.sri.ws.autorizacion\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<aut:autorizacionComprobante>" +
                "<claveAccesoComprobante>" + clave + "</claveAccesoComprobante>" +
                "</aut:autorizacionComprobante>" +
                "</soapenv:Body></soapenv:Envelope>";

        System.out.println(">>> WS2 autorización SRI (ambiente=" + ambiente + "): " + url);
        String respuesta = SriHttpUtil.enviarSoap(url, soapEnvelope);
        resultado.setRespuestaCompleta(respuesta);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Element raiz = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(respuesta.getBytes("UTF-8")))
                .getDocumentElement();

        resultado.setNumeroComprobantes(entero(texto(raiz, "numeroComprobantes")));
        resultado.setEstado(texto(raiz, "estado"));
        resultado.setNumeroAutorizacion(texto(raiz, "numeroAutorizacion"));
        resultado.setFechaAutorizacion(texto(raiz, "fechaAutorizacion"));
        resultado.setMensajes(armarMensajes(raiz));

        // El sobre se recorta de la respuesta CRUDA, no se re-serializa desde el
        // DOM. Dos razones: el contenido del <comprobante> viaja tal como lo mandó
        // el SRI —CDATA o texto escapado, da igual— sin que un serializador lo
        // reescriba, y no se introduce una dependencia de TransformerFactory en un
        // despliegue que ya excluye los módulos org.apache.xml de WildFly (ver el
        // comentario largo del pom.xml sobre Batik/xml-apis).
        resultado.setXmlAutorizacion(extraerSobreAutorizacion(respuesta));

        System.out.println(">>> WS2 estado=" + resultado.getEstado()
                + " numeroComprobantes=" + resultado.getNumeroComprobantes()
                + " autorizacion=" + resultado.getNumeroAutorizacion()
                + " sobreXml=" + (resultado.getXmlAutorizacion() != null
                        ? resultado.getXmlAutorizacion().length() + " chars" : "ausente")
                + (resultado.getMensajes() != null ? " | " + resultado.getMensajes() : ""));

        return resultado;
    }

    // =====================================================================
    // Utilitarios privados
    // =====================================================================

    /**
     * Recorta el sobre {@code <autorizacion>...</autorizacion>} de la respuesta
     * SOAP y le antepone la declaración XML, para que el archivo en disco sea el
     * mismo que el usuario baja del portal del SRI.
     *
     * @param respuesta : Respuesta SOAP cruda
     * @return          : El sobre completo, o null si la respuesta no trae ninguno
     */
    private String extraerSobreAutorizacion(String respuesta) {
        if (respuesta == null) return null;

        Matcher m = APERTURA_AUTORIZACION.matcher(respuesta);
        if (!m.find()) return null;

        String cierre = "</" + m.group(1) + "autorizacion>";
        int fin = respuesta.lastIndexOf(cierre);
        if (fin < m.start()) return null;

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + respuesta.substring(m.start(), fin + cierre.length());
    }

    /**
     * Junta lo que el SRI tenga que decir sobre el comprobante. Se usa para el
     * mensaje que se estampa en el documento cuando el resultado no es
     * DESCARGADO; el llamador lo recorta a las 500 posiciones de DCXPMSRI.
     *
     * @param raiz : Elemento raíz de la respuesta SOAP
     * @return     : Mensajes concatenados, o null si el SRI no dijo nada
     */
    private String armarMensajes(Element raiz) {
        StringBuilder sb = new StringBuilder();
        agregar(sb, texto(raiz, "identificador"));
        agregar(sb, texto(raiz, "mensaje"));
        agregar(sb, texto(raiz, "informacionAdicional"));
        return sb.length() > 0 ? sb.toString() : null;
    }

    private void agregar(StringBuilder sb, String valor) {
        if (valor == null || valor.isEmpty()) return;
        if (sb.length() > 0) sb.append(" | ");
        sb.append(valor);
    }

    /**
     * Lee el primer nodo con ese nombre, con y sin namespace. Es la misma doble
     * búsqueda del original: la respuesta del SRI mezcla elementos calificados y
     * sin calificar según el tramo del sobre.
     *
     * @param raiz : Elemento raíz de la respuesta
     * @param tag  : Nombre del nodo buscado
     * @return     : Texto del primer nodo encontrado, o null
     */
    private String texto(Element raiz, String tag) {
        NodeList nl = raiz.getElementsByTagNameNS("*", tag);
        if (nl.getLength() == 0) nl = raiz.getElementsByTagName(tag);
        if (nl.getLength() == 0) return null;
        String valor = nl.item(0).getTextContent();
        return valor != null ? valor.trim() : null;
    }

    private int entero(String valor) {
        try {
            return valor != null && !valor.isEmpty() ? Integer.parseInt(valor.trim()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
