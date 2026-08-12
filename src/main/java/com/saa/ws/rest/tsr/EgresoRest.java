package com.saa.ws.rest.tsr;

import java.util.List;
import java.util.Map;

import com.saa.basico.util.DatosBusqueda;
import com.saa.ejb.tsr.service.EgresoService;
import com.saa.model.tsr.Egreso;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * REST para Egresos de tesorería sin documento físico (TSR.EGRS).
 * Base path: /egrs
 *
 * Endpoints principales:
 *   POST /egrs/procesar        → registra el egreso y crea su pago en el circuito de PGTR
 *                                (con debitoAutomatico: true contabiliza de una vez)
 *   POST /egrs/anular/{id}     → anula un egreso pendiente (requiere motivo)
 *   GET  /egrs/listar          → egresos por empresa y estado
 *   GET  /egrs/getAll          → todos los egresos
 *   GET  /egrs/getId/{id}      → egreso por ID
 *   POST /egrs/selectByCriteria → búsqueda por criterios
 *
 * El pago del egreso se sigue y opera desde /pgtr (listado de pagos a
 * realizar, lote, respuesta del banco, reversión).
 */
@Path("egrs")
public class EgresoRest {

    @EJB
    private EgresoService egresoService;

    @Context
    private UriInfo context;

    public EgresoRest() {
    }

    /**
     * Registra un egreso y su pago en un solo paso.
     * Body esperado:
     * {
     *   "idEmpresa": 1,
     *   "idTitular": 25,                  (obligatorio si no es débito automático)
     *   "idProductoPago": 7,              (producto CXP; su grupo da la cuenta contable)
     *   "descripcion": "Administración cuenta corriente agosto",
     *   "valor": 12.50,
     *   "fecha": "2026-08-12",
     *   "idCuentaBancariaOrigen": 4,
     *   "idCuentaDestinoTitular": 9,      (obligatorio si no es débito automático)
     *   "debitoAutomatico": true,
     *   "referencia": "DEB-ADM-0812",     (opcional)
     *   "observacion": "...",             (opcional)
     *   "idUsuario": 5
     * }
     */
    @POST
    @Path("/procesar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response procesar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /egrs/procesar");
        try {
            Long idEmpresa      = toLong(datos.get("idEmpresa"));
            Long idTitular      = toLong(datos.get("idTitular"));
            Long idProducto     = toLong(datos.get("idProductoPago"));
            String descripcion  = (String) datos.get("descripcion");
            Double valor        = toDouble(datos.get("valor"));
            String fecha        = (String) datos.get("fecha");
            Long idCuentaOrigen = toLong(datos.get("idCuentaBancariaOrigen"));
            Long idCuentaDest   = toLong(datos.get("idCuentaDestinoTitular"));
            boolean debitoAut   = toBoolean(datos.get("debitoAutomatico"));
            String referencia   = (String) datos.get("referencia");
            String observacion  = (String) datos.get("observacion");
            Long idUsuario      = toLong(datos.get("idUsuario"));

            if (idEmpresa == null || idProducto == null || valor == null || idCuentaOrigen == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa, idProductoPago, valor e idCuentaBancariaOrigen.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = egresoService.procesarEgreso(idEmpresa, idTitular,
                    idProducto, descripcion, valor, fecha, idCuentaOrigen, idCuentaDest,
                    debitoAut, referencia, observacion, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al procesar el egreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula un egreso pendiente de pago.
     * Body esperado: { "motivo": "...", "idUsuario": 5 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /egrs/anular/" + id);
        try {
            String motivo  = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;

            if (motivo == null || motivo.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe indicar el motivo de la anulación.")
                        .type(MediaType.APPLICATION_JSON).build();
            }

            Map<String, Object> resultado = egresoService.anularEgreso(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular el egreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Listado de egresos por empresa, opcionalmente por estado.
     * @param idEmpresa : Id de la empresa (obligatorio)
     * @param estado    : 1=Pendiente 2=Pagado 3=Anulado (opcional)
     */
    @GET
    @Path("/listar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listar(@QueryParam("idEmpresa") Long idEmpresa,
            @QueryParam("estado") Long estado) {
        System.out.println("LLEGA AL SERVICIO GET /egrs/listar");
        try {
            if (idEmpresa == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idEmpresa.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            List<Egreso> lista = egresoService.listar(idEmpresa, estado);
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al listar los egresos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<Egreso> lista = egresoService.selectAll();
            return Response.status(Response.Status.OK).entity(lista)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener los egresos: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            Egreso egreso = egresoService.selectById(id);
            if (egreso == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Egreso con ID " + id + " no encontrado")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(egreso)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener el egreso: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @POST
    @Path("selectByCriteria")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response selectByCriteria(List<DatosBusqueda> registros) {
        System.out.println("selectByCriteria Egreso");
        try {
            return Response.status(Response.Status.OK)
                    .entity(egresoService.selectByCriteria(registros))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en selectByCriteria Egreso: " + e.getMessage())
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

    private boolean toBoolean(Object valor) {
        if (valor == null) return false;
        if (valor instanceof Boolean) return ((Boolean) valor).booleanValue();
        if (valor instanceof Number) return ((Number) valor).intValue() == 1;
        String texto = valor.toString().trim();
        return "true".equalsIgnoreCase(texto) || "1".equals(texto);
    }
}
