package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.ejb.crd.service.dto.SaldoTipoAporte;

import jakarta.ejb.Local;

/**
 * Saldos de aportes por entidad, calculados en la BD con una query agregada (§7.2).
 *
 * Sustituye al cálculo que hoy hace el frontend descargando TODA la tabla CRD.APRT
 * (~980.000 filas) con {@code GET /aprt/getAll}, causa del OutOfMemoryError documentado en
 * {@code docs/general/infraestructura/AUMENTAR_MEMORIA_WILDFLY.md}.
 *
 * @author Sistema SAA
 * @since 2026-08-14
 */
@Local
public interface SaldoAporteService {

    /**
     * Saldos netos por tipo de aporte vigente ({@code TipoAporte.estado = 1}) de una entidad.
     * saldo = SUM(APRTVLRR) — los pagos son filas negativas, la suma neta ES el saldo.
     *
     * @param idEntidad Código de la entidad (partícipe)
     * @return Lista de saldos por tipo; lista VACÍA si la entidad no tiene aportes (no es un error)
     * @throws Throwable Si ocurre un error
     */
    List<SaldoTipoAporte> saldosPorEntidad(Long idEntidad) throws Throwable;

    /**
     * Saldo neto de un tipo de aporte concreto para una entidad.
     *
     * @param idEntidad    Código de la entidad (partícipe)
     * @param idTipoAporte Código del tipo de aporte
     * @return Saldo disponible; 0.0 si no hay aportes de ese tipo
     * @throws Throwable Si ocurre un error
     */
    double saldoPorEntidadYTipo(Long idEntidad, Long idTipoAporte) throws Throwable;
}
