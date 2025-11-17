package com.saa.ws.rest.tesoreria;

import java.util.List;

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
     * Selecciona registros de HistConciliacion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<HistConciliacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA HIST CONCILIACION: " + test);
        return histConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_CONCILIACION);
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
