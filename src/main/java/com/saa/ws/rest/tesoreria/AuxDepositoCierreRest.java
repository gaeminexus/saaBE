package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
     * POST method for updating or creating an instance of AuxDepositoCierreRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de AUX_DEPOSITO_CIERRE");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(auxDepositoCierreService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
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
