package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.CobroTransferenciaService;
import com.saa.model.tsr.CobroTransferencia;
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

@Path("ctrn")
public class CobroTransferenciaRest {

    @EJB
    private CobroTransferenciaDaoService cobroTransferenciaDaoService;

    @EJB
    private CobroTransferenciaService cobroTransferenciaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroTransferenciaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroTransferencia.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<CobroTransferencia> lista = cobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TRANSFERENCIA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobros por transferencia: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de CobroTransferencia por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            CobroTransferencia cobroTransferencia = cobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TRANSFERENCIA);
            if (cobroTransferencia == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("CobroTransferencia con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(cobroTransferencia).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener cobro por transferencia: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(CobroTransferencia registro) {
        System.out.println("LLEGA AL SERVICIO PUT - COBRO TRANSFERENCIA");
        try {
            CobroTransferencia resultado = cobroTransferenciaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar cobro por transferencia: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(CobroTransferencia registro) {
        System.out.println("LLEGA AL SERVICIO POST - COBRO TRANSFERENCIA");
        try {
            CobroTransferencia resultado = cobroTransferenciaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear cobro por transferencia: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of CobroTransferenciaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de COBRO_TRANSFERENCIA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(cobroTransferenciaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
    /**
     * Elimina un registro de CobroTransferencia por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - COBRO TRANSFERENCIA");
        try {
            CobroTransferencia elimina = new CobroTransferencia();
            cobroTransferenciaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar cobro por transferencia: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
