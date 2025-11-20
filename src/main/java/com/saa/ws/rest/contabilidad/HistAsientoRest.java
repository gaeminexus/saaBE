package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.HistAsientoDaoService;
import com.saa.ejb.contabilidad.service.HistAsientoService;
import com.saa.model.contabilidad.HistAsiento;
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
import jakarta.ws.rs.core.UriInfo;

@Path("asnh")
public class HistAsientoRest {

    @EJB
    private HistAsientoDaoService histAsientoDaoService;

    @EJB
    private HistAsientoService histAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistAsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistAsiento> getAll() throws Throwable {
        return histAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_ASIENTO);
    }
    
    
    
    /* Comentamos esta parte por que ya no estamos usando el orden descendente
     */

    /**
     * Retrieves representation of an instance of HistAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<HistAsiento> getDesc() throws Throwable {
        return histAsientoDaoService.selectOrderDesc();
    }


    /**
     * Retrieves representation of an instance of HistAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
    
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(histAsientoDaoService.selectOrderDesc()).build();
    }
    */

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public HistAsiento getId(@PathParam("id") Long id) throws Throwable {
        return histAsientoDaoService.selectById(id, NombreEntidadesContabilidad.HIST_ASIENTO);
    }

    /**
     * PUT method for updating or creating an instance of HistAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public HistAsiento put(HistAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return histAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public HistAsiento post(HistAsiento registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return histAsientoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<HistAsiento> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return histAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_ASIENTO);
    }

    /**
     * POST method for updating or creating an instance of HistAsientoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        HistAsiento elimina = new HistAsiento();
        histAsientoDaoService.remove(elimina, id);
    }

}
