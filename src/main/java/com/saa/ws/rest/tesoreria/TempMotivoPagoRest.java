package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.TempMotivoPagoDaoService;
import com.saa.ejb.tesoreria.service.TempMotivoPagoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempMotivoPago;

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

@Path("tpmt")
public class TempMotivoPagoRest {

    @EJB
    private TempMotivoPagoDaoService tempMotivoPagoDaoService;

    @EJB
    private TempMotivoPagoService tempMotivoPagoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempMotivoPagoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempMotivoPago.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempMotivoPago> getAll() throws Throwable {
        return tempMotivoPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
    }

    /**
     * Recupera un registro de TempMotivoPago por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempMotivoPago getId(@PathParam("id") Long id) throws Throwable {
        return tempMotivoPagoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempMotivoPago put(TempMotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_MOTIVO_PAGO");
        return tempMotivoPagoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempMotivoPago post(TempMotivoPago registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_MOTIVO_PAGO");
        return tempMotivoPagoService.saveSingle(registro);
    }

    /**
     * Selecciona registros de TempMotivoPago por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempMotivoPago> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA TEMP_MOTIVO_PAGO: " + test);
        return tempMotivoPagoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_MOTIVO_PAGO);
    }

    /**
     * Elimina un registro de TempMotivoPago por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_MOTIVO_PAGO");
        TempMotivoPago elimina = new TempMotivoPago();
        tempMotivoPagoDaoService.remove(elimina, id);
    }
}
