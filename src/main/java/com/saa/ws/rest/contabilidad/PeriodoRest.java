package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.PeriodoDaoService;
import com.saa.ejb.contabilidad.service.PeriodoService;
import com.saa.model.contabilidad.Periodo;
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

@Path("prdo")
public class PeriodoRest {

    @EJB
    private PeriodoDaoService periodoDaoService;

    @EJB
    private PeriodoService periodoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PeriodoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PeriodoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<Periodo> getAll() throws Throwable {
        return periodoDaoService.selectAll(NombreEntidadesContabilidad.PERIODO);
    }

    /* Comentamos esta parte por que ya no usamos orden descendente
     * Retrieves representation of an instance of PeriodoRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<Periodo> getDesc() throws Throwable {
        return periodoDaoService.selectOrderDesc();
    }

    /**
     * Retrieves representation of an instance of PeriodoRest
     * 
     * @return an instance of String
     * @throws Throwable
    
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(periodoDaoService.selectOrderDesc()).build();
    }
   */

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public Periodo getId(@PathParam("id") Long id) throws Throwable {
        return periodoDaoService.selectById(id, NombreEntidadesContabilidad.PERIODO);
    }

    /**
     * PUT method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public Periodo put(Periodo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return periodoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public Periodo post(Periodo registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return periodoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<Periodo> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return periodoDaoService.selectAll(NombreEntidadesContabilidad.PERIODO);
    }

    /**
     * POST method for updating or creating an instance of PeriodoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        Periodo elimina = new Periodo();
        periodoDaoService.remove(elimina, id);
    }

}
