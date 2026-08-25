package com.saa.ejb.cxp.dao;

import java.util.List;

import com.saa.basico.util.EntityDao;
import com.saa.model.cxp.DetallePagoOrigenExterno;

import jakarta.ejb.Local;

@Local
public interface DetallePagoOrigenExternoDaoService extends EntityDao<DetallePagoOrigenExterno> {

	/**
	 * Recupera el desglose contable de un pago de origen externo, ordenado por codigo.
	 * Es el orden en el que se generan las lineas DEBE del asiento.
	 * @param idPago     : Id del pago programado (PGS.PGTR)
	 * @return           : Listado de detalles; vacio si el pago no tiene desglose
	 * @throws Throwable : Excepcion
	 */
	List<DetallePagoOrigenExterno> selectByPago(Long idPago) throws Throwable;
}
