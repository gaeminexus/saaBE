package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.saa.ejb.crd.dao.AporteDaoService;
import com.saa.ejb.crd.service.SaldoAporteService;
import com.saa.ejb.crd.service.dto.SaldoTipoAporte;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de los saldos de aportes por entidad (§7.2).
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Stateless
public class SaldoAporteServiceImpl implements SaldoAporteService {

    @EJB
    private AporteDaoService aporteDaoService;

    @Override
    public List<SaldoTipoAporte> saldosPorEntidad(Long idEntidad) throws Throwable {
        System.out.println("SaldoAporteService.saldosPorEntidad - Entidad: " + idEntidad);

        List<SaldoTipoAporte> saldos = new ArrayList<>();
        if (idEntidad == null) {
            return saldos;
        }

        List<Object[]> filas = aporteDaoService.sumValorPorTipoAporteByEntidad(idEntidad);
        if (filas == null) {
            return saldos;
        }

        for (Object[] fila : filas) {
            Long idTipo = fila[0] != null ? ((Number) fila[0]).longValue() : null;
            String nombre = fila[1] != null ? String.valueOf(fila[1]) : null;
            double suma = fila[2] != null ? ((Number) fila[2]).doubleValue() : 0.0;
            saldos.add(new SaldoTipoAporte(idTipo, nombre, redondear(suma)));
        }

        System.out.println("  Tipos con saldo: " + saldos.size());
        return saldos;
    }

    @Override
    public double saldoPorEntidadYTipo(Long idEntidad, Long idTipoAporte) throws Throwable {
        System.out.println("SaldoAporteService.saldoPorEntidadYTipo - Entidad: " + idEntidad
            + " - Tipo: " + idTipoAporte);

        if (idEntidad == null || idTipoAporte == null) {
            return 0.0;
        }
        Double suma = aporteDaoService.sumValorByEntidadYTipo(idEntidad, idTipoAporte);
        return redondear(suma != null ? suma : 0.0);
    }

    private double redondear(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
