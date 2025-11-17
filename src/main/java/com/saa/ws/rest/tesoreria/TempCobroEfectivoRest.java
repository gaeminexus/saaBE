package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempCobroEfectivoDaoService;
import com.saa.ejb.tesoreria.service.TempCobroEfectivoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroEfectivo;

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

@Path("tcef")
public class TempCobroEfectivoRest {

    @EJB
    private TempCobroEfectivoDaoService tempCobroEfectivoDaoService;

    @EJB
    private TempCobroEfectivoService tempCobroEfectivoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroEfectivoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroEfectivo.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroEfectivo> getAll() throws Throwable {
        return tempCobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
    }

    /**
     * Recupera un registro de TempCobroEfectivo por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroEfectivo getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroEfectivoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroEfectivo put(TempCobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_EFECTIVO");
        return tempCobroEfectivoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroEfectivo post(TempCobroEfectivo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_EFECTIVO");
        return tempCobroEfectivoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempCobroEfectivo por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempCobroEfectivo> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_COBRO_EFECTIVO: " + test);
        return tempCobroEfectivoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_EFECTIVO);
    }

    /**
     * Elimina un registro de TempCobroEfectivo por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_EFECTIVO");
        TempCobroEfectivo elimina = new TempCobroEfectivo();
        tempCobroEfectivoDaoService.remove(elimina, id);
    }
}
