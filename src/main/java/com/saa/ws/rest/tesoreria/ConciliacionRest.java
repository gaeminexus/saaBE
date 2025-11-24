package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.ConciliacionDaoService;
import com.saa.ejb.tesoreria.service.ConciliacionService;
import com.saa.model.tesoreria.Conciliacion;
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

@Path("cncl")
public class ConciliacionRest {

    @EJB
    private ConciliacionDaoService conciliacionDaoService;

    @EJB
    private ConciliacionService conciliacionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ConciliacionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Conciliacion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Conciliacion> getAll() throws Throwable {
        return conciliacionDaoService.selectAll(NombreEntidadesTesoreria.CONCILIACION);
    }

    /**
     * Recupera un registro de Conciliacion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Conciliacion getId(@PathParam("id") Long id) throws Throwable {
        return conciliacionDaoService.selectById(id, NombreEntidadesTesoreria.CONCILIACION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Conciliacion put(Conciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CONCILIACION");
        return conciliacionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Conciliacion post(Conciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CONCILIACION");
        return conciliacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ConciliacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CONCILIACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(conciliacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Conciliacion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CONCILIACION");
        Conciliacion elimina = new Conciliacion();
        conciliacionDaoService.remove(elimina, id);
    }
}
