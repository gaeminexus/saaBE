package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.HoraExtraDaoService;
import com.saa.ejb.rhh.service.HoraExtraService;
import com.saa.model.rhh.HoraExtra;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("hrex")
public class HoraExtraRest {

    @EJB
    private HoraExtraDaoService horaExtraDaoService;

    @EJB
    private HoraExtraService horaExtraService;

    @Context
    private UriInfo context;

    public HoraExtraRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        System.out.println("LLEGA AL SERVICIO GET ALL - HORAEXTRA");
        try {
            List<HoraExtra> lista = horaExtraDaoService.selectAll(NombreEntidadesRhh.HORA_EXTRA);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO GET ID - HORAEXTRA");
        try {
            HoraExtra registro = horaExtraDaoService.selectById(id, NombreEntidadesRhh.HORA_EXTRA);
            if (registro == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Registro con ID " + id + " no encontrado").type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(registro).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(HoraExtra registro) {
        System.out.println("LLEGA AL SERVICIO PUT - HORAEXTRA");
        try {
            HoraExtra actualizado = horaExtraService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(HoraExtra registro) {
        System.out.println("LLEGA AL SERVICIO POST - HORAEXTRA");
        try {
            HoraExtra creado = horaExtraService.saveSingle(registro);
            return Response.status(Response.Status.CREATED).entity(creado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al crear registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria de HORAEXTRA");
        try {
            List<HoraExtra> lista = horaExtraService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en busqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - HORAEXTRA");
        try {
            horaExtraService.remove(List.of(id));
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
    // =====================================================================
    // Endpoint de proceso - fase 7
    // =====================================================================

    /**
     * Aprueba las horas extra indicadas para que el motor las pague.
     *
     * <p>Cuerpo <code>List&lt;Long&gt;</code> de ids, siguiendo el precedente de
     * <code>/rest/rlpg/registrarRecepcion</code>: la aprobacion se hace por tandas.</p>
     *
     * <p>La ruta va <b>antes</b> del DELETE de <code>/{id}</code> por claridad, pero no hay
     * ambiguedad: son metodos HTTP distintos.</p>
     */
    @POST
    @Path("/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(List<Long> ids, @QueryParam("usuarioRegistro") String usuarioRegistro) {
        System.out.println("LLEGA AL SERVICIO aprobar - HORA_EXTRA, "
                + (ids != null ? ids.size() : 0) + " id(s)");
        try {
            if (ids == null || ids.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("No se indico ninguna hora extra que aprobar.").type(MediaType.APPLICATION_JSON).build();
            }
            int aprobadas = 0;
            for (Long id : ids) {
                HoraExtra hora = horaExtraDaoService.selectById(id, NombreEntidadesRhh.HORA_EXTRA);
                if (hora == null) {
                    // Se aborta la tanda: dejarla a medias dejaria al operador sin saber
                    // cuales quedaron aprobadas.
                    return Response.status(Response.Status.NOT_FOUND).entity("No existe la hora extra " + id + ". No se aprobo ninguna de la tanda.").type(MediaType.APPLICATION_JSON).build();
                }
                hora.setAprobada("S");
                hora.setUsuarioAprueba(usuarioRegistro);
                hora.setFechaAprobacion(LocalDate.now());
                horaExtraService.saveSingle(hora);
                aprobadas++;
            }
            return Response.status(Response.Status.OK).entity(Integer.valueOf(aprobadas)).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al aprobar las horas extra: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }
}
