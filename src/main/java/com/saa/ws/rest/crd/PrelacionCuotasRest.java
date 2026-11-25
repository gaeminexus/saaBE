package com.saa.ws.rest.crd;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.service.PrelacionCuotasService;
import com.saa.ejb.crd.service.dto.SolicitudReimputacion;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * C33 (cuadre Superintendencia): prelación de cuotas — «no se aplica dinero a una cuota si una anterior tiene saldo».
 *
 * <ul>
 * <li>{@code GET /rest/prelacioncuotas/control?corte=AAAA-MM-DD} — préstamos con la prelación rota al corte (solo
 * lectura; es el control que bloquea el cierre de cartera).</li>
 * <li>{@code POST /rest/prelacioncuotas/reimputar} {@code {idPrestamo, fecha, usuario, idEmpresa, simular}} —
 * re-imputa en orden un préstamo al corte.</li>
 * <li>{@code POST /rest/prelacioncuotas/reimputarAlCorte} {@code {fecha, usuario, idEmpresa, simular}} — el lote de
 * todos los préstamos del control, cada uno en su transacción (después de las restituciones SQL).</li>
 * </ul>
 * Ver {@code cuadreSuper/03-trabajo/cambios-saa/C33-CUOTA-PARCIAL-CON-POSTERIORES-PAGADAS.md} §7.
 */
@Path("prelacioncuotas")
public class PrelacionCuotasRest {

    @EJB
    private PrelacionCuotasService prelacionCuotasService;

    @EJB
    private com.saa.basico.ejb.UsuarioValidacionService usuarioValidacionService;

    @GET
    @Path("/control")
    @Produces(MediaType.APPLICATION_JSON)
    public Response control(@QueryParam("corte") String corte) {
        System.out.println("LLEGA AL SERVICIO CONTROL - PRELACION_CUOTAS - corte: " + corte);
        try {
            List<Map<String, Object>> filas = new ArrayList<>();
            for (Object[] f : prelacionCuotasService.controlAlCorte(LocalDate.parse(corte))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("idPrestamo", f[0]);
                m.put("operacion", f[1]);
                m.put("estadoPrestamo", f[2]);
                m.put("tipo", f[8]);
                m.put("cuota", f[3]);
                m.put("monto", f[4]);
                m.put("primeraPosteriorConPagos", f[5]);
                m.put("cuotas", f[6]);
                m.put("valor", f[7]);
                filas.add(m);
            }
            return Response.ok(filas, MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error("Error en el control de prelación de cuotas: ", e);
        }
    }

    @POST
    @Path("/reimputar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reimputar(SolicitudReimputacion solicitud) {
        System.out.println("LLEGA AL SERVICIO REIMPUTAR - PRELACION_CUOTAS - préstamo: "
            + (solicitud != null ? solicitud.getIdPrestamo() : null));
        try {
            String usuario = solicitud.esSimulacion() ? solicitud.getUsuario()
                : usuarioValidacionService.validar(solicitud.getUsuario());
            return Response.ok(prelacionCuotasService.reimputarPagosEnOrden(solicitud.getIdPrestamo(),
                fecha(solicitud), usuario, solicitud.getIdEmpresa(), solicitud.esSimulacion()),
                MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error("Error al re-imputar los pagos en orden: ", e);
        }
    }

    @POST
    @Path("/reimputarAlCorte")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reimputarAlCorte(SolicitudReimputacion solicitud) {
        System.out.println("LLEGA AL SERVICIO REIMPUTAR_AL_CORTE - PRELACION_CUOTAS - corte: "
            + (solicitud != null ? solicitud.getFecha() : null));
        try {
            if (solicitud.getFecha() == null) {
                throw new com.saa.basico.util.IncomeException("PARAMETRO_INVALIDO: fecha (corte) es obligatoria");
            }
            String usuario = solicitud.esSimulacion() ? solicitud.getUsuario()
                : usuarioValidacionService.validar(solicitud.getUsuario());
            return Response.ok(prelacionCuotasService.reimputarAlCorte(fecha(solicitud), usuario,
                solicitud.getIdEmpresa(), solicitud.esSimulacion()), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error("Error al re-imputar al corte: ", e);
        }
    }

    /**
     * POST /rest/prelacioncuotas/reclasificar {fecha (día de la corrida), usuario, idEmpresa, simular}: asiento de reclasificación por componente de una corrida
     * de re-imputación (uno por corrida). Simular primero. Pendiente de la decisión del contador antes de asentar; NO es idempotente: no repetir.
     */
    @POST
    @Path("/reclasificar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reclasificar(SolicitudReimputacion solicitud) {
        System.out.println("LLEGA AL SERVICIO RECLASIFICAR - PRELACION_CUOTAS - corrida: " + (solicitud != null ? solicitud.getFecha() : null));
        try {
            if (solicitud == null || solicitud.getFecha() == null) {
                throw new com.saa.basico.util.IncomeException("PARAMETRO_INVALIDO: fecha (día de la corrida) es obligatoria");
            }
            String usuario = solicitud.esSimulacion() ? solicitud.getUsuario()
                : usuarioValidacionService.validar(solicitud.getUsuario());
            return Response.ok(prelacionCuotasService.reclasificarCorrida(fecha(solicitud), fecha(solicitud), usuario,
                solicitud.getIdEmpresa(), solicitud.esSimulacion()), MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return error("Error al reclasificar: ", e);
        }
    }

    private static LocalDate fecha(SolicitudReimputacion s) {
        return s.getFecha() == null || s.getFecha().trim().isEmpty() ? null : LocalDate.parse(s.getFecha().trim());
    }

    private static Response error(String prefijo, Throwable e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(prefijo + e.getMessage())
            .type(MediaType.APPLICATION_JSON).build();
    }
}
