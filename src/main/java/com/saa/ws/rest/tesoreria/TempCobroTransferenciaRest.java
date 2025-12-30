package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTransferenciaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTransferencia;

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

@Path("tctr")
public class TempCobroTransferenciaRest {

    @EJB
    private TempCobroTransferenciaDaoService tempCobroTransferenciaDaoService;

    @EJB
    private TempCobroTransferenciaService tempCobroTransferenciaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroTransferenciaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroTransferencia.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempCobroTransferencia> lista = tempCobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener transferencias de cobro temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TempCobroTransferencia por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempCobroTransferencia tempCobroTransferencia = tempCobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
            if (tempCobroTransferencia == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TempCobroTransferencia con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tempCobroTransferencia).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener transferencia de cobro temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempCobroTransferencia registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_COBRO_TRANSFERENCIA");
        try {
            TempCobroTransferencia resultado = tempCobroTransferenciaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar transferencia de cobro temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempCobroTransferencia registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_COBRO_TRANSFERENCIA");
        try {
            TempCobroTransferencia resultado = tempCobroTransferenciaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear transferencia de cobro temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempCobroTransferenciaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_COBRO_TRANSFERENCIA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempCobroTransferenciaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
    /**
     * Elimina un registro de TempCobroTransferencia por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_COBRO_TRANSFERENCIA");
        try {
            TempCobroTransferencia elimina = new TempCobroTransferencia();
            tempCobroTransferenciaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar transferencia de cobro temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
