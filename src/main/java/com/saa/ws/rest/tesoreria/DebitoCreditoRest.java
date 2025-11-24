package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DebitoCreditoService;
import com.saa.model.tesoreria.DebitoCredito;
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

@Path("dbcr")
public class DebitoCreditoRest {

    @EJB
    private DebitoCreditoDaoService debitoCreditoDaoService;

    @EJB
    private DebitoCreditoService debitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DebitoCredito> getAll() throws Throwable {
        return debitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de DebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return debitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DebitoCredito put(DebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DEBITO CREDITO");
        return debitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DebitoCredito post(DebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DEBITO CREDITO");
        return debitoCreditoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DebitoCreditoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DEBITO_CREDITO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(debitoCreditoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DEBITO CREDITO");
        DebitoCredito elimina = new DebitoCredito();
        debitoCreditoDaoService.remove(elimina, id);
    }
}
