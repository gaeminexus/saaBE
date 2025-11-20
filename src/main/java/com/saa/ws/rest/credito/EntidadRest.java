package com.saa.ws.rest.credito;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.credito.dao.EntidadDaoService;
import com.saa.ejb.credito.service.EntidadService;
import com.saa.model.credito.Entidad;
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

@Path("entd")
public class EntidadRest {

    @EJB
    private EntidadDaoService entidadDaoService;

    @EJB
    private EntidadService entidadService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public EntidadRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of EntidadRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Entidad> getAll() throws Throwable {
        return entidadDaoService.selectAll(NombreEntidadesCredito.ENTIDAD);
    }

    /**
     * Retrieves representation of an instance of EntidadRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public Entidad getId(@PathParam("id") Long id) throws Throwable {
        return entidadDaoService.selectById(id, NombreEntidadesCredito.ENTIDAD);
    }

    /**
     * PUT method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Entidad put(Entidad registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return entidadService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Entidad post(Entidad registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return entidadService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de Entidad");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(entidadService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of EntidadRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Entidad elimina = new Entidad();
        entidadDaoService.remove(elimina, id);
    }

}
