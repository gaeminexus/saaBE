package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.CobroEfectivoService;
import com.saa.model.tesoreria.CobroEfectivo;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;

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

@Path("cefc")
public class CobroEfectivoRest {

    @EJB
    private CobroEfectivoDaoService cobroEfectivoDaoService;

    @EJB
    private CobroEfectivoService cobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CobroEfectivo> lista = cobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.COBRO_EFECTIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros en efectivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CobroEfectivo cobroEfectivo = cobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_EFECTIVO);
            if (cobroEfectivo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CobroEfectivo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cobroEfectivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro en efectivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CobroEfectivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT COBRO EFECTIVO");
        try {
            CobroEfectivo resultado = cobroEfectivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro en efectivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CobroEfectivo registro) {
        System.out.println("LLEGA AL SERVICIO POST COBRO EFECTIVO");
        try {
            CobroEfectivo resultado = cobroEfectivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro en efectivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of CobroEfectivoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COBRO_EFECTIVO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cobroEfectivoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO EFECTIVO");
        try {
            CobroEfectivo elimina = new CobroEfectivo();
            cobroEfectivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro en efectivo: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
