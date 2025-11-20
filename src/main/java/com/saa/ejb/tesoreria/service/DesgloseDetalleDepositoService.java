package com.saa.ejb.tesoreria.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.AuxDepositoDesglose;
import com.saa.model.tesoreria.DesgloseDetalleDeposito;
import com.saa.model.tesoreria.DetalleDeposito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad DesgloseDetalleDeposito.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface DesgloseDetalleDepositoService extends EntityService<DesgloseDetalleDeposito>{
 
	/**
	 * Almacena el desglose deun deposito
	 * @param auxDepositoDesglose	: Entidad Auxiliar Deposito desglose
	 * @param detalleDeposito		: Entidad Detalle Deposito
	 * @throws Throwable			: Excepcion
	 */
	void saveDesglose(AuxDepositoDesglose auxDepositoDesglose, DetalleDeposito detalleDeposito) throws Throwable;
	
	/**
	 * Recupera la lista de detalles ratificados por deposito 
	 * @param idDeposito: Id del deposito
	 * @return			: Lista de detalles ratificados
	 * @throws Throwable: Excepcion
	 */
	List<DesgloseDetalleDeposito> selectDetallesRatificadosByDeposito(Long idDeposito) throws Throwable;
}
