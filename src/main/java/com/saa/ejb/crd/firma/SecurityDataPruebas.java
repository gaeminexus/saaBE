package com.saa.ejb.crd.firma;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.firma.SecurityDataProveedor.Configuracion;

/**
 * Pantalla de pruebas paso a paso de Security Data (diseño: crd/DISENO-PANTALLA-PRUEBAS-SECURITY-DATA.md). Cada paso hace UNA
 * llamada a la API del proveedor con el cliente, el token y la URL de {@link SecurityDataProveedor} y devuelve lo enviado y
 * lo recibido ENMASCARADO ({@link SecurityDataEnmascarador}). No toca la base: no graba trámites, firmantes ni adjuntos.
 *
 * <p>Interruptor: propiedad de sistema {@code saa.firma.securitydata.pruebas=true}. En el log solo van la ruta, el HTTP y la
 * duración: ni cuerpos, ni token, ni código dactilar.</p>
 */
public final class SecurityDataPruebas {

    public static final String PROP_PRUEBAS = "saa.firma.securitydata.pruebas";
    public static final String ERR_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";

    private static final String SOLO_ID = "[A-Za-z0-9_\\-]{1,100}";
    private static final String[] CAMPOS_FIRMANTE = {"tpSignature", "tpIdentification", "dni", "ruc", "email", "countryCode",
        "numPhone", "personPosition", "regulatedSuper", "reuseSignature", "fingerprintCode", "biometrixType", "biometrixParam",
        "signatureSource"};

    private SecurityDataPruebas() {
    }

    // ========================================================================
    // Modelo de respuesta
    // ========================================================================

    /** Un archivo recibido (multipart) o devuelto para descargar. */
    public static class Archivo {
        private final String nombre;
        private final byte[] contenido;

        public Archivo(String nombre, byte[] contenido) {
            this.nombre = nombre;
            this.contenido = contenido;
        }

        public String getNombre() { return nombre; }
        public byte[] getContenido() { return contenido; }
    }

    /** Respuesta común: {paso, metodo, ruta, peticion, httpStatus, duracionMs, respuesta} (+ resumen y archivo si aplican). */
    public static class Resultado {
        private int paso;
        private String metodo;
        private String ruta;
        private Object peticion;
        private int httpStatus;
        private long duracionMs;
        private Object respuesta;
        private Object resumen;
        private String pdfBase64;
        private String pdfNombre;
        // Uso interno (sin getter: no se serializa): la respuesta SIN enmascarar.
        private JsonNode crudo;

        public int getPaso() { return paso; }
        public String getMetodo() { return metodo; }
        public String getRuta() { return ruta; }
        public Object getPeticion() { return peticion; }
        public int getHttpStatus() { return httpStatus; }
        public long getDuracionMs() { return duracionMs; }
        public Object getRespuesta() { return respuesta; }
        /** Datos derivados útiles para la pantalla (paso 1: tipo y vigencia del token). */
        public Object getResumen() { return resumen; }
        /** PDF para descargar (pasos 7 y 8): el base64 COMPLETO (en {@code respuesta} va enmascarado). Null si no hay PDF. */
        public String getPdfBase64() { return pdfBase64; }
        public String getPdfNombre() { return pdfNombre; }
    }

    // ========================================================================
    // Interruptor y paso 0
    // ========================================================================

    public static boolean habilitadas() {
        return "true".equalsIgnoreCase(SecurityDataProveedor.propiedad(PROP_PRUEBAS));
    }

    /** Paso 0: la configuración tal como está, sin la clave y sin llamar a nadie. */
    public static Map<String, Object> configuracionActual() {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("paso", 0);
        c.put("habilitadas", habilitadas());
        c.put("url", SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_URL));
        c.put("usuario", SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_USUARIO));
        c.put("claveConfigurada", SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_CLAVE) != null);
        c.put("ruc", SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_RUC));
        c.put("cedulaRepresentante", SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_CEDULA_REPRESENTANTE));
        c.put("referenciaRepresentanteConfigurada",
            SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_REFERENCIA_REPRESENTANTE) != null);
        String metodo = SecurityDataProveedor.propiedad(SecurityDataProveedor.PROP_METODO_NOTIFICACION);
        c.put("metodoNotificacion", metodo != null ? metodo : SecurityDataProveedor.METODO_NOTIFICACION_DEFECTO);
        List<String> faltan = new ArrayList<>();
        for (String p : new String[] {SecurityDataProveedor.PROP_URL, SecurityDataProveedor.PROP_USUARIO,
            SecurityDataProveedor.PROP_CLAVE, SecurityDataProveedor.PROP_RUC}) {
            if (SecurityDataProveedor.propiedad(p) == null) {
                faltan.add(p);
            }
        }
        c.put("propiedadesFaltantes", faltan);
        return c;
    }

    // ========================================================================
    // Pasos 1 a 9
    // ========================================================================

    /**
     * Ejecuta un paso (1 a 9). {@code cuerpo}: JSON del formulario (en los multipart, el de la parte {@code datos});
     * {@code archivos}: los PDF de la parte {@code archivo}.
     */
    public static Resultado ejecutar(int paso, JsonNode cuerpo, List<Archivo> archivos) throws ProveedorFirmaException {
        Configuracion cfg = new SecurityDataProveedor().configuracion(false);
        JsonNode in = cuerpo != null && cuerpo.isObject() ? cuerpo : SecurityDataProveedor.JSON.createObjectNode();
        switch (paso) {
            case 1:
                return autenticar(cfg);
            case 2:
                return validarFirmante(cfg, in);
            case 3:
                return completarDatos(cfg, in, archivos);
            case 4:
                return firmaRepresentante(cfg, in);
            case 5:
                return crearTarea(cfg, in, archivos);
            case 6:
                return llamar(cfg, 6, "GET", "/taskDetail/" + id(in, "processId"), null, null, null);
            case 7:
                return descargarPdf(cfg, 7, "POST", "/base64?pdfId=" + id(in, "pdfId"), "firmado-" + id(in, "pdfId") + ".pdf");
            case 8:
                return descargarPdf(cfg, 8, "GET", "/traceabilityCertificate/" + id(in, "processId"),
                    "trazabilidad-" + id(in, "processId") + ".pdf");
            case 9:
                return llamar(cfg, 9, "GET", "/cancelTask/" + id(in, "processId"), null, null, null);
            default:
                throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": el paso debe estar entre 0 y 9");
        }
    }

    private static Resultado autenticar(Configuracion cfg) throws ProveedorFirmaException {
        ObjectNode cuerpo = SecurityDataProveedor.JSON.createObjectNode();
        cuerpo.put("username", cfg.usuario);
        cuerpo.put("password", cfg.clave);
        Resultado r = llamar(cfg, 1, "POST", "/authenticateUser", cuerpo, null, null);
        // El token llega como texto (o {token}): se calcula su tipo y vigencia desde el propio JWT, sin mostrarlo.
        JsonNode raiz = r.crudo;
        String token = null;
        if (r.httpStatus == 200 && raiz != null && raiz.get("result") != null) {
            JsonNode res = raiz.get("result");
            token = res.isTextual() ? res.asText() : res.path("token").asText(null);
        }
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("exito", r.httpStatus == 200);
        resumen.put("tipo", "Bearer");
        resumen.put("tokenEnmascarado", token != null ? SecurityDataEnmascarador.token(token) : null);
        resumen.put("expira", token != null ? expiraJwt(token) : null);
        r.resumen = resumen;
        return r;
    }

    private static Resultado validarFirmante(Configuracion cfg, JsonNode in) throws ProveedorFirmaException {
        ObjectNode f = SecurityDataProveedor.JSON.createObjectNode();
        f.put("tpSignature", texto(in, "tpSignature", "SC_TP_F_AVANZADA_PN"));
        f.put("tpIdentification", texto(in, "tpIdentification", "TIPO_IDENTIFICACION_CEDULA"));
        f.put("ruc", texto(in, "ruc", ""));
        f.put("personPosition", texto(in, "personPosition", ""));
        f.put("countryCode", texto(in, "countryCode", "+593"));
        f.put("regulatedSuper", in.path("regulatedSuper").asBoolean(false));
        f.put("reuseSignature", in.path("reuseSignature").asBoolean(false));
        for (String k : CAMPOS_FIRMANTE) {
            if (!f.has(k) && in.hasNonNull(k)) {
                f.set(k, in.get(k));
            }
        }
        if (!f.hasNonNull("dni") || f.path("dni").asText().trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": dni es obligatorio");
        }
        ArrayNode cuerpo = SecurityDataProveedor.JSON.createArrayNode();
        cuerpo.add(f);
        return llamar(cfg, 2, "POST", "/signersValidation", cuerpo, null, null);
    }

    /**
     * Paso 3 (completar-datos): {@code datos} = {signatoryId, campos:{CLAVE:valor}}; los PDF van en {@code archivos}
     * ({NEMONICO}_{DNI}.pdf). Se arma el part {@code signatories} que pide el proveedor.
     */
    private static Resultado completarDatos(Configuracion cfg, JsonNode in, List<Archivo> archivos) throws ProveedorFirmaException {
        String signatoryId = texto(in, "signatoryId", "").trim();
        if (signatoryId.isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": signatoryId es obligatorio");
        }
        ArrayNode firmantes = SecurityDataProveedor.JSON.createArrayNode();
        ObjectNode f = firmantes.addObject();
        f.put("signatoryId", signatoryId);
        ArrayNode lista = f.putArray("listFields");
        JsonNode campos = in.get("campos");
        if (campos != null && campos.isObject()) {
            campos.fields().forEachRemaining(e -> {
                ObjectNode c = lista.addObject();
                c.put("key", e.getKey());
                c.put("value", e.getValue().asText());
            });
        }
        List<Parte> partes = new ArrayList<>();
        partes.add(Parte.texto("signatories", firmantes.toString(), "application/json"));
        for (Archivo a : archivos) {
            partes.add(Parte.archivo("files", a.getNombre(), a.getContenido()));
        }
        return llamar(cfg, 3, "POST", "/completeSignatoryData", firmantes, partes, archivos);
    }

    /**
     * Paso 4 (firma-representante): {@code accion} BUSCAR (findSignatureReferences: dni y/o names) o CONSULTAR
     * (consultSDSignature: tpSignature, dni, ruc). Sin accion: CONSULTAR si viene tpSignature, si no BUSCAR.
     */
    private static Resultado firmaRepresentante(Configuracion cfg, JsonNode in) throws ProveedorFirmaException {
        String accion = texto(in, "accion", in.hasNonNull("tpSignature") && !in.get("tpSignature").asText().trim().isEmpty()
            ? "CONSULTAR" : "BUSCAR").toUpperCase();
        ObjectNode cuerpo = SecurityDataProveedor.JSON.createObjectNode();
        if (accion.equals("CONSULTAR")) {
            if (!in.hasNonNull("tpSignature") || !in.hasNonNull("dni")) {
                throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": CONSULTAR requiere tpSignature y dni");
            }
            cuerpo.put("tpSignature", in.get("tpSignature").asText());
            cuerpo.put("dni", in.get("dni").asText());
            cuerpo.put("ruc", texto(in, "ruc", ""));
            return llamar(cfg, 4, "POST", "/consultSDSignature", cuerpo, null, null);
        }
        if (!accion.equals("BUSCAR")) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": accion debe ser BUSCAR o CONSULTAR");
        }
        String dni = texto(in, "dni", "");
        String names = texto(in, "names", "");
        if (dni.trim().isEmpty() && names.trim().isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": BUSCAR requiere dni y/o names");
        }
        cuerpo.put("dni", dni);
        cuerpo.put("names", names);
        return llamar(cfg, 4, "POST", "/findSignatureReferences", cuerpo, null, null);
    }

    /**
     * Paso 5 (crear-tarea): campos planos del formulario → {@code detail} de createTask. Firmantes: socio* y representante*
     * (SignatoryId, Tipo, Pagina, X, Y, Ancho, Alto); si trae {@code socioPosiciones} / {@code representantePosiciones}
     * (arreglo de {pagina,x,y,ancho,alto}) se usan en vez de la posición única (p. ej. contrato + Anexo I). Defaults:
     * socio NORMAL, representante EMBEDDED, 7 días, 24 h, TIPO_ORDEN_CADENA_SECU_CONTRATO, ENVIO_OTPN_CORREO_WSS, showPdf=true.
     */
    private static Resultado crearTarea(Configuracion cfg, JsonNode in, List<Archivo> archivos) throws ProveedorFirmaException {
        if (archivos == null || archivos.isEmpty()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": falta el PDF (parte 'archivo') o el idContrato");
        }
        ObjectNode d = SecurityDataProveedor.JSON.createObjectNode();
        d.put("showPdf", in.has("showPdf") ? in.get("showPdf").asBoolean(true) : true);
        d.put("nickname", texto(in, "nickname", "Prueba de firma ASOPREP-FCPC"));
        ArrayNode pdfs = d.putArray("lisPdfDetail");
        ObjectNode pdf = pdfs.addObject();
        pdf.put("pdfName", archivos.get(0).getNombre());
        ArrayNode participantes = pdf.putArray("listparticipants");
        agregarParticipante(participantes, in, "socio", "TP_SIGNER_NORMAL");
        agregarParticipante(participantes, in, "representante", "TP_SIGNER_EMBEDDED");
        if (participantes.size() == 0) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": hace falta al menos un firmante (socioSignatoryId o representanteSignatoryId)");
        }

        int dias = Math.max(1, in.path("diasVigencia").asInt(SecurityDataProveedor.DIAS_VIGENCIA_DEFECTO));
        int horas = Math.max(0, in.path("horasRenotificacion").asInt(SecurityDataProveedor.HORAS_RENOTIFICACION_DEFECTO));
        int minutos = Math.max(15, Math.min(horas * 60, 23 * 60 + 59));
        ObjectNode n = d.putObject("notification");
        n.put("dateEnd", java.time.LocalDateTime.now().plusDays(dias).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        n.put("hourNoti", minutos / 60);
        n.put("minNoti", minutos % 60);
        n.put("typeNotification", texto(in, "typeNotification", "TIPO_ORDEN_CADENA_SECU_CONTRATO"));
        d.put("methodNotification", texto(in, "methodNotification", SecurityDataProveedor.METODO_NOTIFICACION_DEFECTO));

        List<Parte> partes = new ArrayList<>();
        for (Archivo a : archivos) {
            partes.add(Parte.archivo("files", a.getNombre(), a.getContenido()));
        }
        partes.add(Parte.texto("detail", d.toString(), "application/json"));
        return llamar(cfg, 5, "POST", "/createTask", d, partes, archivos);
    }

    private static void agregarParticipante(ArrayNode participantes, JsonNode in, String prefijo, String tipoDefecto) {
        String id = texto(in, prefijo + "SignatoryId", "").trim();
        if (id.isEmpty()) {
            return;
        }
        ObjectNode p = participantes.addObject();
        p.put("typeSigner", texto(in, prefijo + "Tipo", tipoDefecto));
        p.put("signatoryId", id);
        ArrayNode posiciones = p.putArray("listPositions");
        JsonNode varias = in.get(prefijo + "Posiciones");
        if (varias != null && varias.isArray() && varias.size() > 0) {
            for (JsonNode v : varias) {
                posicion(posiciones, v.path("pagina"), v.path("x"), v.path("y"), v.path("ancho"), v.path("alto"));
            }
        } else {
            posicion(posiciones, in.path(prefijo + "Pagina"), in.path(prefijo + "X"), in.path(prefijo + "Y"),
                in.path(prefijo + "Ancho"), in.path(prefijo + "Alto"));
        }
    }

    /** posX/posY/numPage como pide el proveedor (texto); width/height solo si vienen. */
    private static void posicion(ArrayNode lista, JsonNode pagina, JsonNode x, JsonNode y, JsonNode ancho, JsonNode alto) {
        if (pagina.isMissingNode() || pagina.isNull() || x.isMissingNode() || y.isMissingNode()) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": cada firmante necesita página, x e y de la firma");
        }
        ObjectNode o = lista.addObject();
        o.put("posX", x.asText());
        o.put("posY", y.asText());
        o.put("numPage", pagina.asText());
        if (!ancho.isMissingNode() && !ancho.isNull() && !ancho.asText().isEmpty()) {
            o.put("width", ancho.asText());
        }
        if (!alto.isMissingNode() && !alto.isNull() && !alto.asText().isEmpty()) {
            o.put("height", alto.asText());
        }
    }

    /** Pasos 7 y 8: la respuesta trae el PDF en {@code result.base64}; se devuelve aparte en {@code archivo} y se enmascara en {@code respuesta}. */
    private static Resultado descargarPdf(Configuracion cfg, int paso, String metodo, String ruta, String nombreDefecto)
            throws ProveedorFirmaException {
        Resultado r = llamar(cfg, paso, metodo, ruta, null, null, null);
        JsonNode res = r.crudo == null ? null : r.crudo.get("result");
        if (res != null && res.isObject() && SecurityDataEnmascarador.esBase64Largo(res.path("base64").asText(""))) {
            r.pdfBase64 = res.get("base64").asText();
            r.pdfNombre = res.hasNonNull("pdfName") ? res.get("pdfName").asText() : nombreDefecto;
        }
        return r;
    }

    // ========================================================================
    // HTTP
    // ========================================================================

    private static final class Parte {
        final String nombre;
        final String archivo;
        final String tipo;
        final byte[] contenido;

        private Parte(String nombre, String archivo, String tipo, byte[] contenido) {
            this.nombre = nombre;
            this.archivo = archivo;
            this.tipo = tipo;
            this.contenido = contenido;
        }

        static Parte texto(String nombre, String valor, String tipo) {
            return new Parte(nombre, null, tipo, valor.getBytes(StandardCharsets.UTF_8));
        }

        static Parte archivo(String nombre, String archivo, byte[] contenido) {
            return new Parte(nombre, archivo.replace("\"", ""), "application/pdf", contenido);
        }
    }

    /**
     * Una llamada con token (salvo la 1). No lanza por un HTTP de error: lo devuelve en {@code httpStatus}/{@code respuesta}
     * para mostrarlo. Lanza solo si no hay comunicación con el proveedor.
     */
    private static Resultado llamar(Configuracion cfg, int paso, String metodo, String ruta, JsonNode cuerpoJson,
            List<Parte> partes, List<Archivo> archivos) throws ProveedorFirmaException {
        SecurityDataProveedor p = new SecurityDataProveedor();
        String limite = "----saa" + UUID.randomUUID().toString().replace("-", "");
        byte[] cuerpo = null;
        String tipoContenido = null;
        if (partes != null) {
            cuerpo = multipart(limite, partes);
            tipoContenido = "multipart/form-data; boundary=" + limite;
        } else if (cuerpoJson != null) {
            cuerpo = cuerpoJson.toString().getBytes(StandardCharsets.UTF_8);
            tipoContenido = "application/json";
        }
        final byte[] cuerpoFinal = cuerpo;
        final String tipoFinal = tipoContenido;

        long inicio = System.nanoTime();
        HttpResponse<String> resp = enviar(cfg, p, paso, metodo, ruta, cuerpoFinal, tipoFinal, false);
        if (resp.statusCode() == 401 && paso != 1) {
            resp = enviar(cfg, p, paso, metodo, ruta, cuerpoFinal, tipoFinal, true);
        }
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.println("SecurityDataPruebas paso=" + paso + " ruta=" + ruta.split("\\?")[0].replaceAll("/[^/]*$", "/…")
            + " HTTP=" + resp.statusCode() + " ms=" + ms);

        Resultado r = new Resultado();
        r.paso = paso;
        r.metodo = metodo;
        r.ruta = ruta;
        r.httpStatus = resp.statusCode();
        r.duracionMs = ms;

        Map<String, Object> peticion = new LinkedHashMap<>();
        if (cuerpoJson != null) {
            peticion.put("cuerpo", conv(SecurityDataEnmascarador.enmascarar(cuerpoJson)));
        }
        if (archivos != null && !archivos.isEmpty()) {
            List<Map<String, Object>> lista = new ArrayList<>();
            for (Archivo a : archivos) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("nombre", a.getNombre());
                m.put("bytes", a.getContenido().length);
                lista.add(m);
            }
            peticion.put("archivos", lista);
        }
        r.peticion = peticion;

        JsonNode raiz = null;
        try {
            raiz = resp.body() == null || resp.body().isEmpty() ? null : SecurityDataProveedor.JSON.readTree(resp.body());
        } catch (java.io.IOException e) {
            // no es JSON: se muestra como texto (acotado)
        }
        if (raiz != null) {
            r.crudo = raiz;
            r.respuesta = conv(SecurityDataEnmascarador.enmascarar(raiz));
        } else {
            String t = resp.body() == null ? "" : resp.body();
            r.respuesta = t.length() > 2000 ? t.substring(0, 2000) + "…" : t;
        }
        return r;
    }

    private static HttpResponse<String> enviar(Configuracion cfg, SecurityDataProveedor p, int paso, String metodo, String ruta,
            byte[] cuerpo, String tipo, boolean renovarToken) throws ProveedorFirmaException {
        HttpRequest.Builder b;
        if (paso == 1) {
            b = HttpRequest.newBuilder(java.net.URI.create(cfg.url + ruta)).timeout(Duration.ofSeconds(60))
                .header("Accept", "application/json");
        } else {
            b = p.base(cfg, ruta, p.token(cfg, renovarToken));
        }
        if (tipo != null) {
            b.header("Content-Type", tipo);
        }
        if ("GET".equals(metodo)) {
            b.GET();
        } else {
            b.POST(cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(cuerpo));
        }
        return SecurityDataProveedor.enviarHttp(b.build());
    }

    private static byte[] multipart(String limite, List<Parte> partes) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            for (Parte p : partes) {
                out.write(("--" + limite + "\r\nContent-Disposition: form-data; name=\"" + p.nombre + "\""
                    + (p.archivo != null ? "; filename=\"" + p.archivo + "\"" : "")
                    + "\r\nContent-Type: " + p.tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                out.write(p.contenido);
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("no se pudo armar la solicitud multipart");
        }
        return out.toByteArray();
    }

    // ========================================================================
    // Utilidades
    // ========================================================================

    private static Object conv(JsonNode n) {
        return SecurityDataProveedor.JSON.convertValue(n, Object.class);
    }

    private static String texto(JsonNode in, String campo, String defecto) {
        return in.hasNonNull(campo) ? in.get(campo).asText() : defecto;
    }

    /** Identificador que va en la ruta: solo letras, números, guion y guion bajo (nada de caracteres de ruta). */
    private static String id(JsonNode in, String campo) {
        String v = texto(in, campo, "").trim();
        if (!v.matches(SOLO_ID)) {
            throw new IncomeException(ERR_PARAMETRO_INVALIDO + ": " + campo + " es obligatorio (letras, números, guion)");
        }
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    /** Fecha de vencimiento (claim exp) de un JWT, o null si no se puede leer. */
    private static String expiraJwt(String jwt) {
        try {
            String[] partes = jwt.split("\\.");
            JsonNode carga = SecurityDataProveedor.JSON.readTree(Base64.getUrlDecoder().decode(partes[1]));
            return carga.hasNonNull("exp") ? Instant.ofEpochSecond(carga.get("exp").asLong()).toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
