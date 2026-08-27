package com.saa.ws.rest.tsr;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.dao.PathCajaChicaDaoService;
import com.saa.ejb.tsr.service.PathCajaChicaService;
import com.saa.model.tsr.NombreEntidadesTesoreria;
import com.saa.model.tsr.PathCajaChica;

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

/**
 * REST para PathCajaChica (TSR.PTCH): adjuntos de un movimiento de caja chica.
 * Base path: /ptch
 *
 * El archivo físico se sube primero con {@code POST /file/upload/custom} (FileRest,
 * uploadPath sugerido: "caja-chica/{idCaja}/{idMovimiento}") y el path devuelto se
 * graba aquí con POST /ptch.
 */
@Path("ptch")
public class PathCajaChicaRest {

    @EJB
    private PathCajaChicaDaoService pathCajaChicaDaoService;

    @EJB
    private PathCajaChicaService pathCajaChicaService;

    @Context
    private UriInfo context;

    public PathCajaChicaRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PathCajaChica> lista =
                    pathCajaChicaDaoService.selectAll(NombreEntidadesTesoreria.PATH_CAJA_CHICA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener adjuntos de caja chica: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PathCajaChica entidad =
                    pathCajaChicaDaoService.selectById(id, NombreEntidadesTesoreria.PATH_CAJA_CHICA);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Adjunto con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el adjunto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PathCajaChica registro) {
        try {
            PathCajaChica resultado = pathCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar el adjunto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(PathCajaChica registro) {
        try {
            PathCajaChica resultado = pathCajaChicaService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al crear el adjunto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/porMovimiento/{idMovimiento}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porMovimiento(@PathParam("idMovimiento") Long idMovimiento) {
        try {
            List<PathCajaChica> resultado = pathCajaChicaService.porMovimiento(idMovimiento);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los adjuntos del movimiento: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        try {
            return Response.status(Response.Status.OK)
                    .entity(pathCajaChicaService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Elimina el adjunto y su archivo físico (FileService.deleteFile).
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            pathCajaChicaService.eliminar(id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar el adjunto: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
