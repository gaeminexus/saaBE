package com.saa.ws.rest.cxp;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.cxp.service.AnticipoProveedorService;
import com.saa.model.cxp.AnticipoProveedor;

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
 * REST para Anticipos a Proveedores.
 * Base path: /antp
 *
 * Endpoints principales:
 *   POST /antp/procesar          → registra el anticipo y crea su pago en el circuito de PGTR;
 *                                  la contabilidad se genera al confirmarse el pago (asiento
 *                                  de ANTICIPO, no de egreso). Con débito automático todo
 *                                  ocurre en el mismo paso.
 *   POST /antp/anular/{id}       → anula un anticipo pendiente (requiere motivo)
 *   GET  /antp/getAll            → todos los anticipos
 *   GET  /antp/getId/{id}        → anticipo por ID
 *   GET  /antp/getByTitular/{idTitular}/{idEmpresa}
 *   POST /antp                   → guardar (sin asiento, estado=Ingresado)
 *   PUT  /antp                   → actualizar
 *   DELETE /antp/{id}            → anulación lógica (estado=3)
 */
@Path("antp")
public class AnticipoProveedorRest {

    @EJB
    private AnticipoProveedorService anticiPoProveedorService;

    @Context
    private UriInfo context;

    public AnticipoProveedorRest() {}

    // ── POST /antp/procesar ───────────────────────────────────────────────────
    /**
     * Registra un anticipo a proveedor y crea su pago en el circuito de pagos
     * (PGS.PGTR). Sin débito automático el anticipo queda Ingresado y su pago
     * Registrado (listado de pagos a realizar → lote → archivo → confirmación);
     * la contabilidad se genera recién al confirmarse el pago. Con débito
     * automático todo ocurre en este mismo paso.
     *
     * Body JSON esperado:
     * <pre>
     * {
     *   "idTitular":              123,
     *   "valor":                  500.00,
     *   "idCuentaBancaria":       5,
     *   "idEmpresa":              1,
     *   "idUsuario":              10,
     *   "fechaAnticipo":          "2026-07-31",
     *   "idCuentaDestinoTitular": 8,           (cuenta del proveedor; obligatoria salvo débito automático)
     *   "debitoAutomatico":       false,       (opcional, default false)
     *   "numeroDoc":              "REF-001",   (opcional)
     *   "observacion":            "..."        (opcional)
     * }
     * </pre>
     *
     * Asiento contable (al confirmarse el pago):
     *   DEBE:  Cuenta anticipos del rol proveedor del titular
     *   HABER: PlanCuenta asociado a la cuenta bancaria
     */
    @POST
    @Path("/procesar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesar(Map<String, Object> params) {
        System.out.println("POST /antp/procesar");
        try {
            Long idTitular              = getLong(params, "idTitular");
            Double valor                = getDouble(params, "valor");
            Long idCuentaBancaria       = getLong(params, "idCuentaBancaria");
            Long idEmpresa              = getLong(params, "idEmpresa");
            Long idUsuario              = getLong(params, "idUsuario");
            String fechaAnticipo        = getString(params, "fechaAnticipo");
            String numeroDoc            = getString(params, "numeroDoc");
            String observacion          = getString(params, "observacion");
            Long idCuentaDestinoTitular = getLong(params, "idCuentaDestinoTitular");
            boolean debitoAutomatico    = getBoolean(params, "debitoAutomatico");

            Map<String, Object> resultado = anticiPoProveedorService.procesarAnticipo(
                    idTitular, valor, idCuentaBancaria,
                    idEmpresa, idUsuario, fechaAnticipo,
                    numeroDoc, observacion,
                    idCuentaDestinoTitular, debitoAutomatico);

            boolean exito = Boolean.TRUE.equals(resultado.get("exito"));
            return Response.status(exito ? Response.Status.CREATED : Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();

        } catch (Throwable e) {
            return error500("Error al procesar anticipo a proveedor: " + e.getMessage());
        }
    }

    // ── POST /antp/anular/{id} ───────────────────────────────────────────────
    /**
     * Anula un anticipo pendiente de pago. Si tiene un pago Registrado lo
     * anula también; si el pago está En archivo o Confirmado, la anulación se
     * bloquea (procesar la respuesta del banco o revertir el pago primero).
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> params) {
        System.out.println("POST /antp/anular/" + id);
        try {
            String motivo  = (params != null) ? getString(params, "motivo") : null;
            Long idUsuario = (params != null) ? getLong(params, "idUsuario") : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(msg("Debe indicar el motivo de la anulación."))
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado =
                    anticiPoProveedorService.anularAnticipo(id, motivo, idUsuario);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al anular anticipo: " + e.getMessage());
        }
    }

    // ── GET /antp/getAll ─────────────────────────────────────────────────────
    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<AnticipoProveedor> lista = anticiPoProveedorService.selectAll();
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener anticipos a proveedores: " + e.getMessage());
        }
    }

    // ── GET /antp/getId/{id} ─────────────────────────────────────────────────
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            AnticipoProveedor entidad = anticiPoProveedorService.selectById(id);
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

    // ── GET /antp/getByTitular/{idTitular}/{idEmpresa} ───────────────────────
    @GET
    @Path("/getByTitular/{idTitular}/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getByTitular(
            @PathParam("idTitular") Long idTitular,
            @PathParam("idEmpresa") Long idEmpresa) {
        try {
            List<AnticipoProveedor> lista =
                    anticiPoProveedorService.selectByTitularEmpresa(idTitular, idEmpresa);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener anticipos del proveedor: " + e.getMessage());
        }
    }

    // ── POST /antp  (crear sin asiento — estado Ingresado) ───────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(AnticipoProveedor registro) {
        System.out.println("POST AnticipoProveedor");
        try {
            AnticipoProveedor resultado = anticiPoProveedorService.saveSingle(registro);
            return Response.status(Response.Status.CREATED)
                    .entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al crear anticipo: " + e.getMessage());
        }
    }

    // ── PUT /antp  (actualizar) ──────────────────────────────────────────────
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(AnticipoProveedor registro) {
        System.out.println("PUT AnticipoProveedor");
        try {
            AnticipoProveedor resultado = anticiPoProveedorService.saveSingle(registro);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al actualizar anticipo: " + e.getMessage());
        }
    }

    // ── DELETE /antp/{id} — anulación lógica (estado=3) ─────────────────────
    /**
     * Anulación sin motivo explícito. Pasa por la misma validación que
     * POST /antp/anular/{id}: anula el pago Registrado del circuito si lo hay,
     * y se bloquea si el pago ya está En archivo o Confirmado.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE AnticipoProveedor id=" + id);
        try {
            Map<String, Object> resultado = anticiPoProveedorService.anularAnticipo(
                    id, "Anulación desde la interfaz (DELETE)", null);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al anular anticipo: " + e.getMessage());
        }
    }

    // ── POST /antp/selectByCriteria ──────────────────────────────────────────
    @POST
    @Path("/selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> datos) {
        try {
            List<AnticipoProveedor> result = anticiPoProveedorService.selectByCriteria(datos);
            return Response.ok(result).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error en búsqueda de anticipos: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long getLong(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Long)    return (Long) v;
        if (v instanceof Integer) return ((Integer) v).longValue();
        if (v instanceof String)  return Long.parseLong((String) v);
        return null;
    }

    private Double getDouble(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return null;
        if (v instanceof Double)  return (Double) v;
        if (v instanceof Integer) return ((Integer) v).doubleValue();
        if (v instanceof Long)    return ((Long) v).doubleValue();
        if (v instanceof String)  return Double.parseDouble((String) v);
        return null;
    }

    private String getString(Map<String, Object> params, String key) {
        Object v = params.get(key);
        return v != null ? v.toString() : null;
    }

    private boolean getBoolean(Map<String, Object> params, String key) {
        Object v = params.get(key);
        if (v == null) return false;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString());
    }

    private Response error500(String mensaje) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("error", mensaje);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body).type(MediaType.APPLICATION_JSON).build();
    }

    private Map<String, String> msg(String mensaje) {
        Map<String, String> m = new java.util.HashMap<>();
        m.put("mensaje", mensaje);
        return m;
    }
}
