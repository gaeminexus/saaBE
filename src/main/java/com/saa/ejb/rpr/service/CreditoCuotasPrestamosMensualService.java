package com.saa.ejb.rpr.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.rpr.service.dto.ResumenBandaCcpm;
import com.saa.model.rpr.CreditoCuotasPrestamosMensual;

import jakarta.ejb.Local;

@Local
public interface CreditoCuotasPrestamosMensualService extends EntityService<CreditoCuotasPrestamosMensual> {

    /**
     * Retorna todos los registros CCPM que pertenecen a una ejecución específica.
     * @param codigoEjecucion Código de la EjecucionReporteCartera
     * @return Lista de CreditoCuotasPrestamosMensual
     */
    List<CreditoCuotasPrestamosMensual> selectByEjecucion(Long codigoEjecucion) throws Throwable;

    /**
     * Retorna un registro CCPM por ejecución y número de operación.
     */
    CreditoCuotasPrestamosMensual selectByEjecucionYOperacion(Long codigoEjecucion, String numeroOperacion) throws Throwable;

    /**
     * Resumen financiero de crédito por (producto, tipoCartera, banda) para el mes de una
     * ejecución de reportes de cartera — el mismo contrato que el Resumen Financiero de
     * {@code informes-mensuales-credito} consume, pero calculado en el backend con la MISMA
     * regla de bandas que el cierre de cartera (2026-10-13,
     * {@code DISENO-CCPM-BANDAS-COMO-EL-CIERRE.md} §2.2).
     *
     * <p>Si el período de la ejecución ya tiene una corrida EJECUTADA de
     * {@code CierreCarteraService}, la fuente es su snapshot ({@code CRD.BDCC}), congelado y
     * cuadrado al centavo con los asientos del cierre. Si no, se calcula en vivo con el mismo
     * núcleo (universo del cierre + {@code ClasificadorBandaService}) y se marca
     * {@code fuente = "PRELIMINAR"}.</p>
     *
     * @param idEjcc : Código de la {@code EjecucionReporteCartera}
     * @return       : Filas ordenadas por producto, tipo de cartera y número de banda;
     *                 incluye las bandas parametrizadas sin capital, en cero
     * @throws Throwable : {@code jakarta.persistence.NoResultException} si el EJCC no existe;
     *                     {@code IncomeException} si no se puede resolver la empresa o falta
     *                     parametrización de bandas
     */
    List<ResumenBandaCcpm> resumenBandas(Long idEjcc) throws Throwable;
}
