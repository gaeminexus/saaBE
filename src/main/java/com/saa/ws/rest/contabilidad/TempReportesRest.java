package com.saa.ws.rest.contabilidad;


import java.util.List;

import com.saa.ejb.contabilidad.dao.TempReportesDaoService;
import com.saa.ejb.contabilidad.service.TempReportesService;
import com.saa.model.contabilidad.TempReportes;
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

@Path("tempReportes")
public class TempReportesRest {

    @EJB
    private TempReportesDaoService tempReportesDaoService;

    @EJB
    private TempReportesService tempReportesService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public TempReportesRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<TempReportes> getAll() throws Throwable {
        return tempReportesDaoService.selectAll(NombreEntidadesContabilidad.TEMP_REPORTES);
    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getDesc")
//    public List<TempReportes> getDesc() throws Throwable {
//        return tempReportesDaoService.selectOrderDesc();
//    }

    // Ya no usamos esta parte porque no hacemos un orden descendente
    /**
     * Retrieves representation of an instance of TempReportesRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
//    @GET
//    @Produces("application/json")
//    @Path("/getTest")
//    public Response getTest() throws Throwable {
//        return Response.status(200).entity(tempReportesDaoService.selectOrderDesc()).build();
//    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public TempReportes getId(@PathParam("id") Long id) throws Throwable {
        return tempReportesDaoService.selectById(id, NombreEntidadesContabilidad.TEMP_REPORTES);
    }

    /**
     * PUT method for updating or creating an instance of TempReportesRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public TempReportes put(TempReportes registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return tempReportesService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempReportesRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public TempReportes post(TempReportes registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return tempReportesService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of TempReportesRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<TempReportes> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return tempReportesDaoService.selectAll(NombreEntidadesContabilidad.TEMP_REPORTES);
    }

    /**
     * POST method for updating or creating an instance of TempReportesRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        TempReportes elimina = new TempReportes();
        tempReportesDaoService.remove(elimina, id);
    }

}
