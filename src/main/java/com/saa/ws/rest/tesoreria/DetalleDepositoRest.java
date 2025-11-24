package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.DetalleDepositoDaoService;
import com.saa.ejb.tesoreria.service.DetalleDepositoService;
import com.saa.model.tesoreria.DetalleDeposito;
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

@Path("dtdp")
public class DetalleDepositoRest {

    @EJB
    private DetalleDepositoDaoService detalleDepositoDaoService;

    @EJB
    private DetalleDepositoService detalleDepositoService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public DetalleDepositoRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de DetalleDeposito.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleDeposito> getAll() throws Throwable {
        return detalleDepositoDaoService.selectAll(NombreEntidadesTesoreria.DETALLE_DEPOSITO);
    }

    /**
     * Recupera un registro de DetalleDeposito por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public DetalleDeposito getId(@PathParam("id") Long id) throws Throwable {
        return detalleDepositoDaoService.selectById(id, NombreEntidadesTesoreria.DETALLE_DEPOSITO);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public DetalleDeposito put(DetalleDeposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT DETALLE DEPOSITO");
        return detalleDepositoService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public DetalleDeposito post(DetalleDeposito registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST DETALLE DEPOSITO");
        return detalleDepositoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleDepositoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_DEPOSITO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleDepositoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de DetalleDeposito por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE DETALLE DEPOSITO");
        DetalleDeposito elimina = new DetalleDeposito();
        detalleDepositoDaoService.remove(elimina, id);
    }
}
