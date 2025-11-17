package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.ejb.tesoreria.dao.AuxDepositoCierreDaoService;
import com.saa.ejb.tesoreria.service.AuxDepositoCierreService;
import com.saa.model.tesoreria.AuxDepositoCierre;
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

@Path("acpd")
public class AuxDepositoCierreRest {

    @EJB
    private AuxDepositoCierreDaoService auxDepositoCierreDaoService;

    @EJB
    private AuxDepositoCierreService auxDepositoCierreService;

    @Context
    private UriInfo context;

    public AuxDepositoCierreRest() {
    }

    /**
     * Obtiene todos los registros de AuxDepositoCierre.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<AuxDepositoCierre> getAll() throws Throwable {
        return auxDepositoCierreDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
    }

    /**
     * Obtiene un registro por ID.
     */
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public AuxDepositoCierre getId(@PathParam("id") Long id) throws Throwable {
        return auxDepositoCierreDaoService.selectById(id, NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
    }

    /**
     * Actualiza o crea un registro.
     */
    @PUT
    @Consumes("application/json")
    public AuxDepositoCierre put(AuxDepositoCierre registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return auxDepositoCierreService.saveSingle(registro);
    }

    /**
     * Crea un nuevo registro.
     */
    @POST
    @Consumes("application/json")
    public AuxDepositoCierre post(AuxDepositoCierre registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST");
        return auxDepositoCierreService.saveSingle(registro);
    }

    /**
     * Selecciona registros por criterio.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<AuxDepositoCierre> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return auxDepositoCierreDaoService.selectAll(NombreEntidadesTesoreria.AUX_DEPOSITO_CIERRE);
    }

    /**
     * Elimina un registro.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        AuxDepositoCierre elimina = new AuxDepositoCierre();
        auxDepositoCierreDaoService.remove(elimina, id);
    }

}
