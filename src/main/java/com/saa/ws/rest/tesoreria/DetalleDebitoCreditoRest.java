package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DetalleDebitoCreditoDaoService;
import com.saa.ejb.tesoreria.service.DetalleDebitoCreditoService;
import com.saa.model.tesoreria.DetalleDebitoCredito;
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

@Path("dtdc")
public class DetalleDebitoCreditoRest {

    @EJB
    private DetalleDebitoCreditoDaoService detalleDebitoCreditoDaoService;

    @EJB
    private DetalleDebitoCreditoService detalleDebitoCreditoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleDebitoCreditoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleDebitoCredito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleDebitoCredito> getAll() throws Throwable {
        return detalleDebitoCreditoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
    }

    /**
     * Recupera un registro de DetalleDebitoCredito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleDebitoCredito getId(@PathParam("id") Long id) throws Throwable {
        return detalleDebitoCreditoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEBITO_CREDITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DetalleDebitoCredito put(DetalleDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE DEBITO CREDITO");
        return detalleDebitoCreditoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DetalleDebitoCredito post(DetalleDebitoCredito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DETALLE DEBITO CREDITO");
        return detalleDebitoCreditoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleDebitoCreditoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_DEBITO_CREDITO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleDebitoCreditoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DetalleDebitoCredito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE DEBITO CREDITO");
        DetalleDebitoCredito elimina = new DetalleDebitoCredito();
        detalleDebitoCreditoDaoService.remove(elimina, id);
    }
}
