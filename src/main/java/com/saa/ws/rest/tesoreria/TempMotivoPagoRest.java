package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempMotivoPagoDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoPagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoPago;

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

@Path("tpmt")
public class TempMotivoPagoRest {

    @EJB
    private TempMotivoPagoDaoService tempMotivoPagoDaoService;

    @EJB
    private TempMotivoPagoService tempMotivoPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempMotivoPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempMotivoPago.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<TempMotivoPago> lista = tempMotivoPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener motivos de pago temporales: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de TempMotivoPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            TempMotivoPago tempMotivoPago = tempMotivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
            if (tempMotivoPago == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("TempMotivoPago con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(tempMotivoPago).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener motivo de pago temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(TempMotivoPago registro) {
        System.out.println("LLEGA AL SERVICIO PUT - TEMP_MOTIVO_PAGO");
        try {
            TempMotivoPago resultado = tempMotivoPagoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar motivo de pago temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(TempMotivoPago registro) {
        System.out.println("LLEGA AL SERVICIO POST - TEMP_MOTIVO_PAGO");
        try {
            TempMotivoPago resultado = tempMotivoPagoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear motivo de pago temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of TempMotivoPagoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de TEMP_MOTIVO_PAGO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(tempMotivoPagoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de TempMotivoPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - TEMP_MOTIVO_PAGO");
        try {
            TempMotivoPago elimina = new TempMotivoPago();
            tempMotivoPagoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar motivo de pago temporal: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
