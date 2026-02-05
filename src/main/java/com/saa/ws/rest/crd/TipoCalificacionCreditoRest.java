package com.saa.ws.rest.crd;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.crd.dao.TipoCalificacionCreditoDaoService;
import com.saa.ejb.crd.service.TipoCalificacionCreditoService;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.TipoCalificacionCredito;

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

@Path("tpcl")
public class TipoCalificacionCreditoRest {

    @EJB
    private TipoCalificacionCreditoDaoService tipoCalificacionCreditoDaoService;

    @EJB
    private TipoCalificacionCreditoService tipoCalificacionCreditoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TipoCalificacionCreditoRest() {
    }

    /**
     * GET ALL
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TipoCalificacionCredito> lista = tipoCalificacionCreditoDaoService.selectAll(NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipos de calificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TipoCalificacionCredito tipo = tipoCalificacionCreditoDaoService.selectById(id, NombreEntidadesCredito.TIPO_CALIFICACION_CREDITO);
            if (tipo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TipoCalificacionCredito con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tipo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener tipo de calificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TipoCalificacionCredito registro) {
        System.out.println("LLEGA AL SERVICIO PUT TipoCalificacionCredito");
        try {
            TipoCalificacionCredito resultado = tipoCalificacionCreditoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar tipo de calificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TipoCalificacionCredito registro) {
        System.out.println("LLEGA AL SERVICIO POST TipoCalificacionCredito");
        try {
            TipoCalificacionCredito resultado = tipoCalificacionCreditoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear tipo de calificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST - SELECT BY CRITERIA
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TipoCalificacionCredito");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK)
                    .entity(tipoCalificacionCreditoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
        return respuesta;
    }

    /**
     * DELETE
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE TipoCalificacionCredito");
        try {
            TipoCalificacionCredito elimina = new TipoCalificacionCredito();
            tipoCalificacionCreditoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar tipo de calificación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
