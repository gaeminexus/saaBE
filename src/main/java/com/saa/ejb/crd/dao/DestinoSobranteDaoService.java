package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.DestinoSobrante;

import jakarta.ejb.Local;

/**
 * DAO de CRD.DSSB (destino del sobrante por partícipe en una carga Petro/ARCH, B8).
 */
@Local
public interface DestinoSobranteDaoService extends EntityDao<DestinoSobrante> {

    /**
     * Destinos ACTIVOS (DSSBESTD = 1) de una carga, uno por partícipe. Lista vacía si no hay.
     *
     * @param idCarga código de la carga (CRARCDGO)
     */
    List<DestinoSobrante> selectActivosByCarga(Long idCarga) throws Throwable;

    /**
     * Todos los destinos de una carga, activos y anulados (para la pantalla y la auditoría).
     *
     * @param idCarga código de la carga (CRARCDGO)
     */
    List<DestinoSobrante> selectByCarga(Long idCarga) throws Throwable;

    /**
     * Destino ACTIVO de un partícipe en una carga, o {@code null} si no eligió ninguno. Devuelve null en vez de
     * lanzar NoResultException para no marcar la transacción como rollback.
     *
     * @param idCarga código de la carga (CRARCDGO)
     * @param idEntidad código del partícipe (ENTDCDGO)
     */
    DestinoSobrante selectActivoByCargaYEntidad(Long idCarga, Long idEntidad) throws Throwable;

    /** Fuerza a base lo pendiente; hace falta antes de insertar un destino nuevo sobre uno recién anulado (índice único). */
    void flush() throws Throwable;
}
