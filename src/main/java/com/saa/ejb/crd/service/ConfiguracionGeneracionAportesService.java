package com.saa.ejb.crd.service;

import com.saa.ejb.crd.service.dto.EstadoGeneracionPorFaltante;

import jakarta.ejb.Local;

/**
 * Bandera del camino nuevo de generación de aportes por faltante (rubro
 * {@link com.saa.rubros.Rubros#CRD_GENERACION_POR_FALTANTE}, detalle
 * {@link com.saa.rubros.CrdGeneracionPorFaltante#GENERACION_POR_FALTANTE_ACTIVA}).
 *
 * Fase 4 del plan de devengo de aportes: mientras está apagada (el valor por defecto),
 * {@code GeneracionArchivoPetroServiceImpl.recopilarAportes} sigue con el camino viejo
 * (monto de HistorialSueldo x meses adeudados). Encendida, cobra el faltante mes a mes
 * contra las vigencias de {@code CRD.VGCN}.
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Local
public interface ConfiguracionGeneracionAportesService {

    /**
     * Indica si el camino nuevo (por faltante) está activo.
     *
     * Si la lectura del rubro falla o el detalle no existe/devuelve null, retorna
     * {@code false}: apagado es el lado seguro, y además es el valor con el que se
     * entrega esta fase.
     *
     * @return true si el camino nuevo está activo
     */
    boolean porFaltanteActiva();

    /**
     * Estado completo del flag, con la huella de quién y cuándo lo cambió por última vez.
     *
     * @return Estado del flag con su huella
     */
    EstadoGeneracionPorFaltante obtenerEstado();

    /**
     * Enciende o apaga el camino nuevo de generación de aportes.
     *
     * @param activa  Nuevo estado del flag
     * @param usuario Usuario que hace el cambio (solo se registra en el log/huella)
     * @param motivo  Motivo del cambio (solo se registra en el log/huella)
     * @return El nuevo estado del flag
     * @throws Throwable Si ocurre un error
     */
    boolean actualizar(boolean activa, String usuario, String motivo) throws Throwable;
}
