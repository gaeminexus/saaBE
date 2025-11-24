package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.TempDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.TempDebitoCreditoService;
import com.saa.model.tesoreria.NombreEntidadesTesoreria;
import com.saa.model.tesoreria.TempDebitoCredito;

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

@Path("tdbc")
public class TempDebitoCreditoRest {

    @EJB
    private TempDebitoCreditoDaoService tempDebitoCreditoDaoService;

    @EJB
    private TempDebitoCreditoService tempDebitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public TempDebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de TempDebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempDebitoCredito> getAll() throws Throwable {
        return tempDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de TempDebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempDebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return tempDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.TEMP_DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public TempDebitoCredito put(TempDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT TEMP_DEBITO_CREDITO");
        return tempDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public TempDebitoCredito post(TempDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST TEMP_DEBITO_CREDITO");
        return tempDebitoCreditoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempDebitoCreditoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TEMP_DEBITO_CREDITO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempDebitoCreditoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * Elimina un registro de TempDebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE TEMP_DEBITO_CREDITO");
        TempDebitoCredito elimina = new TempDebitoCredito();
        tempDebitoCreditoDaoService.remove(elimina, id);
    }
}
