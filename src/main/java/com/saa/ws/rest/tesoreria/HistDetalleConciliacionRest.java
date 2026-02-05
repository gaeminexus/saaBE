package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.HistDetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.service.HistDetalleConciliacionService;
import com.saa.model.tsr.HistDetalleConciliacion;
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

@Path("dchi")
public class HistDetalleConciliacionRest {

    @EJB
    private HistDetalleConciliacionDaoService histDetalleConciliacionDaoService;

    @EJB
    private HistDetalleConciliacionService histDetalleConciliacionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public HistDetalleConciliacionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de HistDetalleConciliacion.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistDetalleConciliacion> lista = histDetalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener histórico de detalles de conciliación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera un registro de HistDetalleConciliacion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            HistDetalleConciliacion histDetalleConciliacion = histDetalleConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
            if (histDetalleConciliacion == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("HistDetalleConciliacion con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(histDetalleConciliacion).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener histórico de detalle de conciliación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(HistDetalleConciliacion registro) {
        System.out.println("LLEGA AL SERVICIO PUT HIST DETALLE CONCILIACION");
        try {
            HistDetalleConciliacion resultado = histDetalleConciliacionService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar histórico de detalle de conciliación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(HistDetalleConciliacion registro) {
        System.out.println("LLEGA AL SERVICIO POST HIST DETALLE CONCILIACION");
        try {
            HistDetalleConciliacion resultado = histDetalleConciliacionService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear histórico de detalle de conciliación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of HistDetalleConciliacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de HIST_DETALLE_CONCILIACION");
        try {
            return Response.status(Response.Status.OK)
                    .entity(histDetalleConciliacionService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina un registro de HistDetalleConciliacion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE HIST DETALLE CONCILIACION");
        try {
            HistDetalleConciliacion elimina = new HistDetalleConciliacion();
            histDetalleConciliacionDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar histórico de detalle de conciliación: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
