package com.saa.ejb.crd.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.ConfiguracionCalificacionRiesgoDaoService;
import com.saa.ejb.crd.dao.EscalaCalificacionRiesgoDaoService;
import com.saa.ejb.crd.service.CalificacionRiesgoService;
import com.saa.ejb.crd.service.dto.ResultadoCalificacionRiesgo;
import com.saa.model.crd.ConfiguracionCalificacionRiesgo;
import com.saa.model.crd.EscalaCalificacionRiesgo;
import com.saa.model.crd.Producto;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

@Stateless
public class CalificacionRiesgoServiceImpl implements CalificacionRiesgoService {

    @EJB
    private ConfiguracionCalificacionRiesgoDaoService configuracionCalificacionRiesgoDaoService;

    @EJB
    private EscalaCalificacionRiesgoDaoService escalaCalificacionRiesgoDaoService;

    @Override
    public ResultadoCalificacionRiesgo calificar(Long idProducto, Long idEmpresa, Long dias, LocalDate fecha)
            throws Throwable {
        System.out.println("CalificacionRiesgoService.calificar - producto: " + idProducto
            + " - empresa: " + idEmpresa + " - dias: " + dias + " - fecha: " + fecha);

        if (idProducto == null) {
            throw new IncomeException("El producto es obligatorio para calificar el riesgo");
        }
        // Mismo criterio que el código cableado que este servicio reemplaza
        // (GeneracionG48ServiceImpl.calcularCalificacion): sin morosidad, A1.
        long diasEfectivos = dias != null ? dias : 0L;
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now();

        ConfiguracionCalificacionRiesgo configuracion = configuracionCalificacionRiesgoDaoService
                .selectVigentePorProducto(idProducto, idEmpresa, fechaEfectiva);
        if (configuracion == null) {
            throw new IncomeException("No hay configuración de calificación de riesgo vigente al "
                    + fechaEfectiva + " para el producto " + idProducto
                    + (idEmpresa != null ? ", empresa " + idEmpresa : "") + ". Corra sql/177 y"
                    + " verifique que el producto tenga una fila en CRD.CFCR.");
        }

        List<EscalaCalificacionRiesgo> escalas = escalaCalificacionRiesgoDaoService
                .selectByConfiguracion(configuracion.getCodigo());
        if (escalas == null || escalas.isEmpty()) {
            throw new IncomeException("La configuración de calificación de riesgo "
                    + configuracion.getCodigo() + " (producto " + idProducto
                    + ") no tiene calificaciones cargadas en CRD.ESCR.");
        }

        for (EscalaCalificacionRiesgo escala : escalas) {
            Long diaDesde = escala.getDiaDesde();
            Long diaHasta = escala.getDiaHasta();
            if (diaDesde != null && diasEfectivos >= diaDesde
                    && (diaHasta == null || diasEfectivos <= diaHasta)) {
                ResultadoCalificacionRiesgo resultado = new ResultadoCalificacionRiesgo();
                resultado.setIdConfiguracion(configuracion.getCodigo());
                resultado.setIdEscala(escala.getCodigo());
                resultado.setCalificacion(escala.getCalificacion());
                resultado.setPorcentajeProvision(escala.getPorcentajeProvision());
                resultado.setDiaDesde(diaDesde);
                resultado.setDiaHasta(diaHasta);
                return resultado;
            }
        }

        throw new IncomeException("Ninguna calificación de la configuración " + configuracion.getCodigo()
                + " (producto " + idProducto + ") cubre " + diasEfectivos + " días de morosidad —"
                + " la escala tiene un hueco. Revise CRD.ESCR (sql/177 control D.3).");
    }

    @Override
    public List<Producto> productosSinConfiguracion(LocalDate fecha) throws Throwable {
        LocalDate fechaEfectiva = fecha != null ? fecha : LocalDate.now();
        List<Producto> productos = configuracionCalificacionRiesgoDaoService
                .selectProductosSinConfiguracionVigente(fechaEfectiva);
        return productos != null ? productos : java.util.Collections.emptyList();
    }
}
