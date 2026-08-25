/**
 * Copyright © Gaemi Soft Cía. Ltda. , 2011 Reservados todos los derechos
 * Fernado Ortega N64-28 y Av. José Fernández.
 * Quito - Ecuador
 */
package com.saa.ejb.crd.service;

import java.util.List;

import com.saa.basico.util.EntityService;
import com.saa.ejb.crd.service.dto.BandaProductoDetalle;
import com.saa.model.crd.BandaProducto;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft.
 *         Servicio de la entidad BandaProducto (CRD.BNDP).
 *
 *         <p>
 *         <b>El flujo de negocio NO pasa por aquí.</b> Una banda suelta no es una unidad
 *         válida: las bandas se graban siempre como juego completo desde
 *         {@code ConfiguracionBandaProductoService.guardarConfiguracion}, que es quien
 *         valida la consecutividad, la banda abierta y las cuentas. Este servicio existe
 *         para el CRUD genérico del patrón de capas y para consultas de apoyo.
 *         </p>
 */
@Local
public interface BandaProductoService extends EntityService<BandaProducto> {

    /**
     * Bandas activas de una configuración con el rango en días ya derivado.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CBPR)
     * @return                : Bandas ordenadas por número; lista VACÍA si no tiene
     * @throws Throwable      : Excepcion
     */
    List<BandaProductoDetalle> selectDetalleByConfiguracion(Long idConfiguracion) throws Throwable;

    /**
     * Bandas activas de una configuración, como entidades.
     *
     * @param idConfiguracion : Código de la configuración (CRD.CBPR)
     * @return                : Bandas ordenadas por número; lista VACÍA si no tiene
     * @throws Throwable      : Excepcion
     */
    List<BandaProducto> selectByConfiguracion(Long idConfiguracion) throws Throwable;
}
