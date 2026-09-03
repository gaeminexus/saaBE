package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.EscalaCalificacionRiesgo;

import jakarta.ejb.Local;

/** DAO de CRD.ESCR — PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md. */
@Local
public interface EscalaCalificacionRiesgoDaoService extends EntityDao<EscalaCalificacionRiesgo> {

    /** Todas las calificaciones de una configuración, ordenadas (ESCRORDN). */
    List<EscalaCalificacionRiesgo> selectByConfiguracion(Long idConfiguracion) throws Throwable;
}
