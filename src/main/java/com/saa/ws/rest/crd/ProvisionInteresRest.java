package com.saa.ws.rest.crd;

import java.util.HashMap;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.AjusteTopeProvisionService;
import com.saa.ejb.crd.service.dto.ResultadoAjusteTopeMora;
import com.saa.ejb.crd.service.dto.SolicitudAjusteTopeMora;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Provisión de intereses: herramientas del libro MVIC (lote 7, D-186). No es una tabla: {@code prov} es una ruta de proceso.
 * Contrato: docs/logica-negocio/crd/API-AJUSTE-TOPE-PROVISION-MORA.md.
 *
 * <p>Los errores salen SIEMPRE como JSON {@code {"mensaje": "..."}}: 400 para un error de negocio o un usuario inválido, 500 para
 * cualquier otra falla.</p>
 */
@Path("prov")
public class ProvisionInteresRest {

    @EJB
    private AjusteTopeProvisionService ajusteTopeProvisionService;

    /** POST /rest/prov/ajusteTopeMora — previsualiza (grabar=false) o graba el ajuste tipo 10 contra un asiento del cuadre. */
    @POST
    @Path("/ajusteTopeMora")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ajusteTopeMora(SolicitudAjusteTopeMora solicitud) {
        System.out.println("ProvisionInteresRest.ajusteTopeMora");
        try {
            ResultadoAjusteTopeMora resultado = ajusteTopeProvisionService.ajustar(solicitud);
            return Response.ok(resultado, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al ajustar el tope de la provisión de mora: ");
        }
    }

    /** POST /rest/prov/ajusteTopeMora/anular — anula los ajustes tipo 10 de un asiento (cuando el cuadre anula su asiento). */
    @POST
    @Path("/ajusteTopeMora/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(SolicitudAjusteTopeMora solicitud) {
        System.out.println("ProvisionInteresRest.ajusteTopeMora/anular");
        try {
            int anulados = ajusteTopeProvisionService.anular(
                solicitud != null ? solicitud.getIdAsiento() : null, solicitud != null ? solicitud.getUsuario() : null);
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("idAsiento", solicitud != null ? solicitud.getIdAsiento() : null);
            cuerpo.put("movimientosAnulados", anulados);
            return Response.ok(cuerpo, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al anular el ajuste del tope de la provisión de mora: ");
        }
    }

    private Response error(Throwable e, String prefijo) {
        System.err.println(prefijo + e.getMessage());
        boolean negocio = e instanceof IncomeException || e instanceof com.saa.basico.util.UsuarioInvalidoException;
        Map<String, String> cuerpo = new HashMap<>();
        String texto = e instanceof com.saa.basico.util.UsuarioInvalidoException
            ? "No se puede procesar sin un usuario válido: " + ((com.saa.basico.util.UsuarioInvalidoException) e).getMotivo()
            : e.getMessage();
        cuerpo.put("mensaje", negocio ? texto : prefijo + e.getMessage());
        return Response.status(negocio ? Response.Status.BAD_REQUEST : Response.Status.INTERNAL_SERVER_ERROR)
            .entity(cuerpo).type(MediaType.APPLICATION_JSON).build();
    }
}
