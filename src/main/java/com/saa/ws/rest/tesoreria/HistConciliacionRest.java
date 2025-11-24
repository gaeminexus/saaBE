package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.HistConciliacionDaoService;
import com.saa.ejb.tesoreria.service.HistConciliacionService;
import com.saa.model.tesoreria.HistConciliacion;
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

@Path("cnch")
public class HistConciliacionRest {

    @EJB
    private HistConciliacionDaoService histConciliacionDaoService;

    @EJB
    private HistConciliacionService histConciliacionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public HistConciliacionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de HistConciliacion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistConciliacion> getAll() throws Throwable {
        return histConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_CONCILIACION);
    }

    /**
     * Recupera un registro de HistConciliacion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public HistConciliacion getId(@PathParam("id") Long id) throws Throwable {
        return histConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.HIST_CONCILIACION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public HistConciliacion put(HistConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT HIST CONCILIACION");
        return histConciliacionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public HistConciliacion post(HistConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST HIST CONCILIACION");
        return histConciliacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistConciliacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de HIST_CONCILIACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(histConciliacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de HistConciliacion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE HIST CONCILIACION");
        HistConciliacion elimina = new HistConciliacion();
        histConciliacionDaoService.remove(elimina, id);
    }
}
