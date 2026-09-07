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

    /**
     * Calificaciones de varias configuraciones a la vez, ordenadas igual que
     * {@link #selectByConfiguracion}. Para {@code /cfcr/listado}: una sola consulta para todos
     * los productos en vez de una por configuración — mismo criterio que
     * {@code BandaProductoDaoService#selectByConfiguraciones}.
     *
     * @param idsConfiguracion : Códigos de las configuraciones
     * @return                 : Calificaciones de todas ellas; VACÍA si la lista es vacía
     * @throws Throwable       : Excepcion
     */
    List<EscalaCalificacionRiesgo> selectByConfiguraciones(List<Long> idsConfiguracion) throws Throwable;

    /**
     * Elimina físicamente las calificaciones de una configuración. Lo usa el guardado de
     * configuración completa, que reemplaza el juego de calificaciones dentro de la misma
     * transacción — mismo criterio que {@code BandaProductoDaoService#deleteByConfiguracion}.
     * Nunca se llama sobre una configuración cuya vigencia ya empezó.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CFCR)
     * @return                : Cantidad de filas eliminadas
     * @throws Throwable      : Excepcion
     */
    int deleteByConfiguracion(Long idConfiguracion) throws Throwable;
}
