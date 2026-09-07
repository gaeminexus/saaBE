package com.saa.ws.movil;

import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.CxcKardexParticipeDaoService;
import com.saa.ejb.crd.dao.CxcParticipeDaoService;
import com.saa.ejb.crd.dao.PrestamoDaoService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.model.crd.CxcKardexParticipe;
import com.saa.model.crd.CxcParticipe;
import com.saa.model.crd.Prestamo;
import com.saa.ws.movil.dto.CuentaIndividualMovilDTO;
import com.saa.ws.movil.dto.CxcKardexMovilDTO;
import com.saa.ws.movil.dto.MensajeMovilDTO;

import jakarta.ejb.EJB;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * {@code /movil/cuenta-individual/{idEntidad}} — §5.2 del contrato. Composición: saldos de
 * aportes + préstamos con saldo (no terminales) + kardex CXC del partícipe. Cero lógica de
 * negocio nueva: cada lista sale de un {@code *Service}/{@code *DaoService} existente.
 */
@ClaveMovilRequerida
@Path("cuenta-individual")
public class CuentaIndividualMovilRest {

    /** Últimos renglones del kardex CXC a mostrar (§4.7 del contrato: nunca sin límite). */
    private static final int LIMITE_KARDEX = 50;

    @EJB
    private SaldoAporteService saldoAporteService;

    @EJB
    private PrestamoDaoService prestamoDaoService;

    @EJB
    private CxcParticipeDaoService cxcParticipeDaoService;

    @EJB
    private CxcKardexParticipeDaoService cxcKardexParticipeDaoService;

    @GET
    @Path("/{idEntidad}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response porEntidad(@PathParam("idEntidad") Long idEntidad) {
        System.out.println("LLEGA AL SERVICIO GET cuenta-individual/{idEntidad} - MOVIL - idEntidad: " + idEntidad);
        try {
            CuentaIndividualMovilDTO dto = new CuentaIndividualMovilDTO();
            dto.setIdEntidad(idEntidad);

            dto.setSaldosAportes(saldoAporteService.saldosPorEntidad(idEntidad));

            List<Prestamo> prestamosVigentes = prestamoDaoService.selectVigentesByEntidad(idEntidad);
            List<com.saa.ws.movil.dto.PrestamoMovilDTO> prestamosDto = new ArrayList<>();
            for (Prestamo prestamo : prestamosVigentes) {
                prestamosDto.add(MovilMappers.aDTO(prestamo));
            }
            dto.setPrestamosConSaldo(prestamosDto);

            List<CxcParticipe> cuentasCxc = cxcParticipeDaoService.selectByEntidad(idEntidad);
            List<CxcKardexMovilDTO> kardexDto = new ArrayList<>();
            if (!cuentasCxc.isEmpty()) {
                // Relación 1:1 esperada partícipe-CXC; si hubiera más de una, se usa la primera.
                Long idCxcParticipe = cuentasCxc.get(0).getCodigo();
                List<CxcKardexParticipe> kardex =
                        cxcKardexParticipeDaoService.selectRecientesByCxcParticipe(idCxcParticipe, LIMITE_KARDEX);
                for (CxcKardexParticipe renglon : kardex) {
                    kardexDto.add(aDTO(renglon));
                }
            }
            dto.setKardexCxc(kardexDto);

            return Response.status(Response.Status.OK).entity(dto).type(MediaType.APPLICATION_JSON).build();
        } catch (Throwable e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new MensajeMovilDTO("Error al obtener la cuenta individual del partícipe"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
    }

    private CxcKardexMovilDTO aDTO(CxcKardexParticipe renglon) {
        CxcKardexMovilDTO dto = new CxcKardexMovilDTO();
        dto.setCodigo(renglon.getCodigo());
        dto.setTotalDebito(renglon.getTotalDebito());
        dto.setTotalCredito(renglon.getTotalCredito());
        dto.setSaldoActual(renglon.getSaldoActual());
        dto.setConcepto(renglon.getConcepto());
        dto.setFechaCreado(renglon.getFechaCreado());
        return dto;
    }
}
