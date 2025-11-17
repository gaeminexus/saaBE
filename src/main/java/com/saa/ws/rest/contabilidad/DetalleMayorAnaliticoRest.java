package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.contabilidad.service.DetalleMayorAnaliticoService;
import com.saa.model.contabilidad.DetalleMayorAnalitico;
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

@Path("dtma")
public class DetalleMayorAnaliticoRest {

    @EJB
    private DetalleMayorAnaliticoDaoService detalleMayorAnaliticoDaoService;

    @EJB
    private DetalleMayorAnaliticoService detalleMayorAnaliticoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleMayorAnaliticoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleMayorAnaliticoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleMayorAnalitico> getAll() throws Throwable {
        return detalleMayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
    }

   

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleMayorAnalitico getId(@PathParam("id") Long id) throws Throwable {
        return detalleMayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
    }

    /**
     * PUT method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleMayorAnalitico put(DetalleMayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleMayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleMayorAnalitico post(DetalleMayorAnalitico registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleMayorAnaliticoService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleMayorAnalitico> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return detalleMayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorAnaliticoRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleMayorAnalitico elimina = new DetalleMayorAnalitico();
        detalleMayorAnaliticoDaoService.remove(elimina, id);
    }

}