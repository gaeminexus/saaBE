package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.ejb.contabilidad.dao.DetalleReporteContableDaoService;
import com.saa.ejb.contabilidad.service.DetalleReporteContableService;
import com.saa.model.contabilidad.DetalleReporteContable;
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

@Path("dtrp")
public class DetalleReporteContableRest {

    @EJB
    private DetalleReporteContableDaoService detalleReporteContableDaoService;

    @EJB
    private DetalleReporteContableService detalleReporteContableService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleReporteContableRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleReporteContableRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleReporteContable> getAll() throws Throwable {
        return detalleReporteContableDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE);
    }

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleReporteContable getId(@PathParam("id") Long id) throws Throwable {
        return detalleReporteContableDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE);
    }

    /**
     * PUT method for updating or creating an instance of DetalleReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleReporteContable put(DetalleReporteContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleReporteContableService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleReporteContable post(DetalleReporteContable registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleReporteContableService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @Path("criteria")
    @POST
    @Consumes("application/json")
    public List<DetalleReporteContable> selectByCriteria(Long test) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DE SELECT BY CRITERIA: " + test);
        return detalleReporteContableDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CONTABLE);
    }

    /**
     * POST method for updating or creating an instance of DetalleReporteContableRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleReporteContable elimina = new DetalleReporteContable();
        detalleReporteContableDaoService.remove(elimina, id);
    }

}
