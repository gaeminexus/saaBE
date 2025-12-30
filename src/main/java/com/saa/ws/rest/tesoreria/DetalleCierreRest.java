package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DetalleCierreDaoService;
import com.saa.ejb.tesoreria.service.DetalleCierreService;
import com.saa.model.tesoreria.DetalleCierre;
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

@Path("dtcr")
public class DetalleCierreRest {

    @EJB
    private DetalleCierreDaoService detalleCierreDaoService;

    @EJB
    private DetalleCierreService detalleCierreService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleCierreRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleCierre.
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleCierre> lista = detalleCierreDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CIERRE);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de cierre: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleCierre detalleCierre = detalleCierreDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_CIERRE);
            if (detalleCierre == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("DetalleCierre con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalleCierre).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de cierre: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleCierre registro) {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE CIERRE");
        try {
            DetalleCierre resultado = detalleCierreService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de cierre: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleCierre registro) {
        System.out.println("LLEGA AL SERVICIO POST DETALLE CIERRE");
        try {
            DetalleCierre resultado = detalleCierreService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de cierre: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of DetalleCierreRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_CIERRE");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleCierreService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DetalleCierre por ID.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE CIERRE");
        try {
            DetalleCierre elimina = new DetalleCierre();
            detalleCierreDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de cierre: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
