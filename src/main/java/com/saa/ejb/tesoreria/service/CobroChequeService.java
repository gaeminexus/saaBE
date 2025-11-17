package com.saa.ejb.tesoreria.service;


import com.saa.basico.util.EntityService;
import com.saa.model.tesoreria.Cobro;
import com.saa.model.tesoreria.CobroCheque;
import com.saa.model.tesoreria.DetalleDeposito;
import com.saa.model.tesoreria.TempCobroCheque;

import jakarta.ejb.Remote;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad CobroCheque.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Remote
public interface CobroChequeService extends EntityService<CobroCheque>{

	 
	/**
	 * Almacena el cobro con cheque real de un cobro realizado
	 * @param tempCobroCheque	: Entidad TempCobroCheque
	 * @param cobro			: Entidad del cobro
	 * @throws Throwable		: Excepcion
	 */
	void saveCobroChequeReal(TempCobroCheque tempCobroCheque, Cobro cobro) throws Throwable;
	
	/**
	 * actualiza estado de cheque enviado y el id detalle deposito al que pertenece
	 * @param idDetalleDeposito	: Id de Detalle deposito
	 * @param estado		 	: Estado a actualizar
	 * @throws Throwable		: Exception
	 */
	void actualizaCobroCheque(Long idDetalleDeposito, int estado)  throws Throwable;
	
	/**
	 * actualiza estado de cheque enviado y el id detalle deposito al que pertenece
	 * @param cobroCheque		: Cobro Cheque a actualizar
	 * @param detalleDeposito	: Detalle deposito
	 * @param estado		 	: Estado a actualizar
	 * @throws Throwable		: Exception
	 */
	void actualizaCobroCheque(CobroCheque cobroCheque, DetalleDeposito detalleDeposito, int estado)  throws Throwable;
	/**
	 * Recupera la suma del valor de cheque
	 * @param idCobro	: Id de cobro
	 * @return			: Suma de cheques cobrados
	 * @throws Throwable: Excepcion
	 */
	Double sumaValorCheque(Long idCobro) throws Throwable;
}