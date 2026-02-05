package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.CobroTarjetaService;
import com.saa.model.tsr.CobroTarjeta;
import com.saa.model.tsr.NombreEntidadesTesoreria;

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

@Path("ctrj")
public class CobroTarjetaRest {

    @EJB
    private CobroTarjetaDaoService cobroTarjetaDaoService;

    @EJB
    private CobroTarjetaService cobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CobroTarjeta> lista = cobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TARJETA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros con tarjeta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CobroTarjeta cobroTarjeta = cobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TARJETA);
            if (cobroTarjeta == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CobroTarjeta con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cobroTarjeta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro con tarjeta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CobroTarjeta registro) {
        System.out.println("LLEGA AL SERVICIO PUT COBRO TARJETA");
        try {
            CobroTarjeta resultado = cobroTarjetaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro con tarjeta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CobroTarjeta registro) {
        System.out.println("LLEGA AL SERVICIO POST COBRO TARJETA");
        try {
            CobroTarjeta resultado = cobroTarjetaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro con tarjeta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of CobroTarjetaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de COBRO_TARJETA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(cobroTarjetaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO TARJETA");
        try {
            CobroTarjeta elimina = new CobroTarjeta();
            cobroTarjetaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro con tarjeta: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
