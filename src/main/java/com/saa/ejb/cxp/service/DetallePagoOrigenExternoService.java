package com.saa.ejb.cxp.service;

import com.saa.basico.util.EntityService;
import com.saa.model.cxp.DetallePagoOrigenExterno;

import jakarta.ejb.Local;

/**
 * Desglose contable de un pago cuyo documento de origen vive en otro modulo del sistema
 * (PGS.DPGT).
 *
 * No tiene proceso propio: las filas las crea
 * {@code PagoProgramadoService.registrarPagoDeOrigenExterno} y las lee
 * {@code contabilizarPagoOrigenExterno} para armar las lineas DEBE del asiento. Este
 * servicio existe para completar el patron de capas de la tabla.
 */
@Local
public interface DetallePagoOrigenExternoService extends EntityService<DetallePagoOrigenExterno> {

}
