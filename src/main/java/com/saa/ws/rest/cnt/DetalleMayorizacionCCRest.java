package com.saa.ws.rest.cnt;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.DetalleMayorizacionCCDaoService;
import com.saa.ejb.cnt.service.DetalleMayorizacionCCService;
import com.saa.model.cnt.DetalleMayorizacionCC;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleMayorizacionCC> lista = detalleMayorizacionCCDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleMayorizacionCC detalle = detalleMayorizacionCCDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYORIZACION_CC);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Detalle de mayorización CC con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleMayorizacionCC registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_MAYORIZACION_CC");
        try {
            DetalleMayorizacionCC resultado = detalleMayorizacionCCService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleMayorizacionCC registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_MAYORIZACION_CC");
        try {
            DetalleMayorizacionCC resultado = detalleMayorizacionCCService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_MAYORIZACION_CC");
        try {
            return Response.status(Response.Status.OK)
                    .entity(detalleMayorizacionCCService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_MAYORIZACION_CC");
        try {
            DetalleMayorizacionCC elimina = new DetalleMayorizacionCC();
            detalleMayorizacionCCDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de mayorización CC: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
