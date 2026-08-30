package com.saa.ws.rest.sri;

import java.util.Map;

import com.saa.ejb.sri.service.GeneradorAtsService;
import com.saa.ejb.sri.service.dto.ResultadoGeneracionAts;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * ATS (Anexo Transaccional Simplificado) — Fase 4, ver
 * docs/logica-negocio/sri/LEVANTAMIENTO-ATS-103-104.md §10 para el contrato completo y las
 * limitaciones (campos sin catálogo verificado, `&lt;anulados&gt;` sin distinguir origen SRI).
 */
@Path("ats")
public class AtsRest {

    @EJB
    private GeneradorAtsService generadorAtsService;

    /**
     * Genera el ZIP del ATS de un período.
     * Body: {"idFacturador": 1, "anio": 2026, "mes": 8}
     * Respuesta 200: ver {@link ResultadoGeneracionAts} — el ZIP viaja en `contenidoBase64`.
     * Revisar siempre `avisos` antes de enviar el archivo al SRI.
     */
    @POST
    @Path("/generar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generar(Map<String, Object> datos) {
        System.out.println("LLEGA AL SERVICIO POST /ats/generar");
        try {
            Long idFacturador = toLong(datos != null ? datos.get("idFacturador") : null);
            Integer anio = toInt(datos != null ? datos.get("anio") : null);
            Integer mes = toInt(datos != null ? datos.get("mes") : null);
            if (idFacturador == null || anio == null || mes == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Debe enviar idFacturador, anio y mes.")
                        .type(MediaType.APPLICATION_JSON).build();
            }
            ResultadoGeneracionAts resultado = generadorAtsService.generarAts(idFacturador, anio, mes);
            return Response.status(Response.Status.OK).entity(resultado)
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (com.saa.basico.util.IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error al generar el ATS: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private Long toLong(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).longValue();
        }
        return Long.valueOf(valor.toString());
    }

    private Integer toInt(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof Number) {
            return ((Number) valor).intValue();
        }
        return Integer.valueOf(valor.toString());
    }
}
