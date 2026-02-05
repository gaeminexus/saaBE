package com.saa.ejb.tesoreria.service;

import com.saa.basico.util.EntityService;
import com.saa.model.tsr.Conciliacion;

import jakarta.ejb.Local;

/**
 * @author GaemiSoft
 * <p>Servicio para la entidad Conciliacion.
 *  Accede a los metodos DAO y procesa los datos para el cliente.</p>
 */
@Local
public interface ConciliacionService extends EntityService<Conciliacion>{
	
	/**
	 * Verifica que el periodo anterior se haya conciliado, a excepcion de la primera vez
	 * @param idCuentaBancaria	: Id Cuenta Bancaria
	 * @param idPeriodo			: Id del periodo a conciliar
	 * @param tipo				: 0 Actual , 1 anterior
	 * @throws Throwable		: Excepcions
	 */
	 void validaConciliacionPeriodo(Long idCuentaBancaria, Long idPeriodo, int tipo) throws Throwable;
	 
	 /**
	 * Verifica que el periodo anterior se haya conciliado, a excepcion de la primera vez
	 * @param idEmpresa			: Id de la Empresa
	 * @param idCuentaBancaria	: Id Cuenta Bancaria
	 * @param idPeriodo			: Id del periodo a conciliar
	 * @param mes				: Mes
	 * @param anio				: Anio
	 * @throws Throwable		: Excepcions
	 */
	 void verificaConciliacion(Long idEmpresa, Long idCuentaBancaria, 
			 Long idPeriodo, Long mes, Long anio) throws Throwable;
	 
	 /**
	 * @param idEmpresa			: Id de la Empresa
	 * @param idCuentaBancaria	: Id Cuenta Bancaria
	 * @param idPeriodo			: Id del periodo a conciliar
	 * @param idUsuario			: Id del usuario que realiza la conciliacion
	 * @param saldoAnteriorSegunBanco: Saldo anterior segun bancos
	 * @return					: Id del registro insertado
	 * @throws Throwable		: Excepcion
	 */
	Conciliacion insertaConciliacion(Long idEmpresa, Long idCuentaBancaria, 
			 Long idPeriodo, Long idUsuario, Double saldoAnteriorSegunBanco) throws Throwable;
	
	/**
	 * Respalda los datos de la conciliacion en las tablas históricas y actualiza los estados
	 * @param idCuenta	: Id de la cuenta bancaria
	 * @param idPeriodo	: Id del periodo
	 * @throws Throwable: Excepcion
	 */
	void desconciliacion(Long idCuenta, Long idPeriodo) throws Throwable;
	
	/**
	 * Elimina datos de conciliacion al momento de desconciliar
	 * @param idConciliacion	: Id de conciliacion
	 * @throws Throwable		: Excepcion
	 */
	void eliminaDatosConciliacion(Long idConciliacion) throws Throwable;
	 
}
