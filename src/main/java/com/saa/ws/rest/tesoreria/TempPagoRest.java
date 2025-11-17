package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempPagoDaoService;
import com.saa.ejb.tesoreria.service.TempPagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempPago;

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

@Path("tpgs")
public class TempPagoRest {

    @EJB
    private TempPagoDaoService tempPagoDaoService;

    @EJB
    private TempPagoService tempPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempPago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempPago> getAll() throws Throwable {
        return tempPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_PAGO);
    }

    /**
     * Recupera un registro de TempPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempPago getId(@PathParam("id") Long id) throws Throwable {
        return tempPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempPago put(TempPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_PAGO");
        return tempPagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempPago post(TempPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_PAGO");
        return tempPagoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempPago por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempPago> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_PAGO: " + test);
        return tempPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_PAGO);
    }

    /**
     * Elimina un registro de TempPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_PAGO");
        TempPago elimina = new TempPago();
        tempPagoDaoService.remove(elimina, id);
    }
}
