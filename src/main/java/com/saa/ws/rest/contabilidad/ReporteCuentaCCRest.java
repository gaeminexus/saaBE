package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.ReporteCuentaCCDaoService;
import com.saa.ejb.contabilidad.service.ReporteCuentaCCService;
import com.saa.model.contabilidad.ReporteCuentaCC;
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

@Path("rcnc")
public class ReporteCuentaCCRest {

    @EJB
    private ReporteCuentaCCDaoService reporteCuentaCCDaoService;

    @EJB
    private ReporteCuentaCCService reporteCuentaCCService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public ReporteCuentaCCRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of ReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<ReporteCuentaCC> getAll() throws Throwable {
        return reporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of ReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<ReporteCuentaCC> getDesc() throws Throwable {
//        return reporteCuentaCCDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of ReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(reporteCuentaCCDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public ReporteCuentaCC getId(@PathParam("id") Long id) throws Throwable {
        return reporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
    }

    /**
     * PUT method for updating or creating an instance of ReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public ReporteCuentaCC put(ReporteCuentaCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return reporteCuentaCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public ReporteCuentaCC post(ReporteCuentaCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return reporteCuentaCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of ReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<ReporteCuentaCC> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return reporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
    }

    /**
     * POST method for updating or creating an instance of ReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        ReporteCuentaCC elimina = new ReporteCuentaCC();
        reporteCuentaCCDaoService.remove(elimina, id);
    }

}
