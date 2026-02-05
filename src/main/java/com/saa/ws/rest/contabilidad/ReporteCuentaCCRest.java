package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.ReporteCuentaCCDaoService;
import com.saa.ejb.contabilidad.service.ReporteCuentaCCService;
import com.saa.model.cnt.NombreEntidadesContabilidad;
import com.saa.model.cnt.ReporteCuentaCC;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<ReporteCuentaCC> lista = reporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener reportes de cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            ReporteCuentaCC reporte = reporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.REPORTE_CUENTA_CC);
            if (reporte == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Reporte de cuenta CC con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(reporte).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener reporte de cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of ReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(ReporteCuentaCC registro) {
        System.out.println("LLEGA AL SERVICIO PUT - REPORTE_CUENTA_CC");
        try {
            ReporteCuentaCC resultado = reporteCuentaCCService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar reporte de cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ReporteCuentaCC registro) {
        System.out.println("LLEGA AL SERVICIO POST - REPORTE_CUENTA_CC");
        try {
            ReporteCuentaCC resultado = reporteCuentaCCService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear reporte de cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of ReporteCuentaCCRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de REPORTE_CUENTA_CC");
        try {
            return Response.status(Response.Status.OK)
                    .entity(reporteCuentaCCService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - REPORTE_CUENTA_CC");
        try {
            ReporteCuentaCC elimina = new ReporteCuentaCC();
            reporteCuentaCCDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar reporte de cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
