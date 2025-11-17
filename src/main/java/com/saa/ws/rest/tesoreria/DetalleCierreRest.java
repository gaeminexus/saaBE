package com.saa.ws.rest.tesoreria;

import java.util.List;

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
    @Produces("application/json")
    public List<DetalleCierre> getAll() throws Throwable {
        return detalleCierreDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CIERRE);
    }

    /**
     * Recupera un registro de DetalleCierre por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleCierre getId(@PathParam("id") Long id) throws Throwable {
        return detalleCierreDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_CIERRE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DetalleCierre put(DetalleCierre registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE CIERRE");
        return detalleCierreService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DetalleCierre post(DetalleCierre registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DETALLE CIERRE");
        return detalleCierreService.saveSingle(registro);
    }

    /**
     * Selecciona registros de DetalleCierre por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleCierre> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DETALLE CIERRE: " + test);
        return detalleCierreDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_CIERRE);
    }

    /**
     * Elimina un registro de DetalleCierre por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE CIERRE");
        DetalleCierre elimina = new DetalleCierre();
        detalleCierreDaoService.remove(elimina, id);
    }
}
