package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempCobroChequeDaoService;
import com.saa.ejb.tesoreria.service.TempCobroChequeService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempCobroCheque;

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

@Path("tcch")
public class TempCobroChequeRest {

    @EJB
    private TempCobroChequeDaoService tempCobroChequeDaoService;

    @EJB
    private TempCobroChequeService tempCobroChequeService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempCobroChequeRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempCobroCheque.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempCobroCheque> getAll() throws Throwable {
        return tempCobroChequeDaoService.selectAll(NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
    }

    /**
     * Recupera un registro de TempCobroCheque por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempCobroCheque getId(@PathParam("id") Long id) throws Throwable {
        return tempCobroChequeDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_COBRO_CHEQUE);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempCobroCheque put(TempCobroCheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_COBRO_CHEQUE");
        return tempCobroChequeService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempCobroCheque post(TempCobroCheque registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_COBRO_CHEQUE");
        return tempCobroChequeService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempCobroChequeRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_COBRO_CHEQUE");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempCobroChequeService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de TempCobroCheque por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_COBRO_CHEQUE");
        TempCobroCheque elimina = new TempCobroCheque();
        tempCobroChequeDaoService.remove(elimina, id);
    }
}
