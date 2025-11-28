package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempComposicionCuotaInicialCobroDaoService;
import com.saa.ejb.cxc.service.TempComposicionCuotaInicialCobroService;
import com.saa.model.cxc.TempComposicionCuotaInicialCobro;
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

@Path("tcic")
public class TempComposicionCuotaInicialCobroRest {

    @EJB
    private TempComposicionCuotaInicialCobroDaoService tempComposicionCuotaInicialCobroDaoService;

    @EJB
    private TempComposicionCuotaInicialCobroService tempComposicionCuotaInicialCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempComposicionCuotaInicialCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempComposicionCuotaInicialCobro> getAll() throws Throwable {
        return tempComposicionCuotaInicialCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_COMPOSICION_CUOTA_INICIAL_COBRO);
    }

    /**
     * Retrieves representation of an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempComposicionCuotaInicialCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempComposicionCuotaInicialCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_COMPOSICION_CUOTA_INICIAL_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(TempComposicionCuotaInicialCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempComposicionCuotaInicialCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(TempComposicionCuotaInicialCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempComposicionCuotaInicialCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TempComposicionCuotaInicialCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempComposicionCuotaInicialCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempComposicionCuotaInicialCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TempComposicionCuotaInicialCobro elimina = new TempComposicionCuotaInicialCobro();
        tempComposicionCuotaInicialCobroDaoService.remove(elimina, id);
    }

}