package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.PlanCuentaDaoService;
import com.saa.ejb.contabilidad.service.PlanCuentaService;
import com.saa.model.contabilidad.PlanCuenta;
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

@Path("plnn")
public class PlanCuentaRest {

    @EJB
    private PlanCuentaDaoService planCuentaDaoService;

    @EJB
    private PlanCuentaService planCuentaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public PlanCuentaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of PlanCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<PlanCuenta> getAll() throws Throwable {
        return planCuentaDaoService.selectAll(NombreEntidadesContabilidad.PLAN_CUENTA);
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of PlanCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<PlanCuenta> getDesc() throws Throwable {
//        return planCuentaDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of PlanCuentaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(planCuentaDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public PlanCuenta getId(@PathParam("id") Long id) throws Throwable {
        return planCuentaDaoService.selectById(id, NombreEntidadesContabilidad.PLAN_CUENTA);
    }

    /**
     * PUT method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public PlanCuenta put(PlanCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return planCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public PlanCuenta post(PlanCuenta registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return planCuentaService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<PlanCuenta> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return planCuentaDaoService.selectAll(NombreEntidadesContabilidad.PLAN_CUENTA);
    }

    /**
     * POST method for updating or creating an instance of PlanCuentaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        PlanCuenta elimina = new PlanCuenta();
        planCuentaDaoService.remove(elimina, id);
    }

}
