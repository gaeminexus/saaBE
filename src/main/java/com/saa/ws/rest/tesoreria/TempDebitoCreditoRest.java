package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.TempDebitoCreditoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempDebitoCredito;

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

@Path("tdbc")
public class TempDebitoCreditoRest {

    @EJB
    private TempDebitoCreditoDaoService tempDebitoCreditoDaoService;

    @EJB
    private TempDebitoCreditoService tempDebitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempDebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempDebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempDebitoCredito> getAll() throws Throwable {
        return tempDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de TempDebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempDebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return tempDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempDebitoCredito put(TempDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_DEBITO_CREDITO");
        return tempDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempDebitoCredito post(TempDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_DEBITO_CREDITO");
        return tempDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempDebitoCredito por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempDebitoCredito> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_DEBITO_CREDITO: " + test);
        return tempDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
    }

    /**
     * Elimina un registro de TempDebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_DEBITO_CREDITO");
        TempDebitoCredito elimina = new TempDebitoCredito();
        tempDebitoCreditoDaoService.remove(elimina, id);
    }
}
