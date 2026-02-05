package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.DetalleReporteCuentaCCDaoService;
import com.saa.ejb.cnt.service.DetalleReporteCuentaCCService;
import com.saa.model.cnt.DetalleReporteCuentaCC;
import com.saa.model.cnt.NombreEntidadesContabilidad;

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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleReporteCuentaCC> lista = detalleReporteCuentaCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de reporte cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /*  Comentamos esta parte por que ya no estamos usando el orden descendente
     * Retrieves representation of an instance of DetalleReporteCuentaCCRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getDesc")
    public Response getDesc() {
        try {
            List<DetalleReporteCuentaCC> lista = detalleReporteCuentaCCDaoService.selectOrderDesc();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
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
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleReporteCuentaCC detalle = detalleReporteCuentaCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_REPORTE_CUENTA_CC);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Detalle de reporte cuenta CC con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de reporte cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of DetalleReporteCuentaCCRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleReporteCuentaCC registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_REPORTE_CUENTA_CC");
        try {
            DetalleReporteCuentaCC resultado = detalleReporteCuentaCCService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de reporte cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleReporteCuentaCC registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_REPORTE_CUENTA_CC");
        try {
            DetalleReporteCuentaCC resultado = detalleReporteCuentaCCService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de reporte cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_REPORTE_CUENTA_CC");
        try {
            return Response.status(Response.Status.OK)
                    .entity(detalleReporteCuentaCCService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_REPORTE_CUENTA_CC");
        try {
            DetalleReporteCuentaCC elimina = new DetalleReporteCuentaCC();
            detalleReporteCuentaCCDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de reporte cuenta CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
