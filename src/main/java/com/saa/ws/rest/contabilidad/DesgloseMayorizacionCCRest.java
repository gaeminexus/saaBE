package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.DesgloseMayorizacionCCDaoService;
import com.saa.ejb.contabilidad.service.DesgloseMayorizacionCCService;
import com.saa.model.contabilidad.DesgloseMayorizacionCC;
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

@Path("dtmc")
public class DesgloseMayorizacionCCRest {

    @EJB
    private DesgloseMayorizacionCCDaoService desgloseMayorizacionCCDaoService;

    @EJB
    private DesgloseMayorizacionCCService desgloseMayorizacionCCService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DesgloseMayorizacionCCRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DesgloseMayorizacionCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DesgloseMayorizacionCC> getAll() throws Throwable {
        return desgloseMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
    }


    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DesgloseMayorizacionCC getId(@PathParam("id") Long id) throws Throwable {
        return desgloseMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
    }

    /**
     * PUT method for updating or creating an instance of DesgloseMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DesgloseMayorizacionCC put(DesgloseMayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return desgloseMayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DesgloseMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DesgloseMayorizacionCC post(DesgloseMayorizacionCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return desgloseMayorizacionCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DesgloseMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DesgloseMayorizacionCC> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return desgloseMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DESGLOSE_MAYORIZACION_CC);
    }

    /**
     * POST method for updating or creating an instance of DesgloseMayorizacionCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DesgloseMayorizacionCC elimina = new DesgloseMayorizacionCC();
        desgloseMayorizacionCCDaoService.remove(elimina, id);
    }

}