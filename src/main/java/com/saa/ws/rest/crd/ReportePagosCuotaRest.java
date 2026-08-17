package com.saa.ws.rest.crd;

import java.util.HashMap;
import java.util.Map;

import com.saa.ejb.reporte.service.ReporteService;

import jakarta.ejb.EJB;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Comprobante de pagos de una cuota (CRD).
 *
 * Genera en JasperReports el mismo comprobante que hoy imprime el frontend
 * desde {@code PrestamoPagosDialogComponent}, pero como PDF corporativo con el
 * logo del fondo (ASOPREP-FCPC).
 *
 * Reporte: {@code /rep/crd/RPRT_CMPB_PGCT.jrxml}
 * Spec:    docs/logica-negocio/crd/ESPECIFICACION-REPORTE-JASPER-COMPROBANTE-PAGOS-CUOTA.md
 */
@Path("crd/reportePagosCuota")
public class ReportePagosCuotaRest {

    private static final String MODULO = "crd";
    private static final String REPORTE = "RPRT_CMPB_PGCT";

    @EJB
    private ReporteService reporteService;

    /**
     * GET /rest/crd/reportePagosCuota/{dtprCodigo}
     *
     * @param dtprCodigo Código de la cuota (CRD.DTPR.DTPRCDGO).
     * @param usuario    Usuario que genera el reporte (opcional, query param).
     * @return PDF del comprobante ({@code application/pdf}).
     */
    @GET
    @Path("/{dtprCodigo}")
    @Produces("application/pdf")
    public Response generar(@PathParam("dtprCodigo") Long dtprCodigo,
                            @QueryParam("usuario") @DefaultValue("") String usuario) {
        System.out.println("LLEGA AL SERVICIO reportePagosCuota - dtprCodigo: " + dtprCodigo);
        try {
            if (dtprCodigo == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("El código de la cuota (dtprCodigo) es requerido")
                        .type(MediaType.TEXT_PLAIN).build();
            }

            Map<String, Object> parametros = new HashMap<>();
            parametros.put("P_DTPR_CODIGO", dtprCodigo);
            if (usuario != null && !usuario.trim().isEmpty()) {
                parametros.put("P_USUARIO", usuario.trim());
            }
            // El logo (P_IMAGEN) lo inyecta ReporteServiceImpl si no viene en los parámetros.

            byte[] pdf = reporteService.generarReporte(MODULO, REPORTE, parametros, "PDF");

            return Response.ok(pdf, "application/pdf")
                    .header("Content-Disposition",
                            "inline; filename=\"comprobante-pagos-cuota-" + dtprCodigo + ".pdf\"")
                    .build();

        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar el comprobante de pagos de la cuota: " + e.getMessage())
                    .type(MediaType.TEXT_PLAIN).build();
        }
    }
}
