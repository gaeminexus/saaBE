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
 *
 * Endpoints principales:
 *   POST /antc/procesar          → registra el anticipo, genera el asiento y lo confirma
 *   POST /antc/confirmar         → confirma un anticipo Ingresado y genera su asiento
 *   GET  /antc/verificarAnulacion/{id} → dice si el anticipo puede anularse y si ya fue
 *                                  cruzado con facturas (consulta previa, no modifica nada)
 *   POST /antc/anular/{id}       → anula el anticipo, su asiento y (con confirmación) los
 *                                  abonos que hizo a facturas. Requiere motivo.
 *   GET  /antc/getAll            → todos los anticipos
 *   GET  /antc/getId/{id}        → anticipo por ID
 *   GET  /antc/getByTitular/{idTitular}/{idEmpresa}
 *   GET  /antc/disponibles/{idTitular}/{idEmpresa} → anticipos con saldo para cruzar
 *   GET  /antc/seguimiento/{idTitular}/{idEmpresa}  → estado de cuenta: anticipos, cruces y asientos
 *   POST /antc                   → guardar (sin asiento, estado=Ingresado)
 *   PUT  /antc                   → actualizar
 *   DELETE /antc/{id}            → anulación lógica sin aceptar reversión de cruces
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

    // ── GET /antc/verificarAnulacion/{id} ────────────────────────────────────
    /**
     * Consulta previa a la anulación: dice si el anticipo puede anularse y si
     * ya fue cruzado con facturas, con el detalle de esos cruces. NO modifica
     * nada — la pantalla la usa para preguntarle al usuario antes de anular.
     *
     * Respuesta:
     * <pre>
     * {
     *   "puedeAnular":          true,
     *   "requiereConfirmacion": true,          (true = el anticipo ya fue cruzado)
     *   "valorAnticipo":        500.00,
     *   "saldoDisponible":      200.00,
     *   "montoACruzar":         300.00,
     *   "cruces": [ { "idAplicacion":9, "idFactura":12, "numeroFactura":"001-001-000000123",
     *                 "montoAplicado":300.00, "fechaAplicacion":"2026-08-01" } ],
     *   "mensaje":              "..."
     * }
     * </pre>
     */
    @GET
    @Path("/verificarAnulacion/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response verificarAnulacion(@PathParam("id") Long id) {
        System.out.println("GET /antc/verificarAnulacion/" + id);
        try {
            java.util.Map<String, Object> resultado =
                    anticiPoClienteService.verificarAnulacion(id);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al verificar la anulación del anticipo: " + e.getMessage());
        }
    }

    // ── POST /antc/anular/{id} ───────────────────────────────────────────────
    /**
     * Anula un anticipo de cliente y revierte todo lo que generó.
     * <ul>
     *   <li>Anticipo Ingresado: no tiene asiento ni saldo acreditado, solo pasa
     *       a Anulado.</li>
     *   <li>Anticipo Confirmado: si ya fue cruzado con facturas responde
     *       {@code exito=false, requiereConfirmacion=true} con el detalle de los
     *       cruces; reenviando con {@code confirmarReversionCruces=true} se
     *       eliminan esos abonos (las facturas vuelven a quedar pendientes de
     *       cobro), se anula el asiento del anticipo y su movimiento bancario, y
     *       se descuenta el saldo de anticipos del cliente.</li>
     * </ul>
     * Body esperado:
     * { "motivo": "...", "idUsuario": 5, "confirmarReversionCruces": false }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, java.util.Map<String, Object> params) {
        System.out.println("POST /antc/anular/" + id);
        try {
            String motivo  = (params != null && params.get("motivo") != null)
                    ? params.get("motivo").toString() : null;
            Long idUsuario = (params != null) ? getLongParam(params, "idUsuario") : null;
            boolean confirmarCruces = (params != null)
                    && Boolean.parseBoolean(String.valueOf(
                            params.getOrDefault("confirmarReversionCruces", "false")));

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(msg("Debe indicar el motivo de la anulación."))
                        .type(MediaType.APPLICATION_JSON).build();
            }

            java.util.Map<String, Object> resultado = anticiPoClienteService.anularAnticipo(
                    id, motivo, idUsuario, confirmarCruces);
            // Cuando falta la confirmación de los cruces la respuesta sigue
            // siendo 200: no es un error, es la pregunta al usuario.
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al anular anticipo: " + e.getMessage());
        }
    }

    // ── DELETE /antc/{id} — anulación lógica (estado=3) ─────────────────────
    /**
     * Anulación sin motivo explícito. Pasa por la misma lógica que
     * POST /antc/anular/{id} pero sin aceptar la reversión de cruces: si el
     * anticipo ya fue cruzado con alguna factura, responde
     * {@code requiereConfirmacion=true} en lugar de anular.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        System.out.println("DELETE AnticipoCliente id=" + id);
        try {
            java.util.Map<String, Object> resultado = anticiPoClienteService.anularAnticipo(
                    id, "Anulación desde la interfaz (DELETE)", null, false);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
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

    // ── GET /antc/disponibles/{idTitular}/{idEmpresa} ────────────────────────
    /**
     * Anticipos del cliente que todavía tienen saldo para cruzar. Es la
     * lista que alimenta la pantalla de cruce: el usuario elige de aquí de qué
     * anticipo sale el dinero de cada abono.
     */
    @GET
    @Path("/disponibles/{idTitular}/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response disponibles(@PathParam("idTitular") Long idTitular,
            @PathParam("idEmpresa") Long idEmpresa) {
        System.out.println("GET /antc/disponibles/" + idTitular + "/" + idEmpresa);
        try {
            List<AnticipoCliente> lista =
                    anticiPoClienteService.selectDisponibles(idTitular, idEmpresa);
            return Response.ok(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener los anticipos disponibles: " + e.getMessage());
        }
    }

    // ── GET /antc/seguimiento/{idTitular}/{idEmpresa} ────────────────────────
    /**
     * Estado de cuenta de los anticipos de un cliente: cada anticipo con sus
     * fechas, su documento, su asiento y el detalle de los cruces que lo
     * consumieron — activos y reversados, para poder seguir también las
     * anulaciones.
     *
     * Respuesta:
     * <pre>
     * {
     *   "totalAnticipos": 5000.00, "totalCruzado": 4500.00,
     *   "saldoDisponible": 500.00, "saldoGlobalAnticipos": 500.00,
     *   "diferencia": 0.00, "cuadra": true,
     *   "anticipos": [ { "id":7, "numeroDoc":"ANT-001", "fechaAnticipo":[2026,8,1],
     *                    "valor":3000.00, "saldo":0.00, "estadoDescripcion":"Confirmado",
     *                    "asiento": { "numeroAlterno":"...", "fechaAsiento":[...] },
     *                    "totalCruzado": 3000.00,
     *                    "cruces": [ { "idAplicacion":9, "numeroFactura":"001-001-000000123",
     *                                  "montoAplicado":3000.00, "estadoDescripcion":"Activo",
     *                                  "asiento": {...} } ] } ]
     * }
     * </pre>
     */
    @GET
    @Path("/seguimiento/{idTitular}/{idEmpresa}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response seguimiento(@PathParam("idTitular") Long idTitular,
            @PathParam("idEmpresa") Long idEmpresa) {
        System.out.println("GET /antc/seguimiento/" + idTitular + "/" + idEmpresa);
        try {
            java.util.Map<String, Object> resultado =
                    anticiPoClienteService.seguimiento(idTitular, idEmpresa);
            return Response.ok(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error500("Error al obtener el seguimiento de anticipos: " + e.getMessage());
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
