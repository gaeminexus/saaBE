package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.AdjuntoDaoService;
import com.saa.ejb.crd.dao.CuentaBancariaParticipeDaoService;
import com.saa.ejb.crd.service.AdjuntoService;
import com.saa.ejb.crd.service.CuentaBancariaParticipeService;
import com.saa.model.crd.Adjunto;
import com.saa.model.crd.CuentaBancariaParticipe;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.rubros.Estado;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("adjn")
public class AdjuntoRest {

    @EJB
    private AdjuntoDaoService adjuntoDaoService;

    @EJB
    private AdjuntoService adjuntoService;

    @EJB
    private CuentaBancariaParticipeDaoService cuentaBancariaParticipeDaoService;

    @Context
    private UriInfo context;

    public AdjuntoRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Adjunto> lista = adjuntoDaoService.selectAll(NombreEntidadesCredito.ADJUNTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener adjuntos: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Adjunto adjunto = adjuntoDaoService.selectById(id, NombreEntidadesCredito.ADJUNTO);
            if (adjunto == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Adjunto con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(adjunto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener adjunto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(Adjunto registro) {
        System.out.println("LLEGA AL SERVICIO PUT - ADJUNTO");
        try {
            Adjunto resultado = adjuntoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar adjunto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(Adjunto registro) {
        System.out.println("LLEGA AL SERVICIO POST - ADJUNTO");
        try {
            Adjunto resultado = adjuntoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear adjunto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de ADJUNTO");
        Response respuesta = null;

        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(adjuntoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }

        return respuesta;
    }

    /**
     * ⚠️ Guarda de negocio: si el adjunto es un CERTIFICADO BANCARIO ACTIVO todavía referenciado
     * por una CuentaBancariaParticipe ACTIVA, se rechaza. Sin esto, este DELETE genérico sería
     * exactamente el mismo tipo de bypass que se cerró en
     * {@code CuentaBancariaParticipeRest.post}: la regla "toda cuenta bancaria de partícipe tiene
     * su certificado" se puede saltear borrando el adjunto en vez de la cuenta.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - ADJUNTO");
        try {
            Adjunto existente = adjuntoDaoService.find(new Adjunto(), id);
            if (existente != null
                    && existente.getEstado() != null && existente.getEstado() == Estado.ACTIVO
                    && existente.getTipoAdjunto() != null
                    && CuentaBancariaParticipeService.CERTIFICADO_BANCARIO
                        .equalsIgnoreCase(existente.getTipoAdjunto().getNombre())
                    && existente.getIdReferencia() != null) {

                CuentaBancariaParticipe cuenta = cuentaBancariaParticipeDaoService.find(
                    new CuentaBancariaParticipe(), existente.getIdReferencia());
                if (cuenta != null && cuenta.getEstado() != null && cuenta.getEstado() == Estado.ACTIVO) {
                    return Response.status(409)
                            .entity("No se puede eliminar el certificado bancario de la cuenta " + cuenta.getCodigo()
                                + " porque sigue activa. Anule o elimine la cuenta bancaria primero.")
                            .type(MediaType.APPLICATION_JSON).build();
                }
            }

            Adjunto elimina = new Adjunto();
            adjuntoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar adjunto: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
