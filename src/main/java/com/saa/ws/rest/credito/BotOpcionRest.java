package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.BotOpcionDaoService;
import com.saa.ejb.credito.service.BotOpcionService;
import com.saa.model.credito.BotOpcion;
import com.saa.model.credito.NombreEntidadesCredito;

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

@Path("btpc")
public class BotOpcionRest {

    @EJB
    private BotOpcionDaoService botOpcionDaoService;

    @EJB
    private BotOpcionService botOpcionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public BotOpcionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of BotOpcionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<BotOpcion> getAll() throws Throwable {
        return botOpcionDaoService.selectAll(NombreEntidadesCredito.BOT_OPCION);
    }

    /**
     * Retrieves representation of an instance of BotOpcionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public BotOpcion getId(@PathParam("id") Long id) throws Throwable {
        return botOpcionDaoService.selectById(id, NombreEntidadesCredito.BOT_OPCION);
    }

    /**
     * PUT method for updating or creating an instance of BotOpcionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public BotOpcion put(BotOpcion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return botOpcionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of BotOpcionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public BotOpcion post(BotOpcion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return botOpcionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of BotOpcionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Bot Opcion");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(botOpcionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of BotOpcionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        BotOpcion elimina = new BotOpcion();
        botOpcionDaoService.remove(elimina, id);
    }

}