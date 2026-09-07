package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.DetalleEscalaCalificacionRiesgo;
import com.saa.model.crd.EscalaCalificacionRiesgo;

import jakarta.ejb.Local;

/**
 * Servicio de la entidad EscalaCalificacionRiesgo (CRD.ESCR) — P22,
 * PLAN-CALIFICACION-RIESGO-PARAMETRIZABLE.md.
 *
 * <p><b>El flujo de negocio NO pasa por aquí, y a propósito NO es simétrico con
 * {@code BandaProductoService}.</b> Una banda suelta inválida (número salteado, sin banda
 * abierta) es un defecto de presentación: la estructura de bandas se DERIVA de acumular
 * períodos y no puede quedar en un estado que no tenga sentido matemático con solo agregar o
 * quitar una fila. <b>Una escala de calificación SÍ puede quedar inválida con un solo cambio</b>
 * —un hueco entre día 30 y día 35, dos calificaciones con el mismo rango— y un hueco así hace
 * que una cuota no califique y el G48 salga mal, sin ningún error visible. Por eso
 * {@code EscalaCalificacionRiesgoRest} NO expone POST/PUT/DELETE: toda escritura pasa por
 * {@code ConfiguracionCalificacionRiesgoService#guardarConfiguracion}, que valida el CONJUNTO
 * antes de grabar una sola fila. Este servicio existe para el CRUD de lectura del patrón de
 * capas y para consultas de apoyo — copia el patrón de {@code BandaProductoService}, no su
 * superficie de escritura.</p>
 */
@Local
public interface EscalaCalificacionRiesgoService extends EntityService<EscalaCalificacionRiesgo> {

    /**
     * Calificaciones de una configuración, con la etiqueta del rango ya armada.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CFCR)
     * @return                : Calificaciones ordenadas; lista VACÍA si no tiene ninguna
     * @throws Throwable      : Excepcion
     */
    List<DetalleEscalaCalificacionRiesgo> selectDetalleByConfiguracion(Long idConfiguracion) throws Throwable;

    /**
     * Calificaciones de una configuración, como entidades.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CFCR)
     * @return                : Calificaciones ordenadas; lista VACÍA si no tiene ninguna
     * @throws Throwable      : Excepcion
     */
    List<EscalaCalificacionRiesgo> selectByConfiguracion(Long idConfiguracion) throws Throwable;
}
