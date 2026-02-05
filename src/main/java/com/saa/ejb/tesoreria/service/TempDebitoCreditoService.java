package com.saa.ejb.tesoreria.service;

import java.util.List;
import com.saa.basico.util.EntityService;
import com.saa.model.tsr.TempDebitoCredito;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Chequera.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TempDebitoCreditoService extends EntityService<TempDebitoCredito>{
	 
	/**
	 * Recupera Listado TempDetalleCredito con tipo 
	 * @param tipoMovimiento	: Tipo de Movimento
	 * @param idUsuarioDebito	: Id del Usuario que realiza el Debito
	 * @return					: Listado 
	 * @throws Throwable		: Excepcions
	 */
	List<TempDebitoCredito> selectTempDebitoCreditoByTipo(Long tipoMovimiento, Long idUsuarioDebito)throws Throwable;
	
	/**
	 * Elimina los registros temporales de debito-credito por tipo y usuario
	 * @param tipoMovimiento	: Tipo de Movimento
	 * @param idUsuarioDebito	: Id del Usuario que realiza el Debito
	 * @throws Throwable		: Excepcion
	 */
	void eliminarPorUsuarioTipo(Long tipoMovimiento, Long idUsuarioDebito)throws Throwable;
}
