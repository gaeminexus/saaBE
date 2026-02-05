package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cnt.dao.DetalleMayorAnaliticoDaoService;
import com.saa.ejb.cnt.service.DetalleMayorAnaliticoService;
import com.saa.model.cnt.DetalleMayorAnalitico;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetalleMayorAnalitico> lista = detalleMayorAnaliticoDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetalleMayorAnalitico detalle = detalleMayorAnaliticoDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_MAYOR_ANALITICO);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Detalle de mayor analítico con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetalleMayorAnalitico registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_MAYOR_ANALITICO");
        try {
            DetalleMayorAnalitico resultado = detalleMayorAnaliticoService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetalleMayorAnalitico registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_MAYOR_ANALITICO");
        try {
            DetalleMayorAnalitico resultado = detalleMayorAnaliticoService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_MAYOR_ANALITICO");
        try {
            return Response.status(Response.Status.OK)
                    .entity(detalleMayorAnaliticoService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_MAYOR_ANALITICO");
        try {
            DetalleMayorAnalitico elimina = new DetalleMayorAnalitico();
            detalleMayorAnaliticoDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de mayor analítico: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
