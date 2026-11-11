package com.saa.ws.rest.crd;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saa.basico.ejb.UsuarioValidacionService;
import com.saa.basico.util.IncomeException;
import com.saa.basico.util.UsuarioInvalidoException;
import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionFirma;
import com.saa.ejb.crd.firma.ProveedorFirmaElectronica.PosicionesFirma;
import com.saa.ejb.crd.firma.ProveedorFirmaException;
import com.saa.ejb.crd.firma.SecurityDataPruebas;
import com.saa.ejb.crd.firma.SecurityDataPruebas.Archivo;
import com.saa.ejb.crd.service.SecurityDataPruebasService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Pantalla de pruebas paso a paso de Security Data (pasos 0 a 9). Contrato para el FE:
 * docs/logica-negocio/crd/API-PRUEBAS-SECURITY-DATA.md. Sin el interruptor {@code saa.firma.securitydata.pruebas=true}
 * todo responde 403 PRUEBAS_DESHABILITADAS salvo el paso 0. Las pruebas no graban nada.
 *
 * <p>No se escribe en el log ningún cuerpo, token ni código dactilar.</p>
 */
@Path("sdpr")
public class SecurityDataPruebasRest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern NOMBRE_ARCHIVO = Pattern.compile("filename=\"([^\"]*)\"");

    @EJB
    private SecurityDataPruebasService pruebasService;

    @EJB
    private UsuarioValidacionService usuarioValidacionService;

    public SecurityDataPruebasRest() {
    }

    /** POST /rest/sdpr/configuracion?usuario — paso 0: configuración (nunca la clave). Responde aunque las pruebas estén apagadas. */
    @POST
    @Path("/configuracion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response configuracion(@QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST sdpr/configuracion");
        try {
            usuarioValidacionService.validar(usuario);
            return Response.ok(SecurityDataPruebas.configuracionActual()).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * POST /rest/sdpr/{ruta}?usuario — cuerpo JSON (para autenticar puede ir {@code {}}). Rutas: autenticar (paso 1),
     * validar-firmante (2), firma-representante (4), detalle-tarea (6), descargar-firmado (7), certificado-trazabilidad (8),
     * cancelar-tarea (9). completar-datos y crear-tarea son multipart (métodos aparte).
     */
    @POST
    // La plantilla se limita con una expresión regular a los pasos JSON: con «/{ruta}» a secas, un POST con cuerpo JSON a
    // «configuracion» o «posiciones-contrato» (que no declaran @Consumes) caía acá y respondía «ruta desconocida».
    @Path("/{ruta: autenticar|validar-firmante|firma-representante|detalle-tarea|descargar-firmado|certificado-trazabilidad|cancelar-tarea}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response pasoJson(@PathParam("ruta") String ruta, @QueryParam("usuario") String usuario, String cuerpo) {
        System.out.println("LLEGA AL SERVICIO POST sdpr/" + ruta);
        if ("configuracion".equals(ruta)) {
            return configuracion(usuario);
        }
        try {
            usuarioValidacionService.validar(usuario);
            int paso = paso(ruta);
            Response bloqueo = bloqueoSiApagado();
            if (bloqueo != null) {
                return bloqueo;
            }
            if (paso == 3 || paso == 5) {
                throw new IncomeException("PARAMETRO_INVALIDO: " + ruta + " es multipart/form-data (partes 'datos' y 'archivo')");
            }
            JsonNode json = cuerpo == null || cuerpo.trim().isEmpty() ? null : JSON.readTree(cuerpo);
            return ok(SecurityDataPruebas.ejecutar(paso, json, new ArrayList<Archivo>()));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * POST /rest/sdpr/completar-datos?usuario — paso 3, multipart: parte {@code datos} (JSON {signatoryId, campos:{CLAVE:valor}})
     * y parte {@code archivo} (0..n PDF llamados {NEMONICO}_{DNI}.pdf).
     */
    @POST
    @Path("/completar-datos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completarDatos(@QueryParam("usuario") String usuario, @MultipartForm MultipartFormDataInput input) {
        System.out.println("LLEGA AL SERVICIO POST sdpr/completar-datos");
        try {
            usuarioValidacionService.validar(usuario);
            Response bloqueo = bloqueoSiApagado();
            if (bloqueo != null) {
                return bloqueo;
            }
            Map<String, List<InputPart>> form = input.getFormDataMap();
            return ok(SecurityDataPruebas.ejecutar(3, datos(form), archivos(form, "archivo")));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * POST /rest/sdpr/crear-tarea?usuario — paso 5, multipart: parte {@code datos} (JSON con los campos planos del formulario;
     * {@code idContrato} opcional) y, si no hay {@code idContrato}, parte {@code archivo} (un PDF). Con {@code idContrato} se genera
     * RPRT_CNTR_ADHS de ese contrato. ⚠️ Crea la tarea de verdad: notifica por correo y WhatsApp al firmante.
     */
    @POST
    @Path("/crear-tarea")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response crearTarea(@QueryParam("usuario") String usuario, @MultipartForm MultipartFormDataInput input) {
        System.out.println("LLEGA AL SERVICIO POST sdpr/crear-tarea");
        try {
            usuarioValidacionService.validar(usuario);
            Response bloqueo = bloqueoSiApagado();
            if (bloqueo != null) {
                return bloqueo;
            }
            Map<String, List<InputPart>> form = input.getFormDataMap();
            JsonNode datos = datos(form);
            List<Archivo> archivos = archivos(form, "archivo");
            if (archivos.isEmpty() && datos != null && datos.hasNonNull("idContrato")
                    && !datos.get("idContrato").asText().trim().isEmpty()) {
                try {
                    archivos.add(pruebasService.generarPdfContrato(Long.valueOf(datos.get("idContrato").asText().trim())));
                } catch (NumberFormatException e) {
                    throw new IncomeException("PARAMETRO_INVALIDO: idContrato debe ser un número");
                }
            }
            return ok(SecurityDataPruebas.ejecutar(5, datos, archivos));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** GET /rest/sdpr/socio/{cedula}?usuario — paso 2, «llenar desde socio»: nombre, correo y celular de la entidad. */
    @GET
    @Path("/socio/{cedula}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response socio(@PathParam("cedula") String cedula, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO GET sdpr/socio");
        try {
            usuarioValidacionService.validar(usuario);
            Response bloqueo = bloqueoSiApagado();
            if (bloqueo != null) {
                return bloqueo;
            }
            return Response.ok(pruebasService.socioPorCedula(cedula)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /**
     * GET o POST /rest/sdpr/posiciones-contrato?idContrato=&usuario — paso 5, «calcular para el contrato»:
     * {@code {participe:[{numPage,x,y,width,height}], representante:[…]}} con la página real del PDF de ese contrato.
     */
    @GET
    @Path("/posiciones-contrato")
    @Produces(MediaType.APPLICATION_JSON)
    public Response posicionesGet(@QueryParam("idContrato") Long idContrato, @QueryParam("usuario") String usuario) {
        return posiciones(idContrato, usuario);
    }

    @POST
    @Path("/posiciones-contrato")
    @Produces(MediaType.APPLICATION_JSON)
    public Response posicionesPost(@QueryParam("idContrato") Long idContrato, @QueryParam("usuario") String usuario) {
        return posiciones(idContrato, usuario);
    }

    private Response posiciones(Long idContrato, String usuario) {
        System.out.println("LLEGA AL SERVICIO sdpr/posiciones-contrato - idContrato: " + idContrato);
        try {
            usuarioValidacionService.validar(usuario);
            Response bloqueo = bloqueoSiApagado();
            if (bloqueo != null) {
                return bloqueo;
            }
            PosicionesFirma p = pruebasService.calcularPosiciones(idContrato);
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("participe", posiciones(p.getParticipe()));
            cuerpo.put("representante", posiciones(p.getRepresentante()));
            return Response.ok(cuerpo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    // ------------------------------------------------------------------------

    /** Ruta de la pantalla → número de paso. */
    private static int paso(String ruta) {
        switch (ruta) {
            case "autenticar": return 1;
            case "validar-firmante": return 2;
            case "completar-datos": return 3;
            case "firma-representante": return 4;
            case "crear-tarea": return 5;
            case "detalle-tarea": return 6;
            case "descargar-firmado": return 7;
            case "certificado-trazabilidad": return 8;
            case "cancelar-tarea": return 9;
            default: throw new IncomeException("PARAMETRO_INVALIDO: ruta desconocida '" + ruta + "'");
        }
    }

    private JsonNode datos(Map<String, List<InputPart>> form) throws Throwable {
        String t = parteTexto(form, "datos");
        return t == null || t.trim().isEmpty() ? null : JSON.readTree(t);
    }


    private static List<Map<String, Object>> posiciones(List<PosicionFirma> lista) {
        List<Map<String, Object>> salida = new ArrayList<>();
        for (PosicionFirma p : lista) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("numPage", p.getNumPage());
            m.put("x", p.getX());
            m.put("y", p.getY());
            m.put("width", p.getWidth());
            m.put("height", p.getHeight());
            salida.add(m);
        }
        return salida;
    }

    private Response ok(SecurityDataPruebas.Resultado r) {
        return Response.ok(r).type(MediaType.APPLICATION_JSON).build();
    }

    private Response bloqueoSiApagado() {
        if (SecurityDataPruebas.habilitadas()) {
            return null;
        }
        return cuerpoError(Response.Status.FORBIDDEN.getStatusCode(), "PRUEBAS_DESHABILITADAS",
            "PRUEBAS_DESHABILITADAS: la consola de pruebas de Security Data está apagada (propiedad saa.firma.securitydata.pruebas)");
    }

    private String parteTexto(Map<String, List<InputPart>> form, String nombre) throws Throwable {
        List<InputPart> partes = form.get(nombre);
        return partes == null || partes.isEmpty() ? null : partes.get(0).getBodyAsString();
    }

    private List<Archivo> archivos(Map<String, List<InputPart>> form, String nombre) throws Throwable {
        List<Archivo> salida = new ArrayList<>();
        List<InputPart> partes = form.get(nombre);
        if (partes == null) {
            return salida;
        }
        for (InputPart parte : partes) {
            String cabecera = parte.getHeaders().getFirst("Content-Disposition");
            Matcher m = cabecera == null ? null : NOMBRE_ARCHIVO.matcher(cabecera);
            String archivo = m != null && m.find() && !m.group(1).isEmpty() ? m.group(1) : "documento.pdf";
            try (InputStream in = parte.getBody(InputStream.class, null)) {
                salida.add(new Archivo(archivo, in.readAllBytes()));
            }
        }
        return salida;
    }

    private Response respuestaError(Throwable e) {
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String codigo = null;
        String mensaje = e.getMessage();
        if (e instanceof UsuarioInvalidoException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            mensaje = "No se puede procesar sin un usuario válido: " + ((UsuarioInvalidoException) e).getMotivo();
        } else if (e instanceof ProveedorFirmaException) {
            codigo = ((ProveedorFirmaException) e).getCodigo();
            status = ProveedorFirmaException.PROVEEDOR_NO_CONFIGURADO.equals(codigo)
                ? Response.Status.CONFLICT.getStatusCode() : Response.Status.BAD_GATEWAY.getStatusCode();
        } else if (e instanceof com.fasterxml.jackson.core.JsonProcessingException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            codigo = "PARAMETRO_INVALIDO";
            mensaje = "PARAMETRO_INVALIDO: el JSON recibido no es válido";
        } else if (e instanceof IncomeException && mensaje != null) {
            int i = mensaje.indexOf(':');
            if (i > 0 && mensaje.substring(0, i).matches("[A-Z_]+")) {
                codigo = mensaje.substring(0, i);
                if (codigo.equals("PARAMETRO_INVALIDO")) {
                    status = Response.Status.BAD_REQUEST.getStatusCode();
                } else if (codigo.endsWith("_NO_ENCONTRADO")) {
                    status = Response.Status.NOT_FOUND.getStatusCode();
                } else {
                    status = Response.Status.CONFLICT.getStatusCode();
                }
            }
        }
        return cuerpoError(status, codigo, mensaje);
    }

    private Response cuerpoError(int status, String codigo, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null ? codigo : mensaje);
        return Response.status(status).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
