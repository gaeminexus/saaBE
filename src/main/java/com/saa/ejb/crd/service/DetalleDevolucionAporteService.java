package com.saa.ejb.crd.service;

import com.saa.basico.util.EntityService;
import com.saa.model.crd.DetalleDevolucionAporte;

import jakarta.ejb.Local;

/**
 * Detalle por tipo de aporte de una devolucion (CRD.DDVA).
 *
 * No tiene proceso propio: las filas las crea y las actualiza
 * {@code DevolucionAporteService} (registro y contra-movimientos). Este servicio existe
 * para completar el patron de capas de la tabla.
 */
@Local
public interface DetalleDevolucionAporteService extends EntityService<DetalleDevolucionAporte> {

}
