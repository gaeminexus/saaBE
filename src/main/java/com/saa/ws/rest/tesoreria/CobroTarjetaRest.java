package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.CobroTarjetaDaoService;
import com.saa.ejb.tesoreria.service.CobroTarjetaService;
import com.saa.model.tesoreria.CobroTarjeta;
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

@Path("ctrj")
public class CobroTarjetaRest {

    @EJB
    private CobroTarjetaDaoService cobroTarjetaDaoService;

    @EJB
    private CobroTarjetaService cobroTarjetaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public CobroTarjetaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de CobroTarjeta.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<CobroTarjeta> getAll() throws Throwable {
        return cobroTarjetaDaoService.selectAll(NombreEntidadesTesoreria.COBRO_TARJETA);
    }

    /**
     * Recupera un registro de CobroTarjeta por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public CobroTarjeta getId(@PathParam("id") Long id) throws Throwable {
        return cobroTarjetaDaoService.selectById(id, NombreEntidadesTesoreria.COBRO_TARJETA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public CobroTarjeta put(CobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT COBRO TARJETA");
        return cobroTarjetaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public CobroTarjeta post(CobroTarjeta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST COBRO TARJETA");
        return cobroTarjetaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CobroTarjetaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de COBRO_TARJETA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cobroTarjetaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de CobroTarjeta por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE COBRO TARJETA");
        CobroTarjeta elimina = new CobroTarjeta();
        cobroTarjetaDaoService.remove(elimina, id);
    }
}
