package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.CuentaBancaria;
import com.saa.model.tsr.Transferencia;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Transferencia.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface TransferenciaService extends EntityService<Transferencia>{
	 
	/**
	 * Metodo que Crea Asiento Contable de la Transferencia
	 * @param empresa			: Id de la Empresa
	 * @param usuario			: Usuario 
	 * @param idCuentaDestino	: Id de la Cuenta Destino
	 * @param idCuentaOrigen	: Id de la Cuenta Origen
	 * @param valorDeposito		: Valor a Depositar 
	 * @param concepto			: Concepto
	 * @return					:
	 * @throws Throwable		: Excepcions
	 */
	Long[] generaAsientoTransferenciaBancaria(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaOrigen, Double valorDeposito, String concepto) throws Throwable ;

	/**
	 * Metodo que Crea Asiento Contable de la Transferencia
	 * @param empresa			: Id de la Empresa
	 * @param usuario			: Usuario 
	 * @param idCuentaDestino	: Id de la Cuenta Destino
	 * @param idCuentaContable	: Id de la Cuenta contable origen
	 * @param valorDeposito		: Valor a Depositar 
	 * @return					:
	 * @throws Throwable		: Excepcions
	 */
	Long[] generaAsientoTransferenciaContable(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaContable, Double valorDeposito) throws Throwable ;
	
	/**
	 * Metodo que crea la transferencia entre cuentas
	 * @param empresa			: Id de la Empresa
	 * @param usuario			: Usuario 
	 * @param idCuentaDestino	: Id de la Cuenta Destino
	 * @param idCuentaOrigen	: Id de la Cuenta Origen
	 * @param valorDeposito		: Valor a Depositar 
	 * @param concepto			: Concepto
	 * @return					: Id y numero de asiento
	 * @throws Throwable		: Excepcions
	 */
	Long[] tranferenciaCuentaBancaria(Long empresa, String usuario, Long idCuentaDestino, Long idCuentaOrigen, Double valorDeposito, String concepto) throws Throwable ;
	
	/**
	 * Metodo inserta Registros de Transferencia
	 * @param cuentaOrigen		: Entidad cuenta origen
	 * @param cuentaDestino		: Entidad cuenta destino
	 * @param valor				: Valor a transferir
	 * @param concepto			: Descripcion de transferencia
	 * @param usuario			: Nombre del Usuario
	 * @param tipoTranferencia	: Tipo de transferencia
	 * @throws Throwable		: Excepcion
	 */
	void insertaRegistroTransferencia (CuentaBancaria cuentaOrigen, CuentaBancaria cuentaDestino, Double valor, String concepto, String usuario, int tipoTranferencia)throws Throwable;
	
	/**
	  * Metodo que crea la transferencia entre cuentas
	  * @param empresa			: Id de la Empresa
	  * @param usuario			: Usuario 
	  * @param idCuentaDestino	: Id de la Cuenta Destino
	  * @param idCuentaContable	: Id de la Cuenta contable Origen
	  * @param valorDeposito	: Valor a Depositar 
	  * @return					: Id y numero de asiento
	  * @throws Throwable		: Excepcions
	  */
	 Long[] transferenciaCuentaContable (Long empresa, String usuario, Long idCuentaDestino,
			 								 Long idCuentaContable, Double valorDeposito) throws Throwable;
}
