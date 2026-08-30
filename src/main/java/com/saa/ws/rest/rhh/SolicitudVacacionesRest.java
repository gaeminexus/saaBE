package com.saa.ws.rest.rhh;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.rhh.dao.SolicitudVacacionesDaoService;
import com.saa.ejb.rhh.service.SolicitudVacacionesService;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.SolicitudVacaciones;

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

@Path("slct")
public class SolicitudVacacionesRest {

    @EJB
    private SolicitudVacacionesDaoService SolicitudVacacionesDaoService;

    @EJB
    private SolicitudVacacionesService SolicitudVacacionesService;

    @Context
    private UriInfo context;

    public SolicitudVacacionesRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<SolicitudVacaciones> lista = SolicitudVacacionesDaoService.selectAll(NombreEntidadesRhh.SOLICITUD_VACACIONES);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al obtener registros: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            SolicitudVacaciones registro = SolicitudVacacionesDaoService.selectById(id, NombreEntidadesRhh.SOLICITUD_VACACIONES);
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
    public Response put(SolicitudVacaciones registro) {
        System.out.println("LLEGA AL SERVICIO PUT - SOLICITUD_VACACIONES");
        try {
            SolicitudVacaciones actualizado = SolicitudVacacionesService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(actualizado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al actualizar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(SolicitudVacaciones registro) {
        System.out.println("LLEGA AL SERVICIO POST - SOLICITUD_VACACIONES");
        try {
            SolicitudVacaciones creado = SolicitudVacacionesService.saveSingle(registro);
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
        System.out.println("selectByCriteria de SOLICITUD_VACACIONES");
        try {
            List<SolicitudVacaciones> lista = SolicitudVacacionesService.selectByCriteria(registros);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Error en búsqueda: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("LLEGA AL SERVICIO DELETE - SOLICITUD_VACACIONES");
        try {
            SolicitudVacaciones elimina = new SolicitudVacaciones();
            SolicitudVacacionesDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error al eliminar registro: " + e.getMessage()).type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Aprueba una solicitud de vacaciones: recalcula los dias, valida el saldo
     * disponible, lo consume FIFO y genera la novedad del periodo de la fecha de
     * inicio. Body: {"idUsuario": N, "observacion": "..." (opcional)}.
     * Ver docs/logica-negocio/rhh/CICLO-APROBACION-VACACIONES.md.
     */
    @POST
    @Path("/aprobar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response aprobar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /slct/aprobar/" + id);
        try {
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            String observacion = (datos != null && datos.get("observacion") != null)
                    ? datos.get("observacion").toString() : null;
            SolicitudVacaciones resultado = SolicitudVacacionesService.aprobar(id, idUsuario, observacion);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al aprobar la solicitud: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Rechaza una solicitud de vacaciones. No toca saldo ni novedad.
     * Body: {"idUsuario": N, "motivo": "..." (opcional)}.
     */
    @POST
    @Path("/rechazar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response rechazar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /slct/rechazar/" + id);
        try {
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            String motivo = (datos != null && datos.get("motivo") != null)
                    ? datos.get("motivo").toString() : null;
            SolicitudVacaciones resultado = SolicitudVacacionesService.rechazar(id, idUsuario, motivo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al rechazar la solicitud: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula la aprobacion de una solicitud: devuelve el saldo y anula la novedad que
     * la aprobacion habia creado. Rechaza si la novedad ya entro en un rol pagado.
     * Body: {"idUsuario": N, "motivo": "..." (obligatorio)}.
     */
    @POST
    @Path("/anularAprobacion/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anularAprobacion(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /slct/anularAprobacion/" + id);
        try {
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            String motivo = (datos != null && datos.get("motivo") != null)
                    ? datos.get("motivo").toString() : null;
            SolicitudVacaciones resultado = SolicitudVacacionesService.anularAprobacion(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Error al anular la aprobacion: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
