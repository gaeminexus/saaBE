package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.DetalleMayorizacionCCDaoService;
import com.saa.ejb.contabilidad.service.DetalleMayorizacionCCService;
import com.saa.model.contabilidad.DetalleMayorizacionCC;
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

@Path("mycc")
public class DetalleMayorizacionCCRest {

    @EJB
    private DetalleMayorizacionCCDaoService detalleMayorizacionCCDaoService;

    @EJB
    private DetalleMayorizacionCCService detalleMayorizacionCCService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleMayorizacionCCRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleMayorizacionCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleMayorizacionCC> getAll() throws Throwable {
        return detalleMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
    }


    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleMayorizacionCC getId(@PathParam("id") Long id) throws Throwable {
        return detalleMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
    }

    /**
     * PUT method for updating or creating an instance of DetalleMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleMayorizacionCC put(DetalleMayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleMayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleMayorizacionCC post(DetalleMayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleMayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleMayorizacionCC> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return detalleMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
    }

    /**
     * POST method for updating or creating an instance of DetalleMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleMayorizacionCC elimina = new DetalleMayorizacionCC();
        detalleMayorizacionCCDaoService.remove(elimina, id);
    }

}
