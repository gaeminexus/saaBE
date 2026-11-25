package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.HistDetalleAsientoDaoService;
import com.saa.ejb.cnt.service.HistDetalleAsientoService;
import com.saa.model.cnt.HistDetalleAsiento;
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

@Path("dtah")
public class HistDetalleAsientoRest {

    @EJB
    private HistDetalleAsientoDaoService histDetalleAsientoDaoService;

    @EJB
    private HistDetalleAsientoService histDetalleAsientoService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public HistDetalleAsientoRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<HistDetalleAsiento> lista = histDetalleAsientoDaoService.selectAll(NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener historial de detalles de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    /* Eliminamos esta parte por que ya no usamos orden descendente

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
     
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/getDesc")
    public Response getDesc() {
        try {
            List<HistDetalleAsiento> lista = histDetalleAsientoDaoService.selectOrderDesc();
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Retrieves representation of an instance of HistDetalleAsientoRest
     * 
     * @return an instance of String
     * @throws Throwable
    
    @GET
    @Produces("application/json")
    @Path("/getTest")
    public Response getTest() throws Throwable {
        return Response.status(200).entity(histDetalleAsientoDaoService.selectOrderDesc()).build();
    }
    
    */

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            HistDetalleAsiento hist = histDetalleAsientoDaoService.selectById(id, NombreEntidadesContabilidad.HIST_DETALLE_ASIENTO);
            if (hist == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Historial de detalle de asiento con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(hist).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener historial de detalle de asiento: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de HIST_DETALLE_ASIENTO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(histDetalleAsientoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

}
