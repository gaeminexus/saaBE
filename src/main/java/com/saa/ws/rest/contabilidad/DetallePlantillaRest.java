package com.saa.ws.rest.contabilidad;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.contabilidad.dao.DetallePlantillaDaoService;
import com.saa.ejb.contabilidad.service.DetallePlantillaService;
import com.saa.model.contabilidad.DetallePlantilla;
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

@Path("dtpl")
public class DetallePlantillaRest {

    @EJB
    private DetallePlantillaDaoService detallePlantillaDaoService;

    @EJB
    private DetallePlantillaService detallePlantillaService;

    @Context
    private UriInfo context;

    /**
     * Default constructor.
     */
    public DetallePlantillaRest() {
        // TODO Auto-generated constructor stub
    }

    /**
     * Retrieves representation of an instance of DetallePlantillaRest
     * 
     * @return an instance of String
     * @throws Throwable
     */
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<DetallePlantilla> lista = detallePlantillaDaoService.selectAll(NombreEntidadesContabilidad.DETALLE_PLANTILLA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles de plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }


    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DetallePlantilla detalle = detallePlantillaDaoService.selectById(id, NombreEntidadesContabilidad.DETALLE_PLANTILLA);
            if (detalle == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Detalle de plantilla con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(detalle).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalle de plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    
    @GET
    @Path("/getByParent/{idParent}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByParent(@PathParam("idParent") Long idParent) {
        try {
            List<DetallePlantilla> lista = detallePlantillaDaoService.selectByPlantilla(idParent);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener detalles por plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * PUT method for updating or creating an instance of DetallePlantillaRest
     * 
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(DetallePlantilla registro) {
        System.out.println("LLEGA AL SERVICIO PUT - DETALLE_PLANTILLA");
        try {
            DetallePlantilla resultado = detallePlantillaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar detalle de plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(DetallePlantilla registro) {
        System.out.println("LLEGA AL SERVICIO POST - DETALLE_PLANTILLA");
        try {
            DetallePlantilla resultado = detallePlantillaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear detalle de plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * POST method for updating or creating an instance of DetallePlantillaRest
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de DETALLE_PLANTILLA");
        try {
            return Response.status(Response.Status.OK)
                    .entity(detallePlantillaService.selectByCriteria(registros))
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
        System.out.println("LLEGA AL SERVICIO DELETE - DETALLE_PLANTILLA");
        try {
            DetallePlantilla elimina = new DetallePlantilla();
            detallePlantillaDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar detalle de plantilla: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

}
