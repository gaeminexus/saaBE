package com.saa.ejb.crd.firma;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saa.rubros.EstadoFirmaContrato;
import com.saa.rubros.EstadoFirmanteContrato;

/**
 * Proveedor Security Data (DatAgree). Flujo A (diseño §10): {@code authenticateUser} → {@code signersValidation} del
 * partícipe → {@code createTask} (el partícipe recibe el enlace; el representante legal firma embebido al final) →
 * {@code taskDetail} / {@code base64} / {@code cancelTask}. Referencia: crd/REFERENCIA-API-SECURITY-DATA-DATAGREE.md.
 *
 * <p>Configuración: propiedades de sistema de WildFly, sin valores por defecto (nunca en el código ni en el repo):
 * {@code saa.firma.securitydata.url} (la base, hasta {@code /api/dtagree}), {@code .usuario}, {@code .clave}, {@code .ruc},
 * {@code .cedulaRepresentante} (solo para enviar: con ella se busca la firma delegada del representante legal).
 * Opcionales: {@code .referenciaRepresentante} (si viene, manda sobre la búsqueda), {@code .metodoNotificacion}
 * (ENVIO_OTPN_CORREO_WSS), {@code .diasVigencia} (7), {@code .horasRenotificacion} (24). Si falta una obligatoria:
 * {@code PROVEEDOR_NO_CONFIGURADO} con su nombre (nunca su valor).</p>
 *
 * <p>La clave, el token, el código dactilar y el enlace de firma del partícipe ({@code signerToken}) no se escriben en
 * ningún log ni mensaje ni se devuelven.</p>
 */
public class SecurityDataProveedor implements ProveedorFirmaElectronica {

    public static final String NOMBRE = "SECURITY_DATA";

    static final String PROP_URL = "saa.firma.securitydata.url";
    static final String PROP_USUARIO = "saa.firma.securitydata.usuario";
    static final String PROP_CLAVE = "saa.firma.securitydata.clave";
    static final String PROP_RUC = "saa.firma.securitydata.ruc";
    static final String PROP_REFERENCIA_REPRESENTANTE = "saa.firma.securitydata.referenciaRepresentante";
    static final String PROP_CEDULA_REPRESENTANTE = "saa.firma.securitydata.cedulaRepresentante";
    static final String PROP_METODO_NOTIFICACION = "saa.firma.securitydata.metodoNotificacion";
    static final String PROP_DIAS_VIGENCIA = "saa.firma.securitydata.diasVigencia";
    static final String PROP_HORAS_RENOTIFICACION = "saa.firma.securitydata.horasRenotificacion";

    static final String METODO_NOTIFICACION_DEFECTO = "ENVIO_OTPN_CORREO_WSS";
    static final int DIAS_VIGENCIA_DEFECTO = 7;
    static final int HORAS_RENOTIFICACION_DEFECTO = 24;

    private static final String NICKNAME = "Contrato de adhesión ASOPREP-FCPC";
    private static final String TIPO_NOTIFICACION = "TIPO_ORDEN_CADENA_SECU_CONTRATO";
    private static final String FIRMA_PARTICIPE = "SC_TP_F_AVANZADA_PN";
    private static final String IDENTIFICACION_CEDULA = "TIPO_IDENTIFICACION_CEDULA";
    private static final String SIGNER_NORMAL = "TP_SIGNER_NORMAL";
    private static final String SIGNER_EMBEDDED = "TP_SIGNER_EMBEDDED";
    private static final String PREFIJO_ESTADO_TAREA = "SC_ESTADO_CONTRATO_";
    private static final String PREFIJO_ESTADO_FIRMANTE = "SC_PDF_ESTADO_";

    /** El token vive 15 minutos: se reutiliza 10 y después se pide otro. */
    private static final long VIGENCIA_TOKEN_MS = 10L * 60 * 1000;
    private static final DateTimeFormatter FECHA_PROVEEDOR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FECHA_EVIDENCIA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    // Caché del token (compartida: el servicio crea un proveedor por operación). Solo en memoria.
    private static final Object CERROJO_TOKEN = new Object();
    private static String tokenCache;
    private static long tokenCacheHasta;

    @Override
    public String nombre() {
        return NOMBRE;
    }

    // ========================================================================
    // enviar
    // ========================================================================

    @Override
    public ResultadoEnvio enviar(byte[] documentoPdf, String nombreDocumento, DatosParticipe participe, PosicionesFirma posiciones)
            throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.enviar - documento: " + nombreDocumento);
        Configuracion cfg = configuracion(true);
        if (posiciones == null || posiciones.getParticipe().isEmpty() || posiciones.getRepresentante().isEmpty()) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                "faltan las posiciones de firma del partícipe o del representante");
        }

        String signatoryId = validarFirmante(cfg, participe);

        // createTask
        ObjectNode detalle = JSON.createObjectNode();
        detalle.put("showPdf", true);
        detalle.put("nickname", NICKNAME);
        ArrayNode pdfs = detalle.putArray("lisPdfDetail");
        ObjectNode pdf = pdfs.addObject();
        pdf.put("pdfName", nombreDocumento);
        ArrayNode participantes = pdf.putArray("listparticipants");
        agregarParticipante(participantes, SIGNER_NORMAL, signatoryId, posiciones.getParticipe());
        // La propiedad manda; si no viene, se busca la firma delegada del representante legal en Security Data.
        String referencia = cfg.referenciaRepresentante != null ? cfg.referenciaRepresentante : buscarReferenciaRepresentante(cfg);
        agregarParticipante(participantes, SIGNER_EMBEDDED, referencia, posiciones.getRepresentante());

        ObjectNode notificacion = detalle.putObject("notification");
        notificacion.put("dateEnd", LocalDateTime.now().plusDays(cfg.diasVigencia).format(FECHA_PROVEEDOR));
        int minutos = Math.max(15, Math.min(cfg.horasRenotificacion * 60, 23 * 60 + 59));
        notificacion.put("hourNoti", minutos / 60);
        notificacion.put("minNoti", minutos % 60);
        notificacion.put("typeNotification", TIPO_NOTIFICACION);
        detalle.put("methodNotification", cfg.metodoNotificacion);

        JsonNode respuesta = multipart(cfg, "/createTask", nombreDocumento, documentoPdf, detalle.toString());
        JsonNode result = respuesta.get("result");
        String processId = result == null || result.isNull() ? null
            : (result.isTextual() ? result.asText() : result.path("processId").asText(null));
        if (processId == null || processId.trim().isEmpty()) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                "createTask no devolvió el identificador de la tarea");
        }

        ResultadoEnvio r = new ResultadoEnvio();
        r.setIdExterno(processId.trim());
        r.getIdsFirmantes().add(signatoryId);
        return r;
    }

    /** signersValidation del partícipe. Devuelve su signatoryId (STY-0001) o lanza el error que corresponda. */
    private String validarFirmante(Configuracion cfg, DatosParticipe p) throws ProveedorFirmaException {
        if (p == null) {
            throw new ProveedorFirmaException(ProveedorFirmaException.FIRMANTE_REQUIERE_DATOS, "no hay datos del partícipe");
        }
        String celular = celularLocal(p.getCelular());
        StringBuilder faltan = new StringBuilder();
        if (vacio(p.getIdentificacion())) {
            faltan.append("cédula ");
        }
        if (vacio(p.getCorreo())) {
            faltan.append("correo ");
        }
        if (celular == null) {
            faltan.append("celular (09XXXXXXXX) ");
        }
        if (vacio(p.getCodigoDactilar())) {
            faltan.append("código dactilar ");
        }
        if (faltan.length() > 0) {
            throw new ProveedorFirmaException(ProveedorFirmaException.FIRMANTE_REQUIERE_DATOS,
                "faltan datos del partícipe para validarlo: " + faltan.toString().trim());
        }

        ArrayNode cuerpo = JSON.createArrayNode();
        ObjectNode f = cuerpo.addObject();
        f.put("dni", p.getIdentificacion().trim());
        f.put("ruc", "");
        f.put("email", p.getCorreo().trim());
        f.put("numPhone", celular);
        f.put("countryCode", "+593");
        f.put("tpSignature", FIRMA_PARTICIPE);
        f.put("personPosition", "");
        f.put("regulatedSuper", false);
        f.put("reuseSignature", false);
        f.put("fingerprintCode", p.getCodigoDactilar().trim().toUpperCase(Locale.ROOT));
        f.put("tpIdentification", IDENTIFICACION_CEDULA);

        JsonNode respuesta = llamarJson(cfg, "POST", "/signersValidation", cuerpo.toString());
        JsonNode lista = respuesta.get("result");
        if (lista == null || !lista.isArray() || lista.size() == 0) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                "signersValidation no devolvió el resultado del firmante");
        }
        JsonNode r = lista.get(0);
        String codigo = r.path("code").asText("");
        if ("STY-0001".equals(codigo)) {
            String id = r.path("signatoryId").asText(null);
            if (vacio(id)) {
                throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                    "signersValidation validó al firmante pero no devolvió su signatoryId");
            }
            return id;
        }
        if ("STY-0002".equals(codigo)) {
            throw new ProveedorFirmaException(ProveedorFirmaException.FIRMANTE_REQUIERE_DATOS,
                "Security Data pide más datos del firmante. Campos: " + resumir(r.get("listFields"))
                + ". Adjuntos: " + resumir(r.get("listFiles")));
        }
        // STY-0003 (error), STY-0013 (cédula no habilitada), STY-0014 (cédula expirada) y cualquier otro código: errorReason literal.
        String motivo = r.path("errorReason").asText("");
        throw new ProveedorFirmaException(ProveedorFirmaException.FIRMANTE_RECHAZADO,
            (vacio(codigo) ? "" : codigo + ": ") + (vacio(motivo) ? "el proveedor no aceptó al firmante" : motivo));
    }

    /**
     * findSignatureReferences por la cédula del representante: la firma de tipo Representante Legal, ACTIVA y con
     * expirationDate futura; si hay varias, la que vence más tarde. Sin ninguna: REPRESENTANTE_SIN_FIRMA.
     */
    private String buscarReferenciaRepresentante(Configuracion cfg) throws ProveedorFirmaException {
        ObjectNode cuerpo = JSON.createObjectNode();
        cuerpo.put("dni", cfg.cedulaRepresentante);
        cuerpo.put("names", "");
        JsonNode lista = llamarJson(cfg, "POST", "/findSignatureReferences", cuerpo.toString()).get("result");
        String mejor = null;
        LocalDateTime vence = null;
        if (lista != null && lista.isArray()) {
            for (JsonNode f : lista) {
                String tipo = sinTildes(f.path("tpSignature").asText("")).toUpperCase(Locale.ROOT);
                if (!tipo.contains("REPRESENTANTE") || !tipo.contains("LEGAL")
                        || !"ACTIVO".equalsIgnoreCase(f.path("statusSignature").asText())
                        || vacio(f.path("referenceCode").asText(null))) {
                    continue;
                }
                LocalDateTime exp;
                try {
                    exp = LocalDateTime.parse(f.path("expirationDate").asText(), FECHA_EVIDENCIA);
                } catch (RuntimeException e) {
                    continue; // sin fecha legible no se puede saber si está vigente
                }
                if (exp.isAfter(LocalDateTime.now()) && (vence == null || exp.isAfter(vence))) {
                    vence = exp;
                    mejor = f.path("referenceCode").asText();
                }
            }
        }
        if (mejor == null) {
            throw new ProveedorFirmaException(ProveedorFirmaException.REPRESENTANTE_SIN_FIRMA,
                "Security Data no tiene una firma de representante legal activa y vigente delegada para la cédula configurada"
                + " (saa.firma.securitydata.cedulaRepresentante)");
        }
        return mejor;
    }

    private static String sinTildes(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static void agregarParticipante(ArrayNode participantes, String tipo, String signatoryId,
            java.util.List<PosicionFirma> posiciones) {
        ObjectNode p = participantes.addObject();
        p.put("typeSigner", tipo);
        p.put("signatoryId", signatoryId);
        ArrayNode lista = p.putArray("listPositions");
        for (PosicionFirma pos : posiciones) {
            ObjectNode o = lista.addObject();
            o.put("posX", numero(pos.getX()));
            o.put("posY", numero(pos.getY()));
            o.put("numPage", String.valueOf(pos.getNumPage()));
            o.put("width", numero(pos.getWidth()));
            o.put("height", numero(pos.getHeight()));
        }
    }

    // ========================================================================
    // consultar / descargar / anular
    // ========================================================================

    @Override
    public EstadoTramite consultar(String idExterno) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.consultar - idExterno: " + idExterno);
        Configuracion cfg = configuracion(false);
        JsonNode result = taskDetail(cfg, idExterno);

        EstadoTramite e = new EstadoTramite();
        String tarea = result.path("taskStatus").asText("");
        switch (tarea.startsWith(PREFIJO_ESTADO_TAREA) ? tarea.substring(PREFIJO_ESTADO_TAREA.length()) : tarea) {
            case "COMPLETADO":
                e.setEstado(EstadoFirmaContrato.FIRMADO);
                break;
            case "RECHAZADO":
            case "RECHAZADO_BIOMETRIX":
                e.setEstado(EstadoFirmaContrato.RECHAZADO);
                break;
            case "CADUCADO":
                e.setEstado(EstadoFirmaContrato.VENCIDO);
                break;
            case "CANCELADO":
                e.setEstado(EstadoFirmaContrato.ANULADO);
                break;
            case "PROCESO":
            case "PRE_COMPLETADO":
                e.setEstado(EstadoFirmaContrato.ENVIADO);
                break;
            default:
                throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                    "estado de tarea no reconocido: " + tarea);
        }

        // Solo el partícipe (TP_SIGNER_NORMAL) es firmante en el SAA; el representante firma embebido. El enlace
        // (signerToken) no se lee.
        JsonNode firmantes = result.path("signatories");
        if (firmantes.isArray()) {
            for (JsonNode s : firmantes) {
                String rol = s.path("tpRolSigner").asText(SIGNER_NORMAL);
                if (!SIGNER_NORMAL.equals(rol)) {
                    continue;
                }
                EstadoFirmante ef = new EstadoFirmante();
                ef.setOrden(1);
                String estado = s.path("signatoryStatus").asText("");
                estado = estado.startsWith(PREFIJO_ESTADO_FIRMANTE) ? estado.substring(PREFIJO_ESTADO_FIRMANTE.length()) : estado;
                if ("FIRMADO".equals(estado)) {
                    ef.setEstado(EstadoFirmanteContrato.FIRMADO);
                    ef.setFechaFirma(fechaEvidencia(s.get("evidence"), "Firmado"));
                } else if ("RECHAZADO".equals(estado)) {
                    ef.setEstado(EstadoFirmanteContrato.RECHAZADO);
                    String motivo = s.path("rejectionReason").asText(null);
                    if (!vacio(motivo)) {
                        e.setMotivoRechazo(motivo);
                    }
                } else {
                    ef.setEstado(EstadoFirmanteContrato.PENDIENTE);
                }
                e.getFirmantes().add(ef);
                break;
            }
        }
        return e;
    }

    @Override
    public byte[] descargarFirmado(String idExterno) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.descargarFirmado - idExterno: " + idExterno);
        Configuracion cfg = configuracion(false);
        JsonNode result = taskDetail(cfg, idExterno);
        String pdfId = null;
        JsonNode pdfs = result.path("detailPdf");
        if (pdfs.isArray() && pdfs.size() > 0) {
            pdfId = pdfs.get(0).path("pdfId").asText(null);
        }
        if (vacio(pdfId)) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                "la tarea no tiene todavía el PDF firmado (pdfId); estado: " + result.path("taskStatus").asText(""));
        }
        JsonNode respuesta = llamarJson(cfg, "POST", "/base64?pdfId=" + codificar(pdfId), null);
        String base64 = respuesta.path("result").path("base64").asText(null);
        if (vacio(base64)) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "base64 no devolvió el PDF firmado");
        }
        try {
            return Base64.getMimeDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "el PDF firmado no es un base64 válido");
        }
    }

    @Override
    public void anular(String idExterno, String motivo) throws ProveedorFirmaException {
        System.out.println("SecurityDataProveedor.anular - idExterno: " + idExterno);
        Configuracion cfg = configuracion(false);
        if (vacio(idExterno)) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "el trámite no tiene identificador externo");
        }
        // cancelTask solo cancela tareas en PROCESO: si no lo está, el proveedor responde con su mensaje y el trámite sigue vivo.
        llamarJson(cfg, "GET", "/cancelTask/" + codificar(idExterno.trim()), null);
    }

    private JsonNode taskDetail(Configuracion cfg, String idExterno) throws ProveedorFirmaException {
        if (vacio(idExterno)) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "el trámite no tiene identificador externo");
        }
        JsonNode respuesta = llamarJson(cfg, "GET", "/taskDetail/" + codificar(idExterno.trim()), null);
        JsonNode result = respuesta.get("result");
        if (result == null || !result.isObject()) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "taskDetail no devolvió el detalle de la tarea");
        }
        return result;
    }

    // ========================================================================
    // HTTP
    // ========================================================================

    /** Llamada JSON con token; ante un 401 renueva el token y reintenta una vez. */
    private JsonNode llamarJson(Configuracion cfg, String metodo, String ruta, String cuerpo) throws ProveedorFirmaException {
        return ejecutar(cfg, ruta, (token) -> {
            HttpRequest.Builder b = base(cfg, ruta, token);
            if ("GET".equals(metodo)) {
                b.GET();
            } else {
                b.header("Content-Type", "application/json");
                b.POST(cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
            }
            return b.build();
        });
    }

    private JsonNode multipart(Configuracion cfg, String ruta, String nombrePdf, byte[] pdf, String detalle)
            throws ProveedorFirmaException {
        String limite = "----saa" + UUID.randomUUID().toString().replace("-", "");
        byte[] cuerpo;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(pdf.length + 2048);
            escribir(out, "--" + limite + "\r\nContent-Disposition: form-data; name=\"files\"; filename=\""
                + nombrePdf.replace("\"", "") + "\"\r\nContent-Type: application/pdf\r\n\r\n");
            out.write(pdf);
            escribir(out, "\r\n--" + limite + "\r\nContent-Disposition: form-data; name=\"detail\"\r\n"
                + "Content-Type: application/json\r\n\r\n");
            escribir(out, detalle);
            escribir(out, "\r\n--" + limite + "--\r\n");
            cuerpo = out.toByteArray();
        } catch (IOException e) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "no se pudo armar la solicitud");
        }
        return ejecutar(cfg, ruta, (token) -> base(cfg, ruta, token)
            .header("Content-Type", "multipart/form-data; boundary=" + limite)
            .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo)).build());
    }

    private static void escribir(ByteArrayOutputStream out, String texto) throws IOException {
        out.write(texto.getBytes(StandardCharsets.UTF_8));
    }

    private interface ConstructorSolicitud {
        HttpRequest construir(String token);
    }

    private JsonNode ejecutar(Configuracion cfg, String ruta, ConstructorSolicitud constructor) throws ProveedorFirmaException {
        String token = token(cfg, false);
        HttpResponse<String> r = enviarHttp(constructor.construir(token));
        if (r.statusCode() == 401) {
            token = token(cfg, true);
            r = enviarHttp(constructor.construir(token));
        }
        return interpretar(r, ruta);
    }

    private HttpRequest.Builder base(Configuracion cfg, String ruta, String token) {
        return HttpRequest.newBuilder(URI.create(cfg.url + ruta)).timeout(Duration.ofSeconds(60))
            .header("Authorization", "Bearer " + token).header("Accept", "application/json");
    }

    private static HttpResponse<String> enviarHttp(HttpRequest solicitud) throws ProveedorFirmaException {
        try {
            return HTTP.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProveedorFirmaException(ProveedorFirmaException.PROVEEDOR_NO_DISPONIBLE, "se interrumpió la llamada al proveedor");
        } catch (IOException | IllegalArgumentException e) {
            // El mensaje de la excepción puede traer la URL pero nunca el cuerpo ni los encabezados.
            throw new ProveedorFirmaException(ProveedorFirmaException.PROVEEDOR_NO_DISPONIBLE,
                "no se pudo comunicar con Security Data (" + e.getClass().getSimpleName() + ")");
        }
    }

    /** Respuesta estándar {status, statusCode, message, result, detail}; HTTP no 2xx o status=false → ERROR_PROVEEDOR. */
    private JsonNode interpretar(HttpResponse<String> r, String ruta) throws ProveedorFirmaException {
        JsonNode raiz = null;
        try {
            raiz = r.body() == null || r.body().isEmpty() ? null : JSON.readTree(r.body());
        } catch (IOException e) {
            // se trata abajo
        }
        boolean httpOk = r.statusCode() >= 200 && r.statusCode() < 300;
        boolean statusOk = raiz == null || !raiz.has("status") || raiz.path("status").asBoolean(true);
        if (!httpOk || !statusOk) {
            String mensaje = raiz != null ? raiz.path("message").asText("") : "";
            String detalle = raiz != null ? raiz.path("detail").asText("") : "";
            throw new ProveedorFirmaException(ProveedorFirmaException.ERROR_PROVEEDOR,
                "Security Data respondió " + r.statusCode() + " en " + ruta.split("[?/]", 3)[1]
                + (vacio(mensaje) ? "" : ": " + mensaje) + (vacio(detalle) ? "" : " (" + detalle + ")"));
        }
        if (raiz == null) {
            throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA, "respuesta vacía o no JSON de Security Data");
        }
        return raiz;
    }

    /** authenticateUser. El {@code result} llega como TEXTO con el JWT (verificado); se acepta también {@code {token}}. */
    private String token(Configuracion cfg, boolean renovar) throws ProveedorFirmaException {
        synchronized (CERROJO_TOKEN) {
            if (!renovar && tokenCache != null && System.currentTimeMillis() < tokenCacheHasta) {
                return tokenCache;
            }
            ObjectNode cuerpo = JSON.createObjectNode();
            cuerpo.put("username", cfg.usuario);
            cuerpo.put("password", cfg.clave);
            HttpRequest solicitud = HttpRequest.newBuilder(URI.create(cfg.url + "/authenticateUser"))
                .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString(), StandardCharsets.UTF_8)).build();
            JsonNode raiz = interpretar(enviarHttp(solicitud), "/authenticateUser");
            JsonNode result = raiz.get("result");
            String t = result == null || result.isNull() ? null
                : (result.isTextual() ? result.asText() : result.path("token").asText(null));
            if (vacio(t)) {
                throw new ProveedorFirmaException(ProveedorFirmaException.RESPUESTA_INVALIDA,
                    "authenticateUser no devolvió el token");
            }
            tokenCache = t.trim();
            tokenCacheHasta = System.currentTimeMillis() + VIGENCIA_TOKEN_MS;
            return tokenCache;
        }
    }

    // ========================================================================
    // Utilidades
    // ========================================================================

    /** Celular en el formato local 09XXXXXXXX; null si no se puede normalizar. */
    static String celularLocal(String celular) {
        if (celular == null) {
            return null;
        }
        String d = celular.replaceAll("\\D", "");
        if (d.startsWith("593") && d.length() == 12) {
            d = "0" + d.substring(3);
        } else if (d.length() == 9 && d.startsWith("9")) {
            d = "0" + d;
        }
        return d.matches("09\\d{8}") ? d : null;
    }

    private static LocalDateTime fechaEvidencia(JsonNode evidencias, String estado) {
        LocalDateTime ultima = null;
        if (evidencias != null && evidencias.isArray()) {
            for (JsonNode ev : evidencias) {
                if (estado.equalsIgnoreCase(ev.path("evidenceStatus").asText())) {
                    try {
                        LocalDateTime f = LocalDateTime.parse(ev.path("date").asText(), FECHA_EVIDENCIA);
                        if (ultima == null || f.isAfter(ultima)) {
                            ultima = f;
                        }
                    } catch (RuntimeException e) {
                        // fecha ilegible: se ignora esa evidencia
                    }
                }
            }
        }
        return ultima;
    }

    private static String resumir(JsonNode nodo) {
        if (nodo == null || nodo.isNull() || (nodo.isArray() && nodo.size() == 0)) {
            return "ninguno";
        }
        String t = nodo.toString();
        return t.length() > 500 ? t.substring(0, 500) + "…" : t;
    }

    private static String numero(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private static String codificar(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static boolean vacio(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Lee la configuración de las propiedades de sistema. Si falta alguna obligatoria lanza {@code PROVEEDOR_NO_CONFIGURADO}
     * nombrando TODAS las que faltan (solo los nombres). La del representante solo es obligatoria al enviar.
     */
    private Configuracion configuracion(boolean paraEnviar) throws ProveedorFirmaException {
        String url = propiedad(PROP_URL);
        String usuario = propiedad(PROP_USUARIO);
        String clave = propiedad(PROP_CLAVE);
        String ruc = propiedad(PROP_RUC);
        String representante = propiedad(PROP_REFERENCIA_REPRESENTANTE);
        String cedulaRepresentante = propiedad(PROP_CEDULA_REPRESENTANTE);

        StringBuilder faltan = new StringBuilder();
        agregarSiFalta(faltan, PROP_URL, url);
        agregarSiFalta(faltan, PROP_USUARIO, usuario);
        agregarSiFalta(faltan, PROP_CLAVE, clave);
        agregarSiFalta(faltan, PROP_RUC, ruc);
        if (paraEnviar) {
            agregarSiFalta(faltan, PROP_CEDULA_REPRESENTANTE, cedulaRepresentante);
        }
        if (faltan.length() > 0) {
            throw new ProveedorFirmaException(ProveedorFirmaException.PROVEEDOR_NO_CONFIGURADO,
                "faltan propiedades de sistema de WildFly: " + faltan);
        }
        String metodo = propiedad(PROP_METODO_NOTIFICACION);
        return new Configuracion(url.endsWith("/") ? url.substring(0, url.length() - 1) : url, usuario, clave, ruc, representante,
            cedulaRepresentante, metodo != null ? metodo : METODO_NOTIFICACION_DEFECTO,
            entero(PROP_DIAS_VIGENCIA, DIAS_VIGENCIA_DEFECTO), entero(PROP_HORAS_RENOTIFICACION, HORAS_RENOTIFICACION_DEFECTO));
    }

    private static int entero(String nombre, int defecto) {
        String v = propiedad(nombre);
        try {
            int n = v == null ? defecto : Integer.parseInt(v);
            return n > 0 ? n : defecto;
        } catch (NumberFormatException e) {
            return defecto;
        }
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

    /** Configuración leída. La clave existe solo en memoria: el toString no la incluye. */
    private static final class Configuracion {
        private final String url;
        private final String usuario;
        private final String clave;
        private final String ruc;
        private final String referenciaRepresentante;
        private final String cedulaRepresentante;
        private final String metodoNotificacion;
        private final int diasVigencia;
        private final int horasRenotificacion;

        Configuracion(String url, String usuario, String clave, String ruc, String referenciaRepresentante,
                String cedulaRepresentante, String metodoNotificacion, int diasVigencia, int horasRenotificacion) {
            this.url = url;
            this.usuario = usuario;
            this.clave = clave;
            this.ruc = ruc;
            this.referenciaRepresentante = referenciaRepresentante;
            this.cedulaRepresentante = cedulaRepresentante;
            this.metodoNotificacion = metodoNotificacion;
            this.diasVigencia = diasVigencia;
            this.horasRenotificacion = horasRenotificacion;
        }

        @Override
        public String toString() {
            return "Configuracion[url=" + url + ", usuario=" + usuario + ", ruc=" + ruc + "]";
        }
    }
}
