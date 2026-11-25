package com.saa.ws.rest.crd;

import java.util.HashMap;
import java.util.Map;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.AjusteTopeProvisionService;
import com.saa.ejb.crd.service.LibroInteresArranqueService;
import com.saa.ejb.crd.service.dto.SolicitudLibroInteresArranque;
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

    @EJB
    private LibroInteresArranqueService libroInteresArranqueService;

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

    /**
     * POST /rest/prov/semillaDevengoInteres — A2 (lote 21): previsualiza (grabar=false) o graba el interés devengado y no cobrado al corte, por cuota,
     * como saldo inicial del libro de devengo (MVIC 11). Sin asiento contable propio; idempotente por idAsientoReferencia.
     */
    @POST
    @Path("/semillaDevengoInteres")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response semillaDevengoInteres(SolicitudLibroInteresArranque solicitud) {
        System.out.println("ProvisionInteresRest.semillaDevengoInteres");
        try {
            return Response.ok(libroInteresArranqueService.sembrarDevengoInteres(solicitud), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al sembrar el libro de devengo de interés: ");
        }
    }

    /** POST /rest/prov/semillaDevengoInteres/anular — anula la semilla de una referencia. */
    @POST
    @Path("/semillaDevengoInteres/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularSemillaDevengoInteres(SolicitudLibroInteresArranque solicitud) {
        System.out.println("ProvisionInteresRest.semillaDevengoInteres/anular");
        try {
            int anulados = libroInteresArranqueService.anularSemilla(
                solicitud != null ? solicitud.getIdAsientoReferencia() : null, solicitud != null ? solicitud.getUsuario() : null);
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("idAsientoReferencia", solicitud != null ? solicitud.getIdAsientoReferencia() : null);
            cuerpo.put("movimientosAnulados", anulados);
            return Response.ok(cuerpo, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al anular la semilla del libro de devengo de interés: ");
        }
    }

    /**
     * POST /rest/prov/ajusteLibroProvision — A1 (lote 21): previsualiza (grabar=false) o graba el ajuste del libro de provisión por cuota (MVIC 13 y 14)
     * que deja el saldo provisionado de cada cuota igual a su interés y mora devengados y no cobrados al corte. Sin asiento contable propio;
     * idempotente por idAsientoReferencia.
     */
    @POST
    @Path("/ajusteLibroProvision")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ajusteLibroProvision(SolicitudLibroInteresArranque solicitud) {
        System.out.println("ProvisionInteresRest.ajusteLibroProvision");
        try {
            return Response.ok(libroInteresArranqueService.ajustarLibroProvision(solicitud), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al ajustar el libro de provisión: ");
        }
    }

    /** POST /rest/prov/ajusteLibroProvision/anular — anula el ajuste del libro de una referencia. */
    @POST
    @Path("/ajusteLibroProvision/anular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularAjusteLibroProvision(SolicitudLibroInteresArranque solicitud) {
        System.out.println("ProvisionInteresRest.ajusteLibroProvision/anular");
        try {
            int anulados = libroInteresArranqueService.anularAjusteLibro(
                solicitud != null ? solicitud.getIdAsientoReferencia() : null, solicitud != null ? solicitud.getUsuario() : null);
            Map<String, Object> cuerpo = new HashMap<>();
            cuerpo.put("idAsientoReferencia", solicitud != null ? solicitud.getIdAsientoReferencia() : null);
            cuerpo.put("movimientosAnulados", anulados);
            return Response.ok(cuerpo, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error(e, "Error al anular el ajuste del libro de provisión: ");
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
