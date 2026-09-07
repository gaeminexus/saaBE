package com.saa.ws.movil;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.service.SimulacionPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionCreditoNuevo;
import com.saa.ws.movil.dto.CuotaProyectadaMovilDTO;
import com.saa.ws.movil.dto.MensajeMovilDTO;
import com.saa.ws.movil.dto.SimulacionCreditoMovilDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/simulador/...} — §5.2 del contrato.
 *
 * <p>⛔ Solo {@code simular} está implementado en esta entrega. {@code GET /movil/simulador/productos}
 * (§5.2 y §5.3 del contrato) queda BLOQUEADO: no existe en {@code ProductoDaoService} ningún
 * método de listado de productos vigentes — solo {@code selectByCodigoPetro}/
 * {@code selectAllByCodigoPetro} (buscan por un código Petro puntual) y {@code selectAll}
 * (prohibido desde {@code /movil} por el §5.3/§4.7 del contrato: "ni getAll... desde el borde").
 * Agregar ese método tocaría {@code ProductoDaoService}/{@code ProductoDaoServiceImpl}, que son
 * de otro equipo y NO están entre los dos DAO que el encargo autorizó a modificar (CxcParticipe/
 * CxcKardexParticipe). Reportado al árbitro en vez de resuelto por cuenta propia.</p>
 */
@ClaveMovilRequerida
@Path("simulador")
public class SimuladorMovilRest {

    @EJB
    private SimulacionPrestamoService simulacionPrestamoService;

    /**
     * POST /movil/simulador/simular — simula una tabla de amortización de un préstamo que
     * todavía no existe. No persiste nada (§5.2 del contrato, se apoya en
     * {@code prst/simularCreditoNuevo}).
     */
    @POST
    @Path("/simular")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response simular(ParametrosAmortizacion params) {
        System.out.println("LLEGA AL SERVICIO POST simulador/simular - MOVIL");
        try {
            ResultadoSimulacionCreditoNuevo resultado = simulacionPrestamoService.simularCreditoNuevo(params);
            return Response.status(Response.Status.OK).entity(aDTO(resultado)).type(MediaType.APPLICATION_JSON).build();
        } catch (IncomeException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new MensajeMovilDTO("Parámetros de simulación inválidos"))
                    .type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MensajeMovilDTO("Error al simular el crédito"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private SimulacionCreditoMovilDTO aDTO(ResultadoSimulacionCreditoNuevo origen) {
        SimulacionCreditoMovilDTO dto = new SimulacionCreditoMovilDTO();
        dto.setTotalCapital(origen.getTotalCapital());
        dto.setTotalInteres(origen.getTotalInteres());
        dto.setTotalDesgravamen(origen.getTotalDesgravamen());
        dto.setTotalSeguro(origen.getTotalSeguro());
        dto.setTotalAPagar(origen.getTotalAPagar());
        dto.setValorCuota(origen.getValorCuota());

        List<CuotaProyectadaMovilDTO> tabla = new ArrayList<>();
        for (CuotaProyectada cuotaOrigen : origen.getTablaProyectada()) {
            CuotaProyectadaMovilDTO cuota = new CuotaProyectadaMovilDTO();
            cuota.setNumeroCuota(cuotaOrigen.getNumeroCuota());
            cuota.setFechaVencimiento(cuotaOrigen.getFechaVencimiento());
            cuota.setCapital(cuotaOrigen.getCapital());
            cuota.setInteres(cuotaOrigen.getInteres());
            cuota.setCuota(cuotaOrigen.getCuota());
            cuota.setSaldoCapital(cuotaOrigen.getSaldoCapital());
            cuota.setDesgravamen(cuotaOrigen.getDesgravamen());
            cuota.setSeguroIncendio(cuotaOrigen.getSeguroIncendio());
            cuota.setTotal(cuotaOrigen.getTotal());
            tabla.add(cuota);
        }
        dto.setTablaProyectada(tabla);
        return dto;
    }
}
