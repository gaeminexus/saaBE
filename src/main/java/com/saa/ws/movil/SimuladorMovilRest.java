package com.saa.ws.movil;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ProductoDaoService;
import com.saa.ejb.crd.service.SimulacionPrestamoService;
import com.saa.ejb.crd.service.dto.CuotaProyectada;
import com.saa.ejb.crd.service.dto.ParametrosAmortizacion;
import com.saa.ejb.crd.service.dto.ResultadoSimulacionCreditoNuevo;
import com.saa.model.crd.NombreEntidadesCredito;
import com.saa.model.crd.Producto;
import com.saa.model.crd.TipoPrestamo;
import com.saa.rubros.Estado;
import com.saa.ws.movil.dto.CuotaProyectadaMovilDTO;
import com.saa.ws.movil.dto.MensajeMovilDTO;
import com.saa.ws.movil.dto.ProductoMovilDTO;
import com.saa.ws.movil.dto.SimulacionCreditoMovilDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/simulador/...} — §5.2/§5.3 del contrato.
 */
@ClaveMovilRequerida
@Path("simulador")
public class SimuladorMovilRest {

    @EJB
    private SimulacionPrestamoService simulacionPrestamoService;

    @EJB
    private ProductoDaoService productoDaoService;

    /**
     * GET /movil/simulador/productos — productos vigentes que la app puede simular, recortados.
     * {@code CRD.PRDC} es un catálogo acotado (§4.7 del contrato, precisado 2026-09-07): se
     * lista entero con el {@code selectAll} genérico de {@code EntityDao} y se filtra ACÁ por
     * {@code Producto.estado == Estado.ACTIVO} — sin tocar {@code ProductoDaoService}, que es de
     * otro equipo.
     */
    @GET
    @Path("/productos")
    @Produces(MediaType.APPLICATION_JSON)
    public Response productos() {
        System.out.println("LLEGA AL SERVICIO GET simulador/productos - MOVIL");
        try {
            List<Producto> productos = productoDaoService.selectAll(NombreEntidadesCredito.PRODUCTO);
            List<ProductoMovilDTO> dtos = new ArrayList<>();
            for (Producto producto : productos) {
                if (producto.getEstado() != null && producto.getEstado().intValue() == Estado.ACTIVO) {
                    dtos.add(aDTO(producto));
                }
            }
            return Response.status(Response.Status.OK).entity(dtos).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MensajeMovilDTO("Error al obtener los productos"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private ProductoMovilDTO aDTO(Producto producto) {
        ProductoMovilDTO dto = new ProductoMovilDTO();
        dto.setCodigo(producto.getCodigo());
        dto.setNombre(producto.getNombre());
        dto.setCodigoSBS(producto.getCodigoSBS());
        TipoPrestamo tipoPrestamo = producto.getTipoPrestamo();
        if (tipoPrestamo != null) {
            dto.setIdTipoPrestamo(tipoPrestamo.getCodigo());
            dto.setNombreTipoPrestamo(tipoPrestamo.getNombre());
        }
        return dto;
    }

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
