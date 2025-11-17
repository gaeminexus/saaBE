package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.ExterDaoService;
import com.saa.ejb.credito.service.ExterService;
import com.saa.model.credito.Exter;
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

@Path("extr")
public class ExterRest {

    @EJB
    private ExterDaoService exterDaoService;

    @EJB
    private ExterService exterService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ExterRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ExterRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Exter> getAll() throws Throwable {
        return exterDaoService.selectAll(NombreEntidadesCredito.EXTER);
    }

    /**
     * Retrieves representation of an instance of ExterRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Exter getId(@PathParam("id") Long id) throws Throwable {
        return exterDaoService.selectById(id, NombreEntidadesCredito.EXTER);
    }

    /**
     * PUT method for updating or creating an instance of ExterRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Exter put(Exter registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return exterService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ExterRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Exter post(Exter registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return exterService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ExterRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Exter");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(exterService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of ExterRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Exter elimina = new Exter();
        exterDaoService.remove(elimina, id);
    }

}