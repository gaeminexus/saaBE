package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.ReporteContableDaoService;
import com.saa.ejb.contabilidad.service.ReporteContableService;
import com.saa.model.contabilidad.ReporteContable;
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

@Path("rprt")
public class ReporteContableRest {

    @EJB
    private ReporteContableDaoService reporteContableDaoService;

    @EJB
    private ReporteContableService reporteContableService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ReporteContableRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ReporteContableRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ReporteContable> getAll() throws Throwable {
        return reporteContableDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CONTABLE);
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of ReporteContableRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<ReporteContable> getDesc() throws Throwable {
//        return reporteContableDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of ReporteContableRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(reporteContableDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public ReporteContable getId(@PathParam("id") Long id) throws Throwable {
        return reporteContableDaoService.selectById(id, NombreEntidadesContabilidad.REPORTE_CONTABLE);
    }

    /**
     * PUT method for updating or creating an instance of ReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public ReporteContable put(ReporteContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return reporteContableService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public ReporteContable post(ReporteContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return reporteContableService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<ReporteContable> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return reporteContableDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CONTABLE);
    }

    /**
     * POST method for updating or creating an instance of ReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        ReporteContable elimina = new ReporteContable();
        reporteContableDaoService.remove(elimina, id);
    }

}
