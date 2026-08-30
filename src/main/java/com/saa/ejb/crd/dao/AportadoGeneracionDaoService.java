package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import jakarta.ejb.Local;

/**
 * Consulta de "aportado(entidad, mes, tipo)" para el universo de partícipes de una filial,
 * en una sola consulta agregada (evita N+1 sobre miles de partícipes en un proceso de
 * generación). Fase 4 del plan de devengo de aportes.
 *
 * Vive en {@code com.saa.ejb.crd.dao}/{@code daoImpl} — no en {@code AporteDaoService} — a
 * propósito: es un DAO propio de {@code GeneracionArchivoPetroServiceImpl}, para no tocar el
 * DAO de aportes (en manos de otro agente en esta ola).
 *
 * @author Sistema SAA
 * @since 2026-08-27
 */
@Local
public interface AportadoGeneracionDaoService {

    /**
     * Suma {@code CRD.APRT.APRTVLRR} por entidad + PERIODO EFECTIVO + tipo de aporte, para
     * todas las entidades ACTIVO/ACTIVO_EN_MORA de la filial dada, en el rango
     * [desde, hasta] inclusive.
     *
     * @param codigoFilial : Código de la filial (CRD.FLLL)
     * @param desde        : Primer día del mes de devengo, inclusive (el piso, 2025-06-01)
     * @param hasta        : Primer día del mes de devengo, inclusive (el periodo que se genera)
     * @return             : Filas {@code Object[]{Long idEntidad, LocalDate periodo, Long idTipoAporte, Double suma}}
     * @throws Throwable   : Excepcion
     */
    List<Object[]> sumAportadoPorEntidadPeriodoTipo(Long codigoFilial, LocalDate desde, LocalDate hasta) throws Throwable;

}
