package com.saa.ejb.crd.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import com.saa.basico.util.DatosBusqueda;
import com.saa.basico.util.IncomeException;
import com.saa.ejb.crd.dao.EscalaCalificacionRiesgoDaoService;
import com.saa.ejb.crd.service.EscalaCalificacionRiesgoService;
import com.saa.ejb.crd.service.dto.DetalleEscalaCalificacionRiesgo;
import com.saa.model.crd.EscalaCalificacionRiesgo;
import com.saa.model.crd.NombreEntidadesCredito;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;

/**
 * Implementación del servicio de EscalaCalificacionRiesgo. Ver {@link EscalaCalificacionRiesgoService}:
 * el flujo de negocio graba las calificaciones como juego completo desde
 * {@code ConfiguracionCalificacionRiesgoService}, no por aquí.
 */
@Stateless
public class EscalaCalificacionRiesgoServiceImpl implements EscalaCalificacionRiesgoService {

    @EJB
    private EscalaCalificacionRiesgoDaoService escalaCalificacionRiesgoDaoService;

    @Override
    public void remove(List<Long> id) throws Throwable {
        System.out.println("Ingresa al metodo remove[] de EscalaCalificacionRiesgo service");
        EscalaCalificacionRiesgo entidad = new EscalaCalificacionRiesgo();
        for (Long registro : id) {
            escalaCalificacionRiesgoDaoService.remove(entidad, registro);
        }
    }

    @Override
    public void save(List<EscalaCalificacionRiesgo> lista) throws Throwable {
        System.out.println("Ingresa al metodo save de EscalaCalificacionRiesgo service");
        for (EscalaCalificacionRiesgo entidad : lista) {
            escalaCalificacionRiesgoDaoService.save(entidad, entidad.getCodigo());
        }
    }

    @Override
    public EscalaCalificacionRiesgo saveSingle(EscalaCalificacionRiesgo entidad) throws Throwable {
        System.out.println("Ingresa al metodo (saveSingle) EscalaCalificacionRiesgo Service");
        escalaCalificacionRiesgoDaoService.save(entidad, entidad.getCodigo());
        return entidad;
    }

    @Override
    public List<EscalaCalificacionRiesgo> selectAll() throws Throwable {
        System.out.println("Ingresa al metodo selectAll EscalaCalificacionRiesgoService");
        List<EscalaCalificacionRiesgo> result = escalaCalificacionRiesgoDaoService
                .selectAll(NombreEntidadesCredito.ESCALA_CALIFICACION_RIESGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de EscalaCalificacionRiesgo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public EscalaCalificacionRiesgo selectById(Long id) throws Throwable {
        System.out.println("Ingresa al metodo (selectById) de EscalaCalificacionRiesgo con id: " + id);
        return escalaCalificacionRiesgoDaoService.selectById(id, NombreEntidadesCredito.ESCALA_CALIFICACION_RIESGO);
    }

    @Override
    public List<EscalaCalificacionRiesgo> selectByCriteria(List<DatosBusqueda> datos) throws Throwable {
        System.out.println("Ingresa al metodo (selectByCriteria) EscalaCalificacionRiesgo");
        List<EscalaCalificacionRiesgo> result = escalaCalificacionRiesgoDaoService
                .selectByCriteria(datos, NombreEntidadesCredito.ESCALA_CALIFICACION_RIESGO);
        if (result.isEmpty()) {
            throw new IncomeException("Busqueda de EscalaCalificacionRiesgo no devolvio ningun registro");
        }
        return result;
    }

    @Override
    public List<DetalleEscalaCalificacionRiesgo> selectDetalleByConfiguracion(Long idConfiguracion)
            throws Throwable {
        System.out.println("Ingresa al metodo (selectDetalleByConfiguracion) EscalaCalificacionRiesgo"
                + " - configuracion: " + idConfiguracion);
        if (idConfiguracion == null) {
            throw new IncomeException("La configuracion es obligatoria");
        }
        List<DetalleEscalaCalificacionRiesgo> resultado = new ArrayList<>();
        for (EscalaCalificacionRiesgo escala : escalaCalificacionRiesgoDaoService
                .selectByConfiguracion(idConfiguracion)) {
            resultado.add(aDetalle(escala));
        }
        return resultado;
    }

    @Override
    public List<EscalaCalificacionRiesgo> selectByConfiguracion(Long idConfiguracion) throws Throwable {
        System.out.println("Ingresa al metodo (selectByConfiguracion) EscalaCalificacionRiesgo"
                + " - configuracion: " + idConfiguracion);
        if (idConfiguracion == null) {
            throw new IncomeException("La configuracion es obligatoria");
        }
        return escalaCalificacionRiesgoDaoService.selectByConfiguracion(idConfiguracion);
    }

    /**
     * Arma el DTO de una calificación con la etiqueta del rango ya calculada — mismo criterio
     * que {@code ConfiguracionCalificacionRiesgoServiceImpl.etiquetaTramo} (duplicado a
     * propósito: es un formateo de 3 líneas, no vale la pena una dependencia entre los dos
     * servicios por esto).
     */
    private DetalleEscalaCalificacionRiesgo aDetalle(EscalaCalificacionRiesgo escala) {
        DetalleEscalaCalificacionRiesgo detalle = new DetalleEscalaCalificacionRiesgo();
        detalle.setIdEscala(escala.getCodigo());
        detalle.setCalificacion(escala.getCalificacion());
        detalle.setDiaDesde(escala.getDiaDesde());
        detalle.setDiaHasta(escala.getDiaHasta());
        detalle.setPorcentajeProvision(escala.getPorcentajeProvision());
        Long desde = escala.getDiaDesde();
        Long hasta = escala.getDiaHasta();
        if (hasta == null) {
            detalle.setEtiqueta("mas de " + (desde != null ? desde - 1 : 0) + " (resto)");
        } else if (desde != null && desde.equals(hasta)) {
            detalle.setEtiqueta(String.valueOf(desde));
        } else {
            detalle.setEtiqueta(desde + " - " + hasta);
        }
        return detalle;
    }
}
