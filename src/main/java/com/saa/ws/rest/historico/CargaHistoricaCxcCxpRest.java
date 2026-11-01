package com.saa.ws.rest.historico;

import com.saa.ejb.historico.service.CargaHistoricaCxcCxpService;
import com.saa.ejb.historico.service.CargaHistoricaNominaService;
import com.saa.model.historico.LoteHistoricoCxcCxp;
import com.saa.model.historico.LoteHistoricoNomina;
import com.saa.model.historico.ResultadoCargaHistorica;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Carga histórica de CxC/CxP (C11, cuadre Superintendencia). Base: {@code /SaaBE/rest/historico}.
 * Ver docs/logica-negocio/cxp/CARGA-HISTORICA-CXC-CXP.md.
 */
@Path("historico")
public class CargaHistoricaCxcCxpRest {

    @EJB
    private CargaHistoricaCxcCxpService cargaHistoricaCxcCxpService;

    @EJB
    private CargaHistoricaNominaService cargaHistoricaNominaService;

    /**
     * Carga un lote mensual. Cuerpo: el {@code lote.json} que produce el generador, con {@code idUsuario}
     * (obligatorio) y, opcionalmente, {@code soloValidar} y {@code aceptarInferidos}.
     * 200 = lote procesado (el detalle por documento va en {@code items}, puede haber rechazados);
     * 400 = el lote no se pudo procesar (errores de período, catálogos o petición); 500 = error inesperado.
     */
    @POST
    @Path("/cargarHistoricoCxcCxp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarHistoricoCxcCxp(LoteHistoricoCxcCxp lote) {
        System.out.println("LLEGA AL SERVICIO cargarHistoricoCxcCxp - mes: " + (lote != null ? lote.mes : null));
        try {
            ResultadoCargaHistorica resultado = cargaHistoricaCxcCxpService.cargar(lote);
            Response.Status estado = resultado.erroresLote.isEmpty() ? Response.Status.OK : Response.Status.BAD_REQUEST;
            return Response.status(estado).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la carga historica de CxC/CxP: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /**
     * D-013: mueve parte del saldo inicial del titular SALDO INICIAL a un titular real (sin tocar la contabilidad
     * de mayo 2025). Cuerpo: {@link com.saa.model.historico.ReasignacionSaldoInicial}. 200 = reasignado u OMITIDO
     * (ya existía el documento destino); 400 = petición inválida o RECHAZADO (sin saldo, titular inválido...).
     */
    @POST
    @Path("/reasignarSaldoInicial")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reasignarSaldoInicial(com.saa.model.historico.ReasignacionSaldoInicial reasignacion) {
        System.out.println("LLEGA AL SERVICIO reasignarSaldoInicial - " + (reasignacion != null ? reasignacion.lado + " " + reasignacion.identificacionDestino : null));
        try {
            ResultadoCargaHistorica resultado = cargaHistoricaCxcCxpService.reasignarSaldoInicial(reasignacion);
            return Response.status(resultado.exito ? Response.Status.OK : Response.Status.BAD_REQUEST).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la reasignacion de saldo inicial: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Igual que cargar pero fuerza {@code soloValidar = true}: no graba nada, solo informa lo que haría. */
    @POST
    @Path("/validarHistoricoCxcCxp")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarHistoricoCxcCxp(LoteHistoricoCxcCxp lote) {
        if (lote != null) lote.soloValidar = true;
        return cargarHistoricoCxcCxp(lote);
    }

    /**
     * Carga histórica de nómina (C13): un lote mensual del rol del contador (empleados, período, nóminas, renglones,
     * provisiones, pagos, planillas IESS) con sus asientos por los motores del SAA. Mismo contrato de respuesta que
     * {@link #cargarHistoricoCxcCxp}. Ver docs/logica-negocio/rhh/CARGA-HISTORICA-NOMINA.md.
     */
    @POST
    @Path("/cargarHistoricoNomina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cargarHistoricoNomina(LoteHistoricoNomina lote) {
        System.out.println("LLEGA AL SERVICIO cargarHistoricoNomina - mes: " + (lote != null ? lote.mes : null));
        try {
            ResultadoCargaHistorica resultado = cargaHistoricaNominaService.cargar(lote);
            Response.Status estado = resultado.erroresLote.isEmpty() ? Response.Status.OK : Response.Status.BAD_REQUEST;
            return Response.status(estado).entity(resultado).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error en la carga historica de nomina: " + e.getMessage())
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    /** Igual que cargarHistoricoNomina pero fuerza {@code soloValidar = true}: no graba nada. */
    @POST
    @Path("/validarHistoricoNomina")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response validarHistoricoNomina(LoteHistoricoNomina lote) {
        if (lote != null) lote.soloValidar = true;
        return cargarHistoricoNomina(lote);
    }
}