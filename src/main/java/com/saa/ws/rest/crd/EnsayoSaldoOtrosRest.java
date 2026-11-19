package com.saa.ws.rest.crd;

import java.util.LinkedHashMap;
import java.util.Map;

import com.saa.ejb.crd.service.EnsayoSaldoOtrosService;
import com.saa.ejb.crd.service.dto.SolicitudEnsayoSaldoOtros;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * C35 — {@code POST /rest/c35ensayo}: ensayo SIN EFECTO de la regla de {@code DTPRSLOT} sobre un préstamo real (la
 * transacción se revierte siempre). Ver {@link EnsayoSaldoOtrosService} y {@link SolicitudEnsayoSaldoOtros}.
 */
@Path("c35ensayo")
public class EnsayoSaldoOtrosRest {

    @EJB
    private EnsayoSaldoOtrosService ensayoSaldoOtrosService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response ensayar(SolicitudEnsayoSaldoOtros solicitud) {
        System.out.println("LLEGA AL SERVICIO ENSAYO C35 - Préstamo: " + (solicitud != null ? solicitud.getIdPrestamo() : null));
        try {
            return Response.status(Response.Status.OK).entity(ensayoSaldoOtrosService.ensayar(solicitud))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            Map<String, Object> cuerpo = new LinkedHashMap<>();
            cuerpo.put("exito", Boolean.FALSE);
            cuerpo.put("revertido", Boolean.TRUE);
            cuerpo.put("mensaje", "Error en el ensayo C35: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(cuerpo)
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }
}
