package com.saa.ejb.crd.serviceImpl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.saa.ejb.crd.dao.CierreCarteraDaoService;
import com.saa.ejb.crd.service.CalificacionRiesgoService;
import com.saa.ejb.crd.service.CarteraAlCorteService;
import com.saa.ejb.crd.service.dto.OperacionCarteraCorte;
import com.saa.ejb.crd.service.dto.ResultadoCalificacionRiesgo;
import com.saa.model.crd.EscalaCalificacionRiesgo;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación de {@link CarteraAlCorteService}. Una consulta agregada por operación
 * ({@code CierreCarteraDaoService#selectOperacionesAlCorte}) y la escala resuelta UNA vez por
 * producto, igual que el generador G48.
 */
@Stateless
public class CarteraAlCorteServiceImpl implements CarteraAlCorteService {

    @EJB
    private CierreCarteraDaoService cierreCarteraDaoService;

    @EJB
    private CalificacionRiesgoService calificacionRiesgoService;

    @Override
    public List<OperacionCarteraCorte> calcular(LocalDate corte, boolean corteInclusivo) throws Throwable {
        System.out.println("Ingresa al metodo (calcular) CarteraAlCorteService - corte: " + corte
                + " - corteInclusivo: " + corteInclusivo);
        Map<Long, List<EscalaCalificacionRiesgo>> escalas = new HashMap<>();
        List<OperacionCarteraCorte> resultado = new ArrayList<>();
        for (Object[] f : cierreCarteraDaoService.selectOperacionesAlCorte(corte, corteInclusivo)) {
            OperacionCarteraCorte op = new OperacionCarteraCorte();
            op.setCorte(corte);
            op.setIdPrestamo((Long) f[0]);
            op.setIdProducto((Long) f[1]);
            op.setIdTipoPrestamo((Long) f[2]);
            op.setIdEntidad((Long) f[3]);
            op.setPorVencer(redondear((Double) f[4]));
            op.setVencido(redondear((Double) f[5]));
            op.setDiasMora(((Long) f[6]).longValue());
            op.setInteresVencido(redondear((Double) f[7]));
            op.setMora(redondear((Double) f[8]));
            op.setIdCuotaAncla((Long) f[9]);
            op.setInteresCuotasDelMes(redondear((Double) f[10]));
            op.setCuentaIndividual(redondear((Double) f[11]));
            op.setCapitalCuotasDelMes(f.length > 12 && f[12] != null ? redondear((Double) f[12]) : 0.0);

            List<EscalaCalificacionRiesgo> escala = escalas.get(op.getIdProducto());
            if (escala == null) {
                escala = calificacionRiesgoService.resolverEscala(op.getIdProducto(), null, corte);
                escalas.put(op.getIdProducto(), escala);
            }
            ResultadoCalificacionRiesgo calificacion = calificacionRiesgoService.calificarEnEscala(
                    op.getIdProducto(), escala, Long.valueOf(op.getDiasMora()));
            double porcentaje = calificacion.getPorcentajeProvision() != null
                    ? calificacion.getPorcentajeProvision().doubleValue() : 0.0;
            double vsap = Math.max(0.0, op.getSaldo() - op.getCuentaIndividual());
            op.setCalificacion(calificacion.getCalificacion());
            op.setPorcentajeProvision(porcentaje);
            op.setValorSujetoProvision(redondear(vsap));
            // Sin redondear por operación a propósito: la G48 de mayo (Delta y SAA) guarda el
            // producto sin redondear; el total se redondea al asentar.
            op.setProvisionRequerida(vsap * porcentaje);
            op.setProvisionConstituida(vsap * porcentaje);
            resultado.add(op);
        }
        System.out.println("  CarteraAlCorte: " + resultado.size() + " operaciones al " + corte);
        return resultado;
    }

    @Override
    public Map<Long, OperacionCarteraCorte> calcularPorPrestamo(LocalDate corte, boolean corteInclusivo)
            throws Throwable {
        Map<Long, OperacionCarteraCorte> mapa = new LinkedHashMap<>();
        for (OperacionCarteraCorte op : calcular(corte, corteInclusivo)) {
            mapa.put(op.getIdPrestamo(), op);
        }
        return mapa;
    }

    private double redondear(Double valor) {
        if (valor == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor.doubleValue()).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
