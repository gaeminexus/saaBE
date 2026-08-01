package com.saa.ws.rest.cxc;

import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxc.service.AnticipoClienteService;
import com.saa.model.cxc.AnticipoCliente;

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
 * REST para Anticipos de Clientes.
 * Base path: /antc
 */
@Path("antc")
public class AnticipoClienteRest {

    @EJB
    private AnticipoClienteService anticiPoClienteService;

    @Context
    private UriInfo context;

    public AnticipoClienteRest() {}

    // ── GET /antc/getAll ─────────────────────────────────────────────────────
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AnticipoCliente> lista = anticiPoClienteService.selectAll();
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener anticipos: " + e.getMessage());
        }
    }

    // ── GET /antc/getId/{id} ─────────────────────────────────────────────────
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AnticipoCliente entidad = anticiPoClienteService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(msg("Anticipo con ID " + id + " no encontrado."))
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.ok(entidad).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener anticipo: " + e.getMessage());
        }
    }

    // ── GET /antc/getByTitular/{idTitular}/{idEmpresa} ───────────────────────
    /**
     * Devuelve todos los anticipos activos de un cliente en una empresa.
     */
    @GET
    @Path("/getByTitular/{idTitular}/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByTitular(
            @PathParam("idTitular") Long idTitular,
            @PathParam("idEmpresa") Long idEmpresa) {
        try {
            List<AnticipoCliente> lista =
                    anticiPoClienteService.selectByTitularEmpresa(idTitular, idEmpresa);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener anticipos del cliente: " + e.getMessage());
        }
    }

    // ── POST /antc  (crear) ──────────────────────────────────────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AnticipoCliente registro) {
        System.out.println("POST AnticipoCliente");
        try {
            AnticipoCliente resultado = anticiPoClienteService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al crear anticipo: " + e.getMessage());
        }
    }

    // ── PUT /antc  (actualizar) ──────────────────────────────────────────────
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AnticipoCliente registro) {
        System.out.println("PUT AnticipoCliente");
        try {
            AnticipoCliente resultado = anticiPoClienteService.saveSingle(registro);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al actualizar anticipo: " + e.getMessage());
        }
    }

    // ── DELETE /antc/{id} — anulación lógica (estado=3) ─────────────────────
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE AnticipoCliente id=" + id);
        try {
            AnticipoCliente entidad = anticiPoClienteService.selectById(id);
            if (entidad == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(msg("Anticipo con ID " + id + " no encontrado."))
                        .type(MediaType.APPLICATION_JSON).build();
            }
            if (Long.valueOf(2L).equals(entidad.getEstado())) {
                return Response.status(422)
                        .entity(msg("No se puede anular un anticipo CONFIRMADO. "
                                + "Debe reversar el asiento contable primero."))
                        .type(MediaType.APPLICATION_JSON).build();
            }
            entidad.setEstado(3L); // 3 = Anulado
            anticiPoClienteService.saveSingle(entidad);
            return Response.ok(msg("Anticipo anulado correctamente."))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al anular anticipo: " + e.getMessage());
        }
    }

    // ── POST /antc/confirmar — confirmar anticipo y generar asiento ──────────
    /**
     * Confirma un anticipo en estado Ingresado (1), lo pasa a Confirmado (2)
     * y genera el asiento contable.
     *
     * Body JSON: { "idAnticipo": 123, "usuario": "jperez" }
     */
    @POST
    @Path("/confirmar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response confirmarAnticipo(java.util.Map<String, Object> params) {
        System.out.println("LLEGA AL SERVICIO confirmarAnticipo");
        try {
            Long idAnticipo = getLongParam(params, "idAnticipo");
            String usuario  = (String) params.get("usuario");

            if (idAnticipo == null) {
                java.util.Map<String, Object> err = new java.util.HashMap<>();
                err.put("exito", false);
                err.put("mensaje", "El parámetro 'idAnticipo' es obligatorio.");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(err).type(MediaType.APPLICATION_JSON).build();
            }

            java.util.Map<String, Object> resultado =
                    anticiPoClienteService.confirmarAnticipo(idAnticipo, usuario);

            boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
            String estado  = (String) resultado.getOrDefault("estado", "");

            if (exito || "YA_CONFIRMADO".equals(estado)) {
                return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
            } else {
                // Error de negocio (cuentas no configuradas, período cerrado, etc.) → 422
                return Response.status(422)
                        .entity(resultado).type(MediaType.APPLICATION_JSON).build();
            }

        } catch (Throwable e) {
            System.err.println("ERROR en confirmarAnticipo REST: " + e.getMessage());
            e.printStackTrace();
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("exito", false);
            err.put("mensaje", "Error inesperado al confirmar el anticipo: " + e.getMessage());
            err.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(err).type(MediaType.APPLICATION_JSON).build();
        }
    }

    // ── POST /antc/procesar ───────────────────────────────────────────────────
    /**
     * Procesa un anticipo de cliente en un único paso:
     * graba el registro, genera el asiento contable y confirma (estado=2).
     *
     * Body JSON esperado:
     * <pre>
     * {
     *   "idTitular":        123,
     *   "valor":            500.00,
     *   "idCuentaBancaria": 5,
     *   "idEmpresa":        1,
     *   "idUsuario":        10,
     *   "fechaAnticipo":    "2026-07-31",
     *   "numeroDoc":        "REF-001",     (opcional)
     *   "observacion":      "..."          (opcional)
     * }
     * </pre>
     *
     * Asiento contable:
     *   DEBE:  PlanCuenta asociado a la cuenta bancaria
     *   HABER: Cuenta anticipos del rol cliente del titular
     */
    @POST
    @Path("/procesar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesar(java.util.Map<String, Object> params) {
        System.out.println("POST /antc/procesar");
        try {
            Long idTitular        = getLongParam(params, "idTitular");
            Double valor          = getDoubleParam(params, "valor");
            Long idCuentaBancaria = getLongParam(params, "idCuentaBancaria");
            Long idEmpresa        = getLongParam(params, "idEmpresa");
            Long idUsuario        = getLongParam(params, "idUsuario");
            String fechaAnticipo  = params.get("fechaAnticipo") != null ? params.get("fechaAnticipo").toString() : null;
            String numeroDoc      = params.get("numeroDoc")     != null ? params.get("numeroDoc").toString()     : null;
            String observacion    = params.get("observacion")   != null ? params.get("observacion").toString()   : null;

            java.util.Map<String, Object> resultado = anticiPoClienteService.procesarAnticipo(
                    idTitular, valor, idCuentaBancaria,
                    idEmpresa, idUsuario, fechaAnticipo,
                    numeroDoc, observacion);

            boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
            return Response.status(exito ? Response.Status.CREATED : Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return error500("Error al procesar anticipo de cliente: " + e.getMessage());
        }
    }

    // ── POST /antc/selectByCriteria ──────────────────────────────────────────
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        System.out.println("selectByCriteria AnticipoCliente");
        try {
            List<AnticipoCliente> result = anticiPoClienteService.selectByCriteria(datos);
            return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error en búsqueda de anticipos: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long getLongParam(java.util.Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Long)    return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String)  return Long.parseLong((String) value);
        return null;
    }

    private Double getDoubleParam(java.util.Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Double)  return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Long)    return ((Long) value).doubleValue();
        if (value instanceof String)  return Double.parseDouble((String) value);
        return null;
    }

    private Response error500(String mensaje) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("error", mensaje);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    private java.util.Map<String, String> msg(String mensaje) {
        java.util.Map<String, String> m = new java.util.HashMap<>();
        m.put("mensaje", mensaje);
        return m;
    }
}
