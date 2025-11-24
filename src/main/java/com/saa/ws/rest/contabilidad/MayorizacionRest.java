package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.MayorizacionDaoService;
import com.saa.ejb.contabilidad.service.MayorizacionService;
import com.saa.model.contabilidad.Mayorizacion;
import com.saa.model.contabilidad.NombreEntidadesContabilidad;

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

@Path("myrz")
public class MayorizacionRest {

    @EJB
    private MayorizacionDaoService mayorizacionDaoService;

    @EJB
    private MayorizacionService mayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public MayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of MayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Mayorizacion> getAll() throws Throwable {
        return mayorizacionDaoService.selectAll(NombreEntidadesContabilidad.MAYORIZACION);
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Mayorizacion getId(@PathParam("id") Long id) throws Throwable {
        return mayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.MAYORIZACION);
    }

    /**
     * PUT method for updating or creating an instance of MayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Mayorizacion put(Mayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return mayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Mayorizacion post(Mayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return mayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of MayorizacionRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de MAYORIZACION");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(mayorizacionService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of MayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Mayorizacion elimina = new Mayorizacion();
        mayorizacionDaoService.remove(elimina, id);
    }

}
