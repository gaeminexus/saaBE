package com.saa.ws.movil;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.AporteService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.EstadoCuentaAportesDTO;
import com.saa.ejb.crd.service.dto.MovimientoEstadoCuentaDTO;
import com.saa.ejb.crd.service.dto.PeriodoEstadoCuentaDTO;
import com.saa.ejb.crd.service.dto.SaldoTipoAporte;
import com.saa.ws.movil.dto.EstadoCuentaAportesMovilDTO;
import com.saa.ws.movil.dto.MensajeMovilDTO;
import com.saa.ws.movil.dto.MovimientoEstadoCuentaMovilDTO;
import com.saa.ws.movil.dto.PeriodoEstadoCuentaMovilDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/aportes/...} — §5.2 del contrato. Se apoya en {@link SaldoAporteService} y
 * {@link AporteService}, ya existentes; cero lógica de negocio nueva.
 */
@ClaveMovilRequerida
@Path("aportes")
public class AporteMovilRest {

    /** Rango por defecto cuando el borde no manda desde/hasta: últimos 24 meses (§5.2 del contrato). */
    private static final int MESES_RANGO_POR_DEFECTO = 24;

    @EJB
    private SaldoAporteService saldoAporteService;

    @EJB
    private AporteService aporteService;

    /** GET /movil/aportes/{idEntidad}/resumen — saldos por tipo de aporte, contenido plano. */
    @GET
    @Path("/{idEntidad}/resumen")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resumen(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET aportes/{idEntidad}/resumen - MOVIL - idEntidad: " + idEntidad);
        try {
            List<SaldoTipoAporte> saldos = saldoAporteService.saldosPorEntidad(idEntidad);
            return Response.status(Response.Status.OK).entity(saldos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorInterno("Error al obtener los saldos de aportes");
        }
    }

    /**
     * GET /movil/aportes/{idEntidad}/movimientos?desde=&hasta= — desde/hasta en {@code yyyy-MM},
     * opcionales acá (a diferencia de {@code /rest/aprt/estadoCuenta}, que los exige): si el
     * borde no los manda, se aplican los últimos 24 meses y se declara en la respuesta.
     */
    @GET
    @Path("/{idEntidad}/movimientos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response movimientos(@PathParam("idEntidad") Long idEntidad,
            @QueryParam("desde") String desde, @QueryParam("hasta") String hasta) {
        System.out.println("LLEGA AL SERVICIO GET aportes/{idEntidad}/movimientos - MOVIL - idEntidad: "
                + idEntidad + " - Desde: " + desde + " - Hasta: " + hasta);
        try {
            boolean rangoPorDefecto = desde == null || desde.trim().isEmpty()
                    || hasta == null || hasta.trim().isEmpty();

            YearMonth mesDesde;
            YearMonth mesHasta;
            if (rangoPorDefecto) {
                mesHasta = YearMonth.now();
                mesDesde = mesHasta.minusMonths(MESES_RANGO_POR_DEFECTO - 1);
            } else {
                try {
                    mesDesde = YearMonth.parse(desde.trim());
                    mesHasta = YearMonth.parse(hasta.trim());
                } catch (java.time.format.DateTimeParseException e) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(new MensajeMovilDTO("desde/hasta deben tener formato yyyy-MM"))
                            .type(MediaType.APPLICATION_JSON).build();
                }
            }

            LocalDate fechaDesde = mesDesde.atDay(1);
            LocalDate fechaHasta = mesHasta.atDay(1);

            EstadoCuentaAportesDTO resultado = aporteService.estadoCuenta(idEntidad, fechaDesde, fechaHasta);

            EstadoCuentaAportesMovilDTO dto = aDTO(resultado);
            dto.setRangoPorDefectoAplicado(rangoPorDefecto);
            dto.setDesdeAplicado(mesDesde.toString());
            dto.setHastaAplicado(mesHasta.toString());

            return Response.status(Response.Status.OK).entity(dto).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            String mensaje = e.getMessage() != null ? e.getMessage() : "";
            if (mensaje.startsWith(AporteService.ERR_ENTIDAD_NO_ENCONTRADA)) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new MensajeMovilDTO("No existe el partícipe indicado"))
                        .type(MediaType.APPLICATION_JSON).build();
            }
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MensajeMovilDTO("Parámetros inválidos"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return errorInterno("Error al obtener el estado de cuenta de aportes");
        }
    }

    private EstadoCuentaAportesMovilDTO aDTO(EstadoCuentaAportesDTO origen) {
        EstadoCuentaAportesMovilDTO dto = new EstadoCuentaAportesMovilDTO();
        dto.setIdEntidad(origen.getIdEntidad());
        dto.setIdentificacion(origen.getIdentificacion());
        dto.setRazonSocial(origen.getRazonSocial());
        dto.setTotalFaltante(origen.getTotalFaltante());

        List<PeriodoEstadoCuentaMovilDTO> periodos = new ArrayList<>();
        for (PeriodoEstadoCuentaDTO periodoOrigen : origen.getPeriodos()) {
            PeriodoEstadoCuentaMovilDTO periodo = new PeriodoEstadoCuentaMovilDTO();
            periodo.setPeriodo(periodoOrigen.getPeriodo());
            periodo.setIdTipoAporte(periodoOrigen.getIdTipoAporte());
            periodo.setNombreTipoAporte(periodoOrigen.getNombreTipoAporte());
            periodo.setEsperado(periodoOrigen.getEsperado());
            periodo.setAportado(periodoOrigen.getAportado());
            periodo.setFaltante(periodoOrigen.getFaltante());
            periodo.setEstado(periodoOrigen.getEstado());

            List<MovimientoEstadoCuentaMovilDTO> movimientos = new ArrayList<>();
            for (MovimientoEstadoCuentaDTO movimientoOrigen : periodoOrigen.getMovimientos()) {
                MovimientoEstadoCuentaMovilDTO movimiento = new MovimientoEstadoCuentaMovilDTO();
                movimiento.setIdAporte(movimientoOrigen.getIdAporte());
                movimiento.setFechaTransaccion(movimientoOrigen.getFechaTransaccion());
                movimiento.setValor(movimientoOrigen.getValor());
                movimiento.setTipoMovimiento(movimientoOrigen.getTipoMovimiento());
                movimiento.setTipoMovimientoTexto(movimientoOrigen.getTipoMovimientoTexto());
                movimiento.setGlosa(movimientoOrigen.getGlosa());
                movimientos.add(movimiento);
            }
            periodo.setMovimientos(movimientos);
            periodos.add(periodo);
        }
        dto.setPeriodos(periodos);
        return dto;
    }

    private Response errorInterno(String texto) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new MensajeMovilDTO(texto))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
