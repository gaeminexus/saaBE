package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.CobroEfectivoService;
import com.saa.model.tesoreria.CobroEfectivo;
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

@Path("cefc")
public class CobroEfectivoRest {

    @EJB
    private CobroEfectivoDaoService cobroEfectivoDaoService;

    @EJB
    private CobroEfectivoService cobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroEfectivo> getAll() throws Throwable {
        return cobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.COBRO_EFECTIVO);
    }

    /**
     * Recupera un registro de CobroEfectivo por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroEfectivo getId(@PathParam("id") Long id) throws Throwable {
        return cobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_EFECTIVO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroEfectivo put(CobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO EFECTIVO");
        return cobroEfectivoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroEfectivo post(CobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO EFECTIVO");
        return cobroEfectivoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CobroEfectivo por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CobroEfectivo> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA COBRO EFECTIVO: " + test);
        return cobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.COBRO_EFECTIVO);
    }

    /**
     * Elimina un registro de CobroEfectivo por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO EFECTIVO");
        CobroEfectivo elimina = new CobroEfectivo();
        cobroEfectivoDaoService.remove(elimina, id);
    }
}
