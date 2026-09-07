package com.saa.ws.rest.rhh;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.saa.ejb.rhh.dao.PlanillaIessDaoService;
import com.saa.ejb.rhh.service.PlanillaIessService;
import com.saa.model.rhh.DetallePlanillaIess;
import com.saa.model.rhh.NombreEntidadesRhh;
import com.saa.model.rhh.PeriodoNomina;
import com.saa.model.rhh.PlanillaIess;
import com.saa.model.scp.Empresa;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST para PlanillaIess (RHH.PLIS). Base path: /plis
 *
 * Endpoints (Fase 1 -- docs/logica-negocio/rhh/API-PLANILLA-IESS.md #5):
 *   GET  /plis/getAll             → estándar
 *   GET  /plis/getId/{id}         → con sus renglones
 *   GET  /plis/porPeriodo/{id}    → las planillas de un período (hasta cuatro)
 *   POST /plis/registrar          → captura la planilla del portal
 *   POST /plis/conciliar/{id}     → enfrenta contra la planilla de control
 *   POST /plis/anular/{id}        → sólo en estado 1 o 2
 *   PUT  /plis                    → edición estándar
 *   DELETE /plis/{id}             → estándar
 */
@Path("plis")
public class PlanillaIessRest {

    @EJB
    private PlanillaIessDaoService planillaIessDaoService;

    @EJB
    private PlanillaIessService planillaIessService;

    public PlanillaIessRest() {
    }

    @GET
    @Path("/getAll")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAll() {
        try {
            List<PlanillaIess> lista = planillaIessDaoService.selectAll(NombreEntidadesRhh.PLANILLA_IESS);
            return Response.status(Response.Status.OK).entity(lista).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener planillas del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Recupera la planilla con sus renglones ya cargados.
     */
    @GET
    @Path("/getId/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getId(@PathParam("id") Long id) {
        try {
            PlanillaIess planilla = planillaIessService.selectById(id);
            if (planilla == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Planilla del IESS con ID " + id + " no encontrada")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.OK).entity(planilla).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener la planilla del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Planillas de un período (hasta cuatro: una por tipo).
     */
    @GET
    @Path("/porPeriodo/{idPeriodo}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porPeriodo(@PathParam("idPeriodo") Long idPeriodo) {
        System.out.println("LLEGA AL SERVICIO GET /plis/porPeriodo/" + idPeriodo);
        try {
            List<PlanillaIess> resultado = planillaIessService.porPeriodo(idPeriodo);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al obtener las planillas del período: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Captura la planilla que emitió el portal, con sus renglones.
     * Body esperado:
     * {
     *   "idEmpresa": 1236, "idPeriodo": 42, "tipo": 1,
     *   "numeroComprobante": "2026090012345",
     *   "fechaEmision": "2026-09-05", "fechaMaximaPago": "2026-09-15",
     *   "valorIess": 12345.67,
     *   "renglones": [ { "concepto": "APORTE PERSONAL", "conceptoTipo": 1, "valorIess": 4000.00 } ],
     *   "idUsuario": 12
     * }
     */
    @POST
    @Path("/registrar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registrar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /plis/registrar");
        try {
            Long idEmpresa = toLong(datos.get("idEmpresa"));
            Long idPeriodo = toLong(datos.get("idPeriodo"));
            Long tipo = toLong(datos.get("tipo"));
            String numeroComprobante = (String) datos.get("numeroComprobante");
            LocalDate fechaEmision = toFecha((String) datos.get("fechaEmision"));
            LocalDate fechaMaximaPago = toFecha((String) datos.get("fechaMaximaPago"));
            Double valorIess = toDouble(datos.get("valorIess"));
            String observacion = (String) datos.get("observacion");
            Long idUsuario = toLong(datos.get("idUsuario"));

            Empresa empresa = new Empresa();
            empresa.setCodigo(idEmpresa);
            PeriodoNomina periodo = new PeriodoNomina();
            periodo.setCodigo(idPeriodo);

            PlanillaIess planilla = new PlanillaIess();
            planilla.setEmpresa(empresa);
            planilla.setPeriodo(periodo);
            planilla.setTipo(tipo);
            planilla.setNumeroComprobante(numeroComprobante);
            planilla.setFechaEmision(fechaEmision);
            planilla.setFechaMaximaPago(fechaMaximaPago);
            planilla.setValorIess(valorIess);
            planilla.setObservacion(observacion);

            List<DetallePlanillaIess> renglones = new ArrayList<>();
            Object rawRenglones = datos.get("renglones");
            if (rawRenglones instanceof List) {
                for (Object item : (List<?>) rawRenglones) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> fila = (Map<String, Object>) item;
                        DetallePlanillaIess renglon = new DetallePlanillaIess();
                        renglon.setConcepto((String) fila.get("concepto"));
                        renglon.setConceptoTipo(toLong(fila.get("conceptoTipo")));
                        renglon.setValorIess(toDouble(fila.get("valorIess")));
                        renglones.add(renglon);
                    }
                }
            }

            PlanillaIess resultado = planillaIessService.registrar(planilla, renglones, idUsuario);
            return Response.status(Response.Status.CREATED).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al registrar la planilla del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Enfrenta la planilla contra la planilla de control del período.
     * Body: { "idUsuario": 12 }
     */
    @POST
    @Path("/conciliar/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response conciliar(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /plis/conciliar/" + id);
        try {
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            Map<String, Object> resultado = planillaIessService.conciliar(id, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al conciliar la planilla del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * Anula la planilla. Sólo en estado 1 (Registrada) o 2 (Conciliada).
     * Body: { "motivo": "...", "idUsuario": 12 }
     */
    @POST
    @Path("/anular/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response anular(@PathParam("id") Long id, Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /plis/anular/" + id);
        try {
            String motivo = (datos != null) ? (String) datos.get("motivo") : null;
            Long idUsuario = (datos != null) ? toLong(datos.get("idUsuario")) : null;
            PlanillaIess resultado = planillaIessService.anular(id, motivo, idUsuario);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al anular la planilla del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response put(PlanillaIess registro) {
        try {
            PlanillaIess resultado = planillaIessService.saveSingle(registro);
            return Response.status(Response.Status.OK).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al actualizar la planilla del IESS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") Long id) {
        try {
            PlanillaIess elimina = new PlanillaIess();
            planillaIessDaoService.remove(elimina, id);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al eliminar la planilla del IESS: " + e.getMessage())
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
