package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.CobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.CobroTarjetaService;
import com.saa.model.tesoreria.CobroTarjeta;
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

@Path("ctrj")
public class CobroTarjetaRest {

    @EJB
    private CobroTarjetaDaoService cobroTarjetaDaoService;

    @EJB
    private CobroTarjetaService cobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroTarjeta> getAll() throws Throwable {
        return cobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TARJETA);
    }

    /**
     * Recupera un registro de CobroTarjeta por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroTarjeta getId(@PathParam("id") Long id) throws Throwable {
        return cobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TARJETA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroTarjeta put(CobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO TARJETA");
        return cobroTarjetaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroTarjeta post(CobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO TARJETA");
        return cobroTarjetaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de CobroTarjeta por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<CobroTarjeta> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA COBRO TARJETA: " + test);
        return cobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TARJETA);
    }

    /**
     * Elimina un registro de CobroTarjeta por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO TARJETA");
        CobroTarjeta elimina = new CobroTarjeta();
        cobroTarjetaDaoService.remove(elimina, id);
    }
}
