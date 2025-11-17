package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DetalleDebitoCreditoService;
import com.saa.model.tesoreria.DetalleDebitoCredito;
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

@Path("dtdc")
public class DetalleDebitoCreditoRest {

    @EJB
    private DetalleDebitoCreditoDaoService detalleDebitoCreditoDaoService;

    @EJB
    private DetalleDebitoCreditoService detalleDebitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleDebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleDebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleDebitoCredito> getAll() throws Throwable {
        return detalleDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de DetalleDebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleDebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return detalleDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DetalleDebitoCredito put(DetalleDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE DEBITO CREDITO");
        return detalleDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DetalleDebitoCredito post(DetalleDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DETALLE DEBITO CREDITO");
        return detalleDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de DetalleDebitoCredito por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleDebitoCredito> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA DETALLE DEBITO CREDITO: " + test);
        return detalleDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
    }

    /**
     * Elimina un registro de DetalleDebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE DEBITO CREDITO");
        DetalleDebitoCredito elimina = new DetalleDebitoCredito();
        detalleDebitoCreditoDaoService.remove(elimina, id);
    }
}
