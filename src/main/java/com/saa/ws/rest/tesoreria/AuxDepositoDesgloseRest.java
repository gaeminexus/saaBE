package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.AuxDepositoDesgloseDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoDesgloseService;
import com.saa.model.tesoreria.AuxDepositoDesglose;
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

@Path("apds")
public class AuxDepositoDesgloseRest {

    @EJB
    private AuxDepositoDesgloseDaoService auxDepositoDesgloseDaoService;

    @EJB
    private AuxDepositoDesgloseService auxDepositoDesgloseService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AuxDepositoDesgloseRest() {
        // Constructor por defecto
    }

    /**
     * Recupera todos los registros de AuxDepositoDesglose.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AuxDepositoDesglose> getAll() throws Throwable {
        return auxDepositoDesgloseDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
    }

    /**
     * Recupera un registro por ID.
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public AuxDepositoDesglose getId(@PathParam("id") Long id) throws Throwable {
        return auxDepositoDesgloseDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public AuxDepositoDesglose put(AuxDepositoDesglose registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT AUX_DEPOSITO_DESGLOSE");
        return auxDepositoDesgloseService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public AuxDepositoDesglose post(AuxDepositoDesglose registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST AUX_DEPOSITO_DESGLOSE");
        return auxDepositoDesgloseService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterios personalizados.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<AuxDepositoDesglose> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA AUX_DEPOSITO_DESGLOSE: " + test);
        return auxDepositoDesgloseDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_DESGLOSE);
    }

    /**
     * Elimina un registro por ID.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE AUX_DEPOSITO_DESGLOSE");
        AuxDepositoDesglose elimina = new AuxDepositoDesglose();
        auxDepositoDesgloseDaoService.remove(elimina, id);
    }
}
