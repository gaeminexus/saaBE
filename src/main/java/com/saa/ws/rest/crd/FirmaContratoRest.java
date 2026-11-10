package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.basico.util.UsuarioInvalidoException;
import com.saa.ejb.crd.dao.FirmaContratoDaoService;
import com.saa.ejb.crd.service.FirmaContratoService;
import com.saa.ejb.crd.service.dto.ArchivoFirma;
import com.saa.ejb.crd.service.dto.ResultadoOperacionFirma;
import com.saa.ejb.crd.service.dto.SolicitudEnvioFirma;
import com.saa.ejb.crd.service.dto.TramiteFirma;
import com.saa.model.crd.FirmaContrato;
import com.saa.model.crd.NombreEntidadesCredito;

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
 * REST de la firma electrónica del contrato de adhesión (CRD.FRCN). Contrato para el FE:
 * docs/logica-negocio/crd/API-FIRMA-ELECTRONICA-CONTRATO.md.
 */
@Path("frcn")
public class FirmaContratoRest {

    @EJB
    private FirmaContratoDaoService firmaContratoDaoService;

    @EJB
    private FirmaContratoService firmaContratoService;

    public FirmaContratoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<FirmaContrato> lista = firmaContratoDaoService.selectAll(NombreEntidadesCredito.FIRMA_CONTRATO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            FirmaContrato firma = firmaContratoDaoService.selectById(id, NombreEntidadesCredito.FIRMA_CONTRATO);
            return Response.status(Response.Status.OK).entity(firma).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** POST /rest/frcn/enviar?idContrato&usuario — cuerpo JSON {"codigoDactilar":"XXXXXX"} (no va en la URL: quedaría en los logs de acceso). */
    @POST
    @Path("/enviar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enviar(@QueryParam("idContrato") Long idContrato, @QueryParam("usuario") String usuario,
            SolicitudEnvioFirma cuerpo) {
        System.out.println("LLEGA AL SERVICIO POST enviar - FRCN - idContrato: " + idContrato);
        try {
            return respuestaOperacion(firmaContratoService.enviarAFirmar(idContrato, usuario, codigoDactilar(cuerpo)));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** POST /rest/frcn/reintentar/{id}?usuario — cuerpo JSON {"codigoDactilar":"XXXXXX"}. */
    @POST
    @Path("/reintentar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reintentar(@PathParam("id") Long id, @QueryParam("usuario") String usuario,
            SolicitudEnvioFirma cuerpo) {
        System.out.println("LLEGA AL SERVICIO POST reintentar - FRCN - id: " + id);
        try {
            return respuestaOperacion(firmaContratoService.reintentarEnvio(id, usuario, codigoDactilar(cuerpo)));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** POST /rest/frcn/actualizar/{id}?usuario */
    @POST
    @Path("/actualizar/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response actualizar(@PathParam("id") Long id, @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST actualizar - FRCN - id: " + id);
        try {
            return respuestaOperacion(firmaContratoService.actualizarEstado(id, usuario));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** POST /rest/frcn/anular/{id}?motivo&usuario */
    @POST
    @Path("/anular/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, @QueryParam("motivo") String motivo,
            @QueryParam("usuario") String usuario) {
        System.out.println("LLEGA AL SERVICIO POST anular - FRCN - id: " + id);
        try {
            return respuestaOperacion(firmaContratoService.anular(id, motivo, usuario));
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** GET /rest/frcn/contrato/{idContrato} — historial de trámites con sus firmantes, el más reciente primero. */
    @GET
    @Path("/contrato/{idContrato}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porContrato(@PathParam("idContrato") Long idContrato) {
        System.out.println("LLEGA AL SERVICIO GET contrato - FRCN - idContrato: " + idContrato);
        try {
            List<TramiteFirma> historial = firmaContratoService.consultarPorContrato(idContrato);
            return Response.status(Response.Status.OK).entity(historial).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** GET /rest/frcn/descargar/{id}?tipo=ORIGINAL|FIRMADO — el PDF. */
    @GET
    @Path("/descargar/{id}")
    @Produces({ "application/pdf", MediaType.APPLICATION_JSON })
    public Response descargar(@PathParam("id") Long id, @QueryParam("tipo") String tipo) {
        System.out.println("LLEGA AL SERVICIO GET descargar - FRCN - id: " + id + " - tipo: " + tipo);
        try {
            ArchivoFirma archivo = firmaContratoService.descargar(id, tipo);
            String nombre = archivo.getNombre() != null ? archivo.getNombre().replace("\"", "") : "contrato.pdf";
            return Response.status(Response.Status.OK).entity(archivo.getContenido()).type("application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"" + nombre + "\"").build();
        } catch (Throwable e) {
            return respuestaError(e);
        }
    }

    /** Éxito 200; falla del proveedor 502 con el mismo cuerpo (el trámite quedó guardado con el error). */
    private Response respuestaOperacion(ResultadoOperacionFirma resultado) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", resultado.isExito());
        cuerpo.put("mensaje", resultado.getMensaje());
        cuerpo.put("error", resultado.getCodigoError());
        cuerpo.put("tramite", resultado.getTramite());
        return Response.status(resultado.isExito() ? Response.Status.OK : Response.Status.BAD_GATEWAY)
                .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }

    /** Validación: el código va al inicio del mensaje de IncomeException («CODIGO: texto»). */
    private static String codigoDactilar(SolicitudEnvioFirma cuerpo) {
        return cuerpo != null ? cuerpo.getCodigoDactilar() : null;
    }

    private Response respuestaError(Throwable e) {
        int status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
        String codigo = null;
        String mensaje = e.getMessage();
        if (e instanceof UsuarioInvalidoException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            mensaje = "No se puede procesar sin un usuario válido: " + ((UsuarioInvalidoException) e).getMotivo();
        } else if (e instanceof IncomeException && mensaje != null) {
            int i = mensaje.indexOf(':');
            if (i > 0 && mensaje.substring(0, i).matches("[A-Z_]+")) {
                codigo = mensaje.substring(0, i);
                if (codigo.equals("PARAMETRO_INVALIDO") || codigo.equals("CODIGO_DACTILAR_INVALIDO")) {
                    status = Response.Status.BAD_REQUEST.getStatusCode();
                } else if (codigo.endsWith("_NO_ENCONTRADO")) {
                    status = Response.Status.NOT_FOUND.getStatusCode();
                } else {
                    status = Response.Status.CONFLICT.getStatusCode();
                }
            }
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("exito", Boolean.FALSE);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", codigo != null ? codigo : mensaje);
        return Response.status(status).entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
