package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.HistDetalleMayorizacionDaoService;
import com.saa.ejb.contabilidad.service.HistDetalleMayorizacionService;
import com.saa.model.contabilidad.HistDetalleMayorizacion;
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

@Path("dtmh")
public class HistDetalleMayorizacionRest {

    @EJB
    private HistDetalleMayorizacionDaoService histDetalleMayorizacionDaoService;

    @EJB
    private HistDetalleMayorizacionService histDetalleMayorizacionService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistDetalleMayorizacionRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistDetalleMayorizacionRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<HistDetalleMayorizacion> getAll() throws Throwable {
        return histDetalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
    }
    
    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public HistDetalleMayorizacion getId(@PathParam("id") Long id) throws Throwable {
        return histDetalleMayorizacionDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
    }

    /**
     * PUT method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public HistDetalleMayorizacion put(HistDetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return histDetalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public HistDetalleMayorizacion post(HistDetalleMayorizacion registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return histDetalleMayorizacionService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<HistDetalleMayorizacion> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return histDetalleMayorizacionDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_MAYORIZACION);
    }

    /**
     * POST method for updating or creating an instance of HistDetalleMayorizacionRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        HistDetalleMayorizacion elimina = new HistDetalleMayorizacion();
        histDetalleMayorizacionDaoService.remove(elimina, id);
    }

}
