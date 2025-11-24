package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.HistDetalleAsientoDaoService;
import com.saa.ejb.contabilidad.service.HistDetalleAsientoService;
import com.saa.model.contabilidad.HistDetalleAsiento;
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

@Path("dtah")
public class HistDetalleAsientoRest {

    @EJB
    private HistDetalleAsientoDaoService histDetalleAsientoDaoService;

    @EJB
    private HistDetalleAsientoService histDetalleAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistDetalleAsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistDetalleAsiento> getAll() throws Throwable {
        return histDetalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO);
    }
    
    /* Eliminamos esta parte por que ya no usamos orden descendente

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<HistDetalleAsiento> getDesc() throws Throwable {
        return histDetalleAsientoDaoService.selectOrderDesc();
    }

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
    
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(histDetalleAsientoDaoService.selectOrderDesc()).build();
    }
    
    */

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public HistDetalleAsiento getId(@PathParam("id") Long id) throws Throwable {
        return histDetalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO);
    }

    /**
     * PUT method for updating or creating an instance of HistDetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public HistDetalleAsiento put(HistDetalleAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return histDetalleAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public HistDetalleAsiento post(HistDetalleAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return histDetalleAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleAsientoRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de HIST_DETALLE_ASIENTO");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(histDetalleAsientoService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }

    /**
     * POST method for updating or creating an instance of HistDetalleAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        HistDetalleAsiento elimina = new HistDetalleAsiento();
        histDetalleAsientoDaoService.remove(elimina, id);
    }

}
