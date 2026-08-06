package com.saa.ws.rest.cxc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoints REST para consultar el estado de comprobantes electrónicos ante el SRI.
 *
 * Basado en la especificación oficial del SRI:
 *   - ConsultaComprobante?wsdl  →  consultarEstadoAutorizacionComprobante
 *   - ConsultaFactura?wsdl      →  consultarEstadoConfirmacionFacturaComercialNegociable
 *
 * URLs SRI:
 *   PRUEBAS    (ambiente=1): https://celcer.sri.gob.ec/comprobantes-electronicos-ws/
 *   PRODUCCIÓN (ambiente=2): https://cel.sri.gob.ec/comprobantes-electronicos-ws/
 */
@Path("consultasri")
public class ConsultaSRIRest {

    private static final String URL_PRUEBAS_BASE    = "https://celcer.sri.gob.ec/comprobantes-electronicos-ws/";
    private static final String URL_PRODUCCION_BASE = "https://cel.sri.gob.ec/comprobantes-electronicos-ws/";

    private static final String WS_CONSULTA_COMPROBANTE = "ConsultaComprobante";
    private static final String WS_CONSULTA_FACTURA     = "ConsultaFactura";

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Consultar estado de autorización de cualquier comprobante electrónico
    //    Aplica a: Factura, Nota de Crédito, Nota de Débito, Retención, etc.
    //
    //    GET /consultasri/estado/{clave}?ambiente=1
    //    GET /consultasri/estado/{clave}?ambiente=2
    //
    //    Responde: { "claveAcceso", "estadoAutorizacion", "tipoComprobante",
    //               "rucEmisor", "fechaAutorizacion", "mensajes", "respuestaCompleta" }
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/estado/{clave}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultarEstadoAutorizacion(
            @PathParam("clave")   String clave,
            @QueryParam("ambiente") Long ambiente) {

        System.out.println("=== consultarEstadoAutorizacion | clave=" + clave + " | ambiente=" + ambiente + " ===");

        if (clave == null || clave.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", "La clave de acceso es obligatoria.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error)
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (ambiente == null) ambiente = 1L;

        try {
            String baseUrl = (ambiente == 2L) ? URL_PRODUCCION_BASE : URL_PRUEBAS_BASE;
            String wsUrl   = baseUrl + WS_CONSULTA_COMPROBANTE;

            String soapEnvelope =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ec=\"http://ec.gob.sri.ws.consultas\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<ec:consultarEstadoAutorizacionComprobante>" +
                "<claveAcceso>" + clave.trim() + "</claveAcceso>" +
                "</ec:consultarEstadoAutorizacionComprobante>" +
                "</soapenv:Body></soapenv:Envelope>";

            String respuestaXML = llamarWSConsulta(wsUrl, soapEnvelope);
            System.out.println(">>> Respuesta SRI ConsultaComprobante:");
            System.out.println(respuestaXML);

            Map<String, Object> resultado = parsearRespuestaEstado(respuestaXML, clave.trim());
            resultado.put("ambiente", ambiente == 2L ? "PRODUCCION" : "PRUEBAS");
            resultado.put("exito", true);

            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            System.err.println("ERROR en consultarEstadoAutorizacion: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", "Error al consultar el estado en el SRI: " + e.getMessage());
            error.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Consultar si una factura es comercial negociable
    //    Solo aplica a facturas (tipo 01).
    //
    //    GET /consultasri/negociable/{clave}?ambiente=1
    //    GET /consultasri/negociable/{clave}?ambiente=2
    //
    //    Responde: { "claveAcceso", "estadoConfirmacion" ("SI" o "RECHAZADA"),
    //               "mensajes", "respuestaCompleta" }
    // ─────────────────────────────────────────────────────────────────────────
    @GET
    @Path("/negociable/{clave}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultarFacturaComercialNegociable(
            @PathParam("clave")   String clave,
            @QueryParam("ambiente") Long ambiente) {

        System.out.println("=== consultarFacturaComercialNegociable | clave=" + clave + " | ambiente=" + ambiente + " ===");

        if (clave == null || clave.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", "La clave de acceso es obligatoria.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error)
                    .type(MediaType.APPLICATION_JSON).build();
        }

        if (ambiente == null) ambiente = 1L;

        try {
            String baseUrl = (ambiente == 2L) ? URL_PRODUCCION_BASE : URL_PRUEBAS_BASE;
            String wsUrl   = baseUrl + WS_CONSULTA_FACTURA;

            String soapEnvelope =
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "xmlns:ec=\"http://ec.gob.sri.ws.consultas\">" +
                "<soapenv:Header/><soapenv:Body>" +
                "<ec:consultarEstadoConfirmacionFacturaComercialNegociable>" +
                "<claveAcceso>" + clave.trim() + "</claveAcceso>" +
                "</ec:consultarEstadoConfirmacionFacturaComercialNegociable>" +
                "</soapenv:Body></soapenv:Envelope>";

            String respuestaXML = llamarWSConsulta(wsUrl, soapEnvelope);
            System.out.println(">>> Respuesta SRI ConsultaFactura (Negociable):");
            System.out.println(respuestaXML);

            Map<String, Object> resultado = parsearRespuestaNegociable(respuestaXML, clave.trim());
            resultado.put("ambiente", ambiente == 2L ? "PRODUCCION" : "PRUEBAS");
            resultado.put("exito", true);

            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Exception e) {
            System.err.println("ERROR en consultarFacturaComercialNegociable: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("exito", false);
            error.put("mensaje", "Error al consultar factura negociable en el SRI: " + e.getMessage());
            error.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(error).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Realiza la llamada HTTP al WS del SRI usando HttpURLConnection directamente,
     * evitando el problema de Content-Type de SAAJ en producción.
     */
    private String llamarWSConsulta(String wsUrl, String soapEnvelope) throws Exception {
        return com.saa.ejb.cxc.util.SriHttpUtil.enviarSoap(wsUrl, soapEnvelope);
    }

    /**
     * Parsea la respuesta de consultarEstadoAutorizacionComprobante.
     * Campos: estadoAutorizacion | estadoConsulta | claveAcceso | tipoComprobante |
     *         rucEmisor | fechaAutorizacion | mensajes
     */
    private Map<String, Object> parsearRespuestaEstado(String respuestaXML, String claveOriginal) throws Exception {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("respuestaCompleta", respuestaXML);
        resultado.put("claveAcceso", claveOriginal);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Element docEl = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(respuestaXML.getBytes("UTF-8")))
                .getDocumentElement();

        // Estado principal
        resultado.put("estadoAutorizacion", extraerTexto(docEl, "estadoAutorizacion"));
        resultado.put("estadoConsulta",     extraerTexto(docEl, "estadoConsulta"));

        // Datos del comprobante
        String claveRespuesta = extraerTexto(docEl, "claveAcceso");
        if (!claveRespuesta.isEmpty()) resultado.put("claveAcceso", claveRespuesta);
        resultado.put("tipoComprobante",   extraerTexto(docEl, "tipoComprobante"));
        resultado.put("rucEmisor",         extraerTexto(docEl, "rucEmisor"));
        resultado.put("fechaAutorizacion", extraerTexto(docEl, "fechaAutorizacion"));

        // Mensajes de error (si existen)
        NodeList mensajesNL = docEl.getElementsByTagNameNS("*", "mensaje");
        if (mensajesNL.getLength() == 0) mensajesNL = docEl.getElementsByTagName("mensaje");
        if (mensajesNL.getLength() > 0) {
            java.util.List<Map<String, String>> mensajes = new java.util.ArrayList<>();
            for (int i = 0; i < mensajesNL.getLength(); i++) {
                Element msg = (Element) mensajesNL.item(i);
                Map<String, String> m = new HashMap<>();
                m.put("identificador",       extraerTextoHijo(msg, "identificador"));
                m.put("mensaje",             extraerTextoHijo(msg, "mensaje"));
                m.put("informacionAdicional",extraerTextoHijo(msg, "informacionAdicional"));
                m.put("tipo",                extraerTextoHijo(msg, "tipo"));
                mensajes.add(m);
            }
            resultado.put("mensajes", mensajes);
        }

        return resultado;
    }

    /**
     * Parsea la respuesta de consultarEstadoConfirmacionFacturaComercialNegociable.
     * Campos: estadoConfirmacion | estadoConsulta | claveAcceso | mensajes
     */
    private Map<String, Object> parsearRespuestaNegociable(String respuestaXML, String claveOriginal) throws Exception {
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("respuestaCompleta", respuestaXML);
        resultado.put("claveAcceso", claveOriginal);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Element docEl = dbf.newDocumentBuilder()
                .parse(new ByteArrayInputStream(respuestaXML.getBytes("UTF-8")))
                .getDocumentElement();

        resultado.put("estadoConfirmacion", extraerTexto(docEl, "estadoConfirmacion"));
        resultado.put("estadoConsulta",     extraerTexto(docEl, "estadoConsulta"));

        String claveRespuesta = extraerTexto(docEl, "claveAcceso");
        if (!claveRespuesta.isEmpty()) resultado.put("claveAcceso", claveRespuesta);

        NodeList mensajesNL = docEl.getElementsByTagNameNS("*", "mensaje");
        if (mensajesNL.getLength() == 0) mensajesNL = docEl.getElementsByTagName("mensaje");
        if (mensajesNL.getLength() > 0) {
            java.util.List<Map<String, String>> mensajes = new java.util.ArrayList<>();
            for (int i = 0; i < mensajesNL.getLength(); i++) {
                Element msg = (Element) mensajesNL.item(i);
                Map<String, String> m = new HashMap<>();
                m.put("identificador",       extraerTextoHijo(msg, "identificador"));
                m.put("mensaje",             extraerTextoHijo(msg, "mensaje"));
                m.put("informacionAdicional",extraerTextoHijo(msg, "informacionAdicional"));
                m.put("tipo",                extraerTextoHijo(msg, "tipo"));
                mensajes.add(m);
            }
            resultado.put("mensajes", mensajes);
        }

        return resultado;
    }

    /** Extrae texto de un nodo buscando por nombre, con y sin namespace. */
    private String extraerTexto(Element padre, String tag) {
        NodeList nl = padre.getElementsByTagNameNS("*", tag);
        if (nl.getLength() == 0) nl = padre.getElementsByTagName(tag);
        if (nl.getLength() > 0 && nl.item(0).getTextContent() != null)
            return nl.item(0).getTextContent().trim();
        return "";
    }

    /** Extrae texto de un nodo hijo directo de un Element. */
    private String extraerTextoHijo(Element padre, String tag) {
        NodeList nl = padre.getElementsByTagName(tag);
        if (nl.getLength() == 0) nl = padre.getElementsByTagNameNS("*", tag);
        if (nl.getLength() > 0 && nl.item(0).getTextContent() != null)
            return nl.item(0).getTextContent().trim();
        return "";
    }
}
