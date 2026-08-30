package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.EstadoContabilidadCrd;

import jakarta.ejb.Local;

/**
 * Flag global de contabilidad de CRD (rubro {@link com.saa.rubros.Rubros#CRD_PARAMETROS_CONTABILIDAD},
 * detalle {@link com.saa.rubros.CrdParametroContabilidad#CONTABILIDAD_ACTIVA}).
 *
 * Decision D10 del plan de devengo de aportes: el flag es GLOBAL, "o se alimenta todo o
 * nada". Mientras esta apagado, los procesos de CRD que generan asientos siguen calculando
 * igual pero NO crean el asiento.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Local
public interface ConfiguracionContabilidadService {

    /**
     * Indica si la contabilidad de CRD esta activa.
     *
     * Si la lectura del rubro falla o el detalle no existe/devuelve null, retorna
     * {@code false}: apagado es el lado seguro.
     *
     * @return true si la contabilidad de CRD esta activa
     */
    boolean contabilidadActiva();

    /**
     * Estado completo del flag, con la huella de quién y cuándo lo cambió por última vez.
     *
     * La huella se decodifica de {@code PDTRVLRV} (ver {@link #actualizar}); si nunca se ha
     * cambiado el flag, o el valor guardado no se pudo interpretar, o la lectura del rubro
     * falla, los tres campos de huella vienen en {@code null} y {@code activa} en
     * {@code false} — nunca se lanza una excepción por esto.
     *
     * @return Estado del flag con su huella
     */
    EstadoContabilidadCrd obtenerEstado();

    /**
     * Enciende o apaga la contabilidad de CRD.
     *
     * @param activa  Nuevo estado del flag
     * @param usuario Usuario que hace el cambio (solo se registra en el log: PDTR no tiene
     *                columna de auditoria para este rubro)
     * @param motivo  Motivo del cambio (solo se registra en el log, misma razon que usuario)
     * @return El nuevo estado del flag
     * @throws Throwable Si ocurre un error
     */
    boolean actualizar(boolean activa, String usuario, String motivo) throws Throwable;
}
