package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.GrupoCajaDaoService;
import com.saa.ejb.tesoreria.service.GrupoCajaService;
import com.saa.model.tesoreria.GrupoCaja;
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

@Path("cjin")
public class GrupoCajaRest {

    @EJB
    private GrupoCajaDaoService grupoCajaDaoService;

    @EJB
    private GrupoCajaService grupoCajaService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public GrupoCajaRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de GrupoCaja.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<GrupoCaja> getAll() throws Throwable {
        return grupoCajaDaoService.selectAll(NombreEntidadesTesoreria.GRUPO_CAJA);
    }

    /**
     * Recupera un registro de GrupoCaja por su ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public GrupoCaja getId(@PathParam("id") Long id) throws Throwable {
        return grupoCajaDaoService.selectById(id, NombreEntidadesTesoreria.GRUPO_CAJA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public GrupoCaja put(GrupoCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT GRUPO CAJA");
        return grupoCajaService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public GrupoCaja post(GrupoCaja registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST GRUPO CAJA");
        return grupoCajaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of GrupoCajaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de GRUPO_CAJA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(grupoCajaService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de GrupoCaja por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE GRUPO CAJA");
        GrupoCaja elimina = new GrupoCaja();
        grupoCajaDaoService.remove(elimina, id);
    }
}
