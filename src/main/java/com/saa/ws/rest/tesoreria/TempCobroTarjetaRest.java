package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTarjetaService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.TempCobroTarjeta;

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

@Path("tctj")
public class TempCobroTarjetaRest {

    @EJB
    private TempCobroTarjetaDaoService tempCobroTarjetaDaoService;

    @EJB
    private TempCobroTarjetaService tempCobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempCobroTarjeta> lista = tempCobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros con tarjeta temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TempCobroTarjeta por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempCobroTarjeta tempCobroTarjeta = tempCobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
            if (tempCobroTarjeta == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TempCobroTarjeta con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tempCobroTarjeta).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro con tarjeta temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempCobroTarjeta registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_COBRO_TARJETA");
        try {
            TempCobroTarjeta resultado = tempCobroTarjetaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro con tarjeta temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempCobroTarjeta registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_COBRO_TARJETA");
        try {
            TempCobroTarjeta resultado = tempCobroTarjetaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro con tarjeta temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempCobroTarjetaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_COBRO_TARJETA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempCobroTarjetaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TempCobroTarjeta por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_COBRO_TARJETA");
        try {
            TempCobroTarjeta elimina = new TempCobroTarjeta();
            tempCobroTarjetaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro con tarjeta temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
