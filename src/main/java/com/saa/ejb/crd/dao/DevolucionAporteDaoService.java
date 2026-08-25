package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DevolucionAporte;

import jakarta.ejb.Local;

@Local
public interface DevolucionAporteDaoService extends EntityDao<DevolucionAporte> {

    /**
     * Devoluciones de un participe, de la mas reciente a la mas antigua.
     * @param idEntidad  : Codigo de la entidad (participe)
     * @return           : Listado de devoluciones; lista VACIA si no tiene ninguna
     * @throws Throwable : Excepcion
     */
    List<DevolucionAporte> selectByEntidad(Long idEntidad) throws Throwable;

    /**
     * Devoluciones que el reconciliador debe evaluar: las que estan en estado
     * REGISTRADA(1) o EN_PAGO(2) y ya tienen una orden de pago asociada.
     *
     * Es el universo de {@code DevolucionAporteService.sincronizarPagos}: una devolucion
     * en 3 PAGADA, 4 RECHAZADA o 5 ANULADA ya cerro su ciclo y no se vuelve a tocar.
     * @return           : Listado de devoluciones pendientes de conciliar
     * @throws Throwable : Excepcion
     */
    List<DevolucionAporte> selectPendientesConciliacion() throws Throwable;

    /**
     * Devoluciones de un participe pendientes de conciliar. Lo usa el GET del listado,
     * que reconcilia antes de responder para que lo que ve el usuario este al dia.
     * @param idEntidad  : Codigo de la entidad (participe)
     * @return           : Listado de devoluciones pendientes de conciliar del participe
     * @throws Throwable : Excepcion
     */
    List<DevolucionAporte> selectPendientesConciliacionByEntidad(Long idEntidad) throws Throwable;
}
