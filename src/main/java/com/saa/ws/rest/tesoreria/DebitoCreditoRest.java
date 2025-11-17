package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.DebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DebitoCreditoService;
import com.saa.model.tesoreria.DebitoCredito;
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

@Path("dbcr")
public class DebitoCreditoRest {

    @EJB
    private DebitoCreditoDaoService debitoCreditoDaoService;

    @EJB
    private DebitoCreditoService debitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DebitoCredito> getAll() throws Throwable {
        return debitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de DebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return debitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DebitoCredito put(DebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DEBITO CREDITO");
        return debitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DebitoCredito post(DebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DEBITO CREDITO");
        return debitoCreditoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de DebitoCredito por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DebitoCredito> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DEBITO CREDITO: " + test);
        return debitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DEBITO_CREDITO);
    }

    /**
     * Elimina un registro de DebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DEBITO CREDITO");
        DebitoCredito elimina = new DebitoCredito();
        debitoCreditoDaoService.remove(elimina, id);
    }
}
