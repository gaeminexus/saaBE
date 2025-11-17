package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempCobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.TempCobroTarjetaService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroTarjeta;

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

@Path("tctj")
public class TempCobroTarjetaRest {

    @EJB
    private TempCobroTarjetaDaoService tempCobroTarjetaDaoService;

    @EJB
    private TempCobroTarjetaService tempCobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroTarjeta> getAll() throws Throwable {
        return tempCobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
    }

    /**
     * Recupera un registro de TempCobroTarjeta por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroTarjeta getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroTarjeta put(TempCobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_TARJETA");
        return tempCobroTarjetaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroTarjeta post(TempCobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_TARJETA");
        return tempCobroTarjetaService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempCobroTarjeta por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempCobroTarjeta> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_COBRO_TARJETA: " + test);
        return tempCobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_TARJETA);
    }

    /**
     * Elimina un registro de TempCobroTarjeta por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_TARJETA");
        TempCobroTarjeta elimina = new TempCobroTarjeta();
        tempCobroTarjetaDaoService.remove(elimina, id);
    }
}
