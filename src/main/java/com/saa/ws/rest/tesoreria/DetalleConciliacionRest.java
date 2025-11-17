package com.saa.ws.rest.tesoreria;

import java.util.List;

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
     * Selecciona registros de DetalleConciliacion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleConciliacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DETALLE CONCILIACION: " + test);
        return detalleConciliacionDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CONCILIACION);
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
