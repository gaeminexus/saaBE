package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempCobroTransferenciaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTransferenciaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTransferencia;

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

@Path("tctr")
public class TempCobroTransferenciaRest {

    @EJB
    private TempCobroTransferenciaDaoService tempCobroTransferenciaDaoService;

    @EJB
    private TempCobroTransferenciaService tempCobroTransferenciaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroTransferenciaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroTransferencia.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroTransferencia> getAll() throws Throwable {
        return tempCobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
    }

    /**
     * Recupera un registro de TempCobroTransferencia por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroTransferencia getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroTransferenciaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroTransferencia put(TempCobroTransferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_TRANSFERENCIA");
        return tempCobroTransferenciaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroTransferencia post(TempCobroTransferencia registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_TRANSFERENCIA");
        return tempCobroTransferenciaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempCobroTransferencia por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempCobroTransferencia> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_COBRO_TRANSFERENCIA: " + test);
        return tempCobroTransferenciaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TRANSFERENCIA);
    }

    /**
     * Elimina un registro de TempCobroTransferencia por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_TRANSFERENCIA");
        TempCobroTransferencia elimina = new TempCobroTransferencia();
        tempCobroTransferenciaDaoService.remove(elimina, id);
    }
}
