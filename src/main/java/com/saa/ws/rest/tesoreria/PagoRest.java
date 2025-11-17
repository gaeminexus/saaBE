package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.PagoDaoService;
import com.saa.ejb.tesoreria.service.PagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.Pago;

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

@Path("pgss")
public class PagoRest {

    @EJB
    private PagoDaoService pagoDaoService;

    @EJB
    private PagoService pagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public PagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Pago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Pago> getAll() throws Throwable {
        return pagoDaoService.selectAll(NombreEntidadesTesoreria.PAGO);
    }

    /**
     * Recupera un registro de Pago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Pago getId(@PathParam("id") Long id) throws Throwable {
        return pagoDaoService.selectById(id, NombreEntidadesTesoreria.PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Pago put(Pago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT PAGO");
        return pagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Pago post(Pago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST PAGO");
        return pagoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de Pago por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Pago> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA PAGO: " + test);
        return pagoDaoService.selectAll(NombreEntidadesTesoreria.PAGO);
    }

    /**
     * Elimina un registro de Pago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE PAGO");
        Pago elimina = new Pago();
        pagoDaoService.remove(elimina, id);
    }
}
