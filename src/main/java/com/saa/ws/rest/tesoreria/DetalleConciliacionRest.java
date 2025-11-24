package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.service.DetalleConciliacionService;
import com.saa.model.tesoreria.DetalleConciliacion;
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

@Path("dtcl")
public class DetalleConciliacionRest {

    @EJB
    private DetalleConciliacionDaoService detalleConciliacionDaoService;

    @EJB
    private DetalleConciliacionService detalleConciliacionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleConciliacionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleConciliacion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleConciliacion> getAll() throws Throwable {
        return detalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CONCILIACION);
    }

    /**
     * Recupera un registro de DetalleConciliacion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleConciliacion getId(@PathParam("id") Long id) throws Throwable {
        return detalleConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_CONCILIACION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DetalleConciliacion put(DetalleConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE CONCILIACION");
        return detalleConciliacionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DetalleConciliacion post(DetalleConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DETALLE CONCILIACION");
        return detalleConciliacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleConciliacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_CONCILIACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleConciliacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DetalleConciliacion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE CONCILIACION");
        DetalleConciliacion elimina = new DetalleConciliacion();
        detalleConciliacionDaoService.remove(elimina, id);
    }
}
