package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.EstadoParticipeDaoService;
import com.saa.ejb.credito.service.EstadoParticipeService;
import com.saa.model.credito.EstadoParticipe;
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

@Path("espr")
public class EstadoParticipeRest {

    @EJB
    private EstadoParticipeDaoService estadoParticipeDaoService;

    @EJB
    private EstadoParticipeService estadoParticipeService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public EstadoParticipeRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of EstadoParticipeRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<EstadoParticipe> getAll() throws Throwable {
        return estadoParticipeDaoService.selectAll(NombreEntidadesCredito.ESTADO_PARTICIPE);
    }

    /**
     * Retrieves representation of an instance of EstadoParticipeRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public EstadoParticipe getId(@PathParam("id") Long id) throws Throwable {
        return estadoParticipeDaoService.selectById(id, NombreEntidadesCredito.ESTADO_PARTICIPE);
    }

    /**
     * PUT method for updating or creating an instance of EstadoParticipeRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public EstadoParticipe put(EstadoParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return estadoParticipeService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EstadoParticipeRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public EstadoParticipe post(EstadoParticipe registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return estadoParticipeService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EstadoParticipeRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Estado Participe");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(estadoParticipeService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of EstadoParticipeRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        EstadoParticipe elimina = new EstadoParticipe();
        estadoParticipeDaoService.remove(elimina, id);
    }

}
