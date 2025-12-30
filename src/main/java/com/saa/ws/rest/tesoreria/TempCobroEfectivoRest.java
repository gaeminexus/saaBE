package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.TempCobroEfectivoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroEfectivo;

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

@Path("tcef")
public class TempCobroEfectivoRest {

    @EJB
    private TempCobroEfectivoDaoService tempCobroEfectivoDaoService;

    @EJB
    private TempCobroEfectivoService tempCobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempCobroEfectivo> lista = tempCobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros en efectivo temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TempCobroEfectivo por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempCobroEfectivo tempCobroEfectivo = tempCobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
            if (tempCobroEfectivo == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TempCobroEfectivo con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tempCobroEfectivo).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro en efectivo temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempCobroEfectivo registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_COBRO_EFECTIVO");
        try {
            TempCobroEfectivo resultado = tempCobroEfectivoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro en efectivo temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempCobroEfectivo registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_COBRO_EFECTIVO");
        try {
            TempCobroEfectivo resultado = tempCobroEfectivoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro en efectivo temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempCobroEfectivoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_COBRO_EFECTIVO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempCobroEfectivoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TempCobroEfectivo por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_COBRO_EFECTIVO");
        try {
            TempCobroEfectivo elimina = new TempCobroEfectivo();
            tempCobroEfectivoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro en efectivo temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
