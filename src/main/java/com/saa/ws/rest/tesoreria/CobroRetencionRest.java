package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CobroRetencionDaoService;
import com.saa.ejb.tesoreria.service.CobroRetencionService;
import com.saa.model.tesoreria.CobroRetencion;
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

@Path("crtn")
public class CobroRetencionRest {

    @EJB
    private CobroRetencionDaoService cobroRetencionDaoService;

    @EJB
    private CobroRetencionService cobroRetencionService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroRetencionRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroRetencion.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroRetencion> getAll() throws Throwable {
        return cobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.COBRO_RETENCION);
    }

    /**
     * Recupera un registro de CobroRetencion por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroRetencion getId(@PathParam("id") Long id) throws Throwable {
        return cobroRetencionDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_RETENCION);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroRetencion put(CobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO RETENCION");
        return cobroRetencionService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroRetencion post(CobroRetencion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO RETENCION");
        return cobroRetencionService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CobroRetencion por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CobroRetencion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA COBRO RETENCION: " + test);
        return cobroRetencionDaoService.selectAll(NombreEntidadesTesoreria.COBRO_RETENCION);
    }

    /**
     * Elimina un registro de CobroRetencion por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO RETENCION");
        CobroRetencion elimina = new CobroRetencion();
        cobroRetencionDaoService.remove(elimina, id);
    }
}
