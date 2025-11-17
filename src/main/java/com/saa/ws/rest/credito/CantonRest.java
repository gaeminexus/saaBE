package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.CantonDaoService;
import com.saa.ejb.credito.service.CantonService;
import com.saa.model.credito.Canton;
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

@Path("cntn")
public class CantonRest {

    @EJB
    private CantonDaoService cantonDaoService;

    @EJB
    private CantonService cantonService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public CantonRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of CantonRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Canton> getAll() throws Throwable {
        return cantonDaoService.selectAll(NombreEntidadesCredito.CANTON);
    }

    /**
     * Retrieves representation of an instance of CantonRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Canton getId(@PathParam("id") Long id) throws Throwable {
        return cantonDaoService.selectById(id, NombreEntidadesCredito.CANTON);
    }

    /**
     * PUT method for updating or creating an instance of CantonRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Canton put(Canton registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return cantonService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CantonRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Canton post(Canton registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return cantonService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of CantonRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Canton");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(cantonService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of CantonRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Canton elimina = new Canton();
        cantonDaoService.remove(elimina, id);
    }

}