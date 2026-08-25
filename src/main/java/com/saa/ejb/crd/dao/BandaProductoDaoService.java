/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.crd.BandaProducto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Interface DAO para la entidad BandaProducto (CRD.BNDP).
 */
@Local
public interface BandaProductoDaoService extends EntityDao<BandaProducto> {

    /**
     * Bandas ACTIVAS de una configuración, ordenadas por número ascendente.
     * Es el orden que exige la derivación de rangos (acumula períodos banda a banda):
     * consumir esta lista sin reordenarla.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CBPR)
     * @return                : Listado de bandas; VACÍO si la configuración no tiene ninguna
     * @throws Throwable      : Excepcion
     */
    List<BandaProducto> selectByConfiguracion(Long idConfiguracion) throws Throwable;

    /**
     * Bandas de una configuración incluidas las inactivas, ordenadas por número.
     * Para pantallas de auditoría; los procesos contables usan
     * {@link #selectByConfiguracion(Long)}.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CBPR)
     * @return                : Listado de bandas; VACÍO si no tiene ninguna
     * @throws Throwable      : Excepcion
     */
    List<BandaProducto> selectTodasByConfiguracion(Long idConfiguracion) throws Throwable;

    /**
     * Bandas activas de VARIAS configuraciones en una sola consulta, ordenadas por
     * configuración y número. Evita la consulta por fila del listado de la pantalla.
     *
     * @param idsConfiguracion : Códigos de configuración; lista vacía o nula devuelve VACÍO
     * @return                 : Listado de bandas de todas ellas
     * @throws Throwable       : Excepcion
     */
    List<BandaProducto> selectByConfiguraciones(List<Long> idsConfiguracion) throws Throwable;

    /**
     * Elimina físicamente las bandas de una configuración. Lo usa el guardado de
     * configuración completa, que reemplaza el juego de bandas dentro de la misma
     * transacción; nunca se llama sobre una configuración cuya vigencia ya empezó.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CBPR)
     * @return                : Número de filas eliminadas
     * @throws Throwable      : Excepcion
     */
    int deleteByConfiguracion(Long idConfiguracion) throws Throwable;
}
