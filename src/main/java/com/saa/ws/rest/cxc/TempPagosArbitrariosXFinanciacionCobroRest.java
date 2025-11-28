package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.dao.TempPagosArbitrariosXFinanciacionCobroDaoService;
import com.saa.ejb.cxc.service.TempPagosArbitrariosXFinanciacionCobroService;
import com.saa.model.cxc.TempPagosArbitrariosXFinanciacionCobro;
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

@Path("tpfc")
public class TempPagosArbitrariosXFinanciacionCobroRest {

    @EJB
    private TempPagosArbitrariosXFinanciacionCobroDaoService tempPagosArbitrariosXFinanciacionCobroDaoService;

    @EJB
    private TempPagosArbitrariosXFinanciacionCobroService tempPagosArbitrariosXFinanciacionCobroService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempPagosArbitrariosXFinanciacionCobroRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempPagosArbitrariosXFinanciacionCobro> getAll() throws Throwable {
        return tempPagosArbitrariosXFinanciacionCobroDaoService.selectAll(NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
    }

    /**
     * Retrieves representation of an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getId/{id}")
    @Produces("application/json")
    public TempPagosArbitrariosXFinanciacionCobro getId(@PathParam("id") Long id) throws Throwable {
        return tempPagosArbitrariosXFinanciacionCobroDaoService.selectById(id, NombreEntidadesCobro.TEMP_PAGOS_ARBITRARIOS_X_FINANCIACION_COBRO);
    }

    /**
     * PUT method for updating or creating an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Response put(TempPagosArbitrariosXFinanciacionCobro registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempPagosArbitrariosXFinanciacionCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Response post(TempPagosArbitrariosXFinanciacionCobro registro) throws Throwable {
        System.out.println("LLEGA AL POST");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempPagosArbitrariosXFinanciacionCobroService.saveSingle(registro)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de TempPagosArbitrariosXFinanciacionCobro");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(tempPagosArbitrariosXFinanciacionCobroService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of TempPagosArbitrariosXFinanciacionCobroRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Path("/{id}")
    @Consumes("application/json")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TempPagosArbitrariosXFinanciacionCobro elimina = new TempPagosArbitrariosXFinanciacionCobro();
        tempPagosArbitrariosXFinanciacionCobroDaoService.remove(elimina, id);
    }

}