package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.HistDetalleConciliacionDaoService;
import com.saa.ejb.tesoreria.service.HistDetalleConciliacionService;
import com.saa.model.tesoreria.HistDetalleConciliacion;
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
    @Produces("application/json")
    public List<HistDetalleConciliacion> getAll() throws Throwable {
        return histDetalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
    }

    /**
     * Recupera un registro de HistDetalleConciliacion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public HistDetalleConciliacion getId(@PathParam("id") Long id) throws Throwable {
        return histDetalleConciliacionDaoService.selectById(id, NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public HistDetalleConciliacion put(HistDetalleConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT HIST DETALLE CONCILIACION");
        return histDetalleConciliacionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public HistDetalleConciliacion post(HistDetalleConciliacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST HIST DETALLE CONCILIACION");
        return histDetalleConciliacionService.saveSingle(registro);
    }

    /**
     * Selecciona registros de HistDetalleConciliacion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<HistDetalleConciliacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA HIST DETALLE CONCILIACION: " + test);
        return histDetalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.HIST_DETALLE_CONCILIACION);
    }

    /**
     * Elimina un registro de HistDetalleConciliacion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE HIST DETALLE CONCILIACION");
        HistDetalleConciliacion elimina = new HistDetalleConciliacion();
        histDetalleConciliacionDaoService.remove(elimina, id);
    }
}
