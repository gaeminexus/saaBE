package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.ComposicionCuotaInicialCobroDaoService;
import com.saa.ejb.cxc.service.ComposicionCuotaInicialCobroService;
import com.saa.model.cxc.ComposicionCuotaInicialCobro;
import com.saa.model.cxc.NombreEntidadesCobro;

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

@Path("ccic")
public class ComposicionCuotaInicialCobroRest {

    @EJB
    private ComposicionCuotaInicialCobroDaoService composicionCuotaInicialCobroDaoService;

    @EJB
    private ComposicionCuotaInicialCobroService composicionCuotaInicialCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ComposicionCuotaInicialCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ComposicionCuotaInicialCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ComposicionCuotaInicialCobro> getAll() throws Throwable {
        return composicionCuotaInicialCobroDaoService.selectAll(NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);
    }

    /**
     * Retrieves representation of an instance of ComposicionCuotaInicialCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public ComposicionCuotaInicialCobro getId(@PathParam("id") Long id) throws Throwable {
        return composicionCuotaInicialCobroDaoService.selectById(id, NombreEntidadesCobro.COMPOSICION_CUOTA_INICIAL_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of ComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(ComposicionCuotaInicialCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(composicionCuotaInicialCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of ComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(ComposicionCuotaInicialCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(composicionCuotaInicialCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of ComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de ComposicionCuotaInicialCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(composicionCuotaInicialCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of ComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        ComposicionCuotaInicialCobro elimina = new ComposicionCuotaInicialCobro();
        composicionCuotaInicialCobroDaoService.remove(elimina, id);
    }

}