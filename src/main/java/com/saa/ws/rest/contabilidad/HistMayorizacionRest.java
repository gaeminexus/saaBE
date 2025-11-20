package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.HistMayorizacionDaoService;
import com.saa.ejb.contabilidad.service.HistMayorizacionService;
import com.saa.model.contabilidad.HistMayorizacion;
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

@Path("myrh")
public class HistMayorizacionRest {

    @EJB
    private HistMayorizacionDaoService histMayorizacionDaoService;

    @EJB
    private HistMayorizacionService histMayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistMayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistMayorizacion> getAll() throws Throwable {
        return histMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_MAYORIZACION);
    }
    
    
    /* Comentamos esta perte por que ya no estamos usando orden descendente

    /**
     * Retrieves representation of an instance of HistMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<HistMayorizacion> getDesc() throws Throwable {
        return histMayorizacionDaoService.selectOrderDesc();
    }

    /**
     * Retrieves representation of an instance of HistMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(histMayorizacionDaoService.selectOrderDesc()).build();
    }
    */

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public HistMayorizacion getId(@PathParam("id") Long id) throws Throwable {
        return histMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_MAYORIZACION);
    }

    /**
     * PUT method for updating or creating an instance of HistMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public HistMayorizacion put(HistMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return histMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public HistMayorizacion post(HistMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return histMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<HistMayorizacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return histMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_MAYORIZACION);
    }

    /**
     * POST method for updating or creating an instance of HistMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        HistMayorizacion elimina = new HistMayorizacion();
        histMayorizacionDaoService.remove(elimina, id);
    }

}
