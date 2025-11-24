package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.AsientoDaoService;
import com.saa.ejb.contabilidad.service.AsientoService;
import com.saa.model.contabilidad.Asiento;
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

@Path("asnt")
public class AsientoRest {

    @EJB
    private AsientoDaoService asientoDaoService;

    @EJB
    private AsientoService asientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public AsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of AsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Asiento> getAll() throws Throwable {
        return asientoDaoService.selectAll(NombreEntidadesContabilidad.ASIENTO);
    }


    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Asiento getId(@PathParam("id") Long id) throws Throwable {
        return asientoDaoService.selectById(id, NombreEntidadesContabilidad.ASIENTO);
    }

    /**
     * PUT method for updating or creating an instance of AsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Asiento put(Asiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return asientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of AsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Asiento post(Asiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return asientoService.saveSingle(registro);
    }
    
    /**
     * POST method for updating or creating an instance of AsientoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de ASIENTO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(asientoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of AsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Asiento elimina = new Asiento();
        asientoDaoService.remove(elimina, id);
    }

}
