/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.time.LocalDate;
import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.ConfiguracionBandaProducto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad ConfiguracionBandaProducto (CRD.CBPR).
 */
@Local
public interface ConfiguracionBandaProductoDaoService extends EntityDao<ConfiguracionBandaProducto> {

    /**
     * Configuración vigente a una fecha para la terna (producto, empresa, tipo cartera).
     *
     * Vigente = activa, {@code fechaDesde <= fecha} y ({@code fechaHasta} nula o
     * {@code >= fecha}). Debe existir a lo sumo una; si la parametrización quedó mal y hay
     * varias, este método devuelve la de {@code fechaDesde} más reciente y deja que el
     * proceso siga — la validación de unicidad es del servicio de guardado.
     *
     * @param idProducto    : Código del producto (CRD.PRDC)
     * @param idEmpresa     : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera   : 1 = por vencer, 2 = vencido ({@link com.saa.rubros.TipoCarteraBanda})
     * @param fecha         : Fecha a la que se evalúa la vigencia
     * @return              : Configuración vigente, o {@code null} si no hay ninguna
     * @throws Throwable    : Excepcion
     */
    ConfiguracionBandaProducto selectVigente(Long idProducto, Long idEmpresa, Long tipoCartera,
            LocalDate fecha) throws Throwable;

    /**
     * Todas las configuraciones vigentes a una fecha de una empresa, de los dos tipos de
     * cartera. Es la consulta que alimenta el listado de la pantalla de parametrización.
     *
     * @param idEmpresa     : Código de la empresa (SCP.PJRQ)
     * @param fecha         : Fecha a la que se evalúa la vigencia
     * @return              : Listado ordenado por producto y tipo de cartera; VACÍO si no hay
     * @throws Throwable    : Excepcion
     */
    List<ConfiguracionBandaProducto> selectVigentesByEmpresa(Long idEmpresa, LocalDate fecha)
            throws Throwable;

    /**
     * Historial completo de configuraciones de una terna, vigentes y cerradas, de la más
     * reciente a la más antigua. Para auditoría y reprocesos.
     *
     * @param idProducto    : Código del producto (CRD.PRDC)
     * @param idEmpresa     : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera   : 1 = por vencer, 2 = vencido
     * @return              : Listado histórico; VACÍO si nunca se parametrizó
     * @throws Throwable    : Excepcion
     */
    List<ConfiguracionBandaProducto> selectHistorial(Long idProducto, Long idEmpresa,
            Long tipoCartera) throws Throwable;

    /**
     * Configuraciones activas de la terna cuya vigencia se solapa con el intervalo
     * [desde, hasta]. Lo usa la validación de unicidad del guardado: si devuelve algo
     * distinto de la configuración que se está editando, hay traslape.
     *
     * @param idProducto    : Código del producto (CRD.PRDC)
     * @param idEmpresa     : Código de la empresa (SCP.PJRQ)
     * @param tipoCartera   : 1 = por vencer, 2 = vencido
     * @param desde         : Inicio de la vigencia que se pretende grabar
     * @param hasta         : Fin de la vigencia que se pretende grabar; {@code null} = abierta
     * @return              : Configuraciones que se solapan; VACÍO si no hay conflicto
     * @throws Throwable    : Excepcion
     */
    List<ConfiguracionBandaProducto> selectSolapadas(Long idProducto, Long idEmpresa,
            Long tipoCartera, LocalDate desde, LocalDate hasta) throws Throwable;
}
