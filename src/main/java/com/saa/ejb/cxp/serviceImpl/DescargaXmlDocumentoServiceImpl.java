package com.saa.ejb.cxp.serviceImpl;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.saa.basico.ejb.FileService;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.cxp.dao.DocumentoCxpDaoService;
import com.saa.ejb.cxp.service.DescargaXmlDocumentoService;
import com.saa.ejb.cxp.service.ProcesoCargaDocumentosService;
import com.saa.ejb.cxp.service.SriAutorizacionService;
import com.saa.ejb.cxp.service.dto.ResultadoAutorizacionSri;
import com.saa.model.cxp.DocumentoCxp;
import com.saa.model.cxp.NombreEntidadesCompra;
import com.saa.rubros.EstadoDocumentoCxp;
import com.saa.rubros.OrigenXmlDocumento;
import com.saa.rubros.ResultadoDescargaSri;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Implementación de la descarga del XML de un documento desde el SRI.
 *
 * @author Sistema SAA
 * @since 2026-08-23
 */
@Stateless
public class DescargaXmlDocumentoServiceImpl implements DescargaXmlDocumentoService {

    /** Largo de PGS.DCXP.DCXPMSRI — VARCHAR2(500). */
    private static final int LARGO_MENSAJE_SRI = 500;

    /** Largo de PGS.DCXP.DCXPOBSR — VARCHAR2(2000). */
    private static final int LARGO_OBSERVACION = 2000;

    @EJB private DocumentoCxpDaoService         documentoCxpDaoService;
    @EJB private SriAutorizacionService         sriAutorizacionService;
    @EJB private ProcesoCargaDocumentosService  procesoCargaDocumentosService;
    @EJB private FileService                    fileService;

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Map<String, Object> descargarXmlDocumento(Long idDocumentoCxp, Long idEmpresa,
                                                      Long idUsuario) throws Throwable {

        System.out.println("=== descargarXmlDocumento idDocumentoCxp=" + idDocumentoCxp
                + " idEmpresa=" + idEmpresa + " idUsuario=" + idUsuario);

        DocumentoCxp doc = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        if (doc == null)
            throw new IncomeException("DocumentoCxp no encontrado: " + idDocumentoCxp);

        // ── Idempotencia (§8 regla 6) ────────────────────────────────────────
        // El orquestador ya filtró, pero el lote se re-ejecuta y entre la consulta
        // de ids y este punto pueden haber pasado minutos. Se vuelve a verificar
        // dentro de la transacción, que es donde el dato es de fiar.
        if (doc.getEstadoDocumento() != null && doc.getEstadoDocumento() == EstadoDocumentoCxp.REGISTRADO_BD)
            return omitido("El documento ya está registrado en BD (estado 3).");
        if (doc.getPathXml() != null && !doc.getPathXml().trim().isEmpty())
            return omitido("El documento ya tiene XML cargado.");

        // ── Clave de acceso utilizable ───────────────────────────────────────
        // Se valida entera aquí, y sin excepciones, para no gastar la llamada y
        // para que el motivo quede escrito en el documento.
        String clave = doc.getClaveAcceso() != null ? doc.getClaveAcceso().trim() : "";
        String motivoClave = sriAutorizacionService.motivoClaveInvalida(clave);
        if (motivoClave != null) {
            return estampar(doc, ResultadoDescargaSri.NO_ENCONTRADO,
                    motivoClave + " Sin una clave válida no hay nada que consultar;"
                    + " suba el XML a mano.", false);
        }

        // ── Ventana del SRI: se calcula ANTES de gastar la llamada (§2) ───────
        LocalDate fechaEmision = doc.getFechaEmision() != null
                ? doc.getFechaEmision().toLocalDate() : null;
        if (!sriAutorizacionService.dentroDeVentana(fechaEmision)) {
            return estampar(doc, ResultadoDescargaSri.FUERA_VENTANA,
                    "Emitido el " + fechaEmision + ", anterior al " + sriAutorizacionService.limiteVentanaConsulta()
                    + ". El SRI solo devuelve comprobantes del último mes contado por día del mes,"
                    + " y para los anteriores responde vacío sin decir por qué. Suba el XML a mano.",
                    false);
        }

        // ── Consulta al SRI ──────────────────────────────────────────────────
        ResultadoAutorizacionSri respuesta;
        try {
            respuesta = sriAutorizacionService.consultarAutorizacion(clave);
        } catch (Throwable e) {
            System.err.println("⚠ Error de conexión con el SRI para la clave " + clave
                    + ": " + e.getMessage());
            return estampar(doc, ResultadoDescargaSri.ERROR_CONEXION,
                    "No se pudo consultar al SRI: " + e.getMessage(), true);
        }

        if (respuesta.isNoEncontrado()) {
            return estampar(doc, ResultadoDescargaSri.NO_ENCONTRADO,
                    "El SRI no devolvió ningún comprobante para esta clave de acceso."
                    + (respuesta.getMensajes() != null ? " " + respuesta.getMensajes() : "")
                    + " Verifique la clave del TXT o suba el XML a mano.", false);
        }

        if (!respuesta.isAutorizado()) {
            return estampar(doc, ResultadoDescargaSri.NO_AUTORIZADO,
                    "El SRI reporta el comprobante en estado [" + respuesta.getEstado() + "]."
                    + (respuesta.getMensajes() != null ? " " + respuesta.getMensajes() : ""), false);
        }

        String sobreXml = respuesta.getXmlAutorizacion();

        // ── Guardar el sobre <autorizacion> COMPLETO en disco ────────────────
        // El archivo queda igual al que el usuario baja del portal del SRI. Es lo
        // que permite que registrarFacturaCompra lea fechaAutorizacion del XML
        // externo con getXmlValueOuter — con solo el <comprobante> interno ese
        // dato no existiría. Ver §8 regla 3.
        String pathDestino = fileService.uploadFileToPath(
                new ByteArrayInputStream(sobreXml.getBytes(StandardCharsets.UTF_8)),
                clave + ".xml", SUBDIRECTORIO_XML);
        System.out.println("✓ XML del SRI guardado en: " + pathDestino);

        // ── Validación contra el TXT y paso a estado 2 ───────────────────────
        // Se reutiliza cargarXmlDocumento tal cual: es el mismo camino que recorre
        // la subida manual, así que un XML bajado del SRI y uno subido a mano
        // quedan exactamente iguales en la base.
        Map<String, Object> resultadoCarga = procesoCargaDocumentosService
                .cargarXmlDocumento(idDocumentoCxp, sobreXml, pathDestino, idUsuario, null);

        if (!Boolean.TRUE.equals(resultadoCarga.get("valido"))) {
            // El SRI sí lo tiene, pero no coincide con lo que declara el TXT. No es
            // un problema de descarga; es una discrepancia que alguien tiene que
            // mirar, así que se deja en ERROR con el detalle en la observación.
            String detalle = "El XML descargado del SRI no coincide con el TXT: "
                    + resultadoCarga.get("errores");
            doc.setEstadoDocumento(EstadoDocumentoCxp.ERROR);
            doc.setObservacion(recortar(detalle, LARGO_OBSERVACION));
            Map<String, Object> r = estampar(doc, ResultadoDescargaSri.DESCARGADO, detalle, false);
            r.put(CLAVE_VALIDO, Boolean.FALSE);
            return r;
        }

        // ── Pre-marca de reembolso (§8 regla 10) ─────────────────────────────
        // Solo agrega, nunca quita: un documento que el usuario marcó a mano no se
        // desmarca porque el XML no traiga el bloque. Ese es justamente el caso de
        // las facturas de reembolso mal emitidas.
        DocumentoCxp docActualizado = documentoCxpDaoService.selectById(idDocumentoCxp,
                NombreEntidadesCompra.DOCUMENTO_CXP);
        boolean yaMarcado = docActualizado.getEsReembolso() != null
                && docActualizado.getEsReembolso() == 1L;
        if (!yaMarcado && xmlDeclaraReembolso(sobreXml)) {
            docActualizado.setEsReembolso(1L);
            System.out.println("✓ Pre-marca de reembolso en el documento " + idDocumentoCxp
                    + " a partir del XML del SRI.");
        }

        Map<String, Object> r = estampar(docActualizado, ResultadoDescargaSri.DESCARGADO, null, false);
        r.put(CLAVE_VALIDO, Boolean.TRUE);
        return r;
    }

    // =====================================================================
    // Utilitarios privados
    // =====================================================================

    /**
     * Graba en el documento el resultado del intento y devuelve el mapa que
     * espera el orquestador. El {@code origenXml} solo se toca cuando el XML
     * llegó de verdad: un intento fallido no cambia el origen de un archivo que
     * el usuario pueda haber subido después a mano.
     *
     * @param doc          : Documento a estampar
     * @param resultado    : Valor de {@code ResultadoDescargaSri}
     * @param mensaje      : Motivo, o null si salió bien
     * @param reintentable : true si el orquestador debe volver a intentarlo
     * @return             : Mapa de retorno del método público
     * @throws Throwable   : Si falla el guardado
     */
    private Map<String, Object> estampar(DocumentoCxp doc, String resultado, String mensaje,
                                          boolean reintentable) throws Throwable {

        doc.setResultadoSri(resultado);
        doc.setMensajeSri(recortar(mensaje, LARGO_MENSAJE_SRI));
        doc.setFechaDescargaSri(LocalDateTime.now());
        if (ResultadoDescargaSri.DESCARGADO.equals(resultado))
            doc.setOrigenXml(OrigenXmlDocumento.SRI);
        documentoCxpDaoService.save(doc, doc.getId());

        System.out.println("   → documento " + doc.getId() + ": " + resultado
                + (mensaje != null ? " — " + mensaje : ""));

        Map<String, Object> r = new HashMap<>();
        r.put(CLAVE_RESULTADO, resultado);
        r.put(CLAVE_MENSAJE, mensaje);
        r.put(CLAVE_REINTENTABLE, Boolean.valueOf(reintentable));
        r.put(CLAVE_OMITIDO, Boolean.FALSE);
        return r;
    }

    private Map<String, Object> omitido(String motivo) {
        System.out.println("   → omitido: " + motivo);
        Map<String, Object> r = new HashMap<>();
        r.put(CLAVE_OMITIDO, Boolean.TRUE);
        r.put(CLAVE_MENSAJE, motivo);
        r.put(CLAVE_REINTENTABLE, Boolean.FALSE);
        return r;
    }

    /**
     * ¿El comprobante descargado es una factura de reembolso de gastos?
     *
     * <p>
     * Dos señales, y basta una: el bloque {@code <reembolsoDetalle>}, o el
     * {@code <codDocReembolso>} que va <b>dentro de {@code <infoFactura>}</b>. Lo
     * segundo se busca en ese nodo y no en el documento entero a propósito:
     * {@code codDocReembolso} también aparece dentro de cada
     * {@code reembolsoDetalle}, y una búsqueda global no distinguiría el marcador
     * de cabecera de las líneas de sustento.
     * </p>
     *
     * @param sobreXml : Sobre {@code <autorizacion>} completo
     * @return         : true si el XML declara reembolso
     */
    private boolean xmlDeclaraReembolso(String sobreXml) {
        if (sobreXml == null) return false;
        if (sobreXml.contains("<reembolsoDetalle>")) return true;

        try {
            Document comprobante = parsearComprobanteInterno(sobreXml);
            NodeList infoFacturas = comprobante.getElementsByTagName("infoFactura");
            if (infoFacturas.getLength() == 0) return false;

            Element infoFactura = (Element) infoFacturas.item(0);
            NodeList codigos = infoFactura.getElementsByTagName("codDocReembolso");
            if (codigos.getLength() == 0) return false;

            String valor = codigos.item(0).getTextContent();
            return valor != null && !valor.trim().isEmpty();
        } catch (Exception e) {
            System.err.println("⚠ No se pudo evaluar la pre-marca de reembolso: " + e.getMessage());
            return false;
        }
    }

    /**
     * Desenvuelve el {@code <comprobante>} del sobre de autorización. Es la misma
     * mecánica de {@code ProcesoCargaDocumentosServiceImpl.parsearXmlComprobante}:
     * si hay {@code <comprobante>}, su contenido es el comprobante real; si no, el
     * archivo ya era el comprobante.
     *
     * @param xmlCompleto : Sobre completo
     * @return            : Documento DOM del comprobante interno
     * @throws Exception  : Si el XML no se puede parsear
     */
    private Document parsearComprobanteInterno(String xmlCompleto) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document docOuter = builder.parse(new InputSource(new StringReader(xmlCompleto)));

        NodeList comprobantes = docOuter.getElementsByTagName("comprobante");
        if (comprobantes.getLength() > 0) {
            String contenido = comprobantes.item(0).getTextContent();
            if (contenido != null && !contenido.trim().isEmpty())
                return builder.parse(new InputSource(new StringReader(contenido.trim())));
        }
        return docOuter;
    }

    /**
     * Recorta al largo de la columna. El corte se verifica en bytes UTF-8 y no
     * solo en caracteres: las columnas son VARCHAR2 sin semántica CHAR explícita,
     * y los mensajes del SRI vienen con tildes.
     *
     * @param texto : Texto a recortar
     * @param largo : Largo máximo de la columna
     * @return      : Texto que cabe, o null si no había texto
     */
    private String recortar(String texto, int largo) {
        if (texto == null) return null;

        String recortado = texto.length() > largo ? texto.substring(0, largo) : texto;
        while (!recortado.isEmpty()
                && recortado.getBytes(StandardCharsets.UTF_8).length > largo)
            recortado = recortado.substring(0, recortado.length() - 1);

        return recortado;
    }
}
