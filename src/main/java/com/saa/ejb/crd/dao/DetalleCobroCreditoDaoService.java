/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DetalleCobroCredito;

import jakarta.ejb.Local;

/**
 * @author Sistema SAA.
 *         Interface DAO para la entidad DetalleCobroCredito (CRD.DCBC): una fila por
 *         préstamo dentro de un {@code CobroCredito}.
 */
@Local
public interface DetalleCobroCreditoDaoService extends EntityDao<DetalleCobroCredito> {

    /**
     * Las líneas de un cobro, en el orden en que se registraron.
     *
     * @param idCobro    : Código del cobro (CRD.CBCR)
     * @return           : Listado; nunca vacío para un cobro existente
     * @throws Throwable : Excepcion
     */
    List<DetalleCobroCredito> selectByCobro(Long idCobro) throws Throwable;

    /**
     * Qué cobros tocaron un préstamo, para su ficha.
     *
     * @param idPrestamo : Código del préstamo (CRD.PRST)
     * @return           : Listado; VACÍO si el préstamo nunca fue afectado por esta vía
     * @throws Throwable : Excepcion
     */
    List<DetalleCobroCredito> selectByPrestamo(Long idPrestamo) throws Throwable;

    /**
     * Las líneas de cobro enlazadas a un evento de pago (DCBC.EVPRCDGO), para saber si un evento
     * nació de un cobro y no debe anularse por fuera de su reverso.
     *
     * @param idEvento   : Código del evento (CRD.EVPR)
     * @return           : Listado; VACÍO si el evento no vino de ningún cobro
     * @throws Throwable : Excepcion
     */
    List<DetalleCobroCredito> selectByEvento(Long idEvento) throws Throwable;
}
