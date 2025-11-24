package com.saa.ws.rest.tesoreria;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tesoreria.dao.ChequeraDaoService;
import com.saa.ejb.tesoreria.service.ChequeraService;
import com.saa.model.tesoreria.Chequera;
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

@Path("chqr")
public class ChequeraRest {

    @EJB
    private ChequeraDaoService chequeraDaoService;

    @EJB
    private ChequeraService chequeraService;

    @Context
    private UriInfo context;

    /**
     * Constructor por defecto.
     */
    public ChequeraRest() {
        // Constructor vacío
    }

    /**
     * Recupera todos los registros de Chequera.
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Chequera> getAll() throws Throwable {
        return chequeraDaoService.selectAll(NombreEntidadesTesoreria.CHEQUERA);
    }

    /**
     * Recupera un registro de Chequera por ID.
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Chequera getId(@PathParam("id") Long id) throws Throwable {
        return chequeraDaoService.selectById(id, NombreEntidadesTesoreria.CHEQUERA);
    }

    /**
     * Guarda o actualiza un registro (PUT).
     */
    @PUT
    @Consumes("application/json")
    public Chequera put(Chequera registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT CHEQUERA");
        return chequeraService.saveSingle(registro);
    }

    /**
     * Guarda o actualiza un registro (POST).
     */
    @POST
    @Consumes("application/json")
    public Chequera post(Chequera registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO POST CHEQUERA");
        return chequeraService.saveSingle(registro);
    }
    
    /**
     * POST method for updating or creating an instance of ChequeraRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de CHEQUERA");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(chequeraService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * Elimina un registro de Chequera por ID.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE CHEQUERA");
        Chequera elimina = new Chequera();
        chequeraDaoService.remove(elimina, id);
    }
}
