package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.saa.ejb.rhh.dao.DevolucionAnticipoDaoService;
import com.saa.ejb.rhh.service.DevolucionAnticipoService;
import com.saa.model.rhh.DevolucionAnticipo;
import com.saa.model.rhh.NombreEntidadesRhh;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para DevolucionAnticipo (RHH.DVAN). Base path: /dvan
 *
 * Endpoints (docs/logica-negocio/rhh/API-DEVOLUCION-ANTICIPO.md #5):
 *   POST /dvan/registrar               → captura la devolución, contabiliza y ajusta cuotas
 *   POST /dvan/anular/{id}             → deshace todo (ingreso, cuotas, saldos)
 *   GET  /dvan/porAnticipo/{idAnticipo} → devoluciones de un anticipo
 *   GET  /dvan/getId/{id}              → una devolución
 *
 * Sin getAll/PUT/DELETE genéricos a propósito: es el registro de un hecho
 * contable ya reversado por /anular, no una entidad de edición libre.
 */
@Path("dvan")
public class DevolucionAnticipoRest {

    @EJB
    private DevolucionAnticipoDaoService devolucionAnticipoDaoService;

    @EJB
    private DevolucionAnticipoService devolucionAnticipoService;

    public DevolucionAnticipoRest() {
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            DevolucionAnticipo entidad =
                    devolucionAnticipoDaoService.selectById(id, NombreEntidadesRhh.DEVOLUCION_ANTICIPO);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Devolución de anticipo con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la devolución del anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Devoluciones de un anticipo, más recientes primero.
     */
    @GET
    @Path("/porAnticipo/{idAnticipo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porAnticipo(@PathParam("idAnticipo") Long idAnticipo) {
        System.out.println("LLEGA AL SERVICIO GET /dvan/porAnticipo/" + idAnticipo);
        try {
            List<DevolucionAnticipo> resultado = devolucionAnticipoService.porAnticipo(idAnticipo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las devoluciones del anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Registra la devolución de un anticipo. Body esperado:
     * {
     *   "idAnticipo": 12, "fecha": "2026-09-05", "valor": 150.00,
     *   "idCuentaBancaria": 4, "referencia": "DEP 998877",
     *   "observacion": "Devuelve la cuota de septiembre", "idUsuario": 12
     * }
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dvan/registrar");
        try {
            Long idAnticipo = toLong(datos.get("idAnticipo"));
            LocalDate fecha = toFecha((String) datos.get("fecha"));
            Double valor = toDouble(datos.get("valor"));
            Long idCuentaBancaria = toLong(datos.get("idCuentaBancaria"));
            String referencia = (String) datos.get("referencia");
            String observacion = (String) datos.get("observacion");
            Long idUsuario = toLong(datos.get("idUsuario"));

            Map<String, Object> resultado = devolucionAnticipoService.registrar(idAnticipo, fecha, valor,
                    idCuentaBancaria, referencia, observacion, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la devolución del anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula la devolución: deshace ingreso, cuotas y saldos. Body: { "motivo": "...", "idUsuario": 12 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /dvan/anular/" + id);
        try {
            String motivo = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            Map<String, Object> resultado = devolucionAnticipoService.anular(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la devolución del anticipo: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── Helpers de conversión del JSON ───────────────────────────────────────

    private Long toLong(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).longValue();
        try {
            return Long.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double toDouble(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Number) return ((Number) valor).doubleValue();
        try {
            return Double.valueOf(valor.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate toFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
