package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.DetalleReporteCuentaCCDaoService;
import com.saa.ejb.contabilidad.service.DetalleReporteCuentaCCService;
import com.saa.model.contabilidad.DetalleReporteCuentaCC;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("detalleReporteCuentaCC")
public class DetalleReporteCuentaCCRest {

    @EJB
    private DetalleReporteCuentaCCDaoService detalleReporteCuentaCCDaoService;

    @EJB
    private DetalleReporteCuentaCCService detalleReporteCuentaCCService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetalleReporteCuentaCCRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetalleReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces("application/json")
    public List<DetalleReporteCuentaCC> getAll() throws Throwable {
        return detalleReporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC);
    }

    /*  Comentamos esta parte por que ya no estamos usando el orden descendente
     * Retrieves representation of an instance of DetalleReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getDesc")
    public List<DetalleReporteCuentaCC> getDesc() throws Throwable {
        return detalleReporteCuentaCCDaoService.selectOrderDesc();
    }

    /**
     * Retrieves representation of an instance of DetalleReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(detalleReporteCuentaCCDaoService.selectOrderDesc()).build();
    }
    */

    @GET
    @Produces("application/json")
    @Path("/getId/{id}")
    public DetalleReporteCuentaCC getId(@PathParam("id") Long id) throws Throwable {
        return detalleReporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC);
    }

    /**
     * PUT method for updating or creating an instance of DetalleReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public DetalleReporteCuentaCC put(DetalleReporteCuentaCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO PUT");
        return detalleReporteCuentaCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Consumes("application/json")
    public DetalleReporteCuentaCC post(DetalleReporteCuentaCC registro) throws Throwable {
        System.out.println("LLEGA AL SERVICIO");
        return detalleReporteCuentaCCService.saveSingle(registro);
    }

    /**
     * POST method for updating or creating an instance of DetalleReporteCuentaCCRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes("application/json")
    public Response selectByCriteria(List<DatosBusqueda> registros) throws Throwable {
        System.out.println("selectByCriteria de DETALLE_REPORTE_CUENTA_CC");
        Response respuesta = null;
        try {
            respuesta = Response.status(Response.Status.OK).entity(detalleReporteCuentaCCService.selectByCriteria(registros)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            respuesta = Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
        return respuesta;
    }
    /**
     * POST method for updating or creating an instance of DetalleReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @DELETE
    @Consumes("application/json")
    @Path("/{id}")
    public void delete(@PathParam("id") Long id) throws Throwable {
        System.out.println("LLEGA AL SERVICIO DELETE");
        DetalleReporteCuentaCC elimina = new DetalleReporteCuentaCC();
        detalleReporteCuentaCCDaoService.remove(elimina, id);
    }

}
